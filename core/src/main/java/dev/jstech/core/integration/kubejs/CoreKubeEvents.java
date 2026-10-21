/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.kubejs;

import dev.jstech.core.JsCore;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.event.IOperationLifecycleEvent;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEvent;
import dev.jstech.core.network.NetworkMemberEvent;
import dev.jstech.core.progression.AxisStepReachedEvent;
import dev.jstech.core.progression.TeamProgressEvent;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.RoomEvent;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * The Core's events as a modpack's server scripts hear them, under {@code JsCoreEvents}: what a team learns, the
 * milestones it passes and the steps it reaches along an axis, a player's own steps, the sealed rooms forming,
 * changing, filling, leaking and going, the moving structures striking, parting, docking and undocking, and a
 * dimension's hazard about to fall on a player, which a script cancels to spare them:
 *
 * <pre>{@code
 * JsCoreEvents.knowledgeGained(event => {
 *   if (event.learnt && event.knowledge == 'mymod:fusion') console.info('Fusion is known to ' + event.team)
 * })
 * JsCoreEvents.dimensionHazard(event => { if (event.player.creative) event.cancel() })
 * }</pre>
 *
 * Each is passed on only while a script listens for it.
 */
final class CoreKubeEvents {

    /** The group scripts write as {@code JsCoreEvents}. */
    static final EventGroup GROUP = EventGroup.of("JsCoreEvents");
    static final EventHandler KNOWLEDGE_GAINED = GROUP.server("knowledgeGained", () -> Knowledge.class);
    static final EventHandler MILESTONE_REACHED = GROUP.server("milestoneReached", () -> Milestone.class);
    static final EventHandler TEAM_STEP_REACHED = GROUP.server("teamStepReached", () -> TeamStep.class);
    static final EventHandler PLAYER_STEP_REACHED = GROUP.server("playerStepReached", () -> PlayerStep.class);
    static final EventHandler ROOM_FORMED = GROUP.server("roomFormed", () -> RoomChange.class);
    static final EventHandler ROOM_CHANGED = GROUP.server("roomChanged", () -> RoomChange.class);
    static final EventHandler ROOM_FILLED = GROUP.server("roomFilled", () -> RoomChange.class);
    static final EventHandler ROOM_LEAKING = GROUP.server("roomLeaking", () -> RoomChange.class);
    static final EventHandler ROOM_REMOVED = GROUP.server("roomRemoved", () -> RoomChange.class);
    static final EventHandler STRUCTURE_COLLIDED = GROUP.server("structureCollided", () -> Collision.class);
    static final EventHandler STRUCTURE_PARTED = GROUP.server("structureParted", () -> Parting.class);
    static final EventHandler STRUCTURE_DOCKED = GROUP.server("structureDocked", () -> Docking.class);
    static final EventHandler STRUCTURE_UNDOCKED = GROUP.server("structureUndocked", () -> Docking.class);
    static final EventHandler DIMENSION_HAZARD = GROUP.server("dimensionHazard", () -> Hazard.class);
    static final EventHandler NETWORK_JOINED = GROUP.server("networkJoined", () -> NetworkChange.class);
    static final EventHandler NETWORK_LEFT = GROUP.server("networkLeft", () -> NetworkChange.class);
    static final EventHandler OPERATION_CREATED = GROUP.server("operationCreated", () -> Operation.class);
    static final EventHandler OPERATION_STARTED = GROUP.server("operationStarted", () -> Operation.class);
    static final EventHandler OPERATION_COMPLETED = GROUP.server("operationCompleted", () -> Operation.class);
    static final EventHandler OPERATION_FAILED = GROUP.server("operationFailed", () -> Operation.class);
    static final EventHandler OPERATION_DISCARDED = GROUP.server("operationDiscarded", () -> Operation.class);

    private CoreKubeEvents() {
    }

    /** Has the game's events passed on to the scripts, once, as KubeJS starts. */
    static void listen() {
        NeoForge.EVENT_BUS.addListener(TeamProgressEvent.KnowledgeGained.class, event -> post(KNOWLEDGE_GAINED,
                new Knowledge(event)));
        NeoForge.EVENT_BUS.addListener(TeamProgressEvent.MilestoneReached.class, event -> post(MILESTONE_REACHED,
                new Milestone(event)));
        NeoForge.EVENT_BUS.addListener(TeamProgressEvent.StepReached.class, event -> post(TEAM_STEP_REACHED,
                new TeamStep(event)));
        NeoForge.EVENT_BUS.addListener(AxisStepReachedEvent.class, event -> post(PLAYER_STEP_REACHED,
                new PlayerStep(event)));
        NeoForge.EVENT_BUS.addListener(RoomEvent.Formed.class, event -> post(ROOM_FORMED, new RoomChange(event)));
        NeoForge.EVENT_BUS.addListener(RoomEvent.Changed.class, event -> post(ROOM_CHANGED, new RoomChange(event)));
        NeoForge.EVENT_BUS.addListener(RoomEvent.Filled.class, event -> post(ROOM_FILLED, new RoomChange(event)));
        NeoForge.EVENT_BUS.addListener(RoomEvent.Leaking.class, event -> post(ROOM_LEAKING, new RoomChange(event)));
        NeoForge.EVENT_BUS.addListener(RoomEvent.Removed.class, event -> post(ROOM_REMOVED, new RoomChange(event)));
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Collision.class, event -> post(STRUCTURE_COLLIDED,
                new Collision(event)));
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Parted.class, event -> post(STRUCTURE_PARTED,
                new Parting(event)));
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Docked.class, event -> post(STRUCTURE_DOCKED,
                new Docking(event.structure(), event.host())));
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Undocked.class, event -> post(STRUCTURE_UNDOCKED,
                new Docking(event.structure(), event.host())));
        NeoForge.EVENT_BUS.addListener(DimensionHazardEvent.class, event -> {
            if (DIMENSION_HAZARD.hasListeners()
                    && DIMENSION_HAZARD.post(ScriptType.SERVER, new Hazard(event)).interruptFalse()) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(NetworkMemberEvent.Joined.class, event -> post(NETWORK_JOINED,
                new NetworkChange(event)));
        NeoForge.EVENT_BUS.addListener(NetworkMemberEvent.Left.class, event -> post(NETWORK_LEFT,
                new NetworkChange(event)));
        JsCore.events().subscribe(IOperationLifecycleEvent.class, event -> post(switch (event) {
            case IOperationLifecycleEvent.Created created -> OPERATION_CREATED;
            case IOperationLifecycleEvent.Started started -> OPERATION_STARTED;
            case IOperationLifecycleEvent.Completed completed -> OPERATION_COMPLETED;
            case IOperationLifecycleEvent.Failed failed -> OPERATION_FAILED;
            case IOperationLifecycleEvent.Discarded discarded -> OPERATION_DISCARDED;
        }, new Operation(event)));
    }

    private static void post(final EventHandler handler, final KubeEvent event) {
        if (handler.hasListeners()) {
            handler.post(ScriptType.SERVER, event);
        }
    }

    /** A team knows more of a piece of knowledge. */
    public static final class Knowledge implements KubeEvent {

        private final TeamProgressEvent.KnowledgeGained event;

        Knowledge(final TeamProgressEvent.KnowledgeGained event) {
            this.event = event;
        }

        public MinecraftServer getServer() {
            return this.event.server();
        }

        public String getTeam() {
            return this.event.team();
        }

        /** The knowledge's id, as {@code mymod:fusion}. */
        public String getKnowledge() {
            return this.event.knowledge().id().toString();
        }

        /** How much of it the team knew before, from 0 to 1. */
        public double getBefore() {
            return this.event.before();
        }

        /** How much of it the team knows now, from 0 to 1. */
        public double getNow() {
            return this.event.now();
        }

        /** Whether the team now knows the whole of it, and did not before. */
        public boolean isLearnt() {
            return this.event.learnt();
        }
    }

    /** A team passed a milestone. */
    public static final class Milestone implements KubeEvent {

        private final TeamProgressEvent.MilestoneReached event;

        Milestone(final TeamProgressEvent.MilestoneReached event) {
            this.event = event;
        }

        public MinecraftServer getServer() {
            return this.event.server();
        }

        public String getTeam() {
            return this.event.team();
        }

        /** The milestone's id. */
        public String getMilestone() {
            return this.event.milestone().id().toString();
        }
    }

    /** A team reached a further step along a progression axis. */
    public static final class TeamStep implements KubeEvent {

        private final TeamProgressEvent.StepReached event;

        TeamStep(final TeamProgressEvent.StepReached event) {
            this.event = event;
        }

        public MinecraftServer getServer() {
            return this.event.server();
        }

        public String getTeam() {
            return this.event.team();
        }

        /** The axis's id. */
        public String getAxis() {
            return this.event.axis().id().toString();
        }

        /** The step it was at, by the name its saves write. */
        public String getFrom() {
            return this.event.from().serializedName();
        }

        /** The step it reached. */
        public String getTo() {
            return this.event.to().serializedName();
        }

        /** How far along the axis the step it reached is, counted from the first. */
        public int getLevel() {
            return this.event.to().level();
        }
    }

    /** A player reached a further step along a progression axis of their own. */
    public static final class PlayerStep implements KubeEvent {

        private final AxisStepReachedEvent event;

        PlayerStep(final AxisStepReachedEvent event) {
            this.event = event;
        }

        public MinecraftServer getServer() {
            return this.event.server();
        }

        /** The player, by id. */
        public UUID getPlayer() {
            return this.event.player();
        }

        public String getAxis() {
            return this.event.axis().id().toString();
        }

        public String getFrom() {
            return this.event.from().serializedName();
        }

        public String getTo() {
            return this.event.to().serializedName();
        }

        public int getLevel() {
            return this.event.to().level();
        }
    }

    /** A sealed room formed, changed, filled, leaks or is gone. */
    public static final class RoomChange implements KubeEvent {

        private final RoomEvent event;

        RoomChange(final RoomEvent event) {
            this.event = event;
        }

        public ServerLevel getLevel() {
            return this.event.level();
        }

        /** The room: its volume, its air, its leaks and its bounds. */
        public Room getRoom() {
            return this.event.room();
        }
    }

    /** A moving structure struck a block or another structure. */
    public static final class Collision implements KubeEvent {

        private final MovingStructureEvent.Collision event;

        Collision(final MovingStructureEvent.Collision event) {
            this.event = event;
        }

        public MovingStructure getStructure() {
            return this.event.structure();
        }

        /** The structure it struck, or none for the world. */
        public @Nullable IStructureBody getOther() {
            return this.event.other();
        }

        /** The block struck: of the world, or of the other structure, a place of its yard. */
        public BlockPos getStruck() {
            return this.event.struck();
        }

        /** Where they met, in the world. */
        public Vec3 getAt() {
            return this.event.at();
        }

        /** How fast they closed there, in metres a second. */
        public double getSpeed() {
            return this.event.speed();
        }

        /** The energy of the blow, in joules. */
        public double getEnergy() {
            return this.event.energy();
        }
    }

    /** A moving structure broke into pieces. */
    public static final class Parting implements KubeEvent {

        private final MovingStructureEvent.Parted event;

        Parting(final MovingStructureEvent.Parted event) {
            this.event = event;
        }

        public MovingStructure getStructure() {
            return this.event.structure();
        }

        public List<MovingStructure> getPieces() {
            return this.event.pieces();
        }
    }

    /** A moving structure docked with another, or undocked from it. */
    public static final class Docking implements KubeEvent {

        private final MovingStructure visitor;
        private final MovingStructure host;

        Docking(final MovingStructure visitor, final MovingStructure host) {
            this.visitor = visitor;
            this.host = host;
        }

        public MovingStructure getStructure() {
            return this.visitor;
        }

        public MovingStructure getHost() {
            return this.host;
        }
    }

    /** A machine of any mod joined a data network or left one. */
    public static final class NetworkChange implements KubeEvent {

        private final NetworkMemberEvent event;

        NetworkChange(final NetworkMemberEvent event) {
            this.event = event;
        }

        public ServerLevel getLevel() {
            return this.event.level();
        }

        /** Where the member stands. */
        public BlockPos getPos() {
            return this.event.pos();
        }

        /** The member's identity on the network. */
        public String getNode() {
            return this.event.node().toString();
        }

        /** The network it joined or left. */
        public String getNetwork() {
            return this.event.network().toString();
        }
    }

    /** An Operation on a data network was made, began, completed, failed or was discarded. */
    public static final class Operation implements KubeEvent {

        private final IOperationLifecycleEvent event;

        Operation(final IOperationLifecycleEvent event) {
            this.event = event;
        }

        public @Nullable MinecraftServer getServer() {
            return ServerLifecycleHooks.getCurrentServer();
        }

        /** The network it runs on. */
        public String getNetwork() {
            return this.event.networkUuid().toString();
        }

        /** The Operation's own id. */
        public String getOperation() {
            return this.event.operationId().toString();
        }

        /** What kind of Operation it is, as {@code select} or {@code craft}. */
        public String getType() {
            return this.event.typeId();
        }

        /** How long it took, queue and work, in ticks, once it completed; 0 before. */
        public long getDurationTicks() {
            return this.event instanceof IOperationLifecycleEvent.Completed completed ? completed.durationTicks() : 0;
        }

        /** Why it failed, once it did; nothing before. */
        public String getReason() {
            return this.event instanceof IOperationLifecycleEvent.Failed failed ? failed.reason() : "";
        }
    }

    /** A dimension's hazard about to fall on a player; cancelled, it spares them this time. */
    public static final class Hazard implements KubeEvent {

        private final DimensionHazardEvent event;

        Hazard(final DimensionHazardEvent event) {
            this.event = event;
        }

        public Player getPlayer() {
            return this.event.player();
        }

        /** The hazard, by its name: {@code air}, {@code heat}, {@code cold}, {@code carbon_dioxide}, {@code oxygen}. */
        public String getHazard() {
            return this.event.hazard().name().toLowerCase(Locale.ROOT);
        }
    }
}
