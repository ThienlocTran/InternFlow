package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.AppUser;
import com.java6.springboot.internflow.entity.EmailLog;
import com.java6.springboot.internflow.enums.EmailStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {

    List<EmailLog> findByUserOrderBySentAtDesc(AppUser user);

    List<EmailLog> findByUserAndWorkDateOrderBySentAtDesc(AppUser user, LocalDate workDate);

    List<EmailLog> findByStatusOrderBySentAtDesc(EmailStatus status);

    long countByUserAndStatus(AppUser user, EmailStatus status);
}
