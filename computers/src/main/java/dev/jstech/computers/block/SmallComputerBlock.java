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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.SmallComputerBlockEntity;
import dev.jstech.computers.item.HardwareTooltip;
import dev.jstech.computers.menu.PersonalComputerMenu;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.peripheral.PeripheralLine;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A small computer another mod brings, in one of the cases this mod ships: the mod registers this block in its own
 * register and declares its entry through the register event, which says the boards its case takes, the machine it
 * is and the parts it counts. It is assembled on the Personal Computer's screen and drawn in the case of its era and
 * style; on its back, the access line of its era's data cable and every earlier one's, and peripheral cable on every
 * side, as a Personal Computer has.
 */
public class SmallComputerBlock extends AbstractSmallComputerBlock<SmallComputerBlockEntity> {

    public static final MapCodec<SmallComputerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(SmallComputerBlock::era),
            StableCodecs.byName(CaseStyle.class).fieldOf("case").forGetter(SmallComputerBlock::caseStyle)
    ).apply(i, SmallComputerBlock::new));

    public SmallComputerBlock(final Properties properties, final Era era, final CaseStyle caseStyle) {
        super(properties, era, caseStyle,
                FacePorts.builder().port(FaceRule.BACK, DataLines.upTo(era, DataLine.ACCESS))
                        .port(FaceRule.EVERY, PeripheralLine.of(era)).build(),
                SmallComputerBlockEntity.class, ComputingModule.SMALL_COMPUTER_BE::get,
                PersonalComputerBlockEntity::serverTick);
    }

    /**
     * Joins every small computer any mod registered to the one block entity type they share; the mod's listener on
     * the event, which comes once every block is registered.
     */
    public static void joinTheirEntity(final BlockEntityTypeAddBlocksEvent event) {
        final Block[] computers = BuiltInRegistries.BLOCK.stream().filter(SmallComputerBlock.class::isInstance)
                .toArray(Block[]::new);
        event.modify(ComputingModule.SMALL_COMPUTER_BE.get(), computers);
    }

    /** Its case is the Personal Computer's, which the models name it by. */
    @Override
    public String machineName() {
        return "personal_computer";
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        HardwareTooltip.appendEra(tooltip, era());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new SmallComputerBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends SmallComputerBlock> codec() {
        return CODEC;
    }

    @Override
    protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                               final SmallComputerBlockEntity computer) {
        return new PersonalComputerMenu(containerId, inventory, computer);
    }

    @Override
    protected Component menuTitle() {
        return getName();
    }

    /* What the personal-use cards held, on a small computer that is a Personal Computer, falls out as it goes. */
    @Override
    protected void dropWhenRemoved(final ServerLevel level, final BlockPos pos,
                                   final SmallComputerBlockEntity computer) {
        for (final ItemStack held : computer.workshopDrops()) {
            Block.popResource(level, pos, held);
        }
    }
}
