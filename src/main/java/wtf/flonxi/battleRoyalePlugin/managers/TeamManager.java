package wtf.flonxi.battleRoyalePlugin.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import wtf.flonxi.battleRoyalePlugin.BattleRoyalePlugin;

import java.util.*;

public class TeamManager {

    private final BattleRoyalePlugin plugin;
    private final Map<String, List<UUID>> teams = new HashMap<>();
    private final Map<UUID, String> playerTeamMap = new HashMap<>();

    public TeamManager(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public void assignTeams() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);

        teams.clear();
        playerTeamMap.clear();

        int teamIndex = 1;

        if (players.size() == 2) {
            for (Player p : players) {
                String teamName = String.valueOf(teamIndex);
                List<UUID> members = new ArrayList<>();
                addPlayerToTeam(p, teamName, members);
                teams.put(teamName, members);
                teamIndex++;
            }
        } else {
            while (!players.isEmpty()) {
                String teamName = String.valueOf(teamIndex);
                List<UUID> members = new ArrayList<>();
                
                Player p1 = players.remove(0);
                addPlayerToTeam(p1, teamName, members);

                if (!players.isEmpty()) {
                    Player p2 = players.remove(0);
                    addPlayerToTeam(p2, teamName, members);
                }
                
                teams.put(teamName, members);
                teamIndex++;
            }
        }
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("team-assigned"));
    }

    private void addPlayerToTeam(Player p, String teamName, List<UUID> members) {
        members.add(p.getUniqueId());
        playerTeamMap.put(p.getUniqueId(), teamName);
        p.setPlayerListName(ChatColor.GREEN + "[#" + teamName + "] " + p.getName());
        p.sendMessage(plugin.getConfigManager().getMessage("team-join").replace("%team%", teamName));
    }

    public String getPlayerTeam(UUID uuid) {
        return playerTeamMap.get(uuid);
    }

    public Map<String, List<UUID>> getTeams() {
        return teams;
    }
}