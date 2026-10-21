/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.data.ScreenFiles;
import dev.jstech.core.gui.screen.ScreenFile;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The screen: the screen file named as the thing, {@code assets/<mod>/jstech/screens/<thing>.json}, as the new screen
 * writes it. Wrong when the file does not load. A thing with no screen file has no row.
 */
final class ScreenReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.ITEM_FRAME);

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(thing.id().getNamespace(),
                ScreenFile.FOLDER + "/" + thing.id().getPath() + ".json");
        final Optional<String> problem = ScreenFiles.problem(thing.id());
        if (ScreenFiles.get(thing.id()).isEmpty() && problem.isEmpty()) {
            return;
        }
        final StudioSheet.File source = StudioFiles.asset(file);
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.SCREEN), file.getPath(),
                problem.isPresent() ? StudioSheet.Mark.ERROR : StudioSheet.Mark.FINE, Optional.of(source)));
        problem.ifPresent(wrong -> sheet.check(new StudioSheet.Check(StudioSheet.Mark.ERROR,
                GameText.resolve(StudioTexts.SCREEN_BROKEN), wrong, Optional.of(source))));
    }
}
