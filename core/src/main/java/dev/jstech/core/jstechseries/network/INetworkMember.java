/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.jstechseries.operation.IOperationArgs;
import dev.jstech.core.jstechseries.operation.OperationType;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Writes its rows of {@code table}, one of its {@link #tables()}, as they are now; on the server. One, of itself,
     * unless it says otherwise: a member that reads other machines writes one for each, where each stands.
     */
    @ApiStatus.Experimental
    default void fillRows(final StateTable table, final StateRows rows) {
        fill(table, rows.own());
    }

    /**
     * Why it will not take a request of {@code type} carrying {@code args}, though it carries that kind out (a
     * controller asked to set a machine it does not read); null when it takes it. The network gives such a request
     * to a member that takes it.
     */
    @ApiStatus.Experimental
    default @Nullable Text refuses(final OperationType<?> type, final IOperationArgs args) {
        return null;
    }

    /**
     * The kinds of world event it is the instrument of, by their ids: a seismograph of earthquakes, a ground station
     * of solar storms. A network with it on sees those events coming, as a player carrying one is warned. None unless
     * said.
     */
    @ApiStatus.Experimental
    default List<ResourceLocation> instrumentOf() {
        return List.of();
    }
}
