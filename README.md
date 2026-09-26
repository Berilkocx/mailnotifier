# MailNotifier

**Smart email tracking and notification system with Gmail integration**

A Spring Boot application that lets users define email expectations ("I'm waiting for an email from X about Y") and automatically scans their Gmail inbox, matches incoming emails using flexible string matching and OpenAI-powered NLP analysis, and sends real-time notifications.

---

## Overview

MailNotifier solves a common problem: waiting for an important email and constantly refreshing your inbox. Instead, users define what they're expecting — a sender name, keywords, or a description — and the system continuously monitors Gmail, alerting them the moment a match is found.

**Key Features:**

- Secure Google OAuth 2.0 login with Gmail read-only access
- Flexible string matching (partial name, domain, organization name support)
- OpenAI GPT-4o-mini powered NLP content analysis and summarization
- Real-time notifications via WebSocket (STOMP + SockJS)
- Browser push notifications (Notification API)
- Confidence level scoring system (HIGH / MEDIUM / LOW)
- Turkish locale support (case-insensitive with İ/i handling)
- AES-256-GCM token encryption at rest
- RESTful API with Swagger UI documentation
- Thymeleaf + Bootstrap 5 responsive dashboard
- 56 unit tests (JUnit 5 + Mockito)

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Frontend                         │
│         Thymeleaf + Bootstrap 5 + WebSocket         │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                 Spring Boot 3.x                     │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │ Controllers  │  │  Services    │  │  Config    │ │
│  │ (REST + Page)│  │              │  │            │ │
│  └──────┬──────┘  └──────┬───────┘  └────────────┘ │
│         │                │                          │
│  ┌──────▼────────────────▼───────────────────────┐ │
│  │           Matching Engine                      │ │
│  │  ┌──────────────────┐ ┌─────────────────────┐ │ │
│  │  │ StringMatching   │ │ NlpMatching         │ │ │
│  │  │ Strategy (Primary│ │ Strategy (OpenAI)   │ │ │
│  │  └──────────────────┘ └─────────────────────┘ │ │
│  └───────────────────────────────────────────────┘ │
│                                                     │
│  ┌────────────┐  ┌─────────────┐  ┌──────────────┐ │
│  │ Gmail API  │  │ WebSocket   │  │ Scheduler    │ │
│  │ (OAuth2)   │  │ (STOMP)     │  │ (Scan Job)   │ │
│  └────────────┘  └─────────────┘  └──────────────┘ │
└──────────────────────┬──────────────────────────────┘
                       │
            ┌──────────▼──────────┐
            │    PostgreSQL       │
            │    (Flyway V1-V5)   │
            └─────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL 14+ |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway (V1–V5) |
| Authentication | Spring Security + OAuth 2.0 |
| Mail API | Gmail API (google-api-services-gmail) |
| NLP | OpenAI GPT-4o-mini |
| Encryption | AES-256-GCM |
| Real-time | WebSocket (STOMP + SockJS) |
| Frontend | Thymeleaf + Bootstrap 5 |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5 + Mockito (56 tests) |
| Build | Maven |

---

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL 14+
- Maven 3.9+
- Google Cloud Console account (for Gmail API access)
- OpenAI API key (optional, for NLP features)

### 1. Database Setup

```bash
psql postgres -c "CREATE USER mailnotifier WITH PASSWORD 'mailnotifier';"
psql postgres -c "CREATE DATABASE mailnotifier_db OWNER mailnotifier;"
psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE mailnotifier_db TO mailnotifier;"
```

### 2. Google Cloud Console Configuration

1. Create a new project at [Google Cloud Console](https://console.cloud.google.com)
2. Enable the Gmail API (APIs & Services → Library → Gmail API)
3. Configure OAuth consent screen (External, add test users)
4. Create OAuth 2.0 Client ID:
   - Application type: Web application
   - Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
5. Save the Client ID and Client Secret

### 3. Environment Variables

```bash
export GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
export GOOGLE_CLIENT_SECRET=your-client-secret
export OPENAI_API_KEY=sk-your-openai-key    # Optional (enables NLP features)
```

### 4. Run the Application

```bash
git clone https://github.com/Berilkocx/mailnotifier.git
cd mailnotifier
./mvnw spring-boot:run
```

The application starts at `http://localhost:8080`.

### 5. Run Tests

```bash
./mvnw test
```

---

## API Documentation

Swagger UI is available at:
```
http://localhost:8080/swagger-ui.html
```

### Endpoints

#### Expectations — `/api/expectations`
| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/expectations` | Create a new expectation |
| GET | `/api/expectations` | List active expectations |
| GET | `/api/expectations/{id}` | Get expectation details |
| PUT | `/api/expectations/{id}` | Update an expectation |
| DELETE | `/api/expectations/{id}` | Soft delete an expectation |
| POST | `/api/expectations/{id}/activate` | Reactivate an expectation |

#### Matches — `/api/matches`
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/matches` | List all matches |
| GET | `/api/matches?confidence=HIGH` | Filter by confidence level |
| GET | `/api/matches/{id}` | Get match details |
| DELETE | `/api/matches/{id}` | Dismiss a false match |

#### Notifications — `/api/notifications`
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/notifications` | List notifications |
| GET | `/api/notifications/unread-count` | Get unread count |
| PUT | `/api/notifications/{id}/read` | Mark as read |
| PUT | `/api/notifications/read-all` | Mark all as read |
| DELETE | `/api/notifications/{id}` | Delete a notification |

#### AI Analysis — `/api/ai`
| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/ai/analyze` | Analyze email content with AI |
| GET | `/api/ai/status` | Check AI service status |

---

## Matching Engine

### String Matching (Default)

Token-based flexible matching:

- **Sender matching:** Any part of a name, email, domain, or organization name is sufficient for a match
- **Keyword matching:** Case-insensitive partial matching in subject and body (with Turkish İ/i locale support)
- **Combined score:** `(senderScore × 0.4) + (keywordScore × 0.6)`

### NLP Matching (OpenAI)

Powered by OpenAI GPT-4o-mini:

- Semantic analysis of email content
- Similarity scoring between expectation and email
- Content summarization
- Intent detection (classifies email purpose)
- Graceful degradation: falls back to string matching when API key is unavailable

### Confidence Levels

| Level | Score Range | Description |
|-------|------------|-------------|
| HIGH | ≥ 0.7 | Strong match — very likely the expected email |
| MEDIUM | ≥ 0.4 | Possible match — email resembles the expectation |
| LOW | ≥ 0.15 | Weak match — email may be related |

---

## Project Structure

```
src/main/java/com/beril/mailnotifier/
├── config/
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   ├── AiConfig.java
│   └── MatchingConfig.java
├── controller/
│   ├── MailExpectationController.java
│   ├── MailMatchController.java
│   ├── NotificationController.java
│   ├── DashboardController.java
│   ├── AiController.java
│   └── ...
├── service/
│   ├── MailScanService.java
│   ├── StringMatchingStrategy.java
│   ├── NlpMatchingStrategy.java
│   ├── GmailMailProvider.java
│   ├── NotificationService.java
│   ├── OpenAiService.java
│   └── ...
├── model/
│   ├── User.java
│   ├── MailExpectation.java
│   ├── MailMatch.java
│   └── Notification.java
├── repository/
├── dto/
├── exception/
└── util/
    └── EncryptionUtil.java
```

---

## Design Decisions

**MailProvider interface abstraction:** Decouples the scanning logic from any specific email provider. Currently `GmailMailProvider` is active; the architecture supports adding `OutlookMailProvider` or others without modifying the core matching engine.

**Strategy pattern for matching:** `StringMatchingStrategy` is marked `@Primary` and always available. `NlpMatchingStrategy` activates when an OpenAI API key is configured, providing graceful degradation — the system works with or without AI capabilities.

**Flexible matching philosophy:** Users often don't know the exact sender email or subject line. The system uses probabilistic language ("this email may be what you're waiting for") rather than absolute statements, reducing false negatives while keeping users informed.

**AES-256-GCM encryption:** Google OAuth tokens are encrypted at rest in the database. Each encryption operation generates a unique initialization vector (IV).

**Flyway migrations:** Database schema is managed through 5 versioned migration files, ensuring reproducible deployments and schema version control.

---

## Security

- Google OAuth tokens encrypted with AES-256-GCM at rest
- Gmail API uses `gmail.readonly` scope only (read access, no write/delete permission)
- CSRF protection enabled (WebSocket endpoints excluded)
- Users can only access their own expectations, matches, and notifications
- API keys loaded from environment variables, never hardcoded
- Spring Security enforces authentication on all endpoints except login and static resources

---

## License

This project is licensed under the MIT License.
