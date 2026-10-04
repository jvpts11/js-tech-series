/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * A statement the query language cannot read, and why.
 *
 * <p>The reason is a sentence, read in the player's language wherever a statement is typed; as a Java exception it
 * reads in English, the language the machine keeps its logs in. The language's own words (a verb, a clause, a
 * token that was typed) are data inside it and stay as they are.
 */
@TextHolder
public final class IqlError extends IllegalArgumentException {

    private final transient Text text;

    private static final long serialVersionUID = 1L;

    // A statement.
    static final TextKey EMPTY_STATEMENT = TextKey.of("jsc.iql.error.empty_statement", "empty statement");
    static final TextKey UNKNOWN_VERB = TextKey.of("jsc.iql.error.unknown_verb", "unknown verb: %s");
    static final TextKey UNEXPECTED_TOKEN = TextKey.of("jsc.iql.error.unexpected_token", "unexpected token: %s");
    static final TextKey EXPECTED = TextKey.of("jsc.iql.error.expected", "expected %s");
    static final TextKey EXPECTED_GOT = TextKey.of("jsc.iql.error.expected_got", "expected %s, got: %s");
    static final TextKey PERCENTAGE = TextKey.of("jsc.iql.error.percentage",
            "percentage quantities are not supported yet: %s");
    static final TextKey UNKNOWN_PRIORITY = TextKey.of("jsc.iql.error.unknown_priority",
            "unknown priority level: %s (expected LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH or HIGH)");
    static final TextKey LIMIT_PERCENTAGE = TextKey.of("jsc.iql.error.limit_percentage",
            "LIMIT takes a row count, not a percentage: %s");
    static final TextKey DELETE_NEEDS_TO = TextKey.of("jsc.iql.error.delete_needs_to",
            "DELETE exports out of the network and needs a TO destination");
    static final TextKey MOVE_NEEDS_BOTH = TextKey.of("jsc.iql.error.move_needs_both",
            "MOVE is internal and needs both FROM and TO");
    static final TextKey NO_TO = TextKey.of("jsc.iql.error.no_to", "%s has no TO destination");
    static final TextKey NOT_ON_READ = TextKey.of("jsc.iql.error.not_on_read", "%s is not valid on a read");

    // What was expected where a statement stopped making sense.
    static final TextKey A_VERB = TextKey.of("jsc.iql.expected.verb", "a verb");
    static final TextKey AN_ITEM = TextKey.of("jsc.iql.expected.item", "an item");
    static final TextKey AN_OBJECT = TextKey.of("jsc.iql.expected.object", "an object to query");
    static final TextKey A_QUANTITY = TextKey.of("jsc.iql.expected.quantity", "a quantity");
    static final TextKey A_SOURCE = TextKey.of("jsc.iql.expected.source", "a location after FROM");
    static final TextKey A_DESTINATION = TextKey.of("jsc.iql.expected.destination", "a location after TO");
    static final TextKey A_SORT_FIELD = TextKey.of("jsc.iql.expected.sort_field", "a sort field after ORDER BY");
    static final TextKey A_LEVEL = TextKey.of("jsc.iql.expected.level",
            "a level after PRIORITY (LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH)");
    static final TextKey A_ROW_COUNT = TextKey.of("jsc.iql.expected.row_count", "a row count after LIMIT");
    static final TextKey A_FIELD = TextKey.of("jsc.iql.expected.field", "a field name");
    static final TextKey AN_OPERATOR = TextKey.of("jsc.iql.expected.operator", "a comparison operator");
    static final TextKey A_VALUE = TextKey.of("jsc.iql.expected.value", "a value");

    // A condition.
    static final TextKey UNKNOWN_OPERATOR = TextKey.of("jsc.iql.error.unknown_operator", "unknown operator: %s");

    // A saved object.
    static final TextKey CREATE_WHAT =
            TextKey.of("jsc.iql.error.create_what", "CREATE expects VIEW, PROCEDURE, or JOB");
    static final TextKey NEEDS_NAME_AND_BODY = TextKey.of("jsc.iql.error.needs_name_and_body",
            "CREATE %s needs a name and an AS body");
    static final TextKey EXPECTED_AS = TextKey.of("jsc.iql.error.expected_as", "expected AS after the %s name");
    static final TextKey NEEDS_BODY = TextKey.of("jsc.iql.error.needs_body", "CREATE %s needs a body after AS");
    static final TextKey JOB_NEEDS_TRIGGER = TextKey.of("jsc.iql.error.job_needs_trigger",
            "CREATE JOB needs a trigger: EVERY <duration> or WHEN <condition>");
    static final TextKey JOB_NEEDS_BODY = TextKey.of("jsc.iql.error.job_needs_body",
            "CREATE JOB needs a body before its trigger");
    static final TextKey TRIGGER_NEEDS_VALUE = TextKey.of("jsc.iql.error.trigger_needs_value",
            "CREATE JOB trigger needs a value");
    static final TextKey DROP_NEEDS_NAME = TextKey.of("jsc.iql.error.drop_needs_name", "DROP %s needs a name");
    static final TextKey EXEC_NEEDS_NAME = TextKey.of("jsc.iql.error.exec_needs_name", "EXEC needs a procedure name");
    static final TextKey EXEC_ONE_NAME = TextKey.of("jsc.iql.error.exec_one_name",
            "EXEC takes a single procedure name");

    // A bus's settings.
    static final TextKey BUS_NEEDS_NAME = TextKey.of("jsc.iql.error.bus_needs_name",
            "SET BUS needs the bus's name, in quotes when it has spaces");
    static final TextKey BUS_NEEDS_SETTING = TextKey.of("jsc.iql.error.bus_needs_setting",
            "SET BUS needs a setting: ON, OFF, MODE, FILTER, MATCH, KEEP, MAX, PRIORITY, WHEN, AFTER BUS or ACCESS");
    static final TextKey BUS_UNKNOWN_SETTING = TextKey.of("jsc.iql.error.bus_unknown_setting",
            "a bus has no setting %s");
    static final TextKey NOT_AN_HOUR = TextKey.of("jsc.iql.error.not_an_hour", "not an hour of the day: %s");

    // A Crafting Interface's settings, and a Crafting Input Router's.
    static final TextKey INTERFACE_NEEDS_NAME = TextKey.of("jsc.iql.error.interface_needs_name",
            "%s needs the interface's name, in quotes when it has spaces");
    static final TextKey INTERFACE_NEEDS_SETTING = TextKey.of("jsc.iql.error.interface_needs_setting",
            "SET INTERFACE needs a setting: EXCLUSIVE, MAX JOBS or ROUTE");
    static final TextKey INTERFACE_UNKNOWN_SETTING = TextKey.of("jsc.iql.error.interface_unknown_setting",
            "an interface has no setting %s");
    static final TextKey ROUTER_NEEDS_NAME = TextKey.of("jsc.iql.error.router_needs_name",
            "%s needs the router's name, in quotes when it has spaces");
    static final TextKey RENAME_NEEDS_TO = TextKey.of("jsc.iql.error.rename_needs_to",
            "RENAME needs TO and the new name");
    static final TextKey A_PATTERN = TextKey.of("jsc.iql.expected.pattern", "the pattern's name");
    static final TextKey AN_INPUT = TextKey.of("jsc.iql.expected.input", "one of the pattern's inputs");
    static final TextKey A_ROUTER = TextKey.of("jsc.iql.expected.router", "the router's name");

    // A Redstone Interface's mode.
    static final TextKey REDSTONE_NEEDS_NAME = TextKey.of("jsc.iql.error.redstone_needs_name",
            "SET REDSTONE needs the interface's name, in quotes when it has spaces");
    static final TextKey REDSTONE_NEEDS_MODE = TextKey.of("jsc.iql.error.redstone_needs_mode",
            "SET REDSTONE needs IN, or OUT and a strength from 0 to 15");
    static final TextKey NOT_A_STRENGTH = TextKey.of("jsc.iql.error.not_a_strength",
            "not a redstone strength, which runs from 0 to 15: %s");
    static final TextKey A_LIST = TextKey.of("jsc.iql.error.a_list", "a list of items");
    static final TextKey A_COUNT = TextKey.of("jsc.iql.error.a_count", "a count");

    // A duration.
    static final TextKey EMPTY_DURATION = TextKey.of("jsc.iql.error.empty_duration", "empty duration");
    static final TextKey NOT_A_DURATION = TextKey.of("jsc.iql.error.not_a_duration", "not a duration: %s");
    static final TextKey NEGATIVE_DURATION = TextKey.of("jsc.iql.error.negative_duration",
            "duration must be >= 0: %s");
    static final TextKey UNKNOWN_UNIT = TextKey.of("jsc.iql.error.unknown_unit", "unknown duration unit '%s' in: %s");

    public IqlError(final Text text) {
        super(text.english());
        this.text = text;
    }

    /** Why the statement could not be read, in the player's language. */
    public Text text() {
        return this.text;
    }

    /** Says why, with {@code args} as the key's data. */
    static IqlError of(final TextKey key, final Object... args) {
        return new IqlError(key.with(args));
    }
}
