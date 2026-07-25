package com.tyj.campuscircle.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "campuscircle.ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAiModelClient implements AiModelClient {

    private static final Pattern POST_ID_PATTERN = Pattern.compile("postId: (\\d+)");

    @Override
    public AiModelResult generate(AiModelRequest request) {
        Matcher matcher = POST_ID_PATTERN.matcher(request.userPrompt());
        List<Long> postIds = matcher.results()
                .map(match -> Long.parseLong(match.group(1)))
                .limit(3)
                .toList();
        return new AiModelResult(
                "已找到相关校园帖子，请查看下方引用内容以了解详情。",
                postIds,
                postIds.isEmpty(),
                UUID.randomUUID().toString(),
                0,
                0
        );
    }
}
