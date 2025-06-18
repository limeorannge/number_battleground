package com.example.number_battleground.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
public class SubmissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String expression;
    private double errorRate;
    private int operatorCount;
    private double penalty;
    private LocalDate createdDate;
    private String nickname;
    private LocalDateTime createdAt;

    protected SubmissionEntity() {}  // JPA용

    public SubmissionEntity(String expression,
                            double errorRate,
                            int operatorCount,
                            double penalty,
                            LocalDate createdDate,
                            String nickname,
                            LocalDateTime createdAt) {
        this.expression    = expression;
        this.errorRate     = errorRate;
        this.operatorCount = operatorCount;
        this.penalty       = penalty;
        this.createdDate   = createdDate;
        this.nickname      = nickname;
        this.createdAt     = createdAt;
    }

    // getters (세터는 불필요)
    public Long getId()               { return id; }
    public String getExpression()     { return expression; }
    public double getErrorRate()      { return errorRate; }
    public int getOperatorCount()     { return operatorCount; }
    public double getPenalty()        { return penalty; }
    public LocalDate getCreatedDate() { return createdDate; }
    public String getNickname()     { return nickname; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}