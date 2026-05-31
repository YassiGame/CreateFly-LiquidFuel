package me.yassigame.createliquidfuel.core;

import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.zurrtum.create.foundation.fluid.FluidHelper;
import com.zurrtum.create.infrastructure.fluids.BucketFluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidItemInventory;
import me.yassigame.createliquidfuel.mixin.BlazeBurnerAccessor;
import me.yassigame.createliquidfuel.util.Triplet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

public class BurnerStomachHandler {
    public static final int TANK_CAPACITY = BucketFluidInventory.CAPACITY;
    public static final int UNITS_PER_MILLIBUCKET = TANK_CAPACITY / 1000;
    public static final Map<Fluid, Pair<Identifier, Triplet<Integer, Boolean, Integer>>> LIQUID_BURNER_FUEL_MAP = new HashMap<>();

    public static void tick(SmartBlockEntity entity) {
        if (!(entity instanceof BlazeBurnerAccessor burnerAccessor)) return;
        if (!(entity instanceof IHasStomach hasStomach)) return;
        if (entity.getLevel() == null || entity.getLevel().isClientSide()) return;

        SmartFluidTankBehaviour.TankSegment tank = hasStomach.getStomach().getPrimaryTank();
        if (tank.getFluid().isEmpty()) return;

        Pair<Identifier, Triplet<Integer, Boolean, Integer>> propertyPair =
                LIQUID_BURNER_FUEL_MAP.get(tank.getFluid().getFluid());
        if (propertyPair == null) return;

        Triplet<Integer, Boolean, Integer> burnerProperty = propertyPair.getSecond();
        if (burnerProperty == null) return;

        boolean fluidSuperHeats = burnerProperty.getSecond();

        int unitsConsuming = burnerProperty.getThird();

        if (tank.getFluid().getAmount() < unitsConsuming) {
            tank.extractAny();
            return;
        }

        int newBurnTime = burnerAccessor.createliquidfuel$getRemainingBurnTime() + burnerProperty.getFirst();
        if (newBurnTime > BlazeBurnerBlockEntity.MAX_HEAT_CAPACITY) return;

        if (fluidSuperHeats)
            burnerAccessor.createliquidfuel$invokeSetBlockHeat(BlazeBurnerBlock.HeatLevel.SEETHING);
        else
            burnerAccessor.createliquidfuel$invokeSetBlockHeat(BlazeBurnerBlock.HeatLevel.FADING);

        burnerAccessor.createliquidfuel$setRemainingBurnTime(newBurnTime);
        tank.extractAny(unitsConsuming);
    }

    public static void tryUpdateFuel(@NotNull SmartBlockEntity entity, ItemStack itemStack, boolean forceOverflow, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof IHasStomach hasStomach)) return;

        SmartFluidTankBehaviour.TankSegment tank = hasStomach.getStomach().getPrimaryTank();
        try (FluidItemInventory handler = FluidHelper.getFluidInventory(itemStack)) {
            if (handler == null || handler.size() != 1) return;

            com.zurrtum.create.infrastructure.fluids.FluidStack fluidStack = handler.getStack(0);
            if (fluidStack.isEmpty()) return;
            if (!LIQUID_BURNER_FUEL_MAP.containsKey(fluidStack.getFluid())) return;
            if (!tank.getFluid().isEmpty() && fluidStack.getFluid() != tank.getFluid().getFluid()) return;

            int space = TANK_CAPACITY - tank.getFluid().getAmount();
            if (space <= 0) return;
            if (fluidStack.getAmount() > space && !forceOverflow) return;

            int amountToInsert = Math.min(space, fluidStack.getAmount());
            if (!simulate) {
                if (tank.getFluid().isEmpty())
                    tank.setFluid(fluidStack.copyWithAmount(amountToInsert));
                else
                    tank.getFluid().increment(amountToInsert);
                tank.markDirty();
            }
        }

        cir.setReturnValue(true);
    }
}
