/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.developer.ThingNames;
import dev.jstech.core.text.GameText;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The settings: the keys of the thing's mod's settings files named after it, as {@code machines.compressor.speed} is
 * the compressor's, counted with the files they are in. A thing no setting is named after has no row.
 */
final class SettingReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.COMPARATOR);
    private static final String SEPARATOR = ", ";

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        int count = 0;
        final Set<String> files = new LinkedHashSet<>();
        for (final ConfigFile file : ConfigFiles.of(thing.modid())) {
            for (final ConfigKey<?> key : file.keys()) {
                if (ThingNames.names(key.dottedPath(), thing.id().getPath())) {
                    count++;
                    files.add(file.fileName());
                }
            }
        }
        if (count > 0) {
            sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.SETTINGS),
                    GameText.resolve(StudioTexts.SETTINGS_COUNT.with(count, String.join(SEPARATOR, files))),
                    StudioSheet.Mark.FINE, Optional.empty()));
        }
    }
}
