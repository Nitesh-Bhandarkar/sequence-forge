package io.sequenceforge.placeholder;

import io.sequenceforge.template.PlaceholderType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ResolverRegistry {

    private final Map<PlaceholderType, PlaceholderResolver> resolvers;

    public ResolverRegistry(List<PlaceholderResolver> resolverList) {
        resolvers = resolverList.stream()
                .collect(Collectors.toMap(PlaceholderResolver::supportedType, Function.identity()));
    }

    public PlaceholderResolver get(PlaceholderType type) {
        PlaceholderResolver resolver = resolvers.get(type);
        if (resolver == null) {
            throw new IllegalStateException("No resolver registered for type: " + type);
        }
        return resolver;
    }
}
