# Minigames Manager Integration Examples

This directory contains examples showing how to integrate Spigot and Velocity plugins with the Minigames Manager Application.

## Overview

The Minigames Manager Application provides a centralized way to create, manage, and destroy game servers for BedWars, SkyWars, and Duels. These examples demonstrate how to integrate with it from your Spigot or Velocity plugins.

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Spigot/Velocity   │    │  Minigames Manager │    │   Game Servers   │
│     Plugin         │◄──►│    Application     │◄──►│  (Docker/K8s)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Examples

### 1. Spigot Plugin Example (`spigot/MinigamesManagerPlugin.kt`)

A complete Spigot plugin that demonstrates:
- Creating game servers with `/creategame` command
- Destroying game servers with `/destroygame` command
- Listing active servers with `/listgames` command
- Joining game servers with `/joingame` command

**Commands:**
- `/creategame <gametype> <map> [players]` - Create a new game server
- `/destroygame <serverid>` - Destroy a game server
- `/listgames [gametype] [status]` - List all active game servers
- `/joingame <serverid>` - Join a game server

**Permissions:**
- `minigames.create` - Create game servers (default: op)
- `minigames.destroy` - Destroy game servers (default: op)
- `minigames.list` - List game servers (default: true)
- `minigames.join` - Join game servers (default: true)

### 2. Velocity Plugin Example (`velocity/MinigamesManagerVelocityPlugin.kt`)

A complete Velocity plugin that demonstrates:
- Creating and managing game servers
- Registering servers with Velocity's proxy
- Handling player connections to game servers
- Plugin message communication

**Features:**
- Automatic server registration with Velocity
- Player connection handling
- Server status monitoring
- Plugin message communication

### 3. Integration Example (`integration/GameServerIntegrationExample.kt`)

A comprehensive example showing:
- HTTP API integration with the Minigames Manager Application
- Complete game server lifecycle management
- Server status monitoring
- Error handling and retry logic

## Integration Methods

### Method 1: Direct API Integration (Same JVM)

If your plugin runs in the same JVM as the Minigames Manager Application:

```kotlin
// Initialize the deployment manager
val deploymentManager = UnifiedDeploymentManager()
deploymentManager.initialize()

// Create the game server API
val gameServerApi = GameServerApi(deploymentManager)
gameServerApi.initialize()

// Create a game server
val gameType = GameType("bedwars", "BedWars", "cgs-bedwars")
val serverInfo = gameServerApi.createGameServer(
    gameType = gameType,
    mapId = "bedwars_map_1",
    playerCount = 16,
    creatorId = "your-plugin"
)
```

### Method 2: HTTP API Integration (Separate Service)

If the Minigames Manager Application runs as a separate service:

```kotlin
// Create HTTP request
val request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8080/api/gameservers"))
    .header("Authorization", "Bearer your-api-key")
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString("""
        {
            "gameType": {
                "id": "bedwars",
                "displayName": "BedWars",
                "dataStore": "cgs-bedwars"
            },
            "mapId": "bedwars_map_1",
            "playerCount": 16,
            "creatorId": "your-plugin"
        }
    """))
    .build()

// Send request
val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
```

## Game Server Lifecycle

### 1. Creation
```kotlin
// Create a game server
val serverInfo = gameServerApi.createGameServer(
    gameType = GameType("bedwars", "BedWars", "cgs-bedwars"),
    mapId = "bedwars_map_1",
    playerCount = 16,
    creatorId = "your-plugin"
)

// Server status will be CREATING initially
println("Server status: ${serverInfo.status}") // CREATING
```

### 2. Monitoring
```kotlin
// Monitor server status
while (serverInfo.status == GameServerStatus.CREATING) {
    delay(10_000) // Wait 10 seconds
    val updatedInfo = gameServerApi.getServerInfo(serverInfo.serverId)
    serverInfo.status = updatedInfo?.status ?: GameServerStatus.FAILED
}

// Server is now running
if (serverInfo.status == GameServerStatus.RUNNING) {
    println("Server is ready at ${serverInfo.connectionInfo?.host}:${serverInfo.connectionInfo?.port}")
}
```

### 3. Destruction
```kotlin
// Destroy the game server
val success = gameServerApi.destroyGameServer(serverInfo.serverId)
if (success) {
    println("Server destroyed successfully")
}
```

## Configuration

### Spigot Plugin Configuration

Add to your `plugin.yml`:

```yaml
name: YourPlugin
version: 1.0.0
main: your.package.YourPlugin
api-version: 1.19

commands:
  creategame:
    description: Create a new game server
    usage: /creategame <gametype> <map> [players]
    permission: yourplugin.create

permissions:
  yourplugin.create:
    description: Create game servers
    default: op
```

### Velocity Plugin Configuration

Add to your `velocity-plugin.json`:

```json
{
  "id": "your-plugin",
  "name": "Your Plugin",
  "version": "1.0.0",
  "description": "Your plugin description",
  "authors": ["YourName"]
}
```

## Error Handling

### Common Error Scenarios

1. **Server Creation Failed**
```kotlin
try {
    val serverInfo = gameServerApi.createGameServer(gameType, mapId, playerCount, creatorId)
} catch (e: GameServerException) {
    logger.error("Failed to create game server: ${e.message}")
    // Handle error (show message to player, retry, etc.)
}
```

2. **Server Not Found**
```kotlin
val serverInfo = gameServerApi.getServerInfo(serverId)
if (serverInfo == null) {
    logger.warn("Server $serverId not found")
    // Handle missing server
}
```

3. **Server Status Issues**
```kotlin
when (serverInfo.status) {
    GameServerStatus.FAILED -> {
        logger.error("Server ${serverInfo.serverId} failed to start")
        // Handle server failure
    }
    GameServerStatus.DEGRADED -> {
        logger.warn("Server ${serverInfo.serverId} is degraded")
        // Handle degraded server
    }
    else -> {
        // Server is healthy
    }
}
```

## Best Practices

### 1. Async Operations
Always perform game server operations asynchronously to avoid blocking the main thread:

```kotlin
// Good
pluginScope.launch {
    val serverInfo = gameServerApi.createGameServer(gameType, mapId, playerCount, creatorId)
    // Update UI on main thread
    Bukkit.getScheduler().runTask(plugin) {
        player.sendMessage("Server created: ${serverInfo.serverId}")
    }
}

// Bad - blocks main thread
val serverInfo = gameServerApi.createGameServer(gameType, mapId, playerCount, creatorId)
```

### 2. Resource Cleanup
Always clean up resources when your plugin is disabled:

```kotlin
override fun onDisable() {
    pluginScope.launch {
        gameServerApi.shutdown()
        pluginScope.coroutineContext.cancelChildren()
    }
}
```

### 3. Error Recovery
Implement retry logic for transient failures:

```kotlin
suspend fun createGameServerWithRetry(
    gameType: GameType,
    mapId: String,
    playerCount: Int,
    creatorId: String,
    maxRetries: Int = 3
): GameServerInfo? {
    repeat(maxRetries) { attempt ->
        try {
            return gameServerApi.createGameServer(gameType, mapId, playerCount, creatorId)
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            delay(1000 * (attempt + 1)) // Exponential backoff
        }
    }
    return null
}
```

### 4. Status Monitoring
Monitor server status to handle failures gracefully:

```kotlin
suspend fun monitorServerStatus(serverId: String) {
    while (isActive) {
        val serverInfo = gameServerApi.getServerInfo(serverId)
        
        when (serverInfo?.status) {
            GameServerStatus.FAILED -> {
                logger.error("Server $serverId failed, cleaning up...")
                // Clean up resources, notify players, etc.
                break
            }
            GameServerStatus.DESTROYED -> {
                logger.info("Server $serverId was destroyed")
                break
            }
            else -> {
                // Server is healthy, continue monitoring
            }
        }
        
        delay(30_000) // Check every 30 seconds
    }
}
```

## Troubleshooting

### Common Issues

1. **Connection Refused**
   - Check if the Minigames Manager Application is running
   - Verify the URL and port in your configuration
   - Check firewall settings

2. **Authentication Failed**
   - Verify your API key is correct
   - Check if the API key has the required permissions

3. **Server Creation Timeout**
   - Check if Docker/Kubernetes is properly configured
   - Verify resource limits and availability
   - Check logs for deployment errors

4. **Server Not Found**
   - Verify the server ID is correct
   - Check if the server was already destroyed
   - Verify the server is in the expected status

### Debugging

Enable debug logging to troubleshoot issues:

```kotlin
// In your plugin
logger.level = Level.DEBUG

// In application.conf
logging {
    level = "DEBUG"
}
```

## Support

For issues and questions:
- Check the application logs
- Review the plugin logs
- Check the Minigames Manager Application status
- Contact the development team
