/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.audio.SoundHardwareTexts;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.CaseGlass;
import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.hardware.QuinceCase;
import dev.jstech.computers.hardware.QuinceMachines;
import dev.jstech.computers.item.HardwareTooltip;
import dev.jstech.computers.menu.PersonalComputerMenu;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.connect.FaceRule;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLines;
import dev.jstech.core.jstechseries.peripheral.PeripheralLine;
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
 * One of Quince's computers (computers/docs/design/quince.md), a Personal Computer in every way but its case: it takes
 * only its own logic board, and is drawn by the model of the machine it is. An all-in-one and the Studio take their
 * cables only from behind and have their power button there; a tower takes them as a PC does, with its button on its
 * front.
 */
public class QuinceComputerBlock extends AbstractSmallComputerBlock<PersonalComputerBlockEntity> {

    private final QuinceCase machine;

    private static final Codec<QuinceCase> MACHINE = Codec.STRING.comapFlatMap(model -> {
        final QuinceCase found = QuinceMachines.byModel(model);
        return found == null ? DataResult.error(() -> "no Quince computer is drawn as " + model)
                : DataResult.success(found);
    }, QuinceCase::model);
    public static final MapCodec<QuinceComputerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            MACHINE.fieldOf("machine").forGetter(QuinceComputerBlock::machine)
    ).apply(i, QuinceComputerBlock::new));

    public QuinceComputerBlock(final Properties properties, final QuinceCase machine) {
        super(properties, machine.era(), CaseStyle.SOLE, machine.front(), ports(machine),
                PersonalComputerBlockEntity.class, ComputingModule.PERSONAL_COMPUTER_BE::get,
                PersonalComputerBlockEntity::serverTick);
        this.machine = machine;
    }

    /** Which of Quince's computers it is. */
    public QuinceCase machine() {
        return machine;
    }

    @Override
    public String machineName() {
        return "personal_computer";
    }

    @Override
    public String caseModel() {
        return machine.model();
    }

    @Override
    public boolean drawsSupplyLead() {
        return false;
    }

    @Override
    @Nullable
    public CaseGlass glass() {
        return machine.glass();
    }

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
    protected MapCodec<? extends QuinceComputerBlock> codec() {
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
    @Nullable
    protected FrontArea powerBehind() {
        return machine.powerBehind() ? machine.power() : null;
    }

    @Override
    protected boolean sideComesOff() {
        return machine.kind() == QuinceCase.Kind.TOWER;
    }

    /*
     * The access line of its era and the older ones, as a PC takes; an all-in-one and the Studio take every cable from
     * behind, a tower its peripherals on any face, as a PC does.
     */
    private static FacePorts ports(final QuinceCase machine) {
        final FaceRule peripherals = machine.cablesBehind() ? FaceRule.BACK : FaceRule.EVERY;
        return FacePorts.builder().port(FaceRule.BACK, DataLines.upTo(machine.era(), DataLine.ACCESS))
                .port(peripherals, PeripheralLine.of(machine.era())).build();
    }
}
