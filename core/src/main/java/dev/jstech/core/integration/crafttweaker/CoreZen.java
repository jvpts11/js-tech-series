/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;
import dev.jstech.core.script.CoreScripts;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.openzen.zencode.java.ZenCodeType;

/**
 * What a CraftTweaker script reaches of the Core, the same calls KubeJS hands its scripts as {@code JsCore}
 * ({@link CoreScripts}): teams and what they know, the milestones they passed, the steps they reached, the materials
 * and which item stands for which.
 *
 * <pre>{@code
 * import mods.jscore.Core;
 * events.register<crafttweaker.neoforge.api.event.entity.player.PlayerLoggedInEvent>(event => {
 *     val team = Core.teamOf(event.entity);
 *     Core.grant(event.entity.level.server, team, "mymod:basics");
 * });
 * }</pre>
 */
@ZenRegister
@ZenCodeType.Name("mods.jscore.Core")
public final class CoreZen {

    private CoreZen() {
    }

    /** The team of {@code player}: their own when they are on none. */
    @ZenCodeType.Method
    public static String teamOf(final Player player) {
        return player.getServer() == null ? "" : CoreScripts.teamOf(player.getServer(), player.getUUID());
    }

    @ZenCodeType.Method
    public static List<String> knowledge() {
        return CoreScripts.knowledge();
    }

    @ZenCodeType.Method
    public static boolean knows(final MinecraftServer server, final String team, final String knowledge) {
        return CoreScripts.knows(server, team, knowledge);
    }

    @ZenCodeType.Method
    public static double share(final MinecraftServer server, final String team, final String knowledge) {
        return CoreScripts.share(server, team, knowledge);
    }

    /** Has {@code team} know {@code share} of {@code knowledge}, the whole of it unless said. */
    @ZenCodeType.Method
    public static boolean grant(final MinecraftServer server, final String team, final String knowledge,
                                @ZenCodeType.OptionalDouble(1.0) final double share) {
        return CoreScripts.grant(server, team, knowledge, share);
    }

    @ZenCodeType.Method
    public static void forget(final MinecraftServer server, final String team, final String knowledge) {
        CoreScripts.forget(server, team, knowledge);
    }

    @ZenCodeType.Method
    public static List<String> milestones() {
        return CoreScripts.milestones();
    }

    @ZenCodeType.Method
    public static boolean reached(final MinecraftServer server, final String team, final String milestone) {
        return CoreScripts.reached(server, team, milestone);
    }

    @ZenCodeType.Method
    public static boolean reach(final MinecraftServer server, final String team, final String milestone) {
        return CoreScripts.reach(server, team, milestone);
    }

    @ZenCodeType.Method
    public static List<String> axes() {
        return CoreScripts.axes();
    }

    @ZenCodeType.Method
    public static List<String> steps(final String axis) {
        return CoreScripts.steps(axis);
    }

    @ZenCodeType.Method
    public static String step(final MinecraftServer server, final String team, final String axis) {
        return CoreScripts.step(server, team, axis);
    }

    @ZenCodeType.Method
    public static boolean reachStep(final MinecraftServer server, final String team, final String axis,
                                    final String step) {
        return CoreScripts.reachStep(server, team, axis, step);
    }

    @ZenCodeType.Method
    public static List<String> materials() {
        return CoreScripts.materials();
    }

    /** The thing of {@code material} in {@code form}, or the empty stack. */
    @ZenCodeType.Method
    public static IItemStack material(final String material, final String form) {
        return IItemStack.of(CoreScripts.material(material, form));
    }

    /** The id of the item that stands for {@code item} in recipes and drops. */
    @ZenCodeType.Method
    public static String unified(final String item) {
        return CoreScripts.unified(item);
    }

    @ZenCodeType.Method
    public static List<String> properties() {
        return CoreScripts.properties();
    }

    /** The value of {@code property} for {@code material}, as JSON text; nothing when it has none. */
    @ZenCodeType.Method
    public static String property(final String material, final String property) {
        return CoreScripts.property(material, property);
    }

    @ZenCodeType.Method
    public static List<String> dataKinds() {
        return CoreScripts.dataKinds();
    }

    @ZenCodeType.Method
    public static List<String> dataIds(final String kind) {
        return CoreScripts.dataIds(kind);
    }

    /** The file {@code id} of the kind of data {@code kind}, as JSON text; nothing when the world has none. */
    @ZenCodeType.Method
    public static String data(final String kind, final String id) {
        return CoreScripts.data(kind, id);
    }

    @ZenCodeType.Method
    public static List<String> dataMaps() {
        return CoreScripts.dataMaps();
    }

    /** The value the data map {@code map} gives {@code entry}, as JSON text; nothing when it gives none. */
    @ZenCodeType.Method
    public static String dataMap(final String map, final String entry) {
        return CoreScripts.dataMap(map, entry);
    }
}
