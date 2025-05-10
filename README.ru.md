> ⚠️ **Предупреждение:** Эта версия README на русском языке может содержать неточности или быть неполной. За самой актуальной и достоверной информацией рекомендуется обращаться к [основной версии README](README.md) на английском.

# Обзор

Это бэкенд для платформы, которая позволяет пользователям создавать объявления для поиска команд для своих проектов или присоединения к существующим. Цель платформы — помочь пользователям найти возможности для получения опыта работы в команде и улучшения своих навыков.

Каждый может участвовать и заимствовать код, но важно уважать условия лицензии.

[Read in English](README.md)

<details>
  <summary><strong>Текущая реализация:</strong></summary>

 ### Управление объявлениями
  - **Создание, редактирование и удаление объявлений.**
  - **Просмотр списка объявлений.**
  - **Добавление или удаление объявлений из избранного.**
  - **Интеграция с ElasticSearch.**

### Управление комментариями и ответами
  - **Создание, редактирование и удаление комментариев к объявлениям.**
  - **Добавление и управление ответами на комментарии.**

### Управление профилем пользователя и аккаунтом
  - **Регистрация и авторизация пользователей.**
  - **Редактирование информации профиля.**
  - **Обновление аватара профиля.**
  - **Просмотр профиля пользователя.**
  - **Удаление аккаунта пользователя.**
  - **Подписки на пользователей.**

### Контроль доступа и безопасность
  - **Валидация токена доступа.**
  - **Выдача нового токена доступа с использованием refresh токена.**

### Административные инструменты и управление жалобами
  - **Отправка жалоб на пользователей или объявления.**
  - **Права администратора включают:**
    - Просмотр и удаление жалоб.
    - Модерация пользователей (блокировка/разблокировка, изменение ролей).
    - Управление ключами регистрации администраторов.
  
### Уведомления по электронной почте
  - **Отправка сообщений на электронную почту (для восстановления пароля или уведомлений).**
</details>

---
<details>
  <summary><strong>🚀Быстрый старт:</strong></summary>
	
1. Заполните файл `.env`.
2. Запустите контейнеры.
3. Создайте индексы в ElasticSearch с этой конфигурацией:

```json
{
  "settings": {
    "analysis": {
      "normalizer": {
        "lowercase_normalizer": {
          "type": "custom",
          "filter": [
            "lowercase"
          ]
        }
      },
      "analyzer": {
        "custom_russian_english": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "russian_stemmer",
            "english_stemmer"
          ]
        }
      },
      "filter": {
        "russian_stemmer": {
          "type": "stemmer",
          "language": "russian"
        },
        "english_stemmer": {
          "type": "stemmer",
          "language": "english"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": {
        "type": "long"
      },
      "title": {
        "type": "text",
        "analyzer": "custom_russian_english"
      },
      "text": {
        "type": "text",
        "analyzer": "custom_russian_english"
      },
      "createTime": {
        "type": "date"
      },
      "tags": {
        "type": "keyword"
      }
    }
  }
}
```

4. Настройте SMTP-сервис.

### Скачать образы микросервисов

Чтобы скачать необходимые Docker-образы, выполните следующие команды:

```bash
docker pull xusss/message_broker_microservice:latest
docker pull xusss/comment_microservice:latest
docker pull xusss/image_microservice:latest
docker pull xusss/ads_microservice:latest
docker pull xusss/authentication_microservice:latest
```

Эти команды загрузят последние версии соответствующих образов с Docker Hub.


</details>

---

### Настройки

| **Переменная окружения**       | **Описание**                                                                                           |
|---------------------------------|--------------------------------------------------------------------------------------------------------|
| `X_API_KEY`                    | Ключ для подключения внутренних API друг к другу, который не должен быть доступен посторонним       |
| `ELASTIC_USERNAME`             | Имя пользователя для вашего экземпляра ElasticSearch                                                 |
| `ELASTIC_PASSWORD`             | Пароль для пользователя ElasticSearch                                                                 |
| `POSTGRES_DB`                  | Имя вашей базы данных PostgreSQL                                                                       |
| `POSTGRES_USER`                | Логин для пользователя PostgreSQL                                                                     |
| `POSTGRES_PASSWORD`            | Пароль для пользователя PostgreSQL                                                                    |
| `MINIO_ROOT_USER`              | Логин для вашего экземпляра MinIO                                                                     |
| `MINIO_ROOT_PASSWORD`          | Пароль для вашего экземпляра MinIO                                                                   |
| `RABBITMQ_DEFAULT_USER`        | Логин для RabbitMQ                                                                                   |
| `RABBITMQ_DEFAULT_PASS`        | Пароль для RabbitMQ                                                                                    |
| `SMTP_MAIL`                    | Адрес электронной почты для SMTP                                                                      |
| `SMTP_PASSWORD`                | Пароль для аккаунта электронной почты SMTP                                                           |
| `SMTP_HOST`                    | Хост для вашего SMTP-сервера                                                                          |
| `SMTP_PORT`                    | Порт для вашего SMTP-сервера                                                                          |
| `REDIS_PASSWORD`               | Пароль для Redis                                                                                      |
| `SEND_EMAIL_MESSAGE`           | Нужно ли отправлять сообщения на почту об успешной публикации объявления (`true/false`)               |

---

# Микросервисы
> ⚠️ **Примечание:** Диаграммы, представленные ниже, не описывают работу всех запросов, а только тех, которые уникальны по своей реализации. Если для запроса нет диаграммы, это означает, что он почти или полностью описан аналогичным.

### Микросервис объявлений
Управляет объявлениями и их содержимым, включая жалобы пользователей

*Для детальной документации API посетите Swagger UI:*
[Swagger UI - Микросервис объявлений](http://localhost:8080/swagger-ui/index.html)

<details>
  <summary><strong>Схема эндпоинтов:</strong></summary>

  <p><strong>Схема запроса для получения объявления:</strong></p>
  <div align="center">
    <img src="https://devkarmanov.github.io/ImagesForFinderProject/imagesForGitHub/get_card_request.svg" alt="Схема работы запроса" style="width:80%; border-radius: 8px; box-shadow: 0px 4px 8px rgba(0,0,0,0.1);"/>
    <p><strong>Эта схема — упрощенная идеальная версия запроса.</strong></p>
  </div>

  <p><strong>Схема запроса для получения жалобы:</strong></p>
  <div align="center">
    <img src="https://devkarmanov.github.io/ImagesForFinderProject/imagesForGitHub/get_complaint_request.svg" alt="Схема работы запроса" style="width:80%; border-radius: 8px; box-shadow: 0px 4px 8px rgba(0,0,0,0.1);"/>
    <p><strong>Эта схема — упрощенная идеальная версия запроса.</strong></p>
  </div>

  <p><strong>Запрос на создание объявления:</strong></p>
  <div align="center">
    <img src="https://devkarmanov.github.io/ImagesForFinderProject/imagesForGitHub/add_card_request.svg" alt="Схема работы запроса" style="width:80%; border-radius: 8px; box-shadow: 0px 4px 8px rgba(0,0,0,0.1);"/>
    <p><strong>Эта схема — упрощенная идеальная версия запроса.</strong></p>
  </div>

  <p><strong>Запрос на удаление объявления:</strong></p>
  <div align="center">
    <img src="https://devkarmanov.github.io/ImagesForFinderProject/imagesForGitHub/delete_card_request.svg" alt="Схема работы запроса" style="width:80%; border-radius: 8px; box-shadow: 0px 4px 8px rgba(0,0,0,0.1);"/>
    <p><strong>Эта схема — упрощенная идеальная версия запроса.</strong></p>
  </div>

</details>

---

### Микросервис аутентификации
Управляет аутентификацией и авторизацией пользователей

*Для детальной документации API посетите Swagger UI:*
[Swagger UI - Микросервис аутентификации](http://localhost:8083/swagger-ui/index.html)

---

### Микросервис комментариев
Отвечает за управление комментариями

*Для детальной документации API посетите Swagger UI:*
[Swagger UI - Микросервис комментариев](http://localhost:8081/swagger-ui/index.html)

---

### Микросервис изображений
Необходим для работы с изображениями

*Для детальной документации API посетите Swagger UI:*
[Swagger UI - Микросервис изображений](http://localhost:8082/swagger-ui/index.html)
