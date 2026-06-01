package io.sequenceforge.placeholder;

public enum DateFormat {
    FINANCIAL_YEAR,        // "2627"        — FY 2026-27 (Apr–Mar)
    FINANCIAL_YEAR_FULL,   // "2026-27"     — FY 2026-27 long form
    FINANCIAL_QUARTER,     // "FQ1"–"FQ4"   — Apr=FQ1, Jul=FQ2, Oct=FQ3, Jan=FQ4
    YEAR_4,                // "2026"
    YEAR_2,                // "26"
    MONTH_2,               // "06"
    DAY_2,                 // "01"
    QUARTER,               // "Q1"–"Q4"     — Jan=Q1, Apr=Q2, Jul=Q3, Oct=Q4
    HALF_YEAR,             // "H1" or "H2"  — Jan–Jun=H1, Jul–Dec=H2
    WEEK_OF_YEAR,          // "01"–"53"     — ISO week number
    YYYYMM,                // "202606"
    YYYYMMDD               // "20260601"
}
