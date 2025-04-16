package com.foodmaster.foodmasterbot.service;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TranslatorService {

    private static final String TRANSLATE_URL = "https://api.mymemory.translated.net/get";

    // Метод для перевода текста на английский язык
    public static String translateToEnglish(String text) throws Exception {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String urlString = TRANSLATE_URL + "?q=" + encodedText + "&langpair=ru|en"; // Перевод с русского на английский

        // Отправляем запрос на MyMemory API
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Получаем результат
        String responseString = response.toString();
        System.out.println("Ответ от MyMemory API: " + responseString); // Для диагностики

        // Обрабатываем ответ
        JSONObject jsonResponse = new JSONObject(responseString);

        // Проверяем наличие ключа "responseData"
        if (jsonResponse.has("responseData")) {
            JSONObject responseData = jsonResponse.getJSONObject("responseData");
            if (responseData.has("translatedText")) { // Исправляем на translatedText
                return responseData.getString("translatedText");
            } else {
                throw new Exception("Перевод не найден в ответе.");
            }
        } else {
            throw new Exception("Ошибка в ответе от MyMemory API. Ответ не содержит ключ 'responseData'.");
        }

    }
    public static String translateToFrench(String text) throws Exception {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String urlString = TRANSLATE_URL + "?q=" + encodedText;
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);


        }
    }
}