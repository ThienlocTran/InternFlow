package com.java6.springboot.internflow.repository;

import com.java6.springboot.internflow.entity.Shift;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    Optional<Shift> findByCode(String code);

    boolean existsByCode(String code);

    List<Shift> findByActiveTrueOrderByShiftOrderAscStartTimeAsc();

    List<Shift> findAllByOrderByShiftOrderAscStartTimeAsc();
}
