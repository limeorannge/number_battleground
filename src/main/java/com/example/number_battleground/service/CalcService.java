package com.example.number_battleground.service;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import org.springframework.stereotype.Service;

import com.example.number_battleground.controller.CalcController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;

@Service
public class CalcService {
    private static final Logger log = LoggerFactory.getLogger(CalcController.class);
    // ────────── 내부 메모리 저장소 ──────────
    // Submission(수식 + 오차율 + 날짜) 목록을 메모리에 보관
    private final List<Submission> submissions = new ArrayList<>();

    /**
     * evaluateExpression:
     *   - 입력된 raw 수식을 계산하여 CalcResponse 반환
     *   - 기존 기능(수치 계산, 기호 계산, 연산자 개수, 난수, 오차율) 동일
     */
    public static String extractBigRawDigits(String symjaOutput) {
    // 1. 괄호 안에서 첫 번째 인자 추출
        int start = symjaOutput.indexOf('(');
        int comma = symjaOutput.indexOf(',');
        int E = symjaOutput.indexOf('E');
        int point = symjaOutput.indexOf('.');
        if (start == -1 || comma == -1) return null;

        String numberPart = symjaOutput.substring(start + 1, E).trim();
        String integerPart = symjaOutput.substring(start+1, point).trim();
        String decimalPart = symjaOutput.substring(point + 1, E).trim();
        String exponentPart = symjaOutput.substring(E + 1, comma).trim();
        numberPart = numberPart.replace(".", "");
        if(Integer.parseInt(exponentPart) >0){
            if(numberPart.length() <= Integer.parseInt(exponentPart)){
                // 2-1. 지수 부분이 숫자 길이보다 크면, 0을 채워줌
                int zeroCount = Integer.parseInt(exponentPart) - numberPart.length()+1;
                StringBuilder sb = new StringBuilder(numberPart);
                for(int i = 0; i < zeroCount; i++){
                    sb.append("0");
                }
                numberPart = sb.toString();
            }
            integerPart = numberPart.substring(0, Integer.parseInt(exponentPart)+1);
            decimalPart = numberPart.substring(Integer.parseInt(exponentPart)+1);
            if(decimalPart.length() > 0){
                numberPart = integerPart + "."+decimalPart;
            } else {
                numberPart = integerPart;
            }
        }
        else{
            StringBuilder sb = new StringBuilder("");
            for(int i=1; i< Math.abs(Integer.parseInt(exponentPart)); i++){
                sb.append("0");
            }
            numberPart = "0."+sb.toString() + numberPart.substring(0, 8);
        }
        // 2. 소수점 제거
       
        return numberPart;
    }
    public static String extractSmallRawDigits(String symjaOutput) {
    // 1. 괄호 안에서 첫 번째 인자 추출
        int start = symjaOutput.indexOf('(');
        int comma = symjaOutput.indexOf(',');
        if (start == -1 || comma == -1) return null;

        String numberPart = symjaOutput.substring(start + 1, comma).trim();

        // 2. 소수점 제거
        return numberPart;
    }
    public CalcResponse evaluateExpression(String raw) {
        long dailyRandLong = generateDailyRandom();
        Map<String, Integer> opCounts = countOperators(raw);

        if (raw == null || raw.trim().isEmpty()) {
            return new CalcResponse(
                "", "오류: 입력이 비었습니다", "오류: 입력이 비었습니다",
                dailyRandLong, "—", opCounts
            );
        }

        // 한 자리 숫자 검증
        Pattern digitSeq = Pattern.compile("\\d+");
        Matcher matcher = digitSeq.matcher(raw);
        while (matcher.find()) {
            String numToken = matcher.group();
            if (numToken.length() > 1) {
                return new CalcResponse(
                    raw, "두 자리 수 입력 금지", "두 자리 수 입력 금지",
                    dailyRandLong, "—", opCounts
                );
            }
        }
        if (raw.contains(".")) {
            return new CalcResponse(
                raw, "두 자리 수 입력 금지", "두 자리 수 입력 금지",
                dailyRandLong, "—", opCounts
            );
        }

        String expr = preprocessLatex(raw);
        ExprEvaluator util = new ExprEvaluator();
        String symbolicTeX;
        String numeric5;
        double numericValue;
        try {
            IExpr texForm = util.eval("TeXForm(" + expr + ")");
            symbolicTeX = texForm.toString();

            IExpr numSymja = util.eval("NumberForm[N(" + expr + "), Infinity]");
            
            String numeric6 = numSymja.toString();
            if(numeric6.contains("E")){
                numeric6 = extractBigRawDigits(numeric6);
            }
            else{
                numeric6 = extractSmallRawDigits(numeric6);
            }
            log.error(numSymja.toString());
            numeric5 = numeric6;
            numericValue = Double.parseDouble(numeric6);
        } catch (Exception e) {
            log.error("{}", e);
            symbolicTeX  = "오류: 계산 불가능";
            numeric5     = "오류";
            numericValue = Double.NaN;
        }

        String errorRate;
        if (Double.isNaN(numericValue)) {
            errorRate = "오류";
        } else {
            double err = Math.abs(numericValue - dailyRandLong) / dailyRandLong * 100.0;
            errorRate = String.format("%.2f%%", err);
        }

        return new CalcResponse(
            raw, symbolicTeX, numeric5, dailyRandLong, errorRate, opCounts
        );
    }

    /**
     * submitExpression:
     *   - 사용자가 “제출” 버튼을 눌러 보낸 수식 raw를 받아서
     *     1) evaluateExpression(...) 호출하여 오차율 계산
     *     2) 오늘 날짜(LocalDate, 서울)와 함께 Submission 객체를 메모리에 저장
     *     3) 그날 제출된 모든 Submission 목록을 SubmissionDTO 형태로 반환
     */
    public List<SubmissionDTO> submitExpression(String raw) {
        // 1) 계산 로직(오차율 등) 수행
        CalcResponse cr = evaluateExpression(raw);
        String errorRate = cr.getErrorRate();

        // 2) 오늘 날짜 기준 Submission 저장
        LocalDate todaySeoul = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Submission sub = new Submission(raw, errorRate, todaySeoul);
        synchronized (submissions) {
            submissions.add(sub);
        }

        // 3) 오늘 날짜에 해당하는 저장 목록만 필터링하여 DTO 생성
        List<SubmissionDTO> todayList = new ArrayList<>();
        synchronized (submissions) {
            for (Submission s : submissions) {
                if (s.getDate().equals(todaySeoul)) {
                    todayList.add(new SubmissionDTO(s.getExpression(), s.getErrorRate()));
                }
            }
        }
        return todayList;
    }

    // ────────── 내부 클래스: 저장용 Submission ──────────
    private static class Submission {
        private final String expression;
        private final String errorRate;
        private final LocalDate date;

        public Submission(String expression, String errorRate, LocalDate date) {
            this.expression = expression;
            this.errorRate = errorRate;
            this.date = date;
        }
        public String getExpression() { return expression; }
        public String getErrorRate()  { return errorRate; }
        public LocalDate getDate()    { return date; }
    }

    // ────────── 외부 응답용 DTO: 프론트엔드에 보낼 목록 항목 ──────────
    public static class SubmissionDTO {
        private final String expression;
        private final String errorRate;
        public SubmissionDTO(String expression, String errorRate) {
            this.expression = expression;
            this.errorRate = errorRate;
        }
        public String getExpression() { return expression; }
        public String getErrorRate()  { return errorRate; }
    }

    // ────────── 기존 로직(전처리, 연산자 개수 세기, 난수 생성 등) ──────────

    private String preprocessLatex(String s) {
        if (s == null) return "";
        String tmp = s;
        tmp = tmp.replaceAll("\\\\frac\\{([^}]*)\\}\\{([^}]*)\\}", "($1)/($2)");
        tmp = tmp.replaceAll("\\\\sqrt\\{([^}]*)\\}", "Sqrt[$1]");
        tmp = tmp.replaceAll("\\\\sqrt\\(([^)]*)\\)", "Sqrt[$1]");
        tmp = tmp.replaceAll("sqrt\\{([^}]*)\\}", "Sqrt[$1]");
        tmp = tmp.replaceAll("sqrt\\(([^)]*)\\)", "Sqrt[$1]");
        return tmp;
    }

    private Map<String, Integer> countOperators(String raw) {
        Map<String, Integer> counts = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            counts.put("plus", 0);
            counts.put("minus", 0);
            counts.put("multiply", 0);
            counts.put("sqrt", 0);
            counts.put("tan", 0);
            counts.put("parenPairs", 0);
            return counts;
        }
        int plusCount     = raw.length() - raw.replace("+", "").length();
        int minusCount    = raw.length() - raw.replace("-", "").length();
        int multiplyCount = raw.length() - raw.replace("*", "").length();
        int sqrtCount     = 0;
        Matcher m1 = Pattern.compile("sqrt").matcher(raw);
        while (m1.find()) sqrtCount++;
        int tanCount = 0;
        Matcher m2 = Pattern.compile("tan").matcher(raw);
        while (m2.find()) tanCount++;
        int openParen  = raw.length() - raw.replace("(", "").length();
        int closeParen = raw.length() - raw.replace(")", "").length();
        int parenPairs = Math.min(openParen, closeParen);
        counts.put("plus", plusCount);
        counts.put("minus", minusCount);
        counts.put("multiply", multiplyCount);
        counts.put("sqrt", sqrtCount);
        counts.put("tan", tanCount);
        counts.put("parenPairs", parenPairs);
        return counts;
    }

    private long generateDailyRandom() {
        LocalDate todaySeoul = LocalDate.now(ZoneId.of("Asia/Seoul"));
        int seed = todaySeoul.getYear() * 10000
                 + todaySeoul.getMonthValue() * 100
                 + todaySeoul.getDayOfMonth();
        Random rnd = new Random(seed);
        return (long) rnd.nextInt(1_000_000_000) + 1L;
    }

    // ────────── 기존 CalcResponse ──────────
    public static class CalcResponse {
        private final String expression;
        private final String symbolicTeX;
        private final String numericResult;
        private final String dailyRandom;
        private final String errorRate;
        private final Map<String, Integer> operatorCounts;

        public CalcResponse(
            String expression,
            String symbolicTeX,
            String numericResult,
            long dailyRandom,
            String errorRate,
            Map<String, Integer> operatorCounts
        ) {
            this.expression     = expression;
            this.symbolicTeX    = symbolicTeX;
            this.numericResult  = numericResult;
            this.dailyRandom    = String.valueOf(dailyRandom);
            this.errorRate      = errorRate;
            this.operatorCounts = operatorCounts;
        }
        public String getExpression()    { return expression; }
        public String getSymbolicTeX()   { return symbolicTeX; }
        public String getNumericResult() { return numericResult; }
        public String getDailyRandom()   { return dailyRandom; }
        public String getErrorRate()     { return errorRate; }
        public Map<String,Integer> getOperatorCounts() { return operatorCounts; }
    }
}
