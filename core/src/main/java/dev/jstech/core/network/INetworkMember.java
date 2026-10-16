/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.operation.OperationType;
import java.util.List;

/**
 * A machine of any mod that is a member of the data network it touches, through the Core and never through another
 * mod's classes: its block entity implements this and keeps itself on the network with {@link NetworkMembers#keep}.
 * It says what the network may do with it, the kinds of Operation it carries out, and what the network may read of it,
 * as rows of state tables. The network gives a machine more; it never makes it work: a member works through its own
 * screens with no network at all.
 *
 * <pre>{@code
 * public class ReactorBlockEntity extends SyncedBlockEntity implements INetworkMember {
 *     private final NetworkMembership membership = NetworkMembers.keep(this);
 *
 *     public NetworkCategory category() {
 *         return NetworkCategory.B;                       // read, never given work
 *     }
 *
 *     public List<StateTable> tables() {
 *         return List.of(MyTables.REACTORS);
 *     }
 *
 *     public void fill(StateTable table, StateRow.Writer row) {
 *         row.set("temperature", temperature).set("power", power).set("running", running);
 *     }
 * }
 * }</pre>
 */
public interface INetworkMember {

    /**
     * What the network may do with it: {@link NetworkCategory#B B}, read it; {@link NetworkCategory#C C}, read it and
     * give it the kinds of Operation it carries out. A block that only carries the network is no member.
     */
    NetworkCategory category();

    /** The kinds of Operation it carries out, each registered and open to its category; none for one only read. */
    default List<OperationType<?>> carriesOut() {
        return List.of();
    }

    /** The state tables it has a row in; none unless said. */
    default List<StateTable> tables() {
        return List.of();
    }

    /** Writes its row of {@code table}, one of its {@link #tables()}, as it is now; on the server. */
    default void fill(final StateTable table, final StateRow.Writer row) {
    }
}
