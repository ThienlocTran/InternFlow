package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.ConfirmDailyReportMailRequest;
import com.java6.springboot.internflow.dto.request.ReportEntryRequest;
import com.java6.springboot.internflow.dto.request.SubmitDailyReportMailRequest;
import com.java6.springboot.internflow.dto.response.DailyReportEntryResponse;
import com.java6.springboot.internflow.dto.response.EmailLogResponse;
import com.java6.springboot.internflow.dto.response.MailSubmitResponse;
import com.java6.springboot.internflow.dto.response.ReportEntryResponse;
import com.java6.springboot.internflow.dto.response.ReportProgressResponse;
import com.java6.springboot.internflow.dto.response.ReportRevisionResponse;
import com.java6.springboot.internflow.dto.response.ReportWordUploadResponse;
import com.java6.springboot.internflow.entity.AppUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ReportJournalService {

    ReportProgressResponse getProgress(AppUser user);

    List<DailyReportEntryResponse> getEntriesByDate(LocalDate workDate);

    ReportEntryResponse saveEntry(AppUser currentUser, ReportEntryRequest request);

    ReportWordUploadResponse uploadWordEntry(AppUser currentUser, LocalDate workDate, MultipartFile file);

    StoredWordFile downloadWordEntry(AppUser currentUser, LocalDate workDate);

    List<ReportRevisionResponse> getRevisions(AppUser currentUser, UUID entryId);

    MailSubmitResponse submitDailyMail(AppUser currentUser, SubmitDailyReportMailRequest request);

    MailSubmitResponse confirmDailyMailSent(AppUser currentUser, ConfirmDailyReportMailRequest request);

    List<EmailLogResponse> getEmailLogs(AppUser user);

    record StoredWordFile(String fileName, byte[] bytes, String contentType) {
    }
}
