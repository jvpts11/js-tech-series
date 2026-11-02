/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.jstechseries.builder.Assembly;
import dev.jstech.core.jstechseries.builder.IAssembler;
import dev.jstech.core.jstechseries.network.INetworkMember;
import dev.jstech.core.jstechseries.network.NetworkCategory;
import dev.jstech.core.jstechseries.network.NetworkMembers;
import dev.jstech.core.jstechseries.operation.IItemNeeds;
import dev.jstech.core.jstechseries.operation.IOperationArgs;
import dev.jstech.core.jstechseries.operation.IOperationRequest;
import dev.jstech.core.jstechseries.operation.OperationFailure;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.jstechseries.operation.OperationType;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanAssembler;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.launcher.LauncherLayout;
import dev.jstech.space.rocket.RocketStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Rocket Hangar's terminal, a console in its control room's row: the hangar's multiblock, a machine of J's Core's
 * kit that spends nothing and makes nothing ({@link HangarStructure}). Its bay is the building's, wherever the building
 * stands and however it is turned. As the building forms its fittings take their places: the consoles light their
 * screens, the chairs, the status board, the step's railing and the doors turn the way the building has them, the
 * crane's pieces take their formed shapes, and the work platforms in the bay theirs; as it comes apart they go dark
 * and are blocks of the kit again. It keeps the hangar's rockets: the design on its screen, the plans saved, and the
 * plan sent to the bay to be built; and it opens and shuts the bay door for its console.
 *
 * <p>Its crane builds the plan sent by itself, block by block ({@link PlanAssembler}), from what comes in through the
 * loading dock: whatever the hangar waits for, it builds into the rocket. On a network, a request of
 * {@link Assembly#ASSEMBLE} sets it building, a plan it keeps by name or the plan it was sent, and while it waits for
 * blocks it says which, so the network brings them, made first where they can be.
 */
public final class HangarTerminalBlockEntity extends MachineBlockEntity implements INetworkMember, IItemNeeds,
        IAssembler {

    private final MachineStructure structure;
    private final ValueField<RocketDesign> design;
    private final ValueField<List<RocketDesign>> plans;
    private final ValueField<RocketDesign> plan;
    private final ValueField<BlockPos> planAt;
    private final ValueField<Direction> launcher;
    private final ValueField<List<Integer>> formedParts;
    private final ValueField<List<Integer>> missingParts;
    private final ValueField<List<BlockPos>> platforms;
    private final ValueField<Integer> number;
    private final ValueField<Integer> shapedAs;
    /* The crane, and what it holds of the blocks its rocket wants. */
    private final PlanAssembler crane;
    /* The plan being built, on the server, while its chunk is loaded; and whether it is to start. */
    private @Nullable PlannedBuild build;
    private boolean pending;
    /* The network's request to build, followed until the rocket is whole; on the server. */
    private @Nullable IOperationRequest<Assembly.Args> assembling;

    /** The most plans a hangar keeps. */
    public static final int MOST_PLANS = 16;
    /* Not shaped as any way the building stands. */
    private static final int UNSHAPED = -1;

    /* The terminals loaded on a player's game, and on the server, forgotten as they go. */
    private static final Set<HangarTerminalBlockEntity> LOADED_ON_CLIENT = Collections.newSetFromMap(
            new WeakHashMap<>());
    private static final Set<HangarTerminalBlockEntity> LOADED_ON_SERVER = Collections.newSetFromMap(
            new WeakHashMap<>());

    public HangarTerminalBlockEntity(final BlockPos pos, final BlockState state) {
        super(HangarBlocks.TERMINAL_ENTITY.get(), pos, state, 0, MachineEnergy.fe(0, 0, 0));
        this.structure = MachineStructure.of(HangarStructure.PATTERN);
        formsWith(this.structure);
        this.design = fields().value("Design", RocketDesign.CODEC, RocketDesign.EMPTY).save().toClient();
        this.plans = fields().value("Plans", RocketDesign.CODEC.listOf(0, MOST_PLANS), List.<RocketDesign>of())
                .save().toClient();
        this.plan = fields().nullable("Plan", RocketDesign.CODEC).save().toClient();
        this.planAt = fields().nullable("PlanAt", BlockPos.CODEC).save().toClient();
        // The side of the tower of the mobile launcher the rocket stands on.
        this.launcher = fields().nullable("Launcher", Direction.CODEC).save().toClient();
        // What its players see of the plan being built: the parts formed, and how much of each is missing.
        this.formedParts = fields().value("FormedParts", Codec.INT.listOf(), List.<Integer>of()).toClient();
        this.missingParts = fields().value("MissingParts", Codec.INT.listOf(), List.<Integer>of()).toClient();
        // The work platforms in its formed bay, which its players' games draw whole.
        this.platforms = fields().value("Platforms", BlockPos.CODEC.listOf(), List.<BlockPos>of()).save()
                .toClient();
        // Its number among the world's hangars, taken the first time it forms; none before.
        this.number = fields().value("Number", Codec.INT, 0).save().toClient();
        // How the building stood when its fittings took their places, by its turn; none while they have not.
        this.shapedAs = fields().value("ShapedAs", Codec.INT, UNSHAPED).save();
        this.crane = new PlanAssembler(pos, this::setChanged);
        fields().part(PlanAssembler.KEY, this.crane).save();
        // Loaded, it starts building again once the chunks its plan stands in are loaded too, unless it already is.
        fields().whenLoaded(level -> this.pending = this.build == null && this.plan.get() != null);
        // Broken, its fittings go dark, its rocket's parts come apart and the blocks its crane held drop.
        fields().whenBroken((level, at) -> {
            unshape(level);
            stopBuild(true);
            this.crane.spill(level, at);
        });
        // On the network its cables reach, it may be set building.
        NetworkMembers.keep(this);
    }

    /** One server tick: the machine's, then its plan started once all of it is loaded, and its crane at work. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final HangarTerminalBlockEntity terminal) {
        MachineBlockEntity.serverTick(level, pos, state, terminal);
        if (terminal.pending) {
            terminal.startBuild();
        }
        if (terminal.build != null && level instanceof ServerLevel server) {
            terminal.crane.tick(server);
        }
        terminal.follow();
    }

    /** The formed hangar loaded in {@code level} whose building or bay holds {@code pos}, if one does. */
    public static Optional<HangarTerminalBlockEntity> holding(final Level level, final BlockPos pos) {
        for (final HangarTerminalBlockEntity terminal : level.isClientSide() ? LOADED_ON_CLIENT : LOADED_ON_SERVER) {
            if (terminal.getLevel() == level && !terminal.isRemoved() && terminal.formed()
                    && (terminal.structure.holdsPlace(pos) || terminal.bay().filter(bay -> bay.holds(pos))
                    .isPresent())) {
                return Optional.of(terminal);
            }
        }
        return Optional.empty();
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
     * whose fittings and platforms find it.
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

    /** The terminals loaded on this player's game. */
    public static List<HangarTerminalBlockEntity> loadedOnClient() {
        return List.copyOf(LOADED_ON_CLIENT);
    }

    /** Whether its building stands whole. */
    public boolean formed() {
        return this.structure.formed();
    }

    /** How its building stands, once formed: its turn from the way the pattern draws it. */
    public Optional<Orientation> turn() {
        return formed() ? this.structure.orientation().filter(Orientation.turns()::contains) : Optional.empty();
    }

    /** Its bay, while its building stands whole. */
    public Optional<HangarBay> bay() {
        return turn().map(turn -> HangarStructure.bay(this.worldPosition, turn));
    }

    /**
     * The way from its control room into its bay, through the windows before its consoles: the way a player at its
     * console looks, as the terminal faces the other way, toward its chair.
     */
    public Direction intoBay() {
        return turn().map(turn -> HangarStructure.turned(turn, Direction.WEST))
                .orElse(getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite());
    }

    /** Its number among the world's hangars, or 0 before it has first formed. */
    public int number() {
        final Integer taken = this.number.get();
        return taken == null ? 0 : taken;
    }

    /** The work platforms in its formed bay. */
    public List<BlockPos> platforms() {
        final List<BlockPos> found = this.platforms.get();
        return found == null ? List.of() : found;
    }

    /** The design on its screen. */
    public RocketDesign design() {
        final RocketDesign now = this.design.get();
        return now == null ? RocketDesign.EMPTY : now;
    }

    /**
     * Keeps {@code to} as the design on its screen, when it keeps within a design's limits and every part of it is
     * one the world knows.
     */
    public void design(final RocketDesign to) {
        if (this.level == null || !to.bounded() || !known(this.level, to)) {
            return;
        }
        this.design.set(to);
        setChanged();
    }

    /** The plans it keeps, in the order they were first saved. */
    public List<RocketDesign> plans() {
        final List<RocketDesign> kept = this.plans.get();
        return kept == null ? List.of() : kept;
    }

    /** The plan sent to its bay to be built, if one was. */
    public Optional<RocketDesign> plan() {
        return Optional.ofNullable(this.plan.get());
    }

    /**
     * Saves the design on its screen as one of its plans, in place of the plan of its name; a design with no name or
     * no parts is not saved, nor a new one past {@link #MOST_PLANS}.
     */
    public void save() {
        final RocketDesign now = design();
        if (now.name().isBlank() || now.stack().sections().isEmpty()) {
            return;
        }
        final List<RocketDesign> kept = new ArrayList<>(plans());
        for (int i = 0; i < kept.size(); i++) {
            if (kept.get(i).name().equals(now.name())) {
                kept.set(i, now);
                this.plans.set(List.copyOf(kept));
                setChanged();
                return;
            }
        }
        if (kept.size() < MOST_PLANS) {
            kept.add(now);
            this.plans.set(List.copyOf(kept));
            setChanged();
        }
    }

    /** Deletes the plan at {@code place} among its plans; nothing for a place it has no plan at. */
    public void delete(final int place) {
        final List<RocketDesign> kept = new ArrayList<>(plans());
        if (place >= 0 && place < kept.size()) {
            kept.remove(place);
            this.plans.set(List.copyOf(kept));
            setChanged();
        }
    }

    /**
     * Sends the design on its screen to its bay to be built, when the building stands whole and the design is ready
     * to fly; saved among its plans too, as every rocket built becomes a plan the hangar keeps.
     *
     * @return whether it was sent
     */
    public boolean confirm() {
        final RocketDesign now = design();
        if (!ready(now)) {
            return false;
        }
        save();
        send(now);
        return true;
    }

    /**
     * Where the plan sent to its bay stands, the middle of the rocket's foot, if one was sent: on the deck of its
     * mobile launcher, or on the floor where the launcher has no room.
     */
    public Optional<BlockPos> planAt() {
        return Optional.ofNullable(this.planAt.get());
    }

    /** The side of the tower of the mobile launcher the rocket sent stands on; none when it stands on the floor. */
    public Optional<Direction> launcherSide() {
        return Optional.ofNullable(this.launcher.get());
    }

    /** The plan being built, on the server, once it has started. */
    public Optional<PlannedBuild> build() {
        return Optional.ofNullable(this.build);
    }

    /** The parts of its plan formed, by their places in the plan's stack, as its players are told. */
    public List<Integer> formedParts() {
        final List<Integer> formed = this.formedParts.get();
        return formed == null ? List.of() : formed;
    }

    /** How many blocks of each part of its plan are missing, as its players are told. */
    public List<Integer> missingParts() {
        final List<Integer> missing = this.missingParts.get();
        return missing == null ? List.of() : missing;
    }

    /** Whether the rocket of its plan and its launcher are built whole, every part formed. */
    public boolean finished() {
        final List<Integer> missing = missingParts();
        return this.plan.get() != null && !missing.isEmpty() && missing.stream().allMatch(count -> count == 0);
    }

    /**
     * Lets its plan go with the rocket, as a crawler takes the rocket and its launcher out of the bay: it stops
     * building, its formed parts left as they are, and gives what the rocket's new keeper needs to draw them.
     */
    public Optional<Released> release() {
        final RocketDesign design = this.plan.get();
        final BlockPos foot = this.planAt.get();
        if (design == null || foot == null) {
            return Optional.empty();
        }
        final List<Integer> formed = formedParts();
        stopBuild(false);
        this.plan.set(null);
        this.planAt.set(null);
        this.launcher.set(null);
        this.formedParts.set(List.of());
        this.missingParts.set(List.of());
        setChanged();
        return Optional.of(new Released(design, foot, formed));
    }

    /** Whether its bay door stands open, as the first block of its lower door says. */
    public boolean doorOpen() {
        final Optional<Orientation> turn = turn();
        if (this.level == null || turn.isEmpty()) {
            return false;
        }
        final BlockState door = this.level.getBlockState(HangarStructure.door(this.worldPosition, turn.get())
                .getFirst());
        return door.is(HangarBlocks.BAY_DOOR.get()) && door.getValue(HangarPieceBlock.OPEN);
    }

    /** Opens its bay door, the lower door and the slot over it together, or shuts it, while its building stands. */
    public void door(final boolean open) {
        final Optional<Orientation> turn = turn();
        if (this.level == null || this.level.isClientSide() || turn.isEmpty()) {
            return;
        }
        for (final BlockPos at : HangarStructure.door(this.worldPosition, turn.get())) {
            final BlockState state = this.level.getBlockState(at);
            if (state.is(HangarBlocks.BAY_DOOR.get()) && state.getValue(HangarPieceBlock.OPEN) != open) {
                this.level.setBlock(at, state.setValue(HangarPieceBlock.OPEN, open), Block.UPDATE_ALL);
            }
        }
    }

    /**
     * A work platform was set or taken away at {@code at}, in its bay: the platform there, and those beside it, take
     * their formed shapes, each with its railing where no platform follows it.
     */
    public void platformChanged(final BlockPos at) {
        if (this.level == null || this.level.isClientSide() || !formed()) {
            return;
        }
        final List<BlockPos> now = new ArrayList<>(platforms());
        now.remove(at);
        for (final BlockPos near : List.of(at, at.north(), at.south(), at.east(), at.west())) {
            if (shapePlatform(this.level, near, true)) {
                if (!now.contains(near)) {
                    now.add(near.immutable());
                }
            }
        }
        this.platforms.set(List.copyOf(now));
        setChanged();
    }

    /** Its crane: what it holds, what it waits for, and the build it lays. */
    public PlanAssembler crane() {
        return this.crane;
    }

    /** The crane's inbox, to the loading dock while the building stands; nothing else passes through. */
    @Override
    public @Nullable IItemHandler itemPort(final PortKind kind, final BlockPos part, final @Nullable Direction side) {
        return kind == PortKind.ITEM_INPUT && formed() ? this.crane.inbox() : null;
    }

    @Override
    public NetworkCategory category() {
        return NetworkCategory.C;
    }

    @Override
    public List<OperationType<?>> carriesOut() {
        return List.of(Assembly.ASSEMBLE);
    }

    /** Why it will not build what the network asks: no building, no such plan, no plan at all, or a plan not ready. */
    @Override
    public @Nullable Text refuses(final OperationType<?> type, final IOperationArgs args) {
        final Refusal why = refusal(args);
        return why == null ? null : why.text();
    }

    /** The blocks its crane waits for, the ones it lays first first. */
    @Override
    public List<ItemNeed> needs() {
        final List<ItemNeed> out = new ArrayList<>();
        for (final Map.Entry<Item, Integer> need : this.crane.needs().entrySet()) {
            out.add(new ItemNeed(need.getKey(), need.getValue()));
        }
        return out;
    }

    /** The crane's inbox, while it builds. */
    @Override
    public Optional<IItemHandler> inbox() {
        return this.build == null ? Optional.empty() : Optional.of(this.crane.inbox());
    }

    /**
     * Sets its crane building, the plan named among its plans sent to its bay first, as its screen's confirm sends
     * it; the request hears from then on how the building goes, until the rocket is whole.
     */
    @Override
    public OperationStatus assemble(final IOperationRequest<Assembly.Args> request) {
        final Refusal why = refusal(request.args());
        if (why != null) {
            request.fail(why.failure());
            return OperationStatus.FAILED;
        }
        final Optional<RocketDesign> chosen = request.args().plan().flatMap(this::planNamed);
        if (chosen.isPresent() && !chosen.equals(plan())) {
            send(chosen.get());
        }
        if (this.assembling != null && this.assembling != request) {
            this.assembling.fail(OperationFailure.of(HangarTexts.TAKEN));
        }
        this.assembling = request;
        request.whenCancelled(() -> {
            if (this.assembling == request) {
                this.assembling = null;
            }
        });
        return follow();
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

    /*
     * Its building checked again: formed, its fittings take their places as it stands, the first time it forms it
     * takes its number among the world's hangars; come apart, they go dark.
     */
    @Override
    protected void structureChecked(final ServerLevel level) {
        final Optional<Orientation> turn = turn();
        final int now = turn.map(Orientation.turns()::indexOf).orElse(UNSHAPED);
        if (now == shaped()) {
            return;
        }
        unshape(level);
        if (turn.isPresent()) {
            this.platforms.set(shape(level, turn.get(), true));
            this.shapedAs.set(now);
            if (number() == 0) {
                this.number.set(HangarNumbers.take(level.getServer()));
            }
        }
        setChanged();
    }

    /* How its fittings were shaped, by the building's turn, or none. */
    private int shaped() {
        final Integer was = this.shapedAs.get();
        return was == null ? UNSHAPED : was;
    }

    /* Its fittings, shaped as the building stood, go dark and are blocks of the kit again. */
    private void unshape(final Level level) {
        final int was = shaped();
        if (was >= 0 && was < Orientation.turns().size()) {
            shape(level, Orientation.turns().get(was), false);
        }
        this.shapedAs.set(UNSHAPED);
        this.platforms.set(List.of());
    }

    /*
     * Gives the fittings of the building standing as {@code turn} says their places, or takes them back: the consoles
     * their screens, the chairs, the status board, the railing and the doors their ways, the crane's pieces their
     * formed shapes turned as they run; and the work platforms in its bay theirs. The platforms it shaped.
     */
    private List<BlockPos> shape(final Level level, final Orientation turn, final boolean formed) {
        for (final HangarStructure.Fitting fitting : HangarStructure.fittings()) {
            final BlockPos at = HangarStructure.world(this.worldPosition, turn, fitting.x(), fitting.y(), fitting.z());
            final BlockState state = level.getBlockState(at);
            final BlockState shaped = fitted(state, fitting, turn, formed);
            if (shaped != state) {
                level.setBlock(at, shaped, Block.UPDATE_CLIENTS);
            }
        }
        final List<BlockPos> found = new ArrayList<>();
        final HangarBay bay = HangarStructure.bay(this.worldPosition, turn);
        final BlockPos low = bay.low();
        final BlockPos high = bay.high();
        for (int sx = SectionPos.blockToSectionCoord(low.getX()); sx <= SectionPos.blockToSectionCoord(high.getX());
             sx++) {
            for (int sz = SectionPos.blockToSectionCoord(low.getZ());
                 sz <= SectionPos.blockToSectionCoord(high.getZ()); sz++) {
                final ChunkAccess chunk = level.getChunk(sx, sz);
                for (int sy = SectionPos.blockToSectionCoord(low.getY());
                     sy <= SectionPos.blockToSectionCoord(high.getY()); sy++) {
                    final int index = level.getSectionIndexFromSectionY(sy);
                    // A section holding only air holds no platform: most of a bay's height is skipped so.
                    if (index >= 0 && index < chunk.getSectionsCount() && !chunk.getSection(index).hasOnlyAir()) {
                        platformsIn(level, bay, SectionPos.of(sx, sy, sz), formed, found);
                    }
                }
            }
        }
        return found;
    }

    /*
     * The work platforms of {@code bay} in the section {@code section}, shaped as {@code formed} says, added to
     * {@code found}.
     */
    private static void platformsIn(final Level level, final HangarBay bay, final SectionPos section,
                                     final boolean formed, final List<BlockPos> found) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        final int fromX = Math.max(bay.low().getX(), section.minBlockX());
        final int toX = Math.min(bay.high().getX(), section.maxBlockX());
        final int fromY = Math.max(bay.low().getY(), section.minBlockY());
        final int toY = Math.min(bay.high().getY(), section.maxBlockY());
        final int fromZ = Math.max(bay.low().getZ(), section.minBlockZ());
        final int toZ = Math.min(bay.high().getZ(), section.maxBlockZ());
        for (int x = fromX; x <= toX; x++) {
            for (int y = fromY; y <= toY; y++) {
                for (int z = fromZ; z <= toZ; z++) {
                    if (level.getBlockState(at.set(x, y, z)).is(HangarBlocks.WORK_PLATFORM.get())
                            && shapePlatform(level, at, formed)) {
                        found.add(at.immutable());
                    }
                }
            }
        }
    }

    /*
     * A work platform at {@code at} takes its formed shape, with its railing where no platform follows it the way it
     * faces, or is a block of the kit again; whether one stands there.
     */
    private static boolean shapePlatform(final Level level, final BlockPos at, final boolean formed) {
        final BlockState state = level.getBlockState(at);
        if (!state.is(HangarBlocks.WORK_PLATFORM.get())) {
            return false;
        }
        final boolean edge = formed && !level.getBlockState(at.relative(state.getValue(
                HorizontalDirectionalBlock.FACING))).is(HangarBlocks.WORK_PLATFORM.get());
        final BlockState shaped = state.setValue(HangarPieceBlock.FORMED, formed).setValue(HangarPieceBlock.EDGE, edge);
        if (shaped != state) {
            level.setBlock(at, shaped, Block.UPDATE_CLIENTS);
        }
        return true;
    }

    /* The state {@code state} at {@code fitting}'s place takes as the building forms or comes apart. */
    private static BlockState fitted(final BlockState state, final HangarStructure.Fitting fitting,
                                     final Orientation turn, final boolean formed) {
        if (state.getBlock() instanceof HangarPieceBlock piece) {
            final BlockState shaped = state.setValue(HangarPieceBlock.FORMED, formed);
            return formed ? shaped.setValue(HorizontalDirectionalBlock.FACING, craneWay(piece.kind(), fitting, turn))
                    : shaped;
        }
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state;
        }
        BlockState shaped = state.setValue(HorizontalDirectionalBlock.FACING, HangarStructure.turned(turn,
                fitting.drawn()));
        if (state.is(HangarBlocks.CONSOLE.get())) {
            shaped = shaped.setValue(HangarConsoleBlock.SCREEN, formed ? HangarConsoleBlock.screenAt(fitting.along())
                    : HangarConsoleBlock.Screen.OFF);
        } else if (state.is(HangarBlocks.DOOR_CONSOLE.get())) {
            shaped = shaped.setValue(DoorConsoleBlock.SCREEN, !formed ? HangarConsoleBlock.Screen.OFF
                    : fitting.left() ? HangarConsoleBlock.Screen.LEFT : HangarConsoleBlock.Screen.RIGHT);
        }
        return shaped;
    }

    /*
     * The way a formed piece of the crane faces: a rail into the bay from its wall, the bridge and the hoist along the
     * bridge's span; as the pattern draws them, the rails along the west and east walls and the bridge across.
     */
    private static Direction craneWay(final HangarPieceBlock.Kind kind, final HangarStructure.Fitting fitting,
                                      final Orientation turn) {
        final Direction drawn = switch (kind) {
            case RAIL -> fitting.x() < 0 ? Direction.EAST : Direction.WEST;
            default -> Direction.EAST;
        };
        return HangarStructure.turned(turn, drawn);
    }

    /* Why it will not build what {@code args} asks, or null when it will. */
    private @Nullable Refusal refusal(final IOperationArgs args) {
        if (!formed()) {
            return new Refusal(HangarTexts.NOT_FORMED, "");
        }
        if (!(args instanceof Assembly.Args asked) || asked.plan().isEmpty()) {
            return plan().isPresent() ? null : new Refusal(HangarTexts.NOTHING_SENT, "");
        }
        final String name = asked.plan().get();
        final Optional<RocketDesign> named = planNamed(name);
        if (named.isEmpty()) {
            return new Refusal(HangarTexts.NO_PLAN_NAMED, name);
        }
        return ready(named.get()) ? null : new Refusal(HangarTexts.NOT_READY, name);
    }

    /* The plan of that name among those it keeps. */
    private Optional<RocketDesign> planNamed(final String name) {
        return plans().stream().filter(kept -> kept.name().equals(name)).findFirst();
    }

    /* Whether {@code design} may be sent to its bay: the building stands whole and the design is ready to fly. */
    private boolean ready(final RocketDesign design) {
        return this.level != null && bay().isPresent() && DesignReport.in(this.level, design.stack(), bay()).ready();
    }

    /*
     * Sends {@code design} to its bay: the plan it built before comes apart, and this one is laid out on the deck of a
     * mobile launcher, its tower toward the far wall, where the launcher has room; on the floor where it has not.
     */
    private void send(final RocketDesign design) {
        stopBuild(true);
        final HangarBay at = bay().orElseThrow();
        final Optional<Direction> side = this.level == null ? Optional.empty()
                : RocketPlans.launcherSide(this.level, design.stack(), at);
        this.plan.set(design);
        this.launcher.set(side.orElse(null));
        this.planAt.set(side.isPresent() ? at.pad().above(LauncherLayout.RAISE) : at.pad());
        setChanged();
        startBuild();
    }

    /*
     * Tells the network's request how the building goes, and lets it go once it is over: done when the rocket is
     * whole, waiting while the crane has no block it can lay or the plan waits for its chunks, building otherwise;
     * failed when the plan is taken back. The state it is in now.
     */
    private OperationStatus follow() {
        final IOperationRequest<Assembly.Args> following = this.assembling;
        if (following == null) {
            return OperationStatus.COMPLETED;
        }
        final OperationStatus now;
        if (this.build == null) {
            now = this.pending && plan().isPresent() ? OperationStatus.WAITING : OperationStatus.FAILED;
        } else if (this.build.complete()) {
            now = OperationStatus.COMPLETED;
        } else {
            now = this.crane.waiting() ? OperationStatus.WAITING : OperationStatus.PROCESSING;
        }
        if (now == OperationStatus.FAILED) {
            following.fail(OperationFailure.of(HangarTexts.STOPPED));
        } else {
            following.report(now);
        }
        if (now.isTerminal()) {
            this.assembling = null;
        }
        return now;
    }

    /* Starts building the plan sent to its bay, where its chunks are all loaded; it waits for them otherwise. */
    private void startBuild() {
        final RocketDesign building = this.plan.get();
        final BlockPos at = this.planAt.get();
        if (!(this.level instanceof ServerLevel server) || building == null || at == null) {
            this.pending = false;
            return;
        }
        final BuildPlan laid = RocketPlans.of(server, building.stack(), this.launcher.get());
        // A rocket's formed parts hold when a block of theirs is broken, holed there, as they do in flight; its
        // launcher's, after them in the plan, come apart into their kit as ever.
        final int rocketParts = building.stack().sections().size();
        final PlannedBuild next = new PlannedBuild(laid, at).holed(part -> part < rocketParts);
        if (!loaded(server, next)) {
            this.pending = true;
            return;
        }
        this.pending = false;
        stopBuild(false);
        this.build = next;
        next.start(server, this.worldPosition, this::told);
        this.crane.build(next);
    }

    /* Stops building, its formed parts given back when {@code giveBack}; its crane holds what it holds. */
    private void stopBuild(final boolean giveBack) {
        final PlannedBuild was = this.build;
        this.build = null;
        this.crane.build(null);
        if (was != null) {
            was.stop(giveBack);
        }
        if (giveBack) {
            this.formedParts.set(List.of());
            this.missingParts.set(List.of());
        }
    }

    /* Its plan's counts changed: its crane looks again, and its players hear the parts formed and what each lacks. */
    private void told() {
        final PlannedBuild now = this.build;
        this.crane.wake();
        if (now == null) {
            return;
        }
        this.formedParts.set(now.formedParts());
        final List<Integer> missing = new ArrayList<>();
        for (int part = 0; part < now.plan().parts(); part++) {
            missing.add(now.missing(part));
        }
        this.missingParts.set(List.copyOf(missing));
    }

    /* Whether every chunk the plan {@code laid} stands in is loaded. */
    private static boolean loaded(final ServerLevel level, final PlannedBuild laid) {
        for (final PlanSpot spot : laid.plan().spots()) {
            if (!level.isLoaded(laid.where(spot))) {
                return false;
            }
        }
        return true;
    }

    /* Whether every part of {@code design}, its boosters' with them, is one {@code level} knows. */
    private static boolean known(final Level level, final RocketDesign design) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        for (final RocketStack.Section section : design.stack().sections()) {
            if (!parts.containsKey(section.part().id())) {
                return false;
            }
            for (final List<RocketStack.Part> booster : section.boosters()) {
                for (final RocketStack.Part segment : booster) {
                    if (!parts.containsKey(segment.id())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * A plan let go with its rocket.
     *
     * @param design the rocket's design
     * @param foot   where the middle of the rocket's foot stands
     * @param formed the parts of the plan formed, by their places in its stack
     */
    public record Released(RocketDesign design, BlockPos foot, List<Integer> formed) {
    }

    /* Why it will not build: a sentence, and what fills it, if anything. */
    private record Refusal(TextKey sentence, String filling) {

        Text text() {
            return this.filling.isEmpty() ? this.sentence.text() : this.sentence.with(this.filling);
        }

        OperationFailure failure() {
            return this.filling.isEmpty() ? OperationFailure.of(this.sentence)
                    : OperationFailure.of(this.sentence, this.filling);
        }
    }
}
