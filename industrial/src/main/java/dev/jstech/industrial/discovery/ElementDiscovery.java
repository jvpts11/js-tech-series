/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.discovery;

import dev.jstech.core.advancement.Advancements;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.ElementNaming;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.Superheavies;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.progression.Knowledge;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.ProgressionTriggers;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.JsIndustrial;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * What counts as discovering an element: its first synthesis by a team. The elements science discovered by making
 * them (technetium, promethium, astatine, everything from neptunium on, and every element beyond the known table) are
 * discovered the first time a team makes one, in any form, an isotope of it among them; the team knows the element
 * from then on, which the research of its stable isotope asks for, and its players earn the Discovery advancement.
 * The player who counts is the owner of the machine that made it, who may be away: they earn the advancement as they
 * join. For an element beyond the known table, the first on the server also gives it its name ({@link ElementNaming}).
 * Mining what holds traces of one discovers nothing: only making it does.
 */
@TextHolder
public final class ElementDiscovery {

    /** The mod's event of a discovery made, which the Discovery tab's root asks for. */
    public static final ResourceLocation DISCOVERY_MADE = ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID,
            "discovery_made");
    /** What the knowledge of an element beyond the known table with no series' name is called. */
    public static final TextKey BEYOND_NAME = TextKey.of("jsindustrial.knowledge.element_beyond",
            "An element beyond the known table");
    /* The elements discovered by being made, each with the knowledge a team gets of it. */
    private static final Map<Material, Knowledge> BY_SYNTHESIS = new LinkedHashMap<>();
    /* The first element from which every one is made, none found in nature: neptunium. */
    private static final int FIRST_MADE = 93;

    static {
        for (final Element element : Element.values()) {
            if (element == Element.TECHNETIUM || element == Element.PROMETHIUM || element == Element.ASTATINE
                    || element.number() >= FIRST_MADE) {
                put(Elements.of(element), element.path(), element.nameKey());
            }
        }
        Superheavies.all().forEach((number, material) -> put(material, material.id().getPath(),
                Superheavies.seriesName(number).orElse(BEYOND_NAME)));
    }

    private ElementDiscovery() {
    }

    /** Declares the knowledge and listens for what is made, once, from the mod's constructor. */
    public static void declare() {
        BY_SYNTHESIS.values().forEach(Knowledges::declare);
        ProgressionTriggers.whenMade(ElementDiscovery::made);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, ElementDiscovery::onLoggedIn);
    }

    /** Every element discovered by being made, with the knowledge of it, in the order of the table. */
    public static Map<Material, Knowledge> bySynthesis() {
        return Collections.unmodifiableMap(BY_SYNTHESIS);
    }

    /** The knowledge of the element {@code material} is, or is an isotope of, when it is discovered by being made. */
    public static Optional<Knowledge> knowledge(final Material material) {
        final Material element = material.isotope().map(Material.Isotope::element).orElse(material);
        return Optional.ofNullable(BY_SYNTHESIS.get(element));
    }

    /**
     * The player {@code by} made {@code material}: the first time their team makes an element discovered by being
     * made, it is discovered. Whether it was, now.
     */
    public static boolean discover(final MinecraftServer server, final UUID by, final Material material) {
        final Optional<Knowledge> knowledge = knowledge(material);
        if (knowledge.isEmpty()) {
            return false;
        }
        final Material element = material.isotope().map(Material.Isotope::element).orElse(material);
        // The first on the server names an element beyond the known table, whether or not their team knew it.
        ElementNaming.discover(server, by, element);
        if (!TeamProgress.grant(server, CoreTeams.teamOf(server, by), knowledge.get(), 1.0)) {
            return false;
        }
        final ServerPlayer player = server.getPlayerList().getPlayer(by);
        if (player != null) {
            Advancements.award(player, DISCOVERY_MADE, element.id().getPath());
        }
        return true;
    }

    private static void made(final MinecraftServer server, final UUID by, final ItemStack stack) {
        Materials.of(stack.getItem()).ifPresent(thing -> discover(server, by, thing.material()));
    }

    /* A player whose team discovered an element while they were away earns the advancement as they join. */
    private static void onLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer
                || player.getServer() == null) {
            return;
        }
        final String team = CoreTeams.teamOf(player.getServer(), player.getUUID());
        for (final Map.Entry<Material, Knowledge> entry : BY_SYNTHESIS.entrySet()) {
            if (TeamProgress.knows(player.getServer(), team, entry.getValue().id())) {
                Advancements.award(player, DISCOVERY_MADE, entry.getKey().id().getPath());
            }
        }
    }

    private static void put(final Material element, final String path, final TextKey name) {
        BY_SYNTHESIS.put(element, Knowledge.of(ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID,
                "element/" + path), name));
    }
}
