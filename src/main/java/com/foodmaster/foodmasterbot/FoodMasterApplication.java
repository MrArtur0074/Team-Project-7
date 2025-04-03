package com.foodmaster.foodmasterbot;

import com.foodmaster.foodmasterbot.bot.FoodMasterBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class FoodMasterApplication {

    public static void main(String[] args) {
        // Запускаем приложение Spring
        ApplicationContext context = SpringApplication.run(FoodMasterApplication.class, args);

        // Получаем бин бота из контекста Spring
        FoodMasterBot bot = context.getBean(FoodMasterBot.class);

        // Инициализируем TelegramBotsApi и регистрируем бота
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            System.out.println("Бот успешно запущен!");
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при запуске бота: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
