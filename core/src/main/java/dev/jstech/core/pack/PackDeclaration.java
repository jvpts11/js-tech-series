/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.pack;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;

/**
 * One thing a content pack declares, as its file says it ({@link PackReader}): a block, an item, a material with its
 * forms and ores, a kind of processing machine, the controller block of a machine declared as data, or the creative
 * tab the pack's things are shown in. Every id is {@code namespace:path}, the namespace the pack's own.
 */
public sealed interface PackDeclaration {

    /** The id of what is declared. */
    String id();

    /** The English it is called, which a resource pack's language files may put in other words. */
    String name();

    /**
     * A plain block.
     *
     * @param strength   how long it takes to break, as the game counts it: stone is 1.5, iron 5
     * @param resistance how well it stands a blast: stone is 6
     * @param sound      the sound it makes, by the name of the game's kind: stone, wood, metal, glass...
     * @param light      the light it gives, from 0 to 15
     * @param tool       the tool that breaks it fast, pickaxe, axe, shovel or hoe; empty for none
     * @param needs      the tool's tier it needs to drop anything, wood, stone, iron or diamond; empty when any hand
     *                   breaks it into its drop
     * @param item       whether it has an item to place it
     */
    record Block(String id, String name, float strength, float resistance, String sound, int light, String tool,
                 String needs, boolean item) implements PackDeclaration {
    }

    /**
     * A plain item.
     *
     * @param stack how many make a stack, from 1 to 99
     */
    record Item(String id, String name, int stack) implements PackDeclaration {
    }

    /**
     * A material of the shared catalogue, which the Core gives its things.
     *
     * @param kind       what it is, by its kind's name: metal, crystal, gem, nonmetal_solid, mineral, liquid, gas,
     *                   exotic
     * @param colour     its colour, as {@code 0xRRGGBB}, which its things are drawn in
     * @param liquid     whether it also melts, so it has a liquid
     * @param forms      the forms it is used in, by their ids ({@code ingot} or {@code jscore:ingot})
     * @param ores       the rocks its ore is in, by their ids ({@code stone}, the game's own, or {@code mymod:marble});
     *                   none for no ore
     * @param properties its properties by their ids, each as its property's file writes it
     */
    record Material(String id, String name, String kind, int colour, boolean liquid, List<String> forms,
                    List<String> ores, Map<String, JsonElement> properties) implements PackDeclaration {

        public Material {
            forms = List.copyOf(forms);
            ores = List.copyOf(ores);
            properties = Map.copyOf(properties);
        }
    }

    /**
     * A kind of processing machine: a recipe type whose recipes are the Core's processing recipes.
     *
     * @param machine the id of the item that works it, shown beside its recipes; empty for none
     */
    record Processing(String id, String name, String machine) implements PackDeclaration {
    }

    /**
     * The controller block of a multiblock machine declared as data, which reads the machine file of its own id
     * ({@code data/<namespace>/multiblock_machine/<path>.json}).
     *
     * @param strength how long it takes to break
     */
    record Machine(String id, String name, float strength) implements PackDeclaration {
    }

    /**
     * The creative tab the pack's things are shown in, one a namespace.
     *
     * @param icon the id of the item drawn on it; empty for the first thing the pack declares
     */
    record Tab(String id, String name, String icon) implements PackDeclaration {
    }
}
