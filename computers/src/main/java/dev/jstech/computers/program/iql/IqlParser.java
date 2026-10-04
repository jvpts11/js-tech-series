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
import dev.jstech.computers.workshop.UpdateAction;
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
 *   <li><b>update</b>: {@code UPDATE [qty] item [FROM server] SET action [args] [WITH item] [WHERE cond]
 *       [ORDER BY ...] [LIMIT n] [PRIORITY level]}, where the action is SMELT, ENCHANT [OFFER n], REPAIR,
 *       COMBINE or NAME 'text', and only a REPAIR or a COMBINE takes WITH (a COMBINE must).</li>
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
        // A bus's settings are their own statement: SET BUS 'name' setting.
        if (IqlBusStatement.isSetBus(input)) {
            try {
                return IqlParseResult.okBus(IqlBusStatement.parse(input));
            } catch (final IllegalArgumentException e) {
                return IqlParseResult.error(reason(e), IqlParseResult.NO_POSITION);
            }
        }
        // And a Crafting Interface's or a Crafting Input Router's: SET INTERFACE 'name' setting, and the like.
        if (IqlCraftingStatement.isCrafting(input)) {
            try {
                return IqlParseResult.okCrafting(IqlCraftingStatement.parse(input));
            } catch (final IllegalArgumentException e) {
                return IqlParseResult.error(reason(e), IqlParseResult.NO_POSITION);
            }
        }
        // So is a Redstone Interface's mode: SET REDSTONE 'name' IN, or OUT and a strength.
        if (IqlRedstoneStatement.isSetRedstone(input)) {
            try {
                return IqlParseResult.okRedstone(IqlRedstoneStatement.parse(input));
            } catch (final IllegalArgumentException e) {
                return IqlParseResult.error(reason(e), IqlParseResult.NO_POSITION);
            }
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
            case UPDATE -> parseUpdate(verb);
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
                clauses.guard, clauses.orderBy, clauses.descending, clauses.limit, clauses.priority, null);
    }

    /*
     * The SET comes straight after the item and its FROM, so what follows it reads as the action's own words; the
     * clauses every action takes come after those.
     */
    private IqlOperation parseUpdate(final IqlVerb verb) {
        final long quantity = parseOptionalQuantity();
        final String item = expect(Type.WORD, IqlError.AN_ITEM).text();
        String from = "";
        if (peekKeyword("FROM")) {
            pos++;
            from = expect(Type.WORD, IqlError.A_SOURCE).text();
        }
        if (!peekKeyword("SET")) {
            throw IqlError.of(IqlError.UPDATE_NEEDS_SET);
        }
        pos++;
        final IqlUpdate update = parseSet();
        final Clauses clauses = new Clauses();
        parseClauses(clauses, true);
        if (!clauses.to.isEmpty()) {
            throw IqlError.of(IqlError.NO_TO, verb.name());
        }
        if (!clauses.from.isEmpty()) {
            if (!from.isEmpty()) {
                throw IqlError.of(IqlError.UNEXPECTED_TOKEN, "FROM");
            }
            from = clauses.from;
        }
        return new IqlOperation(verb, quantity, item, from, "", clauses.where, clauses.guard, clauses.orderBy,
                clauses.descending, clauses.limit, clauses.priority, update);
    }

    /** What the card is to do: the action, what it needs said, and the second item when it takes one. */
    private IqlUpdate parseSet() {
        final Token word = expect(Type.WORD, IqlError.AN_ACTION);
        final UpdateAction action = UpdateAction.fromKeyword(word.text())
                .orElseThrow(() -> IqlError.of(IqlError.UNKNOWN_ACTION, word.text()));
        int offer = IqlUpdate.NO_OFFER;
        if (action == UpdateAction.ENCHANT && peekKeyword("OFFER")) {
            pos++;
            offer = parseOffer(expect(Type.NUMBER, IqlError.AN_OFFER).text());
        }
        String name = "";
        if (action == UpdateAction.NAME) {
            name = expect(Type.STRING, IqlError.A_NAME).text();
            if (name.isBlank()) {
                throw IqlError.of(IqlError.EXPECTED, IqlError.A_NAME);
            }
            if (name.length() > IqlUpdate.MAX_NAME) {
                throw IqlError.of(IqlError.NAME_TOO_LONG, IqlUpdate.MAX_NAME);
            }
        }
        String with = "";
        if (peekKeyword("WITH")) {
            if (!action.takesSecond()) {
                throw IqlError.of(IqlError.NO_WITH, action.name());
            }
            pos++;
            with = expect(Type.WORD, IqlError.AN_ITEM).text();
        } else if (action == UpdateAction.COMBINE) {
            throw IqlError.of(IqlError.COMBINE_NEEDS_WITH);
        }
        return new IqlUpdate(action, offer, name, with);
    }

    private static int parseOffer(final String token) {
        try {
            final int offer = Integer.parseInt(token);
            if (offer >= 1 && offer <= IqlUpdate.OFFERS) {
                return offer;
            }
        } catch (final NumberFormatException e) {
            // Falls through to the refusal below, which says what an offer is.
        }
        throw IqlError.of(IqlError.OFFER_RANGE, token);
    }

    private IqlOperation parseQuery(final IqlVerb verb) {
        final String object = expect(Type.WORD, IqlError.AN_OBJECT).text();
        final Clauses clauses = new Clauses();
        parseClauses(clauses, false);
        return new IqlOperation(verb, IqlOperation.NONE, object, "", "", clauses.where, null,
                clauses.orderBy, clauses.descending, clauses.limit, OperationPriority.DEFAULT, null);
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
                IqlOperation.NO_LIMIT, OperationPriority.DEFAULT, null);
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
