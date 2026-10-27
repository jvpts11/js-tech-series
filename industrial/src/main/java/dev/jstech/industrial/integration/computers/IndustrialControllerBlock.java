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
import dev.jstech.computers.block.CaseStyle;
import dev.jstech.computers.block.SmallComputerBlock;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.industrial.JsIndustrial;

/**
 * The Industrial Controller Computer: a computer like a Personal Computer of its era (a system, programs, the data
 * network, a screen), in the cases J's Computers ships, whose Fieldbus Cards read the machines on its industrial
 * control cables and command them. Its case is drawn from this mod's own models: the panel under the front with its
 * RUN, BUS and FAULT lamps, the LOCAL/REMOTE key and the terminal strip.
 */
public class IndustrialControllerBlock extends SmallComputerBlock {

    public static final MapCodec<IndustrialControllerBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(IndustrialControllerBlock::era),
            StableCodecs.byName(CaseStyle.class).fieldOf("case").forGetter(IndustrialControllerBlock::caseStyle)
    ).apply(i, IndustrialControllerBlock::new));

    public IndustrialControllerBlock(final Properties properties, final Era era, final CaseStyle caseStyle) {
        super(properties, era, caseStyle);
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
    protected MapCodec<? extends IndustrialControllerBlock> codec() {
        return CODEC;
    }
}
