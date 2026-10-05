/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import java.util.List;
import net.minecraft.client.renderer.Rect2i;

/**
 * A screen that keeps the recipe viewers' ingredient lists off some of its parts: a monitor's bezel drawn beyond the
 * menu's own rectangle, a side panel that opens out. The Core's bridges to JEI and to EMI ask every container screen
 * that implements this, so a mod names its areas once and both viewers keep clear of them.
 */
public interface IKeepsViewersClear {

    /** The areas, in screen coordinates, that no viewer should draw over right now. */
    List<Rect2i> areasKeptClear();
}
