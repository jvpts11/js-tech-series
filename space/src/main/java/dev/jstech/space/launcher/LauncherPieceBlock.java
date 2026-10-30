/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launcher;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.multiblock.plan.IPlanMember;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A piece of the mobile launcher's kit, laid by a hangar's plan under the rocket it builds: a block of the kit like any
 * other until its part of the plan is whole, when it takes a formed shape of its own, as the approved launcher draws
 * it, keeping itself rather than merging. What it becomes, and which way it lies, it works out from the pieces round
 * it: a tower frame with no frame over it is the crane house, or the crane's boom where nothing of the launcher is
 * under it; an arm's end is the one nearest the rocket, its plug or its white room; a mast's top carries its hood; the
 * elevator's lowest block is its car.
 */
public class LauncherPieceBlock extends HorizontalDirectionalBlock implements IPlanMember {

    private final Kind kind;
    private final MapCodec<LauncherPieceBlock> codec;
    private final Map<Role, Map<Direction, VoxelShape>> shapes = new EnumMap<>(Role.class);

    /** Whether its part of the plan is whole, so it takes its formed shape. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** What it is in its part once formed: its body, its top, or its end. */
    public static final EnumProperty<Role> ROLE = EnumProperty.create("role", Role.class);
    private static final double PIXEL = 1.0 / 16;
    /* The longest arm walked back to its tower. */
    private static final int LONGEST_ARM = 32;

    public LauncherPieceBlock(final BlockBehaviour.Properties properties, final Kind kind) {
        super(properties.noOcclusion());
        this.kind = kind;
        this.codec = simpleCodec(made -> new LauncherPieceBlock(made, kind));
        for (final Role role : Role.values()) {
            final Map<Direction, VoxelShape> turned = new EnumMap<>(Direction.class);
            for (final Direction way : Direction.Plane.HORIZONTAL) {
                VoxelShape shape = Shapes.empty();
                for (final int[] box : kind.boxes(role)) {
                    shape = Shapes.or(shape, turned(box, way));
                }
                turned.put(way, shape.optimize());
            }
            this.shapes.put(role, turned);
        }
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false)
                .setValue(ROLE, Role.BODY));
    }

    /** What piece of the launcher it is. */
    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /** Its part of the plan formed or came apart: it takes its formed shape, worked out from the pieces round it. */
    @Override
    public void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed) {
        final BlockState now = level.getBlockState(pos);
        if (now.getBlock() != this) {
            return;
        }
        BlockState to = now.setValue(FORMED, formed);
        if (formed) {
            to = shaped(level, pos, to.setValue(ROLE, role(level, pos)).setValue(FACING, facing(level, pos,
                    now.getValue(FACING))));
        }
        if (to != now) {
            level.setBlock(pos, to, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        if (!state.getValue(FORMED)) {
            return this.kind == Kind.ELEVATOR ? this.shapes.get(Role.BODY).get(Direction.NORTH) : Shapes.block();
        }
        return this.shapes.get(state.getValue(ROLE)).get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        // The elevator's rails are climbed, never stood in the way of.
        return this.kind == Kind.ELEVATOR ? Shapes.empty() : getShape(state, level, pos, context);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    /** Its formed state's details beyond its role and its facing, worked out from the pieces round it; none here. */
    protected BlockState shaped(final BlockGetter level, final BlockPos pos, final BlockState state) {
        return state;
    }

    /* The kind of launcher piece at {@code pos}, or none: for a piece working out its shape. */
    protected static @Nullable Kind kindOf(final BlockGetter level, final BlockPos pos) {
        return kindAt(level, pos);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, ROLE);
    }

    /* What it is in its part, from the pieces over and under it, and along it. */
    private Role role(final BlockGetter level, final BlockPos pos) {
        final Kind below = kindAt(level, pos.below());
        final Kind above = kindAt(level, pos.above());
        return switch (this.kind) {
            case TOWER -> above != Kind.TOWER ? below == null ? Role.END : Role.TOP : Role.BODY;
            case MAST -> above != Kind.MAST ? Role.TOP : Role.BODY;
            case PEDESTAL -> above != Kind.PEDESTAL ? Role.TOP : below != Kind.PEDESTAL ? Role.END : Role.BODY;
            case ELEVATOR -> below != Kind.ELEVATOR ? Role.END : above != Kind.ELEVATOR ? Role.TOP : Role.BODY;
            case SWING_ARM, CREW_ARM -> {
                // Its end is the block nearest the rocket; its top the one at the tower, on its hinge.
                final Direction out = armOut(level, pos);
                yield out == null ? Role.BODY : kindAt(level, pos.relative(out)) != this.kind ? Role.END
                        : kindAt(level, pos.relative(out.getOpposite())) != this.kind ? Role.TOP : Role.BODY;
            }
            default -> Role.BODY;
        };
    }

    /*
     * Which way it lies once formed: an arm out from its tower, the boom out from the tower, a mast's hood and an
     * exhaust frame's lining toward the opening, a hold-down arm's jaw toward the corner of the opening it stands at;
     * the way it was placed when nothing says otherwise.
     */
    private Direction facing(final BlockGetter level, final BlockPos pos, final Direction was) {
        Direction found = null;
        switch (this.kind) {
            case SWING_ARM, CREW_ARM -> found = armOut(level, pos);
            case TOWER -> {
                if (kindAt(level, pos.below()) == null && kindAt(level, pos.above()) != Kind.TOWER) {
                    found = boomOut(level, pos);
                }
            }
            case MAST -> {
                BlockPos base = pos;
                while (kindAt(level, base.below()) == Kind.MAST) {
                    base = base.below();
                }
                for (final Direction way : Direction.Plane.HORIZONTAL) {
                    if (level.getBlockState(base.relative(way).below()).isAir()) {
                        found = way;
                    }
                }
            }
            case EXHAUST -> {
                for (final Direction way : Direction.Plane.HORIZONTAL) {
                    if (level.getBlockState(pos.relative(way)).isAir()) {
                        found = way;
                    }
                }
            }
            case HOLD_DOWN -> {
                for (final Direction way : Direction.Plane.HORIZONTAL) {
                    // Facing north, its jaw reaches north-west; each quarter turn takes the next corner.
                    final Direction left = way.getCounterClockWise();
                    if (level.getBlockState(pos.relative(way).relative(left).below()).isAir()) {
                        found = way;
                    }
                }
            }
            default -> {
            }
        }
        return found == null ? was : found;
    }

    /*
     * The way an arm runs out from its tower, toward the rocket: back along it to the tower, or to the doorway in the
     * tower's wall the crew access arm leaves from; none when no tower is found along it.
     */
    private @Nullable Direction armOut(final BlockGetter level, final BlockPos pos) {
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            BlockPos back = pos.relative(way.getOpposite());
            for (int step = 0; step < LONGEST_ARM && kindAt(level, back) == this.kind; step++) {
                back = back.relative(way.getOpposite());
            }
            if (kindAt(level, back) == Kind.TOWER || kindAt(level, back) == null
                    && kindAt(level, back.below()) == Kind.TOWER && kindAt(level, back.above(2)) == Kind.TOWER) {
                return way;
            }
        }
        return null;
    }

    /*
     * The way the crane's boom runs out from the top of its tower: back along the boom's frames to one standing on the
     * tower; none when no tower is found along it.
     */
    private static @Nullable Direction boomOut(final BlockGetter level, final BlockPos pos) {
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            BlockPos back = pos.relative(way.getOpposite());
            for (int step = 0; step < LONGEST_ARM && kindAt(level, back) == Kind.TOWER; step++) {
                if (kindAt(level, back.below()) != null) {
                    return way;
                }
                back = back.relative(way.getOpposite());
            }
        }
        return null;
    }

    /* The kind of launcher piece at {@code pos}, or none. */
    private static @Nullable Kind kindAt(final BlockGetter level, final BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof LauncherPieceBlock piece ? piece.kind : null;
    }

    /* A box of the piece, in sixteenths as it faces north, turned to face {@code way}. */
    private static VoxelShape turned(final int[] box, final Direction way) {
        final int x0 = box[0];
        final int z0 = box[2];
        final int x1 = box[3];
        final int z1 = box[5];
        final int[] at = switch (way) {
            case SOUTH -> new int[] {16 - x1, 16 - z1, 16 - x0, 16 - z0};
            case EAST -> new int[] {16 - z1, x0, 16 - z0, x1};
            case WEST -> new int[] {z0, 16 - x1, z1, 16 - x0};
            default -> new int[] {x0, z0, x1, z1};
        };
        return Shapes.box(at[0] * PIXEL, box[1] * PIXEL, at[1] * PIXEL, at[2] * PIXEL, box[4] * PIXEL,
                at[3] * PIXEL);
    }

    /** What a formed piece is in its part. */
    public enum Role implements StringRepresentable {
        /** Its body: a plate of the deck, a bay of the tower, a length of an arm. */
        BODY("body"),
        /**
         * Its top: the tower's crane house, a mast's hood, the top of the elevator's rails, an arm's hinge, a
         * pedestal's cap.
         */
        TOP("top"),
        /** Its end: the crane's boom, an arm's plug or white room, the elevator's car, a pedestal's foot. */
        END("end");

        private final String name;

        Role(final String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    /**
     * The pieces, each with its formed boxes in sixteenths as it faces north, by its role: a mast's hood and an exhaust
     * frame's lining look north, an arm runs north out of its tower, a hold-down arm's jaw reaches north-west.
     */
    public enum Kind {
        DECK(new int[][] {{0, 0, 0, 16, 16, 16}}, null, null),
        EXHAUST(new int[][] {{0, 0, 0, 16, 16, 16}}, null, null),
        HOLD_DOWN(new int[][] {{2, 0, 2, 16, 11, 16}, {0, 4, 0, 8, 9, 8}}, null, null),
        MAST(new int[][] {{2, 0, 2, 14, 16, 14}}, new int[][] {{2, 0, 2, 14, 10, 14}}, null),
        TOWER(new int[][] {{0, 0, 0, 16, 16, 16}}, null, new int[][] {{1, 10, 0, 15, 16, 16}}),
        SWING_ARM(new int[][] {{4, 5, 0, 12, 11, 16}}, null, new int[][] {{4, 5, 3, 12, 11, 16}, {5, 4, 0, 11, 12,
                3}}),
        CREW_ARM(new int[][] {{0, 0, 0, 16, 2, 16}, {0, 2, 0, 1, 16, 16}, {15, 2, 0, 16, 16, 16}}, null,
                new int[][] {{0, 0, 0, 16, 2, 16}, {0, 2, 0, 1, 16, 16}, {15, 2, 0, 16, 16, 16}}),
        ELEVATOR(new int[][] {{5, 0, 5, 11, 16, 11}}, new int[][] {{5, 0, 5, 11, 16, 11}},
                new int[][] {{2, 0, 2, 14, 16, 14}}),
        /** A pedestal the launcher stands on, in the hangar or on the pad; it stays when the launcher goes. */
        PEDESTAL(new int[][] {{0, 0, 0, 16, 16, 16}}, null, null);

        private final int[][] body;
        private final int[][] top;
        private final int[][] end;

        /* A piece whose top or end is shaped as its body where they are null. */
        Kind(final int[][] body, final int[][] top, final int[][] end) {
            this.body = body;
            this.top = top == null ? body : top;
            this.end = end == null ? body : end;
        }

        /** Its boxes, in sixteenths, as it faces north, in its role {@code role}. */
        public int[][] boxes(final Role role) {
            return switch (role) {
                case BODY -> this.body.clone();
                case TOP -> this.top.clone();
                case END -> this.end.clone();
            };
        }
    }
}
