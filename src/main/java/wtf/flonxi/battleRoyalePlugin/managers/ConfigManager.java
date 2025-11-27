package wtf.flonxi.battleRoyalePlugin.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import wtf.flonxi.battleRoyalePlugin.BattleRoyalePlugin;

import java.io.File;

public class ConfigManager {

    private final BattleRoyalePlugin plugin;
    private FileConfiguration langConfig;

    public ConfigManager(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
        loadLang();
    }

    private void loadLang() {
        String lang = plugin.getConfig().getString("lang", "en");
        File langFile = new File(plugin.getDataFolder(), "langs/" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + lang + ".yml, loading en.yml");
            langFile = new File(plugin.getDataFolder(), "langs/en.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getMessage(String key) {
        String msg = langConfig.getString(key);
        if (msg == null) return "Missing key: " + key;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}