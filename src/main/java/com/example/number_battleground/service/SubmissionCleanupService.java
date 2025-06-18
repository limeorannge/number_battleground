package com.example.number_battleground.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;
import com.example.number_battleground.repository.SubmissionRepository;

@Component
public class SubmissionCleanupService {
    private final SubmissionRepository repo;

    public SubmissionCleanupService(SubmissionRepository repo) {
        this.repo = repo;
    }

    @Scheduled(cron = "00 0 9 * * *", zone = "Asia/Seoul")
    public void purgeOldSubmissions() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        long deleted = repo.deleteByCreatedDateBefore(today);
        if (deleted > 0) {
            System.out.println("Purged " + deleted + " old submissions.");
        }
    }
}