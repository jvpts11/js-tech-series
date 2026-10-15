/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Every host rock declared, open to any mod: the game's twelve, and those a mod brings, such as limestone or marble.
 *
 * <pre>{@code
 * public static final HostRock LIMESTONE = HostRocks.rock(rl("limestone"), LIMESTONE_BLOCK, LIMESTONE_ORE_NAME)
 *         .declare();
 * }</pre>
 */
@TextHolder
public final class HostRocks {

    private static final Map<ResourceLocation, HostRock> BY_ID = new ConcurrentHashMap<>();
    private static final List<HostRock> ALL = new CopyOnWriteArrayList<>();

    public static final TextKey STONE_ORE = TextKey.of("jscore.host_rock.stone", "%s Ore");
    public static final TextKey DEEPSLATE_ORE = TextKey.of("jscore.host_rock.deepslate", "Deepslate %s Ore");
    public static final TextKey GRANITE_ORE = TextKey.of("jscore.host_rock.granite", "Granite %s Ore");
    public static final TextKey DIORITE_ORE = TextKey.of("jscore.host_rock.diorite", "Diorite %s Ore");
    public static final TextKey ANDESITE_ORE = TextKey.of("jscore.host_rock.andesite", "Andesite %s Ore");
    public static final TextKey TUFF_ORE = TextKey.of("jscore.host_rock.tuff", "Tuff %s Ore");
    public static final TextKey CALCITE_ORE = TextKey.of("jscore.host_rock.calcite", "Calcite %s Ore");
    public static final TextKey SANDSTONE_ORE = TextKey.of("jscore.host_rock.sandstone", "Sandstone %s Ore");
    public static final TextKey NETHERRACK_ORE = TextKey.of("jscore.host_rock.netherrack", "Nether %s Ore");
    public static final TextKey BASALT_ORE = TextKey.of("jscore.host_rock.basalt", "Basalt %s Ore");
    public static final TextKey BLACKSTONE_ORE = TextKey.of("jscore.host_rock.blackstone", "Blackstone %s Ore");
    public static final TextKey END_STONE_ORE = TextKey.of("jscore.host_rock.end_stone", "End %s Ore");

    public static final HostRock STONE = rock("stone", () -> Blocks.STONE, STONE_ORE).orePrefix("").declare();
    public static final HostRock DEEPSLATE = rock("deepslate", () -> Blocks.DEEPSLATE, DEEPSLATE_ORE).declare();
    public static final HostRock GRANITE = rock("granite", () -> Blocks.GRANITE, GRANITE_ORE).declare();
    public static final HostRock DIORITE = rock("diorite", () -> Blocks.DIORITE, DIORITE_ORE).declare();
    public static final HostRock ANDESITE = rock("andesite", () -> Blocks.ANDESITE, ANDESITE_ORE).declare();
    public static final HostRock TUFF = rock("tuff", () -> Blocks.TUFF, TUFF_ORE).declare();
    public static final HostRock CALCITE = rock("calcite", () -> Blocks.CALCITE, CALCITE_ORE).declare();
    public static final HostRock SANDSTONE = rock("sandstone", () -> Blocks.SANDSTONE, SANDSTONE_ORE).declare();
    /* The game's own Nether ores are nether_gold_ore and nether_quartz_ore. */
    public static final HostRock NETHERRACK = rock("netherrack", () -> Blocks.NETHERRACK, NETHERRACK_ORE)
            .orePrefix("nether_").declare();
    public static final HostRock BASALT = rock("basalt", () -> Blocks.BASALT, BASALT_ORE)
            .texture(ResourceLocation.withDefaultNamespace("block/basalt_side")).declare();
    public static final HostRock BLACKSTONE = rock("blackstone", () -> Blocks.BLACKSTONE, BLACKSTONE_ORE).declare();
    public static final HostRock END_STONE = rock("end_stone", () -> Blocks.END_STONE, END_STONE_ORE)
            .orePrefix("end_").declare();

    private HostRocks() {
    }

    /** A rock called {@code id}, whose block is {@code rock}, its ores named {@code oreName}. */
    public static HostRock.Builder rock(final ResourceLocation id, final Supplier<? extends Block> rock,
                                        final TextKey oreName) {
        return new HostRock.Builder(id, rock, oreName);
    }

    /** Every rock declared, in the order they were. */
    public static List<HostRock> all() {
        return List.copyOf(ALL);
    }

    /** The rock declared as {@code id}, or none. */
    public static Optional<HostRock> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** Declares the game's rocks, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    static HostRock declare(final HostRock rock) {
        if (BY_ID.putIfAbsent(rock.id(), rock) != null) {
            throw DeclarationError.of(rock.id().getNamespace(), "the host rock " + rock.id(), "is declared twice",
                    "declare it once, as a static final");
        }
        ALL.add(rock);
        return rock;
    }

    private static HostRock.Builder rock(final String path, final Supplier<? extends Block> rock,
                                         final TextKey oreName) {
        return rock(ResourceLocation.withDefaultNamespace(path), rock, oreName);
    }
}
