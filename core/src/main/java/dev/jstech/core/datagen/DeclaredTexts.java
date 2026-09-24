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
import java.util.ArrayList;
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
 * {@code static final} {@link TextKey} fields are what it declares, and so, on an enum, is the {@code TextKey} each
 * of its constants carries. That is what lets a sentence be written once, beside the code that says it, with
 * nothing else to keep in step: the English file is made from these.
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
        final List<TextKey> keys = new ArrayList<>();
        for (final Field field : holder.getDeclaredFields()) {
            if (field.getType() != TextKey.class) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                keys.add(read(field, null));
            } else if (holder.isEnum()) {
                /*
                 * A list of messages each with a code of its own, a compiler's for one, is an enum whose every
                 * constant carries its sentence, so the code and the words stay on one line.
                 */
                for (final Object constant : holder.getEnumConstants()) {
                    // A constant with nothing to say leaves the field empty, and declares nothing.
                    final TextKey key = read(field, constant);
                    if (key != null) {
                        keys.add(key);
                    }
                }
            }
        }
        return keys;
    }

    private static TextKey read(final Field field, final Object owner) {
        try {
            field.setAccessible(true);
            return (TextKey) field.get(owner);
        } catch (final IllegalAccessException | RuntimeException unreadable) {
            throw new IllegalStateException("the sentence " + field + " could not be read", unreadable);
        }
    }
}
