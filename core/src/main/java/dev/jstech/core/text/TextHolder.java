/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class that declares sentences as {@code static final} {@link TextKey} fields, so the language generator
 * finds them and writes their English. A class that declares one without this is caught by the build, which checks
 * that every declared key reaches its mod's English file.
 *
 * <p>Every such class is loaded to be read, on a server as well, so it must be one a dedicated server can load. A
 * class that exists only on a client, a screen, keeps its sentences in a small class of constants beside it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TextHolder {
}
