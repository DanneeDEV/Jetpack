package org.nordcraft;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class Plugin extends JavaPlugin { 
    private static final Logger LOGGER = Logger.getLogger("jetpack");
    private JetpackManager jetpackManager;
    private FuelHandler fuelHandler;
    private PlayerFlightHandler flightHandler;

    public void onEnable() { // This is called when the plugin is enabled (e.g., when the server starts or the plugin is reloaded)
        // Loading the config.yml (or creating a new one if it doesnt exist)
        this.saveDefaultConfig();
        // Retrieves all values below from the config.yml
        int jetpackModelData = this.getConfig().getInt("jetpack-model-data");
        int fuelModelData = this.getConfig().getInt("fuel-model-data");
        int fuelConsumptionRate = this.getConfig().getInt("fuel-consumption-rate");
        int fallDamageReduction = this.getConfig().getInt("fall-damage-reduction");
        double liftForce = this.getConfig().getDouble("jetpack-lift-force");
        double horizontalSpeed = this.getConfig().getDouble("jetpack-horizontal-speed");


        // Initializes the core components of the jetpacks functionality
        jetpackManager = new JetpackManager(jetpackModelData); // Manages jetpack states and usage
        fuelHandler = new FuelHandler(this, fuelModelData, fuelConsumptionRate); // Handles the fuel consumption logic for the jetpack
        flightHandler = new PlayerFlightHandler(this, fuelHandler, jetpackManager, liftForce, horizontalSpeed, fallDamageReduction); // Manages player interactions with the jetpack, like flying and handling events
        this.getServer().getPluginManager().registerEvents(flightHandler, this); // Registers PlayerFlightHandler as an event listener so that it can respond to various server events (like player sneaking, quitting, or teleporting)


        LOGGER.info("Jetpack plugin enabled!");
    }

    public void onDisable() {
        LOGGER.info("Jetpack plugin disabled!");
    }

}
