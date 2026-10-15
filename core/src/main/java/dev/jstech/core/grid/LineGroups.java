/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableParts;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.log.CoreLogs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * The groups of a line that carries nothing but groups the machines it joins, in one dimension: each connected run is
 * one group, and its machines work together as their mod says (J's Industrial's research computers on a Research Link
 * Cable, J's Space's ground stations on its telemetry line). A line's wires plug into any block that offers
 * {@link #MEMBER} on the face they meet. A dyed run is a group of its own beside a run of another colour.
 *
 * <p>The groups are worked out again only when the line's shape changes or a wire comes to meet a machine; then every
 * group that is new or whose machines changed is told to the listeners of its grid and to its members, and every
 * group gone is told to the listeners as dissolved.
 *
 * <pre>{@code
 * public static final GridKind RESEARCH = GridKind.declare(rl("research"), GridPolicies.GROUP);
 * LineGroups.listen(RESEARCH, new ResearchPools());
 * }</pre>
 */
public final class LineGroups implements IGridMover {

    private final GridKind kind;
    private final List<LineGroup> groups = new ArrayList<>();
    private final Map<Long, LineGroup> byMember = new HashMap<>();
    private long builtVersion = -1L;
    private long builtTaps = -1L;
    private long taps;

    /** What a machine a group line plugs into offers: it hears of the group it is in. */
    public static final BlockCapability<IGroupMember, @Nullable Direction> MEMBER = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "group_member"), IGroupMember.class);
    /** What the debug screen calls the time the group lines take to work out their groups. */
    public static final ResourceLocation TIMING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "line_groups");
    /** The work of working out the groups again, timed, named in profilers and watched for stalls. */
    public static final Section SECTION = Section.builder(TIMING, CoreLogs.GROUPS).declare();

    private static final Map<GridKind, List<IGroupListener>> LISTENERS = new ConcurrentHashMap<>();

    /** The groups of the line {@code kind}, one for each dimension. */
    public LineGroups(final GridKind kind) {
        this.kind = kind;
    }

    /** Has {@code listener} hear of every group of the line {@code kind} that is new, changed or dissolved. */
    public static void listen(final GridKind kind, final IGroupListener listener) {
        LISTENERS.computeIfAbsent(kind, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /** The groups of the line {@code kind} in {@code level}, as last worked out; none for a line that does not group. */
    public static Optional<LineGroups> of(final ServerLevel level, final GridKind kind) {
        return CoreGrids.mover(level, kind).filter(LineGroups.class::isInstance).map(LineGroups.class::cast);
    }

    /** Whether a group line plugs into the block at {@code pos}, met on its {@code face}: it offers {@link #MEMBER}. */
    public static boolean plugsInto(final ServerLevel level, final BlockPos pos, final Direction face) {
        return level.getCapability(MEMBER, pos, face) != null;
    }

    /** Every group, as last worked out. */
    public List<LineGroup> groups() {
        return List.copyOf(this.groups);
    }

    /** The group the machine at {@code pos} is in, as last worked out. */
    public Optional<LineGroup> groupOf(final BlockPos pos) {
        return Optional.ofNullable(this.byMember.get(pos.asLong()));
    }

    @Override
    public void tapsChanged() {
        this.taps++;
    }

    @Override
    public void tick(final ServerLevel level) {
        final Grid grid = CoreGrids.of(level, this.kind);
        if (grid.version() == this.builtVersion && this.taps == this.builtTaps) {
            return;
        }
        this.builtVersion = grid.version();
        this.builtTaps = this.taps;
        SECTION.run(() -> rebuild(level, grid));
    }

    /* The groups worked out again, and whoever listens told of each that is new, changed or gone. */
    private void rebuild(final ServerLevel level, final Grid grid) {
        final List<LineGroup> before = List.copyOf(this.groups);
        final Map<Long, LineGroup> memberBefore = new HashMap<>(this.byMember);
        this.groups.clear();
        this.byMember.clear();
        if (grid.size() > 0) {
            for (final CableParts.Part part : CableParts.of(level, grid)) {
                final Set<BlockPos> members = new TreeSet<>(Comparator.comparingLong(BlockPos::asLong));
                for (final CableParts.Plug plug : part.plugs()) {
                    members.add(plug.at().immutable());
                }
                int colour = GridMember.NO_COLOUR;
                for (final CableParts.WireAt wire : part.wires()) {
                    final int dyed = grid.memberOf(wire.number()).colour();
                    if (dyed != GridMember.NO_COLOUR) {
                        colour = dyed;
                        break;
                    }
                }
                final LineGroup group = new LineGroup(this.kind, colour, new ArrayList<>(members));
                this.groups.add(group);
                for (final BlockPos member : members) {
                    this.byMember.put(member.asLong(), group);
                }
            }
        }
        tell(level, before, memberBefore);
    }

    private void tell(final ServerLevel level, final List<LineGroup> before, final Map<Long, LineGroup> memberBefore) {
        final List<IGroupListener> listeners = LISTENERS.getOrDefault(this.kind, List.of());
        final Set<LineGroup> kept = new HashSet<>(before);
        for (final LineGroup group : this.groups) {
            if (!kept.remove(group)) {
                listeners.forEach(listener -> listener.changed(level, group));
            }
        }
        for (final LineGroup gone : kept) {
            if (!this.groups.contains(gone)) {
                listeners.forEach(listener -> listener.dissolved(level, gone));
            }
        }
        final Set<Long> touched = new HashSet<>(memberBefore.keySet());
        touched.addAll(this.byMember.keySet());
        for (final long at : touched) {
            final LineGroup was = memberBefore.get(at);
            final LineGroup now = this.byMember.get(at);
            if (now != null && now.equals(was)) {
                continue;
            }
            final BlockPos pos = BlockPos.of(at);
            if (!level.isLoaded(pos)) {
                continue;
            }
            final IGroupMember member = level.getCapability(MEMBER, pos, null);
            if (member == null) {
                continue;
            }
            if (now == null) {
                member.left(this.kind);
            } else {
                member.joined(now);
            }
        }
    }

    /** A machine a group line joins: it hears of the group it is in. */
    public interface IGroupMember {

        /** It is in {@code group} now, a new group or one whose machines changed. */
        void joined(LineGroup group);

        /** It is in no group of the line {@code kind} any more. */
        void left(GridKind kind);
    }

    /** What hears of the groups of a line. */
    public interface IGroupListener {

        /** {@code group} is new, or its machines changed. */
        void changed(ServerLevel level, LineGroup group);

        /** {@code group} is gone: its run was cut, joined to another, or taken up. */
        void dissolved(ServerLevel level, LineGroup group);
    }
}
