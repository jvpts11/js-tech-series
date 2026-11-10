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
    DIRECTIVE_SAYS(TextKey.of("jsc.sigma.error.s1007", "%s")),
    BAD_DIRECTIVE(TextKey.of("jsc.sigma.error.s1008", "'#%s' is not a directive the language reads")),
    BAD_CONDITION(TextKey.of("jsc.sigma.error.s1009", "'%s' is not a condition #%s can read")),
    UNOPENED_DIRECTIVE(TextKey.of("jsc.sigma.error.s1010", "#%s has no #if open before it")),
    UNCLOSED_DIRECTIVE(TextKey.of("jsc.sigma.error.s1011", "this #if is never closed with an #endif")),
    ELSE_TWICE(TextKey.of("jsc.sigma.error.s1012", "this #if already had its #else, so #%s comes too late")),

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
    NOT_IN_THE_LANGUAGE(TextKey.of("jsc.sigma.error.s2014",
            "'%s' is kept for a later version of the language; for now, %s")),
    EXPECTED_STATEMENT(TextKey.of("jsc.sigma.error.s2015", "a label marks a statement, and %s is not one")),

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
            "%s's format has to be written out where it is used, in quotes, so its holes can be read")),
    PRINTF_BAD_FORMAT(TextKey.of("jsc.sigma.error.s3054", "%s: %s")),
    PRINTF_WRONG_COUNT(TextKey.of("jsc.sigma.error.s3055", "%s: the format has %s and the call gives %s")),
    PRINTF_WRONG_VALUE(TextKey.of("jsc.sigma.error.s3056", "%s: '%%%s' takes %s, and this is %s")),
    NEEDS_A_LATER_VERSION(TextKey.of("jsc.sigma.error.s3057", "'%s' needs %s %s; this project is %s %s")),
    REF_ONLY_FOR_STRCAT(TextKey.of("jsc.sigma.error.s3058",
            "'ref' is written only for strcat, which joins onto the text '%s' holds; a method fills in with out")),
    CIRCULAR_BASE(TextKey.of("jsc.sigma.error.s3059",
            "'%s' already stands behind '%s', so it cannot also stand in front of it")),
    NOT_ACCESSIBLE(TextKey.of("jsc.sigma.error.s3060", "'%s' is %s to '%s', so it cannot be reached from here")),
    VALUE_NOT_KNOWN(TextKey.of("jsc.sigma.error.s3061",
            "'%s' is written with =>, so its value has to be one the compiler knows: one written out, or one the"
                    + " library names")),
    NOT_DECLARED(TextKey.of("jsc.sigma.error.s3062",
            "'%s' says what it is with '%s', written with => and a value the compiler knows, and has none")),
    NOT_ONE_OF(TextKey.of("jsc.sigma.error.s3063", "'%s' is not one of %s")),
    ONE_SYSTEM(TextKey.of("jsc.sigma.error.s3064",
            "a program holds at most one system and one desktop, and this one has %s %s")),
    POLICY_OF_ITS_OWN(TextKey.of("jsc.sigma.error.s3065",
            "'%s' chooses for a kernel of the system's own: start it with Kernel.Own(), since Kernel.Of takes one"
                    + " of the mod's kernels as it is")),
    CONST_ASSIGNED(TextKey.of("jsc.sigma.error.s3066", "'%s' is a const, so its value never changes")),
    CONST_NOT_KNOWN(TextKey.of("jsc.sigma.error.s3067",
            "'%s' is a const, so its value is one known when compiling: a number, a bool, a char, a string, an"
                    + " enum's value or null, or one worked out from those")),
    CONST_CIRCULAR(TextKey.of("jsc.sigma.error.s3068", "the value of the const '%s' is worked out from itself")),
    CONST_NEEDS_VALUE(TextKey.of("jsc.sigma.error.s3069",
            "'%s' is a const, so it is given its value where it is declared")),
    CONST_TYPE(TextKey.of("jsc.sigma.error.s3070",
            "a const cannot be a '%s' unless it is null: a const holds a number, a bool, a char, a string or an"
                    + " enum's value")),
    LABEL_TWICE(TextKey.of("jsc.sigma.error.s3071", "the label '%s' is written twice in this method")),
    NO_SUCH_LABEL(TextKey.of("jsc.sigma.error.s3072", "there is no label '%s' in this method")),
    LABEL_OUT_OF_REACH(TextKey.of("jsc.sigma.error.s3073",
            "'%s' is inside a block this cannot enter: a jump may leave a block, never enter one")),
    NO_SUCH_CASE(TextKey.of("jsc.sigma.error.s3074", "the switch this stands in has no %s")),
    NO_SWITCH(TextKey.of("jsc.sigma.error.s3075", "'goto %s' jumps inside the switch it stands in, and there is none")),
    COMEFROM_TWICE(TextKey.of("jsc.sigma.error.s3076", "the label '%s' already has a comefrom, and it takes one")),
    COMEFROM_OUT_OF_REACH(TextKey.of("jsc.sigma.error.s3077",
            "a comefrom stands in a block that holds its label, and '%s' is not in this one")),
    NOT_A_PLACE_TO_IGNORE(TextKey.of("jsc.sigma.error.s3078",
            "only a local, a field or a static field can be ignored or remembered")),
    SEALED_BASE(TextKey.of("jsc.sigma.error.s3079", "'%s' is sealed, so nothing can stand on it or replace it")),
    SEALED_NEEDS_OVERRIDE(TextKey.of("jsc.sigma.error.s3080",
            "'%s' replaces nothing, so there is nothing for sealed to stop: sealed goes with override")),
    STATIC_CLASS_MEMBER(TextKey.of("jsc.sigma.error.s3081",
            "'%s' is in a static class, so it has to be static as well")),
    CANNOT_CREATE_STATIC(TextKey.of("jsc.sigma.error.s3082", "'%s' is a static class, so it is never made with new")),
    STATIC_CLASS_BASE(TextKey.of("jsc.sigma.error.s3083",
            "a static class stands on nothing, so '%s' cannot stand behind its colon")),
    PARAMS_LAST(TextKey.of("jsc.sigma.error.s3084", "params goes on the last parameter, and '%s' is not the last")),
    PARAMS_ARRAY(TextKey.of("jsc.sigma.error.s3085",
            "a params parameter is an array the method reads, and '%s' is not")),
    NAMEOF_NEEDS_NAME(TextKey.of("jsc.sigma.error.s3086",
            "nameof takes one name: a local, a member or a type, written as it is read")),
    LOCAL_FUNCTION_ASSIGNED(TextKey.of("jsc.sigma.error.s3087",
            "'%s' is a local function, so nothing can be written to it")),
    TUPLE_SIZE(TextKey.of("jsc.sigma.error.s3088", "a tuple has from two to eight places, and this one has %s")),
    TUPLE_PLACE_TYPE(TextKey.of("jsc.sigma.error.s3089",
            "a tuple's place holds a value, and '%s' is none: give the place a value, or write the tuple's type")),
    DECONSTRUCT_SHAPE(TextKey.of("jsc.sigma.error.s3090",
            "'%s' is not a tuple of %s places, so it cannot be taken apart into them")),
    EXTENSION_FIRST(TextKey.of("jsc.sigma.error.s3091",
            "'this' goes on the first parameter of an extension method, a plain one, and '%s' is not that")),
    EXTENSION_PLACE(TextKey.of("jsc.sigma.error.s3092",
            "an extension method is a static method of a static class at the top of a namespace, and '%s' is not")),
    OPERATOR_SHAPE(TextKey.of("jsc.sigma.error.s3093",
            "'%s' is not an operator a type can declare: one is public and static, and takes as many plain values as"
                    + " its sign has sides; ++ and -- give back the type they take")),
    OPERATOR_TYPE(TextKey.of("jsc.sigma.error.s3094", "an operator of '%s' takes a '%s' as one of its values")),
    OPERATOR_PAIR(TextKey.of("jsc.sigma.error.s3095",
            "'%s' declares the operator %s, so it declares %s as well, taking the same values")),
    INDEXER_SHAPE(TextKey.of("jsc.sigma.error.s3096",
            "an indexer is of an object, and takes one value in its brackets: '%s' does not")),
    CATCH_TYPE(TextKey.of("jsc.sigma.error.s3097",
            "a catch takes an exception, and '%s' is not one: it is Exception or a class standing on it")),
    CATCH_UNREACHABLE(TextKey.of("jsc.sigma.error.s3098",
            "a catch before this one already takes every '%s', so this one is never reached")),
    FINALLY_EXIT(TextKey.of("jsc.sigma.error.s3099",
            "nothing leaves a finally but its own end: no return, break, continue or jump out of it")),
    THROW_TYPE(TextKey.of("jsc.sigma.error.s3100", "only an exception can be thrown, and '%s' is not one")),
    RETHROW_OUTSIDE(TextKey.of("jsc.sigma.error.s3101",
            "'throw;' throws on the exception a catch took, so it stands in a catch and not in a finally inside one")),
    POINTERS_NEED_UNSAFE(TextKey.of("jsc.sigma.error.s3102",
            "a pointer is held only in unsafe code: write the method unsafe, or put this in an unsafe block")),
    POINTER_TARGET(TextKey.of("jsc.sigma.error.s3103",
            "'%s*' is no pointer the language has: a pointer is to int, long, byte, void or a struct of values")),
    ADDRESS_OF_WHAT(TextKey.of("jsc.sigma.error.s3104",
            "& takes a local variable, a parameter, or a place a pointer reaches, and this is none of them")),
    ADDRESS_KEPT(TextKey.of("jsc.sigma.error.s3105",
            "the address of '%s' cannot be taken: a lambda or a local function keeps it, and it lives where they do")),
    IGNORE_IN_MEMORY(TextKey.of("jsc.sigma.error.s3106",
            "'%s' lives in raw memory once its address is taken, where ignore and remember cannot reach it")),
    SIZEOF_TYPE(TextKey.of("jsc.sigma.error.s3107", "sizeof takes a type that lies in raw memory, and '%s' does not")),

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
