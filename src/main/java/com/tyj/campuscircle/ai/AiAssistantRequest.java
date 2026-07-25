package com.tyj.campuscircle.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAssistantRequest(
        @NotBlank @Size(max = 200) String question,
        @Min(1) @Max(50) Double radiusKm
) {
}
