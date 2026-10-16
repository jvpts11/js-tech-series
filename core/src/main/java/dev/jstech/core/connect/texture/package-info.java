/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
/**
 * Connected textures as resource packs write them, for OptiFine and Continuity ({@code optifine/ctm/*.properties})
 * and for CTM (a texture's {@code .mcmeta} and a model's {@code ctm_overrides}): the readers of both formats, and
 * which tile or which part of a texture each of their methods and types gives a face, from the neighbours it joins
 * or the place it is in. Pure; the game's side is the Core's client.
 */
package dev.jstech.core.connect.texture;
