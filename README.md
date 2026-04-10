# Articles Demo (Spring Boot + Kafka + Redis + Postgres)

A Spring Boot 3.3 demo application implementing an **event-driven, eventually consistent article system** using **Kafka**, **PostgreSQL**, **Redis caching**, and **Redisson distributed locking**.

---

## Overview

This project demonstrates a scalable backend architecture where:

- `POST /api/articles` publishes article creation requests to Kafka
- Kafka consumer persists data into PostgreSQL
- Redis is used as a read-through cache
- Redisson distributed lock prevents cache stampede
- Reads are optimized with caching + fallback DB lookup

---

##  Architecture

### Write Flow
1. Client sends `POST /api/articles`
2. Request is validated
3. Producer publishes message to Kafka topic: `article-topic`
4. Consumer processes message:
  - Checks for duplicates
  - Saves to PostgreSQL
  - Invalidates Redis cache key `article:{title}`

### Read Flow
1. Client calls `GET /api/articles/{title}`
2. Service checks Redis cache first
3. If cache miss:
  - Acquires Redisson lock (`lock:{title}`)
  - Loads from PostgreSQL
  - Stores result in Redis (TTL: 10 minutes)

---

## Tech Stack

- Java 17+
- Spring Boot 3.3
- Spring Web
- Spring Data JPA
- Apache Kafka
- PostgreSQL
- Redis
- Redisson
- Lombok

---

##  Prerequisites

Make sure you have the following services running locally:

- PostgreSQL → `localhost:5432`
- Kafka → `localhost:9092`
- Redis → `localhost:6379`

### Default Database Configuration
- DB: postgres
- User: postgres
- Password: pass

---

## Running the Application

### 1. Clone the repository

```bash
git clone https://github.com/your-username/articles-demo.git
cd articles-demo
```

### 2. Build Project

```bash
./mvnw clean install
```

### 3. Run Application

```bash
./mvnw spring-boot:run
```

## Api Documentation
### Create Article
- **Endpoint**: `POST /api/articles`
- **Request Body**:

```bash 
    {
      "title": "My First Article",
      "content": "This is the content of the article."
    }
```


- **Response**: `202 Accepted`
```bash
  {
      "success": true,
      "message": "SUCCESS",
      "data": "Article queued for processing"
  } 
```

- **Error Responses**:
- `400 Bad Request` if title or content is missing
- `409 Conflict` if an article with the same title already exists

### Get Article
- **Endpoint**: `GET /api/articles/{title}`
- **Response**:
- `200 OK` with article data if found
```bash 
    { 
        "success": true,
        "message": "SUCCESS",
        "data": {
            "title": "My First Article",
            "description": "This is the content of the article."
        }
    }
```

- `404 Not Found` if article does not exist 

## Key Features

- **Event-Driven Architecture**: Decouples write and read operations using Kafka
- **Eventually Consistent**: Data may not be immediately available after creation, but will be
- **Redis Caching**: Improves read performance with a cache layer
- **Redisson Distributed Lock**: Prevents cache stampede on high concurrency
- **Spring Boot 3.3**: Leverages latest Spring features and improvements
- **PostgreSQL**: Reliable relational database for persistence
- **Lombok**: Reduces boilerplate code for models and services
- **Error Handling**: Graceful handling of validation and processing errors
- **Scalable Design**: Can be easily extended to support more features (e.g., article updates, user authentication)
- **Unit Tests**: Basic tests for service and controller layers (not included in this demo but can be added)
- **Idempotent Kafka producer**: Ensures duplicate messages are not processed
- **Case-insensitive search**: Article titles are treated as case-insensitive for both creation and retrieval

## Testing
```bash
    mvn test
```

## Notes
- System is eventually consistent
- Kafka ensures durability and async processing
- Redis is a performance layer, not a source of truth
- PostgreSQL remains the primary database

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
