/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.palette;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class that declares palettes as {@code static final} {@link Palette} fields, so the generator writes each
 * one's file and the client reads each one back from the resource packs, whether or not anything has drawn with it
 * yet.
 *
 * <p>Palettes colour what a client draws, so such a class is loaded only where there is one: by the generator and
 * by the client's resource reload, never on a dedicated server.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PaletteHolder {
}
