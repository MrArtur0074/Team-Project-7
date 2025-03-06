package com.foodmaster.foodmasterbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FoodMasterBot extends TelegramLongPollingBot {

    private static final Map<String, Recipe> RECIPES = new HashMap<>();

    static {
        RECIPES.put("Омлет", new Recipe("Омлет", List.of("яйца", "молоко", "соль"), "1. Взбейте яйца с молоком\n2. Добавьте соль\n3. Жарьте на сковороде"));
        RECIPES.put("Паста", new Recipe("Паста", List.of("макароны", "помидоры", "сыр"), "1. Отварите макароны\n2. Сделайте томатный соус\n3. Посыпьте сыром"));
        RECIPES.put("Салат", new Recipe("Салат", List.of("огурцы", "помидоры", "масло"), "1. Нарежьте овощи\n2. Заправьте маслом"));
    }

    @Override
    public String getBotUsername() {
        return "FoodMaster_MealMaster_bot";
    }

    @Override
    public String getBotToken() {
        return "7881995906:AAEXCt-6Xk3mB-Pf11hcrNiHWCfefXkyu2I";  // Вставь свой токен
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String userMessage = update.getMessage().getText().toLowerCase();

            if (userMessage.equals("/start")) {
                sendStartMenu(chatId);
            } else {
                findRecipesByIngredients(chatId, userMessage);
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    // Отправляет главное меню при /start
    private void sendStartMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👋 Добро пожаловать в *FoodMaster*!\nВыберите, как вы хотите найти рецепт:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(Collections.singletonList(createButton("🔍 Найти рецепт по ингредиентам", "FIND_BY_INGREDIENTS")));
        keyboard.add(Collections.singletonList(createButton("📖 Перейти в раздел рецептов", "SHOW_ALL_RECIPES")));

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
        message.enableMarkdown(true);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Обрабатывает нажатие кнопок
    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        if (callbackData.equals("SHOW_ALL_RECIPES")) {
            showAllRecipes(chatId, messageId);
        } else if (callbackData.equals("FIND_BY_INGREDIENTS")) {
            deleteMessage(chatId, messageId);
            sendFindIngredientsPrompt(chatId);  // Новый метод для запроса ингредиентов
        } else if (callbackData.equals("BACK_TO_MAIN_MENU")) {
            deleteMessage(chatId, messageId); // Удаляем старое сообщение
            sendStartMenu(chatId); // Отправляем главное меню
        } else if (callbackData.startsWith("RECIPE_")) {
            String recipeName = callbackData.replace("RECIPE_", "");
            sendRecipe(chatId, messageId, recipeName);
        }
    }

    // Метод для удаления сообщения
    private void deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(String.valueOf(chatId));  // Преобразуем chatId в String
        deleteMessage.setMessageId(messageId);            // messageId уже типа Integer

        try {
            execute(deleteMessage);  // Выполните метод для удаления сообщения
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Показывает все доступные рецепты
    private void showAllRecipes(long chatId, int messageId) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(messageId);

        StringBuilder responseText = new StringBuilder("📖 *Доступные рецепты:*\n\n");
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (String recipeName : RECIPES.keySet()) {
            responseText.append("🍽 ").append(recipeName).append("\n");
            keyboard.add(Collections.singletonList(createButton(recipeName, "RECIPE_" + recipeName)));
        }

        // Добавляем кнопку "Назад"
        keyboard.add(Collections.singletonList(createButton("🔙 Назад", "BACK_TO_MAIN_MENU")));

        keyboardMarkup.setKeyboard(keyboard);
        editMessage.setText(responseText.toString());
        editMessage.setReplyMarkup(keyboardMarkup);
        editMessage.enableMarkdown(true);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Отправляет рецепт по кнопке
    private void sendRecipe(long chatId, int messageId, String recipeName) {
        Recipe recipe = RECIPES.get(recipeName);
        if (recipe != null) {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText("📌 *" + recipe.name + "*\n\n" + recipe.instructions);
            editMessage.enableMarkdown(true);

            try {
                execute(editMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    // Поиск рецептов по ингредиентам
    // Поиск рецептов по ингредиентам
    private void findRecipesByIngredients(long chatId, String userMessage) {
        List<String> userIngredients = Arrays.asList(userMessage.split(", "));

        List<Recipe> matchingRecipes = new ArrayList<>();
        Map<String, List<String>> missingIngredients = new HashMap<>();

        for (Recipe recipe : RECIPES.values()) {
            Set<String> commonIngredients = new HashSet<>(recipe.ingredients);
            commonIngredients.retainAll(userIngredients);  // Пересечение ингредиентов

            if (!commonIngredients.isEmpty()) {
                if (commonIngredients.size() < recipe.ingredients.size()) {
                    // Если не все ингредиенты совпадают, показываем недостающие ингредиенты
                    List<String> missing = new ArrayList<>(recipe.ingredients);
                    missing.removeAll(userIngredients);
                    missingIngredients.put(recipe.name, missing);
                } else {
                    matchingRecipes.add(recipe);  // Полное совпадение
                }
            }
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        if (matchingRecipes.isEmpty() && missingIngredients.isEmpty()) {
            message.setText("К сожалению, я не нашел рецептов по этим ингредиентам. Попробуйте добавить что-то ещё.");
        } else {
            StringBuilder responseText = new StringBuilder("Вот что можно приготовить, учитывая ваши ингредиенты:\n\n");
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            // Отображаем рецепты с полным совпадением
            for (Recipe recipe : matchingRecipes) {
                responseText.append("✅ ").append(recipe.name).append("\n");
                keyboard.add(Collections.singletonList(createButton(recipe.name, "RECIPE_" + recipe.name)));
            }

            // Отображаем рецепты с частичным совпадением и недостающими ингредиентами
            for (Map.Entry<String, List<String>> entry : missingIngredients.entrySet()) {
                responseText.append("⚠️ ").append(entry.getKey()).append(" (не хватает: ").append(String.join(", ", entry.getValue())).append(")\n");
                keyboard.add(Collections.singletonList(createButton(entry.getKey(), "RECIPE_" + entry.getKey())));
            }

            message.setText(responseText.toString());
            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Новый метод для отправки запроса о поиске ингредиентов
    private void sendFindIngredientsPrompt(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🔍 Пожалуйста, напишите продукты через запятую (например: яйца, молоко, сыр).");

        // Создаем клавиатуру с кнопкой "Назад"
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Collections.singletonList(createButton("🔙 Назад", "BACK_TO_MAIN_MENU")));
        keyboardMarkup.setKeyboard(keyboard);

        // Устанавливаем клавиатуру в сообщение
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);  // Отправляем сообщение пользователю
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private static class Recipe {
        String name;
        List<String> ingredients;
        String instructions;

        public Recipe(String name, List<String> ingredients, String instructions) {
            this.name = name;
            this.ingredients = ingredients;
            this.instructions = instructions;
        }
    }
}
