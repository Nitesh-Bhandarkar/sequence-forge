package io.sequenceforge.placeholder;

import io.sequenceforge.common.exception.PlaceholderValueMissingException;
import io.sequenceforge.template.PlaceholderConfig;
import io.sequenceforge.template.PlaceholderType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StaticPlaceholderResolver implements PlaceholderResolver {

    @Override
    public PlaceholderType supportedType() {
        return PlaceholderType.STATIC;
    }

    @Override
    public String resolve(PlaceholderConfig config, Map<String, String> params) {
        String value = params.get(config.getPlaceholderName());
        if (value == null && config.getIsRequired()) {
            throw new PlaceholderValueMissingException(config.getPlaceholderName());
        }
        return value != null ? value : "";
    }
}
