# Mail System Redesign - Gửi từ System Email

## Hiện trạng

**Code hiện tại:**
- ✅ Gửi mail qua Gmail API
- ✅ From = email sinh viên (dùng OAuth token của sinh viên)
- ✅ Đính kèm file Word + ảnh
- ❌ Sinh viên phải cấp quyền Gmail mỗi lần
- ❌ Không có email_logs table

## Yêu cầu mới

**Thay đổi:**
- ✅ From: `internflow.system@gmail.com`
- ✅ Reply-To: email sinh viên
- ✅ Sử dụng JavaMailSender (SMTP)
- ✅ Lưu log vào `email_logs` table
- ✅ Không cần sinh viên cấp quyền Gmail

## Implementation Plan

### 1. Database Migration

```sql
CREATE TABLE email_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    subject VARCHAR(500) NOT NULL,
    receivers TEXT NOT NULL,
    cc_receivers TEXT,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    attachment_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_logs_user_id ON email_logs(user_id);
CREATE INDEX idx_email_logs_sent_at ON email_logs(sent_at DESC);
CREATE INDEX idx_email_logs_status ON email_logs(status);
```

### 2. Entity

```java
@Entity
@Table(name = "email_logs")
public class EmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;
    
    private String subject;
    private String receivers;
    private String ccReceivers;
    private Instant sentAt;
    
    @Enumerated(EnumType.STRING)
    private EmailStatus status; // SENT, FAILED
    
    private String errorMessage;
    private Integer attachmentCount;
    private Instant createdAt;
}
```

### 3. Configuration (application.properties)

```properties
# System email configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=internflow.system@gmail.com
spring.mail.password=${SYSTEM_EMAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Mail recipients
internflow.mail.to=tuyendungbpns@gmail.com
internflow.mail.cc=xuandat210425cty@gmail.com
```

### 4. Service Method (New)

```java
@Service
public class SystemMailService {
    
    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    
    @Value("${spring.mail.username}")
    private String systemEmail;
    
    @Value("${internflow.mail.to}")
    private String mailTo;
    
    @Value("${internflow.mail.cc}")
    private String mailCc;
    
    public void sendDailyReport(
        AppUser user,
        LocalDate workDate,
        String subject,
        String htmlBody,
        List<MailAttachment> attachments
    ) {
        EmailLog log = new EmailLog();
        log.setUser(user);
        log.setSubject(subject);
        log.setReceivers(mailTo);
        log.setCcReceivers(mailCc);
        log.setAttachmentCount(attachments.size());
        log.setSentAt(Instant.now());
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // From: system email
            helper.setFrom(systemEmail, "InternFlow System");
            
            // Reply-To: student email
            helper.setReplyTo(user.getEmail(), user.getFullName());
            
            // To & CC
            helper.setTo(mailTo);
            helper.setCc(mailCc);
            
            // Subject
            helper.setSubject("[InternFlow] " + subject);
            
            // HTML body
            helper.setText(htmlBody, true);
            
            // Attachments
            for (MailAttachment att : attachments) {
                helper.addAttachment(
                    att.getFileName(),
                    new ByteArrayResource(att.getContent()),
                    att.getContentType()
                );
            }
            
            mailSender.send(message);
            log.setStatus(EmailStatus.SENT);
            
        } catch (Exception e) {
            log.setStatus(EmailStatus.FAILED);
            log.setErrorMessage(e.getMessage());
            throw new BusinessException("Không thể gửi mail: " + e.getMessage());
        } finally {
            emailLogRepository.save(log);
        }
    }
}
```

### 5. Update ReportJournalService

```java
@Override
@Transactional
public MailSubmitResponse submitDailyMail(SubmitDailyReportMailRequest request) {
    // Validate
    AppUser user = findUser(request.userId());
    ReportDocument document = getDocument(user);
    ReportEntry dailyEntry = getDailyEntry(document, request.workDate());
    
    // Build content
    List<Attendance> attendances = getAttendances(user, request.workDate());
    List<ScheduleRegistration> schedules = getSchedules(user, request.workDate());
    
    String subject = buildSubject(user, document, request.workDate());
    String htmlBody = buildHtmlBody(user, dailyEntry, schedules, attendances);
    
    // Build attachments
    List<MailAttachment> attachments = new ArrayList<>();
    attachments.add(buildWordAttachment(document, user));
    attachments.addAll(buildImageAttachments(attendances));
    
    // Send via system email (NO NEED GOOGLE TOKEN!)
    systemMailService.sendDailyReport(
        user,
        request.workDate(),
        subject,
        htmlBody,
        attachments
    );
    
    return new MailSubmitResponse(mailTo, mailCc, subject, attachments.get(0).getFileName());
}
```

### 6. Remove Google OAuth Requirement

**Old:**
```java
if (!StringUtils.hasText(request.googleAccessToken())) {
    throw new BusinessException("Can cap quyen Gmail...");
}
```

**New:**
```java
// No need Google token anymore!
// System email handles everything
```

## Benefits

### ✅ Ưu điểm

1. **Không cần OAuth:** Sinh viên không phải cấp quyền Gmail
2. **Đơn giản hơn:** Chỉ cần bấm "Gửi báo cáo"
3. **Có log:** Lưu lịch sử gửi mail vào database
4. **Reply-To:** Công ty reply trực tiếp cho sinh viên
5. **Branded:** From = internflow.system@gmail.com (chuyên nghiệp)

### ⚠️ Lưu ý

1. **App Password:** Cần tạo App Password cho `internflow.system@gmail.com`
2. **Gmail Limits:** 
   - Free: 500 emails/day
   - Workspace: 2000 emails/day
3. **Attachment Size:** Max 25MB/email

## Setup Steps

### Step 1: Create System Email

1. Tạo Gmail: `internflow.system@gmail.com`
2. Enable 2FA
3. Tạo App Password: https://myaccount.google.com/apppasswords
4. Copy password

### Step 2: Update Environment Variables

```bash
# Backend .env
SYSTEM_EMAIL_PASSWORD=your-app-password-here
```

### Step 3: Run Migration

```bash
psql -U postgres -d internflow < db/migrations/2026-05-31-r2-foundation-shift-photo-source.sql
```

### Step 4: Update Frontend

**Remove Google OAuth button:**
```typescript
// Old: Cần OAuth
const gmailToken = await requestGmailSendToken();

// New: Không cần OAuth
await submitDailyReportMail(userId, workDate);
```

## Migration Path

### Phase 1: Keep both systems
- Old: Gmail API (for backward compatibility)
- New: System email (default)

### Phase 2: Deprecate Gmail API
- Remove Google OAuth requirement
- Remove `googleAccessToken` from request

### Phase 3: Clean up
- Remove Gmail API code
- Remove Google OAuth setup docs

## Testing

```bash
# Test send email
curl -X POST http://localhost:8080/api/report-journals/submit-mail \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "uuid-here",
    "workDate": "2026-05-18"
  }'
```

## Rollback Plan

If system email fails:
1. Keep Gmail API code
2. Add feature flag: `USE_SYSTEM_EMAIL=false`
3. Fallback to Gmail API

## Cost Analysis

| Solution | Cost | Pros | Cons |
|----------|------|------|------|
| Gmail API | Free | No SMTP | Need OAuth |
| System Email (Free) | Free | Simple | 500 emails/day |
| System Email (Workspace) | $6/month | 2000 emails/day | Cost |
| SendGrid | $15/month | 40k emails/month | External service |

**Recommendation:** Start with System Email (Free), upgrade to Workspace if needed.
