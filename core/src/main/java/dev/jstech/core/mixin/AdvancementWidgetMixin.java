/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Lets an advancement's title be as wide as it is written. The advancements screen cuts a title at 163 pixels, which
 * takes the end off the longer names ("Have You Tried Turning It Off and On Again?"); the box it draws grows with the
 * title, so a wider limit only makes the box wider for the titles that need it. In the Core so every mod built on it
 * names its advancements as long as it likes.
 */
@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {

    /* Wide enough for any title the series writes, and still narrower than the screen at the largest interface scale. */
    private static final int TITLE_MAX_WIDTH = 320;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 163))
    private int jscore$titleMaxWidth(final int vanilla) {
        return TITLE_MAX_WIDTH;
    }
}
