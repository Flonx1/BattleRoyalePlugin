# BattleRoyale Plugin

**BattleRoyale** is a lightweight, fully automated mini-game plugin designed to turn your server instance into a competitive PvP battleground. Inspired by the popular genre, this plugin manages the entire game lifecycle without requiring admin intervention, making it perfect for automated event servers or BungeeCord networks.

## 🔹 Key Features

*   **Fully Automated Game Cycle:** From the lobby countdown to the winner announcement and server restart, everything runs automatically.
*   **Dynamic Team System:** automatically shuffles and assigns players into teams (Duos) upon game start.
*   **"The Zone" Mechanic:** A shrinking WorldBorder forces players closer together. Players caught outside the zone take periodic damage.
*   **Smart World Management:** The plugin generates a fresh, temporary game world for every match and handles cleanup automatically.
*   **Economy System:** Integrated coin system that rewards players for kills and victories.
*   **Immersive UI:** Features a custom Scoreboard, Action Bar messages, and Titles to keep players informed of their status, team, and game state.
*   **Multi-Language Support:** Comes with built-in support for **English**, **Russian**, and **German**. You can easily switch languages or add your own via the configuration.

## 🎮 How to Play

1.  **Join the Lobby:** Players join the server and are teleported to a glass waiting area high in the sky.
2.  **Countdown:** Once the minimum player count (configurable) is reached, the lobby timer begins.
3.  **The Drop:** Players are randomly scattered across the map and assigned to teams.
4.  **Freeze Period:** A short freeze timer allows players to look around and prepare before PvP is enabled.
5.  **Fight:** Scavenge (if chests are set up) and fight! The border will begin to shrink after a configurable amount of time.
6.  **Victory:** The last team standing wins the coin reward. The server then restarts automatically to reset the map for the next round.

## ⚙️ Configuration

The plugin is highly configurable via `config.yml`. You can tweak timers, rewards, world sizes, and language settings.

```yaml
lang: en # Options: en, ru, de
timers:
  lobby-countdown: 300
  freeze-time: 30
  border-shrink-start: 600
economy:
  kill-reward: 10
  win-reward: 100
world:
  arena-size: 1000
  final-size: 50
```

## ⚠️ Important Note
This plugin is designed for **dedicated game instances**. Once a match concludes, the plugin is programmed to **shutdown/restart the server** to ensure the world map is reset and memory is cleared for the next game. It is recommended to run this on a separate server connected via a proxy (like Velocity or BungeeCord) rather than your main survival server.
