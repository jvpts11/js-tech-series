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
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.inventory.FaceEnergyStorage;
import dev.jstech.core.inventory.FaceFluidHandler;
import dev.jstech.core.inventory.FaceItemHandler;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.look.LookTexts;
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
import java.util.List;
import java.util.Objects;
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
 * and its sounds and particles as it starts, works and stops.
 *
 * <p>A machine works only when it has something to do: each tick it is ticked through {@link #serverTick}, which
 * runs {@link #work} unless the machine rests, asleep, waiting on its inventory, its energy, its settings, its
 * neighbours or its health to change, as anything that changes them wakes it.
 */
@TextHolder
public abstract class MachineBlockEntity extends SyncedBlockEntity implements IOwned, IMachineParameters,
        ISignalOutput, IDescribed, IAudible, IFaceGate {

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
    private @Nullable List<MachineParameter> parameters;
    private boolean asleep;
    private long wakeAt;
    private boolean signalRead;
    private int lastStrength = -1;
    /* Whether the kit ticks it: a machine that ticks itself keeps no state of the kit's to tell. */
    private boolean ticked;

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
        this.sides = new MachineSides(this::resources, () -> {
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
            machine.tickServer(server);
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

    /** Its switch, its answer to redstone and what a comparator reads of it. */
    public MachineControl control() {
        return control;
    }

    /** How whole it is. */
    public MachineHealth health() {
        return health;
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
                : capability == Capabilities.EnergyStorage.BLOCK ? MachineResource.ENERGY : null;
        if (resource == null) {
            return whole;
        }
        final FaceMode mode = sides.mode(resource, getBlockState(), side);
        return switch (mode) {
            case NONE -> null;
            case BOTH -> whole;
            case IN, OUT -> switch (resource) {
                case ITEMS -> (T) new FaceItemHandler((IItemHandler) whole, mode, ItemFilter.EVERYTHING);
                case FLUIDS -> (T) new FaceFluidHandler((IFluidHandler) whole, mode, ItemFilter.EVERYTHING);
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
            health.showCracks(server, worldPosition);
        }
        return given;
    }

    /** Takes {@code points} of its health, from an explosion, sabotage or heat; how much it took. */
    public int damage(final int points) {
        final int taken = health.damage(points);
        if (taken > 0 && level instanceof ServerLevel server) {
            health.showCracks(server, worldPosition);
        }
        return taken;
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

    /** Its state, once the kit has ticked it, and its health while it is damaged. */
    @Override
    public void describe(final List<Text> lines, final boolean details) {
        if (ticked) {
            lines.add(STATUS_LINE.with(status().text()));
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
        final UpgradeEffect effect = effect();
        if (!effect.equals(applied)) {
            applied = effect;
            power.enlarge(effect);
            loudness.set((int) Math.min(Integer.MAX_VALUE, Math.round(effect.sound() * PERCENT)));
        }
        capacityShown.set(power.capacity());
        power.tick();
        if (health.damaged() && Math.floorMod(now + worldPosition.asLong(), MachineHealth.CRACKS_EVERY) == 0) {
            health.showCracks(server, worldPosition);
        }
        if (!signalRead) {
            readSignal(server);
        }
        if (sides.automatic() && Math.floorMod(now + worldPosition.asLong(), MachineTransfers.EVERY) == 0) {
            MachineTransfers.run(server, this);
        }
        if (now % TICKS_A_SECOND == 0) {
            countMinute(now);
        }
        if (asleep && now < wakeAt) {
            drawn.set(0L);
            return;
        }
        asleep = false;
        drawnThisTick = 0L;
        final MachineStatus stopped = stopped(now);
        final MachineStatus result = stopped != null ? stopped : work(server);
        drawn.set(drawnThisTick);
        show(server, result);
        if (result.restful()) {
            asleep = true;
            wakeAt = now + SLEEP_AT_MOST;
        }
        final int strength = strength();
        if (strength != lastStrength) {
            lastStrength = strength;
            server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /* What keeps it from working now, in the order a player is told: its switch, redstone, its fuse, its health. */
    private @Nullable MachineStatus stopped(final long now) {
        if (!control.on()) {
            return MachineStatus.SWITCHED_OFF;
        }
        if (!control.redstoneAllows()) {
            return MachineStatus.OFF_BY_REDSTONE;
        }
        if (power.fuseBlown()) {
            return MachineStatus.FUSE_BLOWN;
        }
        if (power.burnt() || health.wrecked()) {
            return MachineStatus.DAMAGED;
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

    private void play(final ServerLevel server, @Nullable final SoundKey sound) {
        if (sound != null && loudness.get() > 0) {
            Audio.at(server, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    sound, loudness.get() / (float) PERCENT, 1.0F);
        }
    }

    /* Reads the redstone coming in from around it. */
    private void readSignal(final ServerLevel server) {
        signalRead = true;
        control.hear(server.getBestNeighborSignal(worldPosition));
    }
}
