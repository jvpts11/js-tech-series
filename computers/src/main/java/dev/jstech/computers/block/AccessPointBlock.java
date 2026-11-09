/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.AccessPointBlockEntity;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A Wireless Access Point (computers/docs/design/wireless.md): a network device that gives the network it is cabled to
 * to the computers with a Wi-Fi card around it, while it draws its energy. The Legacy's and the Transition's stand on a
 * desk and take their cables from behind; the Standard's and the Advanced's are fixed to a ceiling or a wall on their
 * mounting plate, and take their cables through that face. Data and energy come on two lines.
 */
@TextHolder
public final class AccessPointBlock extends DeviceBlock {

    private final Era era;
    private final boolean mounted;

    /** Where it is fixed: on a desk, or on its plate under a ceiling or against a wall. */
    public static final EnumProperty<AccessPointMount> MOUNT = EnumProperty.create("mount", AccessPointMount.class);
    public static final MapCodec<AccessPointBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(AccessPointBlock::era),
            Codec.BOOL.fieldOf("mounted").forGetter(AccessPointBlock::mounted)
    ).apply(i, AccessPointBlock::new));
    private static final TextKey REACH_AND_SPEED = TextKey.of("block.jsc.access_point.reach_and_speed",
            "Reach %s blocks · %s items per tick");
    private static final TextKey DRAWS = TextKey.of("block.jsc.access_point.draws", "Draws %s W");
    private static final TextKey ON_A_DESK = TextKey.of("block.jsc.access_point.on_a_desk",
            "On a desk; its cables come in from behind");
    private static final TextKey FIXED = TextKey.of("block.jsc.access_point.fixed",
            "On a ceiling or a wall; its cables come in where it is fixed");
    /* What it is drawn in, in pixels: a desk box with its antennas left out, the plate and its body when fixed. */
    private static final VoxelShape DESK = Block.box(2.5, 0, 3.0, 13.5, 4.0, 13.0);
    private static final VoxelShape CEILING = Block.box(0.5, 12.5, 0.5, 15.5, 16.0, 15.5);
    private static final double FIXED_DEPTH = 3.5;

    public AccessPointBlock(final Properties properties, final Era era, final boolean mounted) {
        super(properties, device(era));
        this.era = era;
        this.mounted = mounted;
        // A fixed one's item shows it on a wall, its front to the eye, so that is how it stands until it is placed.
        registerDefaultState(defaultBlockState().setValue(MOUNT, mounted ? AccessPointMount.WALL
                : AccessPointMount.DESK));
    }

    /**
     * The face of a block in {@code state} its cables come in through: the back of one on a desk or a wall, the top of
     * one under a ceiling.
     */
    public static Direction cableFace(final BlockState state) {
        return state.getValue(MOUNT) == AccessPointMount.CEILING ? Direction.UP
                : state.getValue(FACING).getOpposite();
    }

    /** The era it is of. */
    public Era era() {
        return era;
    }

    /** Whether it is fixed to a ceiling or a wall, rather than standing on a desk. */
    public boolean mounted() {
        return mounted;
    }

    /** The Wi-Fi it gives. */
    public WirelessStandard standard() {
        return Objects.requireNonNull(WirelessStandard.of(era), "an access point is of an era with Wi-Fi");
    }

    /** Its lines come in through one face only: the one {@link #cableFace} names. */
    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        return face == cableFace(state) && super.accepts(state, face, offered);
    }

    /**
     * One on a desk faces whoever places it. One that is fixed hangs under the block above it, or sits on the side of the
     * block it is placed against, facing away from it; it is not set on a floor.
     */
    @Override
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        if (!mounted) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(MOUNT, AccessPointMount.DESK);
        }
        final Direction clicked = context.getClickedFace();
        final BlockState state;
        if (clicked == Direction.DOWN) {
            state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(MOUNT, AccessPointMount.CEILING);
        } else if (clicked.getAxis().isHorizontal()) {
            state = defaultBlockState().setValue(FACING, clicked).setValue(MOUNT, AccessPointMount.WALL);
        } else {
            return null;
        }
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /** One that is fixed needs the face it is fixed to; one on a desk stands anywhere. */
    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        if (state.getValue(MOUNT) == AccessPointMount.DESK) {
            return true;
        }
        final Direction face = cableFace(state);
        final BlockPos support = pos.relative(face);
        return level.getBlockState(support).isFaceSturdy(level, support, face.getOpposite());
    }

    /* The block it is fixed to taken away, it comes off, and drops as itself. */
    @Override
    protected BlockState updateShape(final BlockState state, final Direction direction, final BlockState neighbour,
                                     final LevelAccessor level, final BlockPos pos, final BlockPos neighbourPos) {
        if (state.getValue(MOUNT) != AccessPointMount.DESK && direction == cableFace(state)
                && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return switch (state.getValue(MOUNT)) {
            case DESK -> DESK;
            case CEILING -> CEILING;
            case WALL -> wall(state.getValue(FACING));
        };
    }

    /** It is one model drawn by the block entity, its lamps lit as it stands. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final WirelessStandard standard = standard();
        tooltip.add(Component.literal(standard.accessPointName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(REACH_AND_SPEED.with(standard.reachBlocks(), standard.itemsPerTick()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(DRAWS.with(standard.watts())).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(mounted ? FIXED : ON_A_DESK).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MOUNT);
    }

    /* What a device of {@code era} is: its block entity, its tick, and the data line of its era and energy it takes. */
    private static Device<AccessPointBlockEntity> device(final Era era) {
        return Device.of(() -> ComputingModule.ACCESS_POINT_BE.get())
                .ticks(AccessPointBlockEntity::serverTick)
                .ports(FacePorts.builder().port(FaceRule.EVERY, DataLines.upTo(era, DataLine.ACCESS),
                        SharedEnergy.LOW_VOLTAGE_LINE).build());
    }

    /* The plate and its body against the wall behind a block facing {@code facing}. */
    private static VoxelShape wall(final Direction facing) {
        return switch (facing) {
            case NORTH -> Block.box(0.5, 0.5, 16.0 - FIXED_DEPTH, 15.5, 15.5, 16.0);
            case SOUTH -> Block.box(0.5, 0.5, 0.0, 15.5, 15.5, FIXED_DEPTH);
            case WEST -> Block.box(16.0 - FIXED_DEPTH, 0.5, 0.5, 16.0, 15.5, 15.5);
            default -> Block.box(0.0, 0.5, 0.5, FIXED_DEPTH, 15.5, 15.5);
        };
    }
}
