/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;

/**
 * Every mineral family declared, open to any mod: the Core's, and those a mod adds, such as the families of another
 * world's minerals.
 *
 * <pre>{@code
 * public static final MineralFamily BORATE = MineralFamilies.declare(rl("borate"), BORATE_NAME);
 * }</pre>
 */
@TextHolder
public final class MineralFamilies {

    private static final Map<ResourceLocation, MineralFamily> BY_ID = new ConcurrentHashMap<>();
    private static final List<MineralFamily> ALL = new CopyOnWriteArrayList<>();

    public static final TextKey SULFIDE_NAME = TextKey.of("jscore.mineral_family.sulfide", "Sulfide");
    public static final TextKey OXIDE_NAME = TextKey.of("jscore.mineral_family.oxide", "Oxide");
    public static final TextKey CARBONATE_NAME = TextKey.of("jscore.mineral_family.carbonate", "Carbonate");
    public static final TextKey SILICATE_NAME = TextKey.of("jscore.mineral_family.silicate", "Silicate");
    public static final TextKey ARSENIDE_NAME = TextKey.of("jscore.mineral_family.arsenide", "Arsenide");
    public static final TextKey NATIVE_NAME = TextKey.of("jscore.mineral_family.native",
            "Native metal or natural alloy");
    public static final TextKey HALIDE_NAME = TextKey.of("jscore.mineral_family.halide", "Halide");
    public static final TextKey SULFATE_NAME = TextKey.of("jscore.mineral_family.sulfate", "Sulfate");
    public static final TextKey PHOSPHATE_NAME = TextKey.of("jscore.mineral_family.phosphate", "Phosphate");
    public static final TextKey ARSENATE_NAME = TextKey.of("jscore.mineral_family.arsenate", "Arsenate");
    public static final TextKey VANADATE_NAME = TextKey.of("jscore.mineral_family.vanadate", "Vanadate");

    /** Roasted, which gives sulfur dioxide: chalcopyrite, galena, sphalerite. */
    public static final MineralFamily SULFIDE = declare("sulfide", SULFIDE_NAME);
    /** Reduced: hematite, cassiterite, chromite. */
    public static final MineralFamily OXIDE = declare("oxide", OXIDE_NAME);
    /** Calcined, which gives carbon dioxide: malachite, cerussite, smithsonite. */
    public static final MineralFamily CARBONATE = declare("carbonate", CARBONATE_NAME);
    /** Harder to break down: hemimorphite, zircon, beryl. */
    public static final MineralFamily SILICATE = declare("silicate", SILICATE_NAME);
    /** Gives arsenic, which is toxic: cobaltite, nickeline, arsenopyrite. */
    public static final MineralFamily ARSENIDE = declare("arsenide", ARSENIDE_NAME);
    /** Needs no breaking down: electrum, native bismuth. */
    public static final MineralFamily NATIVE = declare("native", NATIVE_NAME);
    public static final MineralFamily HALIDE = declare("halide", HALIDE_NAME);
    public static final MineralFamily SULFATE = declare("sulfate", SULFATE_NAME);
    public static final MineralFamily PHOSPHATE = declare("phosphate", PHOSPHATE_NAME);
    public static final MineralFamily ARSENATE = declare("arsenate", ARSENATE_NAME);
    public static final MineralFamily VANADATE = declare("vanadate", VANADATE_NAME);

    private MineralFamilies() {
    }

    /**
     * Declares the family {@code id}, called {@code name}.
     *
     * @throws DeclarationError when a family of that id is declared already
     */
    public static MineralFamily declare(final ResourceLocation id, final TextKey name) {
        final MineralFamily family = new MineralFamily(id, name);
        if (BY_ID.putIfAbsent(id, family) != null) {
            throw DeclarationError.of(id.getNamespace(), "the mineral family " + id, "is declared twice",
                    "declare it once, as a static final");
        }
        ALL.add(family);
        return family;
    }

    /** Every family declared, in the order they were. */
    public static List<MineralFamily> all() {
        return List.copyOf(ALL);
    }

    /** The family declared as {@code id}, or none. */
    public static Optional<MineralFamily> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** Declares the Core's families, from its constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static MineralFamily declare(final String path, final TextKey name) {
        return declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), name);
    }
}
