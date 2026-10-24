/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.research;

import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.research.DataKind;
import dev.jstech.core.research.DataKinds;
import dev.jstech.core.research.ResearchDataItem;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.resources.ResourceLocation;

/**
 * J's Industrial's kinds of data, each the record its instruments really leave, and an item for each: sample data, what
 * a material is made of; crystal data, the structure of a solid; spectral data, the lines of light of each element;
 * particle data, what an accelerator's runs show. A discovery asks for the kinds its science needs and spends them a
 * part of the way at a time (industrial/docs/design/science.md).
 */
@TextHolder
public final class IndustrialResearch {

    public static final TextKey SAMPLE_NAME = TextKey.of("jsindustrial.data.sample", "Sample data");
    public static final TextKey CRYSTAL_NAME = TextKey.of("jsindustrial.data.crystal", "Crystal data");
    public static final TextKey SPECTRAL_NAME = TextKey.of("jsindustrial.data.spectral", "Spectral data");
    public static final TextKey PARTICLE_NAME = TextKey.of("jsindustrial.data.particle", "Particle data");
    /* Which instruments give each kind, as the item's tooltip says it. */
    public static final TextKey SAMPLE_FROM = TextKey.of("jsindustrial.data.sample.from",
            "From the Mass Spectrometer and the ICP Mass Spectrometer: what a material is made of, and its isotopes.");
    public static final TextKey CRYSTAL_FROM = TextKey.of("jsindustrial.data.crystal.from",
            "From the X-ray Diffractometer, the Electron Microscope and a synchrotron's light: the structure of a"
                    + " solid.");
    public static final TextKey SPECTRAL_FROM = TextKey.of("jsindustrial.data.spectral.from",
            "From the Optical Spectrometer and the NMR Spectrometer: the lines of light of each element.");
    public static final TextKey PARTICLE_FROM = TextKey.of("jsindustrial.data.particle.from",
            "From a Particle Detector set against an accelerator, at each of its runs.");

    public static final DataKind SAMPLE = DataKinds.declare(new DataKind(id("sample"), SAMPLE_NAME));
    public static final DataKind CRYSTAL = DataKinds.declare(new DataKind(id("crystal"), CRYSTAL_NAME));
    public static final DataKind SPECTRAL = DataKinds.declare(new DataKind(id("spectral"), SPECTRAL_NAME));
    public static final DataKind PARTICLE = DataKinds.declare(new DataKind(id("particle"), PARTICLE_NAME));

    /** A mass spectrum on chart paper. */
    public static final ItemEntry<ResearchDataItem> SAMPLE_DATA = data("sample_data", SAMPLE, SAMPLE_FROM)
            .named("Sample Data").register();
    /** An X-ray diffraction film. */
    public static final ItemEntry<ResearchDataItem> CRYSTAL_DATA = data("crystal_data", CRYSTAL, CRYSTAL_FROM)
            .named("Crystal Data").register();
    /** A spectrographic glass plate. */
    public static final ItemEntry<ResearchDataItem> SPECTRAL_DATA = data("spectral_data", SPECTRAL, SPECTRAL_FROM)
            .named("Spectral Data").register();
    /** A reel of bubble chamber film. */
    public static final ItemEntry<ResearchDataItem> PARTICLE_DATA = data("particle_data", PARTICLE, PARTICLE_FROM)
            .named("Particle Data").register();

    private IndustrialResearch() {
    }

    /** Declares the kinds and their items, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /* The item of a kind of data, its tooltip saying what gives it. */
    private static ItemBuilder<ResearchDataItem> data(final String path, final DataKind kind, final TextKey from) {
        return IndustrialModule.CONTENT.item(path, properties -> new ResearchDataItem(properties, kind, from))
                .look(IItemLook.FLAT).tab(IndustrialModule.RESEARCH);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }
}
