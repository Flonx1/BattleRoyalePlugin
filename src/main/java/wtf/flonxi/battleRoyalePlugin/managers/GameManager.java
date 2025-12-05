package wtf.flonxi.battleRoyalePlugin.managers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import wtf.flonxi.battleRoyalePlugin.BattleRoyalePlugin;
import wtf.flonxi.battleRoyalePlugin.GameState;
import wtf.flonxi.battleRoyalePlugin.tasks.AirdropTask;

import java.io.File;
import java.util.*;

public class GameManager {

    private final BattleRoyalePlugin plugin;
    private GameState state;
    private int countdown;
    private BukkitRunnable startingTask;
    private BukkitTask airdropTask;
    private BukkitTask borderDamageTask;
    private BukkitTask generatorTask;
    private World gameWorld;
    private final String WORLD_NAME_BASE;

    public GameManager(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
        this.state = GameState.LOBBY;
        this.WORLD_NAME_BASE = plugin.getConfig().getString("world.name-base", "arena_world");
        this.countdown = plugin.getConfig().getInt("timers.lobby-countdown");
    }

    public void createNewGameWorld(boolean teleportPlayers) {
        if (startingTask != null) startingTask.cancel();
        if (airdropTask != null) airdropTask.cancel();
        if (borderDamageTask != null) borderDamageTask.cancel();
        if (generatorTask != null) generatorTask.cancel();
        
        if (gameWorld != null) {
            String worldName = gameWorld.getName();
            
            for (Player p : gameWorld.getPlayers()) {
                p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            
            Bukkit.unloadWorld(gameWorld, false);
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            deleteWorldFolder(worldFolder);
            gameWorld = null;
        }

        String newWorldName = WORLD_NAME_BASE + "_" + System.currentTimeMillis();
        WorldCreator creator = new WorldCreator(newWorldName);
        creator.seed(new Random().nextLong());
        gameWorld = Bukkit.createWorld(creator);
        
        if (gameWorld != null) {
            gameWorld.setAutoSave(false);
            gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            gameWorld.setTime(6000);
        }
        
        if (teleportPlayers && gameWorld != null) {
            setupLobby();
            Location lobbySpawn = new Location(gameWorld, 0.5, 151, 0.5);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(lobbySpawn);
                p.setGameMode(GameMode.ADVENTURE);
                p.getInventory().clear();
                p.setHealth(20);
                p.setFoodLevel(20);
            }
            state = GameState.LOBBY;
            checkStart();
        }
    }
    
    public void createNewGameWorld() {
        createNewGameWorld(false);
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

        int startY = 150;
        int height = 6; 

        for (int x = -lobbySize; x <= lobbySize; x++) {
            for (int z = -lobbySize; z <= lobbySize; z++) {
                
                gameWorld.getBlockAt(x, startY, z).setType(Material.GLASS);

                gameWorld.getBlockAt(x, startY + height, z).setType(Material.GLASS);

                if (x == -lobbySize || x == lobbySize || z == -lobbySize || z == lobbySize) {
                    for (int y = 1; y < height; y++) {
                        gameWorld.getBlockAt(x, startY + y, z).setType(Material.GLASS);
                    }
                }
            }
        }
    }

    public void checkStart() {
        if (state != GameState.LOBBY) return;
        if (Bukkit.getOnlinePlayers().size() >= plugin.getConfig().getInt("game-settings.min-players", 2)) {
            if (startingTask == null || (startingTask != null && startingTask.isCancelled())) {
                state = GameState.STARTING;
                startCountdown();
            }
        }
    }

    private void startCountdown() {
        countdown = plugin.getConfig().getInt("timers.lobby-countdown");
        startingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getOnlinePlayers().size() < plugin.getConfig().getInt("game-settings.min-players", 2)) {
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
                    startingTask = null;
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
        int arenaSize = plugin.getConfig().getInt("world.arena-size");

        startWorldPreGeneration(arenaSize, this::continueGameStart);
    }
    
    private void continueGameStart() {
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
                    startAirdropTask();
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
    
    private void startWorldPreGeneration(int size, Runnable onComplete) {
        if (gameWorld == null) {
            onComplete.run(); 
            return;
        }
        
        if (generatorTask != null) generatorTask.cancel();
        
        final int radius = (size / 2) + 100;
        final int chunks = radius / 16; 
        final int totalChunks = (chunks * 2 + 1) * (chunks * 2 + 1); 

        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("world-gen-start").replace("%size%", String.valueOf(size)));
        
        generatorTask = new BukkitRunnable() {
            private int currentChunkX = -chunks;
            private int currentChunkZ = -chunks;
            private int processedChunks = 0;
            
            @Override
            public void run() {
                if (processedChunks >= totalChunks) {
                    Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("world-gen-complete")); 
                    onComplete.run();
                    cancel();
                    generatorTask = null;
                    return;
                }

                int chunksPerTick = 4;
                for (int i = 0; i < chunksPerTick; i++) {
                    
                    if (currentChunkX > chunks) {
                        break; 
                    }

                    gameWorld.loadChunk(currentChunkX, currentChunkZ, true); 
                    processedChunks++;
                    currentChunkZ++;

                    if (currentChunkZ > chunks) {
                        currentChunkX++;
                        currentChunkZ = -chunks;
                    }
                }
                
                if (processedChunks > 0 && (processedChunks % 200 == 0 || processedChunks == totalChunks)) {
                    int percent = (int) (((double) processedChunks / totalChunks) * 100);
                    Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("world-gen-progress").replace("%percent%", String.valueOf(percent)));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void startAirdropTask() {
        if (airdropTask != null) airdropTask.cancel();
        airdropTask = new AirdropTask(plugin).runTaskTimer(plugin, 0L, 20L);
    }


    private void startBorderDamageTask() {
        if (borderDamageTask != null) borderDamageTask.cancel();
        borderDamageTask = new BukkitRunnable() {
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
            
            if (!block.isLiquid() && block.getType() != Material.AIR) {
                return new Location(gameWorld, x + 0.5, y + 1, z + 0.5);
            }
            attempts++;
        }
        
        return new Location(gameWorld, 0.5, gameWorld.getHighestBlockYAt(0, 0) + 1, 0.5);
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
            if (p.getGameMode() == GameMode.SURVIVAL && p.getWorld().equals(gameWorld)) {
                String team = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
                if (team != null && !aliveTeams.contains(team)) {
                    aliveTeams.add(team);
                }
            }
        }

        if (aliveTeams.size() <= 1) {
            endGame(aliveTeams.size() == 1 ? aliveTeams.get(0) : null);
        }
    }
    
    public void endGame(String winnerTeam) {
        state = GameState.ENDING;
        
        if (airdropTask != null) airdropTask.cancel();
        if (borderDamageTask != null) borderDamageTask.cancel();
        if (generatorTask != null) generatorTask.cancel();

        if (winnerTeam != null) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "=============================");
            Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("win-broadcast").replace("%team%", winnerTeam));
            Bukkit.broadcastMessage(ChatColor.GOLD + "=============================");
            
            List<UUID> members = plugin.getTeamManager().getTeams().get(winnerTeam);
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
                createNewGameWorld(true);
            }
        }.runTaskLater(plugin, delay * 20L); 
    }

    private void broadcastActionBar(String message) {
        TextComponent component = new TextComponent(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, component);
        }
    }

    public void cleanup() {
        if (startingTask != null) startingTask.cancel();
        if (airdropTask != null) airdropTask.cancel();
        if (borderDamageTask != null) borderDamageTask.cancel();
        if (generatorTask != null) generatorTask.cancel();
        
        if (gameWorld != null) {
            String worldName = gameWorld.getName();
            Bukkit.unloadWorld(gameWorld, false);
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            deleteWorldFolder(worldFolder);
        }
    }

    public GameState getState() {
        return state;
    }

    public World getGameWorld() {
        return gameWorld;
    }
}