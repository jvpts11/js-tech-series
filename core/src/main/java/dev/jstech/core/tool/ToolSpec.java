/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import dev.jstech.core.item.ItemMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What an item does as a tool, declared with it: what it mines besides the block it breaks, in each of its modes or in
 * all; what it builds; what it works on; how it heats; how long it waits between uses; and how long it charges.
 *
 * <pre>{@code
 * CONTENT.item("atomic_drill", Item::new).named("Atomic Drill")
 *         .modes(SINGLE, AREA, VEIN).holdsEnergy(1_000_000L, 10_000, 0)
 *         .tool(ToolSpec.builder().mines(AREA, MiningShape.area(3, 3, 1)).mines(VEIN, MiningShape.vein(64))
 *                 .runsOn(ToolPower.energy(400)).heats(new ToolHeat(100, 2, 10)).build())
 *         .register();
 * }</pre>
 *
 * @param mining       what it mines besides the block broken, by the id of the mode it does it in
 * @param minesAlways  what it mines in a mode not among those, or with no modes
 * @param building     what it builds, by the id of the mode it does it in
 * @param buildsAlways what it builds in a mode not among those, or with no modes
 * @param power        what it works on; none for a tool that needs nothing
 * @param heat         how it heats; none for one that does not
 * @param cooldown     how many ticks it waits after a use before the next
 * @param charge       how many ticks it takes to charge fully while held in use; 0 for none
 */
public record ToolSpec(Map<String, MiningShape> mining, Optional<MiningShape> minesAlways,
                       Map<String, BuildMode> building, Optional<BuildMode> buildsAlways, Optional<ToolPower> power,
                       Optional<ToolHeat> heat, int cooldown, int charge) {

    public ToolSpec {
        mining = Map.copyOf(mining);
        building = Map.copyOf(building);
        if (cooldown < 0 || charge < 0) {
            throw new IllegalArgumentException("a tool waits and charges for no less than nothing");
        }
    }

    /** Starts declaring a tool. */
    public static Builder builder() {
        return new Builder();
    }

    /** What it mines besides the block broken in the mode saved as {@code mode}, or none. */
    public Optional<MiningShape> mines(final Optional<ItemMode> mode) {
        final MiningShape given = mode.map(ItemMode::id).map(this.mining::get).orElse(null);
        return given != null ? Optional.of(given) : this.minesAlways;
    }

    /** What it builds in the mode saved as {@code mode}, or none. */
    public Optional<BuildMode> builds(final Optional<ItemMode> mode) {
        final BuildMode given = mode.map(ItemMode::id).map(this.building::get).orElse(null);
        return given != null ? Optional.of(given) : this.buildsAlways;
    }

    /** Declares a tool one property at a time. */
    public static final class Builder {

        private final Map<String, MiningShape> mining = new LinkedHashMap<>();
        private final Map<String, BuildMode> building = new LinkedHashMap<>();
        private Optional<MiningShape> minesAlways = Optional.empty();
        private Optional<BuildMode> buildsAlways = Optional.empty();
        private Optional<ToolPower> power = Optional.empty();
        private Optional<ToolHeat> heat = Optional.empty();
        private int cooldown;
        private int charge;

        private Builder() {
        }

        /** In {@code mode}, it mines {@code shape} besides the block broken. */
        public Builder mines(final ItemMode mode, final MiningShape shape) {
            this.mining.put(mode.id(), Objects.requireNonNull(shape, "shape"));
            return this;
        }

        /** In every mode not given its own, or with no modes, it mines {@code shape} besides the block broken. */
        public Builder mines(final MiningShape shape) {
            this.minesAlways = Optional.of(shape);
            return this;
        }

        /** In {@code mode}, it builds as {@code build} says. */
        public Builder builds(final ItemMode mode, final BuildMode build) {
            this.building.put(mode.id(), Objects.requireNonNull(build, "build"));
            return this;
        }

        /** In every mode not given its own, or with no modes, it builds as {@code build} says. */
        public Builder builds(final BuildMode build) {
            this.buildsAlways = Optional.of(build);
            return this;
        }

        /** It works on {@code on}, spent each use; out of it, it does no more than a bare hand. */
        public Builder runsOn(final ToolPower on) {
            this.power = Optional.of(on);
            return this;
        }

        /** It heats as {@code heats} says. */
        public Builder heats(final ToolHeat heats) {
            this.heat = Optional.of(heats);
            return this;
        }

        /** It waits {@code ticks} after a use before the next, the wait the game shows over its icon. */
        public Builder cooldown(final int ticks) {
            this.cooldown = ticks;
            return this;
        }

        /** Held in use, it charges fully in {@code ticks}. */
        public Builder charges(final int ticks) {
            this.charge = ticks;
            return this;
        }

        public ToolSpec build() {
            return new ToolSpec(this.mining, this.minesAlways, this.building, this.buildsAlways, this.power,
                    this.heat, this.cooldown, this.charge);
        }
    }
}
