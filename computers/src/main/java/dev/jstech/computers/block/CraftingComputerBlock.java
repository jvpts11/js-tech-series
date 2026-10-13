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
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.peripheral.PeripheralLine;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Crafting Computer block: an ATX-class computer that executes recipes for the network.
 */
public class CraftingComputerBlock extends AbstractSmallComputerBlock<CraftingComputerBlockEntity> {

    public static final MapCodec<CraftingComputerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(CraftingComputerBlock::era),
            StableCodecs.byName(CaseStyle.class).fieldOf("case").forGetter(CraftingComputerBlock::caseStyle)
    ).apply(i, CraftingComputerBlock::new));

    /*
     * Data over its era's access line on the back (through a router to the backbone), and the crafting cable to its
     * Crafting Interfaces on any face, since the search for the crafting network walks out of all six.
     */
    public CraftingComputerBlock(final Properties properties, final HardwareEra era, final CaseStyle caseStyle) {
        super(properties, era, caseStyle,
                FacePorts.builder()
                        .port(FaceRule.BACK, DataLines.upTo(era, DataLine.ACCESS))
                        .port(FaceRule.EVERY, DataLines.upTo(era, DataLine.CRAFTING), PeripheralLine.of(era))
                        .build(),
                CraftingComputerBlockEntity.class, ComputingModule.CRAFTING_COMPUTER_BE::get,
                CraftingComputerBlockEntity::serverTick);
    }

    @Override
    public String machineName() {
        return "crafting_computer";
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CraftingComputerBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends CraftingComputerBlock> codec() {
        return CODEC;
    }

    @Override
    protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                               final CraftingComputerBlockEntity computer) {
        return new CraftingComputerMenu(containerId, inventory, computer);
    }

    @Override
    protected Component menuTitle() {
        return Component.translatable("block.jsc.crafting_computer");
    }
}
