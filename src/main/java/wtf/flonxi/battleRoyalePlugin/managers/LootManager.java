package wtf.flonxi.battleRoyalePlugin.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import wtf.flonxi.battleRoyalePlugin.BattleRoyalePlugin;

import java.util.Random;

public class LootManager {

    private final BattleRoyalePlugin plugin;
    private final Random random = new Random();

    public LootManager(BattleRoyalePlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnAirdrop() {
        World world = plugin.getGameManager().getGameWorld();
        if (world == null) return;

        int range = plugin.getConfig().getInt("world.arena-size") / 2 - 20;
        int x = random.nextInt(range * 2) - range;
        int z = random.nextInt(range * 2) - range;

        int y = world.getHighestBlockYAt(x, z) + 50;
        Location dropLoc = new Location(world, x, y, z);
        
        world.dropItemNaturally(dropLoc, new ItemStack(Material.CHEST));
        
        new BukkitRunnable() {
            private int countdown = 10;
            @Override
            public void run() {
                if (countdown <= 0) {
                    Block block = dropLoc.getBlock();
                    block.setType(Material.CHEST);
                    if (block.getState() instanceof Chest) {
                        Chest chest = (Chest) block.getState();
                        fillAirdropChest(chest);
                    }
                    cancel();
                    return;
                }
                
                dropLoc.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, dropLoc, 5);
                
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void fillAirdropChest(Chest chest) {
        Random rand = new Random();
        
        chest.getInventory().clear();
        
        chest.getInventory().setItem(rand.nextInt(27), new ItemStack(Material.DIAMOND_SWORD));
        chest.getInventory().setItem(rand.nextInt(27), new ItemStack(Material.DIAMOND_CHESTPLATE));
        chest.getInventory().setItem(rand.nextInt(27), new ItemStack(Material.GOLDEN_APPLE, 5));
        chest.getInventory().setItem(rand.nextInt(27), new ItemStack(Material.BOW));
        chest.getInventory().setItem(rand.nextInt(27), new ItemStack(Material.ARROW, 32));
    }
}