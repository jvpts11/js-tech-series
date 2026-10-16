/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.api.CoreRegisterEvent;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.network.INetworkMember;
import dev.jstech.core.network.NetworkCategory;
import dev.jstech.core.network.NetworkMembers;
import dev.jstech.core.network.NetworkMembership;
import dev.jstech.core.network.StateRow;
import dev.jstech.core.network.StateTable;
import dev.jstech.core.network.StateTables;
import dev.jstech.core.operation.IOperationArgs;
import dev.jstech.core.operation.OperationCategory;
import dev.jstech.core.operation.OperationStatus;
import dev.jstech.core.operation.OperationType;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.IndustrialTier;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Machines of the test mod on the data network through the Core, as a mod with no tie to J's Computers puts its own:
 * a reactor the network reads, with a row in a table of reactors, and an accelerator the network gives a kind of
 * Operation of the test mod's own.
 */
@TextHolder
public final class TestNetworkMembers {

    public static final TextKey REACTORS_TITLE = TextKey.of("jstests.table.reactors", "Test reactors");
    public static final TextKey TEMPERATURE = TextKey.of("jstests.table.reactors.temperature", "Temperature");
    public static final TextKey POWER = TextKey.of("jstests.table.reactors.power", "Power");
    public static final TextKey RUNNING = TextKey.of("jstests.table.reactors.running", "Running");
    public static final TextKey NAME = TextKey.of("jstests.table.reactors.name", "Name");

    /** The test reactors' state: a row for each, a column for each value it shows. */
    public static final StateTable REACTORS = StateTables.declare(StateTable.builder(
                    ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_reactors"), REACTORS_TITLE)
            .number("temperature", TEMPERATURE, "°C")
            .number("power", POWER, "W")
            .flag("running", RUNNING)
            .text("name", NAME)
            .build());
    /** A kind of Operation of the test mod's own, which only its accelerator carries out. */
    public static final OperationType<AccelerateArgs> ACCELERATE = new OperationType<>("jstests:accelerate",
            AccelerateArgs.class, OperationCategory.INDUSTRIAL, IndustrialTier.T1, EnumSet.of(NetworkCategory.C),
            args -> OperationStatus.COMPLETED);

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> REACTOR = TestSounds.CONTENT.block("test_reactor",
            Device.of(() -> TestNetworkMembers.REACTOR_ENTITY.get())::block)
            .named("Test reactor").look(IBlockLook.cubeAll("test_reactor")).register();
    public static final BlockEntry<DeviceBlock> ACCELERATOR = TestSounds.CONTENT.block("test_accelerator",
            Device.of(() -> TestNetworkMembers.ACCELERATOR_ENTITY.get())::block)
            .named("Test accelerator").look(IBlockLook.cubeAll("test_accelerator")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorBlockEntity>> REACTOR_ENTITY =
            TestSounds.CONTENT.blockEntity("test_reactor",
                    (pos, state) -> new ReactorBlockEntity(TestNetworkMembers.REACTOR_ENTITY.get(), pos, state),
                    REACTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorBlockEntity>>
            ACCELERATOR_ENTITY = TestSounds.CONTENT.blockEntity("test_accelerator", AcceleratorBlockEntity::new,
            ACCELERATOR);

    private TestNetworkMembers() {
    }

    /** Declares the machines, before the test mod's content is registered, and its kind of Operation. */
    public static void declare(final IEventBus modBus) {
        modBus.addListener(CoreRegisterEvent.class, event -> event.operations().register(ACCELERATE));
    }

    /** What a request to accelerate carries: nothing. */
    public record AccelerateArgs() implements IOperationArgs {
    }

    /** A reactor the network reads: its temperature, its power, whether it runs, and the name it was given. */
    public static class ReactorBlockEntity extends SyncedBlockEntity implements INetworkMember {

        private final NetworkMembership membership;
        private double temperature = 20.0;
        private double power;
        private boolean running;
        private String name = "";

        public ReactorBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
            super(type, pos, state);
            this.membership = NetworkMembers.keep(this);
        }

        /** Its membership of the network it touches. */
        public NetworkMembership membership() {
            return this.membership;
        }

        /** Sets what it reads, as a test wants it. */
        public void reads(final double celsius, final double watts, final boolean on, final String called) {
            this.temperature = celsius;
            this.power = watts;
            this.running = on;
            this.name = called;
        }

        @Override
        public NetworkCategory category() {
            return NetworkCategory.B;
        }

        @Override
        public List<StateTable> tables() {
            return List.of(REACTORS);
        }

        @Override
        public void fill(final StateTable table, final StateRow.Writer row) {
            row.set("temperature", this.temperature).set("power", this.power).set("running", this.running)
                    .set("name", this.name);
        }
    }

    /** An accelerator the network gives the test mod's kind of Operation to. */
    public static final class AcceleratorBlockEntity extends SyncedBlockEntity implements INetworkMember {

        public AcceleratorBlockEntity(final BlockPos pos, final BlockState state) {
            super(ACCELERATOR_ENTITY.get(), pos, state);
            NetworkMembers.keep(this);
        }

        @Override
        public NetworkCategory category() {
            return NetworkCategory.C;
        }

        @Override
        public List<OperationType<?>> carriesOut() {
            return List.of(ACCELERATE);
        }
    }
}
