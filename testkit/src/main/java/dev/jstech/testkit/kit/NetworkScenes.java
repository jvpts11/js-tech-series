/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.kit;

import dev.jstech.core.jstechseries.network.ConnectivityIndex;
import dev.jstech.core.jstechseries.network.DataLine;
import dev.jstech.core.jstechseries.network.DataLink;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.operation.IOperationArgs;
import dev.jstech.core.jstechseries.operation.IOperationTask;
import dev.jstech.core.jstechseries.operation.OperationDispatch;
import dev.jstech.core.jstechseries.operation.OperationFailure;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.jstechseries.operation.OperationType;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.testkit.gametest.Expect;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/**
 * A scene for the data network and its Operations: a network of its own laid through the arena, which the members
 * placed beside it join; a request of a kind of Operation given to a member that carries it out, as the network gives
 * it; and a dispatcher ticked with the server, to queue Operations on and wait for their status.
 *
 * <pre>{@code
 * final NetworkScenes.Network network = NetworkScenes.lay(helper, new BlockPos(2, 2, 2), new BlockPos(3, 2, 2));
 * helper.setBlock(new BlockPos(1, 2, 2), MyContent.REACTOR.get());
 * helper.startSequence()
 *         .thenWaitUntil(() -> network.expectMembers(helper, 1))
 *         .thenExecute(() -> Expect.equal(helper, CABLE, "the request", OperationStatus.COMPLETED,
 *                 NetworkScenes.send(helper, network, MyOperations.COOL, new CoolArgs(300))))
 *         .thenExecute(network::close)
 *         .thenSucceed();
 * }</pre>
 */
public final class NetworkScenes {

    private NetworkScenes() {
    }

    /**
     * Lays a data network of its own through the arena's places {@code cables}: each the place of a data cable to the
     * Core, those side by side joined, all of it one network. Nothing is set in the world: a member placed beside one
     * of them joins the network as it loads, as beside a cable.
     */
    public static Network lay(final GameTestHelper helper, final BlockPos... cables) {
        final ServerLevel level = helper.getLevel();
        final ConnectivityIndex connectivity = NetworkSystem.get(level).connectivity();
        final NetworkUuid id = new NetworkUuid(UUID.randomUUID());
        final List<Long> laid = new ArrayList<>();
        final Set<Long> placed = new HashSet<>();
        for (final BlockPos cable : cables) {
            final BlockPos pos = helper.absolutePos(cable);
            final Set<Long> beside = new HashSet<>();
            for (final Direction side : Direction.values()) {
                final long next = pos.relative(side).asLong();
                if (placed.contains(next)) {
                    beside.add(next);
                }
            }
            connectivity.onCablePlaced(pos.asLong(), beside, new DataLink(DataLine.ACCESS, HardwareEra.STANDARD));
            // No cable block stands there, so the members beside it are told where the scene's cable is.
            connectivity.standIn(pos.asLong(), pos.asLong());
            placed.add(pos.asLong());
            laid.add(pos.asLong());
        }
        if (!laid.isEmpty()) {
            connectivity.assignUuid(laid.getFirst(), id);
        }
        return new Network(id, List.copyOf(laid), level);
    }

    /**
     * Gives a request of {@code type} to a member of {@code network} that carries it out, as the network gives it,
     * and answers how it went; a failure when no member of the network carries that kind out.
     */
    public static <T extends IOperationArgs> OperationStatus send(final GameTestHelper helper, final Network network,
                                                                  final OperationType<T> type, final T args) {
        final List<NetworkSystem.MemberNode> able = NetworkSystem.get(network.level())
                .membersCarryingOut(network.id(), type.id());
        if (able.isEmpty()) {
            throw Expect.failure(helper, helper.relativePos(BlockPos.of(network.cables().getFirst())),
                    "a request of " + type.id() + ": expected a member that carries it out, found "
                            + network.members().size() + " members, none of them able");
        }
        return type.handler().execute(args);
    }

    /**
     * An Operation dispatcher of {@code queues} queues, ticked with the server for as long as the test runs, to queue
     * Operations on and follow them.
     */
    public static Dispatch dispatch(final GameTestHelper helper, final int queues) {
        final OperationDispatch dispatch = new OperationDispatch(queues);
        final Dispatch scene = new Dispatch(dispatch);
        helper.onEachTick(scene::tick);
        return scene;
    }

    /**
     * A data network laid by a test.
     *
     * @param id     its id
     * @param cables the places of its cables, in the world
     * @param level  where it is laid
     */
    public record Network(NetworkUuid id, List<Long> cables, ServerLevel level) {

        /** The members on it now. */
        public List<NetworkSystem.MemberNode> members() {
            return NetworkSystem.get(level).membersOf(id);
        }

        /** That {@code count} members are on it; else a failure saying how many are. */
        public void expectMembers(final GameTestHelper helper, final int count) {
            Expect.equal(helper, helper.relativePos(BlockPos.of(cables.getFirst())), "the members on the network",
                    count, members().size());
        }

        /** Takes the network up again: every cable gone, and the network forgotten. */
        public void close() {
            final ConnectivityIndex connectivity = NetworkSystem.get(level).connectivity();
            for (final long cable : cables) {
                connectivity.onCableRemovedIfRegistered(cable);
                connectivity.dropStandIn(cable);
            }
            connectivity.clearNetwork(id);
        }
    }

    /** A dispatcher a test queues Operations on, ticked with the server. */
    public static final class Dispatch {

        private final OperationDispatch dispatch;

        private Dispatch(final OperationDispatch dispatch) {
            this.dispatch = dispatch;
        }

        /** Queues {@code task} at the default priority, and gives back its id. */
        public UUID submit(final IOperationTask task) {
            return dispatch.submit(task, OperationPriority.MEDIUM);
        }

        /** The status of the Operation {@code id} now. */
        public OperationStatus statusOf(final UUID id) {
            return dispatch.statusOf(id);
        }

        /** Why the Operation {@code id} failed, when it did. */
        public OperationFailure failureOf(final UUID id) {
            return dispatch.failureOf(id);
        }

        /** That the Operation {@code id} is in {@code status} now; else a failure saying where it is instead. */
        public void expect(final GameTestHelper helper, final BlockPos at, final UUID id,
                           final OperationStatus status) {
            Expect.equal(helper, at, "the Operation " + id, status, statusOf(id));
        }

        /** The dispatcher itself, for what this does not say. */
        public OperationDispatch dispatcher() {
            return dispatch;
        }

        /** Stops the dispatcher, discarding what it still holds. */
        public void close() {
            dispatch.close();
        }

        private void tick() {
            dispatch.tick();
        }
    }
}
