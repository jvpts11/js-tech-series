/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.modpack;

import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.pack.ContentPacks;
import dev.jstech.core.text.GameText;
import dev.jstech.core.unify.Unification;
import dev.jstech.core.unify.UnifyPlan;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * What {@code /reload} did and what it could not, said once it is done to the server's operators and in the log: how
 * many recipes and loot tables the data holds now, what the unification made of them, and what waits for the game to
 * start again, since a reload never registers anything: content packs changed on disk, and settings changed in the
 * game that take hold only on a restart.
 */
public final class ReloadReport {

    private static final Logger LOGGER = CoreLogs.CONTENT.logger();
    /** The operators' level, who are told. */
    private static final int OPERATORS = 2;

    private ReloadReport() {
    }

    /** Hands the game's bus the report, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, ReloadReport::onSync);
    }

    /** What the server would say of a reload now. */
    public static List<Component> lines(final MinecraftServer server) {
        return lines(server.getRecipeManager().getRecipes().size(),
                server.reloadableRegistries().getKeys(Registries.LOOT_TABLE).size(), Unification.current(),
                !ContentPacks.digest().equals(ContentPacks.digestNow()), restartDue());
    }

    /**
     * What a reload says of data holding {@code recipes} recipes and {@code lootTables} loot tables, unified by
     * {@code plan}, with content packs that {@code packsChanged} on disk and settings waiting for a restart, by file.
     */
    public static List<Component> lines(final int recipes, final int lootTables, final UnifyPlan plan,
                                        final boolean packsChanged, final Map<String, Set<String>> settingsDue) {
        final List<Component> out = new ArrayList<>();
        out.add(GameText.component(ModpackTexts.RELOADED.with(recipes, lootTables)));
        if (!plan.isEmpty()) {
            out.add(GameText.component(ModpackTexts.UNIFIED.with(new HashSet<>(plan.replace().values()).size(),
                    plan.replace().size())));
        }
        if (packsChanged) {
            out.add(GameText.component(ModpackTexts.PACKS_WAIT));
        }
        new TreeMap<>(settingsDue).forEach((file, keys) -> out.add(GameText.component(ModpackTexts.SETTINGS_WAIT
                .with(file, String.join(", ", new TreeSet<>(keys))))));
        if (!packsChanged && settingsDue.isEmpty()) {
            out.add(GameText.component(ModpackTexts.NOTHING_WAITS));
        }
        return out;
    }

    /** The settings changed in the game that take hold only on a restart, by the name of their file. */
    public static Map<String, Set<String>> restartDue() {
        final Map<String, Set<String>> out = new TreeMap<>();
        for (final ConfigFile file : ConfigFiles.all()) {
            if (!file.restartDue().isEmpty()) {
                out.put(file.name(), file.restartDue());
            }
        }
        return out;
    }

    /* A reload sends the data to every player at once; a player joining is sent it alone, and is not a reload. */
    private static void onSync(final OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        final List<Component> lines = lines(event.getPlayerList().getServer());
        for (final Component line : lines) {
            LOGGER.info(line.getString());
        }
        for (final ServerPlayer player : event.getPlayerList().getPlayers()) {
            if (player.hasPermissions(OPERATORS)) {
                lines.forEach(player::sendSystemMessage);
            }
        }
    }
}
