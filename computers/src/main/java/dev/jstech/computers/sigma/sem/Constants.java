/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.lex.TokenKind;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The values of the program's consts, worked out when compiling (computers/docs/design/sigma.md).
 *
 * <p>A const holds a number, a bool, a char, a string, an enum's value or null, or a value worked out from those and
 * from other consts. It is worked out here once, the way the language works it out when a program runs, so a whole
 * number that overflows wraps as it would. Every place that reads a const is then written as its value, which is what
 * folding it into the listing means: the listing keeps no field for it, and a program that changes a const is
 * compiled again with everything that reads it.
 */
final class Constants {

    private final SemanticModel model;
    private final Declarations declarations;
    /** Each const field and the declaration that gives it its value. */
    private final Map<IMemberSymbol.FieldSymbol, IDecl.FieldDecl> fields = new LinkedHashMap<>();
    /** The value each const field came to, or {@link #UNKNOWN} when it came to none. */
    private final Map<IMemberSymbol.FieldSymbol, Known> worked = new IdentityHashMap<>();
    /**
     * The const fields being worked out right now, in the order they were reached, so one worked out from itself is
     * caught rather than recursed, and the ones round the loop told from the ones that only lead into it.
     */
    private final Set<IMemberSymbol.FieldSymbol> working = new LinkedHashSet<>();
    private final Set<IMemberSymbol.FieldSymbol> circular = new HashSet<>();
    private final Map<IBinding.Variable, Known> locals = new IdentityHashMap<>();
    /**
     * Whether every const has been read, which is when one may be worked out: before that, a const read in another's
     * value may be one whose own value has not been read yet.
     */
    private boolean ready;

    /** What a const comes to when no value can be worked out. */
    private static final Known UNKNOWN = new Known(null);
    /** The bits a byte keeps. */
    private static final int BYTE_BITS = 0xFF;

    /** A value known when compiling; {@code value} is null for the null a reference const may hold. */
    record Known(Object value) {
    }

    Constants(final SemanticModel model, final Declarations declarations) {
        this.model = model;
        this.declarations = declarations;
    }

    /** Takes note of a const field and the declaration that gives it its value. */
    void declare(final IMemberSymbol.FieldSymbol field, final IDecl.FieldDecl declaration) {
        this.fields.put(field, declaration);
    }

    /** Every const field the program declares, with its declaration. */
    Map<IMemberSymbol.FieldSymbol, IDecl.FieldDecl> fields() {
        return this.fields;
    }

    /** Says every const has been read, so their values can be worked out from here on. */
    void ready() {
        this.ready = true;
    }

    /** The value of a const field, or null when it has none known: one never given, or one not known when compiling. */
    Known field(final IMemberSymbol.FieldSymbol field) {
        if (!this.ready) {
            return null;
        }
        final Known done = this.worked.get(field);
        if (done != null) {
            return done == UNKNOWN ? null : done;
        }
        final IDecl.FieldDecl declaration = this.fields.get(field);
        if (declaration == null || declaration.initializer() == null) {
            return null;
        }
        if (!this.working.add(field)) {
            // Every const from this one on is on the way round the loop; one reached before it only reads the loop.
            boolean onTheLoop = false;
            for (final IMemberSymbol.FieldSymbol reached : this.working) {
                onTheLoop |= reached == field;
                if (onTheLoop) {
                    this.circular.add(reached);
                }
            }
            return null;
        }
        final Known found = this.fold(declaration.initializer());
        this.working.remove(field);
        final Known held = found == null ? null : converted(found, field.type());
        this.worked.put(field, held == null ? UNKNOWN : held);
        return held;
    }

    /** Whether a const field turned out to be worked out from itself. */
    boolean circular(final IMemberSymbol.FieldSymbol field) {
        return this.circular.contains(field);
    }

    /** Takes note of the value a const local holds, worked out from its initializer as it was declared. */
    void local(final IBinding.Variable variable, final Known value) {
        this.locals.put(variable, value);
    }

    /** Whether that local is a const. */
    boolean isLocal(final IBinding.Variable variable) {
        return this.locals.containsKey(variable);
    }

    /**
     * Writes a read of a const as the const's value, when {@code read}, already checked, names one whose value is
     * known; anything else is left as it was written.
     */
    void replaceRead(final IExpr read) {
        final ITypeSymbol type = this.model.typeOf(read);
        if (!this.ready || type == null || !this.isConstRead(read)) {
            return;
        }
        final Known value = this.named(read);
        if (value != null) {
            this.model.setLowered(read, this.literal(value, type, read.line(), read.column()));
        }
    }

    /**
     * What an expression already checked comes to, when it is known when compiling: a literal, an operator over
     * known values, a cast of one, an enum's value or a const; null when it is none of those.
     */
    Known fold(final IExpr expression) {
        if (expression == null) {
            return null;
        }
        final ITypeSymbol type = this.model.typeOf(expression);
        return switch (expression) {
            case IExpr.Literal literal -> new Known(literal.kind() == TokenKind.NULL ? null : literal.value());
            case IExpr.Name ignored -> this.named(expression);
            case IExpr.Member ignored -> this.named(expression);
            case IExpr.Cast cast -> {
                final Known value = this.fold(cast.value());
                yield value == null ? null : converted(value, type);
            }
            case IExpr.Unary unary -> unary.postfix() ? null : unary(unary.operator(), this.fold(unary.operand()),
                    type);
            case IExpr.Binary binary -> this.binary(binary, type);
            case IExpr.Conditional conditional -> {
                final Known condition = this.fold(conditional.condition());
                if (condition == null || !(condition.value() instanceof Boolean chosen)) {
                    yield null;
                }
                final Known value = this.fold(chosen ? conditional.whenTrue() : conditional.whenFalse());
                yield value == null ? null : converted(value, type);
            }
            // A nameof is the text of the name it was handed, known as the call is checked.
            case IExpr.Call call when this.model.loweredOf(call) instanceof IExpr.Literal named -> this.fold(named);
            // So is a sizeof, the number of bytes its type takes.
            case IExpr.SizeOf sized when this.model.loweredOf(sized) instanceof IExpr.Literal size -> this.fold(size);
            default -> null;
        };
    }

    /** The literal a known value is written as, typed so the place it is read in takes it as it took the const. */
    IExpr.Literal literal(final Known known, final ITypeSymbol type, final int line, final int column) {
        final IExpr.Literal literal = switch (known.value()) {
            case null -> new IExpr.Literal(TokenKind.NULL, null, line, column);
            case Boolean yes -> new IExpr.Literal(yes ? TokenKind.TRUE : TokenKind.FALSE, yes, line, column);
            case Character letter -> new IExpr.Literal(TokenKind.CHAR_LITERAL, letter, line, column);
            case String text -> new IExpr.Literal(TokenKind.STRING_LITERAL, text, line, column);
            case Long number -> new IExpr.Literal(TokenKind.LONG_LITERAL, number, line, column);
            case Float number -> new IExpr.Literal(TokenKind.FLOAT_LITERAL, number, line, column);
            case Double number -> new IExpr.Literal(TokenKind.DOUBLE_LITERAL, number, line, column);
            case Number number -> new IExpr.Literal(TokenKind.INT_LITERAL, number.intValue(), line, column);
            default -> new IExpr.Literal(TokenKind.NULL, null, line, column);
        };
        this.model.setType(literal, type);
        return literal;
    }

    /** Whether a const may hold a value of that type other than null. */
    static boolean holdsValues(final ITypeSymbol type, final BuiltIns builtIns) {
        return type instanceof ITypeSymbol.Primitive primitive && primitive != ITypeSymbol.Primitive.VOID
                || type == builtIns.stringType()
                || type instanceof NamedType named && named.kind() == NamedType.Kind.ENUM;
    }

    /** A value worked out, as a value of that type: a number converted as a cast converts it; null when it is none. */
    static Known converted(final Known known, final ITypeSymbol type) {
        final Object value = known.value();
        if (!(type instanceof ITypeSymbol.Primitive primitive)) {
            // An enum's value is its number, a string is itself, and null is null.
            return value instanceof Character letter ? new Known((int) letter) : known;
        }
        if (primitive == ITypeSymbol.Primitive.BOOL) {
            return value instanceof Boolean ? known : null;
        }
        final Number number = numberOf(value);
        if (number == null) {
            return null;
        }
        return new Known(switch (primitive) {
            case LONG -> number.longValue();
            case FLOAT -> number.floatValue();
            case DOUBLE -> number.doubleValue();
            case CHAR -> (char) number.intValue();
            // A byte keeps the low eight bits, as a cast to it does.
            case BYTE -> number.intValue() & BYTE_BITS;
            default -> number.intValue();
        });
    }

    /* The value of the const a name or a member reaches, or of the enum's value it names; null for anything else. */
    private Known named(final IExpr read) {
        final IBinding binding = this.model.bindingOf(read);
        if (binding instanceof IBinding.Variable variable) {
            return this.locals.get(variable);
        }
        if (!(binding instanceof IBinding.Member member)
                || !(member.member() instanceof IMemberSymbol.FieldSymbol field)) {
            return null;
        }
        if (field.isConstant()) {
            return this.field(field);
        }
        if (field.owner().kind() == NamedType.Kind.ENUM
                && this.declarations.source(field.owner()) instanceof IDecl.EnumDecl numbers) {
            return new Known(enumNumber(numbers, field.name()));
        }
        return null;
    }

    /* Whether a read reaches a const, which is what is written as its value; an enum's value is read as it was. */
    private boolean isConstRead(final IExpr read) {
        final IBinding binding = this.model.bindingOf(read);
        return binding instanceof IBinding.Variable variable && this.locals.containsKey(variable)
                || binding instanceof IBinding.Member member
                && member.member() instanceof IMemberSymbol.FieldSymbol field && field.isConstant();
    }

    /* The number behind an enum's name, counted as the listing counts it: one more than the one before. */
    private static int enumNumber(final IDecl.EnumDecl numbers, final String name) {
        int next = 0;
        for (final IDecl.EnumConstant constant : numbers.constants()) {
            final Integer given = constant.value() == null ? null : BodyChecker.numberOf(constant.value());
            final int number = given == null ? next : given;
            if (constant.name().equals(name)) {
                return number;
            }
            next = number + 1;
        }
        return 0;
    }

    private static Number numberOf(final Object value) {
        return value instanceof Character letter ? Integer.valueOf(letter)
                : value instanceof Number number ? number : null;
    }

    private static Known unary(final Operator operator, final Known operand, final ITypeSymbol type) {
        if (operand == null) {
            return null;
        }
        if (operator == Operator.NOT) {
            return operand.value() instanceof Boolean yes ? new Known(!yes) : null;
        }
        final Known number = converted(operand, type);
        if (number == null || !(number.value() instanceof Number held)) {
            return null;
        }
        return switch (operator) {
            case PLUS -> number;
            case NEGATE -> new Known(switch (held) {
                case Long whole -> -whole;
                case Float real -> -real;
                case Double real -> -real;
                default -> -held.intValue();
            });
            case COMPLEMENT -> held instanceof Long whole ? new Known(~whole) : new Known(~held.intValue());
            default -> null;
        };
    }

    private Known binary(final IExpr.Binary binary, final ITypeSymbol type) {
        final Known left = this.fold(binary.left());
        final Known right = this.fold(binary.right());
        if (left == null || right == null) {
            return null;
        }
        final Object a = left.value();
        final Object b = right.value();
        return switch (binary.operator()) {
            case AND -> a instanceof Boolean x && b instanceof Boolean y ? new Known(x && y) : null;
            case OR -> a instanceof Boolean x && b instanceof Boolean y ? new Known(x || y) : null;
            case EQUAL, NOT_EQUAL -> {
                final Boolean same = same(a, b, this.wider(binary));
                yield same == null ? null : new Known(same == (binary.operator() == Operator.EQUAL));
            }
            case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> compared(binary.operator(), a, b, this.wider(binary));
            case ADD -> a instanceof String || b instanceof String
                    ? (a instanceof String x && b instanceof String y ? new Known(x + y) : null)
                    : arithmetic(binary.operator(), a, b, type);
            default -> arithmetic(binary.operator(), a, b, type);
        };
    }

    /* The type two numbers are compared in: the wider of the two, as the language widens them. */
    private ITypeSymbol.Primitive wider(final IExpr.Binary binary) {
        final ITypeSymbol.Primitive left = numeric(this.model.typeOf(binary.left()));
        final ITypeSymbol.Primitive right = numeric(this.model.typeOf(binary.right()));
        if (left == null || right == null) {
            return null;
        }
        return rank(left) >= rank(right) ? left : right;
    }

    /* A number's type for working out: a char and an enum's value are worked out as int. */
    private static ITypeSymbol.Primitive numeric(final ITypeSymbol type) {
        if (type instanceof ITypeSymbol.Primitive primitive) {
            return switch (primitive) {
                case INT, LONG, FLOAT, DOUBLE -> primitive;
                case CHAR, BYTE -> ITypeSymbol.Primitive.INT;
                default -> null;
            };
        }
        return type instanceof NamedType named && named.kind() == NamedType.Kind.ENUM ? ITypeSymbol.Primitive.INT
                : null;
    }

    private static int rank(final ITypeSymbol.Primitive type) {
        return switch (type) {
            case DOUBLE -> 3;
            case FLOAT -> 2;
            case LONG -> 1;
            default -> 0;
        };
    }

    private static Boolean same(final Object a, final Object b, final ITypeSymbol.Primitive wider) {
        if (wider == null) {
            return a instanceof Boolean || b instanceof Boolean || a instanceof String || b instanceof String
                    || a == null || b == null ? Objects.equals(a, b) : null;
        }
        final Known x = converted(new Known(a), wider);
        final Known y = converted(new Known(b), wider);
        return x == null || y == null ? null : x.value().equals(y.value());
    }

    private static Known compared(final Operator operator, final Object a, final Object b,
                                  final ITypeSymbol.Primitive wider) {
        if (wider == null) {
            return null;
        }
        final Known x = converted(new Known(a), wider);
        final Known y = converted(new Known(b), wider);
        if (x == null || y == null) {
            return null;
        }
        final int order = switch (wider) {
            case LONG -> Long.compare((Long) x.value(), (Long) y.value());
            case FLOAT -> Float.compare((Float) x.value(), (Float) y.value());
            case DOUBLE -> Double.compare((Double) x.value(), (Double) y.value());
            default -> Integer.compare((Integer) x.value(), (Integer) y.value());
        };
        final boolean unordered = wider == ITypeSymbol.Primitive.FLOAT && ((Float) x.value()).isNaN()
                || wider == ITypeSymbol.Primitive.DOUBLE && ((Double) x.value()).isNaN()
                || wider == ITypeSymbol.Primitive.FLOAT && ((Float) y.value()).isNaN()
                || wider == ITypeSymbol.Primitive.DOUBLE && ((Double) y.value()).isNaN();
        if (unordered) {
            // Nothing is less than, more than or equal to a number that is not one.
            return new Known(false);
        }
        return new Known(switch (operator) {
            case LESS -> order < 0;
            case LESS_EQUAL -> order <= 0;
            case GREATER -> order > 0;
            default -> order >= 0;
        });
    }

    /*
     * The operators over numbers and bools, worked out in the type the checker gave the result, so an int that runs
     * past its top wraps as it does when the program runs. A whole number divided by zero is not known: it is a
     * mistake the program would stop on, not a value.
     */
    private static Known arithmetic(final Operator operator, final Object a, final Object b, final ITypeSymbol type) {
        if (a instanceof Boolean x && b instanceof Boolean y) {
            return switch (operator) {
                case BIT_AND -> new Known(x & y);
                case BIT_OR -> new Known(x | y);
                case BIT_XOR -> new Known(x ^ y);
                default -> null;
            };
        }
        final ITypeSymbol.Primitive in = numeric(type);
        if (in == null) {
            return null;
        }
        if (operator == Operator.SHIFT_LEFT || operator == Operator.SHIFT_RIGHT) {
            final Known count = converted(new Known(b), ITypeSymbol.Primitive.INT);
            final Known value = converted(new Known(a), in);
            if (count == null || value == null) {
                return null;
            }
            final int by = (Integer) count.value();
            return in == ITypeSymbol.Primitive.LONG
                    ? new Known(operator == Operator.SHIFT_LEFT ? (Long) value.value() << by
                            : (Long) value.value() >> by)
                    : new Known(operator == Operator.SHIFT_LEFT ? (Integer) value.value() << by
                            : (Integer) value.value() >> by);
        }
        final Known x = converted(new Known(a), in);
        final Known y = converted(new Known(b), in);
        if (x == null || y == null) {
            return null;
        }
        return switch (in) {
            case LONG -> whole((Long) x.value(), (Long) y.value(), operator);
            case FLOAT -> {
                final Double real = real((Float) x.value(), (Float) y.value(), operator);
                yield real == null ? null : new Known(real.floatValue());
            }
            case DOUBLE -> {
                final Double real = real((Double) x.value(), (Double) y.value(), operator);
                yield real == null ? null : new Known(real);
            }
            default -> {
                final Known wide = whole((Integer) x.value(), (Integer) y.value(), operator);
                yield wide == null ? null : new Known(((Long) wide.value()).intValue());
            }
        };
    }

    private static Known whole(final long x, final long y, final Operator operator) {
        return switch (operator) {
            case ADD -> new Known(x + y);
            case SUBTRACT -> new Known(x - y);
            case MULTIPLY -> new Known(x * y);
            case DIVIDE -> y == 0 ? null : new Known(x / y);
            case REMAINDER -> y == 0 ? null : new Known(x % y);
            case BIT_AND -> new Known(x & y);
            case BIT_OR -> new Known(x | y);
            case BIT_XOR -> new Known(x ^ y);
            default -> null;
        };
    }

    private static Double real(final double x, final double y, final Operator operator) {
        return switch (operator) {
            case ADD -> x + y;
            case SUBTRACT -> x - y;
            case MULTIPLY -> x * y;
            case DIVIDE -> x / y;
            case REMAINDER -> x % y;
            default -> null;
        };
    }
}
