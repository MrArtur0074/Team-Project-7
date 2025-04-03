package com.foodmaster.foodmasterbot.handlers;

import com.foodmaster.foodmasterbot.service.OpenAIService;
import com.foodmaster.foodmasterbot.service.SpoonacularService;
import com.foodmaster.foodmasterbot.states.UserStateManager;
import com.foodmaster.foodmasterbot.utils.MessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.File;
import java.util.Map;

@Component
public class CommandHandler {

    @Autowired
    private SpoonacularService spoonacularService;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private UserStateManager userStateManager;

    @Autowired
    private MessageUtils messageUtils;

    public void handleTextMessage(Message message) {
        long chatId = message.getChatId();
        String userMessage = message.getText().trim();

        switch (userMessage.toLowerCase()) {
            case "/start":
                sendPhoto(chatId, "src/main/resources/static/images/FoodMaster.webp");
                messageUtils.sendStartMenu(chatId);
                break;
            case "/help":
                messageUtils.handleHelpCommand(chatId);  // Вызов метода для отправки справки
                break;
            default:
                userStateManager.handleUserInput(chatId, userMessage);
        }
    }

    public void processPhoto(Message message) {
        long chatId = message.getChatId();
        String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();

        messageUtils.sendMessage(chatId, "Обрабатываю фото...");

        String imageUrl = messageUtils.getFileUrl(fileId);
        if (imageUrl == null) {
            messageUtils.sendMessage(chatId, "Ошибка при получении фото, попробуйте снова.");
            return;
        }

        Map<String, Object> nutritionInfo = openAIService.analyzeImageAndCalculateNutrition(imageUrl);

        if (!nutritionInfo.isEmpty()) {
            StringBuilder responseText = new StringBuilder("Вот расчет КБЖУ:\n");
            nutritionInfo.forEach((key, value) ->
                    responseText.append("▫ ").append(key).append(": *").append(value).append("*\n")
            );
            messageUtils.sendMessage(chatId, responseText.toString());
        } else {
            messageUtils.sendMessage(chatId, "Не удалось определить блюда на фото. Попробуйте ещё раз!");
        }
    }

    private void sendPhoto(Long chatId, String photoPath) {
        InputFile photo = new InputFile(new File(photoPath));
        messageUtils.sendPhoto(chatId, photo);
    }
}