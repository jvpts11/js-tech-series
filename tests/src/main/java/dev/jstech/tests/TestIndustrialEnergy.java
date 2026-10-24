/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.energy.VoltageClass;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.industrial.machine.BatteryRating;
import dev.jstech.industrial.machine.IndustrialJoiner;
import dev.jstech.industrial.machine.JoinerRating;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * J's Industrial's energy contract, on blocks of the test mod's own: a transformer of the T1 between low and medium
 * voltage, as the mod's first transformer will be, and a battery holding a hundred kilojoules at 2 kW.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestIndustrialEnergy {

    public static final JoinerRating TRANSFORMER_RATING = JoinerRating.transformer(IndustrialTier.T1, VoltageClass.LOW,
            VoltageClass.MEDIUM);
    public static final BatteryRating BATTERY_RATING = BatteryRating.of(IndustrialTier.T2, 100_000L, 2_000L);

    /* Its block state and model are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> TRANSFORMER = TestSounds.CONTENT.block("test_transformer",
                    Device.of(() -> TestIndustrialEnergy.TRANSFORMER_ENTITY.get())
                            .ticks(IndustrialJoiner::serverTick)::block)
            .named("Test transformer").look(IBlockLook.cubeAll("test_transformer")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransformerBlockEntity>>
            TRANSFORMER_ENTITY = TestSounds.CONTENT.blockEntity("test_transformer", TransformerBlockEntity::new,
            TRANSFORMER);
    public static final ItemEntry<Item> BATTERY = TestSounds.CONTENT.item("test_battery",
                    properties -> new Item(properties.stacksTo(1)))
            .named("Test Battery").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();

    private TestIndustrialEnergy() {
    }

    /** Declares the blocks and the battery, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        IndustrialJoiner.register(event, TRANSFORMER_ENTITY.get());
        BATTERY_RATING.holdIn(event, BATTERY.get());
    }

    /** The test transformer. */
    public static final class TransformerBlockEntity extends IndustrialJoiner {

        public TransformerBlockEntity(final BlockPos pos, final BlockState state) {
            super(TRANSFORMER_ENTITY.get(), pos, state, TRANSFORMER_RATING);
        }
    }
}
