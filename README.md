# Discord Slash-Command Bot

A Spring Boot web application that receives Discord slash-command interactions through a public HTTP endpoint, verifies Discord signatures, records commands in PostgreSQL, responds to users in Discord, mirrors notifications to Slack, and provides a login-protected admin dashboard for monitoring and command configuration.

## Features

* Discord slash commands:

  * `/status`
  * `/report <text>`
* Discord HTTP Interactions Endpoint
* Ed25519 signature verification
* Discord PING/PONG handling
* Duplicate interaction protection using Discord interaction IDs
* Request timestamp validation to reject stale requests
* Command logging in PostgreSQL
* Discord responses within the interaction response window
* Asynchronous Slack notifications using an Incoming Webhook
* Slack retry handling for temporary failures
* Login-protected admin dashboard
* Live command log with automatic refresh
* Action tracking for Discord and Slack operations
* Database-backed command configuration
* Enable/disable commands from the dashboard
* Enable/disable Slack mirroring from the dashboard
* Configurable Discord response messages
* Deployment on Render
* PostgreSQL database hosted on Neon

## Architecture

```text
Discord User
     |
     | /status or /report
     v
Discord HTTP Interaction
     |
     v
Render / Spring Boot
     |
     +----------------------+
     |                      |
     v                      v
Signature Verification   Command Configuration
     |                      |
     +----------+-----------+
                |
                v
         Save Command Log
         to Neon PostgreSQL
                |
        +-------+--------+
        |                |
        v                v
 Discord response    Async Slack mirror
        |                |
        v                v
   Discord Server     Slack Channel
                |
                v
        Admin Dashboard
```

## Technology Stack

* Java 17
* Spring Boot
* Spring MVC
* Spring Security
* Spring Data JPA / Hibernate
* PostgreSQL
* Neon PostgreSQL
* Maven
* Discord Interactions API
* Slack Incoming Webhooks
* HTML/CSS/JavaScript
* Render

## Project Structure

The main application code is under:

```text
src/
└── main/
    ├── java/com/example/project/
    │   ├── ProjectApplication.java
    │   ├── DiscordInteractionController.java
    │   ├── DiscordSignatureVerifier.java
    │   ├── CommandLog.java
    │   ├── CommandLogRepository.java
    │   ├── CommandConfig.java
    │   ├── CommandConfigRepository.java
    │   ├── DiscordMirrorService.java
    │   ├── AdminDashboardController.java
    │   └── SecurityConfig.java
    │
    └── resources/
        ├── application.properties
        └── static/
            └── dashboard.html
```

## How the Discord Interaction Flow Works

When a user executes a slash command in Discord, Discord sends a signed HTTP POST request to:

```text
POST /api/discord/interactions
```

The application:

1. Validates the Discord request signature.
2. Validates the request timestamp.
3. Handles Discord PING requests.
4. Identifies the slash command.
5. Reads the command configuration from PostgreSQL.
6. Rejects disabled or unconfigured commands.
7. Checks the interaction ID to prevent duplicate processing.
8. Records the command in PostgreSQL.
9. Responds to the Discord user.
10. Starts the Slack mirror asynchronously when configured.

The Slack operation is asynchronous so a downstream notification does not unnecessarily delay the Discord interaction response.

## Commands

### `/status`

Returns the configured status response.

Example:

```text
Bot is running! ✅
```

The dashboard can control:

* Whether `/status` is enabled
* Its response message
* Whether it is mirrored to Slack

### `/report <text>`

Records the supplied report text and returns a configurable response.

Example:

```text
/report database is unavailable
```

Response:

```text
Report received: database is unavailable
```

When mirroring is enabled, the same notification is sent to the configured Slack channel.

## Database

The application uses PostgreSQL hosted on Neon.

Two main tables are used.

### `command_logs`

Stores received Discord commands and their actions.

Typical fields include:

```text
id
interaction_id
command_name
user_id
channel_id
text
action_taken
created_at
```

### `command_configs`

Stores command behavior.

Typical fields include:

```text
id
command_name
enabled
response_message
mirror_enabled
```

Hibernate updates the schema automatically using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

## Admin Dashboard

The dashboard is protected by Spring Security login.

After authentication, the admin can view:

### Command activity

* Total commands
* Report count
* Status count

### Command configuration

For each command, the admin can configure:

* Enabled / disabled
* Response message
* Slack mirroring enabled / disabled

### Command logs

The dashboard displays:

* Command ID
* Command name
* Discord user ID
* Discord channel ID
* Report text
* Action taken
* Timestamp

The command log automatically refreshes every 10 seconds.

## Environment Variables

Secrets are not committed to the repository.

Create your local environment variables before running the application.

Required variables:

```text
DISCORD_PUBLIC_KEY=
ADMIN_USERNAME=
ADMIN_PASSWORD=
DATABASE_URL=
DB_USERNAME=
DB_PASSWORD=
DISCORD_MIRROR_WEBHOOK_URL=
```

### Variable descriptions

| Variable                     | Purpose                                                                |
| ---------------------------- | ---------------------------------------------------------------------- |
| `DISCORD_PUBLIC_KEY`         | Discord application's public key used for Ed25519 request verification |
| `ADMIN_USERNAME`             | Admin dashboard username                                               |
| `ADMIN_PASSWORD`             | Admin dashboard password                                               |
| `DATABASE_URL`               | Neon PostgreSQL JDBC connection URL                                    |
| `DB_USERNAME`                | Neon PostgreSQL username/role                                          |
| `DB_PASSWORD`                | Neon PostgreSQL password                                               |
| `DISCORD_MIRROR_WEBHOOK_URL` | Slack Incoming Webhook URL                                             |

Example PostgreSQL JDBC URL:

```text
jdbc:postgresql://<host>/<database>?sslmode=require
```

Do not commit real credentials, tokens, database passwords, or webhook URLs.

## Local Setup

### Prerequisites

Install:

* Java 17
* Maven
* Git

### Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd Discord-Slash-Command-Bot/project
```

### Configure environment variables

For a Linux/macOS/Codespaces terminal:

```bash
export DISCORD_PUBLIC_KEY="your-public-key"
export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="your-password"
export DATABASE_URL="jdbc:postgresql://your-host/your-database?sslmode=require"
export DB_USERNAME="your-database-user"
export DB_PASSWORD="your-database-password"
export DISCORD_MIRROR_WEBHOOK_URL="your-slack-webhook-url"
```

For local Windows development, configure the same variables in the operating system environment or through your IDE.

### Build

```bash
mvn clean package -DskipTests
```

### Run

```bash
mvn spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

The dashboard is available at:

```text
http://localhost:8080/dashboard.html
```

The Discord interactions endpoint is:

```text
http://localhost:8080/api/discord/interactions
```

Discord cannot call a localhost endpoint, so a public deployment is required for actual Discord interaction testing.

## Discord Configuration

Create an application in the Discord Developer Portal.

Configure:

1. Application public key
2. Bot
3. Slash commands
4. Interactions Endpoint URL

The production endpoint is:

```text
https://discord-slash-command-bot-hxfk.onrender.com/api/discord/interactions
```

The Discord application must be configured to send interactions to the deployed endpoint.

At least these commands are registered:

```text
/status
/report <text>
```

The bot must be added to the Discord test server with the required permissions.

## Slack Configuration

The application uses a Slack Incoming Webhook for the second-channel notification requirement.

Create a Slack app, enable Incoming Webhooks, install the app into the workspace, and create a webhook for the target channel.

Store the webhook URL only as:

```text
DISCORD_MIRROR_WEBHOOK_URL
```

The URL must never be exposed in frontend code or committed to GitHub.

## Deployment

The application is deployed as a web service on Render.

Production deployment flow:

```text
GitHub repository
        |
        v
Render
        |
        v
Spring Boot application
        |
        +----> Neon PostgreSQL
        |
        +----> Slack Incoming Webhook
        |
        +----> Discord Interactions Endpoint
```

### Render configuration

Configure the required environment variables in:

```text
Render → Service → Environment
```

The application uses Render's provided `PORT` value when available and falls back to port `8080` locally.

The deployed application must remain publicly reachable because Discord sends interaction requests to the production URL.

## Testing

### Test `/status`

In the Discord server:

```text
/status
```

Expected:

```text
Bot is running! ✅
```

When Slack mirroring is enabled for `/status`, the same notification is also sent to the configured Slack channel.

### Test `/report`

In the Discord server:

```text
/report test message
```

Expected:

```text
Report received: test message
```

The command should also:

* Create a row in `command_logs`
* Appear on the dashboard
* Record the action taken
* Send the notification to Slack when mirroring is enabled

### Test command configuration

From the dashboard:

1. Disable a command.
2. Save the configuration.
3. Execute the command in Discord.

The command should report that it is disabled.

Re-enable the command and test again.

### Test Slack mirroring

Enable:

```text
Mirror to Slack = ON
```

Run `/report`.

Expected:

```text
Discord → response
PostgreSQL → command log
Slack → mirrored notification
Dashboard → action status
```

Disable Slack mirroring and repeat the command.

The Discord response should still work, but no Slack notification should be sent.

## Reliability and Security

The application includes several protections required by the exercise.

### Discord signature verification

Every interaction request is verified using the Discord Ed25519 signature headers:

```text
X-Signature-Ed25519
X-Signature-Timestamp
```

### Timestamp validation

Requests older than the configured freshness window are rejected.

### Duplicate protection

The Discord interaction ID is stored in PostgreSQL and checked before processing.

This prevents the same interaction from being processed more than once.

### Asynchronous Slack notification

Slack mirroring is executed asynchronously so the Discord interaction response does not wait for the downstream notification.

### Slack retry handling

Temporary Slack failures can be retried.

Failures are recorded in the command log so they are visible in the dashboard.

### Secret management

Sensitive values are supplied through environment variables instead of source code.

Do not expose:

* Discord bot tokens
* Discord public/private credentials
* Database passwords
* Slack webhook URLs
* Admin passwords

## Example Environment File

Create a local `.env.example` containing variable names only:

```text
DISCORD_PUBLIC_KEY=
ADMIN_USERNAME=
ADMIN_PASSWORD=
DATABASE_URL=
DB_USERNAME=
DB_PASSWORD=
DISCORD_MIRROR_WEBHOOK_URL=
```

Never place real values in this file.

## Future Improvements

Possible future improvements include:

* Interactive Discord buttons and components
* Modal-based `/report`
* AI-based report classification or summarization
* Multi-server configuration
* More detailed observability
* Persistent retry queues for downstream notifications
* PostgreSQL migrations using Flyway or Liquibase
* Role-based dashboard access
