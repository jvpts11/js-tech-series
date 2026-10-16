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
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.BoxShape;
import dev.jstech.core.multiblock.InsideRule;
import dev.jstech.core.multiblock.Limit;
import dev.jstech.core.multiblock.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A multiblock machine of the test mod's own, built by the player block by block: a tank of any size from three to
 * seven a side, iron on its edges, iron or glass on its faces with hoppers among them as its valves, and empty inside,
 * its controller on a face. It runs as many recipes at once as it has valves, to prove the count reaches the machine.
 */
public final class TestStructures {

    /** The tank's shape: iron edges, iron, glass or hopper faces, air inside, three to seven a side. */
    public static final BoxShape TANK_SHAPE = BoxShape.of(Place.of(BlockMatch.blocks("minecraft:iron_block")),
                    Place.of(BlockMatch.blocks("minecraft:iron_block", "minecraft:glass", "minecraft:hopper"))
                            .counting("valve", BlockMatch.blocks("minecraft:hopper")),
                    InsideRule.empty(), new Limit(3, 7), new Limit(3, 7))
            .limit("valve", 0, 4);
    private static final int ENERGY = 1_000;

    /* Its block state and model are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> TANK = TestSounds.CONTENT.block("test_tank_controller",
            Device.of(() -> TestStructures.TANK_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test tank controller").look(IBlockLook.cubeAll("test_tank_controller")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_tank_controller", TankBlockEntity::new, TANK);

    private TestStructures() {
    }

    /** Declares the controller, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** The tank's controller: works whenever its structure stands, running a recipe at once for each valve. */
    public static final class TankBlockEntity extends MachineBlockEntity {

        public TankBlockEntity(final BlockPos pos, final BlockState state) {
            super(TANK_ENTITY.get(), pos, state, 0, MachineEnergy.fe(ENERGY, ENERGY, 0));
            formsWith(MachineStructure.of(TANK_SHAPE).parallel(formed -> Math.max(1, formed.count("valve"))));
        }

        @Override
        protected MachineStatus work(final ServerLevel level) {
            return MachineStatus.WORKING;
        }
    }
}
