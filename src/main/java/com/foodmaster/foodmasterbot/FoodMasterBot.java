package com.foodmaster.foodmasterbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FoodMasterBot extends TelegramLongPollingBot {

    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, UserData> userDataMap = new HashMap<>();

    @Override
    public String getBotUsername() {
        return "FoodMaster_MealMaster_bot";
    }

    @Override
    public String getBotToken() {
        return "7881995906:AAEXCt-6Xk3mB-Pf11hcrNiHWCfefXkyu2I";  // ЗАМЕНИ НА СВОЙ ТОКЕН
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String userMessage = update.getMessage().getText().trim();

            if (userMessage.equalsIgnoreCase("/start")) {
                sendStartMenu(chatId);
            } else {
                handleUserInput(chatId, userMessage);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();

            if (callbackData.equals("CALCULATE_KBZU_NORM")) {
                askForHeight(chatId);
            } else if (callbackData.equals("GENDER_MALE") || callbackData.equals("GENDER_FEMALE")) {
                saveGender(chatId, callbackData);
            } else if (callbackData.startsWith("ACTIVITY_")) {
                saveActivityLevel(chatId, callbackData);
            }   else if (callbackData.equals("CALCULATE_KBZU")) {
                // Логика для расчета КБЖУ
                calculateKBZU(chatId);
            }

        }
    }

    private void calculateKBZU(long chatId) {
        UserData userData = userDataMap.get(chatId);
        if (userData == null) {
            sendMessage(chatId, "❌ Пожалуйста, заполните все данные.");
            return;
        }

        // Расчет нормы КБЖУ
        double bmr = 0;
        if ("Мужской".equals(userData.getGender())) {
            bmr = 88.362 + (13.397 * userData.getWeight()) + (4.799 * userData.getHeight()) - (5.677 * userData.getAge());
        } else if ("Женский".equals(userData.getGender())) {
            bmr = 447.593 + (9.247 * userData.getWeight()) + (3.098 * userData.getHeight()) - (4.330 * userData.getAge());
        }

        // Учет уровня активности
        double activityMultiplier = 1.2;  // Низкая активность по умолчанию
        switch (userData.getActivityLevel()) {
            case "Низкая активность":
                activityMultiplier = 1.2;
                break;
            case "Средняя активность":
                activityMultiplier = 1.55;
                break;
            case "Высокая активность":
                activityMultiplier = 1.9;
                break;
        }

        // Расчет общего уровня КБЖУ
        double totalKBZU = bmr * activityMultiplier;

        String resultMessage = String.format("📊 Ваша норма КБЖУ:\n- Основной обмен (BMR): %.2f\n- Уровень активности: %s\n- Итого: %.2f ккал в день", bmr, userData.getActivityLevel(), totalKBZU);
        sendMessage(chatId, resultMessage);
    }



    private void askForHeight(long chatId) {
        userStates.put(chatId, "AWAITING_HEIGHT");
        sendMessage(chatId, "📍 Укажите ваш рост в сантиметрах (например, 175).");
    }

    private void askForWeight(long chatId) {
        userStates.put(chatId, "AWAITING_WEIGHT");
        sendMessage(chatId, "⚖️ Укажите ваш вес в килограммах (например, 70).");
    }

    private void askForAge(long chatId) {
        userStates.put(chatId, "AWAITING_AGE");
        sendMessage(chatId, "🎂 Укажите ваш возраст (например, 25).");
    }

    private void askForGender(long chatId) {
        userStates.put(chatId, "AWAITING_GENDER");

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👤 Выберите ваш пол:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Arrays.asList(
                createButton("♂️ Мужской", "GENDER_MALE"),
                createButton("♀️ Женский", "GENDER_FEMALE")
        ));

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
        sendMessage(message);
    }

    private void askForActivityLevel(long chatId) {
        userStates.put(chatId, "AWAITING_ACTIVITY_LEVEL");

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🏃‍♀️ Физическая активность\n\n" +
                "Нужно учесть вашу физическую активность в течение дня. Нажмите одну из кнопок ниже, с учетом:\n\n" +
                "🥉 Низкая активность - Офисная работа (сидячая), редкие прогулки.\n" +
                "🥈 Средняя активность - Сидячая работа, но 3 раза в неделю интенсивный спорт.\n" +
                "🥇 Высокая активность - Работа на ногах (8-12 часов), плюс 3 раза в неделю интенсивный спорт. Или, сидячая работа, но спорт 5 раз в неделю.");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Arrays.asList(
                createButton("🥉 Низкая активность", "ACTIVITY_LOW"),
                createButton("🥈 Средняя активность", "ACTIVITY_MEDIUM"),
                createButton("🥇 Высокая активность", "ACTIVITY_HIGH")
        ));

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
        sendMessage(message);
    }

    private void saveActivityLevel(long chatId, String activity) {
        UserData userData = userDataMap.get(chatId);
        if (userData == null) return;

        switch (activity) {
            case "ACTIVITY_LOW":
                userData.setActivityLevel("🥉 Низкая активность");
                break;
            case "ACTIVITY_MEDIUM":
                userData.setActivityLevel("🥈 Средняя активность");
                break;
            case "ACTIVITY_HIGH":
                userData.setActivityLevel("🥇 Высокая активность");
                break;
        }

        userDataMap.put(chatId, userData);
        sendFinalMessage(chatId);
    }


    private void handleUserInput(long chatId, String userMessage) {
        String state = userStates.get(chatId);

        try {
            int value = Integer.parseInt(userMessage);
            UserData userData = userDataMap.getOrDefault(chatId, new UserData());

            if ("AWAITING_HEIGHT".equals(state)) {
                if (value >= 50 && value <= 250) {
                    userData.setHeight(value);
                    userDataMap.put(chatId, userData);
                    askForWeight(chatId);
                } else {
                    sendMessage(chatId, "❌ Рост должен быть в диапазоне 50-250 см. Попробуйте снова.");
                }
            } else if ("AWAITING_WEIGHT".equals(state)) {
                if (value >= 20 && value <= 300) {
                    userData.setWeight(value);
                    userDataMap.put(chatId, userData);
                    askForAge(chatId);
                } else {
                    sendMessage(chatId, "❌ Вес должен быть в диапазоне 20-300 кг. Попробуйте снова.");
                }
            } else if ("AWAITING_AGE".equals(state)) {
                if (value >= 5 && value <= 120) {
                    userData.setAge(value);
                    userDataMap.put(chatId, userData);
                    askForGender(chatId);
                } else {
                    sendMessage(chatId, "❌ Возраст должен быть в диапазоне 5-120 лет. Попробуйте снова.");
                }
            }
        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Введите корректное число.");
        }
    }

    private void saveGender(long chatId, String gender) {
        UserData userData = userDataMap.get(chatId);
        if (userData == null) return;

        userData.setGender(gender.equals("GENDER_MALE") ? "Мужской" : "Женский");
        userDataMap.put(chatId, userData);

        askForActivityLevel(chatId);
    }

    private void sendStartMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👋 Добро пожаловать в *FoodMaster*!\nВыберите одну из опций:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(Collections.singletonList(createButton("🔍 Поиск блюда", "SEARCH_RECIPE")));
        keyboard.add(Collections.singletonList(createButton("🍳 Поиск по ингредиентам", "SEARCH_BY_INGREDIENTS")));
        keyboard.add(Collections.singletonList(createButton("🎲 Случайный рецепт", "RANDOM_RECIPE")));
        keyboard.add(Collections.singletonList(createButton("📊 Рассчет нормы КБЖУ", "CALCULATE_KBZU_NORM")));
        keyboard.add(Collections.singletonList(createButton("🍴 Рассчет КБЖУ блюда", "CALCULATE_KBZU_RECIPE")));

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
        message.enableMarkdown(true);

        sendMessage(message);
    }

    private void sendFinalMessage(long chatId) {
        UserData userData = userDataMap.get(chatId);
        if (userData == null) return;

        String result = String.format(
                "✅ *Ваши данные:*\n" +
                        "- 📏 Рост: *%d см*\n" +
                        "- ⚖️ Вес: *%d кг*\n" +
                        "- 🎂 Возраст: *%d лет*\n" +
                        "- 👤 Пол: *%s*\n" +
                        "- 🏃‍♂️ Активность: *%s*\n\n" +
                        "📊 Теперь мы можем рассчитать вашу норму КБЖУ!",
                userData.getHeight(), userData.getWeight(), userData.getAge(),
                userData.getGender(), userData.getActivityLevel()
        );

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(result);
        message.enableMarkdown(true);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Collections.singletonList(createButton("📊 Рассчитать КБЖУ", "CALCULATE_KBZU")));

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        sendMessage(message);
    }



    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        sendMessage(message);
    }

    private void sendMessage(SendMessage message) {
        try {
            execute(message);
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

    private void deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(String.valueOf(chatId));
        deleteMessage.setMessageId(messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
