package com.foodmaster.foodmasterbot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class SpoonacularService {
    private static final String API_KEY = "796d7a29558f465b9b7d0082ea6a39a4"; // Замените на свой ключ API
    private static final String BASE_URL_RANDOM = "https://api.spoonacular.com/recipes/random";
    private static final String BASE_URL_SEARCH = "https://api.spoonacular.com/recipes/complexSearch";

    // Получение случайного рецепта
    public String getRandomRecipe() {
        try {
            String urlString = BASE_URL_RANDOM + "?apiKey=" + API_KEY;
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray recipes = jsonResponse.getJSONArray("recipes");

            if (recipes.length() > 0) {
                JSONObject recipe = recipes.getJSONObject(0);
                String title = recipe.getString("title");
                String recipeUrl = recipe.getString("sourceUrl");

                return String.format("🎲 Случайный рецепт:\n\n" +
                        "Название: %s\n" +
                        "Рецепт: %s\n", title, recipeUrl);
            } else {
                return "❌ Не удалось получить случайный рецепт.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Произошла ошибка при получении рецепта.";
        }
    }

    // Получение рецепта по названию блюда
    public String getRecipeByName(String dishName, String category) {
        try {
            String urlString = BASE_URL_SEARCH + "?apiKey=" + API_KEY +
                    "&query=" + dishName + "&type=" + category + "&number=1";

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray results = jsonResponse.getJSONArray("results");

            if (results.length() > 0) {
                JSONObject recipe = results.getJSONObject(0);
                String title = recipe.getString("title");
                String recipeId = String.valueOf(recipe.getInt("id"));
                String recipeUrl = "https://spoonacular.com/recipes/" + title.replace(" ", "-") + "-" + recipeId;

                return String.format("🍽 Рецепт найден:\n\n" +
                        "Название: %s\n" +
                        "Рецепт: %s\n", title, recipeUrl);
            } else {
                return "❌ Рецепт не найден.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Произошла ошибка при поиске рецепта.";
        }
    }

    // Получение рецептов по ингредиентам
    public String getRecipesByIngredients(String ingredients) {
        try {
            // Формируем запрос с ингредиентами
            String urlString = BASE_URL_SEARCH + "?apiKey=" + API_KEY +
                    "&includeIngredients=" + ingredients.replace(" ", ",") +
                    "&number=5"; // Ограничиваем количество рецептов

            // Отправляем запрос и получаем ответ
            JSONArray results = sendApiRequest(urlString).getJSONArray("results");

            // Если рецепты найдены
            if (results.length() > 0) {
                StringBuilder recipesList = new StringBuilder("🍽 Рецепты с вашими ингредиентами:\n\n");

                // Перебираем рецепты
                for (int i = 0; i < results.length(); i++) {
                    JSONObject recipe = results.getJSONObject(i);
                    String title = recipe.getString("title");
                    int recipeId = recipe.getInt("id");
                    String recipeUrl = "https://spoonacular.com/recipes/" + title.replace(" ", "-") + "-" + recipeId;

                    // Добавляем рецепт в список
                    recipesList.append(String.format("• %s\n  🔗 Рецепт: %s\n\n", title, recipeUrl));
                }

                return recipesList.toString();
            } else {
                return "❌ Не найдено рецептов, соответствующих ингредиентам.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Произошла ошибка при получении рецептов.";
        }
    }

    // Получение рецептов по времени приготовления
    public String getRecipesByTime(String time) {
        try {
            // Устанавливаем диапазоны времени для поиска
            String timeRange = "";
            switch (time) {
                case "TIME_15":
                    timeRange = "maxReadyTime=15";  // До 15 минут
                    break;
                case "TIME_15_30":
                    timeRange = "maxReadyTime=30";  // 15-30 минут
                    break;
                case "TIME_30_45":
                    timeRange = "maxReadyTime=45";  // 30-45 минут
                    break;
                case "TIME_45_60":
                    timeRange = "maxReadyTime=60";  // 45-60 минут
                    break;
                case "TIME_60":
                    timeRange = "minReadyTime=60";  // 60 минут и больше
                    break;
                default:
                    return "❌ Время не поддерживается.";
            }

            // Запрос на поиск рецептов
            String searchUrl = BASE_URL_SEARCH + "?apiKey=" + API_KEY + "&" + timeRange + "&number=5";
            JSONArray results = sendApiRequest(searchUrl).getJSONArray("results");

            // Если рецепты найдены
            if (results.length() > 0) {
                StringBuilder recipesList = new StringBuilder("🍽 Рецепты, подходящие по времени:\n\n");

                // Перебираем рецепты и фильтруем вручную
                for (int i = 0; i < results.length(); i++) {
                    JSONObject recipe = results.getJSONObject(i);
                    String title = recipe.getString("title");
                    int recipeId = recipe.getInt("id");
                    String recipeUrl = "https://spoonacular.com/recipes/" + title.replace(" ", "-") + "-" + recipeId;

                    // Получаем информацию о рецепте (включая время)
                    int readyInMinutes = getRecipeTime(recipeId);

                    // Фильтрация вручную по диапазону времени
                    if (!isRecipeWithinTimeRange(readyInMinutes, time)) {
                        continue;  // Пропустить рецепт, если он не соответствует диапазону времени
                    }

                    String timeText = (readyInMinutes != -1) ? readyInMinutes + " мин" : "⏳ Время неизвестно";

                    recipesList.append(String.format("• %s (%s)\n  🔗 Рецепт: %s\n\n", title, timeText, recipeUrl));
                }

                return recipesList.length() > 0 ? recipesList.toString() : "❌ Не найдено рецептов, соответствующих времени.";
            } else {
                return "❌ Не найдено рецептов, соответствующих времени.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Произошла ошибка при получении рецептов.";
        }
    }

    // Метод для фильтрации рецептов по времени
    private boolean isRecipeWithinTimeRange(int readyInMinutes, String timeRange) {
        switch (timeRange) {
            case "TIME_15":
                return readyInMinutes <= 15;
            case "TIME_15_30":
                return readyInMinutes > 15 && readyInMinutes <= 30;
            case "TIME_30_45":
                return readyInMinutes > 30 && readyInMinutes <= 45;
            case "TIME_45_60":
                return readyInMinutes > 45 && readyInMinutes <= 60;
            case "TIME_60":
                return readyInMinutes >= 60;
            default:
                return false;
        }
    }

    // Метод для получения информации о рецепте
    private int getRecipeTime(int recipeId) {
        try {
            String infoUrl = "https://api.spoonacular.com/recipes/" + recipeId + "/information?apiKey=" + API_KEY;
            JSONObject recipeInfo = sendApiRequest(infoUrl);

            // Проверяем наличие readyInMinutes
            return recipeInfo.has("readyInMinutes") ? recipeInfo.getInt("readyInMinutes") : -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;  // Ошибка или время неизвестно
        }
    }

    // Метод для выполнения API-запроса и получения JSON-ответа
    private JSONObject sendApiRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return new JSONObject(response.toString());
    }
}
