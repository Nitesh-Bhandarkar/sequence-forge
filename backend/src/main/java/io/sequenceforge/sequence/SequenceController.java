package io.sequenceforge.sequence;

import io.sequenceforge.common.ApiResponse;
import io.sequenceforge.sequence.dto.CounterStatusResponse;
import io.sequenceforge.sequence.dto.GenerateSequenceRequest;
import io.sequenceforge.sequence.dto.GenerateSequenceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sequences")
public class SequenceController {

    private final SequenceGeneratorService sequenceGeneratorService;

    public SequenceController(SequenceGeneratorService sequenceGeneratorService) {
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @PostMapping("/generate")
    public ApiResponse<GenerateSequenceResponse> generate(@Valid @RequestBody GenerateSequenceRequest request) {
        return ApiResponse.ok(sequenceGeneratorService.generate(request));
    }

    // Returns current counter value without incrementing — useful for monitoring.
    @PostMapping("/counter")
    public ApiResponse<CounterStatusResponse> peekCounter(
            @RequestParam UUID templateId,
            @RequestBody(required = false) Map<String, String> params) {
        return ApiResponse.ok(sequenceGeneratorService.peekCounter(templateId,
                params != null ? params : Map.of()));
    }
}
