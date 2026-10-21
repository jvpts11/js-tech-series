/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.create;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import dev.jstech.core.moving.ForeignMover;
import dev.jstech.core.moving.ForeignMovers;
import dev.jstech.core.moving.IForeignKind;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.jetbrains.annotations.Nullable;

/**
 * Create's contraptions as the kit sees them: each one an obstacle that moves, its blocks at its own places, which no
 * level holds, placed in the world as the contraption's entity places them now. How fast one goes and turns is read
 * from where it stood as the last two ticks began, for each contraption met lately: a contraption knows only where it
 * stood a tick ago until its own entity ticks, and in a tick the kit's structures may move before or after it. A
 * contraption's blocks are in no region of the world, so the kit has none of them to refuse and no place of theirs to
 * ask the world about.
 */
final class CreateContraptions implements IForeignKind {

    /* The contraptions met lately, on the server, each with where it stood as the last two ticks began. */
    private final Map<AbstractContraptionEntity, Watch> watched = new ConcurrentHashMap<>();

    /** What a player calls the mod of these structures. */
    static final String NAME = "Create";
    /* How long a contraption met is watched after it was last met, in ticks. */
    private static final long WATCH_TICKS = 40;

    private CreateContraptions() {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void near(final Level level, final AABB box, final Consumer<ForeignMover> into) {
        for (final AbstractContraptionEntity entity : level.getEntitiesOfClass(AbstractContraptionEntity.class, box)) {
            final Contraption contraption = entity.getContraption();
            if (!entity.isAliveOrStale() || contraption == null || contraption.getBlocks().isEmpty()) {
                continue;
            }
            final AABB bounds = contraption.bounds;
            if (bounds == null) {
                continue;
            }
            final Box blocks = new Box((int) Math.floor(bounds.minX), (int) Math.floor(bounds.minY),
                    (int) Math.floor(bounds.minZ), (int) Math.ceil(bounds.maxX) - 1, (int) Math.ceil(bounds.maxY) - 1,
                    (int) Math.ceil(bounds.maxZ) - 1);
            final ContraptionBlocks reads = new ContraptionBlocks(contraption.getBlocks(), level);
            final StructureFrame now = frameOf(entity, 1f, false);
            final Watch watch = level.isClientSide() ? null : watched.computeIfAbsent(entity, met -> new Watch());
            if (watch != null) {
                watch.until = level.getGameTime() + WATCH_TICKS;
            }
            // Until two ticks of it are seen, how it goes is read from the entity, a tick ago and now.
            final ForeignMover moved = watch != null && watch.before != null && watch.last != null
                    ? ForeignMover.moved(NAME, watch.before, watch.last, blocks, reads, null)
                    : ForeignMover.moved(NAME, frameOf(entity, 0f, true), now, blocks, reads, null);
            // Its origin's speed where it stands now, as fast as it went there and turned on since.
            final StructurePose was = moved.frame().pose();
            final StructurePose is = now.pose();
            final Vec3 spin = moved.spin();
            final Vec3 speed = moved.speed().add(spin.cross(new Vec3(is.x() - was.x(), is.y() - was.y(),
                    is.z() - was.z())));
            into.accept(new ForeignMover(NAME, now, blocks, reads, null, speed, spin));
        }
    }

    @Override
    public boolean holds(final Level level, final BlockPos pos) {
        return false;
    }

    @Override
    public Vec3 inWorld(final Level level, final Vec3 place) {
        return place;
    }

    @Override
    public boolean isMover(final Entity entity) {
        return entity instanceof AbstractContraptionEntity;
    }

    /*
     * Hands the kit Create's contraptions to meet, and keeps Create from moving any block of the yard, where the kit's
     * structures keep theirs: a contraption made of them would tear them out of a structure that goes on standing.
     */
    static void start() {
        BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) ->
                YardLayout.isYardBlock(pos.getX()) ? CheckResult.FAIL : CheckResult.PASS);
        final CreateContraptions kind = new CreateContraptions();
        ForeignMovers.declare(kind);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Pre.class, kind::beforeTick);
    }

    /* As a tick begins, where each contraption met lately in the level stands; one met no more is let go. */
    private void beforeTick(final LevelTickEvent.Pre event) {
        final Level level = event.getLevel();
        if (watched.isEmpty() || level.isClientSide()) {
            return;
        }
        final long now = level.getGameTime();
        final Iterator<Map.Entry<AbstractContraptionEntity, Watch>> each = watched.entrySet().iterator();
        while (each.hasNext()) {
            final Map.Entry<AbstractContraptionEntity, Watch> entry = each.next();
            final AbstractContraptionEntity entity = entry.getKey();
            if (entity.level() != level) {
                continue;
            }
            final Watch watch = entry.getValue();
            if (entity.isRemoved() || now > watch.until) {
                each.remove();
                continue;
            }
            watch.before = watch.last;
            watch.last = frameOf(entity, 1f, false);
        }
    }

    /*
     * Where the entity places the contraption's own places, as a frame of the kit's turned about their origin: the
     * world's place of the origin, and the turn that carries each of the three axes where the entity carries it.
     */
    private static StructureFrame frameOf(final AbstractContraptionEntity entity, final float partialTicks,
                                          final boolean before) {
        final Vec3 origin = entity.toGlobalVector(Vec3.ZERO, partialTicks, before);
        final Vec3 east = entity.toGlobalVector(new Vec3(1, 0, 0), partialTicks, before).subtract(origin);
        final Vec3 up = entity.toGlobalVector(new Vec3(0, 1, 0), partialTicks, before).subtract(origin);
        final Vec3 south = entity.toGlobalVector(new Vec3(0, 0, 1), partialTicks, before).subtract(origin);
        final Quaterniond turn = new Matrix3d(east.x, east.y, east.z, up.x, up.y, up.z, south.x, south.y, south.z)
                .getNormalizedRotation(new Quaterniond());
        return new StructureFrame(new StructurePose(origin.x, origin.y, origin.z, turn.x, turn.y, turn.z, turn.w),
                0, 0, 0);
    }

    /* Where a contraption stood as the last two ticks began, and until when it is watched. */
    private static final class Watch {

        private @Nullable StructureFrame before;
        private @Nullable StructureFrame last;
        private long until;
    }

    /* A contraption's blocks at its own places, air where it has none, as heights go in {@code level}. */
    private static final class ContraptionBlocks implements BlockGetter {

        private final Map<BlockPos, StructureBlockInfo> blocks;
        private final Level level;

        ContraptionBlocks(final Map<BlockPos, StructureBlockInfo> blocks, final Level level) {
            this.blocks = blocks;
            this.level = level;
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(final BlockPos pos) {
            final StructureBlockInfo info = blocks.get(pos);
            return info == null ? Blocks.AIR.defaultBlockState() : info.state();
        }

        @Override
        public FluidState getFluidState(final BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return level.getMinBuildHeight();
        }
    }
}
