/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import dev.jstech.core.log.LogSystem;
import dev.jstech.core.log.LogSystems;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.ApiStatus;

/**
 * The last lines of each log system, and of the game's own log of the recipes it could not load, kept from the moment
 * the game starts so the studio's logs show what was written before it opened, and an agent reads them on a server
 * too. Only those logs are kept, a few hundred lines each.
 */
public final class StudioLogs {

    private static final LogRing RING = new LogRing(300);
    private static final String NAME = "jscore_studio";
    /** The game's log of the recipes it loads, whose lines say which did not. */
    private static final String RECIPES = "net.minecraft.world.item.crafting.RecipeManager";
    private static volatile Set<String> watched = Set.of(RECIPES);
    private static volatile int systems = -1;
    private static boolean listening;

    private StudioLogs() {
    }

    /** Starts keeping the lines, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static synchronized void listen() {
        if (listening || !(LogManager.getContext(false) instanceof LoggerContext context)) {
            return;
        }
        listening = true;
        final Configuration config = context.getConfiguration();
        final Keeper keeper = new Keeper();
        keeper.start();
        config.addAppender(keeper);
        config.getRootLogger().addAppender(keeper, null, null);
        context.updateLoggers();
    }

    /** The lines the log {@code logger} wrote, the oldest first. */
    public static List<LogRing.Line> lines(final String logger) {
        return RING.lines(logger);
    }

    /** The game's log of the recipes it loads. */
    public static String recipes() {
        return RECIPES;
    }

    /* The logs kept: every log system's, as many as are declared now, and the recipes'. */
    private static Set<String> watched() {
        final List<LogSystem> all = LogSystems.all();
        if (all.size() != systems) {
            final Set<String> names = new HashSet<>();
            all.forEach(system -> names.add(system.loggerName()));
            names.add(RECIPES);
            watched = Set.copyOf(names);
            systems = all.size();
        }
        return watched;
    }

    /** What keeps each line of the logs watched. */
    private static final class Keeper extends AbstractAppender {

        Keeper() {
            super(NAME, null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(final LogEvent event) {
            final String logger = event.getLoggerName();
            if (logger != null && watched().contains(logger)) {
                RING.add(logger, new LogRing.Line(event.getTimeMillis(), event.getLevel().name().toLowerCase(
                        Locale.ROOT), event.getMessage().getFormattedMessage()));
            }
        }
    }
}
