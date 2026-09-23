/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;

/**
 * Every sentence a mod declares, found where it is declared.
 *
 * <p>The classes marked {@link TextHolder} are read out of the mod's own scan of itself, and each one's
 * {@code static final} {@link TextKey} fields are what it declares. That is what lets a sentence be written once,
 * beside the code that says it, with nothing else to keep in step: the English file is made from these.
 */
public final class DeclaredTexts {

    private DeclaredTexts() {
    }

    /**
     * Every sentence that mod declares, in key order.
     *
     * @throws IllegalStateException when one key is declared twice with different English, which would leave a
     *                               translator two sentences to make one of
     */
    public static Collection<TextKey> of(final String modid) {
        final IModFileInfo file = ModList.get().getModFileById(modid);
        if (file == null) {
            throw new IllegalStateException("no mod " + modid + " to read sentences from");
        }
        final Map<String, TextKey> found = new TreeMap<>();
        final ModFileScanData scan = file.getFile().getScanResult();
        for (final String holder : scan.getAnnotatedBy(TextHolder.class, ElementType.TYPE)
                .map(annotation -> annotation.clazz().getClassName()).sorted().toList()) {
            for (final TextKey key : declaredIn(holder)) {
                final TextKey before = found.putIfAbsent(key.key(), key);
                if (before != null && !before.english().equals(key.english())) {
                    throw new IllegalStateException("the sentence " + key.key() + " is declared twice with different"
                            + " English: \"" + before.english() + "\" and \"" + key.english() + "\"");
                }
            }
        }
        return List.copyOf(found.values());
    }

    private static List<TextKey> declaredIn(final String className) {
        final Class<?> holder;
        try {
            holder = Class.forName(className, true, DeclaredTexts.class.getClassLoader());
        } catch (final ClassNotFoundException missing) {
            throw new IllegalStateException("the sentences of " + className + " could not be read", missing);
        }
        return Arrays.stream(holder.getDeclaredFields())
                .filter(field -> field.getType() == TextKey.class && Modifier.isStatic(field.getModifiers()))
                .map(DeclaredTexts::read)
                .toList();
    }

    private static TextKey read(final Field field) {
        try {
            field.setAccessible(true);
            return (TextKey) field.get(null);
        } catch (final IllegalAccessException | RuntimeException unreadable) {
            throw new IllegalStateException("the sentence " + field + " could not be read", unreadable);
        }
    }
}
