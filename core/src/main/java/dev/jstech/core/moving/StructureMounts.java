/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.uuid.StructureUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * Structures held by structures: a turret on a ship, a rotor on a helicopter, a hangar door, a shuttle docked to a
 * station ({@link StructureMount}). A structure held goes wherever its parent goes, each tick of its parent, and turns
 * on it by its own rule, in its parent's yard; held by a structure moving by its physics, it is part of its parent's
 * body, its mass, its engines and its blocks meeting the world all its parent's. When the block of the parent holding
 * it goes, or it is let go, it goes its own way, at the speed its place on the parent had.
 *
 * <pre>{@code
 * MovingStructures.mount(turret, ship, Optional.of(bearingBlock));
 * MovingStructures.move(turret, new SpinMotion(0, 1, 0, 0.5));   // turns on the ship, in the ship's yard
 * }</pre>
 */
public final class StructureMounts {

    /* Each level's held structures loaded, by their parent's id; on the server's thread. */
    private static final Map<Level, Map<StructureUuid, Set<MovingStructure>>> CHILDREN = new WeakHashMap<>();
    /* How deep structures are held in structures, at most: deeper ones are left where they are. */
    private static final int DEEPEST = 8;

    private StructureMounts() {
    }

    /**
     * Holds {@code child} by {@code parent} where it stands now, {@code anchor} the parent's block that holds it. A
     * child moving by its physics comes to rest on its parent, part of it. Refused, saying why, for a structure held
     * by itself, by one it holds, or by one not yet in the world.
     */
    public static StructureResult mount(final MovingStructure child, final MovingStructure parent,
                                        final Optional<BlockPos> anchor) {
        if (child == parent || holds(child, parent) || child.level() != parent.level()) {
            return StructureResult.refused(StructureTexts.MOUNT_LOOP.text());
        }
        if (!child.ready() || !parent.ready()) {
            return StructureResult.refused(StructureTexts.NOT_READY.text());
        }
        if (child.motion() instanceof PhysicsMotion) {
            child.move(null);
        }
        final StructurePose local = parent.frame().local(child.pose());
        unfile(child);
        child.mounted(new StructureMount(parent.id(), local, anchor));
        file(child);
        wakePhysics(parent);
        return StructureResult.done(child, child.blocks());
    }

    /**
     * Lets {@code child} go from its parent: it goes on at the speed its place on its parent had, by its physics when
     * it {@code falls}, or else at rest where it is.
     */
    public static StructureResult unmount(final MovingStructure child, final boolean falls) {
        final StructureMount held = child.mount();
        if (held == null) {
            return StructureResult.refused(StructureTexts.NOT_MOUNTED.text());
        }
        final MovingStructure parent = parentOf(child);
        unfile(child);
        child.mounted(null);
        file(child);
        if (falls) {
            child.move(goingAsHeld(child, parent));
        }
        if (parent != null) {
            wakePhysics(parent);
        }
        return StructureResult.done(child, child.blocks());
    }

    /** The loaded structures held by {@code parent}, in no order; none, cheaply, for a structure holding none. */
    public static List<MovingStructure> children(final MovingStructure parent) {
        final Map<StructureUuid, Set<MovingStructure>> level = CHILDREN.get(parent.level());
        if (level == null) {
            return List.of();
        }
        final Set<MovingStructure> held = level.get(parent.id());
        return held == null || held.isEmpty() ? List.of() : new ArrayList<>(held);
    }

    /** The structure holding {@code child}, loaded, or null. */
    public static @Nullable MovingStructure parentOf(final MovingStructure child) {
        final StructureMount held = child.mount();
        if (held == null || !(child.level() instanceof ServerLevel level)) {
            return null;
        }
        return MovingStructures.byId(level, held.parent());
    }

    /** The structure at the top of what holds {@code structure}: itself when nothing does. */
    public static MovingStructure rootOf(final MovingStructure structure) {
        MovingStructure at = structure;
        for (int depth = 0; depth < DEEPEST; depth++) {
            final MovingStructure up = parentOf(at);
            if (up == null) {
                return at;
            }
            at = up;
        }
        return at;
    }

    /* Files {@code child} under its parent, as it comes into its level or is held. */
    static void file(final MovingStructure child) {
        final StructureMount held = child.mount();
        if (held != null) {
            CHILDREN.computeIfAbsent(child.level(), any -> new HashMap<>())
                    .computeIfAbsent(held.parent(), any -> new LinkedHashSet<>()).add(child);
        }
    }

    /* Takes {@code child} out of the files, as it leaves its level or is let go. */
    static void unfile(final MovingStructure child) {
        final StructureMount held = child.mount();
        final Map<StructureUuid, Set<MovingStructure>> level = CHILDREN.get(child.level());
        if (held == null || level == null) {
            return;
        }
        final Set<MovingStructure> siblings = level.get(held.parent());
        if (siblings != null) {
            siblings.remove(child);
            if (siblings.isEmpty()) {
                level.remove(held.parent());
            }
        }
    }

    /*
     * Moves each structure {@code parent} holds to where the parent has it after its tick, each turned by its own rule
     * first; and each of those its own, as deep as they go.
     */
    static void drive(final ServerLevel level, final MovingStructure parent, final int depth) {
        if (depth >= DEEPEST) {
            return;
        }
        for (final MovingStructure child : children(parent)) {
            child.followParent(level, parent.frame(), depth);
        }
    }

    /*
     * The structure {@code parent}'s blocks moved by {@code (dx, dy, dz)} in its yard, to another plot: what it holds
     * in {@code level} stands where it stood on it, its place on it moved with them.
     */
    static void shift(final Level level, final StructureUuid parent, final int dx, final int dy, final int dz) {
        final Map<StructureUuid, Set<MovingStructure>> files = CHILDREN.get(level);
        final Set<MovingStructure> held = files == null ? null : files.get(parent);
        if (held == null) {
            return;
        }
        for (final MovingStructure child : held) {
            final StructureMount mount = child.mount();
            if (mount != null) {
                final StructurePose local = mount.local();
                child.mounted(new StructureMount(mount.parent(), local.movedTo(local.x() + dx, local.y() + dy,
                        local.z() + dz), mount.anchor().map(at -> at.offset(dx, dy, dz))));
            }
        }
    }

    /* The parent's block {@code pos} changed: what it held there, now gone, is let go, falling. */
    static void anchorChanged(final MovingStructure parent, final BlockPos pos, final boolean gone) {
        if (!gone) {
            return;
        }
        for (final MovingStructure child : children(parent)) {
            final StructureMount held = child.mount();
            if (held != null && held.anchor().isPresent() && held.anchor().get().equals(pos)) {
                unmount(child, true);
            }
        }
    }

    /* Whether {@code holder} holds {@code structure}, however deep. */
    private static boolean holds(final MovingStructure holder, final MovingStructure structure) {
        MovingStructure at = structure;
        for (int depth = 0; depth < DEEPEST; depth++) {
            final MovingStructure up = parentOf(at);
            if (up == null) {
                return false;
            }
            if (up == holder) {
                return true;
            }
            at = up;
        }
        return true;
    }

    /*
     * The physics a structure let go goes on with: the speed its middle had on its parent, the parent's body's when it
     * moves by its physics, else how its frame moved its last tick; turning as its parent turned.
     */
    private static PhysicsMotion goingAsHeld(final MovingStructure child, @Nullable final MovingStructure parent) {
        final double[] moved = child.velocity();
        final double[] turned = child.spin();
        final Vector3d velocity = new Vector3d(moved[0], moved[1], moved[2]).mul(StructurePhysics.TICKS);
        final Vector3d spin = new Vector3d(turned[0], turned[1], turned[2]).mul(StructurePhysics.TICKS);
        final StructurePhysics physics = parent == null ? null : parent.physics();
        if (physics != null && physics.active()) {
            final StructureFrame frame = child.frame();
            final double x = (child.bounds().minX() + child.bounds().maxX() + 1) / 2.0;
            final double y = (child.bounds().minY() + child.bounds().maxY() + 1) / 2.0;
            final double z = (child.bounds().minZ() + child.bounds().maxZ() + 1) / 2.0;
            final Vector3d middle = new Vector3d(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
            physics.body().velocityAt(middle, velocity);
            spin.set(physics.body().spin());
        }
        return new PhysicsMotion(velocity.x, velocity.y, velocity.z, spin.x, spin.y, spin.z, List.of());
    }

    /* Wakes a parent moving by its physics, for its body to take in what it holds now. */
    private static void wakePhysics(final MovingStructure parent) {
        final MovingStructure root = rootOf(parent);
        final StructurePhysics physics = root.physics();
        if (physics != null) {
            physics.wake();
        }
    }
}
