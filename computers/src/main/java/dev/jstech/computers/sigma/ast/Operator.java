/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.ast;

/**
 * What an operator node does, apart from how it was written.
 *
 * <p>The parser resolves the spelling once: a minus in front of a value is {@link #NEGATE} and a
 * minus between two is {@link #SUBTRACT}, and a compound assignment keeps the arithmetic operator
 * beside {@link IExpr.Assign}, so no later stage has to read the source text again.
 */
public enum Operator {

    NOT("!"),
    NEGATE("-"),
    PLUS("+"),
    COMPLEMENT("~"),
    INCREMENT("++"),
    DECREMENT("--"),
    /** {@code &x}: the address of a variable, or of a place a pointer reaches. */
    ADDRESS_OF("&"),
    /** {@code *p}: what a pointer points at; {@code p->F} is {@code (*p).F}. */
    DEREFERENCE("*"),

    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
    REMAINDER("%"),

    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS("<"),
    LESS_EQUAL("<="),
    GREATER(">"),
    GREATER_EQUAL(">="),

    AND("&&"),
    OR("||"),
    BIT_AND("&"),
    BIT_OR("|"),
    BIT_XOR("^"),
    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),

    ASSIGN("=");

    private final String text;

    Operator(final String text) {
        this.text = text;
    }

    /** How the operator is written in source. */
    public String text() {
        return this.text;
    }

    /** How an assignment with this operator is written: {@code +=} for add, plain {@code =} for assign. */
    public String assignmentText() {
        return this == ASSIGN ? this.text : this.text + "=";
    }

    /** Whether this operator takes one value rather than two. */
    public boolean isUnary() {
        return switch (this) {
            case NOT, NEGATE, PLUS, COMPLEMENT, INCREMENT, DECREMENT, ADDRESS_OF, DEREFERENCE -> true;
            default -> false;
        };
    }

    /**
     * The name of the method that gives this operator a meaning on a program's own type, C#'s own for it; null for
     * the ones no type can give one: {@code &&}, {@code ||}, the plain assignment, and the two of pointers.
     */
    public String methodName() {
        return switch (this) {
            case NOT -> "op_LogicalNot";
            case NEGATE -> "op_UnaryNegation";
            case PLUS -> "op_UnaryPlus";
            case COMPLEMENT -> "op_OnesComplement";
            case INCREMENT -> "op_Increment";
            case DECREMENT -> "op_Decrement";
            case ADD -> "op_Addition";
            case SUBTRACT -> "op_Subtraction";
            case MULTIPLY -> "op_Multiply";
            case DIVIDE -> "op_Division";
            case REMAINDER -> "op_Modulus";
            case EQUAL -> "op_Equality";
            case NOT_EQUAL -> "op_Inequality";
            case LESS -> "op_LessThan";
            case LESS_EQUAL -> "op_LessThanOrEqual";
            case GREATER -> "op_GreaterThan";
            case GREATER_EQUAL -> "op_GreaterThanOrEqual";
            case BIT_AND -> "op_BitwiseAnd";
            case BIT_OR -> "op_BitwiseOr";
            case BIT_XOR -> "op_ExclusiveOr";
            case SHIFT_LEFT -> "op_LeftShift";
            case SHIFT_RIGHT -> "op_RightShift";
            case AND, OR, ASSIGN, ADDRESS_OF, DEREFERENCE -> null;
        };
    }

    /** The operator a method of that name gives a meaning to, or null for a name that is no operator's. */
    public static Operator ofMethod(final String name) {
        for (final Operator operator : values()) {
            if (name.equals(operator.methodName())) {
                return operator;
            }
        }
        return null;
    }

    /** The comparison a type that declares this one has to declare as well, as C# asks, or null for none. */
    public Operator partner() {
        return switch (this) {
            case EQUAL -> NOT_EQUAL;
            case NOT_EQUAL -> EQUAL;
            case LESS -> GREATER;
            case GREATER -> LESS;
            case LESS_EQUAL -> GREATER_EQUAL;
            case GREATER_EQUAL -> LESS_EQUAL;
            default -> null;
        };
    }
}
