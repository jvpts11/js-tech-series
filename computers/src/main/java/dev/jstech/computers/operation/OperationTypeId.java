/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import java.util.Locale;
import java.util.Optional;

/**
 * What kind of thing an Operation was, as the log, the statistics and every view of them say it.
 *
 * <p>Three things about a kind of Operation used to be written down in three places with nothing joining
 * them: the id it is registered under, the number a saved log and a packet carry, and the word a terminal
 * shows. Each could be changed without the others, and a saved log would then read as something else with
 * nothing anywhere to say so. They are one declaration now.
 *
 * <p>A kind is coarser than a registered Operation on purpose. Several Operations are the same thing to
 * somebody reading a log: making something, making it through a machine and making it in stages are all a
 * craft to whoever asked for it, however differently the network goes about them.
 */
public enum OperationTypeId implements IStableId {

    /*
     * Numbered from one, so that zero means a kind this version does not know rather than meaning the first
     * of them. A log written by a later version can then be read by an earlier one without the rows it does
     * not understand quietly becoming SELECTs.
     */
    SELECT(1, "select"),
    INSERT(2, "insert"),
    DELETE(3, "delete"),
    MOVE(4, "move"),
    CRAFT(5, "craft", "processing", "multi_stage"),
    ANALYZE(6, "analyze"),
    REINDEX(7, "reindex"),
    VACUUM(8, "vacuum"),
    DROP(9, "drop");

    /** A kind this version does not know, which is what a row from a later one reads as. */
    public static final byte UNKNOWN = 0;

    /** The namespace every Operation of this mod is registered under. */
    public static final String NAMESPACE = "jsc";

    private static final StableIds<OperationTypeId> IDS = StableIds.of(OperationTypeId.class);

    private final int id;
    private final String path;
    private final String[] alsoCovers;

    OperationTypeId(final int id, final String path, final String... alsoCovers) {
        this.id = id;
        this.path = path;
        this.alsoCovers = alsoCovers;
    }

    /** The one that number stands for, or nothing at all for a kind this version does not know. */
    public static Optional<OperationTypeId> of(final int id) {
        return Optional.ofNullable(IDS.find(id));
    }

    /** The word a terminal shows for that number, or {@code OP} for a kind this version does not know. */
    public static String verbOf(final byte id) {
        return of(id).map(OperationTypeId::verb).orElse("OP");
    }

    @Override
    public int id() {
        return this.id;
    }

    /** The id the Operation is registered under, as {@code jsc:select}. */
    public String registryId() {
        return NAMESPACE + ":" + this.path;
    }

    /**
     * The other ids that are this same kind to whoever reads a log.
     *
     * <p>Empty for most of them. A craft covers three, because a plan carried out by a machine or in stages is
     * still a craft to the one who asked for it.
     */
    public String[] alsoCovers() {
        return this.alsoCovers.clone();
    }

    /** The word a terminal shows: the name, as a player reads it. */
    public String verb() {
        return this.name().toUpperCase(Locale.ROOT);
    }
}
