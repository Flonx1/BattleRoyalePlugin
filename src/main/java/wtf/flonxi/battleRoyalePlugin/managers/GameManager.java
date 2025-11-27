package wtf.flonxi.battleRoyalePlugin.managers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import wtf.flonxi.battleRoyalePlugin.BattleRoyalePlugin;
import wtf.flonxi.battleRoyalePlugin.GameState;

import java.io.File;
import java.util.*;

public class GameManager {

    private final BattleRoyalePlugin plugin;
    private GameState state;
    private int countdown;
    private BukkitRunnable startingTask;
    private World gameWorld;
    private final String WORLD_NAME;

    public GameManager(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
        this.state = GameState.LOBBY;
        this.WORLD_NAME = plugin.getConfig().getString("world.name", "arena_world");
        this.countdown = plugin.getConfig().getInt("timers.lobby-countdown");
    }

    public void createNewGameWorld() {
        Bukkit.unloadWorld(WORLD_NAME, false);
        File worldFolder = new File(Bukkit.getWorldContainer(), WORLD_NAME);
        deleteWorldFolder(worldFolder);

        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.seed(new Random().nextLong());
        gameWorld = Bukkit.createWorld(creator);
        gameWorld.setAutoSave(false);
    }

    private void deleteWorldFolder(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorldFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
            path.delete();
        }
    }

    public void setupLobby() {
        if (gameWorld == null) return;

        int lobbySize = plugin.getConfig().getInt("world.lobby-size");
        WorldBorder border = gameWorld.getWorldBorder();
        border.setCenter(0, 0);
        border.setSize(lobbySize);
        border.setDamageAmount(0);

        for (int x = -lobbySize; x <= lobbySize; x++) {
            for (int z = -lobbySize; z <= lobbySize; z++) {
                gameWorld.getBlockAt(x, 150, z).setType(Material.GLASS);
            }
        }
    }

    public void checkStart() {
        if (state != GameState.LOBBY) return;
        if (Bukkit.getOnlinePlayers().size() >= 2) {
            if (startingTask == null) {
                state = GameState.STARTING;
                startCountdown();
            }
        }
    }

    private void startCountdown() {
        startingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().size() < 2) {
                    state = GameState.LOBBY;
                    countdown = plugin.getConfig().getInt("timers.lobby-countdown");
                    broadcastActionBar(plugin.getConfigManager().getMessage("not-enough-players"));
                    cancel();
                    startingTask = null;
                    return;
                }

                if (countdown <= 0) {
                    startGame();
                    cancel();
                    return;
                }

                if (countdown % 60 == 0 || countdown <= 10) {
                    Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("lobby-countdown").replace("%time%", String.valueOf(countdown)));
                }
                
                broadcastActionBar(plugin.getConfigManager().getMessage("lobby-actionbar").replace("%time%", String.valueOf(countdown)));
                
                countdown--;
            }
        };
        startingTask.runTaskTimer(plugin, 0L, 20L);
    }

    public void startGame() {
        state = GameState.FROZEN;
        plugin.getTeamManager().assignTeams();
        
        int arenaSize = plugin.getConfig().getInt("world.arena-size");
        WorldBorder border = gameWorld.getWorldBorder();
        border.setSize(arenaSize);
        border.setDamageAmount(0); 
        border.setWarningDistance(5);

        teleportPlayersToArena();

        new BukkitRunnable() {
            int freezeTime = plugin.getConfig().getInt("timers.freeze-time");
            @Override
            public void run() {
                if (freezeTime <= 0) {
                    state = GameState.PLAYING;
                    Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("game-started"));
                    broadcastActionBar(plugin.getConfigManager().getMessage("combat-actionbar"));
                    startShrinkTimer();
                    startBorderDamageTask(); 
                    cancel();
                    return;
                }
                
                String title = plugin.getConfigManager().getMessage("freeze-title").replace("%time%", String.valueOf(freezeTime));
                String sub = plugin.getConfigManager().getMessage("freeze-subtitle");
                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.sendTitle(title, sub, 0, 20, 0);
                    String action = plugin.getConfigManager().getMessage("freeze-actionbar").replace("%time%", String.valueOf(freezeTime));
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(action));
                });
                freezeTime--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startBorderDamageTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING) {
                    cancel();
                    return;
                }
                
                WorldBorder border = gameWorld.getWorldBorder();
                double size = border.getSize() / 2.0;
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getGameMode() == GameMode.SURVIVAL && p.getWorld().equals(gameWorld)) {
                        double x = p.getLocation().getX();
                        double z = p.getLocation().getZ();
                        
                        if (Math.abs(x) > size || Math.abs(z) > size) {
                            p.damage(1.0); 
                            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plugin.getConfigManager().getMessage("zone-damage")));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void teleportPlayersToArena() {
        Map<String, List<UUID>> teams = plugin.getTeamManager().getTeams();
        int range = plugin.getConfig().getInt("world.arena-size") / 2 - 50;

        for (List<UUID> members : teams.values()) {
            Location loc = findSafeLocation(range);

            for (UUID uuid : members) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.teleport(loc);
                    p.setGameMode(GameMode.SURVIVAL);
                    p.getInventory().clear();
                    p.setHealth(20);
                    p.setFoodLevel(20);
                }
            }
        }
    }

    private Location findSafeLocation(int range) {
        Random random = new Random();
        int attempts = 0;
        
        while (attempts < 50) {
            int x = random.nextInt(range * 2) - range;
            int z = random.nextInt(range * 2) - range;
            int y = gameWorld.getHighestBlockYAt(x, z);
            
            Block block = gameWorld.getBlockAt(x, y, z);
            
            if (!block.isLiquid()) {
                return new Location(gameWorld, x, y + 1, z);
            }
            attempts++;
        }
        
        return new Location(gameWorld, 0, gameWorld.getHighestBlockYAt(0, 0) + 1, 0);
    }

    private void startShrinkTimer() {
        long delay = plugin.getConfig().getInt("timers.border-shrink-start") * 20L;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING) return;
                Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("border-shrink"));
                WorldBorder border = gameWorld.getWorldBorder();
                int finalSize = plugin.getConfig().getInt("world.final-size");
                int duration = plugin.getConfig().getInt("timers.border-shrink-duration");
                border.setSize(finalSize, duration);
            }
        }.runTaskLater(plugin, delay);
    }

    public void checkWin() {
        if (state != GameState.PLAYING) return;

        List<String> aliveTeams = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL) {
                String team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
                if (team != null && !aliveTeams.contains(team)) {
                    aliveTeams.add(team);
                }
            }
        }

        if (aliveTeams.size() <= 1) {
            state = GameState.ENDING;
            if (aliveTeams.size() == 1) {
                String winningTeam = aliveTeams.get(0);
                Bukkit.broadcastMessage(ChatColor.GOLD + "=============================");
                Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("win-broadcast").replace("%team%", winningTeam));
                Bukkit.broadcastMessage(ChatColor.GOLD + "=============================");
                
                List<UUID> members = plugin.getTeamManager().getTeams().get(winningTeam);
                if (members != null) {
                    int winReward = plugin.getConfig().getInt("economy.win-reward");
                    for (UUID uuid : members) {
                        plugin.getEconomyManager().addCoins(uuid, winReward);
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) p.sendMessage(plugin.getConfigManager().getMessage("win-reward").replace("%amount%", String.valueOf(winReward)));
                    }
                }
            } else {
                Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("no-winner"));
            }
            
            long delay = plugin.getConfig().getInt("timers.restart-delay");
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getServer().shutdown();
                }
            }.runTaskLater(plugin, delay);
        }
    }

    private void broadcastActionBar(String message) {
        TextComponent component = new TextComponent(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, component);
        }
    }

    public void cleanup() {
        if (gameWorld != null) {
            Bukkit.unloadWorld(gameWorld, false);
        }
    }

    public GameState getState() {
        return state;
    }

    public World getGameWorld() {
        return gameWorld;
    }
}