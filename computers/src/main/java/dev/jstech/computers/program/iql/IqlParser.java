/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.program.iql.IqlLexer.Token;
import dev.jstech.computers.program.iql.IqlLexer.Type;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;

import java.util.List;
import java.util.Locale;

/**
 * Parses IQL text into an {@link IqlOperation} intent. Pure logic (no Minecraft), so it is unit-tested
 * directly.
 *
 * <p>Three statement shapes, dispatched on the verb:
 * <ul>
 *   <li><b>action</b>: {@code VERB [qty] item [FROM loc] [TO loc] [WHERE cond] [IF cond]
 *       [ORDER BY field [ASC|DESC]] [LIMIT n] [PRIORITY level]}
 *       (SELECT/INSERT/DELETE/MOVE/DROP/CRAFT/COUNT/LOCK/UNLOCK), with the five-flow validation (see
 *       {@link #validateFlow}). {@code qty} is optional; when omitted it is {@link IqlOperation#NONE}.
 *       {@code level} is one of LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH (or NORMAL for MEDIUM).</li>
 *   <li><b>query</b>: {@code (QUERY|SHOW) object [WHERE cond] [ORDER BY ...] [LIMIT n]}: a read that
 *       names a schema object instead of an item, and has no FROM/TO/IF.</li>
 *   <li><b>maintenance</b>: {@code (ANALYZE|VACUUM|REINDEX) [object]}.</li>
 * </ul>
 *
 * <p>A parse error surfaces as an {@link IqlError}, whose reason is read in the player's language.
 * Percentage quantities ({@code 50%}) are not modelled yet and are rejected with a clear message.
 */
public final class IqlParser {

    private final List<Token> tokens;
    private int pos;

    private IqlParser(final List<Token> tokens) {
        this.tokens = tokens;
    }

    public static IqlOperation parse(final String input) {
        if (input == null || input.isBlank()) {
            throw IqlError.of(IqlError.EMPTY_STATEMENT);
        }
        return new IqlParser(IqlLexer.lex(input)).parseStatement();
    }

    /**
     * Parses without throwing: a clean {@link IqlParseResult} the surfaces render as a diagnostic. The
     * result's position is the token the cursor reached when it failed, so a GUI can point at it.
     */
    public static IqlParseResult tryParse(final String input) {
        if (input == null || input.isBlank()) {
            return IqlParseResult.error(IqlError.EMPTY_STATEMENT.text(), IqlParseResult.NO_POSITION);
        }
        /*
         * Layer 2 first: CREATE/DROP/EXEC of a saved object. tryParse returns null (and we fall through)
         * for an ordinary action; it throws only when the text *is* a malformed definition.
         */
        try {
            final IqlDefinition definition = IqlDefinitionParser.tryParse(input);
            if (definition != null) {
                return IqlParseResult.okDefinition(definition);
            }
        } catch (final IllegalArgumentException e) {
            return IqlParseResult.error(reason(e), IqlParseResult.NO_POSITION);
        }
        final IqlParser parser = new IqlParser(IqlLexer.lex(input));
        try {
            return IqlParseResult.ok(parser.parseStatement());
        } catch (final IllegalArgumentException e) {
            return IqlParseResult.error(reason(e), parser.errorPosition());
        }
    }

    /* Every refusal of the language's own is an IqlError; anything else that slipped through says what it said. */
    private static Text reason(final IllegalArgumentException refused) {
        return refused instanceof IqlError error ? error.text() : Text.literal(String.valueOf(refused.getMessage()));
    }

    /** The token the cursor reached, clamped to the last token, for error reporting. */
    private int errorPosition() {
        if (tokens.isEmpty()) {
            return IqlParseResult.NO_POSITION;
        }
        return Math.min(pos, tokens.size() - 1);
    }

    private IqlOperation parseStatement() {
        final Token verbToken = expect(Type.WORD, IqlError.A_VERB);
        final IqlVerb verb = IqlVerb.fromKeyword(verbToken.text())
                .orElseThrow(() -> IqlError.of(IqlError.UNKNOWN_VERB, verbToken.text()));
        return switch (verb) {
            case QUERY -> parseQuery(verb);
            case ANALYZE, VACUUM, REINDEX -> parseMaintenance(verb);
            default -> parseAction(verb);
        };
    }

    private IqlOperation parseAction(final IqlVerb verb) {
        final long quantity = parseOptionalQuantity();
        final String item = expect(Type.WORD, IqlError.AN_ITEM).text();
        final Clauses clauses = new Clauses();
        parseClauses(clauses, true);
        validateFlow(verb, clauses.from, clauses.to);
        return new IqlOperation(verb, quantity, item, clauses.from, clauses.to, clauses.where,
                clauses.guard, clauses.orderBy, clauses.descending, clauses.limit, clauses.priority);
    }

    private IqlOperation parseQuery(final IqlVerb verb) {
        final String object = expect(Type.WORD, IqlError.AN_OBJECT).text();
        final Clauses clauses = new Clauses();
        parseClauses(clauses, false);
        return new IqlOperation(verb, IqlOperation.NONE, object, "", "", clauses.where, null,
                clauses.orderBy, clauses.descending, clauses.limit, OperationPriority.DEFAULT);
    }

    private IqlOperation parseMaintenance(final IqlVerb verb) {
        String object = "";
        if (peekType(Type.WORD)) {
            object = tokens.get(pos++).text();
        }
        if (pos < tokens.size()) {
            throw unexpected(tokens.get(pos));
        }
        return new IqlOperation(verb, IqlOperation.NONE, object, "", "", null, null, "", false,
                IqlOperation.NO_LIMIT, OperationPriority.DEFAULT);
    }

    /** Reads {@code qty} when the next token is a number or {@code ALL}; otherwise leaves it unset. */
    private long parseOptionalQuantity() {
        if (peekType(Type.NUMBER)) {
            return parseQuantity(tokens.get(pos++).text());
        }
        if (peekKeyword("ALL")) {
            pos++;
            return IqlOperation.ALL;
        }
        return IqlOperation.NONE;
    }

    private static long parseQuantity(final String token) {
        if (token.endsWith("%")) {
            throw IqlError.of(IqlError.PERCENTAGE, token);
        }
        try {
            return Long.parseLong(token);
        } catch (final NumberFormatException e) {
            throw IqlError.of(IqlError.EXPECTED_GOT, IqlError.A_QUANTITY, token);
        }
    }

    /** Mutable accumulator for the optional clauses that follow the item/object. */
    private static final class Clauses {
        private String from = "";
        private String to = "";
        private IIqlCondition where;
        private IIqlCondition guard;
        private String orderBy = "";
        private boolean descending;
        private int limit = IqlOperation.NO_LIMIT;
        private OperationPriority priority = OperationPriority.DEFAULT;
    }

    /** Reads {@code FROM/TO/WHERE/IF/ORDER BY/LIMIT/PRIORITY} in any order until the tokens run out. */
    private void parseClauses(final Clauses acc, final boolean allowFlowClauses) {
        while (pos < tokens.size()) {
            final Token token = tokens.get(pos);
            if (token.type() != Type.WORD) {
                throw unexpected(token);
            }
            switch (token.text().toUpperCase(Locale.ROOT)) {
                case "FROM" -> {
                    requireFlowClause(allowFlowClauses, token);
                    pos++;
                    acc.from = expect(Type.WORD, IqlError.A_SOURCE).text();
                }
                case "TO" -> {
                    requireFlowClause(allowFlowClauses, token);
                    pos++;
                    acc.to = expect(Type.WORD, IqlError.A_DESTINATION).text();
                }
                case "IF" -> {
                    requireFlowClause(allowFlowClauses, token);
                    pos++;
                    acc.guard = parseConditionAtCursor();
                }
                case "WHERE" -> {
                    pos++;
                    acc.where = parseConditionAtCursor();
                }
                case "ORDER" -> {
                    pos++;
                    expectKeyword("BY");
                    acc.orderBy = expect(Type.WORD, IqlError.A_SORT_FIELD).text();
                    acc.descending = parseSortDirection();
                }
                case "LIMIT" -> {
                    pos++;
                    acc.limit = parseLimit();
                }
                case "PRIORITY" -> {
                    // A read has nothing to schedule: the clause belongs to the actions that queue work.
                    requireFlowClause(allowFlowClauses, token);
                    pos++;
                    acc.priority = parsePriority();
                }
                default -> throw unexpected(token);
            }
        }
    }

    private OperationPriority parsePriority() {
        final Token token = expect(Type.WORD, IqlError.A_LEVEL);
        return OperationPriority.fromKeyword(token.text())
                .orElseThrow(() -> IqlError.of(IqlError.UNKNOWN_PRIORITY, token.text()));
    }

    private boolean parseSortDirection() {
        if (peekKeyword("DESC")) {
            pos++;
            return true;
        }
        if (peekKeyword("ASC")) {
            pos++;
        }
        return false;
    }

    private int parseLimit() {
        final Token token = expect(Type.NUMBER, IqlError.A_ROW_COUNT);
        if (token.text().endsWith("%")) {
            throw IqlError.of(IqlError.LIMIT_PERCENTAGE, token.text());
        }
        try {
            return Integer.parseInt(token.text());
        } catch (final NumberFormatException e) {
            throw IqlError.of(IqlError.EXPECTED_GOT, IqlError.A_ROW_COUNT, token.text());
        }
    }

    /** Parses a condition from the shared cursor and advances past it. */
    private IIqlCondition parseConditionAtCursor() {
        final IqlConditionParser conditionParser = new IqlConditionParser(tokens, pos);
        final IIqlCondition condition = conditionParser.parseCondition();
        pos = conditionParser.position();
        return condition;
    }

    /** Enforces the five item flows: who needs a destination and who forbids one. */
    private static void validateFlow(final IqlVerb verb, final String from, final String to) {
        final boolean hasFrom = !from.isEmpty();
        final boolean hasTo = !to.isEmpty();
        switch (verb) {
            case DELETE -> {
                if (!hasTo) {
                    throw IqlError.of(IqlError.DELETE_NEEDS_TO);
                }
            }
            case MOVE -> {
                if (!hasFrom || !hasTo) {
                    throw IqlError.of(IqlError.MOVE_NEEDS_BOTH);
                }
            }
            case SELECT, INSERT, DROP -> {
                if (hasTo) {
                    throw IqlError.of(IqlError.NO_TO, verb.name());
                }
            }
            default -> {
                // QUERY/COUNT/CRAFT/LOCK/UNLOCK/maintenance carry no FROM/TO constraint at this layer.
            }
        }
    }

    private static void requireFlowClause(final boolean allowed, final Token token) {
        if (!allowed) {
            throw IqlError.of(IqlError.NOT_ON_READ, token.text());
        }
    }

    private Token expect(final Type type, final TextKey what) {
        if (pos >= tokens.size()) {
            throw IqlError.of(IqlError.EXPECTED, what);
        }
        final Token token = tokens.get(pos);
        if (token.type() != type) {
            throw IqlError.of(IqlError.EXPECTED_GOT, what, token.text());
        }
        pos++;
        return token;
    }

    private void expectKeyword(final String keyword) {
        if (!peekKeyword(keyword)) {
            throw IqlError.of(IqlError.EXPECTED, keyword);
        }
        pos++;
    }

    private boolean peekKeyword(final String keyword) {
        return peekType(Type.WORD) && tokens.get(pos).text().equalsIgnoreCase(keyword);
    }

    private boolean peekType(final Type type) {
        return pos < tokens.size() && tokens.get(pos).type() == type;
    }

    private static IqlError unexpected(final Token token) {
        return IqlError.of(IqlError.UNEXPECTED_TOKEN, token.text());
    }
}
