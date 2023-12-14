package org.nordcraft;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class JetpackManager {

    private final int jetpackModelData;
    private Map<UUID, Long> lastJetpackUseTime = new HashMap<>(); // This Hashmap stores the information of when the jetpack last was used
                                                                  // It stores this info because we need it for the elytra cooldown period

    public JetpackManager(int jetpackModelData) {
        this.jetpackModelData = jetpackModelData;
    }   
    // This method is later being called in the PlayerFlightHandler class, to make sure the item has the correct custom model data and if it's in the correct slot before activating the jetpack 
    public boolean hasJetpackEquipped(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand(); // Get the item in the off-hand slot
        if (offHandItem != null && offHandItem.hasItemMeta()) {
            ItemMeta meta = offHandItem.getItemMeta();
            if (meta.hasCustomModelData() && meta.getCustomModelData() == jetpackModelData) {
                return true; // Jetpack is equipped in the off-hand slot
            }
        }
        return false; // No jetpack in the off-hand slot
    }
    // The methods below is going to be used later in the PlayerFlightHandler class, this is also for the elytra cooldown periods

    public void setJetpackLastUsedTime(Player player) {
        lastJetpackUseTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public long getJetpackLastUsedTime(Player player) {
        return lastJetpackUseTime.getOrDefault(player.getUniqueId(), 0L);
    }

    public boolean canEquipElytra(Player player) {
        Long lastUseTime = lastJetpackUseTime.get(player.getUniqueId());
        return lastUseTime == null || (System.currentTimeMillis() - lastUseTime) >= 10000; // 10 second cooldown
    }
}

