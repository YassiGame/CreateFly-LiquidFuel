package me.yassigame.createliquidfuel.core;

import me.yassigame.createliquidfuel.CreateLiquidFuel;
import me.yassigame.createliquidfuel.util.Triplet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import net.minecraft.IdentifierException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.material.Fluid;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class LiquidBurnerFuelJsonLoader implements ResourceManagerReloadListener {
    public static final Identifier IDENTIFIER = Identifier.fromNamespaceAndPath(CreateLiquidFuel.MOD_ID, "drainable_fuel_loader");
    private static final FileToIdConverter JSON_LISTER = FileToIdConverter.json("compat");
    public static final LiquidBurnerFuelJsonLoader INSTANCE = new LiquidBurnerFuelJsonLoader();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        BurnerStomachHandler.LIQUID_BURNER_FUEL_MAP.clear();

        for (Map.Entry<Identifier, Resource> entry : JSON_LISTER.listMatchingResources(resourceManager).entrySet()) {
            Identifier id = JSON_LISTER.fileToId(entry.getKey());
            try (var reader = entry.getValue().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    throw new JsonParseException("Expected a JSON object");
                }
                loadFuel(id, element.getAsJsonObject());
            } catch (IOException | JsonParseException | IllegalStateException e) {
                throw new RuntimeException("Failed to load liquid burner fuel " + id, e);
            }
        }

        CreateLiquidFuel.LOGGER.info("Loaded {} liquid burner fuels", BurnerStomachHandler.LIQUID_BURNER_FUEL_MAP.size());
    }

    private static void loadFuel(Identifier id, JsonObject object) {
        JsonElement fluidElement = object.get("fluid");
        if (fluidElement == null) {
            throw new JsonParseException("No fluid specified for liquid burner fuel " + id);
        }

        Identifier fluidId;
        try {
            fluidId = Identifier.parse(fluidElement.getAsString());
        } catch (IdentifierException e) {
            throw new JsonParseException("Liquid burner fuel " + id + " has invalid fluid: " + fluidElement, e);
        }

        Optional<Fluid> fluid = BuiltInRegistries.FLUID.getOptional(fluidId);
        if (fluid.isEmpty()) return;

        boolean superHeat = object.has("superHeat") && object.get("superHeat").getAsBoolean();
        int burnTime = object.has("burnTime") ? object.get("burnTime").getAsInt() : superHeat ? 32 : 20;
        int amountConsumedPerTick = object.has("amountConsumedPerTick")
                ? object.get("amountConsumedPerTick").getAsInt()
                : superHeat ? 10 : 1;

        if (burnTime <= 0 || amountConsumedPerTick <= 0) {
            throw new JsonParseException("Liquid burner fuel " + id + " must have positive burn and consumption values");
        }

        BurnerStomachHandler.LIQUID_BURNER_FUEL_MAP.put(
                fluid.get(),
                Pair.of(
                        id,
                        Triplet.of(
                                burnTime,
                                superHeat,
                                amountConsumedPerTick * BurnerStomachHandler.UNITS_PER_MILLIBUCKET
                        )
                )
        );
    }
}
