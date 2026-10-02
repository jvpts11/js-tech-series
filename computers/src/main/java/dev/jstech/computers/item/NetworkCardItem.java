/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.NetworkCardSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A network adapter, named in its tooltip by what it gives the machine it sits in: the fibre of the backbone.
 */
@TextHolder
public class NetworkCardItem extends SpecItem<NetworkCardSpec> implements IExpansionCardItem {

    private static final TextKey FIBRE =
            TextKey.of("jsc.item.network_card.fibre", "Lets a Mainframe, a rack or a CMC take the fibre backbone");
    private static final TextKey FITS_PCIE = TextKey.of("jsc.item.network_card.fits_pcie", "Fits a PCIe slot");

    public NetworkCardItem(final Properties properties, final NetworkCardSpec spec) {
        super(properties, spec);
    }

    @Override
    public IExpansionCardSpec cardSpec() {
        return spec();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        tooltip.add(GameText.component(FIBRE).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(FITS_PCIE).withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec().era());
    }
}
