/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import com.mojang.serialization.Codec;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Teracoin's settings in {@code jscomputers-server.toml} (computers/docs/design/teracoin.md, "Settings"): the chain's
 * numbers, the rare items a block may bring, and the 3D printers' prices, whose list is the list of what can be
 * printed. Teracoin has no switch, only its numbers; they are estimates.
 */
public final class TeracoinSettings {

    /** The section of the chain's numbers. */
    public static final String SECTION = "teracoin";
    /** The section of the 3D printers' prices. */
    public static final String PRINTER = SECTION + ".printer";
    public static final ConfigKey<Integer> TARGET_MINUTES = ConfigKey.whole(SECTION + ".target_minutes", 10)
            .range(1, 1_440)
            .comment("A block's target time: the difficulty is adjusted every ten blocks to keep blocks coming this "
                    + "often.")
            .named("Target time")
            .unit("min");
    public static final ConfigKey<Integer> REWARD = ConfigKey.whole(SECTION + ".reward", 50)
            .range(0, 1_000_000)
            .comment("What a block pays, in TRC, before any halving.")
            .named("Reward")
            .unit("TRC");
    public static final ConfigKey<Boolean> HALVING = ConfigKey.flag(SECTION + ".halving", true)
            .comment("Whether the reward halves every so many blocks, for the whole server. An operator puts it back "
                    + "to its first value with /jstech crypto halving reset.")
            .named("Halving");
    public static final ConfigKey<Integer> HALVING_BLOCKS = ConfigKey.whole(SECTION + ".halving_blocks", 2_100)
            .range(1, 10_000_000)
            .comment("Every how many blocks the reward halves.")
            .named("Blocks to a halving")
            .unit("blocks");
    public static final ConfigKey<Integer> FLOOR_DAYS = ConfigKey.whole(SECTION + ".floor_days", 100)
            .range(1, 1_000_000)
            .comment("The difficulty never falls below a floor that grows by its start every this many world days, "
                    + "so turning every miner off for days and coming back does not give easy blocks.")
            .named("Floor days")
            .unit("days");
    public static final ConfigKey<Double> RARE_ITEM_CHANCE = ConfigKey.number(SECTION + ".rare_item_chance", 0.05)
            .range(0.0, 1.0)
            .comment("The share of blocks that also bring a rare item, into the network storage of the machine whose "
                    + "work found the block.")
            .named("Rare item chance");
    public static final ConfigKey<List<String>> RARE_ITEMS = ConfigKey.of(SECTION + ".rare_items",
                    Codec.STRING.listOf(), List.of("minecraft:diamond", "minecraft:emerald",
                            "minecraft:netherite_scrap", "minecraft:ancient_debris",
                            "minecraft:enchanted_golden_apple"))
            .comment("The rare items a block may bring, one drawn at random.")
            .named("Rare items");
    public static final ConfigKey<List<String>> PRICES = ConfigKey.of(PRINTER + ".prices", Codec.STRING.listOf(),
                    List.of("minecraft:emerald=5", "minecraft:diamond=10", "minecraft:music_disc_cat=15",
                            "minecraft:shulker_shell=20", "minecraft:ancient_debris=30",
                            "minecraft:totem_of_undying=60", "minecraft:netherite_ingot=80",
                            "minecraft:heart_of_the_sea=100", "minecraft:enchanted_golden_apple=120",
                            "minecraft:trident=150", "minecraft:elytra=400", "minecraft:nether_star=800"))
            .comment("What the 3D printers print and its price in TRC, one item a line: the list is the list of what "
                    + "can be printed. A price may have up to eight decimal places.")
            .named("Prices");

    private TeracoinSettings() {
    }

    /** Adds the settings to the server's file, with their sections. */
    public static ConfigFile.Builder addTo(final ConfigFile.Builder file) {
        return file.sectionNamed(SECTION, "Teracoin")
                .sectionComment(SECTION, "The chain of the world's cryptocurrency: how often its blocks come, what "
                        + "they pay and the rare items they bring.")
                .sectionNamed(PRINTER, "3D printers")
                .sectionComment(PRINTER, "What the 3D printers print, and for how many TRC.")
                .key(TARGET_MINUTES).key(REWARD).key(HALVING).key(HALVING_BLOCKS).key(FLOOR_DAYS)
                .key(RARE_ITEM_CHANCE).key(RARE_ITEMS).key(PRICES);
    }

    /** The chain's numbers as the server's settings give them now. */
    public static TeracoinRules rules() {
        final ConfigFile file = ComputersServerConfig.FILE;
        return new TeracoinRules(file.get(TARGET_MINUTES) * TeracoinRules.TICKS_PER_MINUTE,
                Trc.of(file.get(REWARD)), file.get(HALVING), file.get(HALVING_BLOCKS), file.get(FLOOR_DAYS),
                file.get(RARE_ITEM_CHANCE));
    }

    /** The rare items a block may bring, the names no item has left out. */
    public static List<Item> rareItems() {
        final List<Item> out = new ArrayList<>();
        for (final String name : ComputersServerConfig.FILE.get(RARE_ITEMS)) {
            item(name).ifPresent(out::add);
        }
        return out;
    }

    /** What the 3D printers print, in the list's order, the lines that name no item or no price left out. */
    public static List<Price> prices() {
        final List<Price> out = new ArrayList<>();
        for (final String line : ComputersServerConfig.FILE.get(PRICES)) {
            final int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            final long price = Trc.parse(line.substring(equals + 1));
            final Optional<Item> item = item(line.substring(0, equals).trim());
            if (price > 0L && item.isPresent() && out.stream().noneMatch(p -> p.item() == item.get())) {
                out.add(new Price(item.get(), price));
            }
        }
        return out;
    }

    /** What {@code item} costs to print, or -1 when it is not on the list. */
    public static long priceOf(final Item item) {
        for (final Price price : prices()) {
            if (price.item() == item) {
                return price.units();
            }
        }
        return -1L;
    }

    private static Optional<Item> item(final String name) {
        final ResourceLocation id = ResourceLocation.tryParse(name.trim());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Optional.empty();
        }
        final Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? Optional.empty() : Optional.of(item);
    }

    /** An item the printers print and its price, in hundred-millionths of a TRC. */
    public record Price(Item item, long units) {
    }
}
