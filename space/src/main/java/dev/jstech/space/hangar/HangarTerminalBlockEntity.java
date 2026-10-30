/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.SyncedBlockEntity;
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
import dev.jstech.core.multiblock.IMultiblockPortHost;
import dev.jstech.core.multiblock.MultiblockPartBlockEntity;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanAssembler;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.launcher.LauncherLayout;
import dev.jstech.space.rocket.RocketStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A Rocket Hangar's terminal: it looks at its bay every few seconds ({@link HangarBays}) and holds the bay it found,
 * told to the players who see it; as the bay forms its pieces take their own shapes, its rails, its bridge and its
 * hoist turned to lie as they run, and as it is undone they are blocks of the kit again. It remembers what kept the bay
 * from forming, for the player who uses it. It keeps the hangar's rockets: the design on its screen, the plans saved,
 * and the plan sent to the bay to be built.
 *
 * <p>Its crane builds the plan sent by itself, block by block ({@link PlanAssembler}), from what comes in through the
 * item ports in the bay's shell: whatever the hangar waits for, it builds into the rocket. On a network, a request of
 * {@link Assembly#ASSEMBLE} sets it building, a plan it keeps by name or the plan it was sent, and while it waits for
 * blocks it says which, so the network brings them, made first where they can be.
 */
public final class HangarTerminalBlockEntity extends SyncedBlockEntity implements IMultiblockPortHost, INetworkMember,
        IItemNeeds, IAssembler {

    private final ValueField<HangarBay> bay;
    private final ValueField<RocketDesign> design;
    private final ValueField<List<RocketDesign>> plans;
    private final ValueField<RocketDesign> plan;
    private final ValueField<BlockPos> planAt;
    private final ValueField<Direction> launcher;
    private final ValueField<List<Integer>> formedParts;
    private final ValueField<List<Integer>> missingParts;
    /* The crane, and what it holds of the blocks its rocket wants. */
    private final PlanAssembler crane;
    /* The ports of its shell it is bound to, on the server. */
    private final Set<BlockPos> ports = new HashSet<>();
    private Text reason = HangarTexts.NOT_YET.text();
    private int wait;
    /* The plan being built, on the server, while its chunk is loaded; and whether it is to start. */
    private @Nullable PlannedBuild build;
    private boolean pending;
    /* The network's request to build, followed until the rocket is whole; on the server. */
    private @Nullable IOperationRequest<Assembly.Args> assembling;

    /** The most plans a hangar keeps. */
    public static final int MOST_PLANS = 16;
    /* How often it looks at its bay, in ticks: oftener while it is not formed, the player building it. */
    private static final int LOOK_UNFORMED = 40;
    private static final int LOOK_FORMED = 100;

    /* The terminals loaded on a player's game, forgotten as they go. */
    private static final Set<HangarTerminalBlockEntity> LOADED_ON_CLIENT = Collections.newSetFromMap(
            new WeakHashMap<>());

    public HangarTerminalBlockEntity(final BlockPos pos, final BlockState state) {
        super(HangarBlocks.TERMINAL_ENTITY.get(), pos, state);
        this.bay = fields().nullable("Bay", HangarBay.CODEC).save().toClient();
        this.design = fields().value("Design", RocketDesign.CODEC, RocketDesign.EMPTY).save().toClient();
        this.plans = fields().value("Plans", RocketDesign.CODEC.listOf(0, MOST_PLANS), List.<RocketDesign>of())
                .save().toClient();
        this.plan = fields().nullable("Plan", RocketDesign.CODEC).save().toClient();
        this.planAt = fields().nullable("PlanAt", BlockPos.CODEC).save().toClient();
        // The side of the tower of the mobile launcher the rocket stands on, where the bay has room for one.
        this.launcher = fields().nullable("Launcher", Direction.CODEC).save().toClient();
        // What its players see of the plan being built: the parts formed, and how much of each is missing.
        this.formedParts = fields().value("FormedParts", Codec.INT.listOf(), List.<Integer>of()).toClient();
        this.missingParts = fields().value("MissingParts", Codec.INT.listOf(), List.<Integer>of()).toClient();
        this.crane = new PlanAssembler(pos, this::setChanged);
        fields().part(PlanAssembler.KEY, this.crane).save();
        // Loaded, it starts building again once the chunks its plan stands in are loaded too, unless it already is.
        fields().whenLoaded(level -> this.pending = this.build == null);
        // On the network its cables reach, it may be set building.
        NetworkMembers.keep(this);
    }

    /**
     * Looks at its bay when it is time to, and starts building its plan once all of it is loaded; its crane lays the
     * next blocks it can, and the network's request hears how the building goes.
     */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final HangarTerminalBlockEntity terminal) {
        if (terminal.pending) {
            terminal.startBuild();
        }
        if (terminal.build != null && level instanceof ServerLevel server) {
            terminal.crane.tick(server);
        }
        terminal.follow();
        if (--terminal.wait > 0) {
            return;
        }
        terminal.look();
    }

    /** Its plan's chunk unloads: it stops watching it, leaving its formed parts as they are for when it loads. */
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        stopBuild(false);
        LOADED_ON_CLIENT.remove(this);
    }

    /** On a player's game it is known among the terminals loaded there, whose plans are shown as holograms. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide()) {
            LOADED_ON_CLIENT.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED_ON_CLIENT.remove(this);
    }

    /** The terminals loaded on this player's game. */
    public static List<HangarTerminalBlockEntity> loadedOnClient() {
        return List.copyOf(LOADED_ON_CLIENT);
    }

    /** The bay it found, if it is formed. */
    public Optional<HangarBay> bay() {
        return Optional.ofNullable(this.bay.get());
    }

    /** What it says to the player who uses it: its bay's size, or what keeps it from forming. */
    public Text reason() {
        return this.reason;
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
     * Sends the design on its screen to its bay to be built, when the bay is formed and the design is ready to fly;
     * saved among its plans too, as every rocket built becomes a plan the hangar keeps.
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
     * Where the plan sent to its bay stands, the middle of the rocket's foot, if one was sent: on the floor, or on the
     * deck of its mobile launcher.
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

    /** Looks at its bay now, forming it or undoing it as it finds it. */
    public void look() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        final Direction into = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        final HangarBays.Result found = HangarBays.find(this.level, this.worldPosition, into, new HangarBays.Limits(
                SpaceServerConfig.FILE.get(SpaceServerConfig.HANGAR_NARROWEST),
                SpaceServerConfig.FILE.get(SpaceServerConfig.HANGAR_WIDEST),
                SpaceServerConfig.FILE.get(SpaceServerConfig.HANGAR_LOWEST),
                SpaceServerConfig.FILE.get(SpaceServerConfig.HANGAR_TALLEST)));
        this.reason = found.reason();
        final HangarBay was = this.bay.get();
        final HangarBay now = found.bay().orElse(null);
        if (!Objects.equals(was, now)) {
            if (was != null) {
                shape(this.level, was, false);
            }
            if (now != null) {
                shape(this.level, now, true);
            }
            this.bay.set(now);
            setChanged();
        }
        bind(found.ports());
        this.wait = now == null ? LOOK_UNFORMED : LOOK_FORMED;
    }

    /**
     * Undoes its bay, as it is broken: its pieces are blocks of the kit again, its ports let go, and its rocket's
     * parts come apart.
     */
    public void undo() {
        final HangarBay was = this.bay.get();
        if (was != null && this.level != null) {
            shape(this.level, was, false);
            this.bay.set(null);
        }
        bind(List.of());
        stopBuild(true);
    }

    /** It is broken: its bay is undone, and the blocks its crane held drop where it stood. */
    public void broken() {
        undo();
        if (this.level != null && !this.level.isClientSide()) {
            this.crane.spill(this.level, this.worldPosition);
        }
    }

    /** Its crane: what it holds, what it waits for, and the build it lays. */
    public PlanAssembler crane() {
        return this.crane;
    }

    /** The crane's inbox, to an item port of its shell while the bay is formed; nothing else passes through. */
    @Override
    public @Nullable IItemHandler itemPort(final PortKind kind, final BlockPos part, final @Nullable Direction side) {
        return kind == PortKind.ITEM_INPUT && bay().isPresent() ? this.crane.inbox() : null;
    }

    @Override
    public NetworkCategory category() {
        return NetworkCategory.C;
    }

    @Override
    public List<OperationType<?>> carriesOut() {
        return List.of(Assembly.ASSEMBLE);
    }

    /** Why it will not build what the network asks: no bay, no such plan, no plan at all, or a plan not ready. */
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

    /* Why it will not build what {@code args} asks, or null when it will. */
    private @Nullable Refusal refusal(final IOperationArgs args) {
        if (bay().isEmpty()) {
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

    /* Whether {@code design} may be sent to its bay: the bay is formed and the design is ready to fly from it. */
    private boolean ready(final RocketDesign design) {
        return this.level != null && bay().isPresent() && DesignReport.in(this.level, design.stack(), bay()).ready();
    }

    /*
     * Sends {@code design} to its bay: the plan it built before comes apart, and this one is laid out, on the deck of
     * a mobile launcher where the bay has room for one, on the floor where it has not.
     */
    private void send(final RocketDesign design) {
        stopBuild(true);
        final HangarBay at = bay().orElseThrow();
        final Optional<Direction> side = this.level == null ? Optional.empty()
                : RocketPlans.launcherSide(this.level, design.stack(), at);
        this.plan.set(design);
        this.launcher.set(side.orElse(null));
        this.planAt.set(side.isPresent() ? at.pad().above(LauncherLayout.DECK) : at.pad());
        setChanged();
        startBuild();
    }

    /*
     * Tells the network's request how the building goes, and lets it go once it is over: done when the rocket is
     * whole, waiting while the crane has no block it can lay or the plan waits for its chunks, building otherwise;
     * failed when the plan is taken back or the bay comes apart.
     *
     * @return the state it is in now
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

    /* Binds the ports of its shell found at {@code found} to it, and lets go of those no longer there. */
    private void bind(final List<BlockPos> found) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        final Set<BlockPos> now = Set.copyOf(found);
        for (final BlockPos gone : List.copyOf(this.ports)) {
            if (!now.contains(gone)) {
                this.ports.remove(gone);
                if (this.level.isLoaded(gone) && this.level.getBlockEntity(gone)
                        instanceof MultiblockPartBlockEntity port && this.worldPosition.equals(port.terminalPos())) {
                    port.release();
                    this.level.invalidateCapabilities(gone);
                }
            }
        }
        for (final BlockPos at : now) {
            if (this.ports.add(at) && this.level.getBlockEntity(at) instanceof MultiblockPartBlockEntity port) {
                port.setTerminal(this.worldPosition);
                this.level.invalidateCapabilities(at);
            }
        }
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
        final PlannedBuild next = new PlannedBuild(laid, at);
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

    /*
     * Gives the pieces of {@code bay} their own shapes, or takes them back: each piece in the bay, and each door in its
     * walls; a formed bay's rails turned to look into the bay, its bridge and its hoist to run the way it spans.
     */
    private static void shape(final Level level, final HangarBay bay, final boolean formed) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        final BlockPos low = bay.low();
        final BlockPos high = bay.high();
        for (int x = low.getX() - 1; x <= high.getX() + 1; x++) {
            for (int z = low.getZ() - 1; z <= high.getZ() + 1; z++) {
                for (int y = low.getY(); y <= high.getY(); y++) {
                    final BlockState state = level.getBlockState(at.set(x, y, z));
                    if (!(state.getBlock() instanceof HangarPieceBlock piece)) {
                        continue;
                    }
                    final boolean inside = bay.holds(at);
                    if (!inside && piece.kind() != HangarPieceBlock.Kind.DOOR) {
                        continue;
                    }
                    BlockState shaped = state.setValue(HangarPieceBlock.FORMED, formed);
                    final Direction way = formed ? way(piece.kind(), bay, at) : null;
                    if (way != null) {
                        shaped = shaped.setValue(HorizontalDirectionalBlock.FACING, way);
                    }
                    if (shaped != state) {
                        level.setBlock(at, shaped, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
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

    /* The way a formed piece looks: a rail into the bay, the bridge and the hoist along their span; others as set. */
    private static @Nullable Direction way(final HangarPieceBlock.Kind kind, final HangarBay bay, final BlockPos at) {
        return switch (kind) {
            case RAIL -> at.getZ() == bay.low().getZ() && bay.span() == Direction.Axis.Z ? Direction.SOUTH
                    : at.getZ() == bay.high().getZ() && bay.span() == Direction.Axis.Z ? Direction.NORTH
                    : at.getX() == bay.low().getX() ? Direction.EAST : Direction.WEST;
            case BRIDGE, HOIST -> bay.span() == Direction.Axis.Z ? Direction.NORTH : Direction.EAST;
            default -> null;
        };
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
