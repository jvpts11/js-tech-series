/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.conveyor.ConveyorBlock;
import dev.jstech.core.conveyor.ConveyorBlockEntity;
import dev.jstech.core.conveyor.ConveyorDrive;
import dev.jstech.core.conveyor.ConveyorEnd;
import dev.jstech.core.conveyor.ConveyorType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Conveyors of every drive and end the kit has, to prove the Core's conveyors: one that moves by itself and carries
 * what stands on it, one that lets items fall off its end, one a shaft turns and one that runs on energy. Their block
 * states and model are written by hand, as the test mod's blocks are.
 */
public final class TestConveyors {

    /** How fast every test belt moves at its full speed, in blocks a second. */
    public static final double SPEED = 4.0;
    /** The shaft speed the turned belt moves at its full speed at. */
    public static final double RATED_RPM = 60.0;
    /** What each block of the turned belt asks of its network, in newton-metres. */
    public static final double TORQUE = 1.0;
    /** What each block of the powered belt draws while it carries anything. */
    public static final double WATTS = 20.0;

    /** Moves by itself, four items a block, carrying what stands on it; holds what reaches its end. */
    public static final ConveyorType FREE = ConveyorType.moving(SPEED).carriesEntities().build();
    /** Moves by itself and lets what reaches its end fall off. */
    public static final ConveyorType DROPPING = ConveyorType.moving(SPEED).end(ConveyorEnd.DROP).build();
    /** Moves as a shaft turns it. */
    public static final ConveyorType TURNED = ConveyorType.moving(SPEED)
            .drive(ConveyorDrive.mechanical(RATED_RPM, TORQUE)).build();
    /** Moves on energy. */
    public static final ConveyorType POWERED = ConveyorType.moving(SPEED).drive(ConveyorDrive.energy(WATTS)).build();

    public static final BlockEntry<ConveyorBlock> BELT = belt("test_conveyor", "Test conveyor", FREE);
    public static final BlockEntry<ConveyorBlock> DROPPING_BELT = belt("test_dropping_conveyor",
            "Test dropping conveyor", DROPPING);
    public static final BlockEntry<ConveyorBlock> TURNED_BELT = belt("test_turned_conveyor", "Test turned conveyor",
            TURNED);
    public static final BlockEntry<ConveyorBlock> POWERED_BELT = belt("test_powered_conveyor",
            "Test powered conveyor", POWERED);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBlockEntity>> CONVEYOR =
            TestSounds.CONTENT.blockEntity("test_conveyor",
                    (pos, state) -> new ConveyorBlockEntity(TestConveyors.CONVEYOR.get(), pos, state),
                    BELT, DROPPING_BELT, TURNED_BELT, POWERED_BELT);

    private TestConveyors() {
    }

    /** Declares the belts, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static BlockEntry<ConveyorBlock> belt(final String id, final String name, final ConveyorType type) {
        return TestSounds.CONTENT.block(id, properties -> new ConveyorBlock(properties, type,
                        () -> TestConveyors.CONVEYOR.get())).named(name)
                .look(IBlockLook.facing(new IBlockModel.Handmade("test_conveyor"))).register();
    }
}
