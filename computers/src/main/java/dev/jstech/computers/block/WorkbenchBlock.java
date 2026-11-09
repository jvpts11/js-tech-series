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
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.computers.chain.ChainBenches;
import dev.jstech.computers.menu.WorkbenchMenu;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Electronics Workbench of an era (computers/docs/design/survival.md): the player's own bench, used by hand and
 * never automated. Its window takes the parts' ingredients in nine trays, and it assembles them as a furnace smelts,
 * from the energy that comes in behind it, the window open or closed. Nothing goes in or out through its sides.
 */
@TextHolder
public final class WorkbenchBlock extends DeviceBlock {

    private final Era era;

    public static final MapCodec<WorkbenchBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(WorkbenchBlock::era)
    ).apply(i, WorkbenchBlock::new));
    private static final TextKey ASSEMBLES = TextKey.of("block.jsc.electronics_workbench.assembles",
            "Assembles its era's parts and the older eras', by hand");
    /* The bench's top stands at ten pixels, its tools above it; the drawers and the legs under it. */
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
    private static final Device<WorkbenchBlockEntity> DEVICE = Device.of(() -> ChainBenches.BENCH_BE.get())
            .ticks(WorkbenchBlockEntity::serverTick)
            .opensMenu(WorkbenchMenu::new)
            .ports(FacePorts.builder().port(FaceRule.EVERY, SharedEnergy.LOW_VOLTAGE_LINE).build());

    public WorkbenchBlock(final Properties properties, final Era era) {
        super(properties, DEVICE);
        this.era = era;
    }

    /** The face of a bench in {@code state} its energy comes in through: its back, where its socket is. */
    public static Direction cableFace(final BlockState state) {
        return state.getValue(FACING).getOpposite();
    }

    /** The era whose parts, and the older ones', it assembles. */
    public Era era() {
        return era;
    }

    /** Its line comes in behind it, and only there. */
    @Override
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        return face == cableFace(state) && super.accepts(state, face, offered);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPE;
    }

    /** It is one model drawn by the block entity, with its hookup behind it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(GameText.component(ASSEMBLES).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
