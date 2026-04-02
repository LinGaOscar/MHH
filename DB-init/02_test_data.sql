USE [MHH_DB];
GO

-- =============================================
-- Default Test Data
-- =============================================

-- Users
INSERT INTO [USER] ([USER_ID], [USER_NAME], [COUNTRY_CODE]) VALUES 
('ADMIN', 'SYSTEM ADMIN', 'TW'),
('MAKER_TW', 'TW MAKER', 'TW'),
('CHECKER_TW', 'TW CHECKER', 'TW'),
('MAKER_HK', 'HK MAKER', 'HK');

-- User Roles
INSERT INTO [USER_ROLE] ([USER_ID], [ROLE_NAME], [COUNTRY_CODE]) VALUES 
('ADMIN', 'PARAM_CHECKER', 'TW'),
('MAKER_TW', 'BRANCH_MAKER', 'TW'),
('CHECKER_TW', 'BRANCH_CHECKER', 'TW'),
('MAKER_HK', 'BRANCH_MAKER', 'HK');

-- Job Configurations
INSERT INTO [JOBS_CONF] ([JOB_NAME], [DESCRIPTION], [CRON_SEC], [CRON_MIN], [CRON_HOUR], [CRON_DOM], [CRON_MONTH], [CRON_DOW], [IS_ENABLED]) VALUES 
('SwalSyncJob', '連線到外部 Oracle DB 同步電文資料 (Workflow A)', '0', '0', '1', '*', '*', '?', 1),
('HrSyncJob', '同步 HR 員工在職狀態與組織架構', '0', '0', '2', '*', '*', '?', 1),
('PdfImportJob', '掃描並解析資料夾 PDF 檔案至 MSG_INCOMING / MSG_OUTGOING (Workflow B)', '0', '0', '23', '*', '*', '?', 1),
('ReservationMergeJob', '處理已放行的電文預約下載與 PDF 合併', '0', '0/5', '*', '*', '*', '?', 1),
('LogCleanupJob', '清理超過一年的 SYS_LOGS 與 USER_LOGS', '0', '0', '3', '*', '*', '?', 1);

-- Sample Incoming Messages (進電)
INSERT INTO [MSG_INCOMING] ([MESSAGE_ID], [REFERENCE], [SENDER], [RECEIVER], [MESSAGE_TYPE], [CONTENT], [SOURCE]) VALUES
('MSG20260330001', 'REF001', 'CHASUS33XXX', 'ICBCTWTPXXX', 'MT103', 'SAMPLE MT103 INCOMING CONTENT...', 'SWAL'),
('MSG20260330002', 'REF002', 'BANKUS33XXX', 'ICBCTWTPXXX', 'MT103', 'SAMPLE PDF INCOMING CONTENT...', 'PDF');

-- Sample Outgoing Messages (出電)
INSERT INTO [MSG_OUTGOING] ([MESSAGE_ID], [REFERENCE], [SENDER], [RECEIVER], [MESSAGE_TYPE], [CONTENT], [SOURCE]) VALUES
('MSG20260330003', 'REF003', 'ICBCTWTPXXX', 'CHASUS33XXX', 'MT103', 'SAMPLE MT103 OUTGOING CONTENT...', 'PDF');
GO
