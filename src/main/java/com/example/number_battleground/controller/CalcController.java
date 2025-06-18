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
    @PostMapping("/submit")
    public List<SubmissionDTO> submit(@RequestBody Map<String,String> payload) {
        String expr = payload.get("expression");
        String nickname = payload.get("nickname");
        return calcService.submitExpression(expr, nickname);
    }

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
}
