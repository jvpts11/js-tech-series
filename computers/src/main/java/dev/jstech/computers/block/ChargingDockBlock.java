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
import dev.jstech.computers.PortableItems;
import dev.jstech.computers.blockentity.ChargingDockBlockEntity;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Mobile Device Charging Dock (computers/docs/design/wireless.md): it holds one portable of its era, standing in it
 * by its own model, and charges its battery from the energy that comes in behind it. It has no window: a right click
 * with a portable puts it in, and one with an empty hand takes it out.
 */
@TextHolder
public final class ChargingDockBlock extends DeviceBlock {

    private final Era era;

    public static final MapCodec<ChargingDockBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(ChargingDockBlock::era)
    ).apply(i, ChargingDockBlock::new));
    private static final TextKey CHARGES = TextKey.of("block.jsc.mobile_device_charging_dock.charges",
            "Charges a portable of its era on any energy");
    private static final TextKey NOT_ITS_ERA = TextKey.of("block.jsc.mobile_device_charging_dock.not_its_era",
            "This dock holds a portable of the %s era");
    /* What each era's dock is drawn in, in pixels, the device standing in it left out. */
    private static final VoxelShape CRADLE = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    private static final VoxelShape BASE = Block.box(4.5, 0.0, 4.5, 11.5, 2.0, 11.5);
    private static final VoxelShape STAND = Block.box(2.5, 0.0, 2.5, 13.5, 10.0, 13.5);
    private static final VoxelShape WIRELESS_STAND = Block.box(3.5, 0.0, 3.5, 12.5, 9.0, 12.5);

    public ChargingDockBlock(final Properties properties, final Era era) {
        super(properties, Device.of(() -> PortableItems.CHARGING_DOCK_BE.get())
                .ticks(ChargingDockBlockEntity::serverTick)
                .ports(FacePorts.builder().port(FaceRule.EVERY, SharedEnergy.LOW_VOLTAGE_LINE).build()));
        this.era = era;
    }

    /** The face of a dock in {@code state} its energy comes in through: its back. */
    public static Direction cableFace(final BlockState state) {
        return state.getValue(FACING).getOpposite();
    }

    /** The era of the portables it holds. */
    public Era era() {
        return era;
    }

    /** Its line comes in behind it, and only there. */
    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        return face == cableFace(state) && super.accepts(state, face, offered);
    }

    /** A portable of its era goes in, when it holds none; another era's is refused, with a word why. */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!(stack.getItem() instanceof PortableItem portable)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof ChargingDockBlockEntity dock) || !dock.held().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!portable.model().era().equals(era)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(GameText.component(NOT_ITS_ERA.with(era.name())), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide()) {
            dock.put(stack.split(1));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** An empty hand takes out the portable it holds. */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ChargingDockBlockEntity dock) || dock.held().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            final ItemStack taken = dock.take();
            if (!player.addItem(taken)) {
                player.drop(taken, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        if (era.isAtMost(HardwareEra.LEGACY)) {
            return CRADLE;
        }
        if (era.isAtMost(HardwareEra.TRANSITION)) {
            return BASE;
        }
        return era.isAtMost(HardwareEra.STANDARD) ? STAND : WIRELESS_STAND;
    }

    /** It is one model drawn by the block entity, with the portable it holds and its lamp. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(GameText.component(CHARGES).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
