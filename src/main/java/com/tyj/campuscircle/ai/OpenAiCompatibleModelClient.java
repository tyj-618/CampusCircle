package com.tyj.campuscircle.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyj.campuscircle.common.ErrorCode;
import com.tyj.campuscircle.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "campuscircle.ai", name = "provider", havingValue = "openai-compatible")
public class OpenAiCompatibleModelClient implements AiModelClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleModelClient.class);

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatibleModelClient(AiProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        String baseUrl = properties.getBaseUrl();
        this.restClient = RestClient.builder()
                .baseUrl(isBlank(baseUrl) ? "http://localhost" : baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AiModelResult generate(AiModelRequest request) {
        ensureConfigured();
        long startedAt = System.currentTimeMillis();
        for (int attempt = 0; attempt <= properties.getMaxRetries(); attempt++) {
            try {
                String responseBody = restClient.post()
                        .uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(buildRequestBody(request))
                        .retrieve()
                        .body(String.class);

                AiModelResult result = parseResponse(parseProviderResponse(responseBody));
                log.info("AI call succeeded: requestId={}, model={}, elapsedMs={}, inputTokens={}, outputTokens={}",
                        result.requestId(), properties.getModel(), System.currentTimeMillis() - startedAt,
                        result.inputTokens(), result.outputTokens());
                return result;
            } catch (RestClientResponseException exception) {
                if (isRetryable(exception) && attempt < properties.getMaxRetries()) {
                    backoff(attempt);
                    continue;
                }
                log.warn("AI call failed: model={}, status={}, elapsedMs={}", properties.getModel(),
                        exception.getStatusCode().value(), System.currentTimeMillis() - startedAt);
                throw unavailable();
            } catch (ResourceAccessException exception) {
                if (attempt < properties.getMaxRetries()) {
                    backoff(attempt);
                    continue;
                }
                log.warn("AI call timed out or could not connect: model={}, elapsedMs={}", properties.getModel(),
                        System.currentTimeMillis() - startedAt);
                throw unavailable();
            }
        }
        throw unavailable();
    }

    Map<String, Object> buildRequestBody(AiModelRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", request.systemPrompt()),
                Map.of("role", "user", "content", request.userPrompt())
        ));
        body.put("temperature", 0.2);
        if (properties.isStructuredOutput()) {
            body.put("response_format", Map.of("type", "json_object"));
        } else {
            body.put("max_tokens", properties.getMaxOutputTokens());
        }
        if (properties.getEnableThinking() != null) {
            body.put("enable_thinking", properties.getEnableThinking());
        }
        return body;
    }

    private ProviderResponse parseProviderResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, ProviderResponse.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模型服务返回格式异常");
        }
    }

    private AiModelResult parseResponse(ProviderResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模型服务返回格式异常");
        }
        String content = stripMarkdownFence(response.choices().get(0).message().content());
        try {
            ModelAnswerPayload payload = objectMapper.readValue(content, ModelAnswerPayload.class);
            if (payload.answer() == null || payload.answer().isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模型服务未返回有效答案");
            }
            return new AiModelResult(
                    payload.answer().trim(),
                    payload.citedPostIds() == null ? List.of() : payload.citedPostIds(),
                    Boolean.TRUE.equals(payload.insufficientEvidence()),
                    response.id() == null || response.id().isBlank() ? UUID.randomUUID().toString() : response.id(),
                    response.usage() == null ? null : response.usage().inputTokens(),
                    response.usage() == null ? null : response.usage().outputTokens()
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模型服务返回格式异常");
        }
    }

    private void ensureConfigured() {
        if (isBlank(properties.getBaseUrl()) || isBlank(properties.getApiKey()) || isBlank(properties.getModel())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模型服务未完成配置");
        }
    }

    private boolean isRetryable(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        return status == 429 || status >= 500;
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(200L * (attempt + 1));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable();
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, "智能问答服务暂不可用，请稍后再试");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String stripMarkdownFence(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int closingFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, closingFence).trim();
    }

    private record ProviderMessage(String role, String content) {
    }

    private record ProviderResponse(String id, List<ProviderChoice> choices, ProviderUsage usage) {
    }

    private record ProviderChoice(ProviderMessage message) {
    }

    private record ProviderUsage(
            @JsonProperty("prompt_tokens") Integer inputTokens,
            @JsonProperty("completion_tokens") Integer outputTokens
    ) {
    }

    private record ModelAnswerPayload(String answer, List<Long> citedPostIds, Boolean insufficientEvidence) {
    }
}
