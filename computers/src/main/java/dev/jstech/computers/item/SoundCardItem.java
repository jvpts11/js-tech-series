/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A sound card, named in its tooltip by what it plays: how it makes notes and how many at once, how it plays a
 * recording, the slot it fits, and its era.
 */
@TextHolder
public class SoundCardItem extends SpecItem<SoundCardSpec> implements IExpansionCardItem {

    private static final TextKey FM = TextKey.of("jsc.item.sound_card.fm", "FM synthesis, %s voices");
    private static final TextKey WAVETABLE = TextKey.of("jsc.item.sound_card.wavetable", "Wavetable, %s voices");
    private static final TextKey RECORDINGS =
            TextKey.of("jsc.item.sound_card.recordings", "Recordings: %s-bit %s, %s");
    private static final TextKey MONO = TextKey.of("jsc.item.sound_card.mono", "mono");
    private static final TextKey STEREO = TextKey.of("jsc.item.sound_card.stereo", "stereo");
    private static final TextKey KHZ_22 = TextKey.of("jsc.item.sound_card.khz_22", "22 kHz");
    private static final TextKey KHZ_44 = TextKey.of("jsc.item.sound_card.khz_44", "44.1 kHz");
    private static final TextKey FITS_ISA = TextKey.of("jsc.item.sound_card.fits_isa", "Fits an ISA slot");
    private static final TextKey FITS_PCI = TextKey.of("jsc.item.sound_card.fits_pci", "Fits a PCI slot");
    private static final TextKey FITS_AGP = TextKey.of("jsc.item.sound_card.fits_agp", "Fits an AGP slot");
    private static final TextKey FITS_PCIE = TextKey.of("jsc.item.sound_card.fits_pcie", "Fits a PCIe slot");

    public SoundCardItem(final Properties properties, final SoundCardSpec spec) {
        super(properties, spec);
    }

    @Override
    public IExpansionCardSpec cardSpec() {
        return spec();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final SoundCardSpec spec = spec();
        final TextKey synthesis = switch (spec.synthesis()) {
            case FM -> FM;
            case WAVETABLE -> WAVETABLE;
        };
        tooltip.add(GameText.component(synthesis.with(spec.voices())).withStyle(ChatFormatting.GRAY));
        final TextKey rate = switch (spec.rate()) {
            case KHZ_22 -> KHZ_22;
            case KHZ_44 -> KHZ_44;
        };
        tooltip.add(GameText.component(RECORDINGS.with(spec.sampleBits(), spec.stereo() ? STEREO : MONO, rate))
                .withStyle(ChatFormatting.GRAY));
        final TextKey fits = switch (spec.bus().busFamily()) {
            case ISA -> FITS_ISA;
            case PCI -> FITS_PCI;
            case AGP -> FITS_AGP;
            case PCIE -> FITS_PCIE;
        };
        tooltip.add(GameText.component(fits).withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
