package com.foodmaster.foodmasterbot;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class SpoonacularService {
    private static final String API_URL = "https://api.spoonacular.com/recipes/findByIngredients";
    private static final String BASE_URL = "https://api.spoonacular.com/recipes/"; // Define BASE_URL here
    private static final String API_KEY = "796d7a29558f465b9b7d0082ea6a39a4";

    private final RestTemplate restTemplate;

    public SpoonacularService() {
        this.restTemplate = new RestTemplate();
    }

    public List<Map<String, Object>> findRecipesByIngredients(List<String> ingredients) {
        String ingredientsParam = String.join(",", ingredients);

        String url = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("ingredients", ingredientsParam)
                .queryParam("number", 5)  // Сколько рецептов вернуть
                .queryParam("apiKey", API_KEY)
                .toUriString();

        return restTemplate.getForObject(url, List.class);
    }

    public Map<String, Object> getRecipeById(int recipeId) {
        String url = BASE_URL + recipeId + "/information?apiKey=" + API_KEY;
        return restTemplate.getForObject(url, Map.class);
    }
}
