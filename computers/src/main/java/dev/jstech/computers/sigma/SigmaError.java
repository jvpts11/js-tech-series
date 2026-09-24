/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;

/**
 * Every message the front end can produce, with the code a player quotes when asking for help.
 *
 * <p>The numbering is by stage, so a code says where the compiler gave up: S1xxx while reading the
 * characters, S2xxx while reading the grammar. Later stages take the ranges above them.
 *
 * <p>Each message is a sentence read in the player's language, as a compiler's are on a machine set to another one; the
 * code is the last word of its key, so it is written once and is the same in every language.
 */
@TextHolder
public enum SigmaError {

    UNTERMINATED_STRING(TextKey.of("jsc.sigma.error.s1001",
            "the string was never closed before the end of the line")),
    UNTERMINATED_COMMENT(TextKey.of("jsc.sigma.error.s1002",
            "the comment was never closed before the end of the file")),
    UNEXPECTED_CHARACTER(TextKey.of("jsc.sigma.error.s1003", "'%s' does not begin anything the language knows")),
    MALFORMED_NUMBER(TextKey.of("jsc.sigma.error.s1004", "'%s' is not a number the language can read")),
    INVALID_CHARACTER_LITERAL(TextKey.of("jsc.sigma.error.s1005",
            "a character literal holds exactly one character")),
    UNKNOWN_ESCAPE(TextKey.of("jsc.sigma.error.s1006", "'\\%s' is not an escape the language knows")),

    EXPECTED_TOKEN(TextKey.of("jsc.sigma.error.s2001", "expected %s but found %s")),
    EXPECTED_TYPE(TextKey.of("jsc.sigma.error.s2002", "expected a type but found %s")),
    EXPECTED_EXPRESSION(TextKey.of("jsc.sigma.error.s2003", "expected an expression but found %s")),
    EXPECTED_MEMBER(TextKey.of("jsc.sigma.error.s2004",
            "expected a field, a method, a property or an event but found %s")),
    EXPECTED_TYPE_DECLARATION(TextKey.of("jsc.sigma.error.s2005",
            "expected a class, a struct, a record, an interface, an enum or a delegate but found %s")),
    NOT_A_STATEMENT(TextKey.of("jsc.sigma.error.s2006",
            "only a call, an assignment, an increment, a decrement or a new object can be used as a statement")),
    DUPLICATE_MODIFIER(TextKey.of("jsc.sigma.error.s2007", "'%s' was given twice")),
    USING_TOO_LATE(TextKey.of("jsc.sigma.error.s2009", "a using has to come before the namespace and the types")),
    ONE_NAMESPACE(TextKey.of("jsc.sigma.error.s2010",
            "a file declares one namespace on its own line, before its types; put more of them in blocks")),
    INVALID_ASSIGNMENT_TARGET(TextKey.of("jsc.sigma.error.s2008",
            "the left side of an assignment must be a variable, a field, a property or an element")),
    NAMESPACE_REQUIRED(TextKey.of("jsc.sigma.error.s2011",
            "every type is in a namespace: put 'namespace Name;' at the top of the file")),
    USING_NEEDS_STAR(TextKey.of("jsc.sigma.error.s2012",
            "'%s' names a namespace; write 'using %s.*;' to bring in everything in it, or name one of its types")),
    NESTING_TOO_DEEP(TextKey.of("jsc.sigma.error.s2013",
            "this is nested more than %s levels deep, which is more than the compiler reads; the rest of the file"
                    + " was not read")),

    UNKNOWN_NAME(TextKey.of("jsc.sigma.error.s3001", "'%s' does not name anything here")),
    DUPLICATE_DECLARATION(TextKey.of("jsc.sigma.error.s3002", "'%s' is already declared here")),
    CANNOT_CONVERT(TextKey.of("jsc.sigma.error.s3003", "cannot convert '%s' to '%s'")),
    NO_SUCH_MEMBER(TextKey.of("jsc.sigma.error.s3004", "'%s' has no member called '%s'")),
    NO_MATCHING_OVERLOAD(TextKey.of("jsc.sigma.error.s3005", "no version of '%s' takes those arguments")),
    AMBIGUOUS_CALL(TextKey.of("jsc.sigma.error.s3006", "the call to '%s' fits more than one version of it")),
    OPERATOR_ON_TYPES(TextKey.of("jsc.sigma.error.s3007", "'%s' cannot be applied to '%s' and '%s'")),
    OPERATOR_ON_TYPE(TextKey.of("jsc.sigma.error.s3008", "'%s' cannot be applied to '%s'")),
    CONDITION_MUST_BE_BOOL(TextKey.of("jsc.sigma.error.s3009", "a condition is a bool, not '%s'")),
    BREAK_OUTSIDE_LOOP(TextKey.of("jsc.sigma.error.s3010", "'break' only means something inside a loop or a switch")),
    CONTINUE_OUTSIDE_LOOP(TextKey.of("jsc.sigma.error.s3011", "'continue' only means something inside a loop")),
    MISSING_RETURN_VALUE(TextKey.of("jsc.sigma.error.s3012", "a method that gives back '%s' must return a value")),
    UNEXPECTED_RETURN_VALUE(TextKey.of("jsc.sigma.error.s3013",
            "a method that gives back nothing cannot return a value")),
    THIS_IN_STATIC(TextKey.of("jsc.sigma.error.s3014", "'%s' is not available in a static member")),
    NO_BASE_CLASS(TextKey.of("jsc.sigma.error.s3015", "'%s' has no base class")),
    CANNOT_ASSIGN_READONLY(TextKey.of("jsc.sigma.error.s3016",
            "'%s' is readonly, so it can only be written where it is declared or in a constructor")),
    ENTRY_POINT(TextKey.of("jsc.sigma.error.s3017",
            "a program needs exactly one place to start, either a class that implements IScript or a class with a"
                    + " static Main, and this one has %s")),
    MISSING_INTERFACE_MEMBER(TextKey.of("jsc.sigma.error.s3018", "'%s' says it is a '%s' but does not have '%s'")),
    WRONG_TYPE_ARGUMENT_COUNT(TextKey.of("jsc.sigma.error.s3019", "'%s' takes %s type arguments")),
    STATIC_THROUGH_INSTANCE(TextKey.of("jsc.sigma.error.s3020",
            "'%s' belongs to the type, not to one of its objects")),
    INSTANCE_THROUGH_TYPE(TextKey.of("jsc.sigma.error.s3021", "'%s' belongs to an object, not to the type")),
    CANNOT_DISPOSE(TextKey.of("jsc.sigma.error.s3022", "only an object can be disposed, not '%s'")),
    NOT_A_COLLECTION(TextKey.of("jsc.sigma.error.s3023", "foreach walks an array or a list, not '%s'")),
    CANNOT_INDEX(TextKey.of("jsc.sigma.error.s3024", "'%s' cannot be indexed by '%s'")),
    CANNOT_CALL(TextKey.of("jsc.sigma.error.s3025", "'%s' is not something that can be called")),
    CANNOT_CREATE(TextKey.of("jsc.sigma.error.s3026", "'%s' cannot be made with new")),
    LAMBDA_SHAPE(TextKey.of("jsc.sigma.error.s3027", "this lambda does not have the shape '%s' takes")),
    NOT_A_TYPE(TextKey.of("jsc.sigma.error.s3028", "'%s' does not name a type")),
    DUPLICATE_SWITCH_LABEL(TextKey.of("jsc.sigma.error.s3029", "this switch already has that label")),
    EVENT_OUTSIDE_ITS_TYPE(TextKey.of("jsc.sigma.error.s3030",
            "an event can only be raised inside the type that declares it")),
    INVALID_BASE(TextKey.of("jsc.sigma.error.s3031",
            "only a class or an interface can stand behind the colon, and '%s' is neither")),
    EVENT_NEEDS_DELEGATE(TextKey.of("jsc.sigma.error.s3032",
            "an event's type must be a delegate, and '%s' is not one")),
    METHOD_AS_VALUE(TextKey.of("jsc.sigma.error.s3033",
            "'%s' is a method: call it, or hand it over where a delegate of its shape is wanted")),
    OUT_ARGUMENT_EXPECTED(TextKey.of("jsc.sigma.error.s3034",
            "'%s' is filled in by the method, so the argument is written with out")),
    OUT_ARGUMENT_UNEXPECTED(TextKey.of("jsc.sigma.error.s3035",
            "'%s' is read by the method, so the argument cannot be written with out")),
    OUT_TYPE_MUST_MATCH(TextKey.of("jsc.sigma.error.s3036", "an out argument is exactly '%s', with no conversion on the"
            + " way")),
    OUT_NOT_ASSIGNED(TextKey.of("jsc.sigma.error.s3037",
            "'%s' is filled in by this method, so it must be given a value before every way out")),
    OUT_NOT_A_PLACE(TextKey.of("jsc.sigma.error.s3038", "'%s' is not somewhere a method can write")),
    ENUM_VALUE_MUST_BE_WRITTEN(TextKey.of("jsc.sigma.error.s3039", "an enum's number has to be written as a number")),
    NEEDS_USING(TextKey.of("jsc.sigma.error.s3040",
            "'%s' is in %s; add 'using %s.*;' or 'using %s.%s;' at the top of the file")),
    STRUCT_NO_BASE(TextKey.of("jsc.sigma.error.s3041",
            "a struct can implement interfaces but stands on no class, and '%s' is a class")),
    CANNOT_LOCK(TextKey.of("jsc.sigma.error.s3042", "only an object can be locked, not '%s'")),
    NEEDS_OVERRIDE(TextKey.of("jsc.sigma.error.s3043", "'%s' is already on '%s', so write it with override to replace"
            + " it")),
    CANNOT_OVERRIDE(TextKey.of("jsc.sigma.error.s3044",
            "'%s' on '%s' is not virtual, so nothing can replace it; mark it virtual there")),
    OVERRIDE_WITHOUT_BASE(TextKey.of("jsc.sigma.error.s3045",
            "'%s' is written with override but nothing above '%s' has it")),
    ABSTRACT_NEEDS_ABSTRACT_CLASS(TextKey.of("jsc.sigma.error.s3046",
            "'%s' has no body, so '%s' has to be declared abstract")),
    ABSTRACT_WITH_BODY(TextKey.of("jsc.sigma.error.s3047",
            "'%s' is abstract, so it is written with a semicolon and no body")),
    MISSING_BODY(TextKey.of("jsc.sigma.error.s3048", "'%s' has no body, so it is written with abstract")),
    ABSTRACT_NOT_IMPLEMENTED(TextKey.of("jsc.sigma.error.s3049",
            "'%s' is not abstract, so it has to give '%s' a body")),
    CANNOT_CREATE_ABSTRACT(TextKey.of("jsc.sigma.error.s3050", "'%s' is abstract, so it cannot be made with new")),
    MODIFIER_NOT_ALLOWED(TextKey.of("jsc.sigma.error.s3051", "'%s' cannot be written with %s")),
    NOT_IN_THE_SUBSET(TextKey.of("jsc.sigma.error.s3052", "Sigma has no %s; %s")),
    PRINTF_FORMAT_NOT_WRITTEN_OUT(TextKey.of("jsc.sigma.error.s3053",
            "printf's format has to be written out where it is used, in quotes, so its holes can be read")),
    PRINTF_BAD_FORMAT(TextKey.of("jsc.sigma.error.s3054", "printf: %s")),
    PRINTF_WRONG_COUNT(TextKey.of("jsc.sigma.error.s3055", "printf: the format has %s and the call gives %s")),
    PRINTF_WRONG_VALUE(TextKey.of("jsc.sigma.error.s3056", "printf: '%%%s' takes %s, and this is %s")),

    // A4001 to A4010 are the listing's own problems, reported by reading one back (ListingError).
    NOT_YET_BUILT(TextKey.of("jsc.sigma.error.s4011", "%s is not built yet")),
    OLDEST_MACHINES_TAKE_SIGMA(TextKey.of("jsc.sigma.error.s4012", "%s runs Σ only; write it in Σ, in a Σ project"));

    private final TextKey text;

    SigmaError(final TextKey text) {
        this.text = text;
    }

    /** The code as it appears in a diagnostic, for example {@code S2001}. */
    public String code() {
        final String key = this.text.key();
        return key.substring(key.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
    }

    /**
     * The message with its placeholders filled in. A name, a type or a token goes in as it was written; a sentence
     * handed in is read in the same language as the message around it.
     */
    public Text message(final Object... arguments) {
        return this.text.with(arguments);
    }
}
