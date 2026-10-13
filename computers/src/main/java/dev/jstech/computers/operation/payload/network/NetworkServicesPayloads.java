/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.network;

import dev.jstech.computers.blockentity.EngineReplacement;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.NetworkManagerApp;
import dev.jstech.computers.engine.EngineCapability;
import dev.jstech.computers.engine.EngineDef;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.EngineActionPayload;
import dev.jstech.computers.operation.payload.NetworkServicesPayload;
import dev.jstech.computers.operation.payload.RequestNetworkServicesPayload;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.network.SubframeNode;
import dev.jstech.core.util.ShortId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * The Network Manager's Services tab: what a Mainframe runs for its network, and stopping, starting and replacing
 * the engine that plans the network's work. Only a player at that Mainframe's own screens asks or acts.
 */
public final class NetworkServicesPayloads {

    private NetworkServicesPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestNetworkServicesPayload.TYPE, RequestNetworkServicesPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestNetworkServicesPayload::hostPos),
                NetworkServicesPayloads::handleRequest);
        ComputerAccess.accept(registrar, EngineActionPayload.TYPE, EngineActionPayload.STREAM_CODEC,
                ComputerAccess.machine(EngineActionPayload::hostPos), NetworkServicesPayloads::handleAction);
        registrar.playToClient(NetworkServicesPayload.TYPE, NetworkServicesPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(NetworkServicesPayloads::handleServices));
    }

    /** What the Services tab of the Network Manager on {@code mainframe} shows now. */
    public static NetworkServicesPayload collect(final MainframeBlockEntity mainframe) {
        final EngineReplacement replacing = mainframe.engineReplacement();
        final ResourceLocation active = mainframe.activeEngine();
        final List<NetworkServicesPayload.EngineRow> installed = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, String> entry : mainframe.installedEngines().entrySet()) {
            if (installed.size() >= NetworkServicesPayload.MAX_ENGINES) {
                break;
            }
            final ResourceLocation program = entry.getKey();
            installed.add(new NetworkServicesPayload.EngineRow(program.toString(), clip(nameOf(program)),
                    clip(vendorOf(program)), clip(entry.getValue()), rowState(mainframe, program, replacing)));
        }
        final List<NetworkServicesPayload.SubframeRow> subframes = new ArrayList<>();
        for (final SubframeNode subframe : mainframe.networkSubframes()) {
            if (subframes.size() >= NetworkServicesPayload.MAX_SUBFRAMES) {
                break;
            }
            subframes.add(new NetworkServicesPayload.SubframeRow("SUB-" + ShortId.of(subframe.nodeUuid().asString()),
                    clip(subframe.software()), clip(subframeEngine(subframe)), mainframe.takesWork(subframe)));
        }
        final List<NetworkServicesPayload.ServiceRow> services = List.of(
                serviceRow(Programs.AUTOMATION_ENGINE, mainframe.isAutomationEngineActive()),
                serviceRow(Programs.MIRROR, mainframe.isMirrorActive()));
        return new NetworkServicesPayload(mainframe.getBlockPos(), engineOf(mainframe, active, replacing), installed,
                subframes, services, replacementOf(mainframe, replacing));
    }

    private static void handleRequest(final RequestNetworkServicesPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof MainframeBlockEntity mainframe) {
            PacketDistributor.sendToPlayer(player, collect(mainframe));
        }
    }

    /*
     * Each action does what the button says or nothing at all; the fresh state that follows either way is what tells
     * the player which, the way the tab would read on its next refresh.
     */
    private static void handleAction(final EngineActionPayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof MainframeBlockEntity mainframe)) {
            return;
        }
        final ResourceLocation program = ResourceLocation.tryParse(payload.program());
        switch (payload.action()) {
            case EngineActionPayload.STOP -> mainframe.setEngineRunning(false);
            case EngineActionPayload.START -> {
                if (program != null && program.equals(mainframe.activeEngine())) {
                    mainframe.setEngineRunning(true);
                } else if (program != null) {
                    mainframe.activateEngine(program);
                }
            }
            case EngineActionPayload.REPLACE -> {
                if (program != null) {
                    mainframe.replaceEngine(program);
                }
            }
            default -> {
                return;
            }
        }
        PacketDistributor.sendToPlayer(player, collect(mainframe));
    }

    private static void handleServices(final NetworkServicesPayload payload, final Player player) {
        NetworkManagerApp.acceptServices(payload);
    }

    /* The engine chosen, or the one a replacement is bringing up, with what it spends and does. */
    private static NetworkServicesPayload.Engine engineOf(final MainframeBlockEntity mainframe,
                                                          @Nullable final ResourceLocation active,
                                                          @Nullable final EngineReplacement replacing) {
        final ResourceLocation shown = replacing != null ? replacing.to() : active;
        final String host = clip(hostName(mainframe));
        final int inFlight = mainframe.liveOperations().size();
        if (shown == null) {
            return new NetworkServicesPayload.Engine("", "", "", "", host, NetworkServicesPayload.ENGINE_NONE, "", 0,
                    List.of(), mainframe.indexedTypes(), mainframe.indexedServers(), 0L, mainframe.plansToday(),
                    inFlight);
        }
        final byte state = replacing != null ? NetworkServicesPayload.ENGINE_REPLACING
                : mainframe.runningEngine() != null ? NetworkServicesPayload.ENGINE_RUNNING
                : NetworkServicesPayload.ENGINE_STOPPED;
        final EngineDef def = NetworkEngines.def(shown);
        final List<String> capabilities = new ArrayList<>();
        if (def != null) {
            for (final EngineCapability capability : EngineCapability.values()) {
                if (def.offers(capability)) {
                    capabilities.add(capability.serializedName());
                }
            }
        }
        final ProgramSpec spec = OsRegistry.getProgram(shown);
        return new NetworkServicesPayload.Engine(shown.toString(), clip(nameOf(shown)),
                clip(mainframe.installedEngines().getOrDefault(shown, "")), clip(vendorOf(shown)), host, state,
                clip(def == null ? "" : def.dialect()), spec == null ? 0 : spec.ramMb(), capabilities,
                mainframe.indexedTypes(), mainframe.indexedServers(), mainframe.engineUpTicks(),
                mainframe.plansToday(), inFlight);
    }

    private static NetworkServicesPayload.Replacement replacementOf(final MainframeBlockEntity mainframe,
                                                                    @Nullable final EngineReplacement replacing) {
        if (replacing == null || mainframe.getLevel() == null) {
            return NetworkServicesPayload.Replacement.NONE;
        }
        final String to = nameOf(replacing.to()) + " "
                + mainframe.installedEngines().getOrDefault(replacing.to(), "");
        return new NetworkServicesPayload.Replacement(replacing.from() == null ? "" : clip(nameOf(replacing.from())),
                clip(to.trim()), (int) replacing.elapsed(mainframe.getLevel().getGameTime()), replacing.ticks(),
                replacing.inFlight(), replacing.itemTypes());
    }

    /* Where an installed engine stands: coming up, the chosen one running or stopped, or only installed. */
    private static byte rowState(final MainframeBlockEntity mainframe, final ResourceLocation program,
                                 @Nullable final EngineReplacement replacing) {
        if (replacing != null && program.equals(replacing.to())) {
            return NetworkServicesPayload.ROW_STARTING;
        }
        if (program.equals(mainframe.activeEngine())) {
            return mainframe.runningEngine() != null ? NetworkServicesPayload.ROW_ACTIVE
                    : NetworkServicesPayload.ROW_STOPPED;
        }
        return NetworkServicesPayload.ROW_INSTALLED;
    }

    private static NetworkServicesPayload.ServiceRow serviceRow(final ResourceLocation program, final boolean running) {
        return new NetworkServicesPayload.ServiceRow(clip(nameOf(program)), clip(vendorOf(program)),
                clip(ProgramVersions.of(program)),
                running ? NetworkServicesPayload.SERVICE_RUNNING : NetworkServicesPayload.SERVICE_ABSENT);
    }

    /* The engine a Subframe runs, by name and version, or nothing when it runs whatever its Mainframe runs. */
    private static String subframeEngine(final SubframeNode subframe) {
        if (subframe.software().isEmpty()) {
            return "";
        }
        final ResourceLocation program = ResourceLocation.tryParse(subframe.software());
        return program == null ? subframe.software() : nameOf(program);
    }

    /** What the network calls its Mainframe: the name a player gave it, or its kind and short id. */
    private static String hostName(final MainframeBlockEntity mainframe) {
        if (!mainframe.customName().isEmpty()) {
            return mainframe.customName();
        }
        return mainframe.nodeUuid() == null ? "MF" : "MF-" + ShortId.of(mainframe.nodeUuid().asString());
    }

    private static String nameOf(final ResourceLocation program) {
        final ProgramSpec spec = OsRegistry.getProgram(program);
        return spec == null ? program.toString() : spec.displayName();
    }

    private static String vendorOf(final ResourceLocation program) {
        final ProgramSpec spec = OsRegistry.getProgram(program);
        return spec == null || spec.house().bundled() ? "" : spec.house().name();
    }

    private static String clip(final String text) {
        return NetworkServicesPayload.clip(text);
    }
}
