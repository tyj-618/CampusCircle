package com.tyj.campuscircle.ai;

public record AiModelRequest(
        String systemPrompt,
        String userPrompt
) {
}
