package com.foodmaster.foodmasterbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FoodMasterBot extends TelegramLongPollingBot {

    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, UserData> userDataMap = new HashMap<>();

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
            String userMessage = update.getMessage().getText().trim();

            if (userMessage.equalsIgnoreCase("/start")) {
                sendPhoto(chatId, "src/main/resources/static/images/FoodMaster.webp");
                sendStartMenu(chatId);
            } else {
                handleUserInput(chatId, userMessage);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();

            System.out.println("Received callback: " + callbackData);  // Печатаем callbackData

            if (callbackData.equals("CALCULATE_KBZU_NORM")) {
                askForHeight(chatId);
            } else if (callbackData.equals("GENDER_MALE") || callbackData.equals("GENDER_FEMALE")) {
                saveGender(chatId, callbackData);
            } else if (callbackData.startsWith("ACTIVITY_")) {
                saveActivityLevel(chatId, callbackData);
            } else if (callbackData.equals("CALCULATE_KBZU")) {
                // Логика для расчета КБЖУ
                calculateKBZU(chatId);
            } else if (callbackData.equals("BACK_TO_MAIN_MENU")) {
                sendStartMenu(chatId);
            }
            if (callbackData.equals("RANDOM_RECIPE") || callbackData.equals("MORE_RECIPE")) {
                String recipeMessage = spoonacularService.getRandomRecipe();

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

                InlineKeyboardButton moreButton = new InlineKeyboardButton();
                moreButton.setText("🔄 Еще");
                moreButton.setCallbackData("MORE_RECIPE");

                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Назад в главное меню");
                backButton.setCallbackData("BACK_TO_MAIN_MENU");

                buttons.add(Collections.singletonList(moreButton));
                buttons.add(Collections.singletonList(backButton));

                markup.setKeyboard(buttons);

                sendMessageWithKeyboard(chatId, recipeMessage, markup);
            }
            if (callbackData.equals("SEARCH_RECIPE")) {
                String responseText = "🍽️ Выберите категорию блюда:";

                // Создание списка кнопок
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

                // Категории блюд
                String[] categories = {"Завтрак", "Обед", "Ужин", "Десерты", "Вегетарианское"};

                for (String category : categories) {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(category);
                    button.setCallbackData("CATEGORY_" + category.toUpperCase()); // Callback для обработки
                    rowsInline.add(Collections.singletonList(button));
                }

                markupInline.setKeyboard(rowsInline);

                // Создание сообщения с кнопками
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(responseText);
                message.setReplyMarkup(markupInline);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (callbackData.startsWith("CATEGORY_")) {
                String category = callbackData.replace("CATEGORY_", ""); // Получаем категорию
                userStates.put(chatId, "AWAITING_DISH_NAME:" + category); // Запоминаем, что ждем название

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("🔎 Введите название блюда в категории *" + category + "*:");
                message.enableMarkdown(true);

                // Добавляем кнопку "Назад"
                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Назад в главное меню");
                backButton.setCallbackData("BACK_TO_MAIN_MENU");

                InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                keyboardMarkup.setKeyboard(Collections.singletonList(Collections.singletonList(backButton)));

                message.setReplyMarkup(keyboardMarkup);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (callbackData.equals("TRY_AGAIN_BUTTON")) {
                // Запрашиваем новое название блюда
                String responseMessage = "🔍 Пожалуйста, введите название блюда, которое вы хотите найти:";

                // Отправляем сообщение с запросом на ввод
                sendMessage(chatId, responseMessage);

                // Обновляем состояние пользователя, чтобы он снова ввел название блюда
                userStates.put(chatId, "AWAITING_DISH_NAME:");
            }
            if (callbackData.equals("SEARCH_RECIPE_BY_TIME")) {
                // Создаем кнопки с выбором времени
                String responseText = "⏱️ Выберите время приготовления:";

                InlineKeyboardMarkup markupInline = createTimeButtons();

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(responseText);
                message.setReplyMarkup(markupInline);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (callbackData.equals("TIME_15") || callbackData.equals("TIME_15_30") || callbackData.equals("TIME_30_45") || callbackData.equals("TIME_45_60") || callbackData.equals("TIME_60")) {
                // Получаем рецепты по выбранному времени
                System.out.println("Selected time range: " + callbackData);  // Выводим выбранное время для отладки
                String recipes = spoonacularService.getRecipesByTime(callbackData);

                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(recipes);

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Назад в главное меню");
                backButton.setCallbackData("BACK_TO_MAIN_MENU");

                keyboard.add(Collections.singletonList(backButton));
                inlineKeyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(inlineKeyboardMarkup);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } if (callbackData.equals("SEARCH_BY_INGREDIENTS")) {
                String responseText = "🍳 Пожалуйста, введите ингредиенты, разделённые запятыми (например, курица, помидоры, рис):";

                sendMessage(chatId, responseText);

                // Запоминаем, что теперь бот ожидает ввод ингредиентов
                userStates.put(chatId, "AWAITING_INGREDIENTS");
            }
        }
    }

    // Метод для создания кнопок с выбором времени
    private InlineKeyboardMarkup createTimeButtons() {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        String[] times = {"До 15 минут", "15-30 минут", "30-45 минут", "45-60 минут", "60 минут и больше"};
        String[] callbackData = {"TIME_15", "TIME_15_30", "TIME_30_45", "TIME_45_60", "TIME_60"};

        for (int i = 0; i < times.length; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(times[i]);  // Текст на русском
            button.setCallbackData(callbackData[i]);  // Английский callbackData
            rowsInline.add(Collections.singletonList(button));
        }

        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    public void sendPhoto(Long chatId, String photoPath) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        sendPhoto.setPhoto(new InputFile(new File(photoPath))); // Локальный файл

        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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

        // Рекомендации для похудения и набора массы
        double caloriesToLoseWeight = totalKBZU - totalKBZU * 0.15; // 15% дефицит для похудения
        double caloriesToGainWeight = totalKBZU + totalKBZU * 0.15; // 15% избыток для набора массы

        // Формирование текста с результатами
        String resultMessage = String.format(
                "📊 Ваша норма КБЖУ:\n" +
                        "- Основной обмен (BMR): %.2f\n" +
                        "- Уровень активности: %s\n" +
                        "- Итого: %.2f ккал в день\n\n" +
                        "💡 **Рекомендации по калориям:**\n\n" +
                        "🍏 **Если ваша цель — похудение:**\n" +
                        "Для похудения рекомендуется снизить потребление калорий. Чтобы терять вес, потребляйте около %.2f ккал в день. Это позволит создать дефицит калорий, что приведет к потере массы.\n\n" +
                        "🍎 **Если ваша цель — набор массы:**\n" +
                        "Для набора массы рекомендуется увеличить потребление калорий. Чтобы набирать массу, потребляйте около %.2f ккал в день. Это создаст избыток калорий, что поможет вашему организму набирать мышечную массу.\n\n",
                bmr, userData.getActivityLevel(), totalKBZU, caloriesToLoseWeight, caloriesToGainWeight
        );


        // Кнопка для возврата в главное меню
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад в главное меню");
        backButton.setCallbackData("BACK_TO_MAIN_MENU");
        buttons.add(Collections.singletonList(backButton));
        markup.setKeyboard(buttons);

        sendMessageWithKeyboard(chatId, resultMessage, markup);
    }


    private void sendMessageWithKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboard);
        message.enableHtml(true); // Включаем HTML-разметку, если нужно

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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

        if (state != null && state.equals("AWAITING_INGREDIENTS")) {
            String ingredients = userMessage.trim(); // Получаем список ингредиентов

            if (!ingredients.isEmpty()) {
                String recipes = spoonacularService.getRecipesByIngredients(ingredients); // Получаем рецепты по ингредиентам

                if (recipes != null && !recipes.isEmpty()) {
                    // Если рецепты найдены, отправляем их пользователю
                    sendMessageWithBackButton(chatId, recipes);
                } else {
                    // Если рецепты не найдены, отправляем сообщение
                    sendMessageWithBackButton(chatId, "Извините, не удалось найти рецепты с такими ингредиентами.");
                }
            } else {
                sendMessageWithBackButton(chatId, "❌ Пожалуйста, введите хотя бы один ингредиент.");
            }

            // Сбрасываем состояние пользователя после обработки
            userStates.remove(chatId);
            return;
        }

        // Проверка, ожидается ли ввод названия блюда
        if (state != null && state.startsWith("AWAITING_DISH_NAME:")) {
            String category = state.split(":")[1]; // Извлекаем категорию
            String recipeMessage = spoonacularService.getRecipeByName(userMessage, category); // Поиск с категорией

            if (recipeMessage != null && !recipeMessage.isEmpty()) {
                InlineKeyboardButton tryAgainButton = new InlineKeyboardButton();
                tryAgainButton.setText("🔄 Попробовать еще раз");
                tryAgainButton.setCallbackData("TRY_AGAIN_BUTTON");

                InlineKeyboardButton backButton = new InlineKeyboardButton();
                backButton.setText("🔙 Назад в главное меню");
                backButton.setCallbackData("BACK_TO_MAIN_MENU");

                InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                keyboard.add(Collections.singletonList(tryAgainButton));
                keyboard.add(Collections.singletonList(backButton));
                keyboardMarkup.setKeyboard(keyboard);

                sendMessageWithKeyboard(chatId, recipeMessage, keyboardMarkup);
            }

            userStates.remove(chatId); // Сбрасываем состояние после поиска
            return;
        }

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
        keyboard.add(Collections.singletonList(createButton("⏱️ Найти блюдо по времени", "SEARCH_RECIPE_BY_TIME")));
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

    private void sendMessageWithBackButton(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        // Создаем инлайн-клавиатуру с кнопкой "Назад"
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🔙 Назад в главное меню");
        backButton.setCallbackData("BACK_TO_MAIN_MENU");

        keyboard.add(Collections.singletonList(backButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
