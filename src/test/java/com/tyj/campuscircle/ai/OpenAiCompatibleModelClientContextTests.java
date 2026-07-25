package com.tyj.campuscircle.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = "campuscircle.ai.provider=openai-compatible")
class OpenAiCompatibleModelClientContextTests {

    @Autowired
    private AiModelClient aiModelClient;

    @Test
    void loadsOpenAiCompatibleClientWithoutSendingExternalRequests() {
        assertThat(aiModelClient).isInstanceOf(OpenAiCompatibleModelClient.class);
    }

    @Test
    void usesJsonResponseFormatWithoutMaxTokensWhenStructuredOutputIsEnabled() {
        AiProperties properties = new AiProperties();
        properties.setModel("test-model");
        properties.setStructuredOutput(true);
        properties.setEnableThinking(false);
        OpenAiCompatibleModelClient client = new OpenAiCompatibleModelClient(properties);

        Map<String, Object> requestBody = client.buildRequestBody(new AiModelRequest("system", "user"));

        assertThat(requestBody).containsEntry("response_format", Map.of("type", "json_object"));
        assertThat(requestBody).containsEntry("enable_thinking", false);
        assertThat(requestBody).doesNotContainKey("max_tokens");
    }
}
