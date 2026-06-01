package io.sequenceforge.placeholder;

import io.sequenceforge.template.PlaceholderConfig;
import io.sequenceforge.template.PlaceholderType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
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
        return compute(DateFormat.valueOf(config.getDateFormat()), LocalDate.now());
    }

    String compute(DateFormat format, LocalDate date) {
        return switch (format) {
            case FINANCIAL_YEAR -> financialYear(date);
            case FINANCIAL_YEAR_FULL -> financialYearFull(date);
            case FINANCIAL_QUARTER -> financialQuarter(date);
            case YEAR_4 -> String.valueOf(date.getYear());
            case YEAR_2 -> String.format("%02d", date.getYear() % 100);
            case MONTH_2 -> String.format("%02d", date.getMonthValue());
            case DAY_2 -> String.format("%02d", date.getDayOfMonth());
            case QUARTER -> "Q" + ((date.getMonthValue() - 1) / 3 + 1);
            case HALF_YEAR -> date.getMonthValue() <= 6 ? "H1" : "H2";
            case WEEK_OF_YEAR -> String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case YYYYMM -> String.format("%d%02d", date.getYear(), date.getMonthValue());
            case YYYYMMDD -> String.format("%d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        };
    }

    // Financial year: April–March. June 2026 → FY 2026-27 → startYear=2026, endYear=2027
    private int financialYearStart(LocalDate date) {
        return date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
    }

    private String financialYear(LocalDate date) {
        int start = financialYearStart(date);
        return String.format("%02d%02d", start % 100, (start + 1) % 100);
    }

    private String financialYearFull(LocalDate date) {
        int start = financialYearStart(date);
        return start + "-" + String.format("%02d", (start + 1) % 100);
    }

    private String financialQuarter(LocalDate date) {
        int month = date.getMonthValue();
        // Apr=FQ1, Jul=FQ2, Oct=FQ3, Jan=FQ4
        int quarter = switch (month) {
            case 4, 5, 6 -> 1;
            case 7, 8, 9 -> 2;
            case 10, 11, 12 -> 3;
            default -> 4; // Jan, Feb, Mar
        };
        return "FQ" + quarter;
    }
}
