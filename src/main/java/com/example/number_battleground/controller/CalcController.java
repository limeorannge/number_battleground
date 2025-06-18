package com.example.number_battleground.controller;

import com.example.number_battleground.service.CalcService;
import com.example.number_battleground.service.CalcService.CalcResponse;
import com.example.number_battleground.service.CalcService.SubmissionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CalcController {

    private final CalcService calcService;

    @Autowired
    public CalcController(CalcService calcService) {
        this.calcService = calcService;
    }

    // ────────── 기존 “실시간 계산” 엔드포인트 (/api/calc) ──────────
    @PostMapping(
        path     = "/calc",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CalcResponse calc(@RequestBody Map<String, String> payload) {
        String rawExpr = payload.get("expression");
        if (rawExpr == null || rawExpr.trim().isEmpty()) {
            return calcService.evaluateExpression("");
        }
        return calcService.evaluateExpression(rawExpr.trim());
    }

    // ────────── 신규 엔드포인트: “제출” 기능 (/api/submit) ──────────
    @PostMapping(
        path     = "/submit",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public List<SubmissionDTO> submit(@RequestBody Map<String, String> payload) {
        String rawExpr = payload.get("expression");
        if (rawExpr == null) {
            rawExpr = "";
        }
        // service.submitExpression → 오늘 날짜 기준으로 목록을 반환
        return calcService.submitExpression(rawExpr.trim());
    }
}
