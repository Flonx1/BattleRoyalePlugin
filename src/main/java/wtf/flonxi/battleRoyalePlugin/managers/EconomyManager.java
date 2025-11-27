package wtf.flonxi.battleRoyalePlugin.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final Map<UUID, Integer> balances = new HashMap<>();

    public int getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0);
    }

    public void addCoins(UUID uuid, int amount) {
        balances.put(uuid, getBalance(uuid) + amount);
    }

    public void removeCoins(UUID uuid, int amount) {
        balances.put(uuid, Math.max(0, getBalance(uuid) - amount));
    }
}