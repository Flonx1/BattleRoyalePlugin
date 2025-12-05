package wtf.flonxi.battleRoyalePlugin.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import wtf.flonxi.battleRoyalePlugin.BattleRoyalePlugin;
import wtf.flonxi.battleRoyalePlugin.GameState;

public class GameListener implements Listener {

    private final BattleRoyalePlugin plugin;

    public GameListener(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World world = plugin.getGameManager().getGameWorld();
        
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(new Location(world, 0.5, 151, 0.5));
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);

        String msg = plugin.getConfigManager().getMessage("join").replace("%player%", player.getName());
        event.setJoinMessage(msg);
        plugin.getGameManager().checkStart();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String msg = plugin.getConfigManager().getMessage("quit").replace("%player%", event.getPlayer().getName());
        event.setQuitMessage(msg);
        if (plugin.getGameManager().getState() == GameState.PLAYING) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> 
                plugin.getGameManager().checkWin(), 1L);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        String msg = plugin.getConfigManager().getMessage("death").replace("%player%", victim.getName());
        event.setDeathMessage(msg);

        if (killer != null && killer != victim) {
            int reward = plugin.getConfig().getInt("economy.kill-reward");
            plugin.getEconomyManager().addCoins(killer.getUniqueId(), reward);
            killer.sendMessage(plugin.getConfigManager().getMessage("kill-reward").replace("%amount%", String.valueOf(reward)));
        }

        victim.setGameMode(GameMode.SPECTATOR);
        
        if (plugin.getGameManager().getState() == GameState.PLAYING) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> 
                plugin.getGameManager().checkWin(), 1L);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        GameState state = plugin.getGameManager().getState();

        if (state == GameState.LOBBY || state == GameState.STARTING || state == GameState.FROZEN || state == GameState.ENDING) {
            event.setCancelled(true);
            return;
        }

        if (state == GameState.PLAYING) {
            if (event.getDamager() instanceof Player) {
                Player victim = (Player) event.getEntity();
                Player attacker = (Player) event.getDamager();
                
                String team1 = plugin.getTeamManager().getPlayerTeam(victim.getUniqueId());
                String team2 = plugin.getTeamManager().getPlayerTeam(attacker.getUniqueId());
                
                if (team1 != null && team2 != null && team1.equals(team2)) {
                    attacker.sendMessage(plugin.getConfigManager().getMessage("friendly-fire"));
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (plugin.getGameManager().getState() == GameState.FROZEN) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to == null) return;

            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                event.setTo(from);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getGameManager().getState() == GameState.LOBBY || 
            plugin.getGameManager().getState() == GameState.STARTING ||
            plugin.getGameManager().getState() == GameState.FROZEN) {
            
            if (!event.getPlayer().isOp()) {
                event.setCancelled(true);
            }
        }
    }
}