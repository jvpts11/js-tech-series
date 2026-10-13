/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the IQL Layer-2 definitions: {@code CREATE}/{@code DROP} of saved objects (views, procedures,
 * jobs) and {@code EXEC} of a procedure. Pure logic, no Minecraft. Works on the raw string rather than the
 * token stream so a definition's body (a query, a {@code { ... }} block) is captured verbatim and re-parsed
 * when the object runs.
 *
 * <p>{@link #tryParse} returns {@code null} when the text is not a definition (it is an ordinary
 * action/query), so the main parser tries a definition first and falls back. {@code DROP} is the one
 * overlap: {@code DROP VIEW x} drops a definition, while {@code DROP 64 dirt} is the item-trashing action,
 * so a {@code DROP} not followed by VIEW/PROCEDURE/JOB returns {@code null} to fall through.
 */
public final class IqlDefinitionParser {

    private IqlDefinitionParser() {
    }

    /**
     * Splits a procedure body into its statements: {@code "{ a; b }"} becomes {@code ["a", "b"]}, and a
     * bare statement (no braces) becomes a single-element list. Empty statements are dropped.
     */
    public static List<String> splitBody(final String body) {
        String inner = body.strip();
        if (inner.startsWith("{") && inner.endsWith("}")) {
            inner = inner.substring(1, inner.length() - 1);
        }
        final List<String> statements = new ArrayList<>();
        /* The same quote- and brace-aware split as a script, so a semicolon inside a name ends nothing. */
        for (final IqlScript.Statement statement : IqlScript.split(inner)) {
            statements.add(statement.text());
        }
        return statements;
    }

    /** A definition, or {@code null} when {@code input} is an ordinary action/query for the main parser. */
    public static IqlDefinition tryParse(final String input) {
        final String trimmed = input.strip();
        final String[] head = trimmed.split("\\s+", 2);
        final String rest = head.length > 1 ? head[1] : "";
        return switch (head[0].toUpperCase(Locale.ROOT)) {
            case "CREATE" -> parseCreate(rest);
            case "EXEC", "CALL" -> parseExec(rest);
            case "DROP" -> parseDrop(rest);
            default -> null;
        };
    }

    private static IqlDefinition parseCreate(final String rest) {
        final String[] typeAndRest = rest.split("\\s+", 2);
        final IqlDefinition.ObjectType type = objectType(typeAndRest[0]);
        if (type == null) {
            throw IqlError.of(IqlError.CREATE_WHAT);
        }
        if (typeAndRest.length < 2) {
            throw IqlError.of(IqlError.NEEDS_NAME_AND_BODY, name(type));
        }
        final String[] nameAndRest = typeAndRest[1].strip().split("\\s+", 2);
        final String objectName = nameAndRest[0];
        if (objectName.isEmpty() || nameAndRest.length < 2) {
            throw IqlError.of(IqlError.NEEDS_NAME_AND_BODY, name(type));
        }
        if (objectName.length() > IqlDefinition.MAX_NAME) {
            throw IqlError.of(IqlError.NAME_TOO_LONG, IqlDefinition.MAX_NAME);
        }
        final String afterName = nameAndRest[1].strip();
        if (!startsWithKeyword(afterName, "AS")) {
            throw IqlError.of(IqlError.EXPECTED_AS, name(type));
        }
        final String body = afterName.substring(2).strip();
        if (type == IqlDefinition.ObjectType.JOB) {
            return parseJob(objectName, body);
        }
        if (body.isEmpty()) {
            throw IqlError.of(IqlError.NEEDS_BODY, name(type));
        }
        return IqlDefinition.create(type, objectName, body, IqlDefinition.TriggerKind.NONE, "");
    }

    private static IqlDefinition parseJob(final String name, final String afterAs) {
        final int every = keywordIndex(afterAs, "EVERY");
        final int when = keywordIndex(afterAs, "WHEN");
        if (every < 0 && when < 0) {
            throw IqlError.of(IqlError.JOB_NEEDS_TRIGGER);
        }
        final boolean useEvery = every > when;
        final int at = useEvery ? every : when;
        final int keywordLength = useEvery ? "EVERY".length() : "WHEN".length();
        final String body = afterAs.substring(0, at).strip();
        final String spec = afterAs.substring(at + keywordLength).strip();
        if (body.isEmpty()) {
            throw IqlError.of(IqlError.JOB_NEEDS_BODY);
        }
        if (spec.isEmpty()) {
            throw IqlError.of(IqlError.TRIGGER_NEEDS_VALUE);
        }
        return IqlDefinition.create(IqlDefinition.ObjectType.JOB, name, body,
                useEvery ? IqlDefinition.TriggerKind.EVERY : IqlDefinition.TriggerKind.WHEN, spec);
    }

    private static IqlDefinition parseDrop(final String rest) {
        final String[] parts = rest.strip().split("\\s+", 2);
        final IqlDefinition.ObjectType type = objectType(parts[0]);
        if (type == null) {
            return null; // DROP <item> is the action, not a definition drop, so fall through to the action parser
        }
        if (parts.length < 2 || parts[1].isBlank()) {
            throw IqlError.of(IqlError.DROP_NEEDS_NAME, name(type));
        }
        return IqlDefinition.drop(type, parts[1].strip());
    }

    private static IqlDefinition parseExec(final String rest) {
        final String procedure = rest.strip();
        if (procedure.isEmpty()) {
            throw IqlError.of(IqlError.EXEC_NEEDS_NAME);
        }
        if (procedure.split("\\s+").length > 1) {
            throw IqlError.of(IqlError.EXEC_ONE_NAME);
        }
        return IqlDefinition.exec(procedure);
    }

    private static IqlDefinition.ObjectType objectType(final String word) {
        return switch (word.toUpperCase(Locale.ROOT)) {
            case "VIEW" -> IqlDefinition.ObjectType.VIEW;
            case "PROCEDURE", "PROC" -> IqlDefinition.ObjectType.PROCEDURE;
            case "JOB" -> IqlDefinition.ObjectType.JOB;
            default -> null;
        };
    }

    private static String name(final IqlDefinition.ObjectType type) {
        return type.name();
    }

    private static boolean startsWithKeyword(final String text, final String keyword) {
        return text.length() >= keyword.length()
                && text.substring(0, keyword.length()).equalsIgnoreCase(keyword)
                && (text.length() == keyword.length() || Character.isWhitespace(text.charAt(keyword.length())));
    }

    /**
     * Index of the last {@code keyword} standing alone in {@code text} (case-insensitive) outside any quoted
     * name and any braces, or -1. The last one is the job's trigger: a bus or item name that happens to be
     * the word is quoted, and a bus setting may itself say WHEN earlier in the body.
     */
    private static int keywordIndex(final String text, final String keyword) {
        final Matcher matcher = Pattern.compile("(?i)\\b" + keyword + "\\b").matcher(text);
        int found = -1;
        while (matcher.find()) {
            if (isTopLevel(text, matcher.start())) {
                found = matcher.start();
            }
        }
        return found;
    }

    /** Whether the position in {@code text} is outside every quoted run and every brace block. */
    private static boolean isTopLevel(final String text, final int position) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < position; i++) {
            final char c = text.charAt(i);
            if (quote != 0) {
                quote = c == quote ? 0 : quote;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth = Math.max(0, depth - 1);
            }
        }
        return quote == 0 && depth == 0;
    }
}
