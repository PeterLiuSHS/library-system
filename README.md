# Library System

A microservices-based library management system built with Spring Boot, Docker, and Jenkins CI/CD pipeline.

This project demonstrates backend system design, microservices communication, automated testing, containerized deployment, and continuous integration workflows.

---

# Tech Stack

## Backend
- Java 21
- Spring Boot
- Spring Data JPA
- Hibernate

## Database
- MySQL

## DevOps & CI/CD
- Docker
- Docker Compose
- Jenkins
- GitHub

## Build Tool
- Maven

## Testing
- JUnit 5
- Spring Boot Test
- MockMvc

---

# System Architecture

```text
                +------------------+
                |      GitHub      |
                +------------------+
                          |
                          v
                +------------------+
                |     Jenkins      |
                +------------------+
                          |
                          v

+---------------------------------------------------+
|                 Docker Compose                    |
+---------------------------------------------------+

        +-------------+     +-------------+
        |  user-api   |     |  book-api   |
        +-------------+     +-------------+
                 \             /
                  \           /
                   \         /
                    v       v

                  +-------------+
                  |  loan-api   |
                  +-------------+
                         |
                         v
                  +-------------+
                  |    MySQL    |
                  +-------------+
```

---

# Microservices

## user-api

Handles user management operations.

### Features
- Create users
- Update users
- Delete users
- Search users
- User validation

---

## book-api

Handles book inventory management.

### Features
- Add books
- Update books
- Delete books
- Search books
- Track book availability

---

## loan-api

Handles borrowing and returning logic.

### Features
- Borrow books
- Return books
- Validate users through user-api
- Validate books through book-api
- Track active loans
- Prevent duplicate active loans

---

# Project Structure

```text
library-system/
├── user-api/
├── book-api/
├── loan-api/
├── mysql-init/
├── docker-compose.yml
├── Jenkinsfile
└── README.md
```

---

# CI/CD Pipeline

The Jenkins pipeline automatically performs the following steps:

1. Pull latest source code from GitHub
2. Build all microservices using Maven
3. Run unit and integration tests
4. Package executable JAR files
5. Build Docker images
6. Start all services using Docker Compose
7. Clean up containers after pipeline completion

---

# Testing

The project includes:

- Unit Tests
- Integration Tests
- Spring Boot Test
- MockMvc Testing

All tests are automatically executed during the Jenkins CI pipeline.

---

# API Examples

## Create User

```http
POST /users
```

### Request Body

```json
{
  "name": "Alice",
  "email": "alice@gmail.com"
}
```

---

## Get All Books

```http
GET /books
```

---

## Borrow Book

```http
POST /loans
```

### Request Body

```json
{
  "userId": 1,
  "bookId": 2
}
```

---

# Run Locally

## Build and Start System

```bash
docker compose up -d --build
```

---

## Stop System

```bash
docker compose down
```

---

# Jenkins Pipeline

The project uses Jenkins Pipeline with a Jenkinsfile stored in the GitHub repository.

The pipeline includes:

- Source code checkout
- Maven build
- Automated testing
- Docker image build
- Docker Compose deployment

---

# Future Improvements

- Add frontend application (React)
- Add API Gateway
- Add service discovery
- Add JWT authentication
- Add Swagger/OpenAPI documentation
- Add Kubernetes deployment
- Add monitoring and logging

---

# Author

Kexun Liu