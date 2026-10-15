/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.log;

import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Every system that writes to the log under a name of its own, and how much each writes. A mod declares its systems
 * once, as constants, and writes through each one's logger:
 *
 * <pre>{@code
 * public static final LogSystem MEDIA = LogSystems.declare("mymod", "media");
 * ...
 * MEDIA.logger().warn("a recording could not be read: {}", id);
 * }</pre>
 *
 * <p>A system's level can be turned up or down while the game runs, from here or with {@code /jstech log}, to follow
 * a fault in one part without the rest of the game filling the log. It lasts until the game stops.
 */
public final class LogSystems {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_]+");
    private static final Map<String, LogSystem> BY_ID = new ConcurrentHashMap<>();
    private static final List<LogSystem> ALL = new CopyOnWriteArrayList<>();

    private LogSystems() {
    }

    /**
     * Declares the system {@code name} of {@code mod}.
     *
     * @throws DeclarationError when either is not written in lower case letters, digits and underscores, or the
     *                          system is declared already
     */
    public static LogSystem declare(final String mod, final String name) {
        if (!NAME.matcher(mod).matches() || !NAME.matcher(name).matches()) {
            throw DeclarationError.of(mod, "the log system " + mod + ":" + name,
                    "is not written in lower case letters, digits and underscores",
                    "name it the way the Core's are named, as \"media\" or \"saves\"");
        }
        final LogSystem system = new LogSystem(mod, name);
        if (BY_ID.putIfAbsent(system.id(), system) != null) {
            throw DeclarationError.of(mod, "the log system " + system.id(), "is declared twice",
                    "declare it once, as a static final, and write through that field");
        }
        ALL.add(system);
        return system;
    }

    /** Every system declared, in the order they were declared. */
    public static List<LogSystem> all() {
        return List.copyOf(ALL);
    }

    /** The system known as {@code id}, as {@code jscore:media}, or none. */
    public static Optional<LogSystem> byId(final String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** How much {@code system} writes now. */
    public static LogLevel level(final LogSystem system) {
        final Level level = LogManager.getLogger(system.loggerName()).getLevel();
        if (level == null || level.isMoreSpecificThan(Level.ERROR)) {
            return LogLevel.ERROR;
        }
        for (final LogLevel ours : LogLevel.values()) {
            if (level.isMoreSpecificThan(log4j(ours))) {
                return ours;
            }
        }
        return LogLevel.TRACE;
    }

    /** Has {@code system} write at {@code level} and above from now until the game stops. */
    public static void setLevel(final LogSystem system, final LogLevel level) {
        Configurator.setLevel(system.loggerName(), log4j(level));
    }

    private static Level log4j(final LogLevel level) {
        return switch (level) {
            case ERROR -> Level.ERROR;
            case WARN -> Level.WARN;
            case INFO -> Level.INFO;
            case DEBUG -> Level.DEBUG;
            case TRACE -> Level.TRACE;
        };
    }
}
