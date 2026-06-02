package io.sequenceforge.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    // Use ANTHROPIC_API_KEY env var or explicit config value
    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Bean
    public AnthropicClient anthropicClient() {
        if (apiKey != null && !apiKey.isBlank()) {
            return AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        }
        return AnthropicOkHttpClient.fromEnv();
    }
}
