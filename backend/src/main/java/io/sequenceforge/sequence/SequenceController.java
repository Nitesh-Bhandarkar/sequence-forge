package io.sequenceforge.sequence;

import io.sequenceforge.common.ApiResponse;
import io.sequenceforge.sequence.dto.GenerateSequenceRequest;
import io.sequenceforge.sequence.dto.GenerateSequenceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
}
