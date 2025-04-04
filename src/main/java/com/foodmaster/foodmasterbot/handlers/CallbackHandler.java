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




            case "SEARCH_RECIPE_BY_CALORIES":  // Новый кейс для поиска по калориям
                messageUtils.sendMessage(chatId, "🍽 Пожалуйста, введите максимальное количество калорий:");
                userStateManager.setUserState(chatId, "AWAITING_CALORIES");
                break;


            case "BACK_TO_MAIN_MENU":
                messageUtils.sendStartMenu(chatId);
                break;


            case "TRY_AGAIN_BUTTON":
                messageUtils.sendMessage(chatId, "🔍 Пожалуйста, введите название блюда, которое вы хотите найти:");
                userStateManager.setUserState(chatId, "AWAITING_DISH_NAME:");
                break;


            default:
                if (callbackData.startsWith("CATEGORY_")) {
                    String category = callbackData.replace("CATEGORY_", "");
                    userStateManager.setUserState(chatId, "AWAITING_DISH_NAME:" + category);
                    messageUtils.askForDishName(chatId, category);
                } else if (callbackData.startsWith("TIME_")) {
                    String recipes = spoonacularService.getRecipesByTime(callbackData);
                    messageUtils.sendMessage(chatId, recipes);
                } else if (callbackData.equals("GENDER_MALE") || callbackData.equals("GENDER_FEMALE")) {
                    userStateManager.saveGender(chatId, callbackData);
                } else if (callbackData.startsWith("ACTIVITY_")) {
                    userStateManager.saveActivityLevel(chatId, callbackData);
                } else if (callbackData.equals("CALCULATE_KBZU")) {
                    userStateManager.calculateKBZU(chatId);
                } else if (callbackData.startsWith("CUISINE_PAGE_")) {
                    int page = Integer.parseInt(callbackData.replace("CUISINE_PAGE_", ""));
                    int messageIdToDelete = query.getMessage().getMessageId();  // Получаем ID старого сообщения
                    messageUtils.sendCuisineSelection(chatId, page, messageIdToDelete);
                } else if (callbackData.startsWith("CUISINE_")) {
                    String cuisine = callbackData.replace("CUISINE_", "");
                    String recipes = spoonacularService.getRecipesByCuisine(cuisine);
                    messageUtils.sendMessage(chatId, recipes);
                }


                break;
        }
    }
}

