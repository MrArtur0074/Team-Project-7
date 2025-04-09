package com.foodmaster.foodmasterbot.handlers;


import com.foodmaster.foodmasterbot.service.SpoonacularService;
import com.foodmaster.foodmasterbot.states.UserStateManager;
import com.foodmaster.foodmasterbot.utils.MessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;


@Component
public class CallbackHandler {


    @Autowired
    private SpoonacularService spoonacularService;


    @Autowired
    private UserStateManager userStateManager;


    @Autowired
    private MessageUtils messageUtils;


    public void handleCallback(CallbackQuery query) {
        String callbackData = query.getData();
        long chatId = query.getMessage().getChatId();




        switch (callbackData) {
            case "CALCULATE_KBZU_RECIPE":
                messageUtils.requestPhoto(chatId);
                break;


            case "CALCULATE_KBZU_NORM":
                userStateManager.askForHeight(chatId);
                break;


            case "RANDOM_RECIPE":
            case "MORE_RECIPE":
                String recipeMessage = spoonacularService.getRandomRecipe();
                InlineKeyboardMarkup recipeMarkup = messageUtils.createMoreBackKeyboard();
                messageUtils.sendMessageWithKeyboard(chatId, recipeMessage, recipeMarkup);
                break;


            case "SEARCH_RECIPE":
                messageUtils.sendCategorySelection(chatId);
                break;


            case "SEARCH_RECIPE_BY_TIME":
                messageUtils.sendTimeSelection(chatId);
                break;


            case "SEARCH_BY_INGREDIENTS":
                messageUtils.sendMessage(chatId, "🍳 Пожалуйста, введите ингредиенты, разделённые запятыми (например, курица, помидоры, рис):");
                userStateManager.setUserState(chatId, "AWAITING_INGREDIENTS");
                break;


            case "SEARCH_EXCLUDING_INGREDIENTS":
                messageUtils.askExcludedIngredients(chatId);
                userStateManager.setUserState(chatId, "AWAITING_EXCLUDED_INGREDIENTS");
                break;


}

