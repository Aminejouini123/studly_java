# Studly — Student Learning Management System

Studly is a full-featured desktop LMS built with JavaFX 17. It provides a student-facing frontend and an admin backend, covering course management, planning, collaboration, AI-powered quizzes, and more — all backed by MySQL and a Python AI microservice.

---

## Features

### Authentication
- Email & password login with BCrypt hashing
- GitHub OAuth login
- Google OAuth login
- Face ID login powered by OpenCV

### Dashboard
- Personalized overview of courses, events, progress, and activity

### Event Management
- Create, edit, and delete events with categories (Education, Personal, etc.)
- Calendar view with location picker (map integration)
- Pomodoro timer with motivation tracking

### Planning & Quizzes
- Weekly planner with study session blocks
- AI-generated quizzes from course/activity content via OpenRouter
- Study plan recommendation powered by a Python FastAPI microservice

### Course Management
- Create, edit, publish, and browse courses
- Activity system with attached resources and objectives
- Exam scheduling
- In-course AI chatbot for student Q&A

### Group Collaboration
- Create and manage study groups
- SMS invitations to group members
- Real-time internal group chat
- Invitation inbox for pending invites

### Profile & Recommendations
- Update personal skills and academic profile
- AI-powered recommendation engine suggesting relevant courses and resources

### Roadmap
- Visual step-by-step learning path
- Track completed milestones and upcoming goals

### Admin Backend
- Full CRUD management for users, courses, activities, exams, groups, and events
- Admin overview dashboard with platform-wide stats

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | JavaFX 17, FXML, CSS |
| Database | MySQL 8.0 + raw JDBC (no ORM) |
| Security | BCrypt (jbcrypt), OpenCV face auth |
| OAuth | Google OAuth 2.0, GitHub OAuth |
| AI Services | OpenRouter API, Groq API |
| AI Microservice | Python FastAPI + Uvicorn |
| Email | Jakarta Mail via Gmail SMTP |
| PDF Processing | Apache PDFBox |
| Build | Maven 3.6+ |

---

## Prerequisites

- Java 11 or higher (Java 17 recommended)
- MySQL 8.0+
- Maven 3.6+
- Python 3.9+ (for the AI microservice)

---

## Setup

### 1. Database

```bash
# Create the database
mysql -u root -e "CREATE DATABASE projet_db;"

# Run the schema
mysql -u root projet_db < schema.sql
```

Database credentials are configured in `src/main/java/utils/MyDatabase.java`:
- URL: `jdbc:mysql://localhost:3306/projet_db`
- User: `root`
- Password: *(empty)*

### 2. Create an Admin User

```bash
# Run directly from the test directory
java test/CreateAdmin.java
# or
java test/AdminGenerator.java
```

### 3. Configure OAuth & AI Keys

Edit the following properties files in `src/main/resources/`:

| File | Purpose |
|---|---|
| `google-oauth.properties` | Google OAuth client ID & secret |
| `github-oauth.properties` | GitHub OAuth client ID & secret |
| `openrouter.properties` | OpenRouter API key for AI quiz/chat |
| `email.properties` | Gmail SMTP app password |

### 4. Start the Python AI Microservice

```bash
cd studly_api
pip install -r requirements.txt
uvicorn main:app --host 127.0.0.1 --port 8000
```

Create a `.env` file inside `studly_api/` with your Groq API key:

```
GROQ_API_KEY=your_key_here
```

### 5. Build & Run

```bash
# Build
mvn clean install

# Run
mvn javafx:run

# Compile only
mvn compile
```

---

## Project Structure

```
studly_java/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── controllers/        # JavaFX FXML controllers (65 files)
│   │   │   │   ├── user_controller/
│   │   │   │   ├── courses/
│   │   │   │   ├── activities/
│   │   │   │   ├── exams/
│   │   │   │   ├── group/
│   │   │   │   ├── gestiondetemps/
│   │   │   │   └── backend/
│   │   │   ├── models/             # Java entity classes (30 files)
│   │   │   ├── services/           # JDBC data access layer (27 files)
│   │   │   └── utils/              # Helpers: DB, session, auth, email
│   │   └── resources/
│   │       ├── TEMPLATE/           # Dashboard shell FXML + CSS
│   │       ├── getion_user/        # Auth, login, signup, profile FXML
│   │       ├── gestion_cours/      # Course views
│   │       ├── gestion_activites/  # Activity & quiz views
│   │       ├── gestion_examen/     # Exam views
│   │       ├── gestion_group/      # Group & chat views
│   │       ├── Gestion de temps/   # Planning, calendar, Pomodoro
│   │       ├── recommendations/    # Recommendation feed
│   │       └── roadmap/            # Roadmap visualization
├── studly_api/                     # Python FastAPI AI microservice
├── schema.sql                      # MySQL schema (16 tables)
├── test/                           # App entry point + admin setup utilities
└── pom.xml
```

---

## Architecture

```
┌────────────────────────────────────────┐
│     JavaFX UI (Controllers + FXML)     │
└────────────────────────────────────────┘
                   ↓
┌────────────────────────────────────────┐
│   Services Layer (JDBC PreparedStmt)   │
└────────────────────────────────────────┘
                   ↓
┌────────────────────────────────────────┐
│         MySQL Database (16 tables)     │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│   Python FastAPI (AI study planning)   │
│   Called by StudyApiClient.java        │
└────────────────────────────────────────┘
```

**Key singletons:**
- `MyDatabase.getInstance()` — single JDBC connection with auto-reconnect
- `SessionManager.getCurrentUser()` — globally accessible logged-in user

**Role system:**
- `User.role` holds a `Role` subclass: `Student`, `Admin`, or `Teacher`
- DB stores roles as strings: `ROLE_STUDENT`, `ROLE_ADMIN`
- Access control checked via `user.getRole().hasPermission(...)`

---

## Navigation Flow

```
auth_page.fxml (Login)
    ↓
Role check
    ├── Student → frontend_dashboard.fxml
    └── Admin   → backend_management.fxml
```

---

## Testing

There is no automated test suite. All testing is manual via the GUI. Use the entry points in `test/` to seed the database with an admin user before first run.

---

## License

This project was developed as part of an academic course. All rights reserved.
