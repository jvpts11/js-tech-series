/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.multiblock.IStructureWatcher;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.multiblock.shape.FormedPlace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A plan being built, on the server: its places laid out from its origin in a world, and for each of its parts how
 * many of its blocks are missing. It counts them all once, as it starts; after that a block put down or taken away at
 * one of its places changes its part's count by one, told by the world's index of structures, in the same short time
 * however large the plan. A part forms once nothing of it is missing, its blocks merging into its formed model with no
 * terminal of its own, and comes apart, every block back as it was, as soon as one goes. What it counts is read by
 * whatever shows or builds the plan, a hologram of what is missing, a crane laying the next block: how many of each
 * block it still wants, and which missing place comes next, the lowest first, layer by layer. Its owner, the block
 * that keeps the plan, draws its formed parts, and may hand the plan on, as a rocket takes its own plan with it when it
 * leaves its hangar.
 */
public final class PlannedBuild implements IStructureWatcher {

    private final BuildPlan plan;
    private final BlockPos origin;
    private final List<List<Integer>> spotsOf;
    private final List<Set<Integer>> reachesOf;
    private final Map<Long, Integer> byPlace = new HashMap<>();
    private final boolean[] present;
    private final int[] missing;
    private final boolean[] formed;
    /* The places in the order they are laid, the lowest first, and where each is in that order. */
    private final int[] order;
    private final int[] rank;
    /* How many of each block the missing places want, and where in the order the first missing place is. */
    private final Map<ResourceLocation, Integer> wanted = new HashMap<>();
    private int first;
    private @Nullable ServerLevel level;
    private @Nullable BlockPos owner;
    private Runnable told = () -> { };
    /* Which of its formed parts hold when one of their blocks goes, holed there, rather than coming apart. */
    private IntPredicate holed = part -> false;

    public PlannedBuild(final BuildPlan plan, final BlockPos origin) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.origin = origin.immutable();
        this.spotsOf = plan.byPart();
        final List<Set<Integer>> reaches = new ArrayList<>();
        for (int part = 0; part < plan.parts(); part++) {
            reaches.add(new TreeSet<>());
        }
        for (int i = 0; i < plan.spots().size(); i++) {
            final PlanSpot spot = plan.spots().get(i);
            this.byPlace.put(where(spot).asLong(), i);
            for (final PlanReach reach : spot.reaches()) {
                reaches.get(reach.part()).add(i);
            }
        }
        this.reachesOf = reaches.stream().map(Set::copyOf).toList();
        this.present = new boolean[plan.spots().size()];
        this.missing = new int[plan.parts()];
        this.formed = new boolean[plan.parts()];
        // Layer by layer from the ground, and in a layer in the plan's own order.
        this.order = IntStream.range(0, plan.spots().size()).boxed()
                .sorted(Comparator.<Integer>comparingInt(i -> plan.spots().get(i).at().getY())
                        .thenComparingInt(i -> i))
                .mapToInt(Integer::intValue).toArray();
        this.rank = new int[this.order.length];
        for (int at = 0; at < this.order.length; at++) {
            this.rank[this.order[at]] = at;
        }
    }

    /**
     * Has its formed parts hold when one of their blocks goes, as a hull does, rather than come apart: the block's
     * place leaves its model and its shape and the hole is torn round it ({@code FormedDamage}), and the block of the
     * right kind put back there merges again and mends it. Before it starts.
     *
     * @return this
     */
    public PlannedBuild holed() {
        return holed(part -> true);
    }

    /**
     * Has the formed parts {@code which} says hold when holed, as {@link #holed()} has every part, the others coming
     * apart as ever: a rocket's parts on the launcher that came apart for its kit. Before it starts.
     *
     * @return this
     */
    public PlannedBuild holed(final IntPredicate which) {
        this.holed = Objects.requireNonNull(which, "which");
        return this;
    }

    /**
     * Starts building in {@code in} for the owner at {@code by}: counts what is in place, forms the parts already
     * whole and gives back the blocks of any part that is not, then watches its places; {@code onChange} runs each
     * time a count changes, for the owner to tell its players. A part that holds when holed and stands formed still,
     * missing blocks or not, stays formed.
     */
    public void start(final ServerLevel in, final BlockPos by, final Runnable onChange) {
        this.level = in;
        this.owner = by.immutable();
        this.told = Objects.requireNonNull(onChange, "onChange");
        Arrays.fill(this.missing, 0);
        this.wanted.clear();
        for (int i = 0; i < this.plan.spots().size(); i++) {
            final PlanSpot spot = this.plan.spots().get(i);
            this.present[i] = matches(spot);
            if (!this.present[i]) {
                this.missing[spot.part()]++;
                this.wanted.merge(spot.block(), 1, Integer::sum);
            }
        }
        this.first = 0;
        skipPresent();
        for (int part = 0; part < this.plan.parts(); part++) {
            if (this.missing[part] == 0) {
                form(part);
            } else if (this.holed.test(part) && standsFormed(part)) {
                this.formed[part] = true;
            } else {
                this.formed[part] = true;
                comeApart(part);
            }
        }
        watch();
        this.told.run();
    }

    /**
     * Stops building: its places are not watched any more, and, when {@code giveBack}, every formed part comes apart,
     * its blocks back as they were; without it, as its owner's chunk unloads, they stay as they are, for when it
     * starts again.
     */
    public void stop(final boolean giveBack) {
        final ServerLevel in = this.level;
        if (in == null || this.owner == null) {
            return;
        }
        LevelStructures.of(in).forget(this.owner);
        if (giveBack) {
            for (int part = 0; part < this.plan.parts(); part++) {
                comeApart(part);
            }
        }
        this.level = null;
    }

    /**
     * Hands the plan to the owner at {@code by}: its formed blocks are told whose they are now, and its places are
     * watched for that owner, as a rocket's plan goes with it into its flight computer when it leaves its hangar.
     */
    public void handTo(final BlockPos by) {
        final ServerLevel in = this.level;
        if (in == null || this.owner == null) {
            return;
        }
        LevelStructures.of(in).forget(this.owner);
        this.owner = by.immutable();
        for (final PlanSpot spot : this.plan.spots()) {
            if (in.getBlockEntity(where(spot)) instanceof FormedPartBlockEntity part && part.planned()) {
                FormedParts.ownedBy(in, where(spot), this.owner);
            }
        }
        watch();
    }

    @Override
    public void changed() {
        // Told place by place, it has nothing to look at when told only that something changed.
    }

    /* One of its places changed: its part's count goes up or down by one, and the part forms or comes apart. */
    @Override
    public void changed(final BlockPos pos) {
        final Integer index = this.byPlace.get(pos.asLong());
        if (index == null || this.level == null) {
            return;
        }
        final PlanSpot spot = this.plan.spots().get(index);
        final boolean now = matches(spot);
        if (now == this.present[index]) {
            return;
        }
        this.present[index] = now;
        final int part = spot.part();
        if (now) {
            this.missing[part]--;
            this.wanted.computeIfPresent(spot.block(), (block, count) -> count > 1 ? count - 1 : null);
            skipPresent();
            if (this.holed.test(part) && this.formed[part]) {
                // A block put back into a part that held: it merges again, and the hole is mended.
                FormedDamage.mend(this.level, pos);
            } else if (this.missing[part] == 0) {
                form(part);
            }
        } else {
            this.missing[part]++;
            this.wanted.merge(spot.block(), 1, Integer::sum);
            this.first = Math.min(this.first, this.rank[index]);
            if (!this.holed.test(part)) {
                comeApart(part);
            }
        }
        this.told.run();
    }

    @Override
    public boolean holds(final long pos) {
        final Integer index = this.byPlace.get(pos);
        return index != null && this.formed[this.plan.spots().get(index).part()];
    }

    /** The plan. */
    public BuildPlan plan() {
        return this.plan;
    }

    /** Where the plan's origin is in its world. */
    public BlockPos origin() {
        return this.origin;
    }

    /** The owner it builds for now, if it has started. */
    public Optional<BlockPos> owner() {
        return Optional.ofNullable(this.owner);
    }

    /** Where the place {@code spot} is in its world. */
    public BlockPos where(final PlanSpot spot) {
        return this.origin.offset(spot.at());
    }

    /** The place of the plan at {@code pos}, if it has one there. */
    public Optional<PlanSpot> spotAt(final BlockPos pos) {
        final Integer index = this.byPlace.get(pos.asLong());
        return index == null ? Optional.empty() : Optional.of(this.plan.spots().get(index));
    }

    /** Whether the block wanted at the place numbered {@code spot} is there, as last counted. */
    public boolean present(final int spot) {
        return this.present[spot];
    }

    /** How many blocks of the part {@code part} are missing. */
    public int missing(final int part) {
        return this.missing[part];
    }

    /** How many blocks of the whole plan are missing. */
    public int missing() {
        int all = 0;
        for (final int count : this.missing) {
            all += count;
        }
        return all;
    }

    /** Whether the part {@code part} is formed. */
    public boolean formed(final int part) {
        return this.formed[part];
    }

    /** The parts formed, by their numbers. */
    public List<Integer> formedParts() {
        final List<Integer> out = new ArrayList<>();
        for (int part = 0; part < this.formed.length; part++) {
            if (this.formed[part]) {
                out.add(part);
            }
        }
        return List.copyOf(out);
    }

    /** Whether the whole plan is built, every part formed. */
    public boolean complete() {
        return missing() == 0;
    }

    /** How many places of the plan still want the block {@code block}, as last counted. */
    public int wanted(final ResourceLocation block) {
        return this.wanted.getOrDefault(block, 0);
    }

    /**
     * Every block the plan still wants and how many of each, as last counted, in the order the places wanting them
     * come to be laid: what a crane waits for first comes first.
     */
    public Map<ResourceLocation, Integer> wanted() {
        final Map<ResourceLocation, Integer> out = new LinkedHashMap<>();
        for (int at = this.first; at < this.order.length && out.size() < this.wanted.size(); at++) {
            final int index = this.order[at];
            if (!this.present[index]) {
                final ResourceLocation block = this.plan.spots().get(index).block();
                out.putIfAbsent(block, this.wanted.getOrDefault(block, 0));
            }
        }
        return out;
    }

    /**
     * The first missing place, the lowest first and layer by layer, that {@code can} lay now (it holds the block, and
     * nothing stands in the way), if there is one.
     */
    public Optional<PlanSpot> next(final Predicate<PlanSpot> can) {
        for (int at = this.first; at < this.order.length; at++) {
            final int index = this.order[at];
            if (!this.present[index] && can.test(this.plan.spots().get(index))) {
                return Optional.of(this.plan.spots().get(index));
            }
        }
        return Optional.empty();
    }

    /* Moves the first missing place on past the places that are present. */
    private void skipPresent() {
        while (this.first < this.order.length && this.present[this.order[this.first]]) {
            this.first++;
        }
    }

    /* Watches its places, under its owner, in the world's index of structures. */
    private void watch() {
        final ServerLevel in = this.level;
        if (in == null || this.owner == null || this.plan.spots().isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final PlanSpot spot : this.plan.spots()) {
            final BlockPos at = where(spot);
            minX = Math.min(minX, at.getX());
            minY = Math.min(minY, at.getY());
            minZ = Math.min(minZ, at.getZ());
            maxX = Math.max(maxX, at.getX());
            maxY = Math.max(maxY, at.getY());
            maxZ = Math.max(maxZ, at.getZ());
        }
        LevelStructures.of(in).watch(this.owner, new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ),
                this);
    }

    /*
     * Whether the block wanted at {@code spot} is there: the block itself, or that block merged into a formed part of
     * this plan.
     */
    private boolean matches(final PlanSpot spot) {
        final ServerLevel in = this.level;
        if (in == null) {
            return false;
        }
        final BlockPos at = where(spot);
        final BlockState state = in.getBlockState(at);
        final BlockState was = FormedPartBlock.originalAt(in, at, state);
        return BuiltInRegistries.BLOCK.getKey(was.getBlock()).equals(spot.block());
    }

    /*
     * Forms the part {@code part}: each of its blocks merges into its model, shaped too by the formed models reaching
     * into it; one that keeps something of its own is told; and the formed parts it reaches into take its shape.
     */
    private void form(final int part) {
        final ServerLevel in = this.level;
        if (in == null || this.owner == null) {
            return;
        }
        this.formed[part] = true;
        for (final int index : this.spotsOf.get(part)) {
            final PlanSpot spot = this.plan.spots().get(index);
            final BlockPos at = where(spot);
            // A block that keeps itself is told, whether or not it keeps a block entity; the others merge.
            if (in.getBlockState(at).getBlock() instanceof IPlanMember member) {
                member.partFormed(in, at, true);
            } else if (!FormedParts.lay(in, this.owner, at, spot.place().orElse(null), reaching(spot))) {
                FormedParts.reach(in, at, reaching(spot));
            }
        }
        reshape(part);
    }

    /* Whether the blocks of the part {@code part} standing in their places have merged, as a formed part's have. */
    private boolean standsFormed(final int part) {
        final ServerLevel in = this.level;
        boolean any = false;
        for (final int index : this.spotsOf.get(part)) {
            if (!this.present[index] || in == null) {
                continue;
            }
            final BlockPos at = where(this.plan.spots().get(index));
            if (in.getBlockState(at).getBlock() instanceof FormedPartBlock) {
                any = true;
            } else if (!(in.getBlockState(at).getBlock() instanceof IPlanMember)) {
                return false;
            }
        }
        return any;
    }

    /* The part {@code part} comes apart: its merged blocks are given back, and the parts it reached into lose it. */
    private void comeApart(final int part) {
        final ServerLevel in = this.level;
        if (in == null || !this.formed[part]) {
            return;
        }
        this.formed[part] = false;
        for (final int index : this.spotsOf.get(part)) {
            final BlockPos at = where(this.plan.spots().get(index));
            final BlockState state = in.getBlockState(at);
            if (state.getBlock() instanceof FormedPartBlock && in.getBlockEntity(at)
                    instanceof FormedPartBlockEntity merged && merged.planned()) {
                FormedParts.restoreAt(in, at);
            } else if (state.getBlock() instanceof IPlanMember member) {
                member.partFormed(in, at, false);
            }
        }
        reshape(part);
    }

    /* The formed parts {@code part} reaches into take its shape, or lose it, as it stands now. */
    private void reshape(final int part) {
        final ServerLevel in = this.level;
        if (in == null) {
            return;
        }
        for (final int index : this.reachesOf.get(part)) {
            final PlanSpot spot = this.plan.spots().get(index);
            if (this.formed[spot.part()]) {
                FormedParts.reach(in, where(spot), reaching(spot));
            }
        }
    }

    /* The blocks of the formed models of other formed parts reaching into {@code spot}. */
    private List<FormedPlace> reaching(final PlanSpot spot) {
        final List<FormedPlace> out = new ArrayList<>();
        for (final PlanReach reach : spot.reaches()) {
            if (this.formed[reach.part()]) {
                out.add(reach.place());
            }
        }
        return List.copyOf(out);
    }
}
