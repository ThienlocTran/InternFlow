package com.java6.springboot.internflow.controller;

import com.java6.springboot.internflow.dto.ApiResponse;
import com.java6.springboot.internflow.dto.request.ReportEntryRequest;
import com.java6.springboot.internflow.dto.request.SubmitDailyReportMailRequest;
import com.java6.springboot.internflow.dto.response.DailyReportEntryResponse;
import com.java6.springboot.internflow.dto.response.EmailLogResponse;
import com.java6.springboot.internflow.dto.response.MailSubmitResponse;
import com.java6.springboot.internflow.dto.response.ReportEntryResponse;
import com.java6.springboot.internflow.dto.response.ReportProgressResponse;
import com.java6.springboot.internflow.dto.response.ReportRevisionResponse;
import com.java6.springboot.internflow.service.ReportJournalService;
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

    @GetMapping
    public ApiResponse<ReportProgressResponse> getProgress(@RequestParam UUID userId) {
        return ApiResponse.ok("Lay tien do nhat ky thanh cong", reportJournalService.getProgress(userId));
    }

    @GetMapping("/daily")
    public ApiResponse<List<DailyReportEntryResponse>> getEntriesByDate(@RequestParam(required = false) LocalDate workDate) {
        return ApiResponse.ok("Lay nhat ky theo ngay thanh cong", reportJournalService.getEntriesByDate(workDate));
    }

    @PutMapping("/entries")
    public ApiResponse<ReportEntryResponse> saveEntry(@RequestBody ReportEntryRequest request) {
        return ApiResponse.ok("Luu nhat ky thanh cong", reportJournalService.saveEntry(request));
    }

    @GetMapping("/entries/{entryId}/revisions")
    public ApiResponse<List<ReportRevisionResponse>> getRevisions(@PathVariable UUID entryId) {
        return ApiResponse.ok("Lay lich su sua nhat ky thanh cong", reportJournalService.getRevisions(entryId));
    }

    @PostMapping("/submit-mail")
    public ApiResponse<MailSubmitResponse> submitDailyMail(@RequestBody SubmitDailyReportMailRequest request) {
        return ApiResponse.ok("Gui mail cuoi ngay thanh cong", reportJournalService.submitDailyMail(request));
    }

    @GetMapping("/email-logs")
    public ApiResponse<List<EmailLogResponse>> getEmailLogs(@RequestParam UUID userId) {
        return ApiResponse.ok("Lay lich su gui mail thanh cong", reportJournalService.getEmailLogs(userId));
    }
}
