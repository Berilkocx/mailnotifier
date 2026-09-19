package com.beril.mailnotifier.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private AiRateLimiter rateLimiter;

    private static final ExpectationContext CTX =
            new ExpectationContext("Yazılım projesi teklifi bekliyorum", "firma.com", List.of("teklif", "proje"));

    private MockRestServiceServer mockServer;
    private OpenAiService openAiService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer test-key");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        openAiService = new OpenAiService(builder.build(), new ObjectMapper(), rateLimiter);
    }

    @Test
    void analyzeEmail_shouldParseResponseCorrectly() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        String responseJson = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"intent\\":\\"TEKLIF\\",\\"topics\\":[\\"yazılım\\",\\"proje\\"],\\"summary\\":\\"Yazılım projesi teklifi\\",\\"confidence\\":0.9}"
                    }
                  }]
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiAnalysisResult result = openAiService.analyzeEmail("test@example.com", "Teklif", "İçerik", CTX);

        assertThat(result.intent()).isEqualTo("TEKLIF");
        assertThat(result.topics()).containsExactly("yazılım", "proje");
        assertThat(result.summary()).isEqualTo("Yazılım projesi teklifi");
        assertThat(result.confidence()).isEqualTo(0.9);
        mockServer.verify();
    }

    @Test
    void analyzeEmail_shouldParseRelevanceAndReason() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        String responseJson = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"relevance\\":0.85,\\"reason\\":\\"Beklenen teklif geldi\\",\\"intent\\":\\"TEKLIF\\",\\"topics\\":[\\"yazılım\\"],\\"summary\\":\\"Teklif 15 Ekim'e kadar geçerli\\",\\"confidence\\":0.9}"
                    }
                  }]
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiAnalysisResult result = openAiService.analyzeEmail("test@example.com", "Teklif", "İçerik", CTX);

        assertThat(result.hasRelevance()).isTrue();
        assertThat(result.relevance()).isEqualTo(0.85);
        assertThat(result.reason()).isEqualTo("Beklenen teklif geldi");
        assertThat(result.summary()).contains("15 Ekim");
    }

    @Test
    void analyzeEmail_whenRelevanceMissing_relevanceIsUnknown() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        String responseJson = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"intent\\":\\"GENEL\\",\\"topics\\":[\\"x\\"],\\"summary\\":\\"Özet\\",\\"confidence\\":0.5}"
                    }
                  }]
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiAnalysisResult result = openAiService.analyzeEmail("test@example.com", "Konu", "İçerik", CTX);

        assertThat(result.hasRelevance()).isFalse();
    }

    @Test
    void analyzeEmail_sendsUserExpectationToModel() {
        when(rateLimiter.tryAcquire()).thenReturn(true);

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Yazılım projesi teklifi bekliyorum")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("firma.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("teklif, proje")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("json_object")))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}", MediaType.APPLICATION_JSON));

        openAiService.analyzeEmail("test@example.com", "Konu", "İçerik", CTX);

        mockServer.verify();
    }

    @Test
    void analyzeEmail_whenRateLimitExceeded_returnsEmpty() {
        when(rateLimiter.tryAcquire()).thenReturn(false);

        AiAnalysisResult result = openAiService.analyzeEmail("test@example.com", "Konu", "İçerik", CTX);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void analyzeEmail_whenInvalidJson_returnsEmpty() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        String badResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "bu geçerli JSON değil {{"
                    }
                  }]
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
                .andRespond(withSuccess(badResponse, MediaType.APPLICATION_JSON));

        AiAnalysisResult result = openAiService.analyzeEmail("test@example.com", "Konu", "İçerik", CTX);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void analyzeEmail_whenApiReturnsServerError_returnsEmpty() {
        when(rateLimiter.tryAcquire()).thenReturn(true);

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
                .andRespond(withServerError());

        AiAnalysisResult result = openAiService.analyzeEmail("test@example.com", "Konu", "İçerik", CTX);

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void analyzeEmail_withSnippetExceeding1000Chars_shouldStillSucceed() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        String longSnippet = "x".repeat(2000);
        String responseJson = """
                {
                  "choices": [{
                    "message": {
                      "content": "{\\"intent\\":\\"GENEL\\",\\"topics\\":[],\\"summary\\":\\"Özet\\",\\"confidence\\":0.5}"
                    }
                  }]
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiAnalysisResult result = openAiService.analyzeEmail("test@example.com", "Konu", longSnippet, CTX);

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
        mockServer.verify();
    }

    @Test
    void suggestKeywords_shouldReturnParsedList() {
        when(rateLimiter.tryAcquire()).thenReturn(true);
        String responseJson = """
                {
                  "choices": [{
                    "message": {
                      "content": "[\\"teklif\\",\\"yazılım\\",\\"proje\\",\\"fiyat\\"]"
                    }
                  }]
                }
                """;

        mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/chat/completions")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<String> keywords = openAiService.suggestKeywords("Yazılım projesi teklifi bekliyorum");

        assertThat(keywords).containsExactly("teklif", "yazılım", "proje", "fiyat");
    }

    @Test
    void suggestKeywords_whenDescriptionBlank_returnsEmptyList() {
        List<String> keywords = openAiService.suggestKeywords("   ");

        assertThat(keywords).isEmpty();
    }

    @Test
    void noOpAiService_alwaysReturnsEmptyAndUnavailable() {
        AiService noop = new NoOpAiService();

        assertThat(noop.analyzeEmail("a@b.com", "Konu", "İçerik", CTX).isEmpty()).isTrue();
        assertThat(noop.suggestKeywords("bir şey")).isEmpty();
        assertThat(noop.isAvailable()).isFalse();
    }
}
