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
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.conveyor.ChuteBlock;
import dev.jstech.core.conveyor.ConveyorBlock;
import dev.jstech.core.conveyor.ConveyorBlockEntity;
import dev.jstech.core.conveyor.ConveyorDrive;
import dev.jstech.core.conveyor.ConveyorEnd;
import dev.jstech.core.conveyor.ConveyorType;
import dev.jstech.core.conveyor.DiverterBlock;
import dev.jstech.core.conveyor.ElevatorBlock;
import dev.jstech.core.conveyor.IBeltWorker;
import dev.jstech.core.conveyor.JoinBlock;
import dev.jstech.core.conveyor.SplitterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Conveyors of every drive, end and piece the kit has, to prove the Core's conveyors: a belt that moves by itself and
 * carries what stands on it, one that lets items fall off its end, one a shaft turns and one that runs on energy; a
 * chute, a bucket elevator, a splitter, a join and a diverter; and a press that works cobblestone into gravel on the
 * belt under it. Their block states and models are written by hand, as the test mod's blocks are.
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
    /** How many ticks the press works an item. */
    public static final int PRESSING_TICKS = 10;

    /** Moves by itself, four items a block, carrying what stands on it; holds what reaches its end. */
    public static final ConveyorType FREE = ConveyorType.moving(SPEED).carriesEntities().build();
    /** Moves by itself and lets what reaches its end fall off. */
    public static final ConveyorType DROPPING = ConveyorType.moving(SPEED).end(ConveyorEnd.DROP).build();
    /** Moves as a shaft turns it. */
    public static final ConveyorType TURNED = ConveyorType.moving(SPEED)
            .drive(ConveyorDrive.mechanical(RATED_RPM, TORQUE)).build();
    /** Moves on energy. */
    public static final ConveyorType POWERED = ConveyorType.moving(SPEED).drive(ConveyorDrive.energy(WATTS)).build();
    /** A chute's: items fall by themselves, and out of its bottom with nothing there. */
    public static final ConveyorType FALLING = ConveyorType.moving(SPEED).end(ConveyorEnd.DROP).build();

    public static final BlockEntry<ConveyorBlock> BELT = TestSounds.CONTENT.block("test_conveyor",
                    properties -> new ConveyorBlock(properties, FREE, () -> TestConveyors.CONVEYOR.get()))
            .named("Test conveyor").look(IBlockLook.facing(new IBlockModel.Handmade("test_conveyor"))).item()
            .itemLook(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final BlockEntry<ConveyorBlock> DROPPING_BELT = belt("test_dropping_conveyor",
            "Test dropping conveyor", DROPPING);
    public static final BlockEntry<ConveyorBlock> TURNED_BELT = belt("test_turned_conveyor", "Test turned conveyor",
            TURNED);
    public static final BlockEntry<ConveyorBlock> POWERED_BELT = belt("test_powered_conveyor",
            "Test powered conveyor", POWERED);
    public static final BlockEntry<ChuteBlock> CHUTE = TestSounds.CONTENT.block("test_chute",
                    properties -> new ChuteBlock(properties, FALLING, () -> TestConveyors.CONVEYOR.get()))
            .named("Test chute").look(IBlockLook.fixed(new IBlockModel.Handmade("test_chute"))).register();
    public static final BlockEntry<ElevatorBlock> ELEVATOR = TestSounds.CONTENT.block("test_elevator",
                    properties -> new ElevatorBlock(properties, FREE, () -> TestConveyors.CONVEYOR.get()))
            .named("Test bucket elevator").look(IBlockLook.facing(new IBlockModel.Handmade("test_elevator")))
            .register();
    public static final BlockEntry<SplitterBlock> SPLITTER = TestSounds.CONTENT.block("test_splitter",
                    properties -> new SplitterBlock(properties, FREE, () -> TestConveyors.CONVEYOR.get()))
            .named("Test splitter").look(IBlockLook.facing(new IBlockModel.Handmade("test_splitter"))).register();
    public static final BlockEntry<JoinBlock> JOIN = TestSounds.CONTENT.block("test_join",
                    properties -> new JoinBlock(properties, FREE, () -> TestConveyors.CONVEYOR.get()))
            .named("Test join").look(IBlockLook.facing(new IBlockModel.Handmade("test_join"))).register();
    public static final BlockEntry<DiverterBlock> DIVERTER = TestSounds.CONTENT.block("test_diverter",
                    properties -> new DiverterBlock(properties, FREE, () -> TestConveyors.CONVEYOR.get()))
            .named("Test diverter").look(IBlockLook.facing(new IBlockModel.Handmade("test_diverter"))).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBlockEntity>> CONVEYOR =
            TestSounds.CONTENT.blockEntity("test_conveyor",
                    (pos, state) -> new ConveyorBlockEntity(TestConveyors.CONVEYOR.get(), pos, state),
                    BELT, DROPPING_BELT, TURNED_BELT, POWERED_BELT, CHUTE, ELEVATOR, SPLITTER, JOIN, DIVERTER);
    public static final BlockEntry<BeltPressBlock> BELT_PRESS = TestSounds.CONTENT.block("test_belt_press",
            BeltPressBlock::new).named("Test belt press").look(IBlockLook.cubeAll("test_belt_press")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BeltPressBlockEntity>> BELT_PRESS_ENTITY =
            TestSounds.CONTENT.blockEntity("test_belt_press", BeltPressBlockEntity::new, BELT_PRESS);

    private TestConveyors() {
    }

    /** Declares the conveyors, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static BlockEntry<ConveyorBlock> belt(final String id, final String name, final ConveyorType type) {
        return TestSounds.CONTENT.block(id, properties -> new ConveyorBlock(properties, type,
                        () -> TestConveyors.CONVEYOR.get())).named(name)
                .look(IBlockLook.facing(new IBlockModel.Handmade("test_conveyor"))).register();
    }

    /** The press's block, whose block entity the belt under it calls. */
    public static final class BeltPressBlock extends Block implements EntityBlock {

        public BeltPressBlock(final Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new BeltPressBlockEntity(pos, state);
        }
    }

    /** Presses cobblestone on the belt under it into gravel, an item at a time, and counts the items it gave up on. */
    public static final class BeltPressBlockEntity extends BlockEntity implements IBeltWorker {

        private int pressed;
        private int abandoned;

        public BeltPressBlockEntity(final BlockPos pos, final BlockState state) {
            super(BELT_PRESS_ENTITY.get(), pos, state);
        }

        /** How many items it was working went away before it was done. */
        public int timesAbandoned() {
            return this.abandoned;
        }

        @Override
        public boolean works(final ItemStack item) {
            return item.is(Items.COBBLESTONE);
        }

        @Override
        public @Nullable ItemStack work(final ItemStack item) {
            if (++this.pressed < PRESSING_TICKS) {
                return null;
            }
            this.pressed = 0;
            return new ItemStack(Items.GRAVEL, item.getCount());
        }

        @Override
        public void abandoned() {
            this.pressed = 0;
            this.abandoned++;
        }
    }
}
