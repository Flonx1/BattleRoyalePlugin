package wtf.flonxi.battleRoyalePlugin.tasks;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import wtf.flonxi.battleRoyalePlugin.BattleRoyalePlugin;
import wtf.flonxi.battleRoyalePlugin.GameState;

public class AirdropTask extends BukkitRunnable {

    private final BattleRoyalePlugin plugin;
    private int timer;
    private final int interval;

    public AirdropTask(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
        this.interval = plugin.getConfig().getInt("timers.airdrop-interval", 300);
        this.timer = this.interval;
    }

    @Override
    public void run() {
        if (plugin.getGameManager().getState() != GameState.PLAYING) {
            cancel();
            return;
        }

        if (timer <= 0) {
            plugin.getLootManager().spawnAirdrop();
            Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("airdrop-spawn"));
            timer = interval;
            return;
        }

        if (timer % 60 == 0 || timer <= 10) {
            Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("airdrop-countdown").replace("%time%", String.valueOf(timer)));
        }

        timer--;
    }
}