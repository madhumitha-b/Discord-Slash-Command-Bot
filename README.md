# Discord Slash Command Bot

A Spring Boot application integrated with Discord to handle slash commands using the Java Discord API (JDA).

---

## Current Features

The application currently supports:

* Spring Boot backend
* Discord bot integration using JDA
* Discord bot authentication using an environment variable
* Discord server (Guild) installation
* Slash command registration
* `/hello` slash command
* `/report` slash command
* `/status` slash command
* Slash command event handling through JDA

---

## Tech Stack

* Java 17
* Spring Boot
* JDA (Java Discord API)
* Maven
* Discord Developer Portal
* H2 Database *(currently configured for local development)*

---

## Project Structure

```text
src/
└── main/
    ├── java/
    │   └── com/example/project/
    │       ├── ProjectApplication.java
    │       ├── DiscordBotConfig.java
    │       └── SlashCommandListener.java
    │
    └── resources/
        └── application.properties
```

---

## How the Current Bot Works

The current implementation uses JDA to connect the Spring Boot application to Discord.

```text
Spring Boot Application
        |
        v
DiscordBotConfig
        |
        v
JDA
        |
        v
Discord
        |
        v
SlashCommandListener
        |
        v
Process slash command
        |
        v
Reply to user
```

---

## Discord Bot Configuration

The Discord bot token is **not stored directly in the source code**.

The token is supplied through an environment variable:

```bash
export DISCORD_TOKEN="your-discord-bot-token"
```

The application reads it through:

```properties
discord.bot.token=${DISCORD_TOKEN}
```

The Java configuration retrieves this property using Spring's `@Value`:

```java
@Value("${discord.bot.token}")
private String token;
```

### Security

The Discord bot token should never be:

* committed to Git
* pushed to GitHub
* included directly in `application.properties`
* printed in application logs
* shared publicly

---
## Current Slash Commands

### `/hello`

Test command used to verify that the Discord bot is successfully connected.

Example:

```text
/hello
```

Response:

```text
Hello! 👋
```

---

### `/report`

The intended command accepts a text value.

Example:

```text
/report text: Something is not working
```

The current implementation reads the command option from the interaction event.

```java
String reportText =
        event.getOption("text").getAsString();
```

---

### `/status`

Example:

```text
/status
```

The current bot responds with:

```text
Bot is running! ✅
```

---

## Discord Application Setup

The Discord application was created through the Discord Developer Portal.

The bot was installed into a Discord server using a Guild Install.

The required installation scopes include:

* `bot`
* `applications.commands`

The bot currently has permission to send messages in the server.

---

## Running Locally

### 1. Set the Discord bot token

Linux/macOS/GitHub Codespaces:

```bash
export DISCORD_TOKEN="your-discord-bot-token"
```

Verify that the environment variable exists without printing the token:

```bash
if [ -n "$DISCORD_TOKEN" ]; then
    echo "Token is set"
else
    echo "Token is NOT set"
fi
```

### 2. Start the application

```bash
mvn spring-boot:run
```

The application should start successfully and the JDA connection should show messages similar to:

```text
Login Successful!
Connected to WebSocket
Finished Loading!
```

### 3. Test in Discord

Open the Discord server where the bot was installed.

Type:

```text
/
```

and select one of the registered slash commands.

---

## Current Architecture

At this stage, the application uses JDA's WebSocket-based event handling:

```text
Discord
   |
   | WebSocket
   v
JDA
   |
   v
SlashCommandListener
   |
   +---- /hello
   |
   +---- /report
   |
   +---- /status
```
