/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import dev.jstech.core.text.TextKey;
import dev.jstech.space.api.RocketPart;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The parts on the Rocket Design screen's bins: every part the world knows, sorted into the six categories of its
 * tabs, by diameter within each, and numbered through the whole catalogue as a workshop numbers its bins.
 */
public final class PartCatalogue {

    /** The categories, in the order of their tabs. */
    public static final List<Category> CATEGORIES = List.of(Category.COMMAND, Category.TANKS, Category.ENGINES,
            Category.RINGS, Category.LANDERS, Category.RECOVERY);

    private PartCatalogue() {
    }

    /** The catalogue of {@code parts}, numbered from one. */
    public static List<Entry> of(final Map<ResourceLocation, RocketPart> parts) {
        final List<Map.Entry<ResourceLocation, RocketPart>> sorted = new ArrayList<>(parts.entrySet());
        sorted.sort(Comparator.<Map.Entry<ResourceLocation, RocketPart>>comparingInt(entry -> CATEGORIES.indexOf(
                        category(entry.getValue().kind())))
                .thenComparingInt(entry -> entry.getValue().diameter())
                .thenComparing(entry -> entry.getValue().kind().serializedName())
                .thenComparing(entry -> entry.getKey().toString()));
        final List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            final Map.Entry<ResourceLocation, RocketPart> entry = sorted.get(i);
            entries.add(new Entry(entry.getKey(), entry.getValue(), category(entry.getValue().kind()), i + 1));
        }
        return List.copyOf(entries);
    }

    /** The category a part of {@code kind} is kept in. */
    public static Category category(final RocketPart.Kind kind) {
        return switch (kind) {
            case TANK, SOLID_BOOSTER -> Category.TANKS;
            case ENGINE, ENGINE_MOUNT -> Category.ENGINES;
            case SEPARATION_RING, RADIAL_SEPARATOR, ADAPTER, INSTRUMENT_RING, CONTROL_THRUSTERS -> Category.RINGS;
            case LANDER_DESCENT, LANDER_ASCENT -> Category.LANDERS;
            case LANDING_LEGS, GRID_FINS -> Category.RECOVERY;
            case CAPSULE, ESCAPE_TOWER, HABITATION, SERVICE_MODULE, DOCKING_PORT, FAIRING, PAYLOAD_ADAPTER,
                 CARGO_CONTAINER -> Category.COMMAND;
        };
    }

    /** Whether a part of {@code kind} hangs at another's side, in a ring the mirror makes, rather than on top. */
    public static boolean hangs(final RocketPart.Kind kind) {
        return kind == RocketPart.Kind.SOLID_BOOSTER;
    }

    /** A tab of the bins: what it holds, its icon, its name. */
    public enum Category {
        COMMAND(RocketDesignArt.CAPSULE, RocketDesignTexts.COMMAND),
        TANKS(RocketDesignArt.TANK, RocketDesignTexts.TANKS),
        ENGINES(RocketDesignArt.ENGINE, RocketDesignTexts.ENGINES),
        RINGS(RocketDesignArt.RING, RocketDesignTexts.RINGS),
        LANDERS(RocketDesignArt.LEGS, RocketDesignTexts.LANDERS),
        RECOVERY(RocketDesignArt.CHUTE, RocketDesignTexts.RECOVERY);

        private final int icon;
        private final TextKey title;

        Category(final int icon, final TextKey title) {
            this.icon = icon;
            this.title = title;
        }

        /** Its icon's cell. */
        public int icon() {
            return this.icon;
        }

        /** Its name, written over its bins. */
        public TextKey title() {
            return this.title;
        }
    }

    /**
     * A part in the catalogue.
     *
     * @param id       its id
     * @param part     the part
     * @param category its tab
     * @param number   its number through the whole catalogue, from one
     */
    public record Entry(ResourceLocation id, RocketPart part, Category category, int number) {
    }
}
