/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.network.INetworkMember;
import dev.jstech.core.jstechseries.network.NetworkCategory;
import dev.jstech.core.jstechseries.network.NetworkMembers;
import dev.jstech.core.jstechseries.network.NetworkMembership;
import dev.jstech.core.jstechseries.network.StateRow;
import dev.jstech.core.jstechseries.network.StateRows;
import dev.jstech.core.jstechseries.network.StateTable;
import dev.jstech.core.jstechseries.network.StateTables;
import dev.jstech.core.jstechseries.peripheral.ControlLinks;
import dev.jstech.core.jstechseries.peripheral.IPeripheralOwnerSupport;
import dev.jstech.core.jstechseries.peripheral.PeripheralCableType;
import dev.jstech.core.jstechseries.peripheral.PeripheralPorts;
import dev.jstech.core.jstechseries.peripheral.PortKind;
import dev.jstech.core.machine.IMachineParameters;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.jstechseries.operation.IOperationArgs;
import dev.jstech.core.jstechseries.operation.IOperationRequest;
import dev.jstech.core.jstechseries.operation.OperationCategory;
import dev.jstech.core.jstechseries.operation.OperationFailure;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.jstechseries.operation.OperationType;
import dev.jstech.core.jstechseries.operation.RegisterOperationTypesEvent;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.world.event.WorldEvents;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

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
    public static final TextKey TWIN_TITLE = TextKey.of("jstests.table.twin_reactors", "Twin reactors");
    public static final TextKey GAUGES_TITLE = TextKey.of("jstests.table.gauges", "Test gauges");
    public static final TextKey READING = TextKey.of("jstests.table.gauges.reading", "Reading");
    /**
     * A table of another namespace whose id's path is the test reactors' own, so a QUERY by that path alone names two
     * tables and has to be told the id.
     */
    public static final StateTable TWIN_REACTORS = StateTables.declare(StateTable.builder(
                    ResourceLocation.fromNamespaceAndPath("jstests_twin", "test_reactors"), TWIN_TITLE)
            .number("temperature", TEMPERATURE, "°C")
            .build());
    /** A table no machine has a row in, named alone by its id's path. */
    public static final StateTable GAUGES = StateTables.declare(StateTable.builder(
                    ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_gauges"), GAUGES_TITLE)
            .number("reading", READING, "")
            .build());
    public static final TextKey WATCHED_NAME = TextKey.of("jstests.world_event.watched_storm", "Watched storm");
    /**
     * A storm over the whole overworld that never comes by itself, warned of two seconds ahead and lasting two: the
     * one a test reactor can be the instrument of, apart from the storms the world's own tests plan.
     */
    public static final ResourceLocation WATCHED_STORM = WorldEvents.event(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_watched_storm"), WATCHED_NAME)
            .in(Level.OVERWORLD).warning(40).lasts(40).declare();
    public static final TextKey BEAM_STORM_NAME = TextKey.of("jstests.world_event.beam_storm", "Beam storm");
    /**
     * A storm of its own for the test whose job answers it by running the accelerator, so that test plans it while the
     * tests of the watched storm plan theirs.
     */
    public static final ResourceLocation BEAM_STORM = WorldEvents.event(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_beam_storm"), BEAM_STORM_NAME)
            .in(Level.OVERWORLD).warning(20).lasts(40).declare();
    public static final TextKey ACCELERATE_NAME = TextKey.of("jstests.operation.accelerate", "Accelerate");
    public static final TextKey ACCELERATOR_BUSY = TextKey.of("jstests.operation.accelerator_busy",
            "the accelerator is running another beam");
    /**
     * A kind of Operation of the test mod's own, which only its accelerator carries out: a run of so many seconds,
     * asked for in text as {@code RUN jstests:accelerate WITH seconds = 3}.
     */
    public static final OperationType<AccelerateArgs> ACCELERATE = new OperationType<>("jstests:accelerate",
            ACCELERATE_NAME, AccelerateArgs.class, OperationCategory.INDUSTRIAL, IndustrialTier.T1,
            EnumSet.of(NetworkCategory.C), AccelerateArgs.TEXT, request ->
            request.machine().orElse(null) instanceof AcceleratorBlockEntity accelerator
                    ? accelerator.accelerate(request) : OperationStatus.FAILED);

    public static final TextKey CONTROLLED_TITLE = TextKey.of("jstests.table.controlled", "Controlled machines");
    public static final TextKey CONTROLLED_MACHINE = TextKey.of("jstests.table.controlled.machine", "Machine");
    public static final TextKey CONTROLLED_ON = TextKey.of("jstests.table.controlled.on", "On");
    /** The machines a test controller reads, a row for each, where each stands. */
    public static final StateTable CONTROLLED = StateTables.declare(StateTable.builder(
                    ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_controlled"), CONTROLLED_TITLE)
            .text("machine", CONTROLLED_MACHINE)
            .flag("on", CONTROLLED_ON)
            .build());
    public static final TextKey SWITCH_OFF_NAME = TextKey.of("jstests.operation.switch_off", "Switch off");
    public static final TextKey NOT_READ = TextKey.of("jstests.operation.not_read",
            "the controller reads no machine at %s");
    /**
     * A kind of Operation a test controller carries out on a machine it reads: switching it off, the machine named by
     * where it stands, as {@code RUN jstests:switch_off WITH machine = '1, 2, 3'}.
     */
    public static final OperationType<SwitchOffArgs> SWITCH_OFF = new OperationType<>("jstests:switch_off",
            SWITCH_OFF_NAME, SwitchOffArgs.class, OperationCategory.INDUSTRIAL, IndustrialTier.T1,
            EnumSet.of(NetworkCategory.C), SwitchOffArgs.TEXT, request ->
            request.machine().orElse(null) instanceof ControllerBlockEntity controller
                    ? controller.switchOff(request.args()) : OperationStatus.FAILED);

    /** The data cables of every age, on every face: what the test machines join a network through. */
    public static final FacePorts DATA_PORTS = FacePorts.everyFace(DataLines.upTo(HardwareEra.SINGULARITY,
            DataLine.ACCESS, DataLine.BACKBONE));
    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> REACTOR = TestSounds.CONTENT.block("test_reactor",
            Device.of(() -> TestNetworkMembers.REACTOR_ENTITY.get()).ports(DATA_PORTS)::block)
            .named("Test reactor").look(IBlockLook.cubeAll("test_reactor")).register();
    public static final BlockEntry<DeviceBlock> ACCELERATOR = TestSounds.CONTENT.block("test_accelerator",
            Device.of(() -> TestNetworkMembers.ACCELERATOR_ENTITY.get()).ports(DATA_PORTS)
                    .ticks((level, pos, state, accelerator) -> accelerator.tick())::block)
            .named("Test accelerator").look(IBlockLook.cubeAll("test_accelerator")).register();
    public static final BlockEntry<DeviceBlock> CONTROLLER = TestSounds.CONTENT.block("test_controller",
            Device.of(() -> TestNetworkMembers.CONTROLLER_ENTITY.get()).ports(DATA_PORTS)
                    .ticks((level, pos, state, controller) -> controller.tick())::block)
            .named("Test controller").look(IBlockLook.cubeAll("test_controller")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControllerBlockEntity>>
            CONTROLLER_ENTITY = TestSounds.CONTENT.blockEntity("test_controller", ControllerBlockEntity::new,
            CONTROLLER);
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
        modBus.addListener(RegisterOperationTypesEvent.class, event -> {
            event.operations().register(ACCELERATE);
            event.operations().register(SWITCH_OFF);
        });
    }

    /**
     * What a request to switch a machine off carries: where the machine stands.
     *
     * @param machine its position, as {@code 1, 2, 3}
     */
    public record SwitchOffArgs(String machine) implements IOperationArgs {

        /** How a request written as text is read: {@code machine = '1, 2, 3'}. */
        public static final MapCodec<SwitchOffArgs> TEXT = RecordCodecBuilder.mapCodec(args -> args.group(
                Codec.STRING.fieldOf("machine").forGetter(SwitchOffArgs::machine)).apply(args, SwitchOffArgs::new));
    }

    /**
     * What a request to accelerate carries: how many seconds the beam runs, none for a run that is over at once.
     *
     * @param seconds how long it runs, from 0 to 600
     */
    public record AccelerateArgs(int seconds) implements IOperationArgs {

        /** How a request written as text is read: {@code seconds = 3}. */
        public static final MapCodec<AccelerateArgs> TEXT = RecordCodecBuilder.mapCodec(args -> args.group(
                Codec.intRange(0, 600).fieldOf("seconds").forGetter(AccelerateArgs::seconds))
                .apply(args, AccelerateArgs::new));
    }

    /** A reactor the network reads: its temperature, its power, whether it runs, and the name it was given. */
    public static class ReactorBlockEntity extends SyncedBlockEntity implements INetworkMember {

        private final NetworkMembership membership;
        private double temperature = 20.0;
        private double power;
        private boolean running;
        private String name = "";
        private List<ResourceLocation> instruments = List.of();

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

        /** Makes it the instrument of the world events {@code events}, as a test wants it. */
        public void watches(final ResourceLocation... events) {
            this.instruments = List.of(events);
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
        public List<ResourceLocation> instrumentOf() {
            return this.instruments;
        }

        @Override
        public void fill(final StateTable table, final StateRow.Writer row) {
            row.set("temperature", this.temperature).set("power", this.power).set("running", this.running)
                    .set("name", this.name);
        }
    }

    /**
     * An accelerator the network gives the test mod's kind of Operation to: one beam at a time, for the seconds asked,
     * stopped when the request is called off. It answers to the name a test gives it, as a machine to the name a
     * player gives it on its screen.
     */
    public static final class AcceleratorBlockEntity extends SyncedBlockEntity implements INetworkMember, Nameable {

        private final NetworkMembership membership;
        private @Nullable IOperationRequest<AccelerateArgs> running;
        private int ticksLeft;
        private int finished;
        private int stopped;
        private @Nullable Component customName;

        public AcceleratorBlockEntity(final BlockPos pos, final BlockState state) {
            super(ACCELERATOR_ENTITY.get(), pos, state);
            this.membership = NetworkMembers.keep(this);
        }

        /** Its membership of the network it touches. */
        public NetworkMembership membership() {
            return this.membership;
        }

        /** Starts a beam for the request: over at once for no seconds, refused while another runs. */
        public OperationStatus accelerate(final IOperationRequest<AccelerateArgs> request) {
            if (this.running != null) {
                request.fail(OperationFailure.of(ACCELERATOR_BUSY));
                return OperationStatus.FAILED;
            }
            if (request.args().seconds() == 0) {
                this.finished++;
                return OperationStatus.COMPLETED;
            }
            this.running = request;
            this.ticksLeft = request.args().seconds() * 20;
            request.whenCancelled(() -> {
                this.running = null;
                this.stopped++;
            });
            return OperationStatus.PROCESSING;
        }

        /** Runs the beam on; once its time is up, says the request is done. */
        public void tick() {
            if (this.running != null && --this.ticksLeft <= 0) {
                final IOperationRequest<AccelerateArgs> done = this.running;
                this.running = null;
                this.finished++;
                done.report(OperationStatus.COMPLETED);
            }
        }

        /** Whether a beam runs now. */
        public boolean running() {
            return this.running != null;
        }

        /** How many beams ran to the end. */
        public int finished() {
            return this.finished;
        }

        /** How many beams were stopped by their request being called off. */
        public int stopped() {
            return this.stopped;
        }

        /** Gives it a name, as a test wants it. */
        public void name(final String name) {
            this.customName = name.isEmpty() ? null : Component.literal(name);
        }

        @Override
        public NetworkCategory category() {
            return NetworkCategory.C;
        }

        @Override
        public List<OperationType<?>> carriesOut() {
            return List.of(ACCELERATE);
        }

        @Override
        public Component getName() {
            return this.customName != null ? this.customName : getBlockState().getBlock().getName();
        }

        @Override
        public @Nullable Component getCustomName() {
            return this.customName;
        }
    }

    /**
     * A controller on the industrial control cable, as a mod builds its own: it has as many control ports as a test
     * gives it, keeps its links every second, writes a row of the controlled machines' table for each machine it reads,
     * where that machine stands, and switches one off when the network asks, refusing a machine it does not read.
     */
    public static final class ControllerBlockEntity extends SyncedBlockEntity implements INetworkMember,
            IPeripheralOwnerSupport {

        private final PeripheralPorts ports = new PeripheralPorts();
        private int controlPorts = 2;
        private int keepIn;

        public ControllerBlockEntity(final BlockPos pos, final BlockState state) {
            super(CONTROLLER_ENTITY.get(), pos, state);
            NetworkMembers.keep(this);
            fields().whenBroken((level, at) -> ControlLinks.release(level, at, this));
        }

        /** Gives it {@code count} control ports, as a test wants it. */
        public void controlPorts(final int count) {
            this.controlPorts = count;
        }

        /** Keeps its links at once, as a test wants it, rather than at its next second. */
        public void keepNow() {
            if (this.level instanceof ServerLevel server) {
                ControlLinks.keep(server, this.worldPosition, this);
            }
        }

        /** Its server tick: its links kept once a second. */
        public void tick() {
            if (--this.keepIn <= 0) {
                this.keepIn = ControlLinks.KEEP_EVERY;
                keepNow();
            }
        }

        /** The machines it reads, where each stands. */
        public List<Long> machines() {
            final List<Long> read = new ArrayList<>();
            for (final long linked : linkedEndpoints()) {
                if (kindOf(linked) == PortKind.CONTROL) {
                    read.add(linked);
                }
            }
            return read;
        }

        /** Switches off the machine {@code args} names, through the values it offers. */
        public OperationStatus switchOff(final SwitchOffArgs args) {
            final BlockPos at = read(args.machine());
            final IMachineParameters values = at == null || this.level == null ? null
                    : this.level.getCapability(IMachineParameters.CAPABILITY, at, null);
            return values != null && values.write("on", 0) ? OperationStatus.COMPLETED : OperationStatus.FAILED;
        }

        @Override
        public @Nullable Text refuses(final OperationType<?> type, final IOperationArgs args) {
            return args instanceof SwitchOffArgs off && read(off.machine()) == null ? NOT_READ.with(off.machine())
                    : null;
        }

        @Override
        public NetworkCategory category() {
            return NetworkCategory.C;
        }

        @Override
        public List<OperationType<?>> carriesOut() {
            return List.of(SWITCH_OFF);
        }

        @Override
        public List<StateTable> tables() {
            return List.of(CONTROLLED);
        }

        @Override
        public void fillRows(final StateTable table, final StateRows rows) {
            if (this.level == null) {
                return;
            }
            for (final long machine : machines()) {
                final BlockPos at = BlockPos.of(machine);
                final IMachineParameters values = this.level.getCapability(IMachineParameters.CAPABILITY, at, null);
                rows.at(at).set("machine", GameText.of(this.level.getBlockState(at).getBlock().getName()).english())
                        .set("on", values != null && values.read("on").orElse(0) >= 0.5);
            }
        }

        @Override
        public PeripheralPorts peripheralPorts() {
            return this.ports;
        }

        @Override
        public void markPeripheralChange() {
            setChanged();
        }

        @Override
        public PeripheralCableType cableType() {
            return PeripheralCableType.INDUSTRIAL_CONTROL;
        }

        @Override
        public int ports(final PortKind kind) {
            return kind == PortKind.CONTROL ? this.controlPorts : 0;
        }

        /* The machine standing where {@code written} says, "x, y, z", when it is one it reads; null otherwise. */
        private @Nullable BlockPos read(final String written) {
            final String[] parts = written.split(",");
            if (parts.length != 3) {
                return null;
            }
            try {
                final BlockPos at = new BlockPos(Integer.parseInt(parts[0].strip()), Integer.parseInt(parts[1].strip()),
                        Integer.parseInt(parts[2].strip()));
                return isLinked(at.asLong()) ? at : null;
            } catch (final NumberFormatException notAPlace) {
                return null;
            }
        }
    }
}
