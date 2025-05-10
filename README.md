# Overview

This is a backend for a platform that allows users to create adverts to find teams for their projects or join existing ones. The goal of the platform is to help users find opportunities to gain teamwork experience and improve their skills.

Anyone can participate and borrow code, but it's important to respect the license terms.

[Читать на русском](README.ru.md)

<details>
  <summary><strong>Currently Implemented:</strong></summary>

 ### Ad Management
  - **Create, edit, and delete ads.**
  - **View ad listings.**
  - **Add or remove ads from favorites.**
  - **Integration with the ElasticSearch engine.**

### Comment and Reply Management
  - **Create, edit, and delete comments on ads.**
  - **Add and manage reply comments.**

### User Profile and Account Management
  - **User registration and login.**
  - **Edit user profile information.**
  - **Update profile avatar.**
  - **View user profile.**
  - **Delete user account.**
  - **User subscriptions.**

### Access Control and Security
  - **Access token validation.**
  - **Issue a new access token using a refresh token.**

### Admin Tools and Complaint Management
  - **Submit complaints about users or ads.**
  - **Admin privileges include:**
    - Viewing and removing complaints.
    - User moderation (ban/unban, role adjustment).
    - Admin registration key management.
  
### Email Notifications
  - **Sending messages to email (for password recovery or notifications).**
</details>

---

<details>
  <summary><strong>🚀Quick Start:</strong></summary>

1. Fill in the `.env` file.  
2. Start the containers.  
3. Create indices in ElasticSearch using the following configuration:

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
````

4. Set up the SMTP service.

</details>

---

### Settings

| **Environment Variable**       | **Description**                                                                                           |
|--------------------------------|-----------------------------------------------------------------------------------------------------------|
| `X_API_KEY`                    | The key for connecting internal APIs to each other, which should not be accessible to outsiders          |
| `ELASTIC_USERNAME`             | Username for your ElasticSearch instance                                                                 |
| `ELASTIC_PASSWORD`             | Password for your ElasticSearch user                                                                     |
| `POSTGRES_DB`                  | The name of your PostgreSQL database                                                                     |
| `POSTGRES_USER`                | Login for your PostgreSQL user                                                                           |
| `POSTGRES_PASSWORD`            | Password for your PostgreSQL user                                                                        |
| `MINIO_ROOT_USER`              | Login for your MinIO instance                                                                            |
| `MINIO_ROOT_PASSWORD`          | Password for your MinIO instance                                                                         |
| `RABBITMQ_DEFAULT_USER`        | Login for RabbitMQ                                                                                       |
| `RABBITMQ_DEFAULT_PASS`        | Password for RabbitMQ                                                                                    |
| `SMTP_MAIL`                    | Email address for SMTP                                                                                   |
| `SMTP_PASSWORD`                | Password for the SMTP email account                                                                      |
| `SMTP_HOST`                    | Host for your SMTP server                                                                                |
| `SMTP_PORT`                    | Port for your SMTP server                                                                                |
| `REDIS_PASSWORD`               | Password for your Redis                                                                                  |
| `SEND_EMAIL_MESSAGE`           | Whether to send messages to the mail about successful publication of the advertisement (`true/false`)    |

---

# Microservices
> ⚠️ **Note:** The diagrams presented do not describe the work of all queries, but only those that are unique in their implementation. If there is no diagram for a query, it means that it is almost or completely described by a similar one. (Not all the diagrams are ready)

### Ads Microservice
Manages announcements and their content, including user complaints

*For detailed API documentation, visit the Swagger UI at:*
[Swagger UI - Ads Microservice](http://localhost:8080/swagger-ui/index.html)

<details>
  <summary><strong>Endpoints Scheme:</strong></summary>

  <p><strong>Request scheme for receiving an ad:</strong></p>
  <div align="center">
    <img src="https://devkarmanov.github.io/ImagesForFinderProject/imagesForGitHub/get_card_request.svg" alt="Scheme of operation of the request" style="width:80%; border-radius: 8px; box-shadow: 0px 4px 8px rgba(0,0,0,0.1);"/>
    <p><strong>This scheme is a simplified ideal query option.</strong></p>
  </div>

  <p><strong>Request scheme for receiving a complaint:</strong></p>
  <div align="center">
    <img src="https://devkarmanov.github.io/ImagesForFinderProject/imagesForGitHub/get_complaint_request.svg" alt="Scheme of operation of the request" style="width:80%; border-radius: 8px; box-shadow: 0px 4px 8px rgba(0,0,0,0.1);"/>
    <p><strong>This scheme is a simplified ideal query option.</strong></p>
  </div>

  <p><strong>Request to create an advertisement:</strong></p>
  <div align="center">
    <img src="https://devkarmanov.github.io/ImagesForFinderProject/imagesForGitHub/add_card_request.svg" alt="Scheme of operation of the request" style="width:80%; border-radius: 8px; box-shadow: 0px 4px 8px rgba(0,0,0,0.1);"/>
    <p><strong>This scheme is a simplified ideal query option.</strong></p>
  </div>

  <p><strong>Request to delete an advertisement:</strong></p>
  <div align="center">
    <img src="https://devkarmanov.github.io/ImagesForFinderProject/imagesForGitHub/delete_card_request.svg" alt="Scheme of operation of the request" style="width:80%; border-radius: 8px; box-shadow: 0px 4px 8px rgba(0,0,0,0.1);"/>
    <p><strong>This scheme is a simplified ideal query option.</strong></p>
  </div>

</details>

---

### Authentication Microservice
Manages user authentication and authorization

*For detailed API documentation, visit the Swagger UI at:*
[Swagger UI - Authentication Microservice](http://localhost:8083/swagger-ui/index.html)

---

### Comment Microservice
Responsible for comment management

*For detailed API documentation, visit the Swagger UI at:*
[Swagger UI - Comment Microservice](http://localhost:8081/swagger-ui/index.html)

---

### Image Microservice
Necessary for working with images

*For detailed API documentation, visit the Swagger UI at:*
[Swagger UI - Image Microservice](http://localhost:8082/swagger-ui/index.html)

