/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.ClusterInterfaceCardSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Cluster Interface Card family: exclusive to the Cluster Management Computer, one per machine.
 * The card decides which cabinets the machine can drive and how many nodes it installs at once.
 */
@TextHolder
public class ClusterInterfaceCardItem extends SpecItem<ClusterInterfaceCardSpec> implements IExpansionCardItem {

    private static final TextKey PURPOSE = TextKey.of("jsc.item.cluster_interface_card.purpose",
            "Cluster interface for a Cluster Management Computer");
    private static final TextKey REACHES = TextKey.of("jsc.item.cluster_interface_card.reaches", "Reaches  -  %s");
    private static final TextKey REACH_DATACENTERS =
            TextKey.of("jsc.item.cluster_interface_card.reach_datacenters", "datacenters");
    private static final TextKey REACH_SUPERCOMPUTERS = TextKey.of(
            "jsc.item.cluster_interface_card.reach_supercomputers", "datacenters and supercomputers");
    private static final TextKey REACH_ALL = TextKey.of("jsc.item.cluster_interface_card.reach_all",
            "datacenters, supercomputers and AI clusters");
    private static final TextKey INSTALLS_ONE =
            TextKey.of("jsc.item.cluster_interface_card.installs_one", "Installs  -  %s node at a time");
    private static final TextKey INSTALLS_MANY =
            TextKey.of("jsc.item.cluster_interface_card.installs_many", "Installs  -  %s nodes in parallel");

    public ClusterInterfaceCardItem(final Properties properties, final ClusterInterfaceCardSpec spec) {
        super(properties.stacksTo(16), spec);
    }

    @Override
    public IExpansionCardSpec cardSpec() {
        return spec();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final ClusterInterfaceCardSpec spec = spec();
        tooltip.add(GameText.component(PURPOSE).withStyle(ChatFormatting.GRAY));
        final TextKey reach = switch (spec.reach()) {
            case DATACENTERS -> REACH_DATACENTERS;
            case SUPERCOMPUTERS -> REACH_SUPERCOMPUTERS;
            case ALL -> REACH_ALL;
        };
        tooltip.add(GameText.component(REACHES.with(reach)).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component((spec.parallelNodes() == 1 ? INSTALLS_ONE : INSTALLS_MANY)
                .with(spec.parallelNodes())).withStyle(ChatFormatting.GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
        tooltip.add(GameText.component(HardwareTooltip.TIER_POWER_BUS.with(spec.tier(), spec.tdpWatts(), spec.bus()))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
