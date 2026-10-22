/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.studio.CameraPathRecorder;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.moving.ForceDiagramTexts;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureForcesPayload;
import dev.jstech.core.moving.StructureForcesWatchPayload;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.text.GameText;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The forces on moving structures as a player's game is told them, while it looks: the diagram of forces open, a
 * physics tool holding one, the physics shown in the world. Its key opens the diagram of the structure the player
 * stands on, sits in or looks at.
 */
public final class StructureForcesClient {

    /* The last forces told of each structure looked at, by its entity. */
    private static final Int2ObjectMap<StructureForcesPayload> TOLD = new Int2ObjectOpenHashMap<>();
    /* The structures looked at now, by their entity, each as many times as it is looked at. */
    private static final Int2ObjectMap<int[]> LOOKED = new Int2ObjectOpenHashMap<>();

    private StructureForcesClient() {
    }

    /** Hands the key its press, once, from the Core's client. */
    public static void listen() {
        KeyActionsClient.onPress(CoreKeys.STRUCTURE_FORCES, StructureForcesClient::open);
    }

    /** The forces last told of the structure of {@code entity}, or null while none came. */
    public static @Nullable StructureForcesPayload of(final int entity) {
        return TOLD.get(entity);
    }

    /** Begins looking at the forces on the structure of {@code entity}: the server tells them till it stops. */
    public static void look(final int entity) {
        final int[] count = LOOKED.computeIfAbsent(entity, any -> new int[1]);
        if (count[0]++ == 0) {
            PacketDistributor.sendToServer(new StructureForcesWatchPayload(entity, true));
        }
    }

    /** Stops looking at the forces on the structure of {@code entity}, once for each time it began. */
    public static void stopLooking(final int entity) {
        final int[] count = LOOKED.get(entity);
        if (count == null) {
            return;
        }
        if (--count[0] <= 0) {
            LOOKED.remove(entity);
            TOLD.remove(entity);
            if (Minecraft.getInstance().getConnection() != null) {
                PacketDistributor.sendToServer(new StructureForcesWatchPayload(entity, false));
            }
        }
    }

    /** Every structure looked at now, by its entity. */
    public static IntSet looked() {
        return new IntOpenHashSet(LOOKED.keySet());
    }

    /** The server told the forces on a structure looked at. */
    static void told(final StructureForcesPayload payload) {
        if (LOOKED.containsKey(payload.entity())) {
            TOLD.put(payload.entity(), payload);
        }
    }

    /**
     * The structure the player has before them: the one whose seat they sit in, the one they stand on, or the one
     * whose block they look at; null for none.
     */
    public static @Nullable ClientStructure before(final LocalPlayer player) {
        if (player.getVehicle() instanceof StructureSeatEntity seat && seat.structure() instanceof ClientStructure in) {
            return in;
        }
        if (StructureFooting.standingOn(player) instanceof ClientStructure on) {
            return on;
        }
        final HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                && YardLayout.isYardBlock(block.getBlockPos().getX())) {
            final IStructureBody seen = MovingStructures.at(player.level(), block.getBlockPos());
            return seen instanceof ClientStructure structure ? structure : null;
        }
        return null;
    }

    /* The key: the diagram of the structure before the player, or a word that there is none. */
    private static void open() {
        final Minecraft game = Minecraft.getInstance();
        // While the studio records a camera path, the key keeps keyframes, as its line says, unless rebound.
        if (game.player == null || game.screen != null || CameraPathRecorder.recording()) {
            return;
        }
        final ClientStructure structure = before(game.player);
        if (structure == null) {
            game.player.displayClientMessage(GameText.component(ForceDiagramTexts.NOTHING_SEEN), true);
            return;
        }
        game.setScreen(new ForceDiagramScreen(structure));
    }
}
