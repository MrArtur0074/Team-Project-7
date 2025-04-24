// Main bot class after splitting
package com.foodmaster.foodmasterbot.bot;

import com.foodmaster.foodmasterbot.handlers.CallbackHandler;
import com.foodmaster.foodmasterbot.handlers.CommandHandler;
import com.foodmaster.foodmasterbot.states.UserStateManager;
import com.foodmaster.foodmasterbot.utils.MessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class FoodMasterBot extends TelegramLongPollingBot {
    //class for

    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private CallbackHandler callbackHandler;

    @Autowired
    private UserStateManager userStateManager;

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
            commandHandler.handleTextMessage(update.getMessage());
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            commandHandler.processPhoto(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            callbackHandler.handleCallback(update.getCallbackQuery());
        }
    }
}
