/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.PartField;
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
import dev.jstech.core.multiblock.plan.PlanArms;
import dev.jstech.core.multiblock.plan.PlanAssembler;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.ship.ShipDesign;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
 * and welds it. On a network, a request of {@link Assembly#ASSEMBLE} sets it building, a plan it keeps by name or the
 * plan it was sent, and while it waits for blocks it says which, so the network brings them, made first where they
 * can be.
 */
public final class ShipyardTerminalBlockEntity extends MachineBlockEntity implements INetworkMember, IItemNeeds,
        IAssembler {

    private final MachineStructure structure;
    private final ValueField<ShipDesign> design;
    private final ValueField<List<ShipDesign>> plans;
    private final ValueField<ShipDesign> plan;
    private final ValueField<BlockPos> planAt;
    private final ValueField<Integer> planTurn;
    private final ValueField<List<Integer>> formedParts;
    private final ValueField<List<Integer>> missingParts;
    /* Its robot arms, sent to the players who see them; and what lays through them, with what it holds. */
    private final PlanArms arms;
    private final PartField armsField;
    private final PlanAssembler assembler;
    /* The plan being built, on the server, while its chunks are loaded; and whether it is to start. */
    private @Nullable PlannedBuild build;
    private boolean pending;
    /* The network's request to build, followed until the ship is whole; on the server. */
    private @Nullable IOperationRequest<Assembly.Args> assembling;

    /** The most plans a shipyard keeps. */
    public static final int MOST_PLANS = 16;
    /** The design a shipyard's builder starts from: no parts, no name. */
    public static final ShipDesign EMPTY = new ShipDesign("", List.of(), List.of());

    /* The terminals loaded on a player's game, forgotten as they go. */
    private static final Set<ShipyardTerminalBlockEntity> LOADED_ON_CLIENT = Collections.newSetFromMap(
            new WeakHashMap<>());

    public ShipyardTerminalBlockEntity(final BlockPos pos, final BlockState state) {
        super(ShipyardBlocks.TERMINAL_ENTITY.get(), pos, state, 0, MachineEnergy.fe(0, 0, 0));
        this.structure = MachineStructure.of(ShipyardStructure.PATTERN).merges();
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
        // Told only once it is built, after the field it changes is declared.
        this.arms = new PlanArms(this::armsChanged);
        this.armsField = fields().part("Arms", this.arms).save().toClient();
        this.assembler = new PlanAssembler(pos, this::setChanged).arms(this.arms);
        fields().part(PlanAssembler.KEY, this.assembler).save();
        // Loaded, it starts building again once the chunks its plan stands in are loaded too, unless it already is.
        fields().whenLoaded(level -> this.pending = this.build == null && this.plan.get() != null);
        fields().whenBroken((level, at) -> {
            stopBuild(true);
            this.assembler.spill(level, at);
        });
        // On the network its cables reach, it may be set building.
        NetworkMembers.keep(this);
    }

    /** One server tick: the machine's, then its plan started once all of it is loaded, and its arms at work. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final ShipyardTerminalBlockEntity terminal) {
        MachineBlockEntity.serverTick(level, pos, state, terminal);
        if (terminal.pending) {
            terminal.startBuild();
        }
        if (terminal.build != null && level instanceof ServerLevel server) {
            terminal.assembler.tick(server);
        }
        terminal.follow();
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
    public static List<ShipyardTerminalBlockEntity> loadedOnClient() {
        return List.copyOf(LOADED_ON_CLIENT);
    }

    /** Whether its dock stands whole. */
    public boolean formed() {
        return this.structure.formed();
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

    /**
     * Sends the design to the dock to be built, when it is ready to fly and fits the dock; whether it was sent. The
     * plan built before comes apart, its formed parts' blocks given back, unless it is the same plan, which goes on.
     */
    public boolean confirm() {
        return send(design());
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

    @Override
    public NetworkCategory category() {
        return NetworkCategory.C;
    }

    @Override
    public List<OperationType<?>> carriesOut() {
        return List.of(Assembly.ASSEMBLE);
    }

    /** Why it will not build what the network asks: no dock, no such plan, no plan at all, or a plan not ready. */
    @Override
    public @Nullable Text refuses(final OperationType<?> type, final IOperationArgs args) {
        final Refusal why = refusal(args);
        return why == null ? null : why.text();
    }

    /** The blocks its arms wait for, the ones they lay first first. */
    @Override
    public List<ItemNeed> needs() {
        final List<ItemNeed> out = new ArrayList<>();
        for (final Map.Entry<Item, Integer> need : this.assembler.needs().entrySet()) {
            out.add(new ItemNeed(need.getKey(), need.getValue()));
        }
        return out;
    }

    /** The arms' inbox, while it builds. */
    @Override
    public Optional<IItemHandler> inbox() {
        return this.build == null ? Optional.empty() : Optional.of(this.assembler.inbox());
    }

    /**
     * Sets its arms building, the plan named among its plans sent to its dock first, as its builder's confirm sends
     * it; the request hears from then on how the building goes, until the ship is whole.
     */
    @Override
    public OperationStatus assemble(final IOperationRequest<Assembly.Args> request) {
        final Refusal why = refusal(request.args());
        if (why != null) {
            request.fail(why.failure());
            return OperationStatus.FAILED;
        }
        final Optional<ShipDesign> chosen = request.args().plan().flatMap(this::planNamed);
        if (chosen.isPresent()) {
            send(chosen.get());
        }
        if (this.assembling != null && this.assembling != request) {
            this.assembling.fail(OperationFailure.of(ShipyardTexts.TAKEN));
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
     * Tells the network's request how the building goes, and lets it go once it is over: done when the ship is
     * whole, waiting while the arms have no block they can lay or the plan waits for its chunks, building otherwise;
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
            now = this.assembler.waiting() ? OperationStatus.WAITING : OperationStatus.PROCESSING;
        }
        if (now == OperationStatus.FAILED) {
            following.fail(OperationFailure.of(ShipyardTexts.STOPPED));
        } else {
            following.report(now);
        }
        if (now.isTerminal()) {
            this.assembling = null;
        }
        return now;
    }

    /* Why it will not build what {@code args} asks, or null when it will. */
    private @Nullable Refusal refusal(final IOperationArgs args) {
        if (!formed()) {
            return new Refusal(ShipyardTexts.NOT_FORMED, "");
        }
        if (!(args instanceof Assembly.Args asked) || asked.plan().isEmpty()) {
            return plan().isPresent() ? null : new Refusal(ShipyardTexts.NOTHING_SENT, "");
        }
        final String name = asked.plan().get();
        final Optional<ShipDesign> named = planNamed(name);
        if (named.isEmpty()) {
            return new Refusal(ShipyardTexts.NO_PLAN_NAMED, name);
        }
        return this.level != null && ready(this.level, named.get()) ? null
                : new Refusal(ShipyardTexts.NOT_READY, name);
    }

    /* The plan of that name among those it keeps. */
    private Optional<ShipDesign> planNamed(final String name) {
        return plans().stream().filter(kept -> kept.name().equals(name)).findFirst();
    }

    /* Starts building the plan sent, where its chunks are all loaded; it waits for them otherwise. */
    private void startBuild() {
        final ShipDesign building = this.plan.get();
        final BlockPos at = this.planAt.get();
        if (!(this.level instanceof ServerLevel server) || building == null || at == null) {
            this.pending = false;
            return;
        }
        final PlannedBuild next = new PlannedBuild(ShipyardPlans.of(server, building, planTurn()), at);
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
        setChanged();
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
