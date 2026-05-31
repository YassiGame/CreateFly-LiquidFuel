package me.yassigame.createliquidfuel.eventhandlers;

import com.zurrtum.create.AllBlockEntityTypes;
import me.yassigame.createliquidfuel.core.IHasStomach;
import com.zurrtum.create.infrastructure.transfer.FluidInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;

public class ModEventHandler {
    public static void registerApiLookups() {
        FluidStorage.SIDED.registerForBlockEntity(
                (blockEntity, side) -> FluidInventoryStorage.of(
                        ((IHasStomach) blockEntity).getStomach().getCapability(),
                        side
                ),
                AllBlockEntityTypes.HEATER
        );
    }
}
