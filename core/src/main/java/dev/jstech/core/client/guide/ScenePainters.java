/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.client.model.kit.SceneRenderer;
import dev.jstech.core.guide.GuideBlock;

/**
 * The painter of a scene of blocks, as a manual draws its scenes: the blocks in a small world of their own, each block
 * entity holding what the scene says, in full light. For whoever draws a scene outside a manual, as the studio's bench.
 */
public final class ScenePainters {

    private ScenePainters() {
    }

    /** What paints {@code scene}, every layer of it, for {@link SceneRenderer#draw}. */
    public static SceneRenderer.IScenePainter of(final GuideBlock.Scene scene) {
        return new SceneLevelPainter(new SceneLevel(scene));
    }
}
