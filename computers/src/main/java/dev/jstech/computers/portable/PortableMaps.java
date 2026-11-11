/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.AccessPointBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.operation.payload.portable.PortableMapPayload;
import dev.jstech.computers.operation.payload.portable.PortableProspectPayload;
import dev.jstech.computers.wireless.AccessPoints;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.worldgen.vein.DepositType;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.core.worldgen.vein.Prospecting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A portable's GPS on the server (computers/docs/design/wireless.md): what its Maps app marks on J's Core's map, the
 * network's Mainframe and its access points with their reach, and what its prospecting finds, asked of J's Core's
 * world kit, which gives each vein near only the way it lies and how strongly it shows, never where it is.
 */
public final class PortableMaps {

    /** How far round the device prospecting looks, in blocks. */
    public static final int PROSPECT_RADIUS = 64;
    private static final double FULL_TURN = 360.0;
    /* A material's colour when the vein's is not known. */
    private static final int UNKNOWN_COLOUR = 0x808080;

    private PortableMaps() {
    }

    /** Sends {@code player} what the device's Maps app marks besides them. */
    public static void map(final ServerPlayer player, final ServerLevel level, final UUID device) {
        PacketDistributor.sendToPlayer(player, mapOf(level, device));
    }

    /** What the Maps app of the device {@code device} marks besides its player. */
    public static PortableMapPayload mapOf(final ServerLevel level, final UUID device) {
        final PortableLinks.Joined joined = PortableLinks.of(level, device);
        final NetworkUuid network = joined == null ? null : joined.network();
        Optional<BlockPos> mainframe = Optional.empty();
        final List<PortableMapPayload.Point> points = new ArrayList<>();
        if (network != null) {
            final MainframeBlockEntity found = NetworkLookup.resolveMainframe(level, network);
            if (found != null && found.getLevel() == level) {
                mainframe = Optional.of(found.getBlockPos());
            }
            for (final AccessPointBlockEntity point : AccessPoints.on(level, network)) {
                points.add(new PortableMapPayload.Point(point.getBlockPos(),
                        AccessPointBlockEntity.hostname(point.nodeUuid()), point.standard().reachBlocks()));
            }
        }
        return new PortableMapPayload(device, mainframe, points);
    }

    /** Sends {@code player} the veins the device's prospecting finds round them, the strongest first. */
    public static void prospect(final ServerPlayer player, final ServerLevel level, final UUID device) {
        final PortableProspectPayload found = prospectOf(level, device, player.blockPosition());
        if (!found.veins().isEmpty()) {
            JscEvents.award(player, JscEvents.VEIN_FOUND);
        }
        PacketDistributor.sendToPlayer(player, found);
    }

    /** The veins the prospecting of the device {@code device} finds round {@code at}, the strongest first. */
    public static PortableProspectPayload prospectOf(final ServerLevel level, final UUID device, final BlockPos at) {
        final List<Prospecting.Prospect> found = new ArrayList<>(Prospecting.prospect(level, at, PROSPECT_RADIUS));
        found.sort(Comparator.comparingDouble(Prospecting.Prospect::strength).reversed());
        final List<PortableProspectPayload.Vein> veins = new ArrayList<>();
        for (final Prospecting.Prospect prospect : found) {
            if (veins.size() >= PortableProspectPayload.MOST_VEINS) {
                break;
            }
            final Optional<Material> material = mainMaterial(prospect.deposit());
            veins.add(new PortableProspectPayload.Vein(
                    material.map(Material::name).orElse(Text.literal(prospect.deposit().getPath())),
                    material.map(Material::colour).orElse(UNKNOWN_COLOUR), bearing(prospect.direction()),
                    (float) prospect.strength()));
        }
        return new PortableProspectPayload(device, veins);
    }

    /** Which way {@code direction} points, in degrees clockwise from north, as a compass reads it. */
    public static float bearing(final Vec3 direction) {
        final double degrees = Math.toDegrees(Math.atan2(direction.x, -direction.z));
        return (float) ((degrees % FULL_TURN + FULL_TURN) % FULL_TURN);
    }

    /* What a vein of {@code deposit} is made of most: its heaviest mineral, or the moved ore it is named after. */
    private static Optional<Material> mainMaterial(final ResourceLocation deposit) {
        final Optional<Deposits.Entry> entry = Deposits.get(deposit);
        if (entry.isEmpty() || !entry.get().targets().isEmpty()) {
            return Materials.byId(deposit);
        }
        return entry.get().type().minerals().stream()
                .max(Comparator.comparingDouble(DepositType.Mineral::weight))
                .flatMap(mineral -> Materials.byId(mineral.material()));
    }
}
