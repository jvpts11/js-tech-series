/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.team;

import dev.jstech.core.JsCore;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContextKey;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.jetbrains.annotations.ApiStatus;

/**
 * The permissions the series asks the game's permission system about: one for each command, for opening, using,
 * setting up and breaking what others own, for changing a server's settings from afar, for building multiblocks, for
 * going past the limits each player has, and for being told when a part of the series stalls or slows itself. Each is
 * asked at a place, its dimension and claim told the permission mod as contexts ({@link #DIMENSION}, {@link #CLAIM}),
 * so a node may be granted in one dimension or claim and not another. A
 * server's permission mod (LuckPerms, FTB Ranks) grants or denies each; without one, the operator level each was
 * declared with decides. {@code /jstech dump permissions} writes them all out.
 */
@TextHolder
public final class CorePermissions {

    public static final TextKey USE_OTHERS_TEXT = TextKey.of("jscore.permission.use_others",
            "Use what other players own, whatever its access says");
    public static final TextKey OPEN_OTHERS_TEXT = TextKey.of("jscore.permission.open_others",
            "Open the machines other players own");
    public static final TextKey SET_UP_OTHERS_TEXT = TextKey.of("jscore.permission.set_up_others",
            "Change how the machines other players own are set up");
    public static final TextKey BREAK_OTHERS_TEXT = TextKey.of("jscore.permission.break_others",
            "Break what other players own");
    public static final TextKey EDIT_SETTINGS_TEXT = TextKey.of("jscore.permission.edit_settings",
            "Change a server's settings from the settings screen");
    public static final TextKey BUILD_MULTIBLOCKS_TEXT = TextKey.of("jscore.permission.build_multiblocks",
            "Build multiblock structures");
    public static final TextKey PASS_LIMITS_TEXT = TextKey.of("jscore.permission.pass_limits",
            "Go past the limits each player has: robots, moving structures, large edits");
    public static final TextKey DIAGNOSTIC_WARNINGS_TEXT = TextKey.of("jscore.permission.diagnostic_warnings",
            "Be told in chat when a part of the series stalls or slows itself");
    public static final TextKey DEVELOPER_TOOLS_TEXT = TextKey.of("jscore.permission.developer_tools",
            "Use the developer's tools that read or change the world, while the server's developer mode is on");
    /* The command's name. */
    public static final TextKey COMMAND_TEXT = TextKey.of("jscore.permission.command", "Run /jstech %s");

    /** The operators who may change the game: the level of {@code /give}. */
    public static final int OPERATORS = 2;
    /**
     * The dimension a permission is asked in, by its id, as a permission mod's context: a node granted in one
     * dimension and not another.
     */
    public static final PermissionDynamicContextKey<ResourceLocation> DIMENSION = new PermissionDynamicContextKey<>(
            ResourceLocation.class, "jscore:dimension", ResourceLocation::toString);
    /**
     * The claim a permission is asked in, by its owner's id (a player or a team), or {@link #NO_CLAIM} outside every
     * claim: a node granted in one claim and not elsewhere.
     */
    public static final PermissionDynamicContextKey<String> CLAIM = new PermissionDynamicContextKey<>(String.class,
            "jscore:claim", owner -> owner);
    /** The claim context's value outside every claim. */
    public static final String NO_CLAIM = "none";

    /** Every node declared, in the order it was. */
    private static final List<Declared> DECLARED = new CopyOnWriteArrayList<>();
    private static final Map<String, Declared> BY_NAME = new ConcurrentHashMap<>();

    /** Using what another player owns, whatever its access says. */
    public static final PermissionNode<Boolean> USE_OTHERS = declare(JsCore.MODID, "owned.use_others", OPERATORS,
            USE_OTHERS_TEXT);
    /** Opening another player's machine. */
    public static final PermissionNode<Boolean> OPEN_OTHERS = declare(JsCore.MODID, "owned.open_others", OPERATORS,
            OPEN_OTHERS_TEXT);
    /** Changing how another player's machine is set up. */
    public static final PermissionNode<Boolean> SET_UP_OTHERS = declare(JsCore.MODID, "owned.set_up_others",
            OPERATORS, SET_UP_OTHERS_TEXT);
    /** Breaking what another player owns. */
    public static final PermissionNode<Boolean> BREAK_OTHERS = declare(JsCore.MODID, "owned.break_others",
            OPERATORS, BREAK_OTHERS_TEXT);
    /** Changing a server's settings from one's own game. */
    public static final PermissionNode<Boolean> EDIT_SETTINGS = declare(JsCore.MODID, "config.edit", OPERATORS,
            EDIT_SETTINGS_TEXT);
    /** Building multiblock structures; everyone may, unless the server says otherwise. */
    public static final PermissionNode<Boolean> BUILD_MULTIBLOCKS = declare(JsCore.MODID, "multiblock.build", 0,
            BUILD_MULTIBLOCKS_TEXT);
    /** Going past the limits each player has. */
    public static final PermissionNode<Boolean> PASS_LIMITS = declare(JsCore.MODID, "limits.bypass", OPERATORS,
            PASS_LIMITS_TEXT);
    /** Being told when a part of the series stalls or slows itself. */
    public static final PermissionNode<Boolean> DIAGNOSTIC_WARNINGS = declare(JsCore.MODID, "diagnostics.warnings",
            OPERATORS, DIAGNOSTIC_WARNINGS_TEXT);
    /** Using the developer's tools that read or change the world, while the server's developer mode is on. */
    public static final PermissionNode<Boolean> DEVELOPER_TOOLS = declare(JsCore.MODID, "developer.tools",
            OPERATORS, DEVELOPER_TOOLS_TEXT);

    private CorePermissions() {
    }

    /**
     * A node of the series: {@code <modid>.<name>}, held by default by the players of at least {@code level} (0, every
     * player), and said in the list of nodes with {@code description}. Declared from a mod's constructor, before the
     * game gathers the nodes.
     *
     * @throws DeclarationError when one of that name is declared already
     */
    public static PermissionNode<Boolean> declare(final String modid, final String name, final int level,
                                                  final TextKey description) {
        return declare(modid, name, level, description.text());
    }

    /** The same, with a description that says something of its own, such as the command's name. */
    public static PermissionNode<Boolean> declare(final String modid, final String name, final int level,
                                                  final Text description) {
        final PermissionNode<Boolean> node = new PermissionNode<>(modid, name, PermissionTypes.BOOLEAN,
                (player, id, context) -> level <= 0 || player != null && player.hasPermissions(level), DIMENSION,
                CLAIM);
        node.setInformation(GameText.component(Text.of(node.getNodeName())), GameText.component(description));
        final Declared declared = new Declared(node, level, description);
        if (BY_NAME.putIfAbsent(node.getNodeName(), declared) != null) {
            throw DeclarationError.of(modid, "the permission " + node.getNodeName(), "is declared twice",
                    "declare each permission once");
        }
        DECLARED.add(declared);
        return node;
    }

    /** Every node of the series, in the order they were declared. */
    public static List<Declared> declared() {
        return List.copyOf(DECLARED);
    }

    /** Whether {@code player} holds {@code node} where they stand: in their dimension and the claim they are in. */
    public static boolean has(final ServerPlayer player, final PermissionNode<Boolean> node) {
        return has(player, node, player.serverLevel(), player.blockPosition());
    }

    /**
     * Whether {@code player} holds {@code node} at {@code pos} of {@code level}: the permission mod is told the
     * dimension ({@link #DIMENSION}) and the claim ({@link #CLAIM}), so it may grant the node in one and not another.
     */
    public static boolean has(final ServerPlayer player, final PermissionNode<Boolean> node, final ServerLevel level,
                              final BlockPos pos) {
        return PermissionAPI.getPermission(player, node, contextsAt(level, pos));
    }

    /** Whether the player {@code player}, online or not, holds {@code node}: a machine acting for its owner asks so. */
    public static boolean has(final UUID player, final PermissionNode<Boolean> node) {
        return PermissionAPI.getOfflinePermission(player, node);
    }

    /** The same, at {@code pos} of {@code level}: a machine there acting for its owner. */
    public static boolean has(final UUID player, final PermissionNode<Boolean> node, final ServerLevel level,
                              final BlockPos pos) {
        return PermissionAPI.getOfflinePermission(player, node, contextsAt(level, pos));
    }

    /** What a permission mod is told of a place: its dimension and the claim it lies in. */
    public static PermissionDynamicContext<?>[] contextsAt(final ServerLevel level, final BlockPos pos) {
        return new PermissionDynamicContext<?>[] {DIMENSION.createContext(level.dimension().location()),
            CLAIM.createContext(Protection.claimOwner(level, pos).map(UUID::toString).orElse(NO_CLAIM))};
    }

    /** Whether {@code player} may use what others own. */
    public static boolean mayPassOwners(final ServerPlayer player) {
        return has(player, USE_OTHERS);
    }

    /** Hands the game the series' nodes as it gathers them, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PermissionGatherEvent.Nodes.class, event -> {
            for (final Declared declared : DECLARED) {
                event.addNodes(declared.node());
            }
        });
    }

    /**
     * A node declared.
     *
     * @param node        the node
     * @param level       the operator level that holds it by default; 0 for every player
     * @param description what it lets a player do
     */
    public record Declared(PermissionNode<Boolean> node, int level, Text description) {
    }
}
