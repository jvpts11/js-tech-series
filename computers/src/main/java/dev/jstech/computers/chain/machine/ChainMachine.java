/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.machine.ProcessingMachineBlockEntity.Layout;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One machine of the chain (computers/docs/design/survival.md): its id and English name, the era it belongs to when
 * each era has its own (the Lithography Station, the Electronics Assembly Line), the seconds a step and the watts it
 * draws, its slots and tanks, and its shape: its layers from the bottom, each row from its front, each row west to
 * east as it stands facing north, every block a letter of its legend and the terminal {@code #}.
 *
 * @param id      its id, which its pattern, its model and its terminal are named by
 * @param english its name in English
 * @param era     the era of a machine each era has its own of; none for the others
 * @param seconds the seconds a step takes, the time its recipes are written with; none for an Electronics Assembly
 *                Line, which takes each bench recipe's own
 * @param watts   what it draws while it works, the most of a station of two eras; none for the Coke Oven, which
 *                burns part of its own coal
 * @param layout  its slots and tanks
 * @param layers  its shape, layer by layer from the bottom
 * @param legend  the block each letter of its shape stands for, by the block's id in the mod, in its written order
 * @param linked  whether it works only linked to a computer of the era before, by its peripheral port
 */
public record ChainMachine(String id, String english, @Nullable Era era, int seconds, int watts, Layout layout,
                           List<List<String>> layers, Map<Character, String> legend, boolean linked) {

    public ChainMachine {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(english, "english");
        Objects.requireNonNull(layout, "layout");
        layers = layers.stream().map(List::copyOf).toList();
        // In the order it is written, so what reads it (the manual's pages among them) reads it the same every run.
        legend = Collections.unmodifiableMap(new LinkedHashMap<>(legend));
    }

    /** The id of its shape, as J's Core keeps it. */
    public ResourceLocation patternId() {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "chain/" + id);
    }

    /** The id of the model it is drawn with once formed. */
    public ResourceLocation modelId() {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "chain/" + id);
    }

    /** The path of its own terminal, the block the Machine Terminal becomes once it stands at the shape's {@code #}. */
    public String terminalPath() {
        return id + "_terminal";
    }

    /** Whether it draws energy: every machine but the Coke Oven. */
    public boolean takesEnergy() {
        return watts > 0;
    }

    /** What it spends a tick while it works, in joules: its watts over the game's twenty ticks a second. */
    public long joulesATick() {
        return watts / 20L;
    }

    /** How many blocks of its shape are {@code key}'s. */
    public int count(final String key) {
        int count = 0;
        for (final List<String> layer : layers) {
            for (final String row : layer) {
                for (final char c : row.toCharArray()) {
                    if (key.equals(legend.get(c))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
