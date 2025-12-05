package wtf.flonxi.battleRoyalePlugin.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import wtf.flonxi.battleRoyalePlugin.BattleRoyalePlugin;
import wtf.flonxi.battleRoyalePlugin.managers.ConfigManager;

public class ScoreboardTask extends BukkitRunnable {

    private final BattleRoyalePlugin plugin;

    public ScoreboardTask(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }

    private void updateScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        
        Scoreboard board = player.getScoreboard();
        
        if (board.equals(manager.getMainScoreboard())) {
            board = manager.getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("br_board");
        if (obj == null) {
            obj = board.registerNewObjective("br_board", Criteria.DUMMY, plugin.getConfigManager().getMessage("scoreboard.title"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        int balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        String team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        int online = Bukkit.getOnlinePlayers().size();
        String stateName = plugin.getGameManager().getState().toString();
        ConfigManager cm = plugin.getConfigManager();

        score(obj, cm.getMessage("scoreboard.line") + " ", 6);
        score(obj, cm.getMessage("scoreboard.status").replace("%state%", stateName), 5);
        score(obj, " ", 4);
        score(obj, cm.getMessage("scoreboard.players").replace("%count%", String.valueOf(online)), 3);
        
        if (team != null) {
            score(obj, cm.getMessage("scoreboard.team").replace("%team%", team), 2);
        } else {
            score(obj, cm.getMessage("scoreboard.no-team"), 2);
        }
        
        score(obj, cm.getMessage("scoreboard.coins").replace("%amount%", String.valueOf(balance)), 1);
        score(obj, cm.getMessage("scoreboard.line"), 0);
    }

    private void score(Objective obj, String text, int score) {
        Score s = obj.getScore(text);
        s.setScore(score);
    }
}