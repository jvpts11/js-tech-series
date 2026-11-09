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
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;

/**
 * The Quince Mainframe (computers/docs/design/quince.md), from the Transition on: a Mainframe in every way but its
 * cabinet, the Mainframe's own in Quince's dress, and the board it takes, Quince's logic board for the Mainframe of its
 * era alone.
 */
public class QuinceMainframeBlock extends MainframeBlock {

    public static final MapCodec<QuinceMainframeBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(QuinceMainframeBlock::era)
    ).apply(i, QuinceMainframeBlock::new));

    public QuinceMainframeBlock(final Properties properties, final Era era) {
        super(properties, era);
    }

    @Override
    public String cabinetModel() {
        return "quince_mainframe_" + era().path();
    }

    @Override
    protected MapCodec<? extends QuinceMainframeBlock> codec() {
        return CODEC;
    }
}
