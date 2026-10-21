/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The data network: the graph of data cables, network anchors, gateways and routers that joins the computers and
 * machines of any mod around one Mainframe, the identity each network and each member carries, how fast data goes
 * between two places, what the network may do with each block, and what it may read of each machine.
 *
 * <p>{@link NetworkSystem} is a level's networks, from {@code NetworkSystem.get(level)}: the network a place is on,
 * its Mainframe and its other nodes, and its members. Its grid of cables is the {@link ConnectivityIndex}, which joins
 * the runs a cable touches, settles two networks that meet, and gives the slowest cable on the fastest way between
 * two places. Each data cable is a {@link DataLink}, a {@link DataLine} in an era, with its speed and its range; a
 * port takes {@code DataLines.upTo(era, line)} ({@link DataLines}), its own era's cable and every earlier one, and a
 * block that is an {@link INetworkBridge} joins the runs it touches into one. A machine of any mod becomes a member of
 * the network it touches through the Core, with no tie to another mod's classes: its block entity implements
 * {@link INetworkMember} and keeps itself on the network from its constructor with {@link NetworkMembers}, which hands
 * back its {@link NetworkMembership}. Its {@link NetworkCategory} says what the network may do with it: carry the
 * network only, read it, or give it Operations. What the network may read of it are its rows in {@link StateTable}s,
 * each table declared once with {@link StateTables} and read across a network through {@link NetworkTables}; a member
 * joining or leaving is posted as a {@link NetworkMemberEvent}. The nodes a network keeps are a closed family
 * ({@link INetworkNode}); routers are topology elements ({@link INetworkTopologyElement}), which shape the network and
 * never receive work.
 *
 * <p>How to use it, from zero, is in {@code core/docs/NETWORKS.md}. An example that compiles is the test mod's reactor,
 * read through a state table, and its accelerator, given a kind of Operation of the mod's own
 * ({@code tests/src/main/java/dev/jstech/tests/TestNetworkMembers.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.network;

import org.jetbrains.annotations.ApiStatus;
