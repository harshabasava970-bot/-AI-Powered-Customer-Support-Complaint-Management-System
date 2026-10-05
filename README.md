# 🎧 AI-Powered Customer Support & Complaint Management System

A production-ready, full-stack Java Spring Boot application featuring rule-based NLP complaint classification, role-based access control, and a complete complaint lifecycle workflow.

---

## 🚀 Live Demo

- **Application URL:** _(add your Render URL here after deployment)_
- **Admin credentials:** `admin` / `Admin@1234`

---

## ✨ Features

### Core
- 3 user roles: **Customer**, **Support Agent**, **Admin**
- Full complaint lifecycle: NEW → ASSIGNED → IN_PROGRESS → WAITING_FOR_CUSTOMER → RESOLVED → CLOSED
- AI/NLP-powered complaint classification (category + priority prediction)
- Status history timeline with audit trail
- Customer feedback and star rating system
- Overdue complaint detection (48-hour threshold)

### AI/NLP Engine
- Rule-based keyword scoring across 9 categories
- 4-tier priority prediction (CRITICAL / HIGH / MEDIUM / LOW)
- Human-readable explanation for every prediction
- Confidence score (0–1) per category prediction
- No paid API — fully deterministic and testable

### Security
- Spring Security 6 with BCrypt password hashing (strength 12)
- Session-based authentication
- CSRF protection (disabled only for `/api/**`)
- Role-based URL authorization + method-level `@PreAuthorize`
- Custom login/logout flow with role-based redirects

---

## 🛠️ Technology Stack

| Layer       | Technology                              |
|-------------|------------------------------------------|
| Language    | Java 21                                  |
| Framework   | Spring Boot 3.2.5                        |
| Security    | Spring Security 6                        |
| Persistence | Spring Data JPA + Hibernate              |
| Database    | MySQL 8                                  |
| Frontend    | Thymeleaf + HTML + CSS + JavaScript      |
| Build       | Maven                                    |
| Testing     | JUnit 5 + Mockito                        |
| Container   | Docker (multi-stage build)               |
| Deployment  | Render (backend) + Aiven (MySQL)         |

---

## 🗃️ Database Schema

```
users               → id, username, email, password, first_name, last_name, phone, enabled
roles               → id, name (ROLE_CUSTOMER / ROLE_AGENT / ROLE_ADMIN)
user_roles          → user_id, role_id
complaint_categories→ id, name, description, active
complaints          → id, title, description, status, priority, category_id, customer_id,
                       assigned_agent_id, ai_predicted_*, resolution_notes, resolved_at, ...
complaint_status_history → id, complaint_id, old_status, new_status, changed_by_user_id, notes
complaint_feedback  → id, complaint_id, rating, comment, submitted_at
```

---

## 📦 Project Structure

```
src/main/java/com/supportportal/
├── app/            → SupportSystemApplication.java
├── config/         → DataInitializer (seeds roles, admin, categories)
├── controller/     → MVC controllers (Auth, Customer, Agent, Admin, Home)
│   └── api/        → REST API controllers
├── dto/            → Request/Response DTOs
├── entity/         → JPA entities
├── exception/      → Custom exceptions + GlobalExceptionHandler
├── repository/     → Spring Data JPA repositories
├── security/       → CustomUserDetails, UserDetailsService, SecurityConfig
└── service/        → Business logic (UserService, ComplaintService,
                       ComplaintAnalysisService, AdminService, CategoryService)
```

---

## 🔌 Key REST APIs

| Method | Endpoint                          | Role            | Description              |
|--------|-----------------------------------|-----------------|--------------------------|
| POST   | `/api/auth/register`              | Public          | Register customer        |
| POST   | `/api/complaints`                 | CUSTOMER        | Create complaint         |
| GET    | `/api/complaints`                 | ADMIN / AGENT   | List all (filterable)    |
| GET    | `/api/complaints/mine`            | CUSTOMER        | My complaints            |
| GET    | `/api/complaints/{id}`            | Authenticated   | Complaint detail         |
| PUT    | `/api/complaints/{id}/status`     | AGENT / ADMIN   | Update status            |
| PUT    | `/api/complaints/{id}/assign`     | ADMIN           | Assign agent             |
| PUT    | `/api/complaints/{id}/resolve`    | AGENT           | Resolve complaint        |
| POST   | `/api/complaints/{id}/feedback`   | CUSTOMER        | Submit feedback          |
| POST   | `/api/complaints/analyze`         | Authenticated   | Run AI analysis          |
| GET    | `/api/admin/dashboard`            | ADMIN           | Dashboard stats          |
| GET    | `/api/admin/agents/stats`         | ADMIN           | Agent performance        |

---

## ⚙️ Local Setup

### Prerequisites
- Java 21
- Maven 3.8+
- MySQL 8 running locally

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/YOUR_USERNAME/support-system.git
cd support-system

# 2. Create the database (optional — auto-created via createDatabaseIfNotExist)
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS support_db;"

# 3. Set environment variables (or edit application.properties)
export DB_URL=jdbc:mysql://localhost:3306/support_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
export DB_USERNAME=root
export DB_PASSWORD=your_password

# 4. Build and run
mvn clean package -DskipTests
java -jar target/support-system-1.0.0.jar
```

App starts at **http://localhost:8080**

Default admin: `admin` / `Admin@1234`

---

## 🐳 Docker

```bash
# Build and start everything (app + MySQL)
docker-compose up --build

# Stop
docker-compose down
```

Or build the image only:
```bash
docker build -t support-system .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/support_db \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=yourpassword \
  support-system
```

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Test coverage report
mvn test jacoco:report
```

Test classes:
- `ComplaintAnalysisServiceTest` — 15 tests for NLP engine
- `ComplaintServiceTest` — 12 tests for business logic
- `UserServiceTest` — 7 tests for registration, auth, validation

---

## ☁️ Deployment (Render + Aiven)

### Database — Aiven MySQL (free tier)
1. Sign up at [aiven.io](https://aiven.io)
2. Create a free MySQL service
3. Copy the connection URI

### Backend — Render (free tier)
1. Push this repo to GitHub
2. Go to [render.com](https://render.com) → New Web Service
3. Connect your GitHub repo
4. Set **Runtime** to `Docker`
5. Add environment variables:
   ```
   DB_URL      = jdbc:mysql://<aiven-host>:<port>/support_db?useSSL=true&...
   DB_USERNAME = <aiven-user>
   DB_PASSWORD = <aiven-password>
   PORT        = 8080
   ```
6. Deploy — Render builds the Docker image and starts the service

---

## 🤖 AI/NLP Design (Interview Notes)

The `ComplaintAnalysisService` uses a **weighted keyword scoring** approach:

1. **Category classification**: Each of 9 categories has a keyword dictionary with weights (1–7). The complaint text is lowercased and every keyword is searched. Scores are summed per category. The winner is the category with the highest total score.

2. **Priority prediction**: Four tiers (CRITICAL → LOW) each have indicator phrases. The highest-matching tier wins.

3. **Confidence score**: `winner_score / total_score_across_all_categories`

4. **Explainability**: Every prediction stores the matched keywords and a plain-English explanation string.

No ML model, no paid API — fully deterministic and unit-tested.

---

## 🔮 Future Enhancements

- JWT-based stateless API authentication
- Email notifications on status changes
- File attachment support for complaints
- Real-time updates via WebSockets
- Machine learning model (trained on historical data) replacing the rule-based engine
- REST API rate limiting
- Swagger/OpenAPI documentation

---

## 👤 Author

Built as a major Java-focused resume project demonstrating:
- Spring Boot architecture (Controller → Service → Repository)
- Spring Security 6 with role-based access control
- JPA/Hibernate with MySQL
- Clean OOP + SOLID principles
- Unit testing with JUnit 5 + Mockito
- Docker containerization
- Free-tier cloud deployment
