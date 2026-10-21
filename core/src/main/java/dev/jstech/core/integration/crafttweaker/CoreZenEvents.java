/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.event.NeoForgeEventCancellationCarrier;
import com.blamejared.crafttweaker.api.event.ZenEvent;
import com.blamejared.crafttweaker.api.event.bus.CommonWirelessEventBusWire;
import com.blamejared.crafttweaker.api.event.bus.IEventBus;
import com.blamejared.crafttweaker.api.event.bus.NeoForgeEventBusWire;
import com.blamejared.crafttweaker_annotations.annotations.NativeTypeRegistration;
import dev.jstech.core.JsCore;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.event.IOperationLifecycleEvent;
import dev.jstech.core.moving.MovingStructureEvent;
import dev.jstech.core.network.NetworkMemberEvent;
import dev.jstech.core.progression.AxisStepReachedEvent;
import dev.jstech.core.progression.TeamProgressEvent;
import dev.jstech.core.room.RoomEvent;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.openzen.zencode.java.ZenCodeType;

/**
 * The Core's events as CraftTweaker scripts hear them, under {@code mods.jscore.event}, the same KubeJS's scripts hear:
 * what a team learns, its milestones and steps, a player's steps, the sealed rooms, the moving structures, and a
 * dimension's hazard about to fall on a player, which a script cancels to spare them.
 *
 * <pre>{@code
 * events.register<mods.jscore.event.KnowledgeGained>(event => {
 *     if (event.learnt) println("The team " + event.team + " knows " + event.knowledge);
 * });
 * }</pre>
 */
public final class CoreZenEvents {

    private CoreZenEvents() {
    }

    /**
     * Passes the Operations' events, which the Core posts on its own bus rather than the game's, on to the scripts,
     * once, as the Core is made with CraftTweaker installed.
     */
    static void listen() {
        JsCore.events().subscribe(IOperationLifecycleEvent.class, event -> {
            switch (event) {
                case IOperationLifecycleEvent.Created created -> OperationCreated.BUS.post(created);
                case IOperationLifecycleEvent.Started started -> OperationStarted.BUS.post(started);
                case IOperationLifecycleEvent.Completed completed -> OperationCompleted.BUS.post(completed);
                case IOperationLifecycleEvent.Failed failed -> OperationFailed.BUS.post(failed);
                case IOperationLifecycleEvent.Discarded discarded -> OperationDiscarded.BUS.post(discarded);
            }
        });
    }

    /** What every event of a team's progress has. */
    @ZenRegister
    @NativeTypeRegistration(value = TeamProgressEvent.class, zenCodeName = "mods.jscore.event.TeamProgressEvent")
    public static final class TeamProgress {

        private TeamProgress() {
        }

        @ZenCodeType.Getter("server")
        public static MinecraftServer server(final TeamProgressEvent internal) {
            return internal.server();
        }

        @ZenCodeType.Getter("team")
        public static String team(final TeamProgressEvent internal) {
            return internal.team();
        }
    }

    /** A team knows more of a piece of knowledge. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = TeamProgressEvent.KnowledgeGained.class,
            zenCodeName = "mods.jscore.event.KnowledgeGained")
    public static final class KnowledgeGained {

        @ZenEvent.Bus
        public static final IEventBus<TeamProgressEvent.KnowledgeGained> BUS = IEventBus.direct(
                TeamProgressEvent.KnowledgeGained.class, NeoForgeEventBusWire.of());

        private KnowledgeGained() {
        }

        @ZenCodeType.Getter("knowledge")
        public static String knowledge(final TeamProgressEvent.KnowledgeGained internal) {
            return internal.knowledge().id().toString();
        }

        @ZenCodeType.Getter("before")
        public static double before(final TeamProgressEvent.KnowledgeGained internal) {
            return internal.before();
        }

        @ZenCodeType.Getter("now")
        public static double now(final TeamProgressEvent.KnowledgeGained internal) {
            return internal.now();
        }

        @ZenCodeType.Getter("learnt")
        public static boolean learnt(final TeamProgressEvent.KnowledgeGained internal) {
            return internal.learnt();
        }
    }

    /** A team passed a milestone. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = TeamProgressEvent.MilestoneReached.class,
            zenCodeName = "mods.jscore.event.MilestoneReached")
    public static final class MilestoneReached {

        @ZenEvent.Bus
        public static final IEventBus<TeamProgressEvent.MilestoneReached> BUS = IEventBus.direct(
                TeamProgressEvent.MilestoneReached.class, NeoForgeEventBusWire.of());

        private MilestoneReached() {
        }

        @ZenCodeType.Getter("milestone")
        public static String milestone(final TeamProgressEvent.MilestoneReached internal) {
            return internal.milestone().id().toString();
        }
    }

    /** A team reached a further step along an axis. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = TeamProgressEvent.StepReached.class,
            zenCodeName = "mods.jscore.event.TeamStepReached")
    public static final class TeamStepReached {

        @ZenEvent.Bus
        public static final IEventBus<TeamProgressEvent.StepReached> BUS = IEventBus.direct(
                TeamProgressEvent.StepReached.class, NeoForgeEventBusWire.of());

        private TeamStepReached() {
        }

        @ZenCodeType.Getter("axis")
        public static String axis(final TeamProgressEvent.StepReached internal) {
            return internal.axis().id().toString();
        }

        @ZenCodeType.Getter("from")
        public static String from(final TeamProgressEvent.StepReached internal) {
            return internal.from().serializedName();
        }

        @ZenCodeType.Getter("to")
        public static String to(final TeamProgressEvent.StepReached internal) {
            return internal.to().serializedName();
        }

        @ZenCodeType.Getter("level")
        public static int level(final TeamProgressEvent.StepReached internal) {
            return internal.to().level();
        }
    }

    /** A player reached a further step along an axis of their own. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = AxisStepReachedEvent.class, zenCodeName = "mods.jscore.event.PlayerStepReached")
    public static final class PlayerStepReached {

        @ZenEvent.Bus
        public static final IEventBus<AxisStepReachedEvent> BUS = IEventBus.direct(AxisStepReachedEvent.class,
                NeoForgeEventBusWire.of());

        private PlayerStepReached() {
        }

        @ZenCodeType.Getter("server")
        public static MinecraftServer server(final AxisStepReachedEvent internal) {
            return internal.server();
        }

        /** The player, by id. */
        @ZenCodeType.Getter("player")
        public static String player(final AxisStepReachedEvent internal) {
            return internal.player().toString();
        }

        @ZenCodeType.Getter("axis")
        public static String axis(final AxisStepReachedEvent internal) {
            return internal.axis().id().toString();
        }

        @ZenCodeType.Getter("from")
        public static String from(final AxisStepReachedEvent internal) {
            return internal.from().serializedName();
        }

        @ZenCodeType.Getter("to")
        public static String to(final AxisStepReachedEvent internal) {
            return internal.to().serializedName();
        }
    }

    /** What every event of a sealed room has. */
    @ZenRegister
    @NativeTypeRegistration(value = RoomEvent.class, zenCodeName = "mods.jscore.event.RoomEvent")
    public static final class Room {

        private Room() {
        }

        @ZenCodeType.Getter("level")
        public static ServerLevel level(final RoomEvent internal) {
            return internal.level();
        }

        /** The room's id in its level. */
        @ZenCodeType.Getter("room")
        public static long room(final RoomEvent internal) {
            return internal.room().id();
        }

        /** How many blocks of air it holds. */
        @ZenCodeType.Getter("volume")
        public static int volume(final RoomEvent internal) {
            return internal.room().volume();
        }

        @ZenCodeType.Getter("breached")
        public static boolean breached(final RoomEvent internal) {
            return internal.room().breached();
        }

        @ZenCodeType.Getter("underWater")
        public static boolean underWater(final RoomEvent internal) {
            return internal.room().underWater();
        }
    }

    /** A sealed room formed. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = RoomEvent.Formed.class, zenCodeName = "mods.jscore.event.RoomFormed")
    public static final class RoomFormed {

        @ZenEvent.Bus
        public static final IEventBus<RoomEvent.Formed> BUS = IEventBus.direct(RoomEvent.Formed.class,
                NeoForgeEventBusWire.of());

        private RoomFormed() {
        }
    }

    /** A sealed room changed. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = RoomEvent.Changed.class, zenCodeName = "mods.jscore.event.RoomChanged")
    public static final class RoomChanged {

        @ZenEvent.Bus
        public static final IEventBus<RoomEvent.Changed> BUS = IEventBus.direct(RoomEvent.Changed.class,
                NeoForgeEventBusWire.of());

        private RoomChanged() {
        }
    }

    /** A sealed room filled. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = RoomEvent.Filled.class, zenCodeName = "mods.jscore.event.RoomFilled")
    public static final class RoomFilled {

        @ZenEvent.Bus
        public static final IEventBus<RoomEvent.Filled> BUS = IEventBus.direct(RoomEvent.Filled.class,
                NeoForgeEventBusWire.of());

        private RoomFilled() {
        }
    }

    /** A sealed room leaks. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = RoomEvent.Leaking.class, zenCodeName = "mods.jscore.event.RoomLeaking")
    public static final class RoomLeaking {

        @ZenEvent.Bus
        public static final IEventBus<RoomEvent.Leaking> BUS = IEventBus.direct(RoomEvent.Leaking.class,
                NeoForgeEventBusWire.of());

        private RoomLeaking() {
        }
    }

    /** A sealed room is gone. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = RoomEvent.Removed.class, zenCodeName = "mods.jscore.event.RoomRemoved")
    public static final class RoomRemoved {

        @ZenEvent.Bus
        public static final IEventBus<RoomEvent.Removed> BUS = IEventBus.direct(RoomEvent.Removed.class,
                NeoForgeEventBusWire.of());

        private RoomRemoved() {
        }
    }

    /** What every event of a moving structure has. */
    @ZenRegister
    @NativeTypeRegistration(value = MovingStructureEvent.class, zenCodeName = "mods.jscore.event.StructureEvent")
    public static final class Structure {

        private Structure() {
        }

        /** The structure's id. */
        @ZenCodeType.Getter("structure")
        public static String structure(final MovingStructureEvent internal) {
            return internal.structure().id().toString();
        }
    }

    /** A moving structure struck a block or another structure. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = MovingStructureEvent.Collision.class,
            zenCodeName = "mods.jscore.event.StructureCollided")
    public static final class StructureCollided {

        @ZenEvent.Bus
        public static final IEventBus<MovingStructureEvent.Collision> BUS = IEventBus.direct(
                MovingStructureEvent.Collision.class, NeoForgeEventBusWire.of());

        private StructureCollided() {
        }

        @ZenCodeType.Getter("struck")
        public static BlockPos struck(final MovingStructureEvent.Collision internal) {
            return internal.struck();
        }

        @ZenCodeType.Getter("at")
        public static Vec3 at(final MovingStructureEvent.Collision internal) {
            return internal.at();
        }

        /** How fast they closed, in metres a second. */
        @ZenCodeType.Getter("speed")
        public static double speed(final MovingStructureEvent.Collision internal) {
            return internal.speed();
        }

        /** The energy of the blow, in joules. */
        @ZenCodeType.Getter("energy")
        public static double energy(final MovingStructureEvent.Collision internal) {
            return internal.energy();
        }
    }

    /** A moving structure broke into pieces. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = MovingStructureEvent.Parted.class,
            zenCodeName = "mods.jscore.event.StructureParted")
    public static final class StructureParted {

        @ZenEvent.Bus
        public static final IEventBus<MovingStructureEvent.Parted> BUS = IEventBus.direct(
                MovingStructureEvent.Parted.class, NeoForgeEventBusWire.of());

        private StructureParted() {
        }

        @ZenCodeType.Getter("pieces")
        public static int pieces(final MovingStructureEvent.Parted internal) {
            return internal.pieces().size();
        }
    }

    /** A moving structure docked with another. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = MovingStructureEvent.Docked.class,
            zenCodeName = "mods.jscore.event.StructureDocked")
    public static final class StructureDocked {

        @ZenEvent.Bus
        public static final IEventBus<MovingStructureEvent.Docked> BUS = IEventBus.direct(
                MovingStructureEvent.Docked.class, NeoForgeEventBusWire.of());

        private StructureDocked() {
        }

        @ZenCodeType.Getter("host")
        public static String host(final MovingStructureEvent.Docked internal) {
            return internal.host().id().toString();
        }
    }

    /** A moving structure undocked from another. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = MovingStructureEvent.Undocked.class,
            zenCodeName = "mods.jscore.event.StructureUndocked")
    public static final class StructureUndocked {

        @ZenEvent.Bus
        public static final IEventBus<MovingStructureEvent.Undocked> BUS = IEventBus.direct(
                MovingStructureEvent.Undocked.class, NeoForgeEventBusWire.of());

        private StructureUndocked() {
        }

        @ZenCodeType.Getter("host")
        public static String host(final MovingStructureEvent.Undocked internal) {
            return internal.host().id().toString();
        }
    }

    /** What every event of a machine joining or leaving a data network has. */
    @ZenRegister
    @NativeTypeRegistration(value = NetworkMemberEvent.class, zenCodeName = "mods.jscore.event.NetworkMemberEvent")
    public static final class NetworkMember {

        private NetworkMember() {
        }

        @ZenCodeType.Getter("level")
        public static ServerLevel level(final NetworkMemberEvent internal) {
            return internal.level();
        }

        @ZenCodeType.Getter("pos")
        public static BlockPos pos(final NetworkMemberEvent internal) {
            return internal.pos();
        }

        @ZenCodeType.Getter("node")
        public static String node(final NetworkMemberEvent internal) {
            return internal.node().toString();
        }

        @ZenCodeType.Getter("network")
        public static String network(final NetworkMemberEvent internal) {
            return internal.network().toString();
        }
    }

    /** A machine joined a data network. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = NetworkMemberEvent.Joined.class, zenCodeName = "mods.jscore.event.NetworkJoined")
    public static final class NetworkJoined {

        @ZenEvent.Bus
        public static final IEventBus<NetworkMemberEvent.Joined> BUS = IEventBus.direct(
                NetworkMemberEvent.Joined.class, NeoForgeEventBusWire.of());

        private NetworkJoined() {
        }
    }

    /** A machine left a data network. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = NetworkMemberEvent.Left.class, zenCodeName = "mods.jscore.event.NetworkLeft")
    public static final class NetworkLeft {

        @ZenEvent.Bus
        public static final IEventBus<NetworkMemberEvent.Left> BUS = IEventBus.direct(NetworkMemberEvent.Left.class,
                NeoForgeEventBusWire.of());

        private NetworkLeft() {
        }
    }

    /** What every event of an Operation's life has. */
    @ZenRegister
    @NativeTypeRegistration(value = IOperationLifecycleEvent.class,
            zenCodeName = "mods.jscore.event.OperationEvent")
    public static final class Operation {

        private Operation() {
        }

        /** The server the Operation runs on, or none outside one. */
        @ZenCodeType.Getter("server")
        public static @Nullable MinecraftServer server(final IOperationLifecycleEvent internal) {
            return ServerLifecycleHooks.getCurrentServer();
        }

        @ZenCodeType.Getter("network")
        public static String network(final IOperationLifecycleEvent internal) {
            return internal.networkUuid().toString();
        }

        @ZenCodeType.Getter("operation")
        public static String operation(final IOperationLifecycleEvent internal) {
            return internal.operationId().toString();
        }

        @ZenCodeType.Getter("type")
        public static String type(final IOperationLifecycleEvent internal) {
            return internal.typeId();
        }
    }

    /** An Operation was made on a data network. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = IOperationLifecycleEvent.Created.class,
            zenCodeName = "mods.jscore.event.OperationCreated")
    public static final class OperationCreated {

        @ZenEvent.Bus
        public static final IEventBus<IOperationLifecycleEvent.Created> BUS = IEventBus.direct(
                IOperationLifecycleEvent.Created.class, CommonWirelessEventBusWire.of());

        private OperationCreated() {
        }
    }

    /** An Operation began its work. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = IOperationLifecycleEvent.Started.class,
            zenCodeName = "mods.jscore.event.OperationStarted")
    public static final class OperationStarted {

        @ZenEvent.Bus
        public static final IEventBus<IOperationLifecycleEvent.Started> BUS = IEventBus.direct(
                IOperationLifecycleEvent.Started.class, CommonWirelessEventBusWire.of());

        private OperationStarted() {
        }
    }

    /** An Operation delivered everything it was asked for. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = IOperationLifecycleEvent.Completed.class,
            zenCodeName = "mods.jscore.event.OperationCompleted")
    public static final class OperationCompleted {

        @ZenEvent.Bus
        public static final IEventBus<IOperationLifecycleEvent.Completed> BUS = IEventBus.direct(
                IOperationLifecycleEvent.Completed.class, CommonWirelessEventBusWire.of());

        private OperationCompleted() {
        }

        /** How long it took, queue and work, in ticks. */
        @ZenCodeType.Getter("durationTicks")
        public static long durationTicks(final IOperationLifecycleEvent.Completed internal) {
            return internal.durationTicks();
        }
    }

    /** An Operation fell short. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = IOperationLifecycleEvent.Failed.class,
            zenCodeName = "mods.jscore.event.OperationFailed")
    public static final class OperationFailed {

        @ZenEvent.Bus
        public static final IEventBus<IOperationLifecycleEvent.Failed> BUS = IEventBus.direct(
                IOperationLifecycleEvent.Failed.class, CommonWirelessEventBusWire.of());

        private OperationFailed() {
        }

        @ZenCodeType.Getter("reason")
        public static String reason(final IOperationLifecycleEvent.Failed internal) {
            return internal.reason();
        }
    }

    /** An Operation was stopped or dropped. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = IOperationLifecycleEvent.Discarded.class,
            zenCodeName = "mods.jscore.event.OperationDiscarded")
    public static final class OperationDiscarded {

        @ZenEvent.Bus
        public static final IEventBus<IOperationLifecycleEvent.Discarded> BUS = IEventBus.direct(
                IOperationLifecycleEvent.Discarded.class, CommonWirelessEventBusWire.of());

        private OperationDiscarded() {
        }
    }

    /** A dimension's hazard about to fall on a player; cancelled, it spares them this time. */
    @ZenRegister
    @ZenEvent
    @NativeTypeRegistration(value = DimensionHazardEvent.class, zenCodeName = "mods.jscore.event.DimensionHazard")
    public static final class DimensionHazard {

        @ZenEvent.Bus
        public static final IEventBus<DimensionHazardEvent> BUS = IEventBus.cancelable(DimensionHazardEvent.class,
                NeoForgeEventBusWire.of(), NeoForgeEventCancellationCarrier.of());

        private DimensionHazard() {
        }

        @ZenCodeType.Getter("player")
        public static Player player(final DimensionHazardEvent internal) {
            return internal.player();
        }

        /** The hazard: {@code air}, {@code heat}, {@code cold}, {@code carbon_dioxide} or {@code oxygen}. */
        @ZenCodeType.Getter("hazard")
        public static String hazard(final DimensionHazardEvent internal) {
            return internal.hazard().name().toLowerCase(Locale.ROOT);
        }
    }
}
