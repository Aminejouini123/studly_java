# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the project
mvn clean install

# Run the application
mvn javafx:run

# Compile only
mvn compile
```

**Prerequisites:** Java 11+, MySQL 8.0+, Maven 3.6+

## Database Setup

1. Create MySQL database: `projet_db`
2. Run `schema.sql` to create all 16 tables
3. Credentials are hardcoded in `src/main/java/utils/MyDatabase.java`:
   - URL: `jdbc:mysql://localhost:3306/projet_db`
   - USER: `root`, PASSWORD: `""` (empty)

To create an admin user, run `test/CreateAdmin.java` or `test/AdminGenerator.java` directly.

There is no automated test suite — testing is manual via the GUI.

## Architecture Overview

**Studly** is a JavaFX 17 student LMS with two interfaces: a student frontend and an admin backend. The stack is JavaFX + JDBC + MySQL (no ORM).

### Layer Structure

- **`test/`** — Entry point: `AppLauncher` → `MainFX` (JavaFX Application) loads `/getion_user/auth_page.fxml`
- **`models/`** — Plain Java entity objects (User, Course, Activity, Exam, Event, Group, etc.)
- **`services/`** — All DB access via `IService<T>` CRUD interface; one service per entity using raw JDBC PreparedStatements
- **`controllers/`** — JavaFX FXML controllers; organized by feature subdirectory
- **`utils/`** — `MyDatabase` (singleton connection), `SessionManager` (current user), `DatabaseInitializer`
- **`resources/`** — FXML views organized by feature folder; CSS: `frontend_style.css`, `backend_style.css`

### Navigation Flow

Login (`auth_page.fxml`) → role check → either `frontend_dashboard.fxml` (students) or `backend_management.fxml` (admins). Controllers load child FXML panels into the dashboard's content area.

### Role System

`User` holds a `Role` field which is an abstract class with concrete subclasses: `Student`, `Admin`, `Teacher`. The DB stores roles as strings (`ROLE_STUDENT`, `ROLE_ADMIN`); conversion happens in `UserService`. Access control is checked via `user.getRole().hasPermission(...)` in controllers.

### Key Singletons

- `MyDatabase.getInstance()` — single JDBC connection with auto-reconnection
- `SessionManager.getCurrentUser()` — globally accessible logged-in user; set at login, cleared at logout

### Generic Service Interface

```java
interface IService<T> {
    void ajouter(T t) throws SQLException;    // create
    void modifier(T t) throws SQLException;   // update
    void supprimer(int id) throws SQLException; // delete
    List<T> recuperer() throws SQLException;  // read all
}
```

All service classes implement this; some add extra query methods (e.g., `findByUserId`).

### Password Security

Passwords are hashed with BCrypt (work factor 12) via `utils/PasswordUtil`:
- `PasswordUtil.hash(plain)` — call before any `user.setPassword()` (registration, admin add/edit)
- `PasswordUtil.verify(plain, hashed)` — used in `UserService.authenticateUser()`; login fetches by email only, then verifies in Java (never compares passwords in SQL)

Touch points that must always hash before persisting:
- `Sign_upController.handleSignUp()` — self-registration
- `AddUserController.handleSave()` — admin creates user
- `EditUserController.handleSave()` — admin resets a user's password

**Existing plain-text rows** in the DB will fail login after this change; those users must reset their passwords.

### FXML Resource Directories

| Directory | Feature |
|-----------|---------|
| `getion_user/` | Auth, login, signup, profile |
| `TEMPLATE/` | Main dashboard shells (frontend + backend) |
| `gestion_cours/` | Courses CRUD |
| `gestion_activites/` | Activities CRUD |
| `gestion_examen/` | Exams CRUD |
| `Gestion de temps/` | Planning, events, Pomodoro |
| `Groups/` | Group collaboration, messaging |