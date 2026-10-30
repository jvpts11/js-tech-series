/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.core.multiblock.shape.MeshOutline;
import dev.jstech.core.multiblock.shape.MeshVoxels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Damage to formed models, on the ground and aboard a moving structure alike. A strike is laid where it struck
 * ({@link #strike}): the point and the way the surface faced there are taken into the model's own space, and every
 * block of the model the stamp reaches keeps the record of it ({@link CellMark}), saved with the block, carried with
 * it aboard a moving structure and sent to the players who see it. From those records the server and every player's
 * game work out each reached block's faces as the strike left them, from the model's own file, and the block collides,
 * is aimed at and is outlined by them; the player's game draws the whole model so. Taking the records away
 * ({@link #erase}) undoes all of it.
 *
 * <pre>{@code
 * // A meteoroid holes the hull where it hit: a stamp of the Core's, a tenth of a block across.
 * FormedDamage.strike(level, hit.getBlockPos(), DamageStamps.MICROMETEOROID, hit.getLocation(),
 *         Vec3.atLowerCornerOf(hit.getDirection().getNormal()), 0.1, level.random.nextLong());
 * }</pre>
 */
public final class FormedDamage {

    /* Counts each change of a block's damage a player's game has been told of, so what is drawn from it is redone. */
    private static final AtomicInteger CHANGES = new AtomicInteger();
    /* How many sets of marks are kept worked out, the most recently used. */
    private static final int KEPT = 256;
    private static final Map<Key, Result> DAMAGED = Collections.synchronizedMap(
            new LinkedHashMap<>(KEPT, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<Key, Result> eldest) {
                    return size() > KEPT;
                }
            });
    /* The marks of a model in one order however they were gathered, so the same marks make the same damage. */
    private static final Comparator<DamageMark> ORDER = Comparator.comparingLong(DamageMark::seed)
            .thenComparing(mark -> mark.stamp().toString()).thenComparingDouble(DamageMark::x)
            .thenComparingDouble(DamageMark::y).thenComparingDouble(DamageMark::z)
            .thenComparingDouble(DamageMark::size);
    private static final int AXES = 3;
    private static final double HALF = 0.5;

    private FormedDamage() {
    }

    /**
     * Lays the stamp {@code stamp} on the formed model the block at {@code pos} is drawn with, as it struck there: at
     * {@code at} in the world, the surface facing {@code facing}, {@code size} blocks across, by {@code seed}. On the
     * server; nothing on a player's game, nor for a block of no formed model or a stamp not declared.
     *
     * @return how many of the model's blocks the stamp reached, each now keeping it
     */
    public static int strike(final Level level, final BlockPos pos, final ResourceLocation stamp, final Vec3 at,
                             final Vec3 facing, final double size, final long seed) {
        final Struck struck = struck(level, pos);
        if (struck == null || DamageStamps.get(stamp).isEmpty() || !(size > 0) || facing.lengthSqr() == 0) {
            return 0;
        }
        final FormedPlace place = struck.place();
        final double[] local = new double[AXES];
        CellSpace.toModel(new double[] {at.x - pos.getX(), at.y - pos.getY(), at.z - pos.getZ()}, place.turn(),
                local);
        final double[] way = new double[AXES];
        CellSpace.toModel(new double[] {HALF + facing.x, HALF + facing.y, HALF + facing.z}, place.turn(), way);
        final double[] origin = struck.shape().origin();
        final DamageMark mark = new DamageMark(stamp, origin[0] + place.x() + local[0],
                origin[1] + place.y() + local[1], origin[2] + place.z() + local[2], (float) (way[0] - HALF),
                (float) (way[1] - HALF), (float) (way[2] - HALF), (float) size, seed);
        final Optional<Strike> strike = mark.strike();
        if (strike.isEmpty()) {
            return 0;
        }
        int marked = 0;
        for (final Map.Entry<Long, float[]> cell : struck.rest().entrySet()) {
            final long key = cell.getKey();
            if (reaches(strike.get(), cell.getValue(), corner(origin, key))
                    && level.getBlockEntity(blockOf(pos, place, key)) instanceof FormedPartBlockEntity reached
                    && holds(reached, place.model(), key)) {
                reached.mark(new CellMark(place.model(), MeshCells.x(key), MeshCells.y(key), MeshCells.z(key),
                        mark));
                marked++;
            }
        }
        return marked;
    }

    /**
     * Takes every strike away from the formed model the block at {@code pos} is drawn with, on the server: its blocks
     * stand as the model has them again.
     *
     * @return how many of its blocks had damage
     */
    public static int erase(final Level level, final BlockPos pos) {
        final Struck struck = struck(level, pos);
        if (struck == null) {
            return 0;
        }
        int erased = 0;
        for (final long key : struck.shape().cells().keySet()) {
            if (level.getBlockEntity(blockOf(pos, struck.place(), key)) instanceof FormedPartBlockEntity reached
                    && reached.unmark(struck.place().model())) {
                erased++;
            }
        }
        return erased;
    }

    /**
     * Every strike laid on the formed model the block at {@code pos} is drawn as {@code place} of, gathered from all
     * its blocks, in one order however they are gathered.
     */
    public static List<DamageMark> marks(final Level level, final BlockPos pos, final FormedPlace place) {
        final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
        if (shape.isEmpty()) {
            return List.of();
        }
        final Set<DamageMark> found = new LinkedHashSet<>();
        for (final long key : shape.get().cells().keySet()) {
            if (level.getBlockEntity(blockOf(pos, place, key)) instanceof FormedPartBlockEntity part) {
                for (final CellMark kept : part.damage()) {
                    if (kept.model().equals(place.model()) && MeshCells.key(kept.x(), kept.y(), kept.z()) == key) {
                        found.add(kept.mark());
                    }
                }
            }
        }
        return ordered(found);
    }

    /** The strikes of {@link #marks}, those whose stamps are declared. */
    public static List<Strike> strikes(final Level level, final BlockPos pos, final FormedPlace place) {
        return marks(level, pos, place).stream().map(DamageMark::strike).flatMap(Optional::stream).toList();
    }

    /**
     * The boxes of the block {@code place} as the strikes it keeps, {@code kept}, left its faces, in sixteenths as the
     * model stands facing north; none while it keeps none, or its model does not read.
     */
    public static Optional<int[]> boxes(final Level level, final FormedPlace place, final List<CellMark> kept) {
        final Result result = result(level, place, kept);
        if (result == null) {
            return Optional.empty();
        }
        final float[] faces = result.faces.get(place.cell());
        return faces == null ? Optional.empty()
                : Optional.of(result.boxes.computeIfAbsent(place.cell(), cell -> MeshVoxels.boxes(faces)).clone());
    }

    /**
     * The faces of the merged block at {@code pos}, as the block {@code place}, as the strikes it keeps left them, in
     * its own space, nine numbers to a triangle; none while it keeps none.
     */
    public static Optional<float[]> triangles(final Level level, final BlockPos pos, final FormedPlace place) {
        if (!(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)) {
            return Optional.empty();
        }
        final Result result = result(level, place, part.damage());
        return result == null ? Optional.empty() : Optional.ofNullable(result.faces.get(place.cell()));
    }

    /**
     * The lines of the outline of the merged block at {@code pos}, as the block {@code place}, as the strikes it keeps
     * left its faces, in its own space, six numbers to a line; none while it keeps none.
     */
    public static Optional<float[]> lines(final Level level, final BlockPos pos, final FormedPlace place) {
        if (!(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)) {
            return Optional.empty();
        }
        final Result result = result(level, place, part.damage());
        if (result == null) {
            return Optional.empty();
        }
        final float[] faces = result.faces.get(place.cell());
        return faces == null ? Optional.empty()
                : Optional.of(result.lines.computeIfAbsent(place.cell(), cell -> MeshOutline.lines(faces)));
    }

    /** How many times a player's game has been told of a block's damage changing: what is drawn from it is redone. */
    public static int changes() {
        return CHANGES.get();
    }

    /** Tells that a block's damage changed on a player's game. */
    @ApiStatus.Internal
    public static void changed() {
        CHANGES.incrementAndGet();
    }

    /* The strikes {@code kept} by a block laid on the block {@code place}, worked out; none for none. */
    private static @Nullable Result result(final Level level, final FormedPlace place, final List<CellMark> kept) {
        final List<DamageMark> marks = new ArrayList<>();
        for (final CellMark mark : kept) {
            if (mark.on(place)) {
                marks.add(mark.mark());
            }
        }
        if (marks.isEmpty()) {
            return null;
        }
        final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
        if (shape.isEmpty()) {
            return null;
        }
        final Optional<Map<Long, float[]>> rest = FormedFaces.of(place.model(), shape.get());
        if (rest.isEmpty()) {
            return null;
        }
        final Key key = new Key(place.model(), shape.get(), ordered(marks));
        final Result known = DAMAGED.get(key);
        if (known != null) {
            return known;
        }
        final Result made = deform(rest.get(), shape.get().origin(), key.marks());
        DAMAGED.put(key, made);
        return made;
    }

    /* The faces of each block {@code marks} reach, as they left them, each in its block's own space. */
    private static Result deform(final Map<Long, float[]> rest, final double[] origin, final List<DamageMark> marks) {
        final List<Strike> strikes = marks.stream().map(DamageMark::strike).flatMap(Optional::stream).toList();
        final List<Long> reached = new ArrayList<>();
        int numbers = 0;
        for (final Map.Entry<Long, float[]> cell : rest.entrySet()) {
            for (final Strike strike : strikes) {
                if (reaches(strike, cell.getValue(), corner(origin, cell.getKey()))) {
                    reached.add(cell.getKey());
                    numbers += cell.getValue().length;
                    break;
                }
            }
        }
        // The reached blocks' faces in the model's space, each triangle tagged with its block, struck together.
        final float[] corners = new float[numbers];
        final int[] tags = new int[numbers / (AXES * AXES)];
        int at = 0;
        for (int i = 0; i < reached.size(); i++) {
            final float[] pieces = rest.get(reached.get(i));
            final double[] corner = corner(origin, reached.get(i));
            for (int n = 0; n < pieces.length; n++) {
                corners[at + n] = (float) (pieces[n] + corner[n % AXES]);
            }
            Arrays.fill(tags, at / (AXES * AXES), (at + pieces.length) / (AXES * AXES), i);
            at += pieces.length;
        }
        final MeshDamage.Damaged damaged = MeshDamage.apply(corners, AXES, tags, strikes);
        final int[] counts = new int[reached.size()];
        for (final int tag : damaged.tags()) {
            counts[tag]++;
        }
        final float[][] per = new float[reached.size()][];
        final int[] filled = new int[reached.size()];
        for (int i = 0; i < reached.size(); i++) {
            per[i] = new float[counts[i] * AXES * AXES];
        }
        final float[] all = damaged.corners();
        for (int t = 0; t < damaged.triangles(); t++) {
            final int tag = damaged.tags()[t];
            final double[] corner = corner(origin, reached.get(tag));
            for (int n = 0; n < AXES * AXES; n++) {
                per[tag][filled[tag]++] = (float) (all[t * AXES * AXES + n] - corner[n % AXES]);
            }
        }
        final Map<Long, float[]> faces = new HashMap<>();
        for (int i = 0; i < reached.size(); i++) {
            faces.put(reached.get(i), per[i]);
        }
        return new Result(faces);
    }

    /* Whether {@code strike} reaches any face {@code pieces} of a block whose lowest corner is at {@code corner}. */
    private static boolean reaches(final Strike strike, final float[] pieces, final double[] corner) {
        // The strike taken into the block's own space, where its faces are.
        final Strike local = new Strike(strike.stamp(), strike.x() - corner[0], strike.y() - corner[1],
                strike.z() - corner[2], strike.nx(), strike.ny(), strike.nz(), strike.size(), strike.seed());
        for (int first = 0; first + AXES * AXES <= pieces.length; first += AXES * AXES) {
            if (MeshDamage.reaches(local, pieces, AXES, first)) {
                return true;
            }
        }
        return false;
    }

    /* Where the lowest corner of the block {@code key} is in the model's space. */
    private static double[] corner(final double[] origin, final long key) {
        return new double[] {origin[0] + MeshCells.x(key), origin[1] + MeshCells.y(key), origin[2] + MeshCells.z(key)};
    }

    /* Where in the world the block {@code key} of the model stands, the block at {@code pos} being {@code place}. */
    private static BlockPos blockOf(final BlockPos pos, final FormedPlace place, final long key) {
        final double[] local = {MeshCells.x(key) - place.x() + HALF, MeshCells.y(key) - place.y() + HALF,
                MeshCells.z(key) - place.z() + HALF};
        final double[] world = new double[AXES];
        CellSpace.toWorld(local, place.turn(), world);
        return pos.offset((int) Math.round(world[0] - HALF), (int) Math.round(world[1] - HALF),
                (int) Math.round(world[2] - HALF));
    }

    /* Whether {@code part} is the block {@code key} of the model {@code model}. */
    private static boolean holds(final FormedPartBlockEntity part, final ResourceLocation model, final long key) {
        for (final FormedPlace place : part.places()) {
            if (place.model().equals(model) && place.cell() == key) {
                return true;
            }
        }
        return false;
    }

    /* The struck block's place in its model, the model's shape file and its faces at rest; none for a plain block. */
    private static @Nullable Struck struck(final Level level, final BlockPos pos) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)
                || part.places().isEmpty()) {
            return null;
        }
        final FormedPlace place = part.places().getFirst();
        final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
        if (shape.isEmpty()) {
            return null;
        }
        final Optional<Map<Long, float[]>> rest = FormedFaces.of(place.model(), shape.get());
        return rest.map(faces -> new Struck(place, shape.get(), faces)).orElse(null);
    }

    private static List<DamageMark> ordered(final Collection<DamageMark> marks) {
        return marks.stream().distinct().sorted(ORDER).toList();
    }

    /* A struck block's place, its model's shape file, and the model's faces at rest. */
    private record Struck(FormedPlace place, FormedShape shape, Map<Long, float[]> rest) {

        Struck {
            rest = Map.copyOf(rest);
        }
    }

    /* A model, its shape file and a set of marks laid on it, in order. */
    private record Key(ResourceLocation model, FormedShape shape, List<DamageMark> marks) {

        Key {
            marks = List.copyOf(marks);
        }
    }

    /* The faces of each block a set of marks reached, as they left them, and the boxes and outlines worked from them. */
    private static final class Result {

        private final Map<Long, float[]> faces;
        private final Map<Long, int[]> boxes = new ConcurrentHashMap<>();
        private final Map<Long, float[]> lines = new ConcurrentHashMap<>();

        private Result(final Map<Long, float[]> faces) {
            this.faces = Map.copyOf(faces);
        }
    }
}
