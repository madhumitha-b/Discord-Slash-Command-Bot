# Discord Slash-Command Bot

A Spring Boot web application that receives Discord slash-command interactions through a public HTTP endpoint, verifies Discord requests, records commands in PostgreSQL, responds to users in Discord, mirrors notifications to Slack, and provides a login-protected admin dashboard for monitoring and configuring command behavior.

## Features

* Discord slash commands:

  * `/status`
  * `/report <text>`
* Public Discord HTTP Interactions Endpoint
* Ed25519 signature verification
* Discord PING/PONG handling
* Duplicate interaction protection using Discord interaction IDs
* Request timestamp validation
* PostgreSQL persistence using Neon
* Login-protected admin dashboard
* Live command log
* Action tracking
* Database-backed command configuration
* Enable/disable commands from the dashboard
* Configurable command response messages
* Enable/disable Slack mirroring from the dashboard
* Asynchronous Slack Incoming Webhook notifications
* Slack retry handling for temporary failures
* Deployment using Render

---

## Architecture

```text
                         Discord Server 1
                                |
                         /status /report
                                |
                                v
                    Discord HTTP Interaction
                                |
                                v
                         Render / Spring Boot
                                |
                    +-----------+-----------+
                    |                       |
                    v                       v
             Signature Check        Command Configuration
                    |                 from PostgreSQL
                    +-----------+-----------+
                                |
                                v
                         Process Command
                                |
                    +-----------+-----------+
                    |                       |
                    v                       v
             Neon PostgreSQL        Discord Response
                    |                       |
                    |                       v
                    |                 Discord User
                    |
                    +-----------------------+
                                            |
                                            v
                                  Async Slack Webhook
                                            |
                                            v
                                       Slack Channel
                                           
                               
                         Admin Dashboard
                                |
                                v
                         Command Logs
                         + Actions
                         + Configuration
```

---

## Technology Stack

* Java 17
* Spring Boot
* Spring MVC
* Spring Security
* Spring Data JPA
* Hibernate
* PostgreSQL
* Neon PostgreSQL
* Maven
* Discord Interactions API
* Slack Incoming Webhooks
* HTML / CSS / JavaScript
* Render

---

## Project Structure

```text
project/
├── pom.xml
├── README.md
├── AI_NOTES.md
├── .env.example
│
└── src/
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

---

# Discord Interaction Flow

When a user runs a slash command, Discord sends a signed HTTP POST request to:

```text
POST /api/discord/interactions
```

The application performs the following steps:

1. Verifies the Discord Ed25519 signature.
2. Validates the request timestamp.
3. Handles Discord PING requests.
4. Identifies the slash command.
5. Loads the command configuration from PostgreSQL.
6. Rejects commands that are not configured or are disabled.
7. Checks the Discord interaction ID to prevent duplicate processing.
8. Extracts command information and report text when applicable.
9. Stores the command and action information in PostgreSQL.
10. Responds to the user in Discord.
11. Starts the Slack mirror asynchronously when enabled.

---

# Slash Commands

## `/status`

Checks the bot status.

Example:

```text
/status
```

Example response:

```text
Bot is running! ✅
```

The dashboard can control:

* Whether the command is enabled
* The response message
* Whether the response is mirrored to Slack

---

## `/report <text>`

Records a report entered by the Discord user.

Example:

```text
/report database is unavailable
```

Example Discord response:

```text
Report received: database is unavailable
```

When Slack mirroring is enabled, the notification is also sent to the configured Slack channel.

---

# Database

The deployed application uses PostgreSQL hosted on Neon.

## `command_logs`

Stores received Discord interactions and the resulting actions.

Typical fields:

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

Example action:

```text
Command saved to DB; Discord response sent; Slack mirror sent
```

or, when Slack is disabled:

```text
Command saved to DB; Discord response sent; Slack mirror disabled
```

If the Slack operation fails, the action is updated to indicate the failure.

---

## `command_configs`

Stores the behavior of each supported command.

Typical fields:

```text
id
command_name
enabled
response_message
mirror_enabled
```

Example:

```text
/status
enabled = true
response_message = Bot is running! ✅
mirror_enabled = true
```

The application uses the configuration stored in PostgreSQL rather than relying only on hard-coded command behavior.

Hibernate updates the schema automatically using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

---

# Admin Dashboard

The dashboard is protected by Spring Security login.

Dashboard URL:

```text
https://discord-slash-command-bot-hxfk.onrender.com/dashboard.html
```

## Dashboard sections

### Command Summary

Shows:

* Total commands
* Number of `/report` commands
* Number of `/status` commands

### Command Configuration

For each command, the admin can configure:

* Enabled / disabled
* Response message
* Slack mirroring enabled / disabled

Example:

```text
/report

Enabled: ✅

Response Message:
Report received

Mirror to Slack:
✅
```

Clicking **Save** updates the configuration in PostgreSQL.

### Command Logs

The dashboard shows:

* ID
* Command
* Discord user ID
* Discord channel ID
* Report text
* Action Taken
* Timestamp

The command log automatically refreshes every 10 seconds.

---

# Environment Variables

Secrets are stored outside the source code using environment variables.

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

## Variable descriptions

| Variable                     | Purpose                                                                  |
| ---------------------------- | ------------------------------------------------------------------------ |
| `DISCORD_PUBLIC_KEY`         | Discord application's public key used for request signature verification |
| `ADMIN_USERNAME`             | Username for the admin dashboard                                         |
| `ADMIN_PASSWORD`             | Password for the admin dashboard                                         |
| `DATABASE_URL`               | Neon PostgreSQL JDBC connection URL                                      |
| `DB_USERNAME`                | Neon PostgreSQL database user/role                                       |
| `DB_PASSWORD`                | Neon PostgreSQL password                                                 |
| `DISCORD_MIRROR_WEBHOOK_URL` | Slack Incoming Webhook URL                                               |

Example JDBC URL:

```text
jdbc:postgresql://<host>/<database>?sslmode=require
```

### Secret handling

The following values must never be committed to GitHub:

* Discord bot token
* Discord application credentials
* Discord public key if treated as deployment secret
* Neon database password
* Slack webhook URL
* Admin password

Use `.env.example` only as a template.

---

# Local Setup

## Prerequisites

Install:

* Java 17
* Maven
* Git

## Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd Discord-Slash-Command-Bot/project
```

## Configure environment variables

Create your local environment variables.

Linux/macOS/Codespaces example:

```bash
export DISCORD_PUBLIC_KEY="your-discord-public-key"

export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="your-local-password"

export DATABASE_URL="jdbc:postgresql://your-host/your-database?sslmode=require"
export DB_USERNAME="your-database-user"
export DB_PASSWORD="your-database-password"

export DISCORD_MIRROR_WEBHOOK_URL="your-slack-webhook-url"
```

Do not commit these values to GitHub.

## Build the application

```bash
mvn clean package -DskipTests
```

## Run the application

```bash
mvn spring-boot:run
```

The application is available locally at:

```text
http://localhost:8080
```

Dashboard:

```text
http://localhost:8080/dashboard.html
```

Discord interactions endpoint:

```text
http://localhost:8080/api/discord/interactions
```

Discord cannot send interactions to localhost, so a public deployment is required for real Discord testing.

---

# Discord Configuration

Create a Discord application in the Discord Developer Portal.

Configure:

* Discord application
* Slash commands
* Interaction Endpoint URL

Production interaction endpoint:

```text
https://discord-slash-command-bot-hxfk.onrender.com/api/discord/interactions
```

The registered slash commands are:

```text
/status
/report <text>
```

The application handles Discord interaction types required by the implementation, including:

```text
PING
APPLICATION_COMMAND
```

---

# Discord Bot Installation

Use the Discord application installation/invite URL to add the bot to a test server.

Installation URL:

```text
https://discord.com/oauth2/authorize?client_id=1552451013011837028&scope=bot%20applications.commands&permissions=2048
```

The evaluator must have permission to add applications/bots to the target Discord server.

After installation, use:

```text
/status
```

and:

```text
/report <text>
```

---

# Slack Configuration

Slack is used for the second-channel notification requirement.

The application uses a Slack Incoming Webhook.

Setup:

1. Create a Slack app.
2. Enable Incoming Webhooks.
3. Add the webhook to the desired Slack channel.
4. Store the webhook URL in the environment variable:

```text
DISCORD_MIRROR_WEBHOOK_URL
```

The webhook URL is never exposed to the frontend.

---

# Asynchronous Slack Mirroring

Slack mirroring is performed asynchronously.

The flow is:

```text
Discord /report
      |
      v
Save command to PostgreSQL
      |
      +-------> Start Slack notification in background
      |
      v
Immediately respond to Discord
```

This prevents a slow Slack request from unnecessarily delaying the Discord interaction response.

---

# Slack Retry Handling

Temporary Slack failures are retried.

The application retries:

* HTTP 429 rate-limit responses
* HTTP 5xx server errors
* Temporary request exceptions

A maximum of three attempts is used.

Non-retryable errors such as an invalid webhook configuration are recorded as failures without repeatedly retrying.

The resulting action is stored in the command log.

Examples:

Successful:

```text
Command saved to DB; Discord response sent; Slack mirror sent
```

Failed:

```text
Command saved to DB; Discord response sent; Slack mirror failed (HTTP 404)
```

---

# Security and Reliability

## Ed25519 Signature Verification

Every Discord interaction is verified using:

```text
X-Signature-Ed25519
X-Signature-Timestamp
```

Unsigned or invalid requests are rejected.

---

## Request Timestamp Validation

The request timestamp is checked against the current server time.

Requests outside the configured five-minute freshness window are rejected.

This reduces the risk of replaying an old signed interaction.

---

## Duplicate Interaction Protection

The Discord interaction ID is stored in PostgreSQL.

Before processing a command, the application checks:

```text
Has this interaction ID already been processed?
```

If it has, duplicate processing is prevented.

---

## Downstream Failure Visibility

Slack failures are not silently ignored.

The command log is updated with the result of the mirror operation so the dashboard can show whether the notification was successfully sent or failed.

---

## Secret Management

Secrets are provided through environment variables.

Credentials are not embedded in:

* Java source code
* HTML
* JavaScript
* GitHub repository files
* Dashboard responses
* application logs

---

# Testing

## Test `/status`

Run in Discord:

```text
/status
```

Expected Discord response:

```text
Bot is running! ✅
```

If Slack mirroring is enabled, Slack should also receive the notification.

The dashboard should show a new log entry.

---

## Test `/report`

Run:

```text
/report test message
```

Expected Discord response:

```text
Report received: test message
```

Expected behavior:

```text
Discord response  → ✅
PostgreSQL record → ✅
Dashboard log     → ✅
Slack mirror      → ✅ when enabled
```

---

## Test Command Configuration

Open the dashboard.

For example, change:

```text
/report
```

Response Message:

```text
New report received
```

Click **Save**.

Run:

```text
/report test
```

The Discord response should use the updated configuration.

---

## Test Disabled Command

In the dashboard:

```text
/report
Enabled → OFF
```

Save the configuration.

Run:

```text
/report test
```

Expected:

```text
This command is currently disabled
```

---

## Test Slack Mirroring

Enable:

```text
Mirror to Slack → ON
```

Run:

```text
/report test
```

Expected:

```text
Discord → response
PostgreSQL → command recorded
Slack → notification received
Dashboard → action recorded
```

Disable mirroring and run the command again.

Expected:

```text
Discord → response
PostgreSQL → command recorded
Slack → no notification
```

The dashboard should record that Slack mirroring was disabled.

---

# Deployment

The application is deployed on Render.

The production architecture is:

```text
GitHub
   |
   v
Render
   |
   +---- Spring Boot application
   |
   +---- Neon PostgreSQL
   |
   +---- Slack Incoming Webhook
   |
   +---- Discord Interactions Endpoint
```

Render provides the application's runtime `PORT`.

The application uses:

```text
PORT
```

when provided by the hosting environment and uses the local development port when running locally.

---

# Production URL

```text
https://discord-slash-command-bot-hxfk.onrender.com
```

Dashboard:

```text
https://discord-slash-command-bot-hxfk.onrender.com/dashboard.html
```

Discord interaction endpoint:

```text
https://discord-slash-command-bot-hxfk.onrender.com/api/discord/interactions
```

---

# Evaluator Instructions

## 1. Open the dashboard

Open:

```text
https://discord-slash-command-bot-hxfk.onrender.com/dashboard.html
```

The dashboard requires login.

## 2. Admin credentials

A throwaway administrator account is configured for evaluation.

Credentials should be provided separately from the repository.

No personal credentials should be used by the evaluator.

## 3. Install the Discord bot

Use:

```text
https://discord.com/oauth2/authorize?client_id=1552451013011837028&scope=bot%20applications.commands&permissions=2048
```

to add the bot to a test server.

The evaluator must have sufficient permissions to install the application.

## 4. Test `/status`

Run:

```text
/status
```

Verify:

* Discord response
* Dashboard log
* Slack notification when enabled

## 5. Test `/report`

Run:

```text
/report test message
```

Verify:

* Discord response
* PostgreSQL record
* Dashboard log
* Action Taken
* Slack notification when enabled

## 6. Test configuration

Change the command response message from the dashboard and save it.

Run the command again in Discord.

Verify that the new configuration is used.

## 7. Test disabling mirroring

Turn:

```text
Mirror to Slack → OFF
```

Run `/report`.

Verify:

* Discord still responds
* Command is recorded
* Slack does not receive the message

## 8. Test disabling a command

Turn:

```text
Enabled → OFF
```

Save and execute the command.

Verify that the command reports that it is disabled.

---

# Submission Files

The repository includes:

```text
README.md
.env.example
AI_NOTES.md
```

`.env.example` contains variable names/placeholders only and does not contain real credentials.

`AI_NOTES.md` documents the AI tools used during development, implementation decisions, debugging experience, and possible future improvements.

---

# Known Design Choices

The application uses Discord HTTP Interactions instead of requiring an always-running Discord websocket listener for incoming commands.

Slack Incoming Webhooks are used for the second-channel notification because the assignment explicitly permits a Slack webhook as the mirror destination.

The deployed application uses Neon PostgreSQL instead of an in-memory database so command logs and configuration persist across application restarts.

---

# Future Improvements

Possible future improvements include:

* Interactive Discord buttons/components
* Modal-based `/report`
* AI-based report classification or summarization
* Multi-server support
* Persistent notification queues
* More detailed retry history
* Structured application logging
* Automated integration tests
* Database migrations using Flyway or Liquibase
* More advanced dashboard filtering and analytics

---

# License

This project was created as part of a software engineering assessment.
