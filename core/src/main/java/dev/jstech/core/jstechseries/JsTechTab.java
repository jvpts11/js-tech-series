/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.material.CatalogueItems;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.registry.CoreItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * The series' one creative tab of shared content, "J's Tech", in three sections: the materials, the fluids and
 * components of the shared catalogue, and the shared blocks and tools (the series' manual first). It shows only what
 * the installed mods use ({@link SharedUses}), and a mod that declared its uses finds them left out of its own tabs,
 * which keep its own content: the machines of J's Industrial, not the materials.
 */
public final class JsTechTab {

    /** The tab. */
    public static final ContentTab TAB = CoreItems.CONTENT.tab("jstech", "J's Tech", () -> CoreGuide.MANUAL);
    /** The materials: dusts, plates and the other forms. */
    public static final ContentTab.Section MATERIALS = TAB.section().shownIf(SharedUses::used);
    /** The fluids and the generic components of the shared catalogue. */
    public static final ContentTab.Section FLUIDS_AND_COMPONENTS = TAB.section().shownIf(SharedUses::used);
    /** The shared blocks and tools, the series' manual first. */
    public static final ContentTab.Section BLOCKS_AND_TOOLS = TAB.section().shownIf(SharedUses::used);

    static {
        BLOCKS_AND_TOOLS.with(() -> CoreGuide.MANUAL);
        // Every player has the series' manual, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, () -> CoreGuide.MANUAL);
        // The things of the materials the installed mods use, which exist only once the game registered them, and
        // the liquids' buckets and the components of the shared catalogue the same way.
        MATERIALS.withEach(MaterialUses::sharedItems);
        FLUIDS_AND_COMPONENTS.withEach(MaterialLiquids::buckets);
        FLUIDS_AND_COMPONENTS.withEach(CatalogueItems::items);
    }

    private JsTechTab() {
    }

    /** Declares the tab, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Every item the tab shows now, section by section. */
    public static List<Item> shown() {
        final List<Item> out = new ArrayList<>();
        for (final ContentTab.Section section : TAB.sections()) {
            out.addAll(section.shown());
        }
        return out;
    }

    /**
     * What is left out of the tab {@code tab}: what the J's Tech tab shows, when the tab is another of a mod that
     * declared its uses; nothing otherwise.
     */
    public static List<Item> leftOutOf(final ResourceLocation tab) {
        if (tab.equals(TAB.id()) || !SharedUses.users().contains(tab.getNamespace())) {
            // A mod that declared no use of the shared content keeps its tabs as they are.
            return List.of();
        }
        return shown();
    }

    /** Hands the mod's bus the filling of every tab, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, JsTechTab::onBuildContents);
    }

    private static void onBuildContents(final BuildCreativeModeTabContentsEvent event) {
        for (final Item item : leftOutOf(event.getTabKey().location())) {
            event.remove(new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
    }
}
