/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.tool;

import dev.jstech.core.condition.SettingCondition;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.config.IndustrialServerConfig;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * The kinds of J's Industrial's tools a server may leave out (industrial/docs/design/tools-and-armour.md): swords,
 * pickaxes, axes, shovels and hoes, each a setting of the world's on its own. A kind left out has its recipes gone,
 * each recipe of it carrying its kind's condition ({@link Kind#condition}), and its tools leave the creative tab; a
 * tool of it already made keeps working. A tool is of a kind by the kind's item tag, so a data pack files another
 * mod's under it too.
 */
@TextHolder
public final class ToolKinds {

    public static final TextKey SWORDS_NAME = TextKey.of("tag.item.jsindustrial.tool_kinds.swords", "Swords");
    public static final TextKey PICKAXES_NAME = TextKey.of("tag.item.jsindustrial.tool_kinds.pickaxes", "Pickaxes");
    public static final TextKey AXES_NAME = TextKey.of("tag.item.jsindustrial.tool_kinds.axes", "Axes");
    public static final TextKey SHOVELS_NAME = TextKey.of("tag.item.jsindustrial.tool_kinds.shovels", "Shovels");
    public static final TextKey HOES_NAME = TextKey.of("tag.item.jsindustrial.tool_kinds.hoes", "Hoes");
    public static final Kind SWORDS = new Kind(tag("swords"), IndustrialServerConfig.SWORDS);
    public static final Kind PICKAXES = new Kind(tag("pickaxes"), IndustrialServerConfig.PICKAXES);
    public static final Kind AXES = new Kind(tag("axes"), IndustrialServerConfig.AXES);
    public static final Kind SHOVELS = new Kind(tag("shovels"), IndustrialServerConfig.SHOVELS);
    public static final Kind HOES = new Kind(tag("hoes"), IndustrialServerConfig.HOES);

    private ToolKinds() {
    }

    /** Every kind. */
    public static List<Kind> all() {
        return List.of(SWORDS, PICKAXES, AXES, SHOVELS, HOES);
    }

    /** Takes the tools of the kinds left out off the creative tabs, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EventPriority.LOWEST, BuildCreativeModeTabContentsEvent.class, ToolKinds::leaveOut);
    }

    /** Whether {@code stack} is a tool of a kind the world leaves out. */
    public static boolean leftOut(final ItemStack stack) {
        for (final Kind kind : all()) {
            if (stack.is(kind.items()) && !kind.made()) {
                return true;
            }
        }
        return false;
    }

    private static void leaveOut(final BuildCreativeModeTabContentsEvent event) {
        for (final ItemStack stack : List.copyOf(event.getParentEntries())) {
            if (leftOut(stack)) {
                event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
        for (final ItemStack stack : List.copyOf(event.getSearchEntries())) {
            if (leftOut(stack)) {
                event.remove(stack, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
            }
        }
    }

    private static TagKey<Item> tag(final String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID,
                "tool_kinds/" + path));
    }

    /**
     * A kind of tool.
     *
     * @param items its tools
     * @param on    the world's setting that makes it
     */
    public record Kind(TagKey<Item> items, ConfigKey<Boolean> on) {

        public Kind {
            Objects.requireNonNull(items, "items");
            Objects.requireNonNull(on, "on");
        }

        /** Whether the world makes it. */
        public boolean made() {
            return IndustrialServerConfig.FILE.get(this.on);
        }

        /** The condition each recipe of it carries, so its recipes are gone while it is left out. */
        public SettingCondition condition() {
            return SettingCondition.on(IndustrialServerConfig.FILE, this.on);
        }
    }
}
