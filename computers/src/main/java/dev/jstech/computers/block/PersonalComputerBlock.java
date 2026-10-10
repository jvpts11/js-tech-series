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
import dev.jstech.computers.audio.SoundHardwareTexts;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.item.HardwareTooltip;
import dev.jstech.computers.menu.PersonalComputerMenu;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.peripheral.PeripheralLine;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Personal Computer: the player's hands-on access point to the network, assembled on a consumer ATX board.
 */
public class PersonalComputerBlock extends AbstractSmallComputerBlock<PersonalComputerBlockEntity> {

    public static final MapCodec<PersonalComputerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            StableCodecs.byName(HardwareEra.class).fieldOf("era").forGetter(PersonalComputerBlock::era),
            StableCodecs.byName(CaseStyle.class).fieldOf("case").forGetter(PersonalComputerBlock::caseStyle)
    ).apply(i, PersonalComputerBlock::new));

    /* PCs are Ethernet-only, on their back, the access line of their era's cable and every earlier one's; they reach
     * HBW through a Personal Router. */
    public PersonalComputerBlock(final Properties properties, final HardwareEra era, final CaseStyle caseStyle) {
        super(properties, era, caseStyle,
                FacePorts.builder().port(FaceRule.BACK, DataLines.upTo(era, DataLine.ACCESS))
                        .port(FaceRule.EVERY, PeripheralLine.of(era)).build(),
                PersonalComputerBlockEntity.class, ComputingModule.PERSONAL_COMPUTER_BE::get,
                PersonalComputerBlockEntity::serverTick);
    }

    @Override
    public String machineName() {
        return "personal_computer";
    }

    /** Where the machine's sound comes from, and its era. */
    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        SoundHardwareTexts.appendComputer(tooltip, era());
        HardwareTooltip.appendEra(tooltip, era());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new PersonalComputerBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends PersonalComputerBlock> codec() {
        return CODEC;
    }

    @Override
    protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                               final PersonalComputerBlockEntity computer) {
        return new PersonalComputerMenu(containerId, inventory, computer);
    }

    @Override
    protected Component menuTitle() {
        return getName();
    }

    @Override
    protected void dropWhenRemoved(final ServerLevel level, final BlockPos pos,
                                   final PersonalComputerBlockEntity computer) {
        // What the personal-use cards held is the player's, so it falls out however the computer goes, as a
        // furnace's contents do.
        for (final ItemStack held : computer.workshopDrops()) {
            Block.popResource(level, pos, held);
        }
    }
}
