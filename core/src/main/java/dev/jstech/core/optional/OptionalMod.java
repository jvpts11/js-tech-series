/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.optional;

import dev.jstech.core.log.CoreLogs;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * A mod a mod works with when it is installed and does without when it is not, behind a guard: nothing of the other
 * mod is loaded unless it is there. The work with it lives in an adapter class under an {@code integration} package,
 * reached only through the guard:
 *
 * <pre>{@code
 * public static final OptionalMod JEI = OptionalMod.of("jei");
 * ...
 * JEI.ifLoaded(() -> JeiAdapter::start);   // in the mod's constructor
 * }</pre>
 *
 * <p>The adapter is named inside a lambda the guard calls only when the mod is installed, so its class, and the other
 * mod's classes it names, are loaded only then. An adapter that fails to start (the other mod changed under it) is
 * written to the log and left out, and the game goes on without it. {@link OptionalDependencyCheck} keeps an optional
 * mod from becoming a required one by an import in the wrong place.
 */
@ApiStatus.Experimental
public final class OptionalMod {

    private final String modid;
    private final Predicate<String> installed;

    private OptionalMod(final String modid, final Predicate<String> installed) {
        this.modid = Objects.requireNonNull(modid, "modid");
        this.installed = Objects.requireNonNull(installed, "installed");
    }

    /** The mod {@code modid}, as the game's list of mods knows it. */
    public static OptionalMod of(final String modid) {
        return new OptionalMod(modid, id -> ModList.get().isLoaded(id));
    }

    /**
     * The mod {@code modid}, installed whenever {@code installed} says so: what a test hands over to see a mod's
     * adapters with the other mod present and absent, without the game.
     */
    public static OptionalMod of(final String modid, final Predicate<String> installed) {
        return new OptionalMod(modid, installed);
    }

    /** The other mod's id. */
    public String modid() {
        return modid;
    }

    /** Whether the other mod is installed. */
    public boolean isLoaded() {
        return installed.test(modid);
    }

    /**
     * Starts what {@code adapter} names, if the other mod is installed; nothing is loaded of it otherwise.
     *
     * @return whether the adapter started
     */
    public boolean ifLoaded(final Supplier<Runnable> adapter) {
        if (!isLoaded()) {
            return false;
        }
        try {
            adapter.get().run();
            return true;
        } catch (final LinkageError | RuntimeException broken) {
            // The log is reached only here, so a guard asked outside the game (a unit test) needs none.
            final Logger log = CoreLogs.INTEGRATIONS.logger();
            log.warn("the work with {} did not start and is left out", modid, broken);
            return false;
        }
    }

    @Override
    public String toString() {
        return modid;
    }
}
