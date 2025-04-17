package com.foodmaster.foodmasterbot.states;

import com.foodmaster.foodmasterbot.model.UserData;
import com.foodmaster.foodmasterbot.service.SpoonacularService;
import com.foodmaster.foodmasterbot.utils.MessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserStateManager {
    @Autowired
    @Lazy
    private MessageUtils messageUtils;
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, UserData> userDataMap = new HashMap<>();

    @Autowired
    private SpoonacularService spoonacularService;

    public void setUserState(long chatId, String state) {
        userStates.put(chatId, state);
    }

    public String getUserState(long chatId) {
        return userStates.get(chatId);
    }

    public void handleUserInput(long chatId, String userMessage) {
        String state = getUserState(chatId);

        if (state != null && state.equals("AWAITING_INGREDIENTS")) {
            if (!userMessage.isEmpty()) {
                String recipes = spoonacularService.getRecipesByIngredients(userMessage);
                if (recipes != null && !recipes.isEmpty()) {
                    messageUtils.sendMessage(chatId, "Вот рецепты с вашими ингредиентами:\n" + recipes);
                } else {
                    messageUtils.sendMessage(chatId, "Извините, не удалось найти рецепты с такими ингредиентами.");
                }
            } else {
                messageUtils.sendMessage(chatId, "❌ Пожалуйста, введите хотя бы один ингредиент.");
            }
            userStates.remove(chatId);
            return;
        }
        if (state != null && state.equals("AWAITING_EXCLUDED_INGREDIENTS")) {
            if (!userMessage.isEmpty()) {
                String recipes = spoonacularService.getRecipesExcludingIngredients(userMessage);
                if (recipes != null && !recipes.isEmpty()) {
                    messageUtils.sendMessage(chatId, "Вот рецепты без этих ингредиентов:\n" + recipes);
                } else {
                    messageUtils.sendMessage(chatId, "Извините, не удалось найти рецепты без этих ингредиентов.");
                }
            } else {
                messageUtils.sendMessage(chatId, "❌ Пожалуйста, введите хотя бы один ингредиент.");
            }
            userStates.remove(chatId);
            return;
        }
        if (state != null && state.startsWith("AWAITING_DISH_NAME:")) {
            String category = state.split(":")[1];
            String recipe = spoonacularService.getRecipeByName(userMessage, category);
            if (recipe != null && !recipe.isEmpty()) {
                messageUtils.sendDishResult(chatId, recipe);
            } else {
                messageUtils.sendMessage(chatId, "❌ Блюдо не найдено. Попробуйте снова.");
            }
            userStates.remove(chatId);
            return;
        }
        if (state != null && state.equals("AWAITING_CALORIES")) {
            try {
                int calories = Integer.parseInt(userMessage);
                if (calories > 0) {
                    messageUtils.searchRecipeByCalories(chatId, calories);
                } else {
                    messageUtils.sendMessage(chatId, "❌ Пожалуйста, укажите положительное число.");
                }
            } catch (NumberFormatException e) {
                messageUtils.sendMessage(chatId, "❌ Введите корректное число.");
            }
            userStates.remove(chatId);
            return;
        }
        try {
            int value = Integer.parseInt(userMessage);
            UserData data = userDataMap.getOrDefault(chatId, new UserData());

            switch (state) {
                case "AWAITING_HEIGHT":
                    if (value >= 50 && value <= 250) {
                        data.setHeight(value);
                        userDataMap.put(chatId, data);
                        askForWeight(chatId);
                    } else {
                        messageUtils.sendMessage(chatId, "❌ Рост должен быть в диапазоне 50-250 см.");
                    }
                    break;
                case "AWAITING_WEIGHT":
                    if (value >= 20 && value <= 300) {
                        data.setWeight(value);
                        userDataMap.put(chatId, data);
                        askForAge(chatId);
                    } else {
                        messageUtils.sendMessage(chatId, "❌ Вес должен быть в диапазоне 20-300 кг.");
                    }
                    break;
                case "AWAITING_AGE":
                    if (value >= 5 && value <= 120) {
                        data.setAge(value);
                        userDataMap.put(chatId, data);
                        askForGender(chatId);
                    } else {
                        messageUtils.sendMessage(chatId, "❌ Возраст должен быть в диапазоне 5-120 лет.");
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            messageUtils.sendMessage(chatId, "❌ Введите корректное число.");
        }
    }

    public void saveGender(long chatId, String gender) {
        UserData data = userDataMap.get(chatId);
        if (data == null) return;
        data.setGender(gender.equals("GENDER_MALE") ? "Мужской" : "Женский");
        userDataMap.put(chatId, data);
        askForActivityLevel(chatId);
    }

    public void saveActivityLevel(long chatId, String activity) {
        UserData data = userDataMap.get(chatId);
        if (data == null) return;
        switch (activity) {
            case "ACTIVITY_LOW":
                data.setActivityLevel("🥉 Низкая активность");
                break;
            case "ACTIVITY_MEDIUM":
                data.setActivityLevel("🥈 Средняя активность");
                break;
            case "ACTIVITY_HIGH":
                data.setActivityLevel("🥇 Высокая активность");
                break;
        }
        userDataMap.put(chatId, data);
        messageUtils.sendFinalMessage(chatId, data);
    }

    public void calculateKBZU(long chatId) {
        UserData data = userDataMap.get(chatId);
        if (data == null) {
            messageUtils.sendMessage(chatId, "❌ Пожалуйста, заполните все данные.");
            return;
        }
        messageUtils.sendKBZUResult(chatId, data);
    }

    private void askForWeight(long chatId) {
        userStates.put(chatId, "AWAITING_WEIGHT");
        messageUtils.sendMessage(chatId, "⚖️ Укажите ваш вес в килограммах (например, 70).");
    }

    private void askForAge(long chatId) {
        userStates.put(chatId, "AWAITING_AGE");
        messageUtils.sendMessage(chatId, "🎂 Укажите ваш возраст (например, 25).");
    }

    private void askForGender(long chatId) {
        userStates.put(chatId, "AWAITING_GENDER");
        messageUtils.sendGenderSelection(chatId);
    }

    private void askForActivityLevel(long chatId) {
        userStates.put(chatId, "AWAITING_ACTIVITY_LEVEL");
        messageUtils.sendActivityLevelSelection(chatId);
    }

    public void askForHeight(long chatId) {
        userStates.put(chatId, "AWAITING_HEIGHT");
        messageUtils.sendMessage(chatId, "📍 Укажите ваш рост в сантиметрах (например, 175).");
    }
}