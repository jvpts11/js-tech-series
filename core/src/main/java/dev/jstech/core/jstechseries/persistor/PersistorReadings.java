/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * What a Space Persistor's screen shows of it and of the server, worked out on the server once a second: the chunks it
 * keeps, its owner's against their number, the server's against its budget, the power it draws, its lamps, and the
 * chunks other owners keep near it, for its map to hatch.
 *
 * @param kept         how many chunks it keeps loaded
 * @param owners       how many its owner keeps, across every dimension
 * @param ownersLimit  how many its owner may keep
 * @param server       how many every owner together keeps
 * @param serverBudget how many the server allows
 * @param watts        the power it draws, in watts; 0 while it draws none
 * @param energy       whether the server makes it spend energy at all
 * @param keeping      whether it keeps its chunks loaded now
 * @param full         whether its owner's number or the server's budget left some of its area unkept
 * @param others       the chunks other owners keep near it, packed as the game packs chunks
 */
public record PersistorReadings(int kept, int owners, int ownersLimit, int server, int serverBudget, long watts,
                                boolean energy, boolean keeping, boolean full, List<Long> others) {

    /** Nothing yet, until the server's first word. */
    public static final PersistorReadings NONE = new PersistorReadings(0, 0, 0, 0, 0, 0L, false, false, false,
            List.of());
    public static final Codec<PersistorReadings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("kept").forGetter(PersistorReadings::kept),
            Codec.INT.fieldOf("owners").forGetter(PersistorReadings::owners),
            Codec.INT.fieldOf("owners_limit").forGetter(PersistorReadings::ownersLimit),
            Codec.INT.fieldOf("server").forGetter(PersistorReadings::server),
            Codec.INT.fieldOf("server_budget").forGetter(PersistorReadings::serverBudget),
            Codec.LONG.fieldOf("watts").forGetter(PersistorReadings::watts),
            Codec.BOOL.fieldOf("energy").forGetter(PersistorReadings::energy),
            Codec.BOOL.fieldOf("keeping").forGetter(PersistorReadings::keeping),
            Codec.BOOL.fieldOf("full").forGetter(PersistorReadings::full),
            Codec.LONG.listOf().fieldOf("others").forGetter(PersistorReadings::others))
            .apply(instance, PersistorReadings::new));

    public PersistorReadings {
        others = List.copyOf(others);
    }

    /** How many more chunks its owner may keep loaded. */
    public int roomLeft() {
        return Math.max(0, this.ownersLimit - this.owners);
    }
}
