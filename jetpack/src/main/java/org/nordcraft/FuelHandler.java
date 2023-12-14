package org.nordcraft;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class FuelHandler {

    private final int fuelModelData; // Custom model data ID for the fuel item
    private final long fuelConsumptionInterval; // interval in milliseconds
    private long lastFuelConsumptionTime = 0; // tracks the last time fuel was consumed
    private String lowFuelMessage;
    private String normalFuelMessage;

    public FuelHandler(Plugin plugin, int fuelModelData, int fuelConsumptionRate) {
        this.fuelModelData = fuelModelData;
        this.fuelConsumptionInterval = fuelConsumptionRate * 1000L; // Convert seconds to milliseconds
        this.lowFuelMessage = plugin.getConfig().getString("fuel-messages.low-fuel"); // Reads the fuel message from the config.yml
        this.normalFuelMessage = plugin.getConfig().getString("fuel-messages.normal-fuel"); // Reads the fuel message from the config.yml
    }

    // Method to calculate the total fuel in the player's inventory

    private int getTotalFuel(Player player) {
        int totalFuel = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasCustomModelData() && meta.getCustomModelData() == fuelModelData) {
                    totalFuel += item.getAmount();
                }
            }
        }
        return totalFuel;
    }

    // Method to periodically check and consume fuel

    public boolean shouldConsumeFuel() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFuelConsumptionTime >= fuelConsumptionInterval) {
            lastFuelConsumptionTime = currentTime;
            return true;
        }
        return false;
    }

    // Method to check and consume fuel from the player's inventory

    public boolean consumeFuelIfRequired(Player player) {
        if (!shouldConsumeFuel()) {
            return true; // Do not consume fuel yet, but return true to keep the jetpack active
        }

        int totalFuel = getTotalFuel(player);
        if (totalFuel > 0) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta.hasCustomModelData() && meta.getCustomModelData() == fuelModelData) {
                        if (item.getAmount() > 0) {
                            item.setAmount(item.getAmount() - 1);
                            sendFuelMessage(player, totalFuel - 1); // Triggers the sendFuelMessage method below and sends the updated fuel message in the player's action bar
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean hasEnoughFuel(Player player) {
        return getTotalFuel(player) > 0;
    }

    // Method to send the fuel message, {fuel} is a placeholder of totalFuel(I made it a placeholder because i want to be able to change the text in the config.yml),
    // Then i don't have to go and edit the code if i want to change the text

    private void sendFuelMessage(Player player, int totalFuel) {
        String message;
        if (totalFuel < 20) {
            message = lowFuelMessage.replace("{fuel}", String.valueOf(totalFuel));
        } else {
            message = normalFuelMessage.replace("{fuel}", String.valueOf(totalFuel));
        }
        // This is the method that actually sends the message, i also included ChatColor so i can use different & color codes etc
        message = ChatColor.translateAlternateColorCodes('&', message); 
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
}
