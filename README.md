# FoodMaster Telegram Bot

FoodMaster - это Telegram-бот, который помогает пользователям анализировать блюда по фото, рассчитывать калорийность (КБЖУ) и предлагать рецепты на основе имеющихся ингредиентов.

## Функционал
- 🔍 **Поиск блюда** по названию
- 🍳 **Поиск рецептов** по ингредиентам
- ⏱️ **Поиск рецептов** по времени приготовления
- 🎲 **Случайный рецепт**
- 📊 **Рассчет нормы КБЖУ**
- 🍴 **Рассчет КБЖУ блюда** по фото (с использованием OpenAI API)

## Запуск проекта

### 1. Клонирование репозитория
```sh
git clone https://github.com/MrArtur0074/Team-Project-7.git foodmasterbot
cd foodmasterbot
```

### 2. Настройка переменных окружения
Создайте файл `application.properties` в `src/main/resources/` и укажите
в нем(Все ключи есть кроме ИИ , 
из-за того что гит деактивирует ключи мы вам отправил его лично.
Надо добавить ИИ ключ в application.propertie и в OpenAIService.java сюда(private final String openAiApiKey = ""; // Укажи API-ключ
)):
```properties
spring.application.name=FoodMaster
bot.token=ВАШ_TELEGRAM_BOT_TOKEN
bot.username=ВАШ_TELEGRAM_BOT_USERNAME
openai.api.key=ВАШ_OPENAI_API_KEY
```

### 3. Установка зависимостей
Убедитесь, что у вас установлен Maven и выполните команду:
```sh
mvn clean install
```

### 4. Запуск бота
```sh
mvn spring-boot:run
```

После успешного запуска бот начнет работать, и вы сможете взаимодействовать с ним через Telegram.

## Технологии
- **Java 23**
- **Spring Boot 3.4.3**
- **Telegram Bots API**
- **OpenAI API** (для анализа изображений и расчета КБЖУ)

## Контакты
Если у вас есть вопросы или предложения, свяжитесь со мной через Telegram: [@FoodScanerBot](https://t.me/FoodScanerBot).

