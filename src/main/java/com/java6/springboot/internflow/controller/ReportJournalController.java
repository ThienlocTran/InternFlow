package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.ConfirmDailyReportMailRequest;
import com.java6.springboot.internflow.dto.request.ReportEntryRequest;
import com.java6.springboot.internflow.dto.request.SubmitDailyReportMailRequest;
import com.java6.springboot.internflow.dto.response.DailyReportEntryResponse;
import com.java6.springboot.internflow.dto.response.EmailLogResponse;
import com.java6.springboot.internflow.dto.response.MailSubmitResponse;
import com.java6.springboot.internflow.dto.response.ReportEntryResponse;
import com.java6.springboot.internflow.dto.response.ReportProgressResponse;
import com.java6.springboot.internflow.dto.response.ReportRevisionResponse;
import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.security.CurrentUserService;
import com.java6.springboot.internflow.service.ReportJournalService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report-journals")
@RequiredArgsConstructor
public class ReportJournalController {

    private final ReportJournalService reportJournalService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ApiResponse<ReportProgressResponse> getProgress(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) UUID userId
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        AppUser targetUser = currentUserService.resolveRequestedUser(currentUser, userId);
        return ApiResponse.ok("Lay tien do nhat ky thanh cong", reportJournalService.getProgress(targetUser));
    }

    @GetMapping("/daily")
    public ApiResponse<List<DailyReportEntryResponse>> getEntriesByDate(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) LocalDate workDate
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.requireAdminOrManager(currentUser);
        return ApiResponse.ok("Lay nhat ky theo ngay thanh cong", reportJournalService.getEntriesByDate(workDate));
    }

    @PutMapping("/entries")
    public ApiResponse<ReportEntryResponse> saveEntry(
            HttpServletRequest httpRequest,
            @RequestBody ReportEntryRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.rejectMismatchedRequestUser(currentUser, request == null ? null : request.userId());
        return ApiResponse.ok("Luu nhat ky thanh cong", reportJournalService.saveEntry(currentUser, request));
    }

    @GetMapping("/entries/{entryId}/revisions")
    public ApiResponse<List<ReportRevisionResponse>> getRevisions(
            HttpServletRequest httpRequest,
            @PathVariable UUID entryId
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        return ApiResponse.ok("Lay lich su sua nhat ky thanh cong", reportJournalService.getRevisions(currentUser, entryId));
    }

    @PostMapping("/submit-mail")
    public ApiResponse<MailSubmitResponse> submitDailyMail(
            HttpServletRequest httpRequest,
            @RequestBody SubmitDailyReportMailRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.rejectMismatchedRequestUser(currentUser, request == null ? null : request.userId());
        return ApiResponse.ok("Gui mail cuoi ngay thanh cong", reportJournalService.submitDailyMail(currentUser, request));
    }

    @PostMapping("/confirm-mail-sent")
    public ApiResponse<MailSubmitResponse> confirmDailyMailSent(
            HttpServletRequest httpRequest,
            @RequestBody ConfirmDailyReportMailRequest request
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        currentUserService.rejectMismatchedRequestUser(currentUser, request == null ? null : request.userId());
        return ApiResponse.ok("Xac nhan da gui mail cuoi ngay thanh cong", reportJournalService.confirmDailyMailSent(currentUser, request));
    }

    @GetMapping("/email-logs")
    public ApiResponse<List<EmailLogResponse>> getEmailLogs(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) UUID userId
    ) {
        AppUser currentUser = currentUserService.requireCurrentUser(httpRequest);
        AppUser targetUser = currentUserService.resolveRequestedUser(currentUser, userId);
        return ApiResponse.ok("Lay lich su gui mail thanh cong", reportJournalService.getEmailLogs(targetUser));
    }
}
