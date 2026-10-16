/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import dev.jstech.core.util.BlockEntityTickers;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A piece of conveyor of a kind a mod declares ({@link ConveyorType}): a belt, flat or sloped ({@link ConveyorBlock}),
 * a chute ({@link ChuteBlock}) or a bucket elevator ({@link ElevatorBlock}), or a piece of the mod's own. A piece
 * carries items from where they come on ({@link #entry}) to where they leave ({@link #exit}); pieces of one kind, each
 * leaving where the next comes on, are a run, moved as one by the piece at its end, the only one whose state says it is
 * an end and the only one that ticks: a run of any length costs a piece's tick. Its block entity is a
 * {@link ConveyorBlockEntity}, of a type its mod registers.
 */
public abstract class ConveyorPieceBlock extends Block implements EntityBlock {

    private final ConveyorType type;
    private final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity;

    /** Whether the piece is the end of its run: no piece of its run comes after it. */
    public static final BooleanProperty END = BooleanProperty.create("end");
    /** What {@link #intakeAt} and {@link #landsAt} answer where nothing comes on. */
    public static final double NOWHERE = -1.0;
    /** Where along a piece its start is. */
    public static final double START = 0.0;
    /** Where along a piece its middle is. */
    public static final double MIDDLE = 0.5;
    /** How many pixels a block has a side. */
    protected static final double PIXELS = 16.0;
    /** How far above a piece's line an item lying on it may be and still be taken on, in blocks. */
    protected static final double LYING = 0.25;
    /**
     * About the most pieces one run holds: a line longer is cut into runs of about this many, each handing on to the
     * next, and a loop longer has an end of its own on every piece.
     */
    public static final int LONGEST_RUN = 1024;

    protected ConveyorPieceBlock(final Properties properties, final ConveyorType type,
                                 final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity) {
        super(properties);
        this.type = Objects.requireNonNull(type, "type");
        this.entity = Objects.requireNonNull(entity, "entity");
    }

    /**
     * Whether the piece in {@code state} at {@code pos} carries its run on into the piece after it: the piece it
     * leaves into is of its kind, comes on where it leaves, and has it, of every piece that leaves there, before it.
     */
    public static boolean continues(final BlockGetter level, final BlockPos pos, final BlockState state) {
        final BlockPos next = firstAhead(level, pos, state);
        return next != null && pos.equals(firstBehind(level, next, level.getBlockState(next)));
    }

    /** The first piece of its kind that comes on where the piece in {@code state} at {@code pos} leaves, or none. */
    public static @Nullable BlockPos firstAhead(final BlockGetter level, final BlockPos pos, final BlockState state) {
        if (!(state.getBlock() instanceof ConveyorPieceBlock piece)) {
            return null;
        }
        final ConveyorOpening exit = piece.exit(state, pos);
        // A piece starts in its own block, or, a falling belt, in the block above it.
        final BlockPos below = exit.cell().below();
        if (piece.meets(level, exit.cell(), exit, true)) {
            return exit.cell();
        }
        return piece.meets(level, below, exit, true) ? below : null;
    }

    /** The first piece of its kind that leaves where the piece in {@code state} at {@code pos} comes on, or none. */
    public static @Nullable BlockPos firstBehind(final BlockGetter level, final BlockPos pos, final BlockState state) {
        if (!(state.getBlock() instanceof ConveyorPieceBlock piece)) {
            return null;
        }
        final ConveyorOpening entry = piece.entry(state, pos);
        // A piece leaving here stands behind the opening, or, a rising belt, a block below that.
        final BlockPos behind = entry.cell().relative(entry.way().getOpposite());
        if (piece.meets(level, behind, entry, false)) {
            return behind;
        }
        final BlockPos below = behind.below();
        return piece.meets(level, below, entry, false) ? below : null;
    }

    /** The kind of conveyor it is. */
    public ConveyorType type() {
        return this.type;
    }

    /** The type of its block entity. */
    public BlockEntityType<? extends ConveyorBlockEntity> entityType() {
        return this.entity.get();
    }

    /** Where items come onto the piece in {@code state} at {@code pos}: where a piece before it in its run leaves. */
    public abstract ConveyorOpening entry(BlockState state, BlockPos pos);

    /** Where items leave the piece in {@code state} at {@code pos}: where the piece after it in its run comes on. */
    public abstract ConveyorOpening exit(BlockState state, BlockPos pos);

    /**
     * Where along the piece in {@code state} an item put on through its {@code side} face comes on: {@link #START},
     * {@link #MIDDLE} or in between; {@link #NOWHERE} where it gives. A side of none is the mod's own code.
     */
    public abstract double intakeAt(BlockState state, @Nullable Direction side);

    /** The line the items on the piece in {@code state} lie along, and how they lie on it. */
    public abstract ConveyorGeometry geometry(BlockState state);

    /**
     * Where the piece in {@code state} at {@code pos}, at the end of its run, hands its items, in the order tried; the
     * first is also where they fall off a kind that drops them. Into the block it leaves into, unless said.
     */
    public List<ConveyorOutlet> outlets(final BlockState state, final BlockPos pos) {
        final ConveyorOpening exit = exit(state, pos);
        return List.of(new ConveyorOutlet(exit.cell(), exit.way().getOpposite()));
    }

    /**
     * Where the piece in {@code state} at {@code pos}, {@code belt}, hands on {@code item}, the first on it: its
     * outlets, whatever the item, unless said. A diverter says by its filter.
     */
    public List<ConveyorOutlet> outlets(final BlockState state, final BlockPos pos, final ConveyorBlockEntity belt,
                                        final ItemStack item) {
        return outlets(state, pos);
    }

    /**
     * Whether pieces of its kind may go on into it, and it into them, in one run; true unless said. A splitter, a join
     * and a diverter stand alone, each its own run, taking in and handing on through the item capability.
     */
    public boolean joinsRuns() {
        return true;
    }

    /** Whether it hands each item to the next of its outlets in turn, rather than the first that takes it. */
    public boolean dealsInTurn() {
        return false;
    }

    /** Whether it takes items in from each of its sides in turn, so no side waits on another for long. */
    public boolean takesInTurn() {
        return false;
    }

    /** Whether a machine on top of the piece in {@code state} works its items as they pass ({@link IBeltWorker}). */
    public boolean workedFromAbove(final BlockState state) {
        return false;
    }

    /** Whether its block entity keeps a filter, which the piece reads to choose where an item goes. */
    public boolean filters() {
        return false;
    }

    /**
     * Where along the piece in {@code state} at {@code pos} an item lying at {@code local}, in its block, is taken on;
     * {@link #NOWHERE} when it is not, as unless said.
     */
    public double landsAt(final BlockState state, final BlockPos pos, final Vec3 local) {
        return NOWHERE;
    }

    /** The way the piece in {@code state} carries what stands on it, for a kind that carries it; none unless said. */
    public @Nullable Direction carries(final BlockState state) {
        return null;
    }

    /** The axis a shaft turns the piece in {@code state} on, for a kind on mechanical power; none unless said. */
    public @Nullable Direction.Axis axle(final BlockState state) {
        return null;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return this.entity.get().create(pos, state);
    }

    /* Only the end of a run ticks: it moves the whole run. */
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                            final BlockEntityType<T> blockEntity) {
        if (level.isClientSide() || !state.getValue(END)) {
            return null;
        }
        return BlockEntityTickers.create(blockEntity, this.entity.get(), ConveyorBlockEntity::serverTick);
    }

    /*
     * A piece laid, or turned another way, may join or part the runs of the pieces around it, a corner away too:
     * whether each ends its run is worked out again, here and as a piece is broken, and only then.
     */
    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState old,
                           final boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!old.is(this) || old.setValue(END, state.getValue(END)) != state) {
            refreshAround(level, pos);
        }
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean moved) {
        final boolean gone = !newState.is(this);
        if (gone && level.getBlockEntity(pos) instanceof ConveyorBlockEntity piece) {
            piece.fields().broken(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
        if (gone) {
            refreshAround(level, pos);
        }
    }

    /* An item that lands on it is taken onto it, and what stands on it is carried. */
    @Override
    protected void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (level.getBlockEntity(pos) instanceof ConveyorBlockEntity belt) {
            belt.touched(entity);
        }
    }

    /* Whether the piece at {@code at} is of its kind and comes on (not {@code asEntry}: leaves) at {@code where}. */
    private boolean meets(final BlockGetter level, final BlockPos at, final ConveyorOpening where,
                          final boolean asEntry) {
        final BlockState other = level.getBlockState(at);
        if (!(other.getBlock() instanceof ConveyorPieceBlock piece) || piece.type != this.type || !joinsRuns()
                || !piece.joinsRuns()) {
            return false;
        }
        return (asEntry ? piece.entry(other, at) : piece.exit(other, at)).equals(where);
    }

    /* Works out again whether each piece in the blocks around {@code pos}, its own included, ends its run. */
    private static void refreshAround(final Level level, final BlockPos pos) {
        for (final BlockPos at : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            final BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof ConveyorPieceBlock) {
                final BlockPos here = at.immutable();
                setEnd(level, here, state, endsRun(level, here, state));
            }
        }
    }

    /*
     * Whether the piece in {@code state} at {@code pos} should end its run: when nothing carries its run on; not when a
     * piece ahead of it ends it. Followed round a loop of pieces back to itself, the loop's lowest piece ends it, so a
     * loop moves; followed further than the longest run without an end, it ends a run of its own there.
     */
    private static boolean endsRun(final Level level, final BlockPos pos, final BlockState state) {
        if (!continues(level, pos, state)) {
            return true;
        }
        BlockPos at = pos;
        BlockState now = state;
        BlockPos lowest = pos;
        for (int k = 0; k < LONGEST_RUN; k++) {
            at = Objects.requireNonNull(firstAhead(level, at, now), "a piece that carries on has one ahead");
            if (at.equals(pos)) {
                if (!lowest.equals(pos)) {
                    setEnd(level, lowest, level.getBlockState(lowest), true);
                }
                return lowest.equals(pos);
            }
            if (!level.isLoaded(at)) {
                return false;
            }
            now = level.getBlockState(at);
            if (now.getValue(END)) {
                return false;
            }
            if (!continues(level, at, now)) {
                setEnd(level, at, now, true);
                return false;
            }
            if (at.asLong() < lowest.asLong()) {
                lowest = at;
            }
        }
        return true;
    }

    private static void setEnd(final Level level, final BlockPos pos, final BlockState state, final boolean end) {
        if (state.getValue(END) != end) {
            level.setBlock(pos, state.setValue(END, end), Block.UPDATE_CLIENTS);
        }
    }
}
