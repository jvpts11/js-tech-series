/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.grid.LineGroup;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.research.DataKind;
import dev.jstech.core.research.Discoveries;
import dev.jstech.core.research.Discovery;
import dev.jstech.core.research.Research;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.industrial.research.GroupPower;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/** Works out what a Computational Research Centre's screen shows of it, on the server ({@link CrcReadings}). */
final class CrcScreenReadings {

    private CrcScreenReadings() {
    }

    /** What {@code crc}'s screen shows now. */
    static CrcReadings read(final CrcBlockEntity crc, final ServerLevel level) {
        final MinecraftServer server = level.getServer();
        final String team = crc.team(server);
        final Map<ResourceLocation, Float> shares = new HashMap<>();
        final Set<ResourceLocation> known = new HashSet<>();
        final Map<ResourceLocation, Integer> held = new HashMap<>();
        if (team != null) {
            for (final Discovery discovery : Discoveries.existing(level)) {
                final double share = Research.share(server, team, discovery);
                if (share > 0.0) {
                    shares.put(discovery.id(), (float) share);
                }
                for (final ResourceLocation after : discovery.after()) {
                    if (TeamProgress.knows(server, team, after)) {
                        known.add(after);
                    }
                }
            }
            final Research.IDataStore pool = CrcBlockEntity.pool(crc.mates(level, team));
            for (final DataKind kind : CrcBlockEntity.DATA) {
                held.put(kind.id(), pool.held(kind));
            }
        }
        return new CrcReadings((float) crc.lastPoints(), Math.round(crc.drawing()), Math.round(crc.lastMade()),
                Math.round(crc.shed()), crc.cooledByWater(), crc.cooledThroughPort(),
                team == null ? "" : teamName(server, team), shares, known, held, (float) crc.pace(), crc.outcome(),
                group(crc, level));
    }

    /* Its group as the Group tab draws it: its cable, its CRCs with itself first, and each team's share. */
    private static CrcReadings.Group group(final CrcBlockEntity crc, final ServerLevel level) {
        final MinecraftServer server = level.getServer();
        final List<CrcBlockEntity> crcs = new ArrayList<>(crc.group(level));
        crcs.sort(Comparator.comparing((CrcBlockEntity each) -> each != crc)
                .thenComparingLong(each -> each.getBlockPos().asLong()));
        final List<GroupPower.Member> powers = new ArrayList<>(crcs.size());
        final List<String> teams = new ArrayList<>(crcs.size());
        for (final CrcBlockEntity each : crcs) {
            final String team = each.team(server);
            teams.add(team == null ? "" : team);
            powers.add(new GroupPower.Member(team == null ? "" : team, each.lastPoints()));
        }
        final double[] gives = GroupPower.gives(powers);
        final List<CrcReadings.Member> members = new ArrayList<>(crcs.size());
        final Map<String, Float> shares = new LinkedHashMap<>();
        final Optional<Discovery> chosen = crc.chosenDiscovery().flatMap(Discoveries::byId);
        for (int i = 0; i < crcs.size(); i++) {
            final CrcBlockEntity each = crcs.get(i);
            final String team = teams.get(i);
            final boolean owned = !team.isEmpty();
            members.add(new CrcReadings.Member(each.getBlockPos(), team, owned ? teamName(server, team) : "",
                    (float) each.lastPoints(), owned ? (float) gives[i] : 0F, each.status()));
            if (owned && chosen.isPresent()) {
                shares.putIfAbsent(team, (float) Research.share(server, team, chosen.get()));
            }
        }
        return new CrcReadings.Group(cable(crc.run(level).orElse(null)), members, shares);
    }

    /* The cable's dye by its id, or none. */
    private static int cable(final @Nullable LineGroup run) {
        if (run == null) {
            return CrcReadings.Group.NO_CABLE;
        }
        return run.dye().map(DyeColor::getId).orElse(CrcReadings.Group.UNDYED);
    }

    /* A team's name as its players read it: a player's own team is the player's name. */
    private static String teamName(final MinecraftServer server, final String team) {
        return CoreTeams.soloPlayer(team).flatMap(player -> CoreTeams.nameOf(server, player)).orElse(team);
    }
}
