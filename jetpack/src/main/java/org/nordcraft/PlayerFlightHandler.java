package org.nordcraft;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PlayerFlightHandler implements Listener { // This is the listener class that integrates and manages the methods from our other classes

    private final double liftForce;
    private final double horizontalSpeed;
    private final FuelHandler fuelHandler;
    private final Plugin plugin;
    private final JetpackManager jetpackManager;
    private final int fallDamageReduction;
    private Map<UUID, BukkitRunnable> jetpackTasks = new HashMap<>(); // We need this hashmap to store the tasks for each player so we can cancel them easily if they TP/disconnect etc

    public PlayerFlightHandler(Plugin plugin, FuelHandler fuelHandler, JetpackManager jetpackManager, double liftForce, double horizontalSpeed, int fallDamageReduction) {
        this.plugin = plugin;
        this.fuelHandler = fuelHandler;
        this.jetpackManager = jetpackManager;
        this.liftForce = liftForce;
        this.horizontalSpeed = horizontalSpeed;
        this.fallDamageReduction = fallDamageReduction;
    }

    @EventHandler // Triggered when a player sneaks or stops sneaking, Activates or deactivates the jetpack based on the jetpack's current state, fuel availability, and whether the player is wearing an Elytra
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && jetpackManager.hasJetpackEquipped(player) && fuelHandler.hasEnoughFuel(player)) {
            if (isWearingElytra(player)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lJetpack &c>> You cannot use the jetpack while wearing an Elytra."));
                return;
            }
            activateJetpack(player);
            jetpackManager.setJetpackLastUsedTime(player);
        } else {
            
        }
    }

    private void activateJetpack(Player player) { // Starts the jetpack functionality by applying force and showing particles
        stopJetpack(player);
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isSneaking()) {
                    applyJetpackForce(player);
                    showJetpackParticles(player);
                    if (!fuelHandler.consumeFuelIfRequired(player)) {
                        this.cancel();
                    }
                } else {
                    this.cancel();
                }
            }
        };
        task.runTaskTimer(plugin, 0, 1);
        jetpackTasks.put(player.getUniqueId(), task);
    }

    private void stopJetpack(Player player) { // Stops the jetpack's functionality by cancelling the associated task
        BukkitRunnable task = jetpackTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler // Triggered when a player teleports and ensures the jetpack is safely stopped if the player teleports while using it
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        stopJetpack(event.getPlayer());
    }

    @EventHandler // Triggered when a player quits the game and then stops the jetpack task to avoid any residual effects or errors
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopJetpack(event.getPlayer());
    }



    private void applyJetpackForce(Player player) { // Applies force to the player, simulating jetpack flight, this adjusts the player's velocity to create a lift (vertical force) and horizontal movement
        Vector velocity = player.getVelocity();
        velocity.setY(velocity.getY() + liftForce);
        Vector direction = player.getLocation().getDirection();
        direction.setY(0);
        direction.normalize();
        direction.multiply(horizontalSpeed);
        velocity.add(direction);
        player.setVelocity(velocity);
    }

    private boolean isWearingElytra(Player player) { // This checks if the player is currently wearing an Elytra and then prevents jetpack use if an Elytra is equipped
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && chestplate.getType() == Material.ELYTRA;
    }

    private void showJetpackParticles(Player player) { // Generates visual effects (particles) at the player's location to simulate the jetpack's exhaust
        Location location = player.getLocation().add(0, 1, 0);
        Vector direction = location.getDirection().multiply(-0.2);
        location.add(direction);
        player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, location, 2, 0.1, 0.1, 0.1, 0.02);
        player.getWorld().spawnParticle(Particle.FLAME, location, 2, 0.1, 0.1, 0.1, 0.02);

    }

    @EventHandler
    public void onPlayerFall(EntityDamageEvent event) { // Reduces fall damage if the player is using the jetpack
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = (Player) event.getEntity();
            if (jetpackManager.hasJetpackEquipped(player)) {
                double reducedDamage = event.getDamage() * (1 - (fallDamageReduction / 100.0));
                event.setDamage(reducedDamage);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) { // Prevents equipping an Elytra during the jetpack cooldown
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (!jetpackManager.canEquipElytra(player)) {
                long cooldownTime = getRemainingCooldownTime(player);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.ELYTRA) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lJetpack &c>> You must wait " + cooldownTime + " seconds before equipping an Elytra."));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) { // Prevents fast equipping of an Elytra during cooldown, the bypass you found ;)
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem();
        if (itemInHand != null && itemInHand.getType() == Material.ELYTRA) {
            if (!jetpackManager.canEquipElytra(player)) {
                long cooldownTime = getRemainingCooldownTime(player);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lJetpack &c>> You must wait " + cooldownTime + " seconds before equipping an Elytra."));
                event.setCancelled(true);
            }
        }
    }

    private long getRemainingCooldownTime(Player player) { // Calculates the remaining cooldown time before the player can equip an Elytra after using the jetpack, i only added this method because i want to include the remaining seconds in the message
        long lastUsedTime = jetpackManager.getJetpackLastUsedTime(player);
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastUsedTime;
        long cooldownDuration = 10000; // 10 seconds in milliseconds
        long timeRemaining = (cooldownDuration - timeElapsed + 999) / 1000; // Round up to the nearest second
        return Math.max(timeRemaining, 0); // Ensure it's not negative
    }
}
