/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The lines a block of an energy shows: the class of its port, in its item's tooltip and wherever it is looked at, what
 * it holds, and what is wrong with it. An energy with no classes names no class.
 */
public final class EnergyTooltips {

    private EnergyTooltips() {
    }

    /** The line naming a port of {@code portClass} of {@code energy}, or none for an energy with no classes. */
    public static Optional<Text> port(final Energy energy, final EnergyClass portClass) {
        if (energy.classless()) {
            return Optional.empty();
        }
        return Optional.of(portClass.ratingKey().map(rating -> EnergyTexts.PORT_RATED.with(portClass.name().text(),
                rating.text())).orElseGet(() -> EnergyTexts.PORT.with(portClass.name().text())));
    }

    /** The lines of {@code port}: its class and what it holds. */
    public static List<Text> port(final IEnergyPort port) {
        final List<Text> lines = new ArrayList<>(2);
        port(port.energy(), port.portClass()).ifPresent(lines::add);
        lines.add(EnergyTexts.STORED.with(port.energy().amount(port.stored()),
                port.energy().amount(port.capacity())));
        return lines;
    }

    /** The lines of {@code store} in tick {@code gameTime}: its port's, and whether it is damaged or short. */
    public static List<Text> store(final EnergyStore store, final long gameTime) {
        final List<Text> lines = port(store);
        if (store.damaged()) {
            lines.add(EnergyTexts.DAMAGED.text());
        } else if (store.undervoltage(gameTime)) {
            lines.add(EnergyTexts.UNDERVOLTAGE.text());
        }
        return lines;
    }

    /** The lines of a generator: its class and what it gives. */
    public static List<Text> generator(final GeneratorPort generator) {
        final List<Text> lines = new ArrayList<>(2);
        final Energy energy = generator.energy();
        port(energy, generator.portClass()).ifPresent(lines::add);
        final LoadFollower follower = generator.follower();
        lines.add(follower.minimum() > 0 ? EnergyTexts.OUTPUT_MINIMUM.with(energy.rate(follower.minimum()),
                energy.rate(follower.maximum())) : EnergyTexts.OUTPUT.with(energy.rate(follower.maximum())));
        return lines;
    }
}
