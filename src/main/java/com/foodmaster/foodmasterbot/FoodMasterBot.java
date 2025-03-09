package com.foodmaster.foodmasterbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component
public class FoodMasterBot extends TelegramLongPollingBot {

    @Autowired
    private SpoonacularService spoonacularService;

    @Override
    public String getBotUsername() {
        return "FoodMaster_MealMaster_bot";
    }

    @Override
    public String getBotToken() {
        return "7881995906:AAEXCt-6Xk3mB-Pf11hcrNiHWCfefXkyu2I";
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

    private void findRecipesByIngredients(long chatId, String userMessage) {
        List<String> userIngredients = Arrays.asList(userMessage.split(", "));
        List<Map<String, Object>> externalRecipes = spoonacularService.findRecipesByIngredients(userIngredients);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        if (externalRecipes.isEmpty()) {
            message.setText("❌ Не нашел рецептов с такими ингредиентами. Попробуйте что-то другое.");
        } else {
            StringBuilder responseText = new StringBuilder("🍽 Найденные рецепты:\n\n");
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

            for (Map<String, Object> recipe : externalRecipes) {
                String title = (String) recipe.get("title");
                int id = (Integer) recipe.get("id");

                responseText.append("🍕 ").append(title).append("\n");
                keyboard.add(Collections.singletonList(createButton(title, "SPOONACULAR_" + id)));
            }

            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
            message.setText(responseText.toString());
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendRecipeFromSpoonacular(long chatId, int messageId, int recipeId) {
        Map<String, Object> recipeInfo = spoonacularService.getRecipeById(recipeId);

        if (recipeInfo != null) {
            String title = (String) recipeInfo.get("title");
            String instructions = (String) recipeInfo.getOrDefault("instructions", "Нет инструкции 😔");

            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId);
            editMessage.setMessageId(messageId);
            editMessage.setText("🍽 *" + title + "*\n\n" + instructions);
            editMessage.enableMarkdown(true);

            try {
                execute(editMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private void sendStartMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👋 Привет! Введи список ингредиентов через запятую, и я найду рецепты для тебя.");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        if (callbackData.startsWith("SPOONACULAR_")) {
            int recipeId = Integer.parseInt(callbackData.split("_")[1]);
            sendRecipeFromSpoonacular(chatId, messageId, recipeId);
        }
    }

}
