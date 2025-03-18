package com.foodmaster.foodmasterbot;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAIService {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final String openAiApiKey = ""; // Укажи API-ключ

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> analyzeImageAndCalculateNutrition(String imageUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4-turbo");
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "Ты — эксперт по питанию. Определи, какие блюда есть на фото, и рассчитай точное КБЖУ. "),
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", "Какие блюда на фото? Укажи их КБЖУ."),
                        Map.of("type", "image_url", "image_url", Map.of("url", imageUrl))
                ))
        ));
        requestBody.put("max_tokens", 600);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.POST, requestEntity, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (!choices.isEmpty()) {
                String textResponse = choices.get(0).get("message").toString();
                return parseNutritionResponse(textResponse);
            }
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> parseNutritionResponse(String response) {
        Map<String, Object> nutritionInfo = new HashMap<>();

        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    nutritionInfo.put(key, value);
                }
            }
        }
        return nutritionInfo;
    }
}
