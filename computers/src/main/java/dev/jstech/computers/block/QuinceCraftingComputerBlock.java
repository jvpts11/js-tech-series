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
import dev.jstech.computers.hardware.CaseGlass;
import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.hardware.QuinceCase;
import dev.jstech.core.connect.FaceRule;
import org.jetbrains.annotations.Nullable;

/**
 * One of Quince's Crafting Computers (computers/docs/design/quince.md): a Crafting Computer in the case of the Quince
 * desk machine of its era, drawn by that machine's model, which takes only its own logic board. The Studio's takes its
 * peripherals and its access line only from behind, with its power button there; a tower's takes them as a Crafting
 * Computer does, with its button on its front. The crafting cable goes on any face of either, as on every Crafting
 * Computer.
 */
public class QuinceCraftingComputerBlock extends CraftingComputerBlock {

    private final QuinceCase machine;

    public static final MapCodec<QuinceCraftingComputerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            QuinceComputerBlock.MACHINE.fieldOf("machine").forGetter(QuinceCraftingComputerBlock::machine)
    ).apply(i, QuinceCraftingComputerBlock::new));

    public QuinceCraftingComputerBlock(final Properties properties, final QuinceCase machine) {
        super(properties, machine.era(), CaseStyle.SOLE, machine.front(),
                machine.cablesBehind() ? FaceRule.BACK : FaceRule.EVERY);
        this.machine = machine;
    }

    /** The Quince desk machine whose case it wears. */
    public QuinceCase machine() {
        return machine;
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
    protected MapCodec<? extends QuinceCraftingComputerBlock> codec() {
        return CODEC;
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
}
