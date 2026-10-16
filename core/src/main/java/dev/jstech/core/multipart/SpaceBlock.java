/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A block space of parts alone, with no cable through it: its shape is its parts; using it uses the device looked
 * at; breaking it takes out the part looked at and leaves the rest, the block going with its last part. A part takes
 * as long to break as the block it was cut from would.
 *
 * <p>Its state says which of its faces a piece of an opaque block covers whole, so a neighbour's face behind one is
 * not drawn, and, as {@link PartStates} keeps them, the light its parts give and whether any needs its tick. The game
 * keeps what one block hides of another by their states alone, so what the space hides of its neighbours is in its
 * state, never read from its parts. Its redstone is its parts', face by face ({@link PartRedstone}).
 */
public final class SpaceBlock extends Block implements EntityBlock {

    public static final MapCodec<SpaceBlock> CODEC = simpleCodec(SpaceBlock::new);
    /** For each face, in the order of the game's directions, whether a piece of an opaque block covers it whole. */
    public static final List<BooleanProperty> COVERED = List.of(BooleanProperty.create("covered_down"),
            BooleanProperty.create("covered_up"), BooleanProperty.create("covered_north"),
            BooleanProperty.create("covered_south"), BooleanProperty.create("covered_west"),
            BooleanProperty.create("covered_east"));
    /* How long a device takes to break, as a cable's part does. */
    private static final float DEVICE_HARDNESS = 0.3F;
    /* The game's own rule: a block's hardness to a hand's mining speed, thirty to a block mined by hand. */
    private static final float BY_HAND = 30.0F;
    /* What hides a neighbour's face, for each set of covered faces: a thin plate on each. */
    private static final VoxelShape[] PLATES = plates();

    public SpaceBlock(final Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any().setValue(PartStates.LIGHT, 0)
                .setValue(PartStates.TICKING, false);
        for (final BooleanProperty covered : COVERED) {
            state = state.setValue(covered, false);
        }
        registerDefaultState(state);
    }

    /** The part {@code player} aims at in the space at {@code pos}, or none. */
    public static @Nullable SpaceParts.Hit aimOf(final BlockGetter level, final BlockPos pos, final Player player) {
        if (!(level.getBlockEntity(pos) instanceof ISpaceHolder holder)) {
            return null;
        }
        final Vec3 start = player.getEyePosition();
        final Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
        return holder.parts().aim(pos, start, end);
    }

    /**
     * What a strike on the space at {@code pos} takes out: the part aimed at, or none when the space holds fewer than
     * two parts, and then breaks whole as any block does.
     */
    public static @Nullable SpaceParts.Hit strikeAimOf(final BlockGetter level, final BlockPos pos,
                                                       final Player player) {
        if (!(level.getBlockEntity(pos) instanceof SpaceBlockEntity space) || space.parts().count() < 2) {
            return null;
        }
        return aimOf(level, pos, player);
    }

    /**
     * The item of the part in {@code slot} of {@code parts}: the microblock's, the small block's or the device's;
     * none for none.
     */
    public static ItemStack itemOf(final SpaceParts parts, final int slot) {
        final Microblock block = parts.microblock(slot);
        if (block != null) {
            return CoreSpaces.stack(block.piece());
        }
        if (SpaceSlots.isFace(slot)) {
            final Direction face = Direction.from3DDataValue(SpaceSlots.faceOf(slot));
            final BlockState small = parts.small(face);
            if (small != null) {
                return SmallBlocks.itemOf(small);
            }
            final IFacePart device = parts.get(face);
            if (device != null) {
                return device.partItem();
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Uses the device or the small block on {@code face} of {@code parts}, on the server: a device does what it does,
     * a lever is pulled, a button pressed.
     *
     * @return whether it did anything
     */
    public static boolean use(final ServerLevel level, final SpaceParts parts, final Direction face,
                              final ServerPlayer player) {
        final IFacePart device = parts.get(face);
        return device != null ? device.use(player) : SmallBlocks.use(level, parts, face, player);
    }

    /** Takes the space at {@code pos} away when no part is left in it. */
    public static void vacateIfEmpty(final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SpaceBlockEntity space && !space.parts().any()) {
            level.removeBlock(pos, false);
        }
    }

    /** {@code state} saying the faces in {@code covered}, a bit for each, are covered, and the rest are not. */
    public static BlockState covering(final BlockState state, final int covered) {
        BlockState next = state;
        for (int face = 0; face < COVERED.size(); face++) {
            next = next.setValue(COVERED.get(face), (covered & 1 << face) != 0);
        }
        return next;
    }

    /** The faces {@code state} says are covered, a bit for each in the order of the game's directions. */
    public static int coveredOf(final BlockState state) {
        int covered = 0;
        for (int face = 0; face < COVERED.size(); face++) {
            if (state.getValue(COVERED.get(face))) {
                covered |= 1 << face;
            }
        }
        return covered;
    }

    @Override
    protected MapCodec<SpaceBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COVERED.toArray(new BooleanProperty[0])).add(PartStates.LIGHT, PartStates.TICKING);
    }

    @Override
    protected VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return PLATES[coveredOf(state)];
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
                            final Direction direction) {
        return PartRedstone.signal(level, pos, direction);
    }

    @Override
    protected int getDirectSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final Direction direction) {
        return PartRedstone.directSignal(level, pos, direction);
    }

    @Override
    public boolean canConnectRedstone(final BlockState state, final BlockGetter level, final BlockPos pos,
                                      @Nullable final Direction direction) {
        return PartRedstone.joins(level, pos, direction);
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        return PartRedstone.comparator(level, pos);
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighbourBlock, final BlockPos neighbourPos,
                                   final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbourBlock, neighbourPos, movedByPiston);
        PartRedstone.neighbourChanged(level, pos, neighbourPos);
        // A small block whose block beside the space went falls off; a space left with nothing goes.
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SpaceBlockEntity space) {
            SmallBlocks.checkHeld(level, space.parts());
            vacateIfEmpty(level, pos);
        }
    }

    /* A space read from an older save whose state does not show its parts is set right a tick after it loads. */
    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos,
                        final RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SpaceBlockEntity space) {
            PartStates.show(level, pos, space.parts());
        }
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return level.getBlockEntity(pos) instanceof SpaceBlockEntity space ? space.parts().shape() : Shapes.empty();
    }

    /* A torch, a lever, a button or a plate in the space has no collision, as the game's own has none. */
    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        return level.getBlockEntity(pos) instanceof SpaceBlockEntity space ? space.parts().solidShape()
                : Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return true;
    }

    @Override
    protected void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (level.getBlockEntity(pos) instanceof SpaceBlockEntity space) {
            SmallBlocks.entityInside(level, space.parts(), entity);
        }
    }

    @Override
    public void animateTick(final BlockState state, final Level level, final BlockPos pos,
                            final RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SpaceBlockEntity space) {
            SmallBlocks.animateTick(level, space.parts(), random);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        final SpaceParts.Hit aimed = aimOf(level, pos, player);
        if (aimed == null || !SpaceSlots.isFace(aimed.slot())
                || !(level.getBlockEntity(pos) instanceof SpaceBlockEntity space)) {
            return InteractionResult.PASS;
        }
        final Direction face = Direction.from3DDataValue(SpaceSlots.faceOf(aimed.slot()));
        if (space.parts().type(face) == null && space.parts().small(face) == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return level instanceof ServerLevel server && player instanceof ServerPlayer user
                && use(server, space.parts(), face, user) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level,
                                       final BlockPos pos, final Player player) {
        final SpaceParts.Hit aimed = aimOf(level, pos, player);
        if (aimed == null || !(level.getBlockEntity(pos) instanceof SpaceBlockEntity space)) {
            return ItemStack.EMPTY;
        }
        return itemOf(space.parts(), aimed.slot());
    }

    /* Breaking a part takes as long as the block it was cut from; a device as long as a cable's part. */
    @Override
    protected float getDestroyProgress(final BlockState state, final Player player, final BlockGetter level,
                                       final BlockPos pos) {
        final SpaceParts.Hit aimed = aimOf(level, pos, player);
        final Microblock block = aimed == null || !(level.getBlockEntity(pos) instanceof SpaceBlockEntity space)
                ? null : space.parts().microblock(aimed.slot());
        if (block != null) {
            return block.piece().material().defaultBlockState().getDestroyProgress(player, level, pos);
        }
        final BlockState small = aimed == null || !SpaceSlots.isFace(aimed.slot())
                || !(level.getBlockEntity(pos) instanceof SpaceBlockEntity space) ? null
                : space.parts().small(Direction.from3DDataValue(SpaceSlots.faceOf(aimed.slot())));
        if (small != null) {
            return small.getDestroyProgress(player, level, pos);
        }
        return player.getDestroySpeed(state) / DEVICE_HARDNESS / BY_HAND;
    }

    /* The space gives back every part; its loot table gives nothing of its own. */
    @Override
    protected List<ItemStack> getDrops(final BlockState state, final LootParams.Builder params) {
        final BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return entity instanceof SpaceBlockEntity space ? space.parts().drops() : List.of();
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof SpaceBlockEntity space) {
            // Never void what the devices are holding, on any way the space goes.
            space.parts().dropContents(server);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new SpaceBlockEntity(pos, state);
    }

    /* Only a space whose parts need their tick is ticked, as its state says. */
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                            final BlockEntityType<T> type) {
        if (level.isClientSide() || !state.getValue(PartStates.TICKING)) {
            return null;
        }
        return BlockEntityTickers.create(type, CoreSpaces.BLOCK_ENTITY.get(), SpaceBlockEntity::serverTick);
    }

    private static VoxelShape[] plates() {
        final VoxelShape[] face = new VoxelShape[COVERED.size()];
        for (int f = 0; f < face.length; f++) {
            final PartBox plate = MicroShape.FACE.boxes(SpaceSlots.face(f), MicroShape.THINNEST, SpaceSlots.Y)
                    .getFirst();
            face[f] = box(plate.x0(), plate.y0(), plate.z0(), plate.x1(), plate.y1(), plate.z1());
        }
        final VoxelShape[] out = new VoxelShape[1 << face.length];
        for (int mask = 0; mask < out.length; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int f = 0; f < face.length; f++) {
                if ((mask & 1 << f) != 0) {
                    shape = Shapes.or(shape, face[f]);
                }
            }
            out[mask] = shape.optimize();
        }
        return out;
    }
}
