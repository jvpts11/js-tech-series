/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.edit;

import dev.jstech.core.JsCore;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.task.Tasks;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Large edits of the world that never stall the server: a crater blasted, a city levelled, a terraformed valley. The
 * edit is planned away from the server's thread, from a snapshot of the blocks it reads ({@link RegionSnapshot}), and
 * its blocks are set on the server's thread a share of each tick, within the work's budget, the edits under way taking
 * turns in the order they began.
 *
 * <pre>{@code
 * WorldEdit crater = WorldEdits.plan(level, BoundingBox.fromCorners(from, to), blocks -> {
 *     EditPlan.Builder plan = EditPlan.builder();
 *     // read blocks.get(x, y, z) freely, set what the crater leaves
 *     return plan.build();
 * });
 * crater.whenOver(edit -> report(edit.state()));     // DONE, STOPPED or FAILED
 * }</pre>
 */
public final class WorldEdits {

    /** The setting of the edits' blocks, timed, watched and kept to its budget. */
    public static final Section SECTION = Section.builder(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "world_edits"), CoreLogs.WORLD).budget(Duration.ofMillis(2)).declare();

    private static final Logger LOGGER = CoreLogs.WORLD.logger();
    private static final List<WorldEdit> SETTING = new ArrayList<>();
    /* The edits under way for players, planning or setting, each until it ends. */
    private static final List<WorldEdit> FOR_PLAYERS = new ArrayList<>();
    /* How long a tick's share may run, in nanoseconds, and how many blocks are set between looks at the clock. */
    private static final long SLICE_NANOS = 1_500_000L;
    private static final int BETWEEN_LOOKS = 64;
    /* Each player's last undoable edit, while the server runs. */
    private static final Map<UUID, WorldEdit> LAST = new HashMap<>();
    /* The names a structure file is written with. */
    private static final String PALETTE = "palette";
    private static final String PALETTES = "palettes";
    private static final String BLOCKS = "blocks";
    private static final String POS = "pos";
    private static final String STATE = "state";
    private static final String NBT = "nbt";

    private WorldEdits() {
    }

    /** Hands the game's bus the end of each tick and the server's stop, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (!SETTING.isEmpty() && SECTION.due(event.getServer().getTickCount())) {
                SECTION.run(() -> set(SLICE_NANOS * SECTION.stride()));
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {
            List.copyOf(SETTING).forEach(WorldEdit::stop);
            SETTING.clear();
            FOR_PLAYERS.clear();
            LAST.clear();
        });
    }

    /**
     * Snapshots {@code box} of {@code level} now, plans the edit from it away from the server's thread with
     * {@code planner}, and sets what it plans, a share of each tick. On the server's thread.
     */
    public static WorldEdit plan(final ServerLevel level, final BoundingBox box,
                                 final Function<RegionSnapshot, EditPlan> planner) {
        Objects.requireNonNull(planner, "planner");
        final RegionSnapshot snapshot = RegionSnapshot.of(level, box);
        final WorldEdit edit = new WorldEdit(level);
        edit.planning(Tasks.onServer(() -> planner.apply(snapshot), made -> {
            if (!edit.over()) {
                begin(edit, made);
            }
        }, failure -> {
            LOGGER.warn("A large edit of {} could not be planned", level.dimension().location(), failure);
            edit.end(WorldEdit.State.FAILED);
        }));
        return edit;
    }

    /** Sets the blocks of {@code plan}, made already, in {@code level}, a share each tick. On the server's thread. */
    public static WorldEdit apply(final ServerLevel level, final EditPlan plan) {
        final WorldEdit edit = new WorldEdit(level);
        begin(edit, Objects.requireNonNull(plan, "plan"));
        return edit;
    }

    /**
     * Plans an edit as {@link #plan} does, for {@code player}: it counts against their limit of large edits under way
     * ({@link PlayerLimits#LARGE_EDITS}) until it ends, done, stopped or failed. Empty, and nothing planned, when they
     * are at the limit, which {@code PlayerLimits.reached(LARGE_EDITS, underWayFor(player))} tells them.
     */
    public static Optional<WorldEdit> planFor(final ServerLevel level, final UUID player, final BoundingBox box,
                                              final Function<RegionSnapshot, EditPlan> planner) {
        if (!PlayerLimits.allows(player, PlayerLimits.LARGE_EDITS, underWayFor(player))) {
            return Optional.empty();
        }
        return Optional.of(counted(plan(level, box, planner), player));
    }

    /** Sets the blocks of {@code plan} as {@link #apply} does, for {@code player}, counted as {@link #planFor} is. */
    public static Optional<WorldEdit> applyFor(final ServerLevel level, final UUID player, final EditPlan plan) {
        if (!PlayerLimits.allows(player, PlayerLimits.LARGE_EDITS, underWayFor(player))) {
            return Optional.empty();
        }
        return Optional.of(counted(apply(level, plan), player));
    }

    /**
     * How many edits {@code player} has under way, planning or setting: what their limit counts. An edit does not
     * outlive the server, so neither does the count, which is of the edits themselves.
     */
    public static int underWayFor(final UUID player) {
        int count = 0;
        for (final WorldEdit edit : FOR_PLAYERS) {
            if (player.equals(edit.player())) {
                count++;
            }
        }
        return count;
    }

    /** The edits under way, in the order they began. */
    public static List<WorldEdit> underWay() {
        return List.copyOf(SETTING);
    }

    /**
     * Sets blocks of the edits under way until {@code budgetNanos} are spent: what each tick does. Returns how many
     * were set.
     */
    public static int set(final long budgetNanos) {
        final long start = System.nanoTime();
        int setNow = 0;
        while (!SETTING.isEmpty() && System.nanoTime() - start < budgetNanos) {
            final WorldEdit edit = SETTING.getFirst();
            final EditPlan plan = edit.plan();
            if (edit.over() || plan == null) {
                SETTING.removeFirst();
                continue;
            }
            final int from = edit.set();
            final int to = Math.min(plan.size(), from + BETWEEN_LOOKS);
            final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int i = from; i < to; i++) {
                at.set(plan.place(i));
                if (plan.undoable()) {
                    edit.replaced(plan.place(i), edit.level().getBlockState(at), held(edit.level(), at));
                }
                // As the game's structures do, what a container held is cleared rather than spilled on the ground.
                Clearable.tryClear(edit.level().getBlockEntity(at));
                edit.level().setBlock(at, plan.block(i), plan.flags());
                final CompoundTag data = plan.data(i);
                final BlockEntity entity = data == null ? null : edit.level().getBlockEntity(at);
                if (entity != null) {
                    entity.loadWithComponents(data, edit.level().registryAccess());
                    entity.setChanged();
                }
            }
            edit.advance(to - from);
            setNow += to - from;
            if (edit.set() >= plan.size()) {
                SETTING.removeFirst();
                edit.end(WorldEdit.State.DONE);
            }
        }
        return setNow;
    }

    /**
     * The last edit made for {@code player} whose plan was undoable, under way or ended, while the server runs: the
     * one {@code /jstech edit undo} undoes. Only the last is kept, so what an older one replaced is let go.
     */
    public static Optional<WorldEdit> lastFor(final UUID player) {
        return Optional.ofNullable(LAST.get(player));
    }

    /**
     * Saves the blocks of {@code box} in {@code level}, with their block entities and the air among them, as the
     * game's structure file {@code id} in the world's {@code generated} folder, as a structure block saves one; whether
     * it was written. On the server's thread.
     */
    public static boolean export(final ServerLevel level, final BoundingBox box, final ResourceLocation id) {
        final StructureTemplateManager templates = level.getStructureManager();
        final StructureTemplate template = templates.getOrCreate(id);
        template.fillFromWorld(level, new BlockPos(box.minX(), box.minY(), box.minZ()), new Vec3i(box.getXSpan(),
                box.getYSpan(), box.getZSpan()), false, Blocks.STRUCTURE_VOID);
        return templates.save(id);
    }

    /**
     * The blocks of the structure file {@code id} (the world's own, or any resource pack's) as a plan that sets them
     * with {@code origin} at its corner, turned by {@code rotation} round it, each with what its block entity held: to
     * be made undoable or waking its neighbours, built and applied as a large edit. Empty when no file has that id.
     */
    public static Optional<EditPlan.Builder> planFrom(final ServerLevel level, final ResourceLocation id,
                                                      final BlockPos origin, final Rotation rotation) {
        final Optional<StructureTemplate> template = level.getStructureManager().get(id);
        if (template.isEmpty()) {
            return Optional.empty();
        }
        final CompoundTag file = template.get().save(new CompoundTag());
        final HolderGetter<Block> blocks = level.holderLookup(Registries.BLOCK);
        final List<BlockState> palette = new ArrayList<>();
        final ListTag written = file.contains(PALETTES, Tag.TAG_LIST)
                ? file.getList(PALETTES, Tag.TAG_LIST).getList(0) : file.getList(PALETTE, Tag.TAG_COMPOUND);
        for (int i = 0; i < written.size(); i++) {
            palette.add(NbtUtils.readBlockState(blocks, written.getCompound(i)));
        }
        final StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        final EditPlan.Builder plan = EditPlan.builder();
        final ListTag placed = file.getList(BLOCKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < placed.size(); i++) {
            final CompoundTag one = placed.getCompound(i);
            final ListTag pos = one.getList(POS, Tag.TAG_INT);
            final BlockPos at = origin.offset(StructureTemplate.calculateRelativePosition(settings, new BlockPos(
                    pos.getInt(0), pos.getInt(1), pos.getInt(2))));
            final BlockState state = palette.get(one.getInt(STATE)).rotate(rotation);
            plan.set(at.getX(), at.getY(), at.getZ(), state, one.contains(NBT, Tag.TAG_COMPOUND)
                    ? one.getCompound(NBT) : null);
        }
        return Optional.of(plan);
    }

    /* Counts {@code edit} as {@code player}'s until it ends, and keeps it as the last they may undo. */
    private static WorldEdit counted(final WorldEdit edit, final UUID player) {
        edit.forPlayer(player);
        if (!edit.over()) {
            FOR_PLAYERS.add(edit);
            edit.whenOver(FOR_PLAYERS::remove);
        }
        final EditPlan plan = edit.plan();
        if (plan == null || plan.undoable()) {
            LAST.put(player, edit);
        }
        return edit;
    }

    /* What the block entity at {@code pos} holds, to be put back by an undo; null for a block with none. */
    private static @Nullable CompoundTag held(final ServerLevel level, final BlockPos pos) {
        final BlockEntity entity = level.getBlockEntity(pos);
        return entity == null ? null : entity.saveWithFullMetadata(level.registryAccess());
    }

    private static void begin(final WorldEdit edit, final EditPlan plan) {
        edit.planned(plan);
        if (plan.size() == 0) {
            edit.end(WorldEdit.State.DONE);
        } else {
            SETTING.add(edit);
        }
    }
}
