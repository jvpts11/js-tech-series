/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The game's own small blocks a block space takes: torches, levers, buttons and pressure plates, from any mod that
 * makes them of the game's kinds. Each hangs on the face of the space toward the block it is attached to, as it would
 * in a space of its own, and is placed by using its item on that block's face where a cable or a space of parts
 * stands. They are the game's own blocks run at the space's place: their shape, signal, light and particles are their
 * own, a lever is pulled and a button pressed when used, a pressure plate pressed by what stands on it, and a piece
 * whose block beside it goes falls off. A pressed button or plate is looked at again on the space's tick, which it has
 * only while one is pressed.
 */
public final class SmallBlocks {

    private static final float PIXELS = PartBox.SIDE;
    private static final float LEVER_ON = 0.6F;
    private static final float LEVER_OFF = 0.5F;
    private static final float LEVER_VOLUME = 0.3F;

    private SmallBlocks() {
    }

    /** Hands the game's bus the placing of small blocks into spaces, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(UseItemOnBlockEvent.class, SmallBlocks::onUseItemOnBlock);
    }

    /** Whether a space takes {@code block}: a torch, a lever, a button or a pressure plate. */
    public static boolean takes(final Block block) {
        return block instanceof TorchBlock || block instanceof LeverBlock || block instanceof ButtonBlock
                || block instanceof BasePressurePlateBlock;
    }

    /** The face of the space the small block in {@code state} hangs on: the side toward what holds it up. */
    public static Direction faceOf(final BlockState state) {
        if (state.getBlock() instanceof WallTorchBlock) {
            return state.getValue(WallTorchBlock.FACING).getOpposite();
        }
        if (state.getBlock() instanceof FaceAttachedHorizontalDirectionalBlock) {
            return switch (state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE)) {
                case FLOOR -> Direction.DOWN;
                case CEILING -> Direction.UP;
                case WALL -> state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING).getOpposite();
            };
        }
        return Direction.DOWN;
    }

    /** Its shape, as the game's own block has it. */
    public static VoxelShape shapeOf(final BlockState state) {
        return state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** The room it takes, in whole pixels, its shape rounded outward. */
    public static List<PartBox> boxesOf(final BlockState state) {
        final List<PartBox> out = new ArrayList<>();
        for (final AABB box : shapeOf(state).toAabbs()) {
            out.add(new PartBox(down(box.minX), down(box.minY), down(box.minZ), up(box.maxX), up(box.maxY),
                    up(box.maxZ)));
        }
        return out;
    }

    /** It as the fit sees it: in its face's place, kept clear of every other part and of what runs through. */
    public static SpaceFit.Piece pieceOf(final BlockState state) {
        return new SpaceFit.Piece(SpaceSlots.face(faceOf(state).get3DDataValue()), boxesOf(state), false, false);
    }

    /**
     * A player used the small block on {@code face} of {@code parts}, on the server: a lever is pulled, a button
     * pressed.
     *
     * @return whether it did anything
     */
    public static boolean use(final ServerLevel level, final SpaceParts parts, final Direction face,
                              @Nullable final Player player) {
        final BlockState state = parts.small(face);
        final BlockPos pos = parts.hostPos();
        if (state == null) {
            return false;
        }
        if (state.getBlock() instanceof LeverBlock) {
            final BlockState pulled = state.cycle(LeverBlock.POWERED);
            final boolean on = pulled.getValue(LeverBlock.POWERED);
            parts.setSmall(face, pulled, 0L);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, LEVER_VOLUME,
                    on ? LEVER_ON : LEVER_OFF);
            level.gameEvent(player, on ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
            return true;
        }
        if (state.getBlock() instanceof ButtonBlock button) {
            if (!state.getValue(ButtonBlock.POWERED)) {
                parts.setSmall(face, state.setValue(ButtonBlock.POWERED, true),
                        level.getGameTime() + button.ticksToStayPressed);
                level.playSound(null, pos, button.type.buttonClickOn(), SoundSource.BLOCKS);
                level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
            }
            return true;
        }
        return false;
    }

    /** {@code entity} stands in the space: on a pressure plate, or an arrow in a wooden button; on the server. */
    public static void entityInside(final Level level, final SpaceParts parts, final Entity entity) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        for (final Direction face : Direction.values()) {
            final BlockState state = parts.small(face);
            if (state == null) {
                continue;
            }
            if (state.getBlock() instanceof BasePressurePlateBlock && signalOf(state) == 0) {
                checkPlate(server, parts, face, state, entity);
            } else if (state.getBlock() instanceof ButtonBlock button && button.type.canButtonBeActivatedByArrows()
                    && !state.getValue(ButtonBlock.POWERED) && entity instanceof AbstractArrow) {
                checkButton(server, parts, face, state, button);
            }
        }
    }

    /** Looks again at each pressed button and plate whose time came, on the space's tick; on the server. */
    public static void tick(final ServerLevel level, final SpaceParts parts) {
        final long now = level.getGameTime();
        for (final Direction face : Direction.values()) {
            final BlockState state = parts.small(face);
            final long due = parts.smallDue(face);
            if (state == null || due == 0L || due > now) {
                continue;
            }
            if (state.getBlock() instanceof BasePressurePlateBlock) {
                checkPlate(level, parts, face, state, null);
            } else if (state.getBlock() instanceof ButtonBlock button) {
                checkButton(level, parts, face, state, button);
            }
        }
    }

    /** Takes off every small block whose block beside the space no longer holds it up, dropping it; on the server. */
    public static void checkHeld(final Level level, final SpaceParts parts) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        final BlockPos pos = parts.hostPos();
        for (final Direction face : Direction.values()) {
            final BlockState state = parts.small(face);
            if (state != null && !state.canSurvive(server, pos)) {
                parts.removeSmall(face);
                Block.popResource(server, pos, itemOf(state));
            }
        }
    }

    /** Shows each small block's own particles, a torch's flame and smoke, on the player's game. */
    public static void animateTick(final Level level, final SpaceParts parts, final RandomSource random) {
        for (final BlockState state : parts.smallBlocks()) {
            state.getBlock().animateTick(state, level, parts.hostPos(), random);
        }
    }

    /** Whether going from {@code was} to {@code now} changes what the small block gives its neighbours. */
    public static boolean signalChanged(final BlockState was, final BlockState now) {
        return signalOf(was) != signalOf(now);
    }

    /** The redstone strength the small block in {@code state} gives, 0 to 15. */
    public static int strengthOf(final BlockState state) {
        return signalOf(state);
    }

    /**
     * {@code state} as a wrench turns it: a lever or a button on a floor or a ceiling a quarter turn; none for the
     * rest, which hang one way only.
     */
    public static @Nullable BlockState turned(final BlockState state) {
        if (state.getBlock() instanceof FaceAttachedHorizontalDirectionalBlock
                && state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE) != AttachFace.WALL) {
            return state.rotate(Rotation.CLOCKWISE_90);
        }
        return null;
    }

    /** The item a small block gives back: a wall torch its torch. */
    public static ItemStack itemOf(final BlockState state) {
        return new ItemStack(state.getBlock());
    }

    /* The strength a small block gives, from its state alone. */
    private static int signalOf(final BlockState state) {
        return state.getSignal(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Direction.UP);
    }

    /*
     * A plate counts what stands on it, as the game's own does at the space's place, and keeps pressed and looked at
     * again while anything does.
     */
    private static void checkPlate(final ServerLevel level, final SpaceParts parts, final Direction face,
                                   final BlockState state, @Nullable final Entity entity) {
        final BasePressurePlateBlock plate = (BasePressurePlateBlock) state.getBlock();
        final BlockPos pos = parts.hostPos();
        final int was = signalOf(state);
        final int now = plate.getSignalStrength(level, pos);
        final BlockState next = withSignal(state, now);
        parts.setSmall(face, next, now > 0 ? level.getGameTime() + plate.getPressedTime() : 0L);
        if (now == 0 && was > 0) {
            level.playSound(null, pos, plate.type.pressurePlateClickOff(), SoundSource.BLOCKS);
            level.gameEvent(entity, GameEvent.BLOCK_DEACTIVATE, pos);
        } else if (now > 0 && was == 0) {
            level.playSound(null, pos, plate.type.pressurePlateClickOn(), SoundSource.BLOCKS);
            level.gameEvent(entity, GameEvent.BLOCK_ACTIVATE, pos);
        }
    }

    /* A button stays pressed while an arrow is in it, for a button an arrow presses; else it springs back. */
    private static void checkButton(final ServerLevel level, final SpaceParts parts, final Direction face,
                                    final BlockState state, final ButtonBlock button) {
        final BlockPos pos = parts.hostPos();
        final AbstractArrow arrow = button.type.canButtonBeActivatedByArrows()
                ? level.getEntitiesOfClass(AbstractArrow.class, shapeOf(state).bounds().move(pos)).stream()
                .findFirst().orElse(null) : null;
        final boolean pressed = arrow != null;
        final boolean was = state.getValue(ButtonBlock.POWERED);
        parts.setSmall(face, state.setValue(ButtonBlock.POWERED, pressed),
                pressed ? level.getGameTime() + button.ticksToStayPressed : 0L);
        if (pressed != was) {
            level.playSound(null, pos, pressed ? button.type.buttonClickOn() : button.type.buttonClickOff(),
                    SoundSource.BLOCKS);
            level.gameEvent(arrow, pressed ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
        }
    }

    /* A plate's state giving {@code strength}: pressed or not for the plain plates, its power for weighted ones. */
    private static BlockState withSignal(final BlockState state, final int strength) {
        if (state.hasProperty(BlockStateProperties.POWERED)) {
            return state.setValue(BlockStateProperties.POWERED, strength > 0);
        }
        return state.hasProperty(BlockStateProperties.POWER) ? state.setValue(BlockStateProperties.POWER, strength)
                : state;
    }

    /*
     * A small block's item used on the face of a block where a space stands beyond it: the block goes into the space,
     * placed as its item would place it, when it fits there. Only once the block used on has let the use pass.
     */
    private static void onUseItemOnBlock(final UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK
                || !(event.getItemStack().getItem() instanceof BlockItem item) || !takes(item.getBlock())) {
            return;
        }
        final BlockPlaceContext context = new BlockPlaceContext(event.getUseOnContext());
        final BlockPos target = context.getClickedPos();
        final Level level = event.getLevel();
        if (!(level.getBlockEntity(target) instanceof ISpaceHolder holder)) {
            return;
        }
        final BlockState state = item.getPlacementState(context);
        if (state == null || !takes(state.getBlock()) || !holder.parts().fitsSmall(state)) {
            return;
        }
        if (!level.isClientSide() && holder.parts().addSmall(state)) {
            final Player player = event.getPlayer();
            final SoundType sound = state.getSoundType(level, target, player);
            level.playSound(null, target, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Context.of(player, state));
            event.getItemStack().consume(1, player);
        }
        event.cancelWithResult(ItemInteractionResult.sidedSuccess(level.isClientSide()));
    }

    private static int down(final double blocks) {
        return (int) Math.floor(blocks * PIXELS + 1.0e-6);
    }

    private static int up(final double blocks) {
        return (int) Math.ceil(blocks * PIXELS - 1.0e-6);
    }
}
