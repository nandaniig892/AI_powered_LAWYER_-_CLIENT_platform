# LegalConnect

LegalConnect is a Java EE web application that connects clients and lawyers with case management, chat, notifications, admin moderation, and AI-assisted legal support.

## Highlights

- Multi-role authentication (`client`, `lawyer`, `admin`)
- Client workflows: register, upload cases, track progress, review lawyers, submit complaints
- Lawyer workflows: discover pending cases, accept/manage active cases, update status/timeline
- Shared chat per case with optional file attachments
- Notification center with read/unread tracking
- Admin panel for lawyer verification, complaints handling, and audit logs
- AI support endpoint with provider mode + built-in fallback responses

## Tech Stack

- Java 8
- Java EE Web (Servlet/JSP)
- Apache Ant / NetBeans web project
- MySQL (JDBC: `mysql-connector-j-9.1.0`)
- Frontend: JSP/HTML/CSS/Vanilla JS

## Project Structure

```text
LegalConnent/
  src/java/               # Servlets + utility classes
  web/                    # JSP/HTML views and static assets
    WEB-INF/web.xml       # Servlet declarations + mappings
  nbproject/              # NetBeans project metadata
  build.xml               # Ant build entrypoint
```

## Prerequisites

- JDK 8+
- MySQL 8+
- GlassFish/Payara server compatible with Java EE web apps
- NetBeans (recommended) or Ant CLI

## Configuration

### 1) Database connection

Configure database credentials via environment variables (or JVM system properties):

```bash
# Linux/macOS
export LEGALCONNECT_DB_URL="jdbc:mysql://localhost:3306/legalconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export LEGALCONNECT_DB_USER="your_db_user"
export LEGALCONNECT_DB_PASSWORD="your_db_password"
```

```powershell
# Windows PowerShell (current session)
$env:LEGALCONNECT_DB_URL="jdbc:mysql://localhost:3306/legalconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:LEGALCONNECT_DB_USER="your_db_user"
$env:LEGALCONNECT_DB_PASSWORD="your_db_password"
```

`DBConnectionUtil` now requires these keys:

- `LEGALCONNECT_DB_URL`
- `LEGALCONNECT_DB_USER`
- `LEGALCONNECT_DB_PASSWORD`

Create the database:

```sql
CREATE DATABASE legalconnect_db;
```

> Note: Feature tables (`case_timeline`, `notifications`, `case_messages`, `lawyer_reviews`, `complaints`, `admin_logs`) are auto-created at runtime by `FeatureSchemaUtil`.

### 2) AI support (optional)

Configure Gemini via environment variables (or JVM system properties):

```bash
# Linux/macOS
export GEMINI_API_KEY="your_gemini_api_key"
export GEMINI_MODEL="gemini-2.0-flash"
export GEMINI_ENDPOINT="https://generativelanguage.googleapis.com/v1beta/models"
```

```powershell
# Windows PowerShell (current session)
$env:GEMINI_API_KEY="your_gemini_api_key"
$env:GEMINI_MODEL="gemini-2.0-flash"
$env:GEMINI_ENDPOINT="https://generativelanguage.googleapis.com/v1beta/models"
```

If `GEMINI_API_KEY` is empty, the app automatically uses a grounded fallback mode.

AI analysis now includes:

- Grounded case summarization (case fields + extractable document text)
- Applicable rule hints from built-in legal knowledge notes
- Case strength and proof-required checklist
- Confidence + insufficient-evidence gate with strict response behavior
- Lawyer recommendation scoring (city, specialization, ratings, experience, similarity)

## Running the App

### Option A: NetBeans (recommended)

1. Open this folder as a NetBeans project.
2. Configure your application server (GlassFish/Payara).
3. Build and run.
4. Open:
   - `http://localhost:8080/LegalConnect/`

### Option B: Ant build

```bash
ant clean
ant
```

Deploy the generated WAR (typically `dist/LegalConnect.war`) to your Java EE server.

## Core Endpoints (sample)

- Auth: `LoginServlet`, `LogoutServlet`, `ClientRegisterServlet`, `LawyerRegisterServlet`
- Client: `UploadCaseServlet`, `GetCasesServlet`, `GetClientCaseTrackerServlet`, `SubmitReviewServlet`, `SubmitComplaintServlet`
- Lawyer: `GetNewCasesServlet`, `AcceptCaseServlet`, `GetActiveCasesServlet`, `UpdateCaseStatusServlet`, `GetLawyerStatsServlet`
- Messaging: `GetCaseChatListServlet`, `GetCaseMessagesServlet`, `SendCaseMessageServlet`
- Admin: `GetAdminStatsServlet`, `GetAdminLawyersServlet`, `AdminLawyerActionServlet`, `GetAdminComplaintsServlet`, `UpdateComplaintStatusServlet`, `GetAdminLogsServlet`
- Shared: `GetNotificationsServlet`, `MarkNotificationsReadServlet`, `GetCaseTimelineServlet`, `AiSupportServlet`

Full servlet mappings are defined in `web/WEB-INF/web.xml`.

## Current Known Gaps

- `Forgot Password` link in `web/login.html` is currently a placeholder.
- Terms/Privacy/Code of Conduct links in registration pages are placeholders.
- `Remember me` is present in UI but not persisted in backend login logic.

## Security Notes

- Do not commit DB passwords or API keys.
- Use environment variables or secret managers for all credentials.
- Add password reset, email verification, and stronger auth controls before production rollout.

## License

No license file is currently included in this repository.
