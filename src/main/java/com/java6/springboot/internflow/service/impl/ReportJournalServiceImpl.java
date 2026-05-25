package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.ReportEntryRequest;
import com.java6.springboot.internflow.dto.request.SubmitDailyReportMailRequest;
import com.java6.springboot.internflow.dto.response.DailyReportEntryResponse;
import com.java6.springboot.internflow.dto.response.EmailLogResponse;
import com.java6.springboot.internflow.dto.response.MailSubmitResponse;
import com.java6.springboot.internflow.dto.response.ReportDocumentResponse;
import com.java6.springboot.internflow.dto.response.ReportEntryResponse;
import com.java6.springboot.internflow.dto.response.ReportProgressResponse;
import com.java6.springboot.internflow.dto.response.ReportRevisionResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.EmailLog;
import com.java6.springboot.internflow.entity.ReportDocument;
import com.java6.springboot.internflow.entity.ReportEntry;
import com.java6.springboot.internflow.entity.ReportRevision;
import com.java6.springboot.internflow.entity.ScheduleRegistration;
import com.java6.springboot.internflow.enums.EmailStatus;
import com.java6.springboot.internflow.enums.ReportEntryStatus;
import com.java6.springboot.internflow.enums.ScheduleRegistrationStatus;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceImageRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.EmailLogRepository;
import com.java6.springboot.internflow.repository.ReportDocumentRepository;
import com.java6.springboot.internflow.repository.ReportEntryRepository;
import com.java6.springboot.internflow.repository.ReportRevisionRepository;
import com.java6.springboot.internflow.repository.ScheduleRegistrationRepository;
import com.java6.springboot.internflow.service.ReportJournalService;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReportJournalServiceImpl implements ReportJournalService {

    private static final int DAY_SHIFT_REQUIRED_PAGES = 8;
    private static final int NIGHT_SHIFT_REQUIRED_PAGES = 5;
    private static final int WORDS_PER_PAGE_ESTIMATE = 210;
    private static final int MAX_IMAGE_ATTACHMENT_BYTES = 8 * 1024 * 1024;
    private static final int MAX_MAIL_ATTACHMENT_BYTES = 20 * 1024 * 1024;
    private static final DateTimeFormatter SUBJECT_DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yy");

    private final AppUserRepository appUserRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceImageRepository attendanceImageRepository;
    private final ScheduleRegistrationRepository scheduleRegistrationRepository;
    private final ReportDocumentRepository reportDocumentRepository;
    private final ReportEntryRepository reportEntryRepository;
    private final ReportRevisionRepository reportRevisionRepository;
    private final EmailLogRepository emailLogRepository;
    private final InternshipProgressCalculator internshipProgressCalculator;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${internflow.mail.to:tuyendungbpns@gmail.com}")
    private String reportMailTo;

    @Value("${internflow.mail.cc:xuandat210425cty@gmail.com}")
    private String reportMailCc;

    @Override
    @Transactional
    public ReportProgressResponse getProgress(UUID userId) {
        AppUser user = findUser(userId);
        ReportDocument document = getOrCreateDocument(user);
        return new ReportProgressResponse(
                ReportDocumentResponse.from(document),
                reportEntryRepository.findByDocumentOrderByWorkDateDesc(document)
                        .stream()
                        .map(ReportEntryResponse::from)
                        .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyReportEntryResponse> getEntriesByDate(LocalDate workDate) {
        LocalDate targetDate = workDate == null ? LocalDate.now() : workDate;
        return reportEntryRepository.findByWorkDateOrderByUpdatedAtDesc(targetDate)
                .stream()
                .filter(entry -> {
                    UserRole role = entry.getDocument().getUser().getRole();
                    return role == UserRole.INTERN || role == UserRole.TEAM_LEADER;
                })
                .map(DailyReportEntryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public ReportEntryResponse saveEntry(ReportEntryRequest request) {
        validateRequest(request);
        AppUser user = findUser(request.userId());
        ReportDocument document = getOrCreateDocument(user);
        List<ScheduleRegistration> daySchedules = scheduleRegistrationRepository
                .findByUserAndScheduleDateAndStatus(user, request.workDate(), ScheduleRegistrationStatus.REGISTERED)
                .stream()
                .sorted(Comparator.comparing(item -> item.getShift().getStartTime()))
                .toList();
        if (daySchedules.isEmpty()) {
            throw new BusinessException("Ngay nay chua co ca dang ky nen chua can viet nhat ky");
        }

        ReportEntry entry = reportEntryRepository.findByDocumentAndWorkDate(document, request.workDate())
                .orElseGet(() -> ReportEntry.builder()
                        .document(document)
                        .workDate(request.workDate())
                        .build());
        String oldContent = entry.getContent();
        int oldPages = entry.getPageCount();
        String content = request.content() == null ? "" : request.content().trim();
        int pageCount = estimatePageCount(content);
        int requiredPages = requiredPages(daySchedules);

        entry.setContent(content);
        entry.setReferenceLinks(trimToNull(request.referenceLinks()));
        entry.setShiftCodes(shiftCodes(daySchedules));
        entry.setShiftCount(daySchedules.size());
        entry.setWorkTimeSummary(workTimeSummary(daySchedules));
        entry.setPageCount(pageCount);
        entry.setRequiredPages(requiredPages);
        entry.setStatus(pageCount >= requiredPages ? ReportEntryStatus.READY_FOR_MAIL : ReportEntryStatus.NEEDS_MORE_PAGES);
        ReportEntry savedEntry = reportEntryRepository.save(entry);

        reportRevisionRepository.save(ReportRevision.builder()
                .entry(savedEntry)
                .oldContent(oldContent)
                .newContent(content)
                .diffSummary(diffSummary(oldContent, content, oldPages, pageCount))
                .pageCountBefore(oldPages)
                .pageCountAfter(pageCount)
                .build());
        refreshDocument(document);
        return ReportEntryResponse.from(savedEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportRevisionResponse> getRevisions(UUID entryId) {
        ReportEntry entry = reportEntryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay nhat ky"));
        return reportRevisionRepository.findByEntryOrderByCreatedAtDesc(entry)
                .stream()
                .map(ReportRevisionResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailLogResponse> getEmailLogs(UUID userId) {
        AppUser user = findUser(userId);
        return emailLogRepository.findByUserOrderBySentAtDesc(user)
                .stream()
                .map(EmailLogResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public MailSubmitResponse submitDailyMail(SubmitDailyReportMailRequest request) {
        if (request == null || request.userId() == null || request.workDate() == null) {
            throw new BusinessException("User id va ngay gui mail la bat buoc");
        }
        if (!StringUtils.hasText(request.googleAccessToken())) {
            throw new BusinessException("Can cap quyen Gmail de gui mail bang chinh tai khoan cua sinh vien");
        }

        AppUser user = findUser(request.userId());
        String tokenEmail = fetchGoogleEmail(request.googleAccessToken());
        if (!user.getEmail().equalsIgnoreCase(tokenEmail)) {
            throw new BusinessException("Token Gmail khong khop voi email dang nhap");
        }
        ReportDocument document = reportDocumentRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Sinh vien chua co nhat ky thuc tap"));
        refreshDocument(document);
        ReportEntry dailyEntry = reportEntryRepository.findByDocumentAndWorkDate(document, request.workDate())
                .orElseThrow(() -> new BusinessException("Ngay nay chua co noi dung nhat ky"));
        if (dailyEntry.getPageCount() < dailyEntry.getRequiredPages()) {
            throw new BusinessException("Nhat ky ngay nay chua du so trang yeu cau");
        }

        List<ScheduleRegistration> schedules = scheduleRegistrationRepository
                .findByUserAndScheduleDateAndStatus(user, request.workDate(), ScheduleRegistrationStatus.REGISTERED)
                .stream()
                .sorted(Comparator.comparing(item -> item.getShift().getStartTime()))
                .toList();
        List<Attendance> attendances = attendanceRepository.findByUserAndAttendanceDateOrderByShift_StartTimeAsc(
                user,
                request.workDate()
        );
        int completedShiftCount = document.getCompletedShiftCount();
        String subject = user.getFullName() + ", được " + completedShiftCount
                + " ca, ngày " + request.workDate().format(SUBJECT_DATE_FORMAT);
        String attachmentName = fileName(user, document.getCompletedShiftCount()) + ".docx";
        byte[] docxBytes = buildJournalDocx(document);
        List<MailAttachment> attachments = new ArrayList<>();
        attachments.add(new MailAttachment(
                attachmentName,
                docxBytes,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ));
        List<MailAttachment> imageAttachments = buildAttendanceImageAttachments(attendances);
        attachments.addAll(imageAttachments);

        // Create email log before sending
        EmailLog emailLog = EmailLog.builder()
                .user(user)
                .subject(subject)
                .receivers(reportMailTo)
                .ccReceivers(reportMailCc)
                .workDate(request.workDate())
                .sentAt(java.time.Instant.now())
                .attachmentCount(1 + imageAttachments.size())
                .status(EmailStatus.PENDING)
                .build();

        try {
            sendMailWithGmailApi(user, request.googleAccessToken(), subject, buildMailBody(user, dailyEntry, schedules, attendances), attachments);
            emailLog.setStatus(EmailStatus.SENT);
        } catch (Exception exception) {
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(exception.getMessage());
            emailLogRepository.save(emailLog);
            throw exception;
        }

        emailLogRepository.save(emailLog);
        return new MailSubmitResponse(reportMailTo, reportMailCc, subject, attachmentName);
    }

    private ReportDocument getOrCreateDocument(AppUser user) {
        return reportDocumentRepository.findByUser(user)
                .orElseGet(() -> {
                    ReportDocument document = ReportDocument.builder()
                            .user(user)
                            .title("Nhat ky thuc tap")
                            .currentFileName(fileName(user, 0))
                            .build();
                    return reportDocumentRepository.save(document);
                });
    }

    private void refreshDocument(ReportDocument document) {
        List<ReportEntry> entries = reportEntryRepository.findByDocumentOrderByWorkDateDesc(document);
        int totalPages = entries.stream().mapToInt(ReportEntry::getPageCount).sum();
        int completedShifts = internshipProgressCalculator.calculateEffectiveCompletedCompanyShifts(document.getUser());
        document.setTotalPages(totalPages);
        document.setCompletedShiftCount(completedShifts);
        document.setCurrentFileName(fileName(document.getUser(), completedShifts));
        reportDocumentRepository.save(document);
    }

    private void sendMailWithGmailApi(
            AppUser user,
            String googleAccessToken,
            String subject,
            String body,
            List<MailAttachment> attachments
    ) {
        try {
            MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(user.getEmail());
            helper.setTo(reportMailTo);
            helper.setCc(reportMailCc);
            helper.setSubject(subject);
            helper.setText(body, false);
            for (MailAttachment attachment : attachments) {
                helper.addAttachment(attachment.name(), new ByteArrayResource(attachment.bytes()), attachment.contentType());
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            message.writeTo(output);
            String rawMessage = Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                    .header("Authorization", "Bearer " + googleAccessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"raw\":\"" + rawMessage + "\"}", StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("Gmail tu choi gui mail. Hay cap quyen gui mail lai");
            }
        } catch (MessagingException exception) {
            throw new BusinessException("Khong the tao mail cuoi ngay");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("Gui mail that bai qua Gmail cua sinh vien");
        }
    }

    private String fetchGoogleEmail(String googleAccessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                    .header("Authorization", "Bearer " + googleAccessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("Khong xac minh duoc tai khoan Gmail gui mail");
            }
            return objectMapper.readTree(response.body()).path("email").asText().trim().toLowerCase();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("Khong xac minh duoc email Google");
        }
    }

    private byte[] buildJournalDocx(ReportDocument document) {
        try (XWPFDocument wordDocument = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            addHeading(wordDocument, fileName(document.getUser(), document.getCompletedShiftCount()));
            addParagraph(wordDocument, "Họ tên: " + document.getUser().getFullName());
            addParagraph(wordDocument, "Email: " + document.getUser().getEmail());
            addParagraph(wordDocument, "SĐT: " + nullToEmpty(document.getUser().getPhone()));
            addParagraph(wordDocument, "MSSV: " + nullToEmpty(document.getUser().getStudentCode()));
            addParagraph(wordDocument, "Lớp: " + nullToEmpty(document.getUser().getStudentClass()));
            addParagraph(wordDocument, "Trường: " + nullToEmpty(document.getUser().getSchool()));
            addParagraph(wordDocument, "");

            reportEntryRepository.findByDocumentOrderByWorkDateAsc(document).forEach(entry -> {
                addHeading(wordDocument, entry.getWorkDate() + " - " + nullToEmpty(entry.getShiftCodes()));
                addParagraph(wordDocument, nullToEmpty(entry.getWorkTimeSummary()));
                addParagraph(wordDocument, "Số trang ước tính: " + entry.getPageCount() + "/" + entry.getRequiredPages());
                addParagraph(wordDocument, "Tài liệu tham khảo: " + nullToEmpty(entry.getReferenceLinks()));
                addParagraph(wordDocument, nullToEmpty(entry.getContent()));
                addParagraph(wordDocument, "");
            });

            wordDocument.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException("Khong the tao file Word nhat ky");
        }
    }

    private String buildMailBody(
            AppUser user,
            ReportEntry dailyEntry,
            List<ScheduleRegistration> schedules,
            List<Attendance> attendances
    ) {
        StringBuilder body = new StringBuilder();
        body.append("Họ tên: ").append(user.getFullName()).append('\n');
        body.append("SĐT: ").append(nullToEmpty(user.getPhone())).append('\n');
        body.append("Mã số sinh viên: ").append(nullToEmpty(user.getStudentCode())).append('\n');
        body.append("Lớp: ").append(nullToEmpty(user.getStudentClass())).append('\n');
        body.append("Trường: ").append(nullToEmpty(user.getSchool())).append('\n');
        body.append("Ca đã làm hôm nay: ").append(shiftCodesCompact(schedules)).append('\n');
        body.append("Thời gian làm việc: ").append(workTimeSummary(schedules)).append("\n\n");

        body.append("Hình điểm danh được đính kèm trong mail và liệt kê link bên dưới:\n");
        attendances.forEach(attendance -> {
            body.append("- ").append(attendance.getShift().getName()).append(":\n");
            appendUrl(body, "  TimeMark đầu giờ", attendance.getCheckinTimemarkImageUrl());
            appendUrl(body, "  Ảnh nhóm đầu giờ", attendance.getCheckinGroupImageUrl());
            attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendance.getId())
                    .forEach(image -> appendUrl(body, "  " + imageLabel(image), image.getImageUrl()));
            appendUrl(body, "  TimeMark cuối ca", attendance.getCheckoutTimemarkImageUrl());
            appendUrl(body, "  Ảnh nhóm cuối ca", attendance.getCheckoutGroupImageUrl());
        });

        body.append("\nBáo cáo ngày: ").append(dailyEntry.getPageCount())
                .append('/').append(dailyEntry.getRequiredPages()).append(" trang ước tính\n");
        body.append("Tài liệu tham khảo theo ca: ").append(nullToEmpty(dailyEntry.getReferenceLinks())).append('\n');
        body.append("\nFile Word nhật ký thực tập được đính kèm trong mail này.\n");
        return body.toString();
    }

    private List<MailAttachment> buildAttendanceImageAttachments(List<Attendance> attendances) {
        List<MailAttachment> attachments = new ArrayList<>();
        int totalBytes = 0;
        for (Attendance attendance : attendances) {
            Map<String, String> images = attendanceImagesForMail(attendance);
            for (Map.Entry<String, String> image : images.entrySet()) {
                RemoteFile remoteFile = downloadRemoteFile(image.getValue());
                if (remoteFile == null || remoteFile.bytes().length > MAX_IMAGE_ATTACHMENT_BYTES) {
                    continue;
                }
                if (totalBytes + remoteFile.bytes().length > MAX_MAIL_ATTACHMENT_BYTES) {
                    continue;
                }
                totalBytes += remoteFile.bytes().length;
                attachments.add(new MailAttachment(image.getKey(), remoteFile.bytes(), remoteFile.contentType()));
            }
        }
        return attachments;
    }

    private Map<String, String> attendanceImagesForMail(Attendance attendance) {
        Map<String, String> images = new LinkedHashMap<>();
        String shiftName = sanitizeFilePart(attendance.getShift().getName());
        putImage(images, shiftName + "_TimeMark_dau_gio.jpg", attendance.getCheckinTimemarkImageUrl());
        putImage(images, shiftName + "_Anh_nhom_dau_gio.jpg", attendance.getCheckinGroupImageUrl());
        attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendance.getId())
                .forEach(image -> putImage(
                        images,
                        shiftName + "_" + sanitizeFilePart(imageLabel(image)) + ".jpg",
                        image.getImageUrl()
                ));
        putImage(images, shiftName + "_TimeMark_cuoi_ca.jpg", attendance.getCheckoutTimemarkImageUrl());
        putImage(images, shiftName + "_Anh_nhom_cuoi_ca.jpg", attendance.getCheckoutGroupImageUrl());
        return images;
    }

    private void putImage(Map<String, String> images, String name, String url) {
        if (StringUtils.hasText(url) && !images.containsValue(url)) {
            images.put(name, url);
        }
    }

    private RemoteFile downloadRemoteFile(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("image/jpeg");
            return new RemoteFile(response.body(), contentType);
        } catch (Exception exception) {
            return null;
        }
    }

    private void appendUrl(StringBuilder body, String label, String url) {
        if (StringUtils.hasText(url)) {
            body.append(label).append(": ").append(url).append('\n');
        }
    }

    private String imageLabel(AttendanceImage image) {
        String type = switch (image.getImageType()) {
            case PERSONAL_TIMEMARK -> "TimeMark";
            case GROUP -> "Ảnh nhóm";
        };
        String phase = switch (image.getPhase()) {
            case CHECKIN -> "đầu giờ";
            case DURING_SHIFT -> "giữa giờ";
            case CHECKOUT -> "cuối ca";
        };
        return type + " " + phase + " " + formatTime(image.getExpectedTime());
    }

    private void addHeading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(16);
        run.setText(text);
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setFontSize(12);
        for (String line : nullToEmpty(text).split("\\R", -1)) {
            run.setText(line);
            run.addBreak();
        }
    }

    private int requiredPages(List<ScheduleRegistration> schedules) {
        boolean hasDayShift = schedules.stream().anyMatch(item -> {
            String code = item.getShift().getCode();
            return "SHIFT_1".equals(code) || "SHIFT_2".equals(code);
        });
        return hasDayShift ? DAY_SHIFT_REQUIRED_PAGES : NIGHT_SHIFT_REQUIRED_PAGES;
    }

    private String shiftCodes(List<ScheduleRegistration> schedules) {
        return schedules.stream()
                .map(item -> item.getShift().getName())
                .reduce((first, second) -> first + ", " + second)
                .orElse("");
    }

    private String shiftCodesCompact(List<ScheduleRegistration> schedules) {
        String codes = schedules.stream()
                .map(item -> {
                    String name = item.getShift().getName();
                    String number = name == null ? "" : name.replaceAll("\\D+", "");
                    return StringUtils.hasText(number) ? number : name;
                })
                .filter(StringUtils::hasText)
                .reduce((first, second) -> first + "+" + second)
                .orElse("");
        return StringUtils.hasText(codes) ? "ca " + codes : "";
    }

    private String workTimeSummary(List<ScheduleRegistration> schedules) {
        return schedules.stream()
                .map(item -> formatTime(item.getShift().getStartTime())
                        + " đến "
                        + formatTime(item.getShift().getEndTime()))
                .reduce((first, second) -> first + " và " + second)
                .map(value -> "từ " + value)
                .orElse("");
    }

    private String formatTime(LocalTime time) {
        if (time == null) {
            return "";
        }
        return time.getMinute() == 0 ? time.getHour() + "h" : time.getHour() + "h" + String.format("%02d", time.getMinute());
    }

    private int estimatePageCount(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        int words = content.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / (double) WORDS_PER_PAGE_ESTIMATE));
    }

    private String diffSummary(String oldContent, String newContent, int oldPages, int newPages) {
        int oldWords = StringUtils.hasText(oldContent) ? oldContent.trim().split("\\s+").length : 0;
        int newWords = StringUtils.hasText(newContent) ? newContent.trim().split("\\s+").length : 0;
        int delta = newWords - oldWords;
        return "Thay đổi " + (delta >= 0 ? "+" : "") + delta + " từ, từ " + oldPages + " lên " + newPages + " trang ước tính";
    }

    private String fileName(AppUser user, int completedShifts) {
        return "Nhật ký thực tập- được " + completedShifts + " ca -" + user.getFullName();
    }

    private String sanitizeFilePart(String value) {
        String decoded = value == null ? "" : URLDecoder.decode(value, StandardCharsets.UTF_8);
        return decoded.replaceAll("[^\\p{L}\\p{N}]+", "_").replaceAll("^_+|_+$", "");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay user"));
    }

    private void validateRequest(ReportEntryRequest request) {
        if (request == null || request.userId() == null) {
            throw new BusinessException("User id la bat buoc");
        }
        if (request.workDate() == null || request.workDate().isAfter(LocalDate.now().plusDays(1))) {
            throw new BusinessException("Ngay viet nhat ky khong hop le");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record MailAttachment(String name, byte[] bytes, String contentType) {
    }

    private record RemoteFile(byte[] bytes, String contentType) {
    }
}
