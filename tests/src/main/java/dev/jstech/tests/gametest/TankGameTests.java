/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.item.ItemStates;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.blockentity.TankBlockEntity;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Tank keeping its fluid in its item when it is broken, and getting it back when it is placed. */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TankGameTests {

    private static final BlockPos TANK = new BlockPos(2, 2, 2);
    private static final int HELD_MB = 3_000;

    private TankGameTests() {
    }

    @GameTest(template = "empty")
    public static void breaking_keepsTheFluidInTheItem(final GameTestHelper helper) {
        final TankBlockEntity tank = placeTank(helper);
        tank.fluidHandler().fill(new FluidStack(Fluids.WATER, HELD_MB), IFluidHandler.FluidAction.EXECUTE);
        final BlockPos at = helper.absolutePos(TANK);
        helper.getLevel().destroyBlock(at, true);
        helper.succeedWhen(() -> {
            final List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(at).inflate(2.0));
            helper.assertTrue(drops.size() == 1, "the tank drops one item, got " + drops.size());
            final ItemStack dropped = drops.get(0).getItem();
            helper.assertTrue(dropped.is(IndustrialModule.TANK.get().asItem()), "the drop is the tank");
            final SimpleFluidContent kept = dropped.getOrDefault(ItemStates.FLUID.get(), SimpleFluidContent.EMPTY);
            helper.assertTrue(kept.is(Fluids.WATER) && kept.getAmount() == HELD_MB,
                    "the item keeps the tank's water, got " + kept.getAmount() + " mB");
        });
    }

    @GameTest(template = "empty")
    public static void placing_aTankItemThatHoldsAFluidFillsTheTank(final GameTestHelper helper) {
        final ItemStack item = new ItemStack(IndustrialModule.TANK.get().asItem());
        item.set(ItemStates.FLUID.get(), SimpleFluidContent.copyOf(new FluidStack(Fluids.LAVA, HELD_MB)));
        final TankBlockEntity tank = placeTank(helper);
        tank.applyComponentsFromItemStack(item);
        helper.assertTrue(tank.fluid().is(Fluids.LAVA) && tank.fluid().getAmount() == HELD_MB,
                "the placed tank holds the item's lava, got " + tank.fluid().getAmount() + " mB");
        helper.succeed();
    }

    private static TankBlockEntity placeTank(final GameTestHelper helper) {
        helper.setBlock(TANK, IndustrialModule.TANK.get());
        if (!(helper.getBlockEntity(TANK) instanceof TankBlockEntity tank)) {
            helper.fail("no Tank at " + TANK);
            throw new IllegalStateException("unreachable");
        }
        return tank;
    }
}
