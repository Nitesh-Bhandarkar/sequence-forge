package io.sequenceforge.template;

import io.sequenceforge.common.ApiResponse;
import io.sequenceforge.template.dto.CreateTemplateRequest;
import io.sequenceforge.template.dto.TemplateResponse;
import io.sequenceforge.template.dto.UpdateTemplateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TemplateResponse> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.ok(templateService.createTemplate(request));
    }

    @GetMapping
    public ApiResponse<List<TemplateResponse>> listTemplates() {
        return ApiResponse.ok(templateService.listTemplates());
    }

    @PutMapping("/{id}")
    public ApiResponse<TemplateResponse> updateTemplate(@PathVariable UUID id,
                                                         @RequestBody UpdateTemplateRequest request) {
        return ApiResponse.ok(templateService.updateTemplate(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<TemplateResponse> getTemplate(@PathVariable UUID id) {
        return ApiResponse.ok(templateService.getTemplate(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
    }
}
