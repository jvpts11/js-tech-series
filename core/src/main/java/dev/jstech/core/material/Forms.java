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
 * Every form a material can take, open to any mod: the Core's twenty-one, and those a mod adds when it has a real use
 * for one. A form only has things in the game for the materials an installed mod says it uses
 * ({@link MaterialUses}).
 *
 * <pre>{@code
 * public static final Form PELLET = Forms.form(rl("pellet"), Form.Shape.ITEM, "pellets", PELLET_NAME)
 *         .fits(MaterialKind.METAL, MaterialKind.NONMETAL_SOLID).declare();
 * }</pre>
 */
@TextHolder
public final class Forms {

    private static final Map<ResourceLocation, Form> BY_ID = new ConcurrentHashMap<>();
    private static final List<Form> ALL = new CopyOnWriteArrayList<>();

    public static final TextKey INGOT_NAME = TextKey.of("jscore.form.ingot", "%s Ingot");
    public static final TextKey NUGGET_NAME = TextKey.of("jscore.form.nugget", "%s Nugget");
    public static final TextKey BLOCK_NAME = TextKey.of("jscore.form.block", "Block of %s");
    public static final TextKey DUST_NAME = TextKey.of("jscore.form.dust", "%s Dust");
    public static final TextKey PLATE_NAME = TextKey.of("jscore.form.plate", "%s Plate");
    public static final TextKey SHEET_NAME = TextKey.of("jscore.form.sheet", "%s Sheet");
    public static final TextKey WIRE_NAME = TextKey.of("jscore.form.wire", "%s Wire");
    public static final TextKey ROD_NAME = TextKey.of("jscore.form.rod", "%s Rod");
    public static final TextKey BOLT_NAME = TextKey.of("jscore.form.bolt", "%s Bolt");
    public static final TextKey GEAR_NAME = TextKey.of("jscore.form.gear", "%s Gear");
    public static final TextKey COIL_NAME = TextKey.of("jscore.form.coil", "%s Coil");
    public static final TextKey SPRING_NAME = TextKey.of("jscore.form.spring", "%s Spring");
    public static final TextKey FOIL_NAME = TextKey.of("jscore.form.foil", "%s Foil");
    public static final TextKey FRAME_NAME = TextKey.of("jscore.form.frame", "%s Frame");
    public static final TextKey LENS_NAME = TextKey.of("jscore.form.lens", "%s Lens");
    public static final TextKey BOULE_NAME = TextKey.of("jscore.form.boule", "%s Boule");
    public static final TextKey WAFER_NAME = TextKey.of("jscore.form.wafer", "%s Wafer");
    public static final TextKey CRYSTAL_NAME = TextKey.of("jscore.form.crystal", "%s Crystal");
    /* A gem is called by its material's name alone: a ruby. */
    public static final TextKey GEM_NAME = TextKey.of("jscore.form.gem", "%s");
    public static final TextKey ORE_NAME = TextKey.of("jscore.form.ore", "%s Ore");
    public static final TextKey RAW_NAME = TextKey.of("jscore.form.raw", "Raw %s");

    public static final Form INGOT = form("ingot", Form.Shape.ITEM, "ingots", INGOT_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form NUGGET = form("nugget", Form.Shape.ITEM, "nuggets", NUGGET_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form BLOCK = form("block", Form.Shape.BLOCK, "storage_blocks", BLOCK_NAME)
            .fits(MaterialKind.METAL, MaterialKind.GEM, MaterialKind.CRYSTAL, MaterialKind.NONMETAL_SOLID).declare();
    public static final Form DUST = form("dust", Form.Shape.ITEM, "dusts", DUST_NAME)
            .fits(MaterialKind.METAL, MaterialKind.CRYSTAL, MaterialKind.GEM, MaterialKind.NONMETAL_SOLID).declare();
    public static final Form PLATE = form("plate", Form.Shape.ITEM, "plates", PLATE_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form SHEET = form("sheet", Form.Shape.ITEM, "sheets", SHEET_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form WIRE = form("wire", Form.Shape.ITEM, "wires", WIRE_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form ROD = form("rod", Form.Shape.ITEM, "rods", ROD_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form BOLT = form("bolt", Form.Shape.ITEM, "bolts", BOLT_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form GEAR = form("gear", Form.Shape.ITEM, "gears", GEAR_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form COIL = form("coil", Form.Shape.ITEM, "coils", COIL_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form SPRING = form("spring", Form.Shape.ITEM, "springs", SPRING_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form FOIL = form("foil", Form.Shape.ITEM, "foils", FOIL_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form FRAME = form("frame", Form.Shape.ITEM, "frames", FRAME_NAME)
            .fits(MaterialKind.METAL).declare();
    public static final Form LENS = form("lens", Form.Shape.ITEM, "lenses", LENS_NAME)
            .fits(MaterialKind.CRYSTAL).declare();
    public static final Form BOULE = form("boule", Form.Shape.ITEM, "boules", BOULE_NAME)
            .fits(MaterialKind.CRYSTAL).declare();
    public static final Form WAFER = form("wafer", Form.Shape.ITEM, "wafers", WAFER_NAME)
            .fits(MaterialKind.CRYSTAL).declare();
    public static final Form CRYSTAL = form("crystal", Form.Shape.ITEM, "crystals", CRYSTAL_NAME)
            .fits(MaterialKind.CRYSTAL).declare();
    public static final Form GEM = form("gem", Form.Shape.ITEM, "gems", GEM_NAME)
            .fits(MaterialKind.GEM).declare();
    /* A mineral's ore, and an element's "X ore" (tin ore), whatever the element's kind. */
    public static final Form ORE = form("ore", Form.Shape.ORE, "ores", ORE_NAME)
            .fits(MaterialKind.MINERAL, MaterialKind.METAL, MaterialKind.GEM, MaterialKind.CRYSTAL,
                    MaterialKind.NONMETAL_SOLID).declare();
    public static final Form RAW = form("raw", Form.Shape.ITEM, "raw_materials", RAW_NAME)
            .fits(MaterialKind.METAL, MaterialKind.MINERAL).declare();

    private Forms() {
    }

    /** A form called {@code id}, of {@code shape}, filed under {@code c:<tagFolder>/...}, named {@code name}. */
    public static Form.Builder form(final ResourceLocation id, final Form.Shape shape, final String tagFolder,
                                    final TextKey name) {
        return new Form.Builder(id, shape, tagFolder, name);
    }

    /** Every form declared, in the order they were. */
    public static List<Form> all() {
        return List.copyOf(ALL);
    }

    /** The form declared as {@code id}, or none. */
    public static Optional<Form> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** Declares the Core's forms, from its constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    static Form declare(final Form form) {
        if (BY_ID.putIfAbsent(form.id(), form) != null) {
            throw DeclarationError.of(form.id().getNamespace(), "the form " + form.id(), "is declared twice",
                    "declare it once, as a static final");
        }
        ALL.add(form);
        return form;
    }

    private static Form.Builder form(final String path, final Form.Shape shape, final String tagFolder,
                                     final TextKey name) {
        return form(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path), shape, tagFolder, name);
    }
}
