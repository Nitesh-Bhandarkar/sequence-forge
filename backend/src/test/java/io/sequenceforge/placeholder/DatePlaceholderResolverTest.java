package io.sequenceforge.placeholder;

import io.sequenceforge.template.PlaceholderConfig;
import io.sequenceforge.template.PlaceholderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatePlaceholderResolverTest {

    private DatePlaceholderResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DatePlaceholderResolver();
    }

    @Test
    void useCallerValueWhenProvided() {
        assertThat(resolver.resolve(config("FY", "FINANCIAL_YEAR"), Map.of("FY", "2324")))
                .isEqualTo("2324");
    }

    // --- FINANCIAL_YEAR ---

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "2026-05-15, 2627",  // May 2026 → FY 2026-27
            "2026-03-31, 2526",  // March 2026 (before April) → FY 2025-26
            "2026-04-01, 2627",  // April 1 = new FY start
            "2025-12-31, 2526"   // Dec 2025 → FY 2025-26
    })
    void financialYear(LocalDate date, String expected) {
        assertThat(resolver.compute(DateFormat.FINANCIAL_YEAR, date)).isEqualTo(expected);
    }

    // --- FINANCIAL_YEAR_FULL ---

    @Test
    void financialYearFull_mayDate() {
        assertThat(resolver.compute(DateFormat.FINANCIAL_YEAR_FULL, LocalDate.of(2026, 5, 15)))
                .isEqualTo("2026-27");
    }

    @Test
    void financialYearFull_marchDate() {
        assertThat(resolver.compute(DateFormat.FINANCIAL_YEAR_FULL, LocalDate.of(2026, 3, 31)))
                .isEqualTo("2025-26");
    }

    // --- FINANCIAL_QUARTER ---

    @ParameterizedTest(name = "month {0} → {1}")
    @CsvSource({"4,FQ1", "6,FQ1", "7,FQ2", "9,FQ2", "10,FQ3", "12,FQ3", "1,FQ4", "3,FQ4"})
    void financialQuarter(int month, String expected) {
        assertThat(resolver.compute(DateFormat.FINANCIAL_QUARTER, LocalDate.of(2026, month, 15)))
                .isEqualTo(expected);
    }

    // --- QUARTER (calendar) ---

    @ParameterizedTest(name = "month {0} → {1}")
    @CsvSource({"1,Q1", "3,Q1", "4,Q2", "6,Q2", "7,Q3", "9,Q3", "10,Q4", "12,Q4"})
    void calendarQuarter(int month, String expected) {
        assertThat(resolver.compute(DateFormat.QUARTER, LocalDate.of(2026, month, 15)))
                .isEqualTo(expected);
    }

    // --- HALF_YEAR ---

    @ParameterizedTest(name = "month {0} → {1}")
    @CsvSource({"1,H1", "6,H1", "7,H2", "12,H2"})
    void halfYear(int month, String expected) {
        assertThat(resolver.compute(DateFormat.HALF_YEAR, LocalDate.of(2026, month, 15)))
                .isEqualTo(expected);
    }

    // --- Standard date formats ---

    @Test
    void year4() {
        assertThat(resolver.compute(DateFormat.YEAR_4, LocalDate.of(2026, 6, 1))).isEqualTo("2026");
    }

    @Test
    void year2() {
        assertThat(resolver.compute(DateFormat.YEAR_2, LocalDate.of(2026, 6, 1))).isEqualTo("26");
    }

    @Test
    void month2_padded() {
        assertThat(resolver.compute(DateFormat.MONTH_2, LocalDate.of(2026, 6, 1))).isEqualTo("06");
    }

    @Test
    void day2_padded() {
        assertThat(resolver.compute(DateFormat.DAY_2, LocalDate.of(2026, 6, 1))).isEqualTo("01");
    }

    @Test
    void yyyyMM() {
        assertThat(resolver.compute(DateFormat.YYYYMM, LocalDate.of(2026, 6, 1))).isEqualTo("202606");
    }

    @Test
    void yyyyMMdd() {
        assertThat(resolver.compute(DateFormat.YYYYMMDD, LocalDate.of(2026, 6, 1))).isEqualTo("20260601");
    }

    @Test
    void weekOfYear_isZeroPadded() {
        String week = resolver.compute(DateFormat.WEEK_OF_YEAR, LocalDate.of(2026, 1, 5));
        assertThat(week).matches("\\d{2}");
    }

    private PlaceholderConfig config(String name, String dateFormat) {
        PlaceholderConfig config = new PlaceholderConfig();
        config.setPlaceholderName(name);
        config.setPlaceholderType(PlaceholderType.DATE);
        config.setDateFormat(dateFormat);
        config.setIsRequired(true);
        return config;
    }
}
