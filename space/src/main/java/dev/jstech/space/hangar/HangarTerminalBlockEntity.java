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
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.text.Text;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.rocket.RocketStack;
import java.util.ArrayList;
import java.util.Collections;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Rocket Hangar's terminal: it looks at its bay every few seconds ({@link HangarBays}) and holds the bay it found,
 * told to the players who see it; as the bay forms its pieces take their own shapes, its rails, its bridge and its
 * hoist turned to lie as they run, and as it is undone they are blocks of the kit again. It remembers what kept the bay
 * from forming, for the player who uses it. It keeps the hangar's rockets: the design on its screen, the plans saved,
 * and the plan sent to the bay to be built.
 */
public final class HangarTerminalBlockEntity extends SyncedBlockEntity {

    private final ValueField<HangarBay> bay;
    private final ValueField<RocketDesign> design;
    private final ValueField<List<RocketDesign>> plans;
    private final ValueField<RocketDesign> plan;
    private final ValueField<BlockPos> planAt;
    private final ValueField<List<Integer>> formedParts;
    private final ValueField<List<Integer>> missingParts;
    private Text reason = HangarTexts.NOT_YET.text();
    private int wait;
    /* The plan being built, on the server, while its chunk is loaded; and whether it is to start. */
    private @Nullable PlannedBuild build;
    private boolean pending;

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
        // What its players see of the plan being built: the parts formed, and how much of each is missing.
        this.formedParts = fields().value("FormedParts", Codec.INT.listOf(), List.<Integer>of()).toClient();
        this.missingParts = fields().value("MissingParts", Codec.INT.listOf(), List.<Integer>of()).toClient();
        // Loaded, it starts building again once the chunks its plan stands in are loaded too.
        fields().whenLoaded(level -> this.pending = true);
    }

    /** Looks at its bay when it is time to, and starts building its plan once all of it is loaded. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final HangarTerminalBlockEntity terminal) {
        if (terminal.pending) {
            terminal.startBuild();
        }
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
        if (this.level == null || bay().isEmpty() || !DesignReport.in(this.level, now.stack(), bay()).ready()) {
            return false;
        }
        save();
        // The plan it built before comes apart, and the new one is laid out on the bay's floor.
        stopBuild(true);
        this.plan.set(now);
        this.planAt.set(bay().get().pad());
        setChanged();
        startBuild();
        return true;
    }

    /** Where the plan sent to its bay stands, the middle of the rocket's foot, if one was sent. */
    public Optional<BlockPos> planAt() {
        return Optional.ofNullable(this.planAt.get());
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
        this.wait = now == null ? LOOK_UNFORMED : LOOK_FORMED;
    }

    /** Undoes its bay, as it is broken: its pieces are blocks of the kit again, and its rocket's parts come apart. */
    public void undo() {
        final HangarBay was = this.bay.get();
        if (was != null && this.level != null) {
            shape(this.level, was, false);
            this.bay.set(null);
        }
        stopBuild(true);
    }

    /* Starts building the plan sent to its bay, where its chunks are all loaded; it waits for them otherwise. */
    private void startBuild() {
        final RocketDesign building = this.plan.get();
        final BlockPos at = this.planAt.get();
        if (!(this.level instanceof ServerLevel server) || building == null || at == null) {
            this.pending = false;
            return;
        }
        final BuildPlan laid = RocketPlans.of(server, building.stack());
        final PlannedBuild next = new PlannedBuild(laid, at);
        if (!loaded(server, next)) {
            this.pending = true;
            return;
        }
        this.pending = false;
        stopBuild(false);
        this.build = next;
        next.start(server, this.worldPosition, this::told);
    }

    /* Stops building, its formed parts given back when {@code giveBack}. */
    private void stopBuild(final boolean giveBack) {
        final PlannedBuild was = this.build;
        this.build = null;
        if (was != null) {
            was.stop(giveBack);
        }
        if (giveBack) {
            this.formedParts.set(List.of());
            this.missingParts.set(List.of());
        }
    }

    /* Its plan's counts changed: its players are told the parts formed and what each lacks. */
    private void told() {
        final PlannedBuild now = this.build;
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
}
