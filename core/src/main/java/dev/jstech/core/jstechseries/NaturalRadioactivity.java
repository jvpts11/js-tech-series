/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialMarks;
import dev.jstech.core.material.Mineral;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Every mineral with uranium or thorium in it is naturally radioactive, at the lowest level of the Core's radiation,
 * unless the world turns that off: uraninite, carnotite, monazite and the rest. Its things say so in their tooltips; the
 * dose they give comes with the Core's radiation.
 */
@TextHolder
public final class NaturalRadioactivity {

    public static final TextKey MARK = TextKey.of("jscore.material.mark.radioactive", "Naturally radioactive");

    /** The mark the radioactive minerals bear. */
    public static final MaterialMarks.Mark RADIOACTIVE = MaterialMarks.declare(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "naturally_radioactive"), MARK,
            NaturalRadioactivity::radioactive);

    private NaturalRadioactivity() {
    }

    /** Declares the mark, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Whether {@code material} is a naturally radioactive mineral, in a world that has them. */
    public static boolean radioactive(final Material material) {
        return on() && bearsUraniumOrThorium(material);
    }

    /** Whether the world has naturally radioactive minerals: radiation on, and the minerals' own setting. */
    public static boolean on() {
        return HazardBalance.radiation() && HazardBalance.naturalRadioactivity();
    }

    /** Whether {@code material} is a mineral with uranium or thorium in it, whatever the world's settings. */
    public static boolean bearsUraniumOrThorium(final Material material) {
        final Optional<Mineral> mineral = material.mineral();
        if (mineral.isEmpty()) {
            return false;
        }
        for (final Mineral.Content content : mineral.get().contents()) {
            final Material inside = content.material().isotope().map(Material.Isotope::element)
                    .orElse(content.material());
            if (inside == Elements.of(Element.URANIUM) || inside == Elements.of(Element.THORIUM)) {
                return true;
            }
        }
        return false;
    }
}
