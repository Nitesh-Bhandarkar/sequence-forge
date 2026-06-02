package io.sequenceforge.ai;

import io.sequenceforge.ai.dto.ChatRequest;
import io.sequenceforge.ai.dto.ClassifyPlaceholderRequest;
import io.sequenceforge.ai.dto.ClassifyPlaceholderResponse;
import io.sequenceforge.common.ApiResponse;
import io.sequenceforge.common.exception.AiDisabledException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;
    private final boolean aiEnabled;

    public AiController(AiService aiService,
                        @Value("${app.ai.enabled:true}") boolean aiEnabled) {
        this.aiService = aiService;
        this.aiEnabled = aiEnabled;
    }

    @PostMapping("/classify-placeholder")
    public ApiResponse<ClassifyPlaceholderResponse> classifyPlaceholder(
            @Valid @RequestBody ClassifyPlaceholderRequest request) {
        if (!aiEnabled) throw new AiDisabledException();
        return ApiResponse.ok(aiService.classifyPlaceholder(request.placeholderName(), request.context()));
    }

    // Returns an SSE stream of text tokens. Frontend reads via fetch + ReadableStream.
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        if (!aiEnabled) throw new AiDisabledException();
        SseEmitter emitter = new SseEmitter(120_000L); // 2 minute timeout
        Thread.ofVirtual().start(() -> aiService.streamChat(request.messages(), emitter));
        return emitter;
    }
}
