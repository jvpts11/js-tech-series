/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.connect.Neighbours;
import dev.jstech.core.signal.ISignalOutput;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A part of the Core's kit of mechanical power: a shaft, a flywheel, a gear, a gearbox, a clutch, a brake, a speed
 * controller, a shear pin or a pulley ({@link MechanicalPartSpec}), set along the axis of the face it was placed
 * against, and its block entity of the type its mod registers. A sheared pin is pinned again by using it. Its model
 * and recipe are its mod's.
 *
 * <pre>{@code
 * BlockEntry<MechanicalPartBlock> IRON_SHAFT = CONTENT.block("iron_shaft", properties -> new MechanicalPartBlock(
 *         properties, MechanicalPartSpec.shaft(0.005, 0.1), () -> PART.get())).named("Iron Shaft").register();
 * BlockEntry<MechanicalPartBlock> GEAR = CONTENT.block("gear", properties -> MechanicalPartBlock.turning(
 *         properties, MechanicalPartSpec.gear(16, 0.05, 0.2), () -> PART.get())).named("Gear").register();
 * DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalPartBlockEntity>> PART = CONTENT.blockEntity("part",
 *         (pos, state) -> new MechanicalPartBlockEntity(PART.get(), pos, state), IRON_SHAFT, GEAR, GEARBOX);
 * }</pre>
 */
public class MechanicalPartBlock extends RotatedPillarBlock implements EntityBlock {

    private final MechanicalPartSpec spec;
    private final Supplier<? extends BlockEntityType<? extends MechanicalPartBlockEntity>> type;
    private final boolean turnsInSight;
    private @Nullable SoundKey sound;
    private double fullRpm = 1;

    /* Light passes a part drawn turning as it passes glass, its block's whole cube being no part of what is seen. */
    private static final float SEEN_THROUGH = 1.0F;

    public MechanicalPartBlock(final Properties properties, final MechanicalPartSpec spec,
                               final Supplier<? extends BlockEntityType<? extends MechanicalPartBlockEntity>> type) {
        this(properties, spec, type, false);
    }

    private MechanicalPartBlock(final Properties properties, final MechanicalPartSpec spec,
                                final Supplier<? extends BlockEntityType<? extends MechanicalPartBlockEntity>> type,
                                final boolean turnsInSight) {
        super(properties);
        this.spec = Objects.requireNonNull(spec, "spec");
        this.type = Objects.requireNonNull(type, "type");
        this.turnsInSight = turnsInSight;
    }

    /**
     * A part drawn turning, at its real speed, by the model its mod hands {@code TurningParts.draw(...)} on the
     * player's game, in place of its block's own model. Its block hides nothing behind it and lets light through,
     * as its model shows only the part, so the faces beside it are drawn and the part is lit as the place it stands
     * in is.
     */
    public static MechanicalPartBlock turning(final Properties properties, final MechanicalPartSpec spec,
                                              final Supplier<? extends BlockEntityType<
                                                      ? extends MechanicalPartBlockEntity>> type) {
        return new MechanicalPartBlock(properties.noOcclusion(), spec, type, true);
    }

    /** What the part is. */
    public MechanicalPartSpec spec() {
        return this.spec;
    }

    /** Whether it is drawn turning by a model, its block's own model left undrawn. */
    public boolean drawnTurning() {
        return this.turnsInSight;
    }

    /**
     * It is heard turning, {@code loop} running while it turns: higher and louder the faster, as loud as it gets and
     * a half higher than recorded at {@code full} revolutions a minute.
     */
    public MechanicalPartBlock turningSound(final SoundKey loop, final double full) {
        this.sound = Objects.requireNonNull(loop, "loop");
        this.fullRpm = Math.max(1, full);
        return this;
    }

    /** The sound it turns with, or none. */
    public @Nullable SoundKey sound() {
        return this.sound;
    }

    /** The speed it is loudest at, in revolutions a minute. */
    public double fullRpm() {
        return this.fullRpm;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return this.turnsInSight ? RenderShape.ENTITYBLOCK_ANIMATED : super.getRenderShape(state);
    }

    @Override
    protected boolean propagatesSkylightDown(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return this.turnsInSight || super.propagatesSkylightDown(state, level, pos);
    }

    @Override
    protected float getShadeBrightness(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return this.turnsInSight ? SEEN_THROUGH : super.getShadeBrightness(state, level, pos);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return this.type.get().create(pos, state);
    }

    /* The part hears which of its faces saw its neighbour change, so its network is joined again. */
    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighbourBlock, final BlockPos neighbourPos,
                                   final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbourBlock, neighbourPos, movedByPiston);
        final Direction face = Neighbours.faceTowards(pos, neighbourPos);
        if (face != null && level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            synced.fields().neighbourChanged(level, face);
        }
    }

    /* A comparator reads the part as a sensor: its speed, or the torque through a torque meter. */
    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ISignalOutput output ? output.strength() : 0;
    }

    /* A sheared pin is pinned again by hand, as a fuse is changed. */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MechanicalPartBlockEntity part && part.sheared()) {
            if (!level.isClientSide()) {
                part.repin();
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            synced.fields().broken(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
