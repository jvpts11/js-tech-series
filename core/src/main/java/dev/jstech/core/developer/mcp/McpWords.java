/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the MCP server says of itself and of each call: what became of a call, as its log shows it, and why a call or a
 * pairing was refused, which the player reads in their language and the agent in English.
 */
@TextHolder
public final class McpWords {

    /* What became of a call. */
    public static final TextKey ALLOWED = TextKey.of("jscore.mcp.answer.allowed", "allowed");
    public static final TextKey REFUSED = TextKey.of("jscore.mcp.answer.refused", "refused");
    public static final TextKey DONE = TextKey.of("jscore.mcp.answer.done", "done");
    public static final TextKey FAILED = TextKey.of("jscore.mcp.answer.failed", "failed");
    public static final TextKey ACTING = TextKey.of("jscore.mcp.answer.acting", "acting");
    public static final TextKey TAKEN_BACK = TextKey.of("jscore.mcp.answer.taken_back", "taken back");
    /* A pairing, in the log. */
    public static final TextKey PAIRED = TextKey.of("jscore.mcp.paired", "paired: %s");
    public static final TextKey READS = TextKey.of("jscore.mcp.reads", "reads");
    public static final TextKey READS_AND_ACTS = TextKey.of("jscore.mcp.reads_and_acts", "reads and acts");
    public static final TextKey KEY_TAKEN = TextKey.of("jscore.mcp.key_taken", "its key taken back");
    /* Why a pairing was refused. */
    public static final TextKey PAIR_REFUSED = TextKey.of("jscore.mcp.why.pair_refused",
            "The player refused to let this client in.");
    public static final TextKey PAIR_TIMED_OUT = TextKey.of("jscore.mcp.why.pair_timed_out",
            "Nobody answered in time, so the client was not let in; ask again while the player is at the game.");
    public static final TextKey PAIR_BUSY = TextKey.of("jscore.mcp.why.pair_busy",
            "Another client is being asked about; ask again in a moment.");
    public static final TextKey PAIR_RESTING = TextKey.of("jscore.mcp.why.pair_resting",
            "A client was just refused; ask again in %s seconds.");
    public static final TextKey NOBODY_TO_ASK = TextKey.of("jscore.mcp.why.nobody_to_ask",
            "No player who may let a client in is at the game.");
    /* Why a call was refused. */
    public static final TextKey NEEDS_ACT = TextKey.of("jscore.mcp.why.needs_act",
            "%s acts in the game, and this client was let in to read only.");
    public static final TextKey NO_TOOL = TextKey.of("jscore.mcp.why.no_tool", "There is no tool %s.");
    public static final TextKey MOD_OFF = TextKey.of("jscore.mcp.why.mod_off",
            "The tools of %s are off; the player turns them on in the studio.");
    public static final TextKey OWNER_AWAY = TextKey.of("jscore.mcp.why.owner_away",
            "The player who let this client in is not on the server.");
    public static final TextKey OFF = TextKey.of("jscore.mcp.why.off",
            "The developer mode of this game is off, so its tools are too.");
    public static final TextKey TOOK_TOO_LONG = TextKey.of("jscore.mcp.why.took_too_long",
            "The game did not answer within %s seconds.");
    public static final TextKey WENT_WRONG = TextKey.of("jscore.mcp.why.went_wrong", "It went wrong: %s");
    public static final TextKey TAKEN_BY_PLAYER = TextKey.of("jscore.mcp.why.taken_by_player",
            "The player pressed a key and took the controls back.");
    public static final TextKey BAD_ARGUMENT = TextKey.of("jscore.mcp.why.bad_argument", "%s should be %s");
    /* What an argument should be. */
    public static final TextKey A_NUMBER = TextKey.of("jscore.mcp.wants.number", "a whole number from %s to %s");
    public static final TextKey AN_ID = TextKey.of("jscore.mcp.wants.id", "an id, such as minecraft:stone");
    public static final TextKey A_PLACE = TextKey.of("jscore.mcp.wants.place",
            "a place {x, y, z}, or left out for the block looked at");
    public static final TextKey SOME_WORDS = TextKey.of("jscore.mcp.wants.words", "some words");
    public static final TextKey ONE_OF = TextKey.of("jscore.mcp.wants.one_of", "one of %s");
    /* Commands and files. */
    public static final TextKey COMMAND_NEVER = TextKey.of("jscore.mcp.why.command_never",
            "/%s is never among the allowed commands.");
    public static final TextKey COMMAND_NOT_LISTED = TextKey.of("jscore.mcp.why.command_not_listed",
            "/%s is not among the allowed commands.");
    public static final TextKey COMMAND_NOT_HERE = TextKey.of("jscore.mcp.why.command_not_here",
            "Commands run only in a test world, or on a server with its developer mode on, for an operator.");
    public static final TextKey COMMAND_EMPTY = TextKey.of("jscore.mcp.why.command_empty", "There is no command.");
    public static final TextKey FILE_OUTSIDE = TextKey.of("jscore.mcp.why.file_outside",
            "%s is outside the mods' and the pack's folders.");
    public static final TextKey FILE_KIND = TextKey.of("jscore.mcp.why.file_kind",
            "Files ending in %s are not read or written: only data, art and sound.");
    public static final TextKey FILE_TOO_BIG = TextKey.of("jscore.mcp.why.file_too_big",
            "%s is larger than %s bytes.");
    public static final TextKey FILE_MISSING = TextKey.of("jscore.mcp.why.file_missing", "There is no file %s.");
    public static final TextKey FOLDER_UNKNOWN = TextKey.of("jscore.mcp.why.folder_unknown",
            "There is no folder %s; the folders are %s.");
    /* The MCP server of a dedicated server, from its command. */
    public static final TextKey SERVER_ON = TextKey.of("commands.jscore.mcp.server_on",
            "The server's MCP server listens at http://127.0.0.1:%s/mcp, on the server's computer only.");
    public static final TextKey SERVER_OFF = TextKey.of("commands.jscore.mcp.server_off",
            "The server's MCP server stopped, and every key it gave is taken back.");
    public static final TextKey SERVER_IS_ON = TextKey.of("commands.jscore.mcp.server_is_on",
            "The server's MCP server is on at port %s, with %s clients let in.");
    public static final TextKey SERVER_IS_OFF = TextKey.of("commands.jscore.mcp.server_is_off",
            "The server's MCP server is off.");
    public static final TextKey SERVER_CLIENT = TextKey.of("commands.jscore.mcp.server_client",
            "%s %s, which %s, %s calls");
    public static final TextKey SERVER_CUT = TextKey.of("commands.jscore.mcp.server_cut", "%s clients cut off.");
    public static final TextKey SERVER_FAILED = TextKey.of("commands.jscore.mcp.server_failed",
            "The MCP server could not listen at port %s: %s");
    public static final TextKey FROM_THE_GAME = TextKey.of("commands.jscore.mcp.from_the_game",
            "Only a player in the game turns an MCP server on.");

    private McpWords() {
    }
}
