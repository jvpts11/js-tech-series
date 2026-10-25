/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.ClusterInterfaceCardSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * What a Cluster Interface Card's tooltip says: which cabinets the Cluster Management Computer it sits in can drive,
 * and how many nodes it installs at once.
 */
@TextHolder
final class ClusterCardTooltip {

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

    private ClusterCardTooltip() {
    }

    static void append(final ClusterInterfaceCardSpec spec, final List<Component> tooltip) {
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
        tooltip.add(GameText.component(HardwareTooltip.TIER_POWER_BUS.with(spec.tier().text(), spec.tdpWatts(),
                spec.bus().label())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
