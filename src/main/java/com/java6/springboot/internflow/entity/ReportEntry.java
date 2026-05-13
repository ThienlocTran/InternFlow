package com.java6.springboot.internflow.entity;

import com.java6.springboot.internflow.enums.ReportEntryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "report_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_entry_document_date",
                columnNames = {"document_id", "work_date"}
        )
)
public class ReportEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private ReportDocument document;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "shift_codes", length = 120)
    private String shiftCodes;

    @Column(name = "shift_count", nullable = false)
    private int shiftCount;

    @Column(name = "work_time_summary", length = 500)
    private String workTimeSummary;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "reference_links", columnDefinition = "text")
    private String referenceLinks;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "required_pages", nullable = false)
    private int requiredPages;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportEntryStatus status = ReportEntryStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
