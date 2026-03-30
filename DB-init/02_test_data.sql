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
INSERT INTO [JOBS_CONF] ([JOB_NAME], [CRON_EXPRESSION], [IS_ENABLED]) VALUES 
('SwallowSyncJob', '0 0 1 * * ?', 1),
('HrSyncJob', '0 0 2 * * ?', 1),
('PdfImportJob', '0 0 23 * * ?', 1),
('ReservationMergeJob', '0 0/5 * * * ?', 1),
('LogCleanupJob', '0 0 3 * * ?', 1);

-- Sample Message History
INSERT INTO [MSG_HISTORY] ([REFERENCE_NO], [SENDER_BIC], [RECEIVER_BIC], [MSG_TYPE], [MSG_CONTENT], [TRANS_DATE]) VALUES 
('REF20260330001', 'CHASUS33XXX', 'ICBCTWTPXXX', 'MT103', 'SAMPLE MT103 CONTENT...', '2026-03-30'),
('REF20260330002', 'ICBCTWTPXXX', 'CHASUS33XXX', 'MT202', 'SAMPLE MT202 CONTENT...', '2026-03-30');
GO
