package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.ConfirmDailyReportMailRequest;
import com.java6.springboot.internflow.dto.request.ReportEntryRequest;
import com.java6.springboot.internflow.dto.request.SubmitDailyReportMailRequest;
import com.java6.springboot.internflow.dto.response.DailyReportEntryResponse;
import com.java6.springboot.internflow.dto.response.EmailLogResponse;
import com.java6.springboot.internflow.dto.response.MailSubmitResponse;
import com.java6.springboot.internflow.dto.response.ReportDocumentResponse;
import com.java6.springboot.internflow.dto.response.ReportEntryResponse;
import com.java6.springboot.internflow.dto.response.ReportProgressResponse;
import com.java6.springboot.internflow.dto.response.ReportRevisionResponse;
import com.java6.springboot.internflow.dto.response.ReportWordUploadResponse;
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
import com.java6.springboot.internflow.exception.ForbiddenException;
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
import com.java6.springboot.internflow.util.ProfileCompleteness;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReportJournalServiceImpl implements ReportJournalService {

    private static final int DAY_SHIFT_REQUIRED_PAGES = 8;
    private static final int NIGHT_SHIFT_REQUIRED_PAGES = 5;
    private static final int WORDS_PER_PAGE_ESTIMATE = 210;
    private static final long MAX_WORD_UPLOAD_BYTES = 10 * 1024 * 1024;
    private static final String WORD_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Path WORD_UPLOAD_ROOT = Path.of("uploads", "report-journals");
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

    @Value("${internflow.image-cleanup.retention-after-mail-days:30}")
    private int retentionAfterMailDays;

    @Override
    @Transactional
    public ReportProgressResponse getProgress(AppUser user) {
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
    public ReportEntryResponse saveEntry(AppUser user, ReportEntryRequest request) {
        validateRequest(request);
        if (user.getRole() != UserRole.INTERN && user.getRole() != UserRole.TEAM_LEADER) {
            throw new ForbiddenException("Chi sinh vien hoac nhom truong moi duoc luu nhat ky");
        }
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
        entry.setSourceReferences(trimToNull(request.sourceReferences()));
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
    @Transactional
    public ReportWordUploadResponse uploadWordEntry(AppUser user, LocalDate workDate, MultipartFile file) {
        if (user.getRole() != UserRole.INTERN && user.getRole() != UserRole.TEAM_LEADER) {
            throw new ForbiddenException("Chi sinh vien hoac nhom truong moi duoc upload nhat ky Word");
        }
        if (workDate == null || workDate.isAfter(LocalDate.now().plusDays(1))) {
            throw new BusinessException("Ngay viet nhat ky khong hop le");
        }
        validateWordFile(file);
        ReportDocument document = getOrCreateDocument(user);
        List<ScheduleRegistration> daySchedules = registeredSchedules(user, workDate);
        if (daySchedules.isEmpty()) {
            throw new BusinessException("Ngay nay chua co ca dang ky nen chua can viet nhat ky");
        }

        WordDocumentContent wordContent = readWordDocument(file);
        ReportEntry entry = reportEntryRepository.findByDocumentAndWorkDate(document, workDate)
                .orElseGet(() -> ReportEntry.builder()
                        .document(document)
                        .workDate(workDate)
                        .build());
        String oldContent = entry.getContent();
        int oldPages = entry.getPageCount();
        int requiredPages = requiredPages(daySchedules);
        int pageCount = wordContent.pageCount() > 0 ? wordContent.pageCount() : estimatePageCount(wordContent.text());

        entry.setContent(wordContent.text());
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
                .newContent(wordContent.text())
                .diffSummary(diffSummary(oldContent, wordContent.text(), oldPages, pageCount))
                .pageCountBefore(oldPages)
                .pageCountAfter(pageCount)
                .build());
        saveWordFile(user, workDate, file);
        refreshDocument(document);
        document.setCurrentFileName(fileNameOnly(file.getOriginalFilename()));
        reportDocumentRepository.save(document);
        return new ReportWordUploadResponse(
                ReportEntryResponse.from(savedEntry),
                document.getCurrentFileName(),
                wordDownloadUrl(user.getId(), workDate),
                pageCount,
                wordContent.wordCount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StoredWordFile downloadWordEntry(AppUser user, LocalDate workDate) {
        if (workDate == null) {
            throw new BusinessException("Ngay tai file Word la bat buoc");
        }
        ReportDocument document = reportDocumentRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Sinh vien chua co nhat ky thuc tap"));
        Path filePath = wordFilePath(user.getId(), workDate);
        if (!Files.exists(filePath)) {
            throw new NotFoundException("Khong tim thay file Word da upload");
        }
        try {
            return new StoredWordFile(
                    storedWordDisplayName(user.getId(), workDate, document.getCurrentFileName()),
                    Files.readAllBytes(filePath),
                    WORD_CONTENT_TYPE
            );
        } catch (IOException exception) {
            throw new BusinessException("Khong the doc file Word da upload");
        }
    }

    private StoredWordFile storedWordFile(AppUser user, LocalDate workDate) {
        Path filePath = wordFilePath(user.getId(), workDate);
        if (!Files.exists(filePath)) {
            return null;
        }
        try {
            ReportDocument document = reportDocumentRepository.findByUser(user).orElse(null);
            String fileName = document != null && StringUtils.hasText(document.getCurrentFileName())
                    ? document.getCurrentFileName()
                    : wordStorageFileName(workDate);
            return new StoredWordFile(storedWordDisplayName(user.getId(), workDate, fileName), Files.readAllBytes(filePath), WORD_CONTENT_TYPE);
        } catch (IOException exception) {
            throw new BusinessException("Khong the doc file Word da upload");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportRevisionResponse> getRevisions(AppUser currentUser, UUID entryId) {
        ReportEntry entry = reportEntryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Khong tim thay nhat ky"));
        assertCanReadEntry(currentUser, entry);
        return reportRevisionRepository.findByEntryOrderByCreatedAtDesc(entry)
                .stream()
                .map(ReportRevisionResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmailLogResponse> getEmailLogs(AppUser user) {
        return emailLogRepository.findByUserOrderBySentAtDesc(user)
                .stream()
                .map(EmailLogResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public MailSubmitResponse submitDailyMail(AppUser user, SubmitDailyReportMailRequest request) {
        if (user.getRole() != UserRole.INTERN && user.getRole() != UserRole.TEAM_LEADER) {
            throw new ForbiddenException("Chi sinh vien hoac nhom truong moi duoc gui mail nhat ky");
        }
        if (request == null || request.workDate() == null) {
            throw new BusinessException("Ngay gui mail la bat buoc");
        }
        if (!StringUtils.hasText(request.googleAccessToken())) {
            throw new BusinessException("Can cap quyen Gmail de gui mail bang chinh tai khoan cua sinh vien");
        }

        List<String> missingProfileFields = ProfileCompleteness.missingRequiredFields(user);
        if (!missingProfileFields.isEmpty()) {
            throw new BusinessException("Ho so sinh vien chua du thong tin bat buoc: " + String.join(", ", missingProfileFields));
        }
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
        UploadedDocument uploadedDocument = decodeUploadedDocument(request);
        StoredWordFile storedWordFile = uploadedDocument == null ? storedWordFile(user, request.workDate()) : null;
        String attachmentName = uploadedDocument != null
                ? uploadedDocument.name()
                : storedWordFile != null
                        ? storedWordFile.fileName()
                        : fileName(user, document.getCompletedShiftCount()) + ".docx";
        byte[] docxBytes = uploadedDocument != null
                ? uploadedDocument.bytes()
                : storedWordFile != null
                        ? storedWordFile.bytes()
                        : buildJournalDocx(document);
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
            markAttendanceImagesRetained(attendances, emailLog.getSentAt());
        } catch (Exception exception) {
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(exception.getMessage());
            emailLogRepository.save(emailLog);
            throw exception;
        }

        emailLogRepository.save(emailLog);
        return new MailSubmitResponse(reportMailTo, reportMailCc, subject, attachmentName);
    }

    @Override
    @Transactional
    public MailSubmitResponse confirmDailyMailSent(AppUser user, ConfirmDailyReportMailRequest request) {
        if (user.getRole() != UserRole.INTERN && user.getRole() != UserRole.TEAM_LEADER) {
            throw new ForbiddenException("Chi sinh vien hoac nhom truong moi duoc xac nhan gui mail nhat ky");
        }
        if (request == null || request.workDate() == null) {
            throw new BusinessException("Ngay xac nhan gui mail la bat buoc");
        }

        ReportDocument document = reportDocumentRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Sinh vien chua co nhat ky thuc tap"));
        ReportEntry dailyEntry = reportEntryRepository.findByDocumentAndWorkDate(document, request.workDate())
                .orElseThrow(() -> new BusinessException("Ngay nay chua co noi dung nhat ky"));
        if (dailyEntry.getPageCount() < dailyEntry.getRequiredPages()) {
            throw new BusinessException("Nhat ky ngay nay chua du so trang yeu cau");
        }

        List<Attendance> attendances = attendanceRepository.findByUserAndAttendanceDateOrderByShift_StartTimeAsc(
                user,
                request.workDate()
        );
        Instant confirmedAt = Instant.now();
        String subject = user.getFullName() + ", xac nhan da gui mail ngay " + request.workDate().format(SUBJECT_DATE_FORMAT);
        EmailLog emailLog = EmailLog.builder()
                .user(user)
                .subject(subject)
                .receivers(reportMailTo)
                .ccReceivers(reportMailCc)
                .workDate(request.workDate())
                .sentAt(confirmedAt)
                .attachmentCount(0)
                .status(EmailStatus.MANUAL_CONFIRMED)
                .build();
        emailLogRepository.save(emailLog);
        markAttendanceImagesRetained(attendances, confirmedAt);
        return new MailSubmitResponse(reportMailTo, reportMailCc, subject, null);
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

    private List<ScheduleRegistration> registeredSchedules(AppUser user, LocalDate workDate) {
        return scheduleRegistrationRepository
                .findByUserAndScheduleDateAndStatus(user, workDate, ScheduleRegistrationStatus.REGISTERED)
                .stream()
                .sorted(Comparator.comparing(item -> item.getShift().getStartTime()))
                .toList();
    }

    private void validateWordFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File Word la bat buoc");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase().endsWith(".docx")) {
            throw new BusinessException("Chi chap nhan file .docx");
        }
        String contentType = file.getContentType();
        if (StringUtils.hasText(contentType)
                && !WORD_CONTENT_TYPE.equalsIgnoreCase(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            throw new BusinessException("Content-Type file Word khong hop le");
        }
        if (file.getSize() > MAX_WORD_UPLOAD_BYTES) {
            throw new BusinessException("File Word vuot qua gioi han 10MB");
        }
    }

    private WordDocumentContent readWordDocument(MultipartFile file) {
        try (InputStream input = file.getInputStream(); XWPFDocument wordDocument = new XWPFDocument(input)) {
            String text = wordDocument.getParagraphs()
                    .stream()
                    .map(XWPFParagraph::getText)
                    .filter(StringUtils::hasText)
                    .reduce((first, second) -> first + "\n" + second)
                    .orElse("");
            return new WordDocumentContent(text, estimatePageCount(text), countWords(text));
        } catch (IOException exception) {
            throw new BusinessException("Khong the doc noi dung file Word");
        }
    }

    private void saveWordFile(AppUser user, LocalDate workDate, MultipartFile file) {
        try {
            Files.createDirectories(wordUserDirectory(user.getId()));
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, wordFilePath(user.getId(), workDate), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(wordFileNamePath(user.getId(), workDate), fileNameOnly(file.getOriginalFilename()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException("Khong the luu file Word nhat ky");
        }
    }

    private Path wordUserDirectory(UUID userId) {
        return WORD_UPLOAD_ROOT.resolve(userId.toString()).normalize();
    }

    private Path wordFilePath(UUID userId, LocalDate workDate) {
        return wordUserDirectory(userId).resolve(wordStorageFileName(workDate)).normalize();
    }

    private Path wordFileNamePath(UUID userId, LocalDate workDate) {
        return wordUserDirectory(userId).resolve(workDate + ".name").normalize();
    }

    private String storedWordDisplayName(UUID userId, LocalDate workDate, String fallback) {
        Path namePath = wordFileNamePath(userId, workDate);
        if (Files.exists(namePath)) {
            try {
                String fileName = Files.readString(namePath, StandardCharsets.UTF_8).trim();
                if (StringUtils.hasText(fileName)) {
                    return fileName;
                }
            } catch (IOException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String wordStorageFileName(LocalDate workDate) {
        return workDate + ".docx";
    }

    private String wordDownloadUrl(UUID userId, LocalDate workDate) {
        return "/api/report-journals/entries/word?userId=" + userId + "&workDate=" + workDate;
    }

    private String fileNameOnly(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "nhat-ky.docx";
        }
        return filename.replace('\\', '/').substring(filename.replace('\\', '/').lastIndexOf('/') + 1);
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
                addParagraph(wordDocument, "Nguon trich dan: " + nullToEmpty(entry.getSourceReferences()));
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
            attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendance.getId())
                    .forEach(image -> appendUrl(body, "  " + imageLabel(image), image.getImageUrl()));
            appendUrl(body, "  TimeMark cuối ca", attendance.getCheckoutTimemarkImageUrl());
        });

        body.append("\nBáo cáo ngày: ").append(dailyEntry.getPageCount())
                .append('/').append(dailyEntry.getRequiredPages()).append(" trang ước tính\n");
        body.append("Tài liệu tham khảo theo ca: ").append(nullToEmpty(dailyEntry.getReferenceLinks())).append('\n');
        body.append("Nguon trich dan theo ca: ").append(nullToEmpty(dailyEntry.getSourceReferences())).append('\n');
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

    private void markAttendanceImagesRetained(List<Attendance> attendances, Instant sentAt) {
        List<UUID> attendanceIds = attendances.stream()
                .map(Attendance::getId)
                .toList();
        if (attendanceIds.isEmpty()) {
            return;
        }
        Instant retentionUntil = sentAt.plus(Duration.ofDays(Math.max(1, retentionAfterMailDays)));
        attendanceImageRepository.markRetentionUntilForAttendances(attendanceIds, retentionUntil);
    }

    private UploadedDocument decodeUploadedDocument(SubmitDailyReportMailRequest request) {
        if (!StringUtils.hasText(request.uploadedDocumentName()) || !StringUtils.hasText(request.uploadedDocumentBase64())) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(request.uploadedDocumentBase64().trim());
            if (bytes.length == 0) {
                return null;
            }
            String normalizedName = request.uploadedDocumentName().trim();
            String finalName = normalizedName.toLowerCase().endsWith(".docx") ? normalizedName : normalizedName + ".docx";
            return new UploadedDocument(finalName, bytes);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("File Word tai len khong hop le");
        }
    }

    private Map<String, String> attendanceImagesForMail(Attendance attendance) {
        Map<String, String> images = new LinkedHashMap<>();
        String shiftName = sanitizeFilePart(attendance.getShift().getName());
        putImage(images, shiftName + "_TimeMark_dau_gio.jpg", attendance.getCheckinTimemarkImageUrl());
        attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendance.getId())
                .forEach(image -> putImage(
                        images,
                        shiftName + "_" + sanitizeFilePart(imageLabel(image)) + ".jpg",
                        image.getImageUrl()
                ));
        putImage(images, shiftName + "_TimeMark_cuoi_ca.jpg", attendance.getCheckoutTimemarkImageUrl());
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

    private int countWords(String content) {
        return StringUtils.hasText(content) ? content.trim().split("\\s+").length : 0;
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
        if (request == null) {
            throw new BusinessException("Du lieu nhat ky la bat buoc");
        }
        if (request.workDate() == null || request.workDate().isAfter(LocalDate.now().plusDays(1))) {
            throw new BusinessException("Ngay viet nhat ky khong hop le");
        }
    }

    private void assertCanReadEntry(AppUser currentUser, ReportEntry entry) {
        boolean owner = entry.getDocument().getUser().getId().equals(currentUser.getId());
        boolean privileged = currentUser.getRole() == UserRole.ADMIN;
        if (!owner && !privileged) {
            throw new ForbiddenException("Ban khong co quyen xem lich su nhat ky cua user khac");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record MailAttachment(String name, byte[] bytes, String contentType) {
    }

    private record RemoteFile(byte[] bytes, String contentType) {
    }

    private record UploadedDocument(String name, byte[] bytes) {
    }

    private record WordDocumentContent(String text, int pageCount, int wordCount) {
    }
}
