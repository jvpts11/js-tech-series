/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.jstech.core.JsCore;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The hazards of a material, as real data: its hazard classes ({@link HazardClass}), which a data pack may change as
 * it may any of a material's values, and what follows from them, the GHS pictograms its things and its safety data
 * sheet show and, for a gas, what a cloud of it does. The tooltip of each of its things lists the pictograms.
 *
 * <pre>{@code
 * Materials.material(rl("phosgene"), MaterialKind.GAS, PHOSGENE)
 *         .with(MaterialHazards.HAZARDS, List.of(HazardClass.ACUTELY_TOXIC, HazardClass.CORROSIVE)).declare();
 * MaterialHazards.pictograms(chlorine, 2 * GasMath.ATMOSPHERE)   // a cylinder of it: the gas cylinder among them
 * }</pre>
 */
@TextHolder
public final class MaterialHazards {

    public static final TextKey HAZARDS_NAME = TextKey.of("jscore.material_property.hazards", "Hazards");
    /* How a hazard class is written in a data pack: "acutely_toxic". */
    private static final Codec<HazardClass> CLASS_CODEC = Codec.STRING.comapFlatMap(
            name -> HazardClass.byName(name).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "no hazard class is called " + name)),
            HazardClass::serializedName);
    /** The material's hazard classes; its line lists the pictograms they call for and the hazards beyond them. */
    public static final MaterialProperty<List<HazardClass>> HAZARDS = MaterialProperties.property(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hazards"), CLASS_CODEC.listOf(), HAZARDS_NAME)
            .format(MaterialHazards::reads).declare();

    private MaterialHazards() {
    }

    /** Declares the property, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** {@code material}'s hazard classes, on the server's side: a data pack's, or its declaration's. */
    public static Set<HazardClass> of(final Material material) {
        return of(material, false);
    }

    /** {@code material}'s hazard classes on the player's side or the server's. */
    public static Set<HazardClass> of(final Material material, final boolean playerSide) {
        final Set<HazardClass> out = EnumSet.noneOf(HazardClass.class);
        MaterialValues.value(material, HAZARDS, playerSide).ifPresent(out::addAll);
        return out;
    }

    /** The pictograms {@code material}'s things show as they are, a gas at the open air's pressure. */
    public static List<GhsPictogram> pictograms(final Material material) {
        return GhsPictograms.of(of(material), false);
    }

    /**
     * The pictograms {@code material} shows held at {@code kilopascals}: as a gas above the open air's pressure, in a
     * cylinder or a pressure vessel, with the gas cylinder among them.
     */
    public static List<GhsPictogram> pictograms(final Material material, final double kilopascals) {
        return GhsPictograms.of(of(material), material.takes(MatterState.GAS) && kilopascals > GasMath.ATMOSPHERE);
    }

    /** What a cloud of {@code material} does once let out; nothing for a material that is never a gas. */
    public static Set<GasHazard> gasHazards(final Material material) {
        return material.takes(MatterState.GAS) ? GasHazard.of(of(material)) : EnumSet.noneOf(GasHazard.class);
    }

    /* The hazards' line: the pictograms by their meaning, then the hazards outside the GHS. */
    private static Text reads(final List<HazardClass> classes) {
        final Set<HazardClass> all = classes.isEmpty() ? EnumSet.noneOf(HazardClass.class) : EnumSet.copyOf(classes);
        final List<Text> words = new ArrayList<>();
        for (final GhsPictogram pictogram : GhsPictograms.of(all, false)) {
            words.add(pictogram.nameKey().text());
        }
        for (final HazardClass beyond : GhsPictograms.beyond(all)) {
            words.add(beyond.nameKey().text());
        }
        return TextLists.join(", ", words);
    }
}
