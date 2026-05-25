-- Migration: Create email_logs table
-- Date: 2026-05-18
-- Description: Lưu lịch sử gửi mail báo cáo cuối ca

CREATE TABLE IF NOT EXISTS email_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    subject VARCHAR(500) NOT NULL,
    receivers TEXT NOT NULL,
    cc_receivers TEXT,
    work_date DATE NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    attachment_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_email_logs_user_id ON email_logs(user_id);
CREATE INDEX idx_email_logs_work_date ON email_logs(work_date DESC);
CREATE INDEX idx_email_logs_sent_at ON email_logs(sent_at DESC);
CREATE INDEX idx_email_logs_status ON email_logs(status);

-- Comments
COMMENT ON TABLE email_logs IS 'Lịch sử gửi mail báo cáo cuối ca';
COMMENT ON COLUMN email_logs.user_id IS 'Sinh viên gửi mail';
COMMENT ON COLUMN email_logs.subject IS 'Tiêu đề mail';
COMMENT ON COLUMN email_logs.receivers IS 'Danh sách người nhận (TO)';
COMMENT ON COLUMN email_logs.cc_receivers IS 'Danh sách người nhận (CC)';
COMMENT ON COLUMN email_logs.work_date IS 'Ngày làm việc';
COMMENT ON COLUMN email_logs.sent_at IS 'Thời gian gửi';
COMMENT ON COLUMN email_logs.status IS 'Trạng thái: SENT, FAILED';
COMMENT ON COLUMN email_logs.error_message IS 'Lỗi nếu gửi thất bại';
COMMENT ON COLUMN email_logs.attachment_count IS 'Số file đính kèm';
