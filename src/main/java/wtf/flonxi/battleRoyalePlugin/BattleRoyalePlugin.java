package wtf.flonxi.battleRoyalePlugin;

import org.bukkit.plugin.java.JavaPlugin;
import wtf.flonxi.battleRoyalePlugin.listeners.GameListener;
import wtf.flonxi.battleRoyalePlugin.managers.ConfigManager;
import wtf.flonxi.battleRoyalePlugin.managers.EconomyManager;
import wtf.flonxi.battleRoyalePlugin.managers.GameManager;
import wtf.flonxi.battleRoyalePlugin.managers.TeamManager;

import java.io.File;

public final class BattleRoyalePlugin extends JavaPlugin {

    private GameManager gameManager;
    private TeamManager teamManager;
    private EconomyManager economyManager;
    private ConfigManager configManager;
    String version = "1.0";
    String lastUpdate = "27.11.2025";
    @Override
    public void onEnable() {
        getLogger().info("\nStarting BattleRoyalePlugin...\nCreator: Flonxi\nVersion: " + version + "\nLast update - " + lastUpdate);

        saveDefaultConfig();
        createLangFiles();
        
        this.configManager = new ConfigManager(this);
        this.economyManager = new EconomyManager();
        this.teamManager = new TeamManager(this);
        this.gameManager = new GameManager(this);

        gameManager.createNewGameWorld();

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        
        new ScoreboardTask(this).runTaskTimer(this, 0L, 20L);
        
        gameManager.setupLobby();
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.cleanup();
        }
    }

    private void createLangFiles() {
        File langDir = new File(getDataFolder(), "langs");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        if (!new File(langDir, "en.yml").exists()) {
            saveResource("langs/en.yml", false);
        }
        if (!new File(langDir, "ru.yml").exists()) {
            saveResource("langs/ru.yml", false);
        }
        if (!new File(langDir, "de.yml").exists()) {
            saveResource("langs/de.yml", false);
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}