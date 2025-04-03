package com.foodmaster.foodmasterbot.utils;

import com.foodmaster.foodmasterbot.model.UserData;
import com.foodmaster.foodmasterbot.service.SpoonacularService;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MessageUtils {

    private final SpoonacularService spoonacularService;
    private final BotExecutor botExecutor;

    public MessageUtils(SpoonacularService spoonacularService, BotExecutor botExecutor) {
        this.spoonacularService = spoonacularService;
        this.botExecutor = botExecutor;
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        executeMessage(message);
    }

    public void sendMessageWithKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboard);
        message.enableMarkdown(true);
        executeMessage(message);
    }

    public void sendPhoto(Long chatId, InputFile photo) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        sendPhoto.setPhoto(photo);
        try {
            botExecutor.getBot().execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendStartMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("👋 Добро пожаловать в *FoodMaster*!\nВыберите одну из опций:");
        message.enableMarkdown(true);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Убираем три кнопки
        keyboard.add(Collections.singletonList(createButton("🔍 Поиск блюда", "SEARCH_RECIPE")));
        keyboard.add(Collections.singletonList(createButton("🍳 Поиск по ингредиентам", "SEARCH_BY_INGREDIENTS")));
        keyboard.add(Collections.singletonList(createButton("🎲 Случайный рецепт", "RANDOM_RECIPE")));
        keyboard.add(Collections.singletonList(createButton("📊 Рассчет нормы КБЖУ", "CALCULATE_KBZU_NORM")));
        keyboard.add(Collections.singletonList(createButton("🍴 Рассчет КБЖУ блюда", "CALCULATE_KBZU_RECIPE")));

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        executeMessage(message);
    }

    public void handleHelpCommand(long chatId) {
        String helpMessage = "👨‍🍳 Добро пожаловать в FoodMasterBot!\n" +
                "Привет! Я помогу тебе найти рецепты и рассчитать нужные питательные вещества. Вот, что ты можешь сделать:\n" +
                "\n" +
                "1)Поиск рецептов по названию, ингредиентам, калориям или случайным рецептом.\n" +
                "\n" +
                "2)Исключить определённые ингредиенты из поиска.\n" +
                "\n" +
                "3)Рассчитать свою суточную норму калорий, белков, жиров и углеводов.\n" +
                "\n" +
                "Если у тебя возникнут вопросы, просто напиши мне! Я всегда готов помочь!";

        sendMessage(chatId, helpMessage);
    }

    public void sendFinalMessage(long chatId, UserData data) {
        String result = String.format(
                "✅ *Ваши данные:*\n" +
                        "- 📏 Рост: *%d см*\n" +
                        "- ⚖️ Вес: *%d кг*\n" +
                        "- 🎂 Возраст: *%d лет*\n" +
                        "- 👤 Пол: *%s*\n" +
                        "- 🏃‍♂️ Активность: *%s*\n\n" +
                        "📊 Теперь мы можем рассчитать вашу норму КБЖУ!",
                data.getHeight(), data.getWeight(), data.getAge(),
                data.getGender(), data.getActivityLevel()
        );

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(createButton("📊 Рассчитать КБЖУ", "CALCULATE_KBZU"))
        ));

        sendMessageWithKeyboard(chatId, result, markup);
    }

    public void sendKBZUResult(long chatId, UserData data) {
        double bmr = data.getGender().equals("Мужской") ?
                88.362 + (13.397 * data.getWeight()) + (4.799 * data.getHeight()) - (5.677 * data.getAge()) :
                447.593 + (9.247 * data.getWeight()) + (3.098 * data.getHeight()) - (4.330 * data.getAge());

        double multiplier = switch (data.getActivityLevel()) {
            case "🥈 Средняя активность" -> 1.55;
            case "🥇 Высокая активность" -> 1.9;
            default -> 1.2;
        };

        double total = bmr * multiplier;
        double lose = total - total * 0.15;
        double gain = total + total * 0.15;

        String text = String.format(
                "📊 Ваша норма КБЖУ:\n" +
                        "- Основной обмен (BMR): %.2f\n" +
                        "- Уровень активности: %s\n" +
                        "- Итого: %.2f ккал в день\n\n" +
                        "🍏 Для похудения: %.2f ккал/день\n" +
                        "🍎 Для набора массы: %.2f ккал/день",
                bmr, data.getActivityLevel(), total, lose, gain);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(createButton("🔙 Назад в главное меню", "BACK_TO_MAIN_MENU"))
        ));

        sendMessageWithKeyboard(chatId, text, markup);
    }

    public void requestPhoto(long chatId) {
        sendMessage(chatId, "📸 Пожалуйста, отправьте фото блюда.");
    }

    public void sendGenderSelection(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(createButton("♂️ Мужской", "GENDER_MALE"), createButton("♀️ Женский", "GENDER_FEMALE"))
        ));
        sendMessageWithKeyboard(chatId, "👤 Выберите ваш пол:", markup);
    }

    public void sendActivityLevelSelection(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(createButton("🥉 Низкая активность", "ACTIVITY_LOW")),
                List.of(createButton("🥈 Средняя активность", "ACTIVITY_MEDIUM")),
                List.of(createButton("🥇 Высокая активность", "ACTIVITY_HIGH"))
        ));
        sendMessageWithKeyboard(chatId, "🏃‍♀️ Выберите ваш уровень активности:", markup);
    }

    public void sendCategorySelection(long chatId) {
        String[] categories = {"Завтрак", "Обед", "Ужин", "Десерты", "Вегетарианское"};
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String cat : categories) {
            rows.add(Collections.singletonList(createButton(cat, "CATEGORY_" + cat.toUpperCase())));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        sendMessageWithKeyboard(chatId, "🍽️ Выберите категорию блюда:", markup);
    }

    public void sendDishResult(long chatId, String recipe) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(createButton("🔄 Попробовать еще раз", "TRY_AGAIN_BUTTON")),
                List.of(createButton("🔙 Назад в главное меню", "BACK_TO_MAIN_MENU"))
        ));

        sendMessageWithKeyboard(chatId, recipe, markup);
    }

    public void askForDishName(long chatId, String category) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(
                List.of(createButton("🔙 Назад в главное меню", "BACK_TO_MAIN_MENU"))
        ));
        sendMessageWithKeyboard(chatId, "🔎 Введите название блюда в категории *" + category + "*:", markup);
    }

    public void sendTimeSelection(long chatId) {
        String[] labels = {"До 15 минут", "15-30 минут", "30-45 минут", "45-60 минут", "60 минут и больше"};
        String[] callbacks = {"TIME_15", "TIME_15_30", "TIME_30_45", "TIME_45_60", "TIME_60"};
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            rows.add(Collections.singletonList(createButton(labels[i], callbacks[i])));
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        sendMessageWithKeyboard(chatId, "⏱️ Выберите время приготовления:", markup);
    }

    public InlineKeyboardMarkup createMoreBackKeyboard() {
        InlineKeyboardButton more = createButton("🔄 Еще", "MORE_RECIPE");
        InlineKeyboardButton back = createButton("🔙 Назад в главное меню", "BACK_TO_MAIN_MENU");
        return new InlineKeyboardMarkup(List.of(List.of(more), List.of(back)));
    }

    public InlineKeyboardButton createButton(String text, String data) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(data);
        return button;
    }

    public SpoonacularService getSpoonacularService() {
        return spoonacularService;
    }

    private void executeMessage(SendMessage message) {
        try {
            botExecutor.getBot().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void deleteMessage(long chatId, int messageId) {
        DeleteMessage delete = new DeleteMessage();
        delete.setChatId(String.valueOf(chatId));
        delete.setMessageId(messageId);
        try {
            botExecutor.getBot().execute(delete);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public String getFileUrl(String fileId) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            File file = botExecutor.getBot().execute(getFile);
            return file.getFileUrl(botExecutor.getBot().getBotToken());
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }
}
