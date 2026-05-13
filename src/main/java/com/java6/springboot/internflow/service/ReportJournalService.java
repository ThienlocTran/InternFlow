package com.java6.springboot.internflow.service;

import com.java6.springboot.internflow.dto.request.ReportEntryRequest;
import com.java6.springboot.internflow.dto.request.SubmitDailyReportMailRequest;
import com.java6.springboot.internflow.dto.response.DailyReportEntryResponse;
import com.java6.springboot.internflow.dto.response.MailSubmitResponse;
import com.java6.springboot.internflow.dto.response.ReportEntryResponse;
import com.java6.springboot.internflow.dto.response.ReportProgressResponse;
import com.java6.springboot.internflow.dto.response.ReportRevisionResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportJournalService {

    ReportProgressResponse getProgress(UUID userId);

    List<DailyReportEntryResponse> getEntriesByDate(LocalDate workDate);

    ReportEntryResponse saveEntry(ReportEntryRequest request);

    List<ReportRevisionResponse> getRevisions(UUID entryId);

    MailSubmitResponse submitDailyMail(SubmitDailyReportMailRequest request);
}
