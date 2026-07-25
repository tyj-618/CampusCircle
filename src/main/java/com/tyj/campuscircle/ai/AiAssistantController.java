package com.tyj.campuscircle.ai;

import com.tyj.campuscircle.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/assistant")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/ask")
    public ApiResponse<AiAssistantResponse> ask(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AiAssistantRequest request) {
        return ApiResponse.success(aiAssistantService.ask(authorization, request));
    }
}
