/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import com.mojang.serialization.Codec;
import dev.jstech.core.audio.LoopRequest;
import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureAssembly;
import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.moving.StructureDisassembly;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureTexts;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.multiblock.plan.PlanArms;
import dev.jstech.core.multiblock.plan.PlanAssembler;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.region.Box;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import dev.jstech.space.advancement.SpaceFirsts;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.crew.CrewRole;
import dev.jstech.space.crew.ShipCrew;
import dev.jstech.space.crew.ShipCrews;
import dev.jstech.space.hangar.ItemPortBlockEntity;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAboard;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipFixtureTexts;
import dev.jstech.space.ship.ShipFlight;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipStairs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Small Shipyard's terminal, in its control booth: the dock's multiblock, a machine of J's Core's kit that spends
 * nothing and makes nothing. Formed, the dock's blocks merge into its model. It keeps the dock's ships: the design on
 * its builder's screen, the plans saved by name, and the plan sent to the dock to be built.
 *
 * <p>The plan sent is laid out over the pit as the dock stood when it was sent, and built by hand from its hologram:
 * each part of the ship forms by itself into its model as its last block is put in place, and comes apart as one is
 * taken away ({@link PlannedBuild}). The terminal draws the parts formed. Sending another plan, or breaking the
 * terminal, gives the blocks of the parts formed back.
 *
 * <p>Its robot arms build the plan too, block by block ({@link PlanAssembler}, {@link PlanArms}), from what comes in
 * through the dock's item port: each takes a block from the feeder of its side, swings it to its place, sets it down
 * and welds it. It is no member of a data network: a machine, not a computer.
 */
public final class ShipyardTerminalBlockEntity extends MachineBlockEntity {

    private final MachineStructure structure;
    private final ValueField<ShipDesign> design;
    private final ValueField<List<ShipDesign>> plans;
    private final ValueField<ShipDesign> plan;
    private final ValueField<BlockPos> planAt;
    private final ValueField<Integer> planTurn;
    private final ValueField<List<Integer>> formedParts;
    private final ValueField<List<Integer>> missingParts;
    private final ValueField<List<Integer>> holes;
    /* Its robot arms, sent to the players who see them; and what lays through them, with what it holds. */
    private final PlanArms arms;
    private final PartField armsField;
    private final PlanAssembler assembler;
    /* Its crane, its floor and its cradle as they open and close, sent to the players who see them. */
    private final ShipyardDoors doors;
    private final PartField doorsField;
    /* Whether its design is sent to it once it has closed, a design sent while it stood open. */
    private final ValueField<Boolean> sendOnClose;
    /* The plan being built, on the server, while its chunks are loaded; and whether it is to start. */
    private @Nullable PlannedBuild build;
    private boolean pending;
    /* The ticks before a plan waiting for its chunks looks again. */
    private int pendingWait;
    /* Its dock's Item Ports as they tick, forgotten as they go; on the server. */
    private final Set<ItemPortBlockEntity> ports = Collections.newSetFromMap(new WeakHashMap<>());

    /** The most plans a shipyard keeps. */
    public static final int MOST_PLANS = 16;
    /** The design a shipyard's builder starts from: no parts, no name. */
    public static final ShipDesign EMPTY = new ShipDesign("", List.of(), List.of());
    private static final double HALF = 0.5;
    /* How near two points are to be the same, in blocks. */
    private static final double CLOSE = 1.0e-6;
    /* How often a plan waiting for its chunks looks again: a second. */
    private static final int PENDING_RETRY = 20;

    /* The terminals loaded on a player's game, and on the server, forgotten as they go. */
    private static final Set<ShipyardTerminalBlockEntity> LOADED_ON_CLIENT = Collections.newSetFromMap(
            new WeakHashMap<>());
    private static final Set<ShipyardTerminalBlockEntity> LOADED_ON_SERVER = Collections.newSetFromMap(
            new WeakHashMap<>());

    public ShipyardTerminalBlockEntity(final BlockPos pos, final BlockState state) {
        super(ShipyardBlocks.TERMINAL_ENTITY.get(), pos, state, 0, MachineEnergy.fe(0, 0, 0));
        // Closed, as it builds and changes ships, or open, as it lets one go or waits for one; held as it stood while
        // its crane and its floor move.
        this.doors = new ShipyardDoors(this, this::doorsChanged);
        this.structure = MachineStructure.of(ShipyardStructure.PATTERN).or(ShipyardStructure.OPEN_PATTERN).merges()
                .holdWhile(this.doors::moving);
        formsWith(this.structure);
        this.design = fields().value("Design", ShipDesign.CODEC, EMPTY).save().toClient();
        this.plans = fields().value("Plans", ShipDesign.CODEC.listOf(0, MOST_PLANS), List.<ShipDesign>of()).save()
                .toClient();
        this.plan = fields().nullable("Plan", ShipDesign.CODEC).save().toClient();
        // Where the plan sent stands, its ship's nose, and how the dock stood when it was sent.
        this.planAt = fields().nullable("PlanAt", BlockPos.CODEC).save().toClient();
        this.planTurn = fields().value("PlanTurn", Codec.INT, 0).save().toClient();
        // What its players see of the plan being built: the parts formed, and how much of each is missing.
        this.formedParts = fields().value("FormedParts", Codec.INT.listOf(), List.<Integer>of()).toClient();
        this.missingParts = fields().value("MissingParts", Codec.INT.listOf(), List.<Integer>of()).toClient();
        // The places of its plan missing a block in a formed part: holes the part holds round, drawn as holes.
        this.holes = fields().value("Holes", Codec.INT.listOf(), List.<Integer>of()).toClient();
        // Told only once it is built, after the field it changes is declared.
        this.arms = new PlanArms(this::armsChanged);
        this.armsField = fields().part("Arms", this.arms).save().toClient();
        this.assembler = new PlanAssembler(pos, this::setChanged).arms(this.arms)
                .armSounds(ShipyardSounds.ARM, ShipyardSounds.WELD);
        fields().part(PlanAssembler.KEY, this.assembler).save();
        this.doorsField = fields().part("Doors", this.doors).save().toClient();
        this.sendOnClose = fields().value("SendOnClose", Codec.BOOL, false).save();
        // Loaded, it starts building again once the chunks its plan stands in are loaded too, unless it already is.
        fields().whenLoaded(level -> this.pending = this.build == null && this.plan.get() != null);
        fields().whenBroken((level, at) -> {
            stopBuild(true);
            this.assembler.spill(level, at);
            this.doors.broken(level, at);
        });
    }

    /**
     * One server tick: the machine's, then its plan started once all of it is loaded, its arms at work, and its crane
     * and its floor on their way as it opens or closes.
     */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final ShipyardTerminalBlockEntity terminal) {
        MachineBlockEntity.serverTick(level, pos, state, terminal);
        if (level instanceof ServerLevel server) {
            terminal.doors.tick(server);
        }
        // A plan waiting for its chunks is laid out again once a second, not every tick: laying a ship out costs.
        if (terminal.pending && --terminal.pendingWait <= 0) {
            terminal.pendingWait = PENDING_RETRY;
            terminal.startBuild();
        }
        // Its arms reach the ship only round its cradle: they work while the dock stands closed.
        if (terminal.build != null && terminal.doors() == ShipyardDoors.State.CLOSED
                && level instanceof ServerLevel server) {
            terminal.assembler.tick(server);
        }
    }

    /** Its plan's chunk unloads: it stops watching it, leaving its formed parts as they are for when it loads. */
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        stopBuild(false);
        LOADED_ON_CLIENT.remove(this);
        LOADED_ON_SERVER.remove(this);
    }

    /**
     * It is known among the terminals loaded: on a player's game, whose plans are shown as holograms; on the server,
     * whose docks a ship coming in is seated by.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null) {
            (this.level.isClientSide() ? LOADED_ON_CLIENT : LOADED_ON_SERVER).add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED_ON_CLIENT.remove(this);
        LOADED_ON_SERVER.remove(this);
    }

    /** The terminals loaded on the server in {@code level} within {@code reach} blocks of {@code at}, nearest first. */
    public static List<ShipyardTerminalBlockEntity> near(final ServerLevel level, final Vec3 at, final double reach) {
        return LOADED_ON_SERVER.stream().filter(terminal -> terminal.getLevel() == level && !terminal.isRemoved()
                && Vec3.atCenterOf(terminal.getBlockPos()).distanceTo(at) <= reach)
                .sorted(Comparator.comparingDouble(terminal -> Vec3.atCenterOf(terminal.getBlockPos())
                        .distanceTo(at))).toList();
    }

    /** The terminals loaded on this player's game. */
    public static List<ShipyardTerminalBlockEntity> loadedOnClient() {
        return List.copyOf(LOADED_ON_CLIENT);
    }

    /** Whether its dock stands whole. */
    public boolean formed() {
        return this.structure.formed();
    }

    /** Whether its dock stands whole and open: its crane at the back of the bay, its floor up, no cradle. */
    public boolean open() {
        return formed() && this.structure.shape() == ShipyardStructure.OPEN_PATTERN;
    }

    /** How its crane, its floor and its cradle stand or move. */
    public ShipyardDoors.State doors() {
        return this.doors.state();
    }

    /** How far its moving part has gone at the game's tick {@code now}, from 0 to 1, as the players' games draw it. */
    public double doorsProgress(final double now) {
        return this.doors.progress(now);
    }

    /** Its floor heard running while it rises under the ship or goes down from under it. */
    @Override
    public List<LoopRequest> loops() {
        final List<LoopRequest> loops = super.loops();
        if (this.level == null || !this.doors.floorMoving(this.level.getGameTime())) {
            return loops;
        }
        final List<LoopRequest> all = new ArrayList<>(loops);
        all.add(LoopRequest.of(ShipyardSounds.DECK));
        return all;
    }

    /* Its dock is heard from the middle of its bay's floor, where its floor moves; from itself while unformed. */
    @Override
    public double audioX() {
        return heardFrom().x;
    }

    @Override
    public double audioY() {
        return heardFrom().y;
    }

    @Override
    public double audioZ() {
        return heardFrom().z;
    }

    /** How its dock stands, once formed. */
    public Optional<Orientation> turn() {
        return this.structure.orientation().filter(Orientation.turns()::contains);
    }

    /**
     * Starts the ship built whole in its dock, as its pilot {@code by} does from its flight console: the dock opens
     * round it and lets it go once the ship stands on its gear on the open floor. Whether the dock began to open; told
     * why when it cannot: the ship not whole, more blocks than a moving structure may be, its pilot at their limit of
     * moving structures, the dock moving, something in its way. All its letting go needs is asked before it opens.
     */
    public boolean launch(final @Nullable UUID by, final Consumer<Text> told) {
        if (!(this.level instanceof ServerLevel server) || !whole(told) || !mayGo(server, by, told)) {
            return false;
        }
        if (open() && !this.doors.moving()) {
            // Open already round it, as a ship that could not go before: it goes now.
            return release(by, told).isPresent();
        }
        if (!this.doors.open(server, by, true, told)) {
            return false;
        }
        told.accept(ShipyardDoorTexts.OPENING.text());
        return true;
    }

    /** Whether the ship planned in its dock stands whole with its flight console, as it must to go. */
    public boolean whole() {
        return whole(why -> { });
    }

    /** The design on its builder's screen. */
    public ShipDesign design() {
        final ShipDesign kept = this.design.get();
        return kept == null ? EMPTY : kept;
    }

    /** Keeps {@code next} as the design on its builder's screen, when it keeps within a design's limits. */
    public void design(final ShipDesign next) {
        if (next.bounded() && !next.equals(design())) {
            this.design.set(next);
            setChanged();
        }
    }

    /** The plans it keeps by name. */
    public List<ShipDesign> plans() {
        final List<ShipDesign> kept = this.plans.get();
        return kept == null ? List.of() : kept;
    }

    /** The plan sent to its dock to be built, if one is. */
    public Optional<ShipDesign> plan() {
        return Optional.ofNullable(this.plan.get());
    }

    /** The name of the ship its dock builds, as its Item Ports show it; empty while it builds none. */
    public String shipName() {
        return this.build == null ? "" : plan().map(ShipDesign::name).orElse("").toUpperCase(Locale.ROOT);
    }

    /** {@code port}, one of its dock's Item Ports, is there: what it holds counts with what the arms hold. */
    public void portSeen(final ItemPortBlockEntity port) {
        this.ports.add(port);
    }

    /**
     * How many blocks its dock has toward the ship it builds: what its arms were given, laid, carried or held, and
     * what its Item Ports hold; none while it builds nothing.
     */
    public int heldForBuild() {
        if (this.build == null) {
            return 0;
        }
        int count = this.build.plan().spots().size();
        for (final int waited : this.assembler.needs().values()) {
            count -= waited;
        }
        for (final ItemPortBlockEntity port : this.ports) {
            if (!port.isRemoved() && port.dock().filter(dock -> dock == this).isPresent()) {
                count += port.held();
            }
        }
        return count;
    }

    /** Where the plan sent stands, the block of its ship's nose on the centre line at the hull's floor, if one was. */
    public Optional<BlockPos> planAt() {
        return Optional.ofNullable(this.planAt.get());
    }

    /** How the dock stood when the plan was sent, which the plan is turned as. */
    public Orientation planTurn() {
        final Integer turn = this.planTurn.get();
        return turn == null || turn < 0 || turn >= FormedShapes.ORIENTATIONS.size() ? Orientation.NORTH
                : FormedShapes.ORIENTATIONS.get(turn);
    }

    /** The plan being built, on the server, once it has started. */
    public Optional<PlannedBuild> build() {
        return Optional.ofNullable(this.build);
    }

    /** The parts of its plan formed, by their numbers, the spine's from the nose first, as its players are told. */
    public List<Integer> formedParts() {
        final List<Integer> formed = this.formedParts.get();
        return formed == null ? List.of() : formed;
    }

    /** How many blocks of each part of its plan are missing, as its players are told. */
    public List<Integer> missingParts() {
        final List<Integer> missing = this.missingParts.get();
        return missing == null ? List.of() : missing;
    }

    /** The places of its plan missing a block in a formed part, by their numbers, as its players are told. */
    public List<Integer> holes() {
        final List<Integer> kept = this.holes.get();
        return kept == null ? List.of() : kept;
    }

    /** Whether the ship of its plan is built whole, every part formed. */
    public boolean built() {
        final List<Integer> missing = missingParts();
        return this.plan.get() != null && !missing.isEmpty() && missing.stream().allMatch(count -> count == 0);
    }

    /**
     * Saves the design as one of its plans, over the plan of its name, while it has room for another; a design with
     * no name is not saved. Whether it was.
     */
    public boolean save() {
        final ShipDesign now = design();
        if (now.name().isBlank()) {
            return false;
        }
        final List<ShipDesign> kept = new ArrayList<>(plans());
        kept.removeIf(saved -> saved.name().equals(now.name()));
        if (kept.size() >= MOST_PLANS) {
            return false;
        }
        kept.add(now);
        this.plans.set(List.copyOf(kept));
        setChanged();
        return true;
    }

    /** Forgets its plan at {@code place} in its list; the ship its dock builds or has built is left as it is. */
    public void delete(final int place) {
        final List<ShipDesign> kept = new ArrayList<>(plans());
        if (place >= 0 && place < kept.size()) {
            kept.remove(place);
            this.plans.set(List.copyOf(kept));
            setChanged();
        }
    }

    /**
     * Sends the design to the dock to be built, when it is ready to fly and fits the dock; whether it was taken. The
     * plan built before comes apart, its formed parts' blocks given back, unless it is the same plan, which goes on.
     * Sent to the dock standing open, the dock closes first, for its arms to build the ship in its cradle; told why
     * when it cannot close yet, it closes once its way is clear.
     */
    public boolean confirm(final Consumer<Text> told) {
        if (this.doors.moving()) {
            told.accept(ShipyardDoorTexts.MOVING.text());
            return false;
        }
        if (open()) {
            if (!(this.level instanceof ServerLevel server) || !ready(server, design())) {
                return false;
            }
            this.sendOnClose.set(true);
            this.doors.close(server, told);
            return true;
        }
        return send(design());
    }

    /** Sends the design as {@link #confirm(Consumer)} does, telling no one why the dock waits. */
    public boolean confirm() {
        return confirm(why -> { });
    }

    /**
     * Lets the ship built whole in its dock go, as its pilot starts it from its flight console: it becomes one moving
     * structure, once, of the blocks at the places of its plan and no others, the dock's left where they stand, each
     * block and its block entity as they are; it stays a structure from then on, standing still or flying. Its flight
     * console takes its plan, to draw its parts aboard, and the dock forgets it. None while it is not whole, has no
     * console, or its blocks cannot go.
     */
    public Optional<MovingStructure> release(final @Nullable UUID by) {
        return release(by, why -> { });
    }

    /**
     * Lets the ship go as {@link #release(UUID)} does, telling {@code told} why when it cannot: not whole yet, or its
     * blocks unable to go, as past its pilot's limit of moving structures.
     */
    public Optional<MovingStructure> release(final @Nullable UUID by, final Consumer<Text> told) {
        final PlannedBuild built = this.build;
        final ShipDesign design = this.plan.get();
        final BlockPos at = this.planAt.get();
        if (!(this.level instanceof ServerLevel server) || built == null || design == null || at == null) {
            return Optional.empty();
        }
        if (this.doors.moving()) {
            told.accept(ShipyardDoorTexts.MOVING.text());
            return Optional.empty();
        }
        // Every part formed: a hole in one goes with it, mended wherever a block is put back.
        if (built.formedParts().size() < built.plan().parts()) {
            told.accept(ShipFixtureTexts.NOT_WHOLE.text());
            return Optional.empty();
        }
        final ResourceLocation console = ShipKitBlocks.FLIGHT_CONSOLE.getId();
        final Optional<BlockPos> seat = built.plan().spots().stream().filter(spot -> spot.block().equals(console))
                .map(built::where).findFirst();
        if (seat.isEmpty() || !(server.getBlockEntity(seat.get()) instanceof AirReserveEntity keeper)) {
            return Optional.empty();
        }
        final List<BlockPos> places = new ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final PlanSpot spot : built.plan().spots()) {
            final BlockPos place = built.where(spot);
            if (!server.getBlockState(place).isAir()) {
                places.add(place);
                minX = Math.min(minX, place.getX());
                minZ = Math.min(minZ, place.getZ());
                maxX = Math.max(maxX, place.getX());
                maxZ = Math.max(maxZ, place.getZ());
            }
        }
        final List<Integer> formed = formedParts();
        // The plan stops watching before its blocks go, so their going takes no part apart.
        stopBuild(false);
        final StructureResult made = StructureAssembly.blocks(server, places, new Vec3((minX + maxX + 1) / 2.0,
                at.getY(), (minZ + maxZ + 1) / 2.0), by);
        if (!made.ok() || made.structure() == null) {
            startBuild();
            if (made.refusal() != null) {
                told.accept(made.refusal());
            }
            return Optional.empty();
        }
        // Its blocks came into the structure at once where its plot stood loaded, its console's block entity with
        // them, a new one there: the console is told where it stands now, or, its blocks still to come, where it was.
        final BlockPos keeps = BlockPos.containing(MovingStructures.toYard(made.structure(),
                Vec3.atCenterOf(seat.get())));
        final AirReserveEntity helm = server.isLoaded(keeps) && server.getBlockEntity(keeps)
                instanceof AirReserveEntity aboard ? aboard : keeper;
        helm.carry(new ShipAboard(design, at.subtract(seat.get()), this.planTurn.get(), formed));
        // The dock it leaves is one its pilot may choose to come back down onto.
        helm.dock(GlobalPos.of(server.dimension(), getBlockPos()));
        // Its pilot who started it owns it, their team its crew; a ship back from a dock keeps its crew.
        if (by != null && helm.crew().isEmpty()) {
            helm.crew(ShipCrew.of(by, CoreTeams.nameOf(server.getServer(), by).orElse("")));
        }
        if (by != null) {
            SpaceFirsts.starship(server.getServer(), by);
        }
        // It flies from now on as its pilot sends it, its nose the way its plan laid it, by what its parts weigh and
        // push, at rest until then, its console showing its flight.
        final int[] nose = planTurn().apply(0, 0, -1);
        MovingStructures.move(made.structure(), ShipFlight.resting(nose[0], nose[2], ShipFlight.performance(design,
                ShipParts.all(server)), Optional.of(ShipFlight.fromPivot(made.structure(), keeps))));
        // Its floor pulls its crew however it is turned, out in space too, so they walk it to its helm and its doors.
        made.structure().deckGravity(true);
        this.plan.set(null);
        this.planAt.set(null);
        this.doors.shipLetGo();
        this.formedParts.set(List.of());
        this.missingParts.set(List.of());
        this.holes.set(List.of());
        setChanged();
        return Optional.of(made.structure());
    }

    /**
     * Takes the ship {@code ship} back onto the grid, a moving structure come into its bay: set down where its plan
     * goes in this dock, as the dock stands, each block and what it keeps as it was; the dock keeps its plan again,
     * to change by its arms or by hand, and to let go once more; a dock standing open closes round it. Whether it did:
     * not while the dock is not formed, moves or builds another ship, nor for a structure with no console keeping a
     * ship's plan, or that cannot be set down.
     */
    public boolean seat(final MovingStructure ship) {
        return seat(ship, why -> { });
    }

    /**
     * Takes the ship back as {@link #seat(MovingStructure)} does, telling {@code told} why the dock standing open
     * cannot close round it yet: it closes once its way is clear.
     */
    public boolean seat(final MovingStructure ship, final Consumer<Text> told) {
        final Optional<Orientation> dock = this.structure.orientation();
        if (!(this.level instanceof ServerLevel server) || !formed() || dock.isEmpty() || this.plan.get() != null
                || !Orientation.turns().contains(dock.get()) || this.doors.moving()) {
            return false;
        }
        final Optional<BlockPos> console = consoleIn(server, ship);
        if (console.isEmpty() || !(server.getBlockEntity(console.get()) instanceof AirReserveEntity keeper)
                || keeper.ship().isEmpty() || !Orientation.turns().contains(keeper.ship().get().orientation())) {
            return false;
        }
        final ShipAboard aboard = keeper.ship().get();
        final BlockPos at = ShipyardStructure.shipAt(this.worldPosition, dock.get());
        final Optional<StructurePose> pose = seated(ship, console.get().offset(aboard.nose()), at,
                Math.floorMod(Orientation.turns().indexOf(dock.get())
                        - Orientation.turns().indexOf(aboard.orientation()), Orientation.turns().size()));
        if (pose.isEmpty()) {
            return false;
        }
        final StructurePose was = ship.pose();
        ship.teleport(pose.get());
        final BlockPos landed = BlockPos.containing(ship.frame().worldX(console.get().getX() + HALF,
                console.get().getY() + HALF, console.get().getZ() + HALF), ship.frame().worldY(console.get().getX()
                + HALF, console.get().getY() + HALF, console.get().getZ() + HALF), ship.frame().worldZ(
                console.get().getX() + HALF, console.get().getY() + HALF, console.get().getZ() + HALF));
        // Its boarding stair is none of its plan: taken up first, it is not left on the grid, and let down again
        // where the ship cannot be set down.
        ShipStairs.takeUp(server, ship);
        if (!StructureDisassembly.setDown(ship).ok()) {
            ship.teleport(was);
            ShipStairs.groundChanged(server, ship, ship.motion() instanceof ShipFlight flight && flight.grounded());
            return false;
        }
        if (server.getBlockEntity(landed) instanceof AirReserveEntity set) {
            set.land();
        }
        this.design.set(aboard.design());
        this.plan.set(aboard.design());
        this.planAt.set(at);
        this.planTurn.set(FormedShapes.ORIENTATIONS.indexOf(dock.get()));
        // The ship taken back is the dock's plan now: a design sent while it stood open waits no more.
        this.sendOnClose.set(false);
        setChanged();
        startBuild();
        if (open()) {
            this.doors.close(server, told);
        }
        return true;
    }

    /**
     * Takes back the ship standing in its bay, as {@code by} asks from the dock's screen with Take ship in, the pair of
     * Release ship: a dock takes a ship back only when someone asks it to, never because a pilot uses the ship's
     * console. Only a pilot of that ship may ask. Whether it did; told why when it does not: no ship in the bay, the
     * asker no pilot of it, or the ship not to be set down where its plan goes.
     */
    public boolean takeIn(final ServerPlayer by, final Consumer<Text> told) {
        final Optional<Orientation> dock = turn();
        if (!(this.level instanceof ServerLevel server) || dock.isEmpty()) {
            return false;
        }
        for (final IStructureBody body : MovingStructures.near(server, ShipyardStructure.bay(this.worldPosition,
                dock.get()))) {
            if (!(body instanceof MovingStructure ship) || consoleIn(server, ship).isEmpty()) {
                continue;
            }
            if (!ShipCrews.roleOf(by, ship).holds(CrewRole.PILOT)) {
                told.accept(ShipyardDoorTexts.NOT_YOUR_SHIP.text());
                return false;
            }
            if (!seat(ship, told)) {
                told.accept(ShipyardDoorTexts.CANNOT_TAKE.text());
                return false;
            }
            told.accept(ShipyardDoorTexts.TAKEN_IN.text());
            return true;
        }
        told.accept(ShipyardDoorTexts.NO_SHIP.text());
        return false;
    }

    /** Whether {@code design} is ready to be built in a Small Shipyard: nothing keeps it down, and it fits the bay. */
    public static boolean ready(final Level level, final ShipDesign design) {
        return design.problems(ShipParts.all(level)).isEmpty() && ShipyardLayout.fits(ShipyardLayout.laid(
                ShipParts.all(level), ShipKits.all(level), design));
    }

    /** Its robot arms, where they stand and what each does. */
    public PlanArms arms() {
        return this.arms;
    }

    /** What lays its plan through its arms: what it holds, and what it waits for. */
    public PlanAssembler assembler() {
        return this.assembler;
    }

    /** The arms' inbox, to the dock's item port while the dock is formed; nothing else passes through. */
    @Override
    public @Nullable IItemHandler itemPort(final PortKind kind, final BlockPos part, final @Nullable Direction side) {
        return kind == PortKind.ITEM_INPUT && formed() ? this.assembler.inbox() : null;
    }

    /* It spends no energy. */
    @Override
    public boolean takesEnergy() {
        return false;
    }

    /* It holds no items. */
    @Override
    public boolean inventoryOn(final int tab) {
        return false;
    }

    /* It shows nothing as it works. */
    @Override
    public void animate(final Level level, final BlockPos pos, final RandomSource random) {
    }

    @Override
    protected MachineStatus work(final ServerLevel level) {
        return MachineStatus.WAITING;
    }

    @Override
    protected Set<MachineResource> resources() {
        return Set.of();
    }

    /* Its dock checked again: formed, its arms stand on their rails as the dock stands. */
    @Override
    protected void structureChecked(final ServerLevel level) {
        final Optional<Orientation> dock = this.structure.orientation();
        if (formed() && dock.isPresent() && this.build == null) {
            this.arms.layout(ShipyardStructure.arms(this.worldPosition, dock.get()), job -> { });
        }
    }

    /*
     * Sends {@code design} to the dock to be built, when it is ready to fly and fits the dock; whether it was sent.
     * The plan built before comes apart, its formed parts' blocks given back, unless it is the same plan, which goes
     * on.
     */
    private boolean send(final ShipDesign sent) {
        final Optional<Orientation> dock = this.structure.orientation();
        if (this.level == null || !formed() || dock.isEmpty() || !ready(this.level, sent)) {
            return false;
        }
        final BlockPos at = ShipyardStructure.shipAt(this.worldPosition, dock.get());
        final int turn = FormedShapes.ORIENTATIONS.indexOf(dock.get());
        if (sent.equals(this.plan.get()) && at.equals(this.planAt.get()) && turn == this.planTurn.get()) {
            return true;
        }
        stopBuild(true);
        this.plan.set(sent);
        this.planAt.set(at);
        this.planTurn.set(turn);
        setChanged();
        startBuild();
        return true;
    }

    /*
     * Whether the ship standing whole may go as its letting go makes it: no more blocks than a moving structure may
     * be, its pilot under their limit of moving structures; told why not.
     */
    private boolean mayGo(final ServerLevel server, final @Nullable UUID by, final Consumer<Text> told) {
        final PlannedBuild built = this.build;
        if (built == null) {
            return false;
        }
        final long blocks = built.plan().spots().stream().map(built::where).filter(place -> !server
                .getBlockState(place).isAir()).count();
        if (blocks > StructureBalance.mostBlocks()) {
            told.accept(StructureTexts.TOO_MANY.with(StructureBalance.mostBlocks(), blocks));
            return false;
        }
        if (by != null && !PlayerLimits.allows(by, PlayerLimits.MOVING_STRUCTURES, PlayerLimits.count(
                server.getServer(), by, PlayerLimits.MOVING_STRUCTURES))) {
            told.accept(PlayerLimits.reached(server.getServer(), by, PlayerLimits.MOVING_STRUCTURES));
            return false;
        }
        return true;
    }

    /* Whether the ship planned in its dock stands whole with its flight console, as it must to go; told why not. */
    private boolean whole(final Consumer<Text> told) {
        final PlannedBuild built = this.build;
        if (built == null || this.plan.get() == null || this.planAt.get() == null || this.level == null) {
            return false;
        }
        if (built.formedParts().size() < built.plan().parts()) {
            told.accept(ShipFixtureTexts.NOT_WHOLE.text());
            return false;
        }
        final ResourceLocation console = ShipKitBlocks.FLIGHT_CONSOLE.getId();
        return built.plan().spots().stream().anyMatch(spot -> spot.block().equals(console)
                && this.level.getBlockEntity(built.where(spot)) instanceof AirReserveEntity);
    }

    /* Its doors closed: a design sent while it stood open is sent now, for its arms to build. */
    void doorsClosed() {
        if (this.sendOnClose.get()) {
            this.sendOnClose.set(false);
            send(design());
        }
        setChanged();
    }

    /* Its doors moved on: the players who see it are told. */
    private void doorsChanged() {
        this.doorsField.changed();
        setChanged();
    }

    /* Where in {@code ship}'s blocks the flight console keeping a ship's plan stands, if one does. */
    private static Optional<BlockPos> consoleIn(final ServerLevel level, final MovingStructure ship) {
        final Box bounds = ship.bounds();
        for (final BlockPos at : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(),
                bounds.maxY(), bounds.maxZ())) {
            if (level.getBlockEntity(at) instanceof AirReserveEntity keeper && keeper.ship().isPresent()) {
                return Optional.of(at.immutable());
            }
        }
        return Optional.empty();
    }

    /*
     * The pose that sets {@code ship} down with the block of its nose, {@code nose} among its blocks, on {@code at},
     * turned {@code quarters} quarter turns clockwise from how it stands among its blocks; none when no pose does.
     */
    private static Optional<StructurePose> seated(final MovingStructure ship, final BlockPos nose, final BlockPos at,
                                                  final int quarters) {
        final int[] along = Orientation.turns().get(quarters).apply(0, 0, 1);
        final int[] across = Orientation.turns().get(quarters).apply(1, 0, 0);
        final double nx = nose.getX() + HALF;
        final double ny = nose.getY() + HALF;
        final double nz = nose.getZ() + HALF;
        for (final double angle : new double[] {quarters * Math.PI / 2, -quarters * Math.PI / 2}) {
            final StructureFrame turned = new StructureFrame(StructurePose.turned(0, 0, 0, 0, 1, 0, angle),
                    ship.pivotX(), ship.pivotY(), ship.pivotZ());
            final double wx = turned.worldX(nx, ny, nz);
            final double wy = turned.worldY(nx, ny, nz);
            final double wz = turned.worldZ(nx, ny, nz);
            if (same(turned, nx, ny, nz + 1, wx, wy, wz, along) && same(turned, nx + 1, ny, nz, wx, wy, wz, across)) {
                return Optional.of(StructurePose.turned(at.getX() + HALF - wx, at.getY() + HALF - wy,
                        at.getZ() + HALF - wz, 0, 1, 0, angle));
            }
        }
        return Optional.empty();
    }

    /*
     * Whether {@code frame} takes the point ({@code x}, {@code y}, {@code z}) {@code way} from where it takes the
     * nose, at ({@code wx}, {@code wy}, {@code wz}).
     */
    private static boolean same(final StructureFrame frame, final double x, final double y, final double z,
                                final double wx, final double wy, final double wz, final int[] way) {
        return Math.abs(frame.worldX(x, y, z) - wx - way[0]) < CLOSE && Math.abs(frame.worldY(x, y, z) - wy - way[1])
                < CLOSE && Math.abs(frame.worldZ(x, y, z) - wz - way[2]) < CLOSE;
    }

    /* Starts building the plan sent, where its chunks are all loaded; it waits for them otherwise. */
    private void startBuild() {
        final ShipDesign building = this.plan.get();
        final BlockPos at = this.planAt.get();
        if (!(this.level instanceof ServerLevel server) || building == null || at == null) {
            this.pending = false;
            return;
        }
        // A ship's parts hold when a block of theirs is broken, holed there, as they do aboard wherever it goes.
        final PlannedBuild next = new PlannedBuild(ShipyardPlans.of(server, building, planTurn()), at).holed();
        for (final PlanSpot spot : next.plan().spots()) {
            if (!Loaded.at(server, next.where(spot))) {
                this.pending = true;
                return;
            }
        }
        this.pending = false;
        stopBuild(false);
        this.build = next;
        next.start(server, this.worldPosition, this::told);
        // Its arms stand as the dock stood when the plan was sent, and lay it.
        this.arms.layout(ShipyardStructure.arms(this.worldPosition, planTurn()), job -> { });
        this.assembler.build(next);
    }

    /* Stops building, its formed parts' blocks given back when {@code giveBack}; its arms hold what they hold. */
    private void stopBuild(final boolean giveBack) {
        final PlannedBuild was = this.build;
        this.build = null;
        this.assembler.build(null);
        if (was != null) {
            was.stop(giveBack);
        }
        if (giveBack) {
            this.formedParts.set(List.of());
            this.missingParts.set(List.of());
            this.holes.set(List.of());
        }
    }

    /* Its arms' work changed: it is saved, and sent to its players. */
    private void armsChanged() {
        this.armsField.changed();
    }

    /* Its plan's counts changed: its arms look again, and its players hear the parts formed and what each lacks. */
    private void told() {
        final PlannedBuild now = this.build;
        this.assembler.wake();
        if (now == null) {
            return;
        }
        this.formedParts.set(now.formedParts());
        final List<Integer> missing = new ArrayList<>();
        for (int part = 0; part < now.plan().parts(); part++) {
            missing.add(now.missing(part));
        }
        this.missingParts.set(List.copyOf(missing));
        this.holes.set(ShipyardPlans.holesIn(now));
        setChanged();
    }

    /* The middle of its bay's floor, closed, once its dock is formed; else the middle of itself. */
    private Vec3 heardFrom() {
        return turn().map(dock -> ShipyardStructure.bay(this.worldPosition, dock))
                .map(bay -> new Vec3(bay.getCenter().x, bay.minY, bay.getCenter().z))
                .orElseGet(() -> Vec3.atCenterOf(this.worldPosition));
    }
}
