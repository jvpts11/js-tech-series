/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

/**
 * Every message the front end can produce, with the code a player quotes when asking for help.
 *
 * <p>The numbering is by stage, so a code says where the compiler gave up: S1xxx while reading the
 * characters, S2xxx while reading the grammar. Later stages take the ranges above them.
 */
public enum SigmaError {

    UNTERMINATED_STRING("S1001", "the string was never closed before the end of the line"),
    UNTERMINATED_COMMENT("S1002", "the comment was never closed before the end of the file"),
    UNEXPECTED_CHARACTER("S1003", "'%s' does not begin anything the language knows"),
    MALFORMED_NUMBER("S1004", "'%s' is not a number the language can read"),
    INVALID_CHARACTER_LITERAL("S1005", "a character literal holds exactly one character"),
    UNKNOWN_ESCAPE("S1006", "'\\%s' is not an escape the language knows"),

    EXPECTED_TOKEN("S2001", "expected %s but found %s"),
    EXPECTED_TYPE("S2002", "expected a type but found %s"),
    EXPECTED_EXPRESSION("S2003", "expected an expression but found %s"),
    EXPECTED_MEMBER("S2004", "expected a field, a method, a property or an event but found %s"),
    EXPECTED_TYPE_DECLARATION("S2005", "expected a class, a struct, a record, an interface, an enum or a "
            + "delegate but found %s"),
    NOT_A_STATEMENT("S2006", "only a call, an assignment, an increment, a decrement or a new object "
            + "can be used as a statement"),
    DUPLICATE_MODIFIER("S2007", "'%s' was given twice"),
    USING_TOO_LATE("S2009", "a using has to come before the namespace and the types"),
    ONE_NAMESPACE("S2010", "a file declares one namespace on its own line, before its types; put more "
            + "of them in blocks"),
    INVALID_ASSIGNMENT_TARGET("S2008", "the left side of an assignment must be a variable, a field, "
            + "a property or an element"),
    NAMESPACE_REQUIRED("S2011", "every type is in a namespace: put 'namespace Name;' at the top of the file"),
    USING_NEEDS_STAR("S2012", "'%s' names a namespace; write 'using %s.*;' to bring in everything in it, or "
            + "name one of its types"),
    NESTING_TOO_DEEP("S2013", "this is nested more than %s levels deep, which is more than the compiler reads; "
            + "the rest of the file was not read"),

    UNKNOWN_NAME("S3001", "'%s' does not name anything here"),
    DUPLICATE_DECLARATION("S3002", "'%s' is already declared here"),
    CANNOT_CONVERT("S3003", "cannot convert '%s' to '%s'"),
    NO_SUCH_MEMBER("S3004", "'%s' has no member called '%s'"),
    NO_MATCHING_OVERLOAD("S3005", "no version of '%s' takes those arguments"),
    AMBIGUOUS_CALL("S3006", "the call to '%s' fits more than one version of it"),
    OPERATOR_ON_TYPES("S3007", "'%s' cannot be applied to '%s' and '%s'"),
    OPERATOR_ON_TYPE("S3008", "'%s' cannot be applied to '%s'"),
    CONDITION_MUST_BE_BOOL("S3009", "a condition is a bool, not '%s'"),
    BREAK_OUTSIDE_LOOP("S3010", "'break' only means something inside a loop or a switch"),
    CONTINUE_OUTSIDE_LOOP("S3011", "'continue' only means something inside a loop"),
    MISSING_RETURN_VALUE("S3012", "a method that gives back '%s' must return a value"),
    UNEXPECTED_RETURN_VALUE("S3013", "a method that gives back nothing cannot return a value"),
    THIS_IN_STATIC("S3014", "'%s' is not available in a static member"),
    NO_BASE_CLASS("S3015", "'%s' has no base class"),
    CANNOT_ASSIGN_READONLY("S3016", "'%s' is readonly, so it can only be written where it is declared "
            + "or in a constructor"),
    ENTRY_POINT("S3017", "a program needs exactly one place to start, either a class that implements "
            + "IScript or a class with a static Main, and this one has %s"),
    MISSING_INTERFACE_MEMBER("S3018", "'%s' says it is a '%s' but does not have '%s'"),
    WRONG_TYPE_ARGUMENT_COUNT("S3019", "'%s' takes %s type arguments"),
    STATIC_THROUGH_INSTANCE("S3020", "'%s' belongs to the type, not to one of its objects"),
    INSTANCE_THROUGH_TYPE("S3021", "'%s' belongs to an object, not to the type"),
    CANNOT_DISPOSE("S3022", "only an object can be disposed, not '%s'"),
    NOT_A_COLLECTION("S3023", "foreach walks an array or a list, not '%s'"),
    CANNOT_INDEX("S3024", "'%s' cannot be indexed by '%s'"),
    CANNOT_CALL("S3025", "'%s' is not something that can be called"),
    CANNOT_CREATE("S3026", "'%s' cannot be made with new"),
    LAMBDA_SHAPE("S3027", "this lambda does not have the shape '%s' takes"),
    NOT_A_TYPE("S3028", "'%s' does not name a type"),
    DUPLICATE_SWITCH_LABEL("S3029", "this switch already has that label"),
    EVENT_OUTSIDE_ITS_TYPE("S3030", "an event can only be raised inside the type that declares it"),
    INVALID_BASE("S3031", "only a class or an interface can stand behind the colon, and '%s' is neither"),
    EVENT_NEEDS_DELEGATE("S3032", "an event's type must be a delegate, and '%s' is not one"),
    METHOD_AS_VALUE("S3033", "'%s' is a method: call it, or hand it over where a delegate of its shape "
            + "is wanted"),
    OUT_ARGUMENT_EXPECTED("S3034", "'%s' is filled in by the method, so the argument is written with out"),
    OUT_ARGUMENT_UNEXPECTED("S3035", "'%s' is read by the method, so the argument cannot be written with out"),
    OUT_TYPE_MUST_MATCH("S3036", "an out argument is exactly '%s', with no conversion on the way"),
    OUT_NOT_ASSIGNED("S3037", "'%s' is filled in by this method, so it must be given a value before "
            + "every way out"),
    OUT_NOT_A_PLACE("S3038", "'%s' is not somewhere a method can write"),
    ENUM_VALUE_MUST_BE_WRITTEN("S3039", "an enum's number has to be written as a number"),
    NEEDS_USING("S3040", "'%s' is in %s; add 'using %s.*;' or 'using %s.%s;' at the top of the file"),
    STRUCT_NO_BASE("S3041", "a struct can implement interfaces but stands on no class, and '%s' is a class"),
    CANNOT_LOCK("S3042", "only an object can be locked, not '%s'"),
    NEEDS_OVERRIDE("S3043", "'%s' is already on '%s', so write it with override to replace it"),
    CANNOT_OVERRIDE("S3044", "'%s' on '%s' is not virtual, so nothing can replace it; mark it virtual there"),
    OVERRIDE_WITHOUT_BASE("S3045", "'%s' is written with override but nothing above '%s' has it"),
    ABSTRACT_NEEDS_ABSTRACT_CLASS("S3046", "'%s' has no body, so '%s' has to be declared abstract"),
    ABSTRACT_WITH_BODY("S3047", "'%s' is abstract, so it is written with a semicolon and no body"),
    MISSING_BODY("S3048", "'%s' has no body, so it is written with abstract"),
    ABSTRACT_NOT_IMPLEMENTED("S3049", "'%s' is not abstract, so it has to give '%s' a body"),
    CANNOT_CREATE_ABSTRACT("S3050", "'%s' is abstract, so it cannot be made with new"),
    MODIFIER_NOT_ALLOWED("S3051", "'%s' cannot be written with %s"),
    NOT_IN_THE_SUBSET("S3052", "Sigma has no %s; %s"),
    PRINTF_FORMAT_NOT_WRITTEN_OUT("S3053", "printf's format has to be written out where it is used, in quotes, "
            + "so its holes can be read"),
    PRINTF_BAD_FORMAT("S3054", "printf: %s"),
    PRINTF_WRONG_COUNT("S3055", "printf: the format has %s and the call gives %s"),
    PRINTF_WRONG_VALUE("S3056", "printf: '%%%s' takes %s, and this is %s"),

    // A4001 to A4010 are the listing's own problems, reported by reading one back (ListingError).
    NOT_YET_BUILT("S4011", "%s is not built yet"),
    OLDEST_MACHINES_TAKE_SIGMA("S4012", "%s runs Σ only; write it in Σ, in a Σ project"),

    LUA_UNTERMINATED_STRING("L1001", "unfinished string"),
    LUA_UNTERMINATED_LONG("L1002", "unfinished long %s"),
    LUA_UNEXPECTED_CHARACTER("L1003", "unexpected symbol near '%s'"),
    LUA_MALFORMED_NUMBER("L1004", "malformed number near '%s'"),
    LUA_BAD_ESCAPE("L1005", "invalid escape sequence '\\%s'"),
    LUA_EXPECTED("L2001", "'%s' expected near %s"),
    LUA_EXPECTED_CLOSE("L2002", "'%s' expected (to close '%s' at line %s) near %s"),
    LUA_UNEXPECTED("L2003", "unexpected symbol near %s"),
    LUA_ASSIGNMENT_TARGET("L2004", "syntax error near %s"),
    LUA_BREAK_OUTSIDE_LOOP("L2005", "<break> not inside a loop"),
    LUA_VARARG_OUTSIDE("L2006", "cannot use '...' outside a vararg function"),
    LUA_NOT_SUPPORTED("L2007", "%s is not supported on this runtime");

    private final String code;
    private final String template;

    SigmaError(final String code, final String template) {
        this.code = code;
        this.template = template;
    }

    /** The code as it appears in a diagnostic, for example {@code S2001}. */
    public String code() {
        return this.code;
    }

    /** The message with its placeholders filled in. */
    public String message(final Object... arguments) {
        return arguments.length == 0 ? this.template : String.format(this.template, arguments);
    }
}
