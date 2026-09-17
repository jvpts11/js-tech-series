/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.language;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Every language the computers of this world know.
 *
 * <p>An addon may add one, and may take one away, including the one this series ships with, while the game loads.
 * That is deliberate: a pack that wants its computers programmed in something else entirely should not have to
 * live beside a language nobody in it uses. Once every mod has loaded, the registry is closed, so the languages a
 * world runs with cannot change under it.
 *
 * <p>Everything that deals in programs resolves through here by extension rather than by naming a language, so the
 * prompt, the editor and the file explorer work with whatever happens to be registered. An extension belongs to one
 * language: a second language that claims one already taken is refused, because otherwise which of the two answers
 * for a file would depend on the order the mods loaded in. An extension the machines keep for themselves, such as
 * that of the listings they run, belongs to no language at all.
 */
public final class LanguageRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<ResourceLocation, IProgrammingLanguage> byId = new LinkedHashMap<>();
    /** Every extension a language writes or builds, to that language; worked out again whenever one comes or goes. */
    private final Map<String, IProgrammingLanguage> byExtension = new HashMap<>();
    /** Every extension a language runs, to that language. */
    private final Map<String, IProgrammingLanguage> runners = new HashMap<>();
    /** The extensions kept back from every language. */
    private final Set<String> reserved = new HashSet<>();
    private volatile boolean frozen;

    /**
     * Adds one, replacing any already registered under that id.
     *
     * <p>Replacing lets an addon improve a language in place, which is the same gesture as adding one; the log says
     * so, so a pack can see it happened. A language that claims an extension another language already has is
     * refused, and so is every change once the registry is closed.
     *
     * @return whether the language is now registered
     */
    public synchronized boolean register(final IProgrammingLanguage language) {
        if (language == null || language.id() == null) {
            return false;
        }
        if (this.frozen) {
            LOGGER.warn("The language {} was not registered: languages can only be added while the game loads",
                    language.id());
            return false;
        }
        for (final String extension : extensionsOf(language)) {
            if (this.reserved.contains(extension)) {
                LOGGER.warn("The language {} was not registered: .{} belongs to the machines", language.id(),
                        extension);
                return false;
            }
            final IProgrammingLanguage owner = this.byExtension.get(extension);
            if (owner != null && !owner.id().equals(language.id())) {
                LOGGER.warn("The language {} was not registered: .{} already belongs to {}", language.id(), extension,
                        owner.id());
                return false;
            }
        }
        if (this.byId.put(language.id(), language) != null) {
            LOGGER.info("The language {} replaces the one registered under that id before it", language.id());
        }
        this.index();
        return true;
    }

    /** Takes one away while the game loads; false when it was not there, or when the registry is closed. */
    public synchronized boolean unregister(final ResourceLocation id) {
        if (this.frozen) {
            LOGGER.warn("The language {} was not removed: languages can only be removed while the game loads", id);
            return false;
        }
        if (this.byId.remove(id) == null) {
            return false;
        }
        this.index();
        return true;
    }

    /**
     * Keeps an extension back from every language, as the machines do for the listings they run themselves.
     *
     * @return whether it is now kept back; false once the registry is closed, or when a language already claims it
     */
    public synchronized boolean reserve(final String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        final String lower = extension.toLowerCase(Locale.ROOT);
        if (this.frozen) {
            LOGGER.warn("The extension .{} was not kept back: the registry is closed", lower);
            return false;
        }
        final IProgrammingLanguage owner = this.byExtension.get(lower);
        if (owner != null) {
            LOGGER.warn("The extension .{} was not kept back: it already belongs to {}", lower, owner.id());
            return false;
        }
        this.reserved.add(lower);
        return true;
    }

    /** Whether an extension is kept back from every language. */
    public boolean isReserved(final String extension) {
        return extension != null && this.reserved.contains(extension.toLowerCase(Locale.ROOT));
    }

    /** Closes the registry, as the core does once every mod has loaded: nothing is added or taken away after. */
    public void freeze() {
        this.frozen = true;
    }

    /** Whether the registry has been closed. */
    public boolean isFrozen() {
        return this.frozen;
    }

    /** The one with that id, or null. */
    @Nullable
    public IProgrammingLanguage get(final ResourceLocation id) {
        return this.byId.get(id);
    }

    /**
     * The language whose files end in that extension, or null.
     *
     * <p>Both what a person writes and what a compiler produces count, so this answers for a source file and for
     * the thing built from it alike.
     */
    @Nullable
    public IProgrammingLanguage byExtension(final String extension) {
        if (extension == null || extension.isBlank()) {
            return null;
        }
        return this.byExtension.get(extension.toLowerCase(Locale.ROOT));
    }

    /** The language that RUNS files ending in that extension, or null. */
    @Nullable
    public IProgrammingLanguage runnerOf(final String extension) {
        if (extension == null || extension.isBlank()) {
            return null;
        }
        return this.runners.get(extension.toLowerCase(Locale.ROOT));
    }

    /** The language whose SOURCE files end in that extension, the files a person writes in it, or null. */
    @Nullable
    public IProgrammingLanguage sourceOf(final String extension) {
        final IProgrammingLanguage language = this.byExtension(extension);
        return language != null && language.sourceExtensions().contains(extension.toLowerCase(Locale.ROOT))
                ? language : null;
    }

    /** Every language there is, in the order they were registered. */
    public List<IProgrammingLanguage> all() {
        return List.copyOf(this.byId.values());
    }

    /** Works the extension lookups out again from the languages registered. */
    private void index() {
        this.byExtension.clear();
        this.runners.clear();
        for (final IProgrammingLanguage language : this.byId.values()) {
            for (final String extension : extensionsOf(language)) {
                this.byExtension.put(extension, language);
            }
            for (final String extension : language.binaryExtensions()) {
                this.runners.put(extension.toLowerCase(Locale.ROOT), language);
            }
        }
    }

    /** Every extension a language claims, as a person writes it or as its compiler produces it, in lower case. */
    private static List<String> extensionsOf(final IProgrammingLanguage language) {
        final List<String> all = new ArrayList<>();
        for (final String extension : language.sourceExtensions()) {
            all.add(extension.toLowerCase(Locale.ROOT));
        }
        for (final String extension : language.binaryExtensions()) {
            all.add(extension.toLowerCase(Locale.ROOT));
        }
        return all;
    }
}
