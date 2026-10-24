/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.IFaceGate;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.client.audio.SoundDirector;
import dev.jstech.core.energy.EnergyFaults;
import dev.jstech.core.energy.EnergyProtection;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.heat.HeatBody;
import dev.jstech.core.heat.HeatMath;
import dev.jstech.core.heat.HeatPorts;
import dev.jstech.core.heat.HeatReadings;
import dev.jstech.core.heat.IHeatHolder;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.mechanical.IMechanicalHolder;
import dev.jstech.core.mechanical.IMechanicalPart;
import dev.jstech.core.mechanical.MechanicalPorts;
import dev.jstech.core.inventory.FaceEnergyStorage;
import dev.jstech.core.inventory.FaceFluidHandler;
import dev.jstech.core.inventory.FaceItemHandler;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.FaceMatterHandler;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.heat.IHeatPort;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.multiblock.IMultiblockPortHost;
import dev.jstech.core.multiblock.IRedstonePortHost;
import dev.jstech.core.multiblock.IStructureTerminal;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.physics.Physics;
import dev.jstech.core.physics.PhysicsMath;
import dev.jstech.core.signal.ISignalOutput;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.IOwned;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A machine of any mod: an item inventory and its energy (FE, or a mod's energy through a port of a class with its
 * fuse and battery, {@link MachineEnergy}), both saved, both offered to pipes and cables, the items spilled when the
 * machine is broken; and what every machine shares: its state always in view ({@link MachineStatus}), its ON/OFF
 * switch and its answer to redstone ({@link MachineControl}), its health, which explosions and burns take and repair
 * kits give back ({@link MachineHealth}), its owner and who else may use it, the values it lets the outside read and
 * set ({@link IMachineParameters}), what a comparator and any reader of sensors read of it ({@link ISignalOutput}),
 * its sounds and particles as it starts, works and stops, and, for a machine that makes heat, its casing's heat
 * ({@link MachineHeat}), which stops it to cool past its limit.
 *
 * <p>A machine works only when it has something to do: each tick it is ticked through {@link #serverTick}, which
 * runs {@link #work} unless the machine rests, asleep, waiting on its inventory, its energy, its settings, its
 * neighbours or its health to change, as anything that changes them wakes it.
 */
@TextHolder
public abstract class MachineBlockEntity extends SyncedBlockEntity implements IOwned, IMachineParameters,
        ISignalOutput, IDescribed, IAudible, IFaceGate, IHeatHolder, IMechanicalHolder, IMultiblockPortHost,
        IRedstonePortHost, IStructureTerminal {

    private final FieldItemHandler inventory;
    private final @Nullable FieldEnergyStorage energy;
    private final MachinePower power;
    private final MachineControl control;
    private final MachineHealth health;
    private final MachineSides sides;
    private final ValueField<MachineStatus> status;
    private final BoolField running;
    private final ValueField<Ownership> ownership;
    private final LongField drawn;
    private final IntField perMinute;
    private final LongField capacityShown;
    private final IntField loudness;
    private UpgradeEffect applied = UpgradeEffect.NONE;
    private final Deque<long[]> made = new ArrayDeque<>();
    private long drawnThisTick;
    /* Its last ten minutes, for its screen's page of statistics, and what that page shows as of the last second. */
    private final MachineStatistics statistics = new MachineStatistics();
    private MachineHistory history = MachineHistory.NONE;
    private @Nullable List<MachineParameter> parameters;
    private boolean asleep;
    private long wakeAt;
    private boolean signalRead;
    private int lastStrength = -1;
    /* Whether the kit ticks it: a machine that ticks itself keeps no state of the kit's to tell. */
    private boolean ticked;
    /* Its heat, for a machine that makes heat; declared from its constructor. */
    private @Nullable MachineHeat heat;
    /* Whether a fault of its own only stops it and harms nothing; declared from its constructor. */
    private boolean failsSafe;
    /* Its shaft, for a motor or a dynamo; declared from its constructor. */
    private @Nullable MachineShaft shaft;
    /* Its structure, for a multiblock's terminal; declared from its constructor. */
    private @Nullable MachineStructure structure;
    /* What each of its structure's redstone ports that read hears, by the port's place. */
    private final Map<BlockPos, Integer> portSignals = new HashMap<>();
    /* The part of a tick its place's rate of time has left it, towards its next work. */
    private double timeCarried;

    public static final TextKey PARAMETER_ON = TextKey.of("jscore.machine.parameter.on", "On");
    public static final TextKey PARAMETER_PROGRESS = TextKey.of("jscore.machine.parameter.progress", "Progress");
    public static final TextKey PARAMETER_ENERGY = TextKey.of("jscore.machine.parameter.energy", "Energy");
    public static final TextKey PARAMETER_HEALTH = TextKey.of("jscore.machine.parameter.health", "Health");
    public static final TextKey PARAMETER_INPUT = TextKey.of("jscore.machine.parameter.input", "Input");
    public static final TextKey PARAMETER_OUTPUT = TextKey.of("jscore.machine.parameter.output", "Output");
    public static final TextKey STATUS_LINE = TextKey.of("jscore.machine.status_line", "State: %s");
    public static final TextKey HEALTH_LINE = TextKey.of("jscore.machine.health_line", "Health: %s%%");
    /** How long a resting machine sleeps at most before it looks again, in ticks, whatever woke it or not. */
    public static final int SLEEP_AT_MOST = 100;
    /** What an explosion takes of a machine's health, rather than breaking it. */
    public static final int EXPLOSION_DAMAGE = 50;
    /** What a line too high takes of a machine's health as it burns its port. */
    public static final int BURN_DAMAGE = 50;
    private static final int TICKS_A_SECOND = 20;
    private static final int TICKS_A_MINUTE = 1_200;
    private static final int PERCENT = 100;
    private static final String REDSTONE_MODE = "redstone_mode";
    private static final String COMPARATOR_READS = "comparator_reads";
    private static final StableNames<RedstoneMode> MODES = StableNames.of(RedstoneMode.class);
    private static final StableNames<ComparatorReads> READINGS = StableNames.of(ComparatorReads.class);

    protected MachineBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                 final int inventorySize, final int energyCapacity, final int energyMaxReceive,
                                 final int energyMaxExtract) {
        this(type, pos, state, inventorySize, MachineEnergy.fe(energyCapacity, energyMaxReceive, energyMaxExtract));
    }

    protected MachineBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                 final int inventorySize, final MachineEnergy declared) {
        super(type, pos, state);
        this.inventory = fields().items("Inventory", inventorySize).save().exposed().dropsWhenBroken();
        if (declared.fe()) {
            this.energy = fields().energy("Energy", (int) Math.min(Integer.MAX_VALUE, declared.capacity()),
                    (int) Math.min(Integer.MAX_VALUE, declared.maxReceive()),
                    (int) Math.min(Integer.MAX_VALUE, declared.maxExtract())).save().toMenu().exposed();
            this.power = MachinePower.fe(this.energy);
        } else {
            this.energy = null;
            final PartField[] stored = new PartField[1];
            final EnergyStore store = new EnergyStore("Energy", declared.energy(),
                    Objects.requireNonNull(declared.portClass()), declared.capacity(), declared.maxReceive(),
                    declared.maxExtract(), () -> stored[0].changed());
            stored[0] = fields().part("Energy", store).save().toClient();
            final PartField[] guarded = new PartField[1];
            final EnergyProtection protection = new EnergyProtection(() -> guarded[0].changed());
            guarded[0] = fields().part("Protection", protection).save().toClient();
            fields().spills(protection.slots());
            store.guardedBy(protection);
            store.onFault(what -> {
                damage(BURN_DAMAGE);
                if (this.level instanceof ServerLevel server) {
                    EnergyFaults.strike(server, this.worldPosition, what);
                }
            });
            this.power = MachinePower.port(store, protection);
        }
        this.control = MachineControl.in(fields());
        this.health = MachineHealth.in(fields());
        final PartField[] sidesField = new PartField[1];
        this.sides = new MachineSides(this::sidesFor, () -> {
            sidesField[0].changed();
            if (this.level != null) {
                this.level.invalidateCapabilities(this.worldPosition);
            }
        });
        sidesField[0] = fields().part("Sides", this.sides).save().toClient();
        this.status = fields().value("Status", StableCodecs.byName(MachineStatus.class), MachineStatus.WAITING)
                .toClient();
        this.running = fields().flag("Running", false).toClient();
        this.ownership = fields().nullable("Owner", Ownership.CODEC).save().toClient();
        // Shown only on its screen, while it is open: what it drew last tick and what it made the last minute.
        this.drawn = fields().longInteger("Drawn", 0L).toMenu();
        this.perMinute = fields().integer("PerMinute", 0).toMenu();
        this.capacityShown = fields().longInteger("CapacityShown", this.power.builtCapacity()).toMenu();
        // How loud its upgrades make it, in hundredths, for the sounds the players near it hear.
        this.loudness = fields().integer("Loudness", PERCENT).toClient();
        fields().whenNeighbourChanges((server, face) -> readSignal(server));
    }

    /**
     * One server tick of {@code machine}, the ticker every machine is ticked by: its battery, its cracks, and, unless
     * it rests, what stops it or else its {@link #work}.
     */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final MachineBlockEntity machine) {
        if (level instanceof ServerLevel server) {
            // Time runs at its place's rate: where it runs at half speed a machine works every other tick.
            final double rate = Physics.lawsAt(server, Vec3.atCenterOf(pos)).timeRate();
            if (rate == 1.0) {
                machine.timeCarried = 0;
                machine.tickServer(server);
                return;
            }
            final double[] ticks = PhysicsMath.ticks(rate, machine.timeCarried);
            machine.timeCarried = ticks[1];
            for (int i = 0; i < (int) ticks[0]; i++) {
                machine.tickServer(server);
            }
        }
    }

    public FieldItemHandler getInventory() {
        return inventory;
    }

    /** Its FE store; none for a machine that runs on a port of a class ({@link #power} serves both). */
    public @Nullable FieldEnergyStorage getEnergy() {
        return energy;
    }

    /** Its energy at work, whichever it runs on. */
    public MachinePower power() {
        return power;
    }

    /**
     * Whether it takes energy at all now: a machine whose energy a setting turns off offers no port and no FE then.
     * Every machine does, unless it says otherwise.
     */
    public boolean takesEnergy() {
        return true;
    }

    /**
     * Whether the player's inventory is under its tab {@code tab} ({@link MachineMenu#MAIN} and the others): a machine
     * that draws a screen of its own ({@code MachineScreens}) leaves it off a tab that takes no items, where its slots
     * are neither drawn nor clicked. Every machine has it under every tab, unless it says otherwise.
     */
    public boolean inventoryOn(final int tab) {
        return true;
    }

    /**
     * How wide its screen is: every machine's is {@link MachineScreenLayout#WIDTH}, and a machine whose own screen is
     * wider says so, for its menu to centre the player's inventory and the standard tabs' slots under it.
     */
    public int screenWidth() {
        return MachineScreenLayout.WIDTH;
    }

    /**
     * How many tabs of its own its screen has after the five every machine has, numbered on from
     * {@link MachineScreenLayout#TABS}: none, unless a machine with a screen of its own says otherwise.
     */
    public int ownTabs() {
        return 0;
    }

    /** What its own screen shows that only the server knows ({@link MachineScreenData}); none unless it says. */
    public @Nullable MachineScreenData<?> screenData() {
        return null;
    }

    /** Its switch, its answer to redstone and what a comparator reads of it. */
    public MachineControl control() {
        return control;
    }

    /** How whole it is. */
    public MachineHealth health() {
        return health;
    }

    /** Its heat, or none for a machine that makes none. */
    public @Nullable MachineHeat heat() {
        return heat;
    }

    @Override
    public @Nullable HeatBody heatBody() {
        return heat == null ? null : heat.body();
    }

    /** Its shaft, or none for a machine that neither drives nor is driven. */
    public @Nullable MachineShaft shaft() {
        return shaft;
    }

    /** Its structure, for a multiblock's terminal; none for a machine of one block. */
    public @Nullable MachineStructure structure() {
        return structure;
    }

    /** How many recipes it works at once: its upgrades', and its structure's when it is a multiblock's terminal. */
    public int recipesAtOnce() {
        return withStructure(effect()).recipesAtOnce();
    }

    /** The items a port of its structure takes in or gives out: the slots of the port's group. */
    @Override
    public @Nullable IItemHandler itemPort(final PortKind kind, final BlockPos part, final @Nullable Direction side) {
        final int group = portGroup(part);
        if (group < 0) {
            return null;
        }
        return new FaceItemHandler(kind.input() ? itemsIn(group) : itemsOut(group),
                kind.input() ? FaceMode.IN : FaceMode.OUT, ItemFilter.EVERYTHING);
    }

    /** The liquid a port of its structure takes in or gives out: the tanks of the port's group. */
    @Override
    public @Nullable IFluidHandler fluidPort(final PortKind kind, final BlockPos part,
                                             final @Nullable Direction side) {
        final int group = portGroup(part);
        final IFluidHandler tanks = group < 0 ? null : kind.input() ? fluidsIn(group) : fluidsOut(group);
        return tanks == null ? null : new FaceFluidHandler(tanks, kind.input() ? FaceMode.IN : FaceMode.OUT,
                ItemFilter.EVERYTHING);
    }

    /** The energy, as FE, an energy port of its structure takes in or gives out. */
    @Override
    public @Nullable IEnergyStorage energyPort(final PortKind kind, final BlockPos part,
                                               final @Nullable Direction side) {
        if (portGroup(part) < 0) {
            return null;
        }
        final EnergyStore store = power.portStore();
        final IEnergyStorage fe = energy != null ? energy
                : store != null && store.energy().acceptsFe() ? store.asFe() : null;
        return fe == null ? null : new FaceEnergyStorage(fe, kind.input() ? FaceMode.IN : FaceMode.OUT);
    }

    /** Its port of J's Energy, with its voltage class, as an energy port of its structure meets a cable. */
    @Override
    public @Nullable IEnergyPort energyClassPort(final PortKind kind, final BlockPos part,
                                                 final @Nullable Direction side) {
        return portGroup(part) < 0 ? null : power.portStore();
    }

    /** What a port of its structure takes in or gives out of a state of matter. */
    @Override
    public @Nullable IMatterHandler matterPort(final PortKind kind, final BlockPos part,
                                               final @Nullable Direction side) {
        final IMatterHandler whole = portGroup(part) < 0 || kind.state().isEmpty() ? null
                : matterHandler(kind.state().get());
        return whole == null ? null : new FaceMatterHandler(whole, kind.input() ? FaceMode.IN : FaceMode.OUT,
                ItemFilter.EVERYTHING);
    }

    /** Its casing's heat, through a heat port of its structure, for a machine that makes heat. */
    @Override
    public @Nullable IHeatPort heatPort(final BlockPos part, final @Nullable Direction side) {
        return portGroup(part) < 0 ? null : HeatPorts.port(this, null);
    }

    /** A redstone port of its structure that reads heard {@code signal}; it answers the strongest it hears. */
    @Override
    public void heard(final BlockPos port, final int signal) {
        final Integer before = signal > 0 ? portSignals.put(port.immutable(), signal) : portSignals.remove(port);
        if ((before == null ? 0 : before) != signal && level instanceof ServerLevel server) {
            readSignal(server);
            wake();
        }
    }

    /** What its structure's redstone ports that give put out: what a comparator reads of it. */
    @Override
    public int givenSignal() {
        return strength();
    }

    @Override
    public @Nullable IMechanicalPart mechanicalPart() {
        return shaft;
    }

    /** What each of its faces does with each of its resources, and what it pushes and pulls by itself. */
    public MachineSides sides() {
        return sides;
    }

    /**
     * What it offers on {@code side} of {@code whole}: as its Sides tab sets the face for the resource, the whole, a
     * part that only takes in or only gives out, or nothing; from inside the block, the whole.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T throughFace(final BlockCapability<T, ?> capability, final @Nullable Direction side,
                                       final @Nullable T whole) {
        if (whole == null || side == null) {
            return whole;
        }
        final MachineResource resource = capability == Capabilities.ItemHandler.BLOCK ? MachineResource.ITEMS
                : capability == Capabilities.FluidHandler.BLOCK ? MachineResource.FLUIDS
                : capability == Capabilities.EnergyStorage.BLOCK ? MachineResource.ENERGY
                : capability == HeatPorts.HEAT.block() ? MachineResource.HEAT : null;
        if (resource == null) {
            return whole;
        }
        final FaceMode mode = sides.mode(resource, getBlockState(), side);
        return switch (mode) {
            case NONE -> null;
            case BOTH -> whole;
            // Heat flows by itself from hot to cold: a face open to it at all lets it through both ways.
            case IN, OUT -> switch (resource) {
                case ITEMS -> (T) new FaceItemHandler((IItemHandler) whole, mode, ItemFilter.EVERYTHING);
                case FLUIDS -> (T) new FaceFluidHandler((IFluidHandler) whole, mode, ItemFilter.EVERYTHING);
                case HEAT -> whole;
                default -> (T) new FaceEnergyStorage((IEnergyStorage) whole, mode);
            };
        };
    }

    /** What it is doing, as its screen and whoever looks at it see. */
    public MachineStatus status() {
        return Objects.requireNonNull(status.get());
    }

    /** Whether it rests, asleep, until something it waits on changes. */
    public boolean asleep() {
        return asleep;
    }

    /** Wakes it: something it waits on changed. */
    public void wake() {
        asleep = false;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        // Anything saved that changes (its inventory, its energy, its settings, its health) is something to look at.
        asleep = false;
    }

    @Override
    public @Nullable Ownership ownership() {
        return ownership.get();
    }

    /** Hands it to {@code to}, or to nobody for none: whoever placed it, by default. */
    public void own(@Nullable final Ownership to) {
        ownership.set(to);
    }

    /** Lets {@code to} use it besides its owner: nobody else, the owner's team, or everyone. Nobody's stays so. */
    public void setAccess(final Access to) {
        final Ownership owned = ownership.get();
        if (owned != null) {
            ownership.set(owned.withAccess(to));
        }
    }

    /** What it drew of its energy last tick, as its screen shows it while it is open. */
    public long drawn() {
        return drawn.get();
    }

    /** How many things it made the last minute, as its screen shows it while it is open. */
    public int perMinute() {
        return perMinute.get();
    }

    /**
     * What it did over its last ten minutes, as of the last second, on the server: its screen reads it through
     * {@link MachineMenu#history()}. It starts again when the machine is loaded.
     */
    public MachineHistory history() {
        return history;
    }

    /**
     * Its slots its Main tab shows, and where: none unless it says. A machine with a face of its own puts each where
     * its face draws it.
     */
    public List<FaceSlot> mainSlots() {
        return List.of();
    }

    /** Gives back {@code points} of its health, mending a burnt port once it is whole; how much it gave back. */
    public int repair(final int points) {
        final int given = health.repair(points);
        if (!health.damaged()) {
            power.mend();
        }
        if (given > 0 && level instanceof ServerLevel server) {
            showCracks(server);
        }
        return given;
    }

    /** Takes {@code points} of its health, from an explosion, sabotage or heat; how much it took. */
    public int damage(final int points) {
        final int taken = health.damage(points);
        if (taken > 0 && level instanceof ServerLevel server) {
            showCracks(server);
        }
        return taken;
    }

    @Override
    public boolean holdsPlace(final BlockPos pos) {
        return structure != null && structure.holdsPlace(pos);
    }

    @Override
    public Optional<Orientation> formedOrientation() {
        return structure == null ? Optional.empty() : structure.orientation();
    }

    @Override
    public boolean merged() {
        return structure != null && structure.merged();
    }

    @Override
    public List<MachineParameter> parameters() {
        if (parameters == null) {
            final List<MachineParameter> declared = new ArrayList<>();
            declared.add(MachineParameter.set("on", PARAMETER_ON, null, 0, 1, () -> control.on() ? 1 : 0,
                    value -> control.setOn(value >= 0.5)));
            declared.add(MachineParameter.read("progress", PARAMETER_PROGRESS, null, 0, 1, this::progressShare));
            declared.add(MachineParameter.read("input", PARAMETER_INPUT, null, 0, 1, this::inputShare));
            declared.add(MachineParameter.read("output", PARAMETER_OUTPUT, null, 0, 1, this::outputShare));
            declared.add(MachineParameter.read("energy", PARAMETER_ENERGY, null, 0,
                    Math.max(1, power.capacity()), power::stored));
            declared.add(MachineParameter.read("health", PARAMETER_HEALTH, null, 0, MachineHealth.FULL,
                    health::health));
            final MachineHeat holds = heat;
            if (holds != null) {
                // Read from freezing up to the hottest it stands, so a full signal says it is about to stop.
                declared.add(MachineParameter.read("temperature", HeatReadings.TEMPERATURE, HeatReadings.KELVIN,
                        HeatMath.ZERO_CELSIUS, Math.max(HeatMath.ZERO_CELSIUS + 1.0, holds.limit()),
                        () -> holds.body().known() ? holds.body().temperature() : HeatMath.ZERO_CELSIUS));
            }
            declareParameters(declared);
            parameters = List.copyOf(declared);
        }
        return parameters;
    }

    /**
     * What it reads: progress, how full its inputs and its outputs are, and its energy, in the order of
     * {@link ComparatorReads}, then every value it declares.
     */
    @Override
    public List<Reading> readings() {
        final List<Reading> readings = new ArrayList<>();
        for (final MachineParameter parameter : parameters()) {
            if (!"on".equals(parameter.id()) && !"health".equals(parameter.id())) {
                readings.add(parameter.reading());
            }
        }
        return readings;
    }

    /** Its comparator reads what the player picked. */
    @Override
    public int chosen() {
        return ComparatorReads.round().indexOf(control.reads());
    }

    /* The reading the player picked, read alone: a comparator asks often. */
    @Override
    public int strength() {
        return switch (control.reads()) {
            case PROGRESS -> Reading.strength(progressShare(), 0, 1);
            case INPUT -> Reading.strength(inputShare(), 0, 1);
            case OUTPUT -> Reading.strength(outputShare(), 0, 1);
            case ENERGY -> Reading.strength(power.stored(), 0, Math.max(1, power.capacity()));
        };
    }

    /** Whether it is working now, damaged or not: what its working sound and smoke follow on the player's side. */
    public boolean running() {
        return running.get();
    }

    /** What it sounds like, as its mod declares it: what the studio lists among what it is made of. */
    public final MachineSounds declaredSounds() {
        return sounds();
    }

    /** Its state, once the kit has ticked it, and its health while it is damaged. */
    @Override
    public void describe(final List<Text> lines, final boolean details) {
        if (ticked) {
            lines.add(STATUS_LINE.with(status().text()));
        }
        if (structure != null) {
            structure.describe(lines, details, recipesAtOnce());
        }
        if (health.damaged()) {
            lines.add(HEALTH_LINE.with(health.health()));
        }
    }

    @Override
    public double audioX() {
        return worldPosition.getX() + 0.5;
    }

    @Override
    public double audioY() {
        return worldPosition.getY() + 0.5;
    }

    @Override
    public double audioZ() {
        return worldPosition.getZ() + 0.5;
    }

    /** Its working sound while it works, as loud as its upgrades make it. */
    @Override
    public List<LoopRequest> loops() {
        final SoundKey loop = sounds().work();
        return loop != null && running.get() && loudness.get() > 0
                ? List.of(new LoopRequest(loop, loudness.get() / (float) PERCENT, 1.0F, null)) : List.of();
    }

    /** The most energy it holds, as the screen shows it: its upgrades' buffer counted, on the player's side too. */
    public long capacityShown() {
        return level != null && level.isClientSide() ? capacityShown.get() : power.capacity();
    }

    /** What its upgrades do to it; nothing for a machine with no upgrade slots. */
    public UpgradeEffect upgrades() {
        return effect();
    }

    /**
     * Its setup, for another machine to take ({@link #pasteSetup}): what each face does with each resource, what it
     * pushes and pulls by itself and through which filter, how it answers redstone and what a comparator reads, and
     * whatever its kind adds ({@link #writeSetup}).
     */
    public CompoundTag copySetup(final HolderLookup.Provider registries) {
        final CompoundTag setup = new CompoundTag();
        sides.copyOut(setup, registries);
        setup.putString(REDSTONE_MODE, control.mode().serializedName());
        setup.putString(COMPARATOR_READS, control.reads().serializedName());
        writeSetup(setup, registries);
        return setup;
    }

    /**
     * Takes what of {@code setup}, another machine's, fits it: the faces and the pushing and pulling of each resource
     * both have, the redstone mode and what the comparator reads; and for a machine of the same kind
     * ({@code sameKind}) what its kind adds. Whether anything was taken.
     */
    public boolean pasteSetup(final CompoundTag setup, final boolean sameKind, final HolderLookup.Provider registries) {
        boolean taken = false;
        if (sides.speaksOf(setup)) {
            sides.paste(setup, registries);
            taken = true;
        }
        final RedstoneMode mode = MODES.find(setup.getString(REDSTONE_MODE));
        if (mode != null) {
            control.setMode(mode);
            taken = true;
        }
        final ComparatorReads reads = READINGS.find(setup.getString(COMPARATOR_READS));
        if (reads != null) {
            control.setReads(reads);
            taken = true;
        }
        if (sameKind) {
            readSetup(setup, registries);
        }
        wake();
        return taken;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide()) {
            SoundDirector.track(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) {
            SoundDirector.untrack(this);
        }
        if (structure != null && level instanceof ServerLevel server) {
            structure.removed(server, worldPosition);
        }
    }

    /** Moved with a moving structure: its fields' places follow, and so does its multiblock, which stays formed. */
    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        if (structure != null) {
            structure.moved(move);
        }
    }

    /**
     * On the player's side, now and then while it works: smoke from its top, as a working machine shows. A machine
     * that shows something else says so.
     */
    public void animate(final Level level, final BlockPos pos, final RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.02, 0.0);
        }
    }

    /**
     * Its work this tick, with redstone, its switch, its fuse and its health letting it: what it does, and so its
     * state. A machine that does nothing on its own (a generator ticks itself) waits.
     */
    protected MachineStatus work(final ServerLevel level) {
        return MachineStatus.WAITING;
    }

    /**
     * What it does once its state for the tick is known, whether it worked or something stopped it, before it may
     * rest: a machine that holds something only while it works (chunks kept loaded) lets go of it here.
     */
    protected void settled(final ServerLevel level, final MachineStatus status) {
    }

    /** Adds the values a machine of a mod lets the outside read and set, after every machine's own. */
    protected void declareParameters(final List<MachineParameter> declared) {
    }

    /** How far through its work it is, from none to all. */
    protected double progressShare() {
        return 0.0;
    }

    /** How full its inputs are, from empty to full. */
    protected double inputShare() {
        return 0.0;
    }

    /** How full its outputs are, from empty to full. */
    protected double outputShare() {
        return 0.0;
    }

    /** What it sounds like; silent unless its mod says. */
    protected MachineSounds sounds() {
        return MachineSounds.SILENT;
    }

    /** What its upgrades do to it; nothing unless it has upgrade slots ({@link ProcessingMachineBlockEntity}). */
    protected UpgradeEffect effect() {
        return UpgradeEffect.NONE;
    }

    /** Adds to its setup what only a machine of its kind can take: its input locks, the recipe picked. */
    protected void writeSetup(final CompoundTag setup, final HolderLookup.Provider registries) {
    }

    /** Takes from a setup of a machine of its own kind what {@link #writeSetup} added. */
    protected void readSetup(final CompoundTag setup, final HolderLookup.Provider registries) {
    }

    /** Counts {@code amount} of its energy drawn this tick, for its screen. */
    protected void drew(final long amount) {
        drawnThisTick += Math.max(0L, amount);
    }

    /** Counts {@code count} things it made in tick {@code gameTime}, for its screen's rate a minute. */
    protected void madeThings(final int count, final long gameTime) {
        made.addLast(new long[] {gameTime, count});
    }

    /** What goes in and out of it, which its Sides tab sets face by face: items and energy, and whatever it adds. */
    protected Set<MachineResource> resources() {
        return Set.of(MachineResource.ITEMS, MachineResource.ENERGY);
    }

    /**
     * Declares that it makes heat, as {@code declared} says: called once from its constructor. Its casing is then
     * saved and sent to the players who see it, offered to its neighbours through the faces its Sides tab opens to
     * heat, warmed by its work and cooled by the place around it; past its limit it stops to cool.
     */
    protected final void holdsHeat(final MachineHeat declared) {
        if (heat != null) {
            throw new IllegalStateException("a machine declares its heat once");
        }
        heat = Objects.requireNonNull(declared, "declared");
        HeatPorts.keep(this, declared.body());
    }

    /**
     * Declares that it fails safe, as a source a player builds to forget does: called from its constructor. A fault of
     * its own then only stops it, and it starts again once the fault is gone: a line however high keeps it from
     * starting rather than burn its port, and heat past its limit stops it to cool without taking its health. What
     * strikes it from outside, an explosion or sabotage, still damages it.
     */
    protected final void failsSafe() {
        failsSafe = true;
        final EnergyStore store = power.portStore();
        if (store != null) {
            store.failsSafe();
        }
    }

    /**
     * Declares it the terminal of a multiblock, as {@code declared} says: called once from its constructor. It
     * stands {@link MachineStatus#NOT_FORMED} until the world holds its structure whole, checked again whenever a
     * block it watches changes; whether it is formed is saved, and shown to whoever looks at it.
     */
    protected final void formsWith(final MachineStructure declared) {
        if (structure != null) {
            throw new IllegalStateException("a machine declares its structure once");
        }
        structure = Objects.requireNonNull(declared, "declared");
        final PartField field = fields().part("Structure", declared).save().toClient();
        declared.on(this, field::changed);
    }

    /**
     * Declares its shaft, a motor's or a dynamo's, as {@code declared} says: called once from its constructor. It joins
     * the network of mechanical power at its face, saved and sent to the players who see it; the machine's
     * {@link #work} runs it with {@link MachineShaft#run}, and a stopped machine neither drives nor takes.
     */
    protected final void turnsWith(final MachineShaft declared) {
        if (shaft != null) {
            throw new IllegalStateException("a machine declares its shaft once");
        }
        shaft = Objects.requireNonNull(declared, "declared");
        final PartField field = fields().part("Shaft", declared).save().toClient();
        declared.on(this, field::changed);
        MechanicalPorts.keep(this);
    }

    /**
     * What it works with, its structure's recipes at once counted: {@code effect} with as many more at once as the
     * structure runs, for a multiblock's terminal; {@code effect} itself otherwise.
     */
    protected final UpgradeEffect withStructure(final UpgradeEffect effect) {
        if (structure == null || structure.parallel() <= 1) {
            return effect;
        }
        return effect.withParallel(effect.parallel() + structure.parallel() - 1);
    }

    /**
     * The slots the input ports of group {@code group} of its structure put items into: its own inputs for every
     * group, unless it keeps the groups apart ({@link ProcessingMachineBlockEntity} gives each its own lane).
     */
    protected IItemHandler itemsIn(final int group) {
        return itemsIn();
    }

    /** The slots the output ports of group {@code group} of its structure take items from. */
    protected IItemHandler itemsOut(final int group) {
        return itemsOut();
    }

    /** The tanks the input ports of group {@code group} of its structure fill; its own, shared by every group. */
    protected @Nullable IFluidHandler fluidsIn(final int group) {
        return fluidsIn();
    }

    /** The tanks the output ports of group {@code group} of its structure drain. */
    protected @Nullable IFluidHandler fluidsOut(final int group) {
        return fluidsOut();
    }

    /** Its tanks of {@code state} as a whole, for the ports of its structure; none unless it says. */
    protected @Nullable IMatterHandler matterHandler(final MatterState state) {
        return null;
    }

    /** The slots it pulls items into by itself; none unless it says. */
    protected IItemHandler itemsIn() {
        return EmptyItemHandler.INSTANCE;
    }

    /** The slots it pushes items out of by itself; none unless it says. */
    protected IItemHandler itemsOut() {
        return EmptyItemHandler.INSTANCE;
    }

    /** The tanks it pulls liquids into by itself; none unless it says. */
    protected @Nullable IFluidHandler fluidsIn() {
        return null;
    }

    /** The tanks it pushes liquids out of by itself; none unless it says. */
    protected @Nullable IFluidHandler fluidsOut() {
        return null;
    }

    /** Its input tanks of {@code state}; none unless it says. */
    protected List<MatterTank> matterIn(final MatterState state) {
        return List.of();
    }

    /** Its output tanks of {@code state}; none unless it says. */
    protected List<MatterTank> matterOut(final MatterState state) {
        return List.of();
    }

    private void tickServer(final ServerLevel server) {
        ticked = true;
        final long now = server.getGameTime();
        if (structure != null) {
            structure.tick(server, worldPosition);
        }
        final UpgradeEffect effect = effect();
        if (!effect.equals(applied)) {
            applied = effect;
            power.enlarge(effect);
            loudness.set((int) Math.min(Integer.MAX_VALUE, Math.round(effect.sound() * PERCENT)));
        }
        capacityShown.set(power.capacity());
        power.tick();
        if (heat != null) {
            final int harm = heat.tick(running.get(), effect.heat());
            if (harm > 0 && !failsSafe) {
                damage(harm);
            }
        }
        if (health.damaged() && Math.floorMod(now + worldPosition.asLong(), MachineHealth.CRACKS_EVERY) == 0) {
            showCracks(server);
        }
        if (!signalRead) {
            readSignal(server);
        }
        if (sides.automatic() && Math.floorMod(now + worldPosition.asLong(), MachineTransfers.EVERY) == 0) {
            MachineTransfers.run(server, this);
        }
        // Last tick's work and draw: counted before this tick changes them, whether or not it rests.
        statistics.tick(running.get(), drawn.get());
        if (now % TICKS_A_SECOND == 0) {
            countMinute(now);
            statistics.second(perMinute.get());
            history = MachineHistory.of(statistics, now);
        }
        if (asleep && now < wakeAt) {
            drawn.set(0L);
            return;
        }
        asleep = false;
        drawnThisTick = 0L;
        final MachineStatus stopped = stopped(now);
        if (stopped != null && shaft != null) {
            shaft.halt();
        }
        final MachineStatus result = stopped != null ? stopped : work(server);
        drawn.set(drawnThisTick);
        show(server, result);
        settled(server, result);
        if (result.restful()) {
            asleep = true;
            wakeAt = now + SLEEP_AT_MOST;
        }
        final int strength = strength();
        if (strength != lastStrength) {
            lastStrength = strength;
            server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            if (structure != null) {
                for (final BlockPos port : structure.redstonePorts()) {
                    server.updateNeighborsAt(port, server.getBlockState(port).getBlock());
                }
            }
        }
    }

    /*
     * What keeps it from working now, in the order a player is told: its structure, its switch, redstone, its fuse,
     * its heat, its health, its line's voltage.
     */
    private @Nullable MachineStatus stopped(final long now) {
        if (structure != null && !structure.formed()) {
            return MachineStatus.NOT_FORMED;
        }
        if (!control.on()) {
            return MachineStatus.SWITCHED_OFF;
        }
        if (!control.redstoneAllows()) {
            return MachineStatus.OFF_BY_REDSTONE;
        }
        if (power.fuseBlown()) {
            return MachineStatus.FUSE_BLOWN;
        }
        if (heat != null && heat.cutOut()) {
            return MachineStatus.OVERHEATED;
        }
        if (power.burnt() || health.wrecked()) {
            return MachineStatus.DAMAGED;
        }
        if (power.overvoltage(now)) {
            return MachineStatus.OVERVOLTAGE;
        }
        return power.undervoltage(now) ? MachineStatus.UNDERVOLTAGE : null;
    }

    /* Shows {@code result}, damaged while it works damaged, with its start and stop as it begins or ends working. */
    private void show(final ServerLevel server, final MachineStatus result) {
        final boolean nowRunning = result == MachineStatus.WORKING;
        if (nowRunning && !running.get()) {
            play(server, sounds().start());
            server.sendParticles(ParticleTypes.POOF, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 4, 0.2, 0.05, 0.2, 0.01);
        } else if (!nowRunning && running.get()) {
            if (result != MachineStatus.SWITCHED_OFF && result != MachineStatus.OFF_BY_REDSTONE) {
                statistics.stopped(result.serializedName(), server.getGameTime());
            }
            play(server, sounds().stop());
            server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 3, 0.2, 0.05, 0.2, 0.01);
        }
        running.set(nowRunning);
        status.set(nowRunning && health.damaged() ? MachineStatus.DAMAGED : result);
    }

    /* Forgets what was made more than a minute ago, and counts the rest. */
    private void countMinute(final long now) {
        while (!made.isEmpty() && now - made.peekFirst()[0] > TICKS_A_MINUTE) {
            made.removeFirst();
        }
        long count = 0L;
        for (final long[] each : made) {
            count += each[1];
        }
        perMinute.set((int) Math.min(Integer.MAX_VALUE, count));
    }

    /*
     * Shows its cracks, as many as its damage, on its own block and on every block of its formed structure; merged
     * into its model, the model shows them whole.
     */
    private void showCracks(final ServerLevel server) {
        health.showCracks(server, worldPosition);
        if (structure != null) {
            for (final BlockPos place : structure.crackedPlaces()) {
                health.showCracks(server, place);
            }
        }
    }

    /* Plays {@code sound} once where it stands, as loud as its upgrades leave it; nothing for none. */
    void play(final ServerLevel server, @Nullable final SoundKey sound) {
        if (sound != null && loudness.get() > 0) {
            Audio.at(server, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    sound, loudness.get() / (float) PERCENT, 1.0F);
        }
    }

    /* Reads the redstone coming in from around it, and from its structure's redstone ports that read. */
    private void readSignal(final ServerLevel server) {
        signalRead = true;
        int strongest = server.getBestNeighborSignal(worldPosition);
        for (final int heard : portSignals.values()) {
            strongest = Math.max(strongest, heard);
        }
        control.hear(strongest);
    }

    /* Its structure came apart: what its redstone ports heard no longer reaches it. */
    void forgetPortSignals(final ServerLevel server) {
        if (!portSignals.isEmpty()) {
            portSignals.clear();
            readSignal(server);
        }
    }

    /* The group of the port at {@code part} of its formed structure, or -1 when it has none there. */
    private int portGroup(final BlockPos part) {
        return structure == null ? -1 : structure.groupOf(part);
    }

    /* What its Sides tab sets: what it says goes in and out of it, and heat when it makes heat. */
    private Set<MachineResource> sidesFor() {
        final Set<MachineResource> declared = resources();
        if (heat == null || declared.contains(MachineResource.HEAT)) {
            return declared;
        }
        final Set<MachineResource> withHeat = EnumSet.noneOf(MachineResource.class);
        withHeat.addAll(declared);
        withHeat.add(MachineResource.HEAT);
        return withHeat;
    }
}
