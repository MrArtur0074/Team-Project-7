package com.foodmaster.foodmasterbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class FoodMasterApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(FoodMasterApplication.class, args);

        // Получаем бин FoodMasterBot и регистрируем его в TelegramBotsApi
        FoodMasterBot bot = context.getBean(FoodMasterBot.class);
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            System.out.println("Бот успешно запущен!");
        } catch (TelegramApiException e) {
            System.out.println("Ошибка при запуске бота: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
