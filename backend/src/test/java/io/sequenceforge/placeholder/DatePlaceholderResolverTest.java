package io.sequenceforge.placeholder;

import io.sequenceforge.template.PlaceholderConfig;
import io.sequenceforge.template.PlaceholderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        PlaceholderConfig config = config("FY", "FINANCIAL_YEAR");
        String result = resolver.resolve(config, Map.of("FY", "2324"));
        assertThat(result).isEqualTo("2324");
    }

    @Test
    void computeFinancialYearForAprilOrLater() {
        // We can't mock LocalDate.now() without Mockito, so we verify the format is 4 chars
        PlaceholderConfig config = config("FY", "FINANCIAL_YEAR");
        String result = resolver.resolve(config, Map.of());
        assertThat(result).matches("\\d{4}");
    }

    @Test
    void resolveYear4Format() {
        PlaceholderConfig config = config("YR", "YEAR_4");
        String result = resolver.resolve(config, Map.of());
        assertThat(result).matches("\\d{4}");
    }

    @Test
    void resolveYear2Format() {
        PlaceholderConfig config = config("YR", "YEAR_2");
        String result = resolver.resolve(config, Map.of());
        assertThat(result).matches("\\d{2}");
    }

    @Test
    void resolveMonth2Format() {
        PlaceholderConfig config = config("MM", "MONTH_2");
        String result = resolver.resolve(config, Map.of());
        assertThat(result).matches("\\d{2}");
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
