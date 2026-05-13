package com.java6.springboot.internflow.service.impl;

import com.java6.springboot.internflow.dto.request.ReportEntryRequest;
import com.java6.springboot.internflow.dto.request.SubmitDailyReportMailRequest;
import com.java6.springboot.internflow.dto.response.DailyReportEntryResponse;
import com.java6.springboot.internflow.dto.response.MailSubmitResponse;
import com.java6.springboot.internflow.dto.response.ReportDocumentResponse;
import com.java6.springboot.internflow.dto.response.ReportEntryResponse;
import com.java6.springboot.internflow.dto.response.ReportProgressResponse;
import com.java6.springboot.internflow.dto.response.ReportRevisionResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.Attendance;
import com.java6.springboot.internflow.entity.AttendanceImage;
import com.java6.springboot.internflow.entity.ReportDocument;
import com.java6.springboot.internflow.entity.ReportEntry;
import com.java6.springboot.internflow.entity.ReportRevision;
import com.java6.springboot.internflow.entity.ScheduleRegistration;
import com.java6.springboot.internflow.enums.ReportEntryStatus;
import com.java6.springboot.internflow.enums.ScheduleRegistrationStatus;
import com.java6.springboot.internflow.enums.UserRole;
import com.java6.springboot.internflow.exception.BusinessException;
import com.java6.springboot.internflow.exception.NotFoundException;
import com.java6.springboot.internflow.repository.AppUserRepository;
import com.java6.springboot.internflow.repository.AttendanceImageRepository;
import com.java6.springboot.internflow.repository.AttendanceRepository;
import com.java6.springboot.internflow.repository.ReportDocumentRepository;
import com.java6.springboot.internflow.repository.ReportEntryRepository;
import com.java6.springboot.internflow.repository.ReportRevisionRepository;
import com.java6.springboot.internflow.repository.ScheduleRegistrationRepository;
import com.java6.springboot.internflow.service.ReportJournalService;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
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
    private static final int WORDS_PER_PAGE_ESTIMATE = 450;
    private static final DateTimeFormatter SUBJECT_DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.yy");

    private final AppUserRepository appUserRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceImageRepository attendanceImageRepository;
    private final ScheduleRegistrationRepository scheduleRegistrationRepository;
    private final ReportDocumentRepository reportDocumentRepository;
    private final ReportEntryRepository reportEntryRepository;
    private final ReportRevisionRepository reportRevisionRepository;
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
        String subject = user.getFullName() + ", duoc " + document.getCompletedShiftCount()
                + " ca, ngay " + request.workDate().format(SUBJECT_DATE_FORMAT);
        String attachmentName = fileName(user, document.getCompletedShiftCount()) + ".docx";
        byte[] docxBytes = buildJournalDocx(document);

        sendMailWithGmailApi(user, request.googleAccessToken(), subject, buildMailBody(user, dailyEntry, schedules, attendances), attachmentName, docxBytes);
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
        int completedShifts = Math.toIntExact(attendanceRepository.countByUserAndStatus(
                document.getUser(),
                com.java6.springboot.internflow.enums.AttendanceStatus.CHECKED_OUT
        ));
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
            String attachmentName,
            byte[] docxBytes
    ) {
        try {
            MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(user.getEmail());
            helper.setTo(reportMailTo);
            helper.setCc(reportMailCc);
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.addAttachment(attachmentName, new ByteArrayResource(docxBytes));
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
            addParagraph(wordDocument, "Ho ten: " + document.getUser().getFullName());
            addParagraph(wordDocument, "Email: " + document.getUser().getEmail());
            addParagraph(wordDocument, "SDT: " + nullToEmpty(document.getUser().getPhone()));
            addParagraph(wordDocument, "MSSV: " + nullToEmpty(document.getUser().getStudentCode()));
            addParagraph(wordDocument, "Lop: " + nullToEmpty(document.getUser().getStudentClass()));
            addParagraph(wordDocument, "Truong: " + nullToEmpty(document.getUser().getSchool()));
            addParagraph(wordDocument, "");

            reportEntryRepository.findByDocumentOrderByWorkDateAsc(document).forEach(entry -> {
                addHeading(wordDocument, entry.getWorkDate() + " - " + nullToEmpty(entry.getShiftCodes()));
                addParagraph(wordDocument, nullToEmpty(entry.getWorkTimeSummary()));
                addParagraph(wordDocument, "So trang uoc tinh: " + entry.getPageCount() + "/" + entry.getRequiredPages());
                addParagraph(wordDocument, "Tai lieu tham khao: " + nullToEmpty(entry.getReferenceLinks()));
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
        body.append("Ho ten: ").append(user.getFullName()).append('\n');
        body.append("SDT: ").append(nullToEmpty(user.getPhone())).append('\n');
        body.append("Ma so sinh vien: ").append(nullToEmpty(user.getStudentCode())).append('\n');
        body.append("Lop: ").append(nullToEmpty(user.getStudentClass())).append('\n');
        body.append("Truong: ").append(nullToEmpty(user.getSchool())).append('\n');
        body.append("Thoi gian lam viec: ").append(workTimeSummary(schedules)).append("\n\n");

        body.append("Hinh diem danh:\n");
        attendances.forEach(attendance -> {
            body.append("- ").append(attendance.getShift().getName()).append(":\n");
            appendUrl(body, "  TimeMark vao ca", attendance.getCheckinTimemarkImageUrl());
            appendUrl(body, "  Anh nhom vao ca", attendance.getCheckinGroupImageUrl());
            attendanceImageRepository.findByAttendanceIdOrderByExpectedTimeAscDisplayOrderAsc(attendance.getId())
                    .forEach(image -> appendUrl(body, "  " + imageLabel(image), image.getImageUrl()));
            appendUrl(body, "  TimeMark tan ca", attendance.getCheckoutTimemarkImageUrl());
            appendUrl(body, "  Anh nhom tan ca", attendance.getCheckoutGroupImageUrl());
        });

        body.append("\nBao cao ngay: ").append(dailyEntry.getPageCount())
                .append('/').append(dailyEntry.getRequiredPages()).append(" trang uoc tinh\n");
        body.append("Tai lieu tham khao: ").append(nullToEmpty(dailyEntry.getReferenceLinks())).append('\n');
        body.append("\nFile Word nhat ky thuc tap duoc dinh kem trong mail nay.\n");
        return body.toString();
    }

    private void appendUrl(StringBuilder body, String label, String url) {
        if (StringUtils.hasText(url)) {
            body.append(label).append(": ").append(url).append('\n');
        }
    }

    private String imageLabel(AttendanceImage image) {
        return image.getImageType() + " " + image.getPhase() + " " + image.getExpectedTime();
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

    private String workTimeSummary(List<ScheduleRegistration> schedules) {
        return schedules.stream()
                .map(item -> item.getShift().getStartTime().toString().substring(0, 5)
                        + " - "
                        + item.getShift().getEndTime().toString().substring(0, 5))
                .reduce((first, second) -> first + " va " + second)
                .map(value -> "Thoi gian lam viec tu " + value)
                .orElse("");
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
        return "Thay doi " + (delta >= 0 ? "+" : "") + delta + " tu, tu " + oldPages + " len " + newPages + " trang uoc tinh";
    }

    private String fileName(AppUser user, int completedShifts) {
        return "Nhat ky thuc tap- duoc " + completedShifts + " ca -" + user.getFullName();
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
}
