# AI Notes

## AI tools used

I used ChatGPT as an AI development assistant throughout this project. I used it for understanding the assignment, designing the application flow, generating and reviewing Java/Spring Boot code, debugging deployment errors, improving the dashboard UI, and explaining concepts while I implemented the project.

I remained responsible for running the application, testing the implementation, checking deployment logs, configuring Discord/Slack/Neon/Render, and deciding whether suggested solutions actually worked.

## Key decisions I made

### 1. Discord HTTP Interactions instead of an always-on Discord listener

I implemented Discord slash commands using the HTTP Interactions Endpoint described in the assignment rather than relying on an always-on Discord websocket listener.

This matched the assignment's requirements and allowed the application to receive signed Discord requests through a public Render endpoint. The application verifies the Ed25519 signature and handles Discord PING requests before processing slash commands.

### 2. Neon PostgreSQL instead of H2 for the deployed application

I initially used H2 during development, but changed the deployed application to Neon PostgreSQL because the assignment explicitly suggested Neon or Supabase and PostgreSQL provides persistent storage.

The application stores command logs and command configuration in PostgreSQL.

### 3. Slack Incoming Webhook for mirroring

The assignment allowed either a Slack Incoming Webhook or a separate Discord channel for mirrored notifications. I chose Slack so the notification path would be independent of the Discord bot's outbound API connection.

Slack mirroring is executed asynchronously so the Discord interaction response does not wait for the downstream notification.

## Hardest bug / wrong turn

The hardest issue was the outbound Discord notification path.

Initially, I used JDA to send a message to a second Discord channel. The application worked for incoming interactions and database logging, but the deployed Render service received a Discord/Cloudflare rate-limit response:

`429 / 1015`

The Render logs reported a very large `Retry-After` period. This caused JDA to have problems when communicating with Discord from the deployed environment.

The initial assumption was that replacing JDA with a direct Discord webhook would solve the problem. However, the Discord webhook request from the same Render service also received a `429 / 1015` response.

This showed that the problem was not simply the Java/JDA implementation. The important factor was the outbound Render-to-Discord request path and its rate limiting.

I then changed the mirror destination to Slack using an Incoming Webhook, which was explicitly allowed by the assignment. After that change, the end-to-end flow worked:

Discord → Spring Boot → PostgreSQL → Discord response

and, asynchronously:

Spring Boot → Slack Webhook → Slack channel

## Reliability and security improvements

The application was also updated to address the assignment's quality requirements.

* Discord request signatures are verified using the Ed25519 signature headers.
* Discord request timestamps are checked so stale requests are rejected.
* Discord interaction IDs are stored and checked to prevent duplicate processing.
* Slack notifications are asynchronous so a slow downstream service does not block the Discord response.
* Slack mirror failures are recorded in the command log.
* Temporary Slack failures can be retried.
* Secrets are provided through environment variables instead of being stored in source code.
* The admin dashboard is protected by Spring Security login.

## What I would improve with more time

With more development time, I would improve the application in several areas.

First, I would separate the notification/service naming more clearly; for example, the current mirror service name can be made more explicit now that Slack is the notification destination.

Second, I would implement a persistent retry queue rather than relying only on in-memory asynchronous execution. That would prevent a notification from being lost if the application restarts while a Slack request is in progress.

Third, I would improve the dashboard with more detailed observability, including separate success/failure states, retry counts, and filtering by command or date.

Finally, I would add automated tests for signature verification, duplicate interaction handling, command configuration, and downstream notification failures.
