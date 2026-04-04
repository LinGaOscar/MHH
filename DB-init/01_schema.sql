-- =============================================
-- MHH Database Schema Initialization (MS SQL Server)
-- =============================================

USE [master];
GO

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'MHH_DB')
BEGIN
    CREATE DATABASE [MHH_DB];
END
GO

USE [MHH_DB];
GO

-- 1. 使用者基礎帳號表
CREATE TABLE [USER] (
    [USER_ID] NVARCHAR(50) PRIMARY KEY,
    [USER_NAME] NVARCHAR(100) NOT NULL,
    [COUNTRY_CODE] NVARCHAR(10) NOT NULL,
    [STATUS] NVARCHAR(20) DEFAULT 'ACTIVE',
    [CREATED_AT] DATETIME DEFAULT GETDATE(),
    [UPDATED_AT] DATETIME DEFAULT GETDATE()
);

-- 2. 使用者自訂角色與權限映射表
CREATE TABLE [USER_ROLE] (
    [ROLE_ID] INT IDENTITY(1,1) PRIMARY KEY,
    [USER_ID] NVARCHAR(50) NOT NULL,
    [ROLE_NAME] NVARCHAR(50) NOT NULL, -- BRANCH_MAKER, BRANCH_CHECKER, PARAM_MAKER, PARAM_CHECKER
    [COUNTRY_CODE] NVARCHAR(10) NOT NULL,
    FOREIGN KEY ([USER_ID]) REFERENCES [USER]([USER_ID])
);

-- 3. 自定義使用者屬性配置表格
CREATE TABLE [USER_CUST] (
    [CUST_ID] INT IDENTITY(1,1) PRIMARY KEY,
    [USER_ID] NVARCHAR(50) NOT NULL,
    [ATTR_KEY] NVARCHAR(100),
    [ATTR_VALUE] NVARCHAR(MAX),
    [STATUS] NVARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED
    FOREIGN KEY ([USER_ID]) REFERENCES [USER]([USER_ID])
);

-- 4. HR 員工基本資料同步表
CREATE TABLE [HR_USER] (
    [EMP_ID] NVARCHAR(50) PRIMARY KEY,
    [EMP_NAME] NVARCHAR(100),
    [UNIT_CODE] NVARCHAR(20),
    [IS_ACTIVE] BIT DEFAULT 1,
    [LAST_SYNC] DATETIME DEFAULT GETDATE()
);

-- 5. HR 組織單位架構同步表
CREATE TABLE [HR_UNIT] (
    [UNIT_CODE] NVARCHAR(20) PRIMARY KEY,
    [UNIT_NAME] NVARCHAR(100),
    [PARENT_UNIT] NVARCHAR(20)
);

-- 6a. 進電搜尋表 (對齊 SWAL: SWMIMSG)
-- 僅存可查詢欄位，原始電文另存 MSG_INCOMING_TX
CREATE TABLE [MSG_INCOMING] (
    [ID]           BIGINT IDENTITY(1,1) PRIMARY KEY,

    -- ── 識別 ──────────────────────────────────────────────────────────
    [MESSAGE_ID]   NVARCHAR(100) UNIQUE,   -- SWAL: SW_UMID
    [MT_TYPE]      NVARCHAR(10),           -- SWAL: SWMIMSG.MESG_TYPE      (e.g. "103")
    [MX_TYPE]      NVARCHAR(50),           -- SWAL: SWMIMSG.MX_MESG_TYPE   (e.g. "pacs.008.001.09")
    [MESSAGE_TYPE] NVARCHAR(50),           -- 顯示用主類型：有MX取MX，否則 "MT"+MESG_TYPE

    -- ── 路由 ──────────────────────────────────────────────────────────
    [SENDER]       NVARCHAR(50),           -- SWAL: SEND_SWIFT_ADDR
    [RECEIVER]     NVARCHAR(50),           -- SWAL: RECV_SWIFT_ADDR
    [UNIT_CODE]    NVARCHAR(20),           -- SWAL: MESG_UNIT

    -- ── 金融 ──────────────────────────────────────────────────────────
    [AMOUNT]       DECIMAL(18,5),          -- SWAL: TAG_AMT
    [CURRENCY]     NVARCHAR(3),            -- SWAL: CCY
    [VALUE_DATE]   NVARCHAR(8),            -- SWAL: VALUE_DATE (YYYYMMDD)

    -- ── 參考 ──────────────────────────────────────────────────────────
    [REFERENCE]    NVARCHAR(100),          -- SWAL: SWP_REF_NO_ITEM
    [TAG_20]       NVARCHAR(35),           -- SWAL: TAG_20
    [TAG_21]       NVARCHAR(35),           -- SWAL: TAG_21
    [OSN]          NVARCHAR(20),           -- SWAL: SESSION_NO + SEQUENCE_NO

    -- ── 狀態 ──────────────────────────────────────────────────────────
    [AML_FLAG]     NVARCHAR(1),            -- SWAL: AML_FLAG
    [AML_STATUS]   NVARCHAR(10),           -- SWAL: AML_RESULT
    [FLOW_STATUS]  NVARCHAR(1),            -- SWAL: HOST_TRANSMIT_STUS (in-Flow)
    [PDE_FLAG]     NVARCHAR(1),            -- SWAL: PDE_FLAG

    -- ── 時間 ──────────────────────────────────────────────────────────
    [MSG_DATE]     DATETIME,               -- SWAL: MESG_CREATE_DATE_TIME (增量 watermark)
    [SYNC_TIME]    DATETIME DEFAULT GETDATE(),
    [SOURCE]       NVARCHAR(20),           -- SWAL | PDF

    -- ── 彈性 JSON ────────────────────────────────────────────────────
    [PARAMETERS]   NVARCHAR(MAX)           -- PDE_TRAILER, PDM_TRAILER, AML細項, MX_MSGID 等
);

-- 6a-tx. 進電電文表 (對齊 SWAL: SWIMTX)
-- 以 MESSAGE_ID 對應 MSG_INCOMING，單獨儲存原始電文以避免全表掃描時載入大欄位
CREATE TABLE [MSG_INCOMING_TX] (
    [ID]           BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MESSAGE_ID]   NVARCHAR(100) UNIQUE,   -- FK → MSG_INCOMING.MESSAGE_ID
    [MT_CONTENT]   NVARCHAR(MAX),          -- MT 原始 Block 4 格式電文
    [MX_CONTENT]   NVARCHAR(MAX),          -- MX 原始 XML 電文
    [SYNC_TIME]    DATETIME DEFAULT GETDATE(),
    FOREIGN KEY ([MESSAGE_ID]) REFERENCES [MSG_INCOMING]([MESSAGE_ID])
);

-- 6b. 出電搜尋表 (對齊 SWAL: SWMOMSG)
-- 僅存可查詢欄位，原始電文另存 MSG_OUTGOING_TX
CREATE TABLE [MSG_OUTGOING] (
    [ID]           BIGINT IDENTITY(1,1) PRIMARY KEY,

    -- ── 識別 ──────────────────────────────────────────────────────────
    [MESSAGE_ID]   NVARCHAR(100) UNIQUE,
    [MT_TYPE]      NVARCHAR(10),
    [MX_TYPE]      NVARCHAR(50),
    [MESSAGE_TYPE] NVARCHAR(50),

    -- ── 路由 ──────────────────────────────────────────────────────────
    [SENDER]       NVARCHAR(50),
    [RECEIVER]     NVARCHAR(50),
    [UNIT_CODE]    NVARCHAR(20),

    -- ── 金融 ──────────────────────────────────────────────────────────
    [AMOUNT]       DECIMAL(18,5),
    [CURRENCY]     NVARCHAR(3),
    [VALUE_DATE]   NVARCHAR(8),

    -- ── 參考 ──────────────────────────────────────────────────────────
    [REFERENCE]    NVARCHAR(100),
    [TAG_20]       NVARCHAR(35),
    [TAG_21]       NVARCHAR(35),
    [OSN]          NVARCHAR(20),

    -- ── 狀態 ──────────────────────────────────────────────────────────
    [AML_FLAG]     NVARCHAR(1),
    [AML_STATUS]   NVARCHAR(10),
    [FLOW_STATUS]  NVARCHAR(1),
    [PDE_FLAG]     NVARCHAR(1),

    -- ── 時間 ──────────────────────────────────────────────────────────
    [MSG_DATE]     DATETIME,
    [SYNC_TIME]    DATETIME DEFAULT GETDATE(),
    [SOURCE]       NVARCHAR(20),

    -- ── 彈性 JSON ────────────────────────────────────────────────────
    [PARAMETERS]   NVARCHAR(MAX)
);

-- 6b-tx. 出電電文表 (對齊 SWAL: SWOMTX)
CREATE TABLE [MSG_OUTGOING_TX] (
    [ID]           BIGINT IDENTITY(1,1) PRIMARY KEY,
    [MESSAGE_ID]   NVARCHAR(100) UNIQUE,   -- FK → MSG_OUTGOING.MESSAGE_ID
    [MT_CONTENT]   NVARCHAR(MAX),
    [MX_CONTENT]   NVARCHAR(MAX),
    [SYNC_TIME]    DATETIME DEFAULT GETDATE(),
    FOREIGN KEY ([MESSAGE_ID]) REFERENCES [MSG_OUTGOING]([MESSAGE_ID])
);

-- 7. 預約下載詳情與合併狀態表
CREATE TABLE [MSG_DOWNLOAD] (
    [DOWNLOAD_ID] UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    [APPLICANT_ID] NVARCHAR(50) NOT NULL,
    [STATUS] NVARCHAR(20) DEFAULT 'PENDING', 
    [QUERY_CRITERIA] NVARCHAR(MAX),
    [FILE_COUNT] INT DEFAULT 0,
    [FILE_SIZE] BIGINT DEFAULT 0,
    [ZIP_PATH] NVARCHAR(500),
    [EXPIRY_DATE] DATETIME,
    [CREATED_AT] DATETIME DEFAULT GETDATE(),
    FOREIGN KEY ([APPLICANT_ID]) REFERENCES [USER]([USER_ID])
);

-- 8. 工作流審核任務表
CREATE TABLE [MSG_APPROVAL] (
    [APPROVAL_ID] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [REF_ID] NVARCHAR(100), 
    [APPLICANT_ID] NVARCHAR(50),
    [APPROVER_ID] NVARCHAR(50),
    [TYPE] NVARCHAR(50), 
    [STATUS] NVARCHAR(20), 
    [REMARK] NVARCHAR(500),
    [CREATED_AT] DATETIME DEFAULT GETDATE(),
    [APPROVED_AT] DATETIME
);

-- 9. SWALLOW 原始資料同步至本系統之暫存表 (Workflow A)
CREATE TABLE [MSG_SWAL_SYNC] (
    [SYNC_ID] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [RAW_DATA] NVARCHAR(MAX),
    [SYNC_STATUS] NVARCHAR(20) DEFAULT 'NEW',
    [CREATED_AT] DATETIME DEFAULT GETDATE()
);

-- 10. 系統運行日誌 (對齊 SysLog.java)
CREATE TABLE [SYS_LOGS] (
    [LOG_ID] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [LOG_LEVEL] NVARCHAR(10),
    [LOGGER_NAME] NVARCHAR(200),
    [MESSAGE] NVARCHAR(MAX),
    [STACK_TRACE] NVARCHAR(MAX),
    [CREATE_TIME] DATETIME DEFAULT GETDATE(),
    [NODE_IP] NVARCHAR(50)
);

-- 11. 使用者行為稽核日誌 (對齊 UserLog.java)
CREATE TABLE [USER_LOGS] (
    [LOG_ID] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [USER_ID] NVARCHAR(50),
    [ACTION] NVARCHAR(100),
    [DESCRIPTION] NVARCHAR(MAX),
    [IP_ADDRESS] NVARCHAR(50),
    [LOG_TIME] DATETIME DEFAULT GETDATE(),
    [STATUS] NVARCHAR(20)
);

-- 12. 背景任務配置
CREATE TABLE [JOBS_CONF] (
    [JOB_NAME] NVARCHAR(100) PRIMARY KEY,
    [DESCRIPTION] NVARCHAR(200), 
    [CRON_SEC] NVARCHAR(10) DEFAULT '0', 
    [CRON_MIN] NVARCHAR(10) DEFAULT '0', 
    [CRON_HOUR] NVARCHAR(10) DEFAULT '0', 
    [CRON_DOM] NVARCHAR(10) DEFAULT '*', 
    [CRON_MONTH] NVARCHAR(10) DEFAULT '*', 
    [CRON_DOW] NVARCHAR(10) DEFAULT '?', 
    [IS_ENABLED] BIT DEFAULT 1, 
    [LAST_RUN] DATETIME,
    [PARAMS] NVARCHAR(MAX) 
);

-- 13. 排程 Job 執行歷史
CREATE TABLE [JOBS_LOGS] (
    [RUN_ID] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [JOB_NAME] NVARCHAR(100),
    [START_TIME] DATETIME,
    [END_TIME] DATETIME,
    [STATUS] NVARCHAR(20),
    [ERROR_MSG] NVARCHAR(MAX)
);
GO

-- Indices for Performance
CREATE INDEX [IDX_MSG_INCOMING_SYNC_TIME] ON [MSG_INCOMING]([SYNC_TIME]);
CREATE INDEX [IDX_MSG_INCOMING_SENDER]    ON [MSG_INCOMING]([SENDER]);
CREATE INDEX [IDX_MSG_INCOMING_MSG_DATE]  ON [MSG_INCOMING]([MSG_DATE]);
CREATE INDEX [IDX_MSG_INCOMING_TAG20]     ON [MSG_INCOMING]([TAG_20]);
CREATE INDEX [IDX_MSG_OUTGOING_SYNC_TIME] ON [MSG_OUTGOING]([SYNC_TIME]);
CREATE INDEX [IDX_MSG_OUTGOING_SENDER]    ON [MSG_OUTGOING]([SENDER]);
CREATE INDEX [IDX_MSG_OUTGOING_MSG_DATE]  ON [MSG_OUTGOING]([MSG_DATE]);
CREATE INDEX [IDX_USER_LOGS_USER]         ON [USER_LOGS]([USER_ID]);
GO
