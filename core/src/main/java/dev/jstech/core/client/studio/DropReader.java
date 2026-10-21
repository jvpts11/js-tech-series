/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.content.Drops;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The drops: what a block's mod declared it gives when broken. Wrong when it is said to drop by a loot table and, in
 * a world of the player's own, whose data the studio reads, the table is not there.
 */
final class DropReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.BUNDLE);

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        if (thing.entry() == null || thing.block() == null) {
            return;
        }
        final Drops drops = thing.entry().drops();
        final ResourceKey<LootTable> table = thing.block().getLootTable();
        final StudioSheet.File file = StudioFiles.data(ResourceLocation.fromNamespaceAndPath(
                table.location().getNamespace(), "loot_table/" + table.location().getPath() + ".json"));
        final boolean missing = drops != Drops.NONE && missing(table);
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.DROPS), GameText.resolve(said(drops)),
                missing ? StudioSheet.Mark.ERROR : StudioSheet.Mark.FINE,
                drops == Drops.NONE ? Optional.empty() : Optional.of(file)));
        if (missing) {
            sheet.check(new StudioSheet.Check(StudioSheet.Mark.ERROR, GameText.resolve(StudioTexts.NO_LOOT),
                    GameText.resolve(StudioTexts.NO_LOOT_DETAIL.with(file.path())), Optional.of(file)));
        }
    }

    private static TextKey said(final Drops drops) {
        return switch (drops) {
            case SELF -> StudioTexts.DROPS_SELF;
            case SELF_WITH_CONTENTS -> StudioTexts.DROPS_KEEPING;
            case NONE -> StudioTexts.DROPS_NOTHING;
            case HANDMADE -> StudioTexts.DROPS_HANDMADE;
        };
    }

    /* Only a world of the player's own runs in this game, with its data to read; on a server nothing is missing. */
    private static boolean missing(final ResourceKey<LootTable> table) {
        final MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        return server != null && server.reloadableRegistries().getLootTable(table) == LootTable.EMPTY;
    }
}
