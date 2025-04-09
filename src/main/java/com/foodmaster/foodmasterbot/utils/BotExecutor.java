package com.foodmaster.foodmasterbot.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

@Component
public class BotExecutor {

    private final TelegramLongPollingBot bot;

    public BotExecutor(TelegramLongPollingBot bot) {
        this.bot = bot;
    }

    public TelegramLongPollingBot getBot() {
        return bot;
    }
}
