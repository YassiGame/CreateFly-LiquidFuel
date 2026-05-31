package me.yassigame.createliquidfuel;

import me.yassigame.createliquidfuel.core.LiquidBurnerFuelJsonLoader;
import me.yassigame.createliquidfuel.eventhandlers.ModEventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CreateLiquidFuel implements ModInitializer {
    public static final String MOD_ID = "createliquidfuel";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                LiquidBurnerFuelJsonLoader.IDENTIFIER,
                LiquidBurnerFuelJsonLoader.INSTANCE
        );
        ModEventHandler.registerApiLookups();
    }
}
