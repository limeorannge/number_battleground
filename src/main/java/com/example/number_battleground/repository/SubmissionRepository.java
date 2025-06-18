package com.example.number_battleground.repository;

import com.example.number_battleground.entity.SubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SubmissionRepository
        extends JpaRepository<SubmissionEntity, Long> {
    List<SubmissionEntity> findAllByCreatedDateOrderByPenalty(LocalDate date);
    long deleteByCreatedDateBefore(LocalDate date);
}