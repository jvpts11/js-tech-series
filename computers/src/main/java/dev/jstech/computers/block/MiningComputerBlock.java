/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.blockentity.ChunkAnchor;
import dev.jstech.computers.blockentity.MiningComputerBlockEntity;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.RigGeometry;
import dev.jstech.computers.menu.MiningComputerMenu;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.bigblock.AbstractBigBlock;
import dev.jstech.core.bigblock.IBigBlockGeometry;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.connect.IFaceConnector;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.peripheral.PeripheralLine;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Mining Computer (computers/docs/design/teracoin.md), a big block: the Transition's rig in one block, the
 * Standard's and the Advanced's two blocks wide, the second standing on the left of a player facing its front. The
 * origin is the block the player places; a wide rig's second block is its part ({@link MiningComputerPartBlock}).
 *
 * <p>A click opens its assembly screen, where the board, the processor, the memory, the supply, the disk and the risers
 * go in. Its cards go in here, in the world: a graphics card in hand aimed at a riser's place seats it there, and
 * sneaking with an empty hand at a seated card takes it off, both only with the rig switched off.
 */
public class MiningComputerBlock extends AbstractBigBlock implements IFaceConnector, IEraChassisBlock {

    private final Era era;
    /* An open frame: the data cable of its era or an earlier one, and the peripheral cable, meet it on any face. */
    private final FacePorts ports;

    public static final MapCodec<MiningComputerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(MiningComputerBlock::era)
    ).apply(i, MiningComputerBlock::new));
    private static final int TURNS = 4;
    private static final int NORTH_FROM = 2;

    public MiningComputerBlock(final Properties properties, final Era era) {
        super(properties);
        this.era = era;
        this.ports = portsOf(era);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The era of the rig: its frame, the board it takes and how many risers it carries. */
    public Era era() {
        return era;
    }

    @Override
    public Era chassisEra() {
        return era;
    }

    @Override
    public FacePorts ports() {
        return ports;
    }

    /** What a rig of {@code era} takes on any face of its blocks: its data cable or an earlier one, and peripherals. */
    public static FacePorts portsOf(final Era era) {
        return FacePorts.builder().port(FaceRule.EVERY, DataLines.upTo(era, DataLine.ACCESS))
                .port(FaceRule.EVERY, PeripheralLine.of(era)).build();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction facing = context.getHorizontalDirection().getOpposite();
        final Level level = context.getLevel();
        final List<BlockPos> obstructed = getObstructedBlocks(level, context.getClickedPos(), facing);
        if (!obstructed.isEmpty()) {
            // No room beside it for a wide rig: nothing is placed, and what is in the way is shown.
            spawnMisplaceParticles(level, obstructed);
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new MiningComputerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        return level.isClientSide() ? null : BlockEntityTickers.create(type,
                ComputingModule.MINING_COMPUTER_BE.get(), MiningComputerBlockEntity::serverTick);
    }

    /** Opens the rig's assembly screen for {@code player}, from its origin or its part. */
    public static void open(final ServerPlayer player, final BlockPos origin, final MiningComputerBlockEntity rig) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MiningComputerMenu(id, inventory, rig),
                rig.getBlockState().getBlock().getName()), buf -> buf.writeBlockPos(origin));
    }

    /**
     * The riser whose place a look from {@code from} to {@code to} meets first, of the rig whose origin is at
     * {@code origin}; -1 when it meets none. Its risers stand inside its blocks, where the game's own aim never
     * reaches, so the look is followed through them.
     */
    public static int aimedRiser(final BlockGetter level, final BlockPos origin, final Vec3 from, final Vec3 to) {
        final BlockState state = level.getBlockState(origin);
        if (!(state.getBlock() instanceof MiningComputerBlock rig)) {
            return -1;
        }
        final Direction facing = state.getValue(FACING);
        final int risers = MiningComputerStructure.risers(rig.era());
        int best = -1;
        double nearest = Double.MAX_VALUE;
        for (int i = 0; i < risers; i++) {
            final Optional<Vec3> hit = riserPlace(origin, facing, rig.era(), i).clip(from, to);
            if (hit.isPresent() && hit.get().distanceToSqr(from) < nearest) {
                nearest = hit.get().distanceToSqr(from);
                best = i;
            }
        }
        return best;
    }

    /** The place of riser {@code index} of a rig of {@code era} at {@code origin}, in the world. */
    public static AABB riserPlace(final BlockPos origin, final Direction facing, final Era era, final int index) {
        final double[] at = RigGeometry.of(era).placeTurned(index, turnsOf(facing));
        return new AABB(at[0], at[1], at[2], at[3], at[4], at[5]).move(origin);
    }

    /** How many quarter turns clockwise, seen from above, a rig facing {@code facing} stands from facing north. */
    public static int turnsOf(final Direction facing) {
        return (facing.get2DDataValue() + NORTH_FROM) % TURNS;
    }

    /**
     * A click with {@code held} on the rig at {@code origin}: a graphics card seated on the riser the player aims at,
     * or why it is not; nothing for a click that aims at no riser or holds no graphics card.
     */
    static ItemInteractionResult seatAt(final Level level, final BlockPos origin, final Player player,
                                        final ItemStack held) {
        if (!(HardwareRegistry.card(held) instanceof GpuSpec)
                || !(level.getBlockEntity(origin) instanceof MiningComputerBlockEntity rig)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        final int riser = aimedRiser(level, origin, player.getEyePosition(), reach(player));
        if (riser < 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            final TextKey why = rig.seatRefusal(riser, held);
            if (why != null) {
                player.displayClientMessage(GameText.component(why), true);
            } else {
                rig.seat(riser, player.getAbilities().instabuild ? held.copy() : held);
                MachineOperators.note(rig, player);
                level.playSound(null, origin, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * A click with an empty hand on the rig at {@code origin}: sneaking at a seated card takes it off, the rig off;
     * anything else opens the assembly screen.
     */
    static InteractionResult useAt(final Level level, final BlockPos origin, final Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(origin) instanceof MiningComputerBlockEntity rig)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        MachineOperators.note(rig, player);
        final int riser = player.isShiftKeyDown()
                ? aimedRiser(level, origin, player.getEyePosition(), reach(player)) : -1;
        if (riser >= 0 && !rig.cardOn(riser).isEmpty()) {
            if (!rig.switchedOff()) {
                player.displayClientMessage(GameText.component(MiningComputerTexts.SWITCH_OFF_FIRST), true);
            } else {
                final ItemStack card = rig.unseat(riser);
                if (!player.getInventory().add(card)) {
                    player.drop(card, false);
                }
                level.playSound(null, origin, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            return InteractionResult.SUCCESS;
        }
        open(serverPlayer, origin, rig);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MapCodec<? extends MiningComputerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /* The frame and all it holds are one model drawn by the block entity; the block paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected IBigBlockGeometry geometry() {
        return MiningComputerStructure.of(era);
    }

    @Override
    protected boolean isOwnPart(final BlockState state) {
        return state.getBlock() instanceof MiningComputerPartBlock;
    }

    @Override
    protected boolean isOwnOrigin(final BlockState state) {
        return state.getBlock() instanceof MiningComputerBlock;
    }

    @Override
    protected BlockState partStateFor(final BlockPos origin, final Direction facing, final BlockPos part,
                                      final BlockState originState) {
        return ComputingModule.MINING_COMPUTER_PART.get().defaultBlockState()
                .setValue(FACING, facing)
                .setValue(MiningComputerPartBlock.ERA, PartEras.valueFor(era));
    }

    /*
     * The rig is given back whole with what went in at its screen; the cards, seated in the world, and the risers they
     * stood on fall beside it.
     */
    @Override
    protected void dropContents(final ServerLevel level, final BlockPos origin) {
        if (level.getBlockEntity(origin) instanceof MiningComputerBlockEntity rig) {
            rig.spillRigging(level, origin);
            rig.spillFront(level, origin);
            Block.popResource(level, origin, rig.asItem());
        } else {
            Block.popResource(level, origin, new ItemStack(asItem()));
        }
    }

    @Override
    protected void onOriginBroken(final ServerLevel level, final BlockPos origin) {
        if (level.getBlockEntity(origin) instanceof MiningComputerBlockEntity rig) {
            rig.onBroken(level);
        }
        ChunkAnchor.letGo(level, origin);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack held, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        return seatAt(level, pos, player, held);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        return useAt(level, pos, player);
    }

    /* As far as the player reaches, along the player's look. */
    private static Vec3 reach(final Player player) {
        return player.getEyePosition().add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
    }
}
