package com.example.number_battleground.service;

import com.example.number_battleground.controller.CalcController;
import com.example.number_battleground.entity.SubmissionEntity;
import com.example.number_battleground.repository.SubmissionRepository;
import com.example.number_battleground.service.CalcService.SubmissionDTO;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;


@Service
public class CalcService {
    private static final Logger log = LoggerFactory.getLogger(CalcController.class);
    private final SubmissionRepository repo;

    public CalcService(SubmissionRepository repo) {
        this.repo = repo;
    }

    public List<SubmissionDTO> submitExpression(String raw, String nickname) {
        CalcResponse cr = evaluateExpression(raw);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        double errValue = 0.0;
        String errStr = cr.getErrorRate();
        if (errStr != null && errStr.endsWith("%")) {
            errValue = Double.parseDouble(errStr.replace("%","")) / 100.0;
        }
        else{
            return repo.findAllByCreatedDateOrderByPenalty(today)
                       .stream().map(this::toDTO).collect(Collectors.toList());
        }

        Map<String,Integer> opCounts = cr.getOperatorCounts();
        int totalOps           = opCounts.values().stream().mapToInt(i->i).sum();
        double opsPenalty      = totalOps * 10;
        double accuracyPenalty = Math.exp(errValue * 10);
        double totalPenalty    = opsPenalty + accuracyPenalty;

        SubmissionEntity ent = new SubmissionEntity(
            raw, errValue, totalOps, totalPenalty, today, nickname, now
        );
        repo.save(ent);

        return repo.findAllByCreatedDateOrderByPenalty(today)
                   .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private SubmissionDTO toDTO(SubmissionEntity e) {
        String pct = String.format("%.2f%%", e.getErrorRate() * 100);
        String ts  = e.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return new SubmissionDTO(e.getExpression(), pct, e.getPenalty(), e.getNickname(), ts);
    }

    public static String extractBigRawDigits(String symjaOutput) {
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
            if(numberPart.charAt(0)=='-'){
                numberPart = "-0."+sb.toString() + numberPart.substring(1, 8);
            }
            else numberPart = "0."+sb.toString() + numberPart.substring(0, 8);
        }
       
        return numberPart;
    }
    public static String extractSmallRawDigits(String symjaOutput) {
        int start = symjaOutput.indexOf('(');
        int comma = symjaOutput.indexOf(',');
        if (start == -1 || comma == -1) return null;

        String numberPart = symjaOutput.substring(start + 1, comma).trim();
        return numberPart;
    }
    private static final String VALID_REGEX = "^(?:(?:tan)|\\d+|[+\\-*/()])+$";
    public static boolean isValidExpression(String expr) {
        return expr.matches(VALID_REGEX);
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

        Pattern digitSeq = Pattern.compile("\\d+");
        Matcher matcher = digitSeq.matcher(raw);
        while (matcher.find()) {
            String numToken = matcher.group();
            if (numToken.length() > 1) {
                return new CalcResponse(
                    raw, "두 \\,자리 \\,수 \\,입력 \\,금지", "두\\, 자리\\, 수\\, 입력\\, 금지",
                    dailyRandLong, "—", opCounts
                );
            }
        }
        if (raw.contains(".")) {
            return new CalcResponse(
                raw, "소수점 \\,입력 \\,금지" , "소수점 \\,입력 \\,금지",
                dailyRandLong, "—", opCounts
            );
        }
        if (raw.contains(".")) {
            return new CalcResponse(
                raw, "소수점 \\,입력 \\,금지" , "소수점 \\,입력 \\,금지",
                dailyRandLong, "—", opCounts
            );
        }
        if(!isValidExpression(raw)) {
            return new CalcResponse(
                raw, "허용되지 \\,않은 \\,수식 \\,형식", "허용되지 \\,않은 \\,수식 \\,형식",
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
            symbolicTeX  = "오류: 수식 \\,입력 \\,오류";
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

    public static class SubmissionDTO {
        private final String expression;
        private final String errorRate;
        private final double penalty;
        private final String nickname;      // 추가
        private final String submittedAt;   // 추가 (문자열 포맷)
        public SubmissionDTO(String expr, String err, double pen, String nickname, String submittedAt) {
            this.expression = expr;
            this.errorRate  = err;
            this.penalty    = pen;
            this.nickname    = nickname;
            this.submittedAt = submittedAt;
        }
        // getters
        public String getExpression() { return expression; }
        public String getErrorRate()  { return errorRate; }
        public double getPenalty()    { return penalty; }
        public String getNickname()    { return nickname; }
        public String getSubmittedAt(){ return submittedAt; }
    }


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
