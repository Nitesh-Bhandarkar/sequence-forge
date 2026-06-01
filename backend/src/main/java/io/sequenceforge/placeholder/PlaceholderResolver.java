package io.sequenceforge.placeholder;

import io.sequenceforge.template.PlaceholderConfig;
import io.sequenceforge.template.PlaceholderType;

import java.util.Map;

public interface PlaceholderResolver {

    PlaceholderType supportedType();

    String resolve(PlaceholderConfig config, Map<String, String> params);
}
