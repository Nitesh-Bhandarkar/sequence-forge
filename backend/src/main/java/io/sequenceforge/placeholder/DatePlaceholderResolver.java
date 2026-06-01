package io.sequenceforge.placeholder;

import io.sequenceforge.template.PlaceholderConfig;
import io.sequenceforge.template.PlaceholderType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
public class DatePlaceholderResolver implements PlaceholderResolver {

    @Override
    public PlaceholderType supportedType() {
        return PlaceholderType.DATE;
    }

    @Override
    public String resolve(PlaceholderConfig config, Map<String, String> params) {
        String callerValue = params.get(config.getPlaceholderName());
        if (callerValue != null) {
            return callerValue;
        }
        return computeFromCurrentDate(config.getDateFormat());
    }

    private String computeFromCurrentDate(String dateFormat) {
        LocalDate today = LocalDate.now();
        return switch (dateFormat) {
            case "FINANCIAL_YEAR" -> resolveFinancialYear(today);
            case "YEAR_4" -> String.valueOf(today.getYear());
            case "YEAR_2" -> String.format("%02d", today.getYear() % 100);
            case "MONTH_2" -> String.format("%02d", today.getMonthValue());
            case "DAY_2" -> String.format("%02d", today.getDayOfMonth());
            case "YYYYMM" -> String.format("%d%02d", today.getYear(), today.getMonthValue());
            case "YYYYMMDD" -> String.format("%d%02d%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
            default -> throw new IllegalArgumentException("Unknown dateFormat: " + dateFormat);
        };
    }

    // Financial year: April–March. May 2026 → FY 2026-27 → "2627"
    private String resolveFinancialYear(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        int endYear = startYear + 1;
        return String.format("%02d%02d", startYear % 100, endYear % 100);
    }
}
