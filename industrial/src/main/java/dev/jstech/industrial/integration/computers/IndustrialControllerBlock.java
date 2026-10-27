/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.block.CaseStyle;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
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
import dev.jstech.industrial.JsIndustrial;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Industrial Controller Computer: a computer like a Personal Computer of its era (a system, programs, the data
 * network, a screen), in the cases J's Computers ships, whose Fieldbus Cards read the machines on its industrial
 * control cables and command them. On its back, the access line of its era's data cable and every earlier one's, and
 * peripheral cable on every side, as a Personal Computer has. Its case is drawn from this mod's own models: the panel
 * under the front with its RUN, BUS and FAULT lamps, the LOCAL/REMOTE key and the terminal strip.
 */
public class IndustrialControllerBlock extends AbstractSmallComputerBlock<IndustrialControllerBlockEntity> {

    public static final MapCodec<IndustrialControllerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(IndustrialControllerBlock::era),
            StableCodecs.byName(CaseStyle.class).fieldOf("case").forGetter(IndustrialControllerBlock::caseStyle)
    ).apply(i, IndustrialControllerBlock::new));

    public IndustrialControllerBlock(final Properties properties, final Era era, final CaseStyle caseStyle) {
        super(properties, era, caseStyle,
                FacePorts.builder().port(FaceRule.BACK, DataLines.upTo(era, DataLine.ACCESS))
                        .port(FaceRule.EVERY, PeripheralLine.of(era)).build(),
                IndustrialControllerBlockEntity.class, IndustrialControllers.ENTITY::get,
                PersonalComputerBlockEntity::serverTick);
    }

    @Override
    public String machineName() {
        return "industrial_controller_computer";
    }

    @Override
    public String modelsOf() {
        return JsIndustrial.MODID;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        HardwareTooltip.appendEra(tooltip, era());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new IndustrialControllerBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends IndustrialControllerBlock> codec() {
        return CODEC;
    }

    @Override
    protected AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                               final IndustrialControllerBlockEntity computer) {
        return new PersonalComputerMenu(containerId, inventory, computer);
    }

    @Override
    protected Component menuTitle() {
        return getName();
    }
}
