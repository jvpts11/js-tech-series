/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

/**
 * A class a mod hands to the scripts of a scripting mod, such as KubeJS, under a name: a script calls the class's
 * public static methods as {@code Name.method(...)}. The class itself names nothing of the scripting mod, so it loads
 * whether one is installed or not.
 *
 * @param name    the name the scripts know it by, a plain identifier unique across every mod
 * @param statics the class whose public static methods they call
 */
public record ScriptBinding(String name, Class<?> statics) {
}
