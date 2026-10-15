/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.jstech.core.command.SeriesCommands;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The protection's commands, for the server's administrators: {@code /jstech dump permissions}, writing every
 * permission of the series out, with the groups ready to paste into LuckPerms; and {@code /jstech audit}, reading the
 * audit log, newest first, a page at a time, of everything, of one player, or of one place.
 */
@TextHolder
public final class ProtectionCommands {

    /* The folder the files went into. */
    public static final TextKey DUMPED = TextKey.of("jscore.command.dump.permissions",
            "Wrote the series' %s permissions, and the LuckPerms groups to paste, into %s");
    public static final TextKey NOT_DUMPED = TextKey.of("jscore.command.dump.failed",
            "The permissions could not be written: %s");
    public static final TextKey AUDIT_OFF = TextKey.of("jscore.command.audit.off",
            "The audit log is off; turn on \"Keep the audit log\" in the world's settings");
    public static final TextKey AUDIT_EMPTY = TextKey.of("jscore.command.audit.empty", "Nothing is written down there");
    /* The page, and how many entries the log holds. */
    public static final TextKey AUDIT_HEADER = TextKey.of("jscore.command.audit.header",
            "Page %s of what is written down, newest first; %s entries in all:");
    /* When, who, what they did, what to, where, in which dimension, and whose it is. */
    public static final TextKey AUDIT_LINE = TextKey.of("jscore.command.audit.line", "%s: %s %s %s at %s in %s (%s's)");
    public static final TextKey USED = TextKey.of("jscore.command.audit.used", "used");
    public static final TextKey CHANGED = TextKey.of("jscore.command.audit.changed", "changed");
    public static final TextKey BROKE = TextKey.of("jscore.command.audit.broke", "broke");

    /** The folder in the game's own the permissions are written into. */
    public static final String FOLDER = "jstech-permissions";
    private static final String NODES_FILE = "nodes.md";
    private static final String LUCKPERMS_FILE = "luckperms.txt";
    private static final String PLAYERS_GROUP = "jstech-players";
    private static final String ADMINS_GROUP = "jstech-admins";
    private static final int PAGE = 10;
    private static final String PAGE_ARGUMENT = "page";
    private static final String PLAYER_ARGUMENT = "player";
    private static final String POS_ARGUMENT = "pos";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private static final DynamicCommandExceptionType FAILED =
            new DynamicCommandExceptionType(why -> GameText.component(NOT_DUMPED.with(String.valueOf(why))));

    private ProtectionCommands() {
    }

    /** Declares the commands, from the Core's constructor. */
    public static void declare() {
        SeriesCommands.declare("dump", SeriesCommands.ADMINS, (node, context) -> node
                .then(Commands.literal("permissions").executes(ProtectionCommands::dumpPermissions)));
        SeriesCommands.declare("audit", SeriesCommands.ADMINS, (node, context) -> node
                .executes(source -> audit(source, entry -> true, 1))
                .then(Commands.argument(PAGE_ARGUMENT, IntegerArgumentType.integer(1))
                        .executes(source -> audit(source, entry -> true, page(source))))
                .then(Commands.literal(PLAYER_ARGUMENT).then(Commands.argument(PLAYER_ARGUMENT,
                                StringArgumentType.word())
                        .executes(source -> audit(source, byPlayer(source), 1))
                        .then(Commands.argument(PAGE_ARGUMENT, IntegerArgumentType.integer(1))
                                .executes(source -> audit(source, byPlayer(source), page(source))))))
                .then(Commands.literal("at").then(Commands.argument(POS_ARGUMENT, BlockPosArgument.blockPos())
                        .executes(source -> audit(source, atPlace(source), 1))
                        .then(Commands.argument(PAGE_ARGUMENT, IntegerArgumentType.integer(1))
                                .executes(source -> audit(source, atPlace(source), page(source)))))));
    }

    /** The series' permissions as a table: each node, who holds it by default, and what it lets a player do. */
    public static String nodesTable() {
        final StringBuilder out = new StringBuilder("# Permissions of the J's Tech Series\n\n")
                .append("| Node | Held by default | What it lets a player do |\n| --- | --- | --- |\n");
        for (final CorePermissions.Declared declared : CorePermissions.declared()) {
            out.append("| ").append(declared.node().getNodeName()).append(" | ")
                    .append(declared.level() <= 0 ? "every player" : "operators of level " + declared.level())
                    .append(" | ").append(declared.description().english()).append(" |\n");
        }
        return out.toString();
    }

    /**
     * The LuckPerms console commands that make two groups: one with what every player holds by default, one, built on
     * it, with what the operators hold.
     */
    public static String luckPermsGroups() {
        final StringBuilder out = new StringBuilder("# Paste into the server's console with LuckPerms installed.\n")
                .append("lp creategroup ").append(PLAYERS_GROUP).append('\n')
                .append("lp creategroup ").append(ADMINS_GROUP).append('\n')
                .append("lp group ").append(ADMINS_GROUP).append(" parent add ").append(PLAYERS_GROUP).append('\n');
        for (final CorePermissions.Declared declared : CorePermissions.declared()) {
            out.append("lp group ").append(declared.level() <= 0 ? PLAYERS_GROUP : ADMINS_GROUP)
                    .append(" permission set ").append(declared.node().getNodeName()).append(" true\n");
        }
        return out.toString();
    }

    private static int dumpPermissions(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final Path folder = FMLPaths.GAMEDIR.get().resolve(FOLDER);
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(NODES_FILE), nodesTable(), StandardCharsets.UTF_8);
            Files.writeString(folder.resolve(LUCKPERMS_FILE), luckPermsGroups(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw FAILED.create(e.getMessage());
        }
        final int count = CorePermissions.declared().size();
        context.getSource().sendSuccess(() -> GameText.component(DUMPED.with(count, folder.toString())), false);
        return count;
    }

    private static int audit(final CommandContext<CommandSourceStack> context, final Predicate<AuditLog.Entry> keep,
                             final int page) {
        final MinecraftServer server = context.getSource().getServer();
        if (!ProtectionBalance.audit()) {
            context.getSource().sendFailure(GameText.component(AUDIT_OFF));
            return 0;
        }
        final AuditLog log = AuditLog.of(server);
        final List<AuditLog.Entry> entries = log.newest(keep, (page - 1) * PAGE, PAGE);
        if (entries.isEmpty()) {
            context.getSource().sendSuccess(() -> GameText.component(AUDIT_EMPTY), false);
            return 0;
        }
        context.getSource().sendSuccess(() -> GameText.component(AUDIT_HEADER.with(page, log.size())), false);
        for (final AuditLog.Entry entry : entries) {
            final String when = WHEN.format(Instant.ofEpochMilli(entry.time()).atZone(ZoneId.systemDefault()));
            final String owner = CoreTeams.nameOf(server, entry.owner()).orElse(entry.owner().toString());
            final TextKey did = switch (entry.kind()) {
                case USE -> USED;
                case CHANGE -> CHANGED;
                case BREAK -> BROKE;
            };
            context.getSource().sendSuccess(() -> GameText.component(AUDIT_LINE.with(when, entry.actorName(), did,
                    entry.thing().toString(), entry.pos().toShortString(), entry.dimension().toString(), owner)),
                    false);
        }
        return entries.size();
    }

    private static int page(final CommandContext<CommandSourceStack> context) {
        return IntegerArgumentType.getInteger(context, PAGE_ARGUMENT);
    }

    /* The entries of one player, by the name the log wrote down or by id. */
    private static Predicate<AuditLog.Entry> byPlayer(final CommandContext<CommandSourceStack> context) {
        final String who = StringArgumentType.getString(context, PLAYER_ARGUMENT);
        return entry -> entry.actorName().equalsIgnoreCase(who) || entry.actor().toString().equalsIgnoreCase(who);
    }

    /* The entries of one place, in the dimension the command is run in. */
    private static Predicate<AuditLog.Entry> atPlace(final CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        final BlockPos pos = BlockPosArgument.getBlockPos(context, POS_ARGUMENT);
        final String dimension = context.getSource().getLevel().dimension().location().toString();
        return entry -> entry.pos().equals(pos) && entry.dimension().toString().equals(dimension);
    }
}
