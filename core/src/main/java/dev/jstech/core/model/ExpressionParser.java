/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * Reads the expressions of keyframes: the Core's own expression language, which reads the Molang Bedrock files carry.
 * Numbers and {@code + - * /}, comparisons and {@code && || !}, {@code a ? b : c} and {@code a ?? b}, brackets,
 * queries ({@code query.anim_time}, {@code q.life_time}, and any a mod gives), variables set and read
 * ({@code variable.x = 2; return v.x * 3;}), and the functions of {@code math.} Molang has, its angles in degrees. An
 * expression that is only a number is kept as one. Pure.
 */
public final class ExpressionParser {

    private final String source;
    private final List<String> tokens;
    private int at;

    private static final double TRUE = 1.0;
    private static final double FALSE = 0.0;

    private ExpressionParser(final String source) {
        this.source = source;
        this.tokens = tokenize(source);
    }

    /**
     * The expression {@code source} writes.
     *
     * @throws ModelFormatException when it is not one
     */
    public static ModelExpression parse(final String source) {
        final String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return ModelExpression.ZERO;
        }
        try {
            return ModelExpression.constant(Double.parseDouble(trimmed));
        } catch (final NumberFormatException notANumber) {
            return new ExpressionParser(trimmed.toLowerCase(Locale.ROOT)).program();
        }
    }

    /* Statements apart by semicolons, the last or the one after return giving the value. */
    private ModelExpression program() {
        final List<ModelExpression> statements = new ArrayList<>();
        ModelExpression value = null;
        while (this.at < this.tokens.size()) {
            if (accept(";")) {
                continue;
            }
            if (accept("return")) {
                value = ternary();
                break;
            }
            final ModelExpression statement = statement();
            statements.add(statement);
            value = statement;
        }
        if (this.at < this.tokens.size() && !peek().equals(";")) {
            throw new ModelFormatException("the expression \"" + this.source + "\" has more after its value: "
                    + peek());
        }
        final ModelExpression result = value == null ? ModelExpression.ZERO : value;
        if (statements.isEmpty() || statements.size() == 1 && statements.getFirst() == result) {
            return result;
        }
        final ModelExpression[] steps = statements.toArray(ModelExpression[]::new);
        return context -> {
            double last = 0.0;
            for (final ModelExpression step : steps) {
                last = step.evaluate(context);
            }
            return result == steps[steps.length - 1] ? last : result.evaluate(context);
        };
    }

    /* An assignment to a variable, or an expression. */
    private ModelExpression statement() {
        if (this.at + 1 < this.tokens.size() && isVariable(peek()) && this.tokens.get(this.at + 1).equals("=")) {
            final String name = variableName(next());
            next();
            final ModelExpression value = ternary();
            return context -> {
                final double set = value.evaluate(context);
                context.setVariable(name, set);
                return set;
            };
        }
        return ternary();
    }

    private ModelExpression ternary() {
        final ModelExpression condition = coalesce();
        if (!accept("?")) {
            return condition;
        }
        final ModelExpression yes = ternary();
        if (!accept(":")) {
            return context -> condition.evaluate(context) != FALSE ? yes.evaluate(context) : FALSE;
        }
        final ModelExpression no = ternary();
        return context -> condition.evaluate(context) != FALSE ? yes.evaluate(context) : no.evaluate(context);
    }

    /* Molang's {@code ??}: a variable never set gives way to what is after it; any other value stands. */
    private ModelExpression coalesce() {
        final ModelExpression left = or();
        if (!accept("??")) {
            return left;
        }
        final ModelExpression right = coalesce();
        if (left instanceof VariableRead read) {
            return context -> context.hasVariable(read.name()) ? context.variable(read.name())
                    : right.evaluate(context);
        }
        return left;
    }

    private ModelExpression or() {
        ModelExpression left = and();
        while (accept("||")) {
            final ModelExpression a = left;
            final ModelExpression b = and();
            left = context -> a.evaluate(context) != FALSE || b.evaluate(context) != FALSE ? TRUE : FALSE;
        }
        return left;
    }

    private ModelExpression and() {
        ModelExpression left = equality();
        while (accept("&&")) {
            final ModelExpression a = left;
            final ModelExpression b = equality();
            left = context -> a.evaluate(context) != FALSE && b.evaluate(context) != FALSE ? TRUE : FALSE;
        }
        return left;
    }

    private ModelExpression equality() {
        ModelExpression left = comparison();
        while (true) {
            final ModelExpression a = left;
            if (accept("==")) {
                final ModelExpression b = comparison();
                left = context -> a.evaluate(context) == b.evaluate(context) ? TRUE : FALSE;
            } else if (accept("!=")) {
                final ModelExpression b = comparison();
                left = context -> a.evaluate(context) != b.evaluate(context) ? TRUE : FALSE;
            } else {
                return left;
            }
        }
    }

    private ModelExpression comparison() {
        ModelExpression left = sum();
        while (true) {
            final ModelExpression a = left;
            if (accept("<=")) {
                final ModelExpression b = sum();
                left = context -> a.evaluate(context) <= b.evaluate(context) ? TRUE : FALSE;
            } else if (accept(">=")) {
                final ModelExpression b = sum();
                left = context -> a.evaluate(context) >= b.evaluate(context) ? TRUE : FALSE;
            } else if (accept("<")) {
                final ModelExpression b = sum();
                left = context -> a.evaluate(context) < b.evaluate(context) ? TRUE : FALSE;
            } else if (accept(">")) {
                final ModelExpression b = sum();
                left = context -> a.evaluate(context) > b.evaluate(context) ? TRUE : FALSE;
            } else {
                return left;
            }
        }
    }

    private ModelExpression sum() {
        ModelExpression left = product();
        while (true) {
            final ModelExpression a = left;
            if (accept("+")) {
                final ModelExpression b = product();
                left = folded(a, b, context -> a.evaluate(context) + b.evaluate(context));
            } else if (accept("-")) {
                final ModelExpression b = product();
                left = folded(a, b, context -> a.evaluate(context) - b.evaluate(context));
            } else {
                return left;
            }
        }
    }

    private ModelExpression product() {
        ModelExpression left = unary();
        while (true) {
            final ModelExpression a = left;
            if (accept("*")) {
                final ModelExpression b = unary();
                left = folded(a, b, context -> a.evaluate(context) * b.evaluate(context));
            } else if (accept("/")) {
                final ModelExpression b = unary();
                // Molang divides by zero into zero rather than into infinity.
                left = folded(a, b, context -> {
                    final double under = b.evaluate(context);
                    return under == 0.0 ? 0.0 : a.evaluate(context) / under;
                });
            } else {
                return left;
            }
        }
    }

    private ModelExpression unary() {
        if (accept("-")) {
            final ModelExpression value = unary();
            return value.constant() ? ModelExpression.constant(-value.evaluate(null))
                    : context -> -value.evaluate(context);
        }
        if (accept("+")) {
            return unary();
        }
        if (accept("!")) {
            final ModelExpression value = unary();
            return context -> value.evaluate(context) == FALSE ? TRUE : FALSE;
        }
        return primary();
    }

    private ModelExpression primary() {
        if (accept("(")) {
            final ModelExpression inside = ternary();
            expect(")");
            return inside;
        }
        final String token = next();
        if (Character.isDigit(token.charAt(0)) || token.charAt(0) == '.') {
            return ModelExpression.constant(number(token));
        }
        if (token.equals("true")) {
            return ModelExpression.constant(TRUE);
        }
        if (token.equals("false")) {
            return ModelExpression.constant(FALSE);
        }
        if (token.startsWith("query.") || token.startsWith("q.")) {
            final String name = token.substring(token.indexOf('.') + 1);
            if (accept("(")) {
                // A query that takes arguments reads as the query itself: the arguments are worked out and let go.
                arguments();
            }
            return context -> context.readQuery(name);
        }
        if (isVariable(token)) {
            return new VariableRead(variableName(token));
        }
        if (token.startsWith("math.")) {
            return function(token.substring("math.".length()));
        }
        throw new ModelFormatException("the expression \"" + this.source + "\" has a word it cannot read: " + token);
    }

    private ModelExpression function(final String name) {
        if (name.equals("pi")) {
            return ModelExpression.constant(Math.PI);
        }
        expect("(");
        final ModelExpression[] args = arguments();
        return switch (name) {
            case "abs" -> one(args, Math::abs);
            case "sin" -> one(args, degrees -> Math.sin(Math.toRadians(degrees)));
            case "cos" -> one(args, degrees -> Math.cos(Math.toRadians(degrees)));
            case "asin" -> one(args, value -> Math.toDegrees(Math.asin(value)));
            case "acos" -> one(args, value -> Math.toDegrees(Math.acos(value)));
            case "atan" -> one(args, value -> Math.toDegrees(Math.atan(value)));
            case "atan2" -> two(args, (y, x) -> Math.toDegrees(Math.atan2(y, x)));
            case "ceil" -> one(args, Math::ceil);
            case "floor" -> one(args, Math::floor);
            case "round" -> one(args, value -> (double) Math.round(value));
            case "trunc" -> one(args, value -> value < 0 ? Math.ceil(value) : Math.floor(value));
            case "sqrt" -> one(args, Math::sqrt);
            case "exp" -> one(args, Math::exp);
            case "ln" -> one(args, Math::log);
            case "pow" -> two(args, Math::pow);
            case "min" -> two(args, Math::min);
            case "max" -> two(args, Math::max);
            case "mod" -> two(args, (a, b) -> b == 0.0 ? 0.0 : a % b);
            case "min_angle" -> one(args, ExpressionParser::minAngle);
            case "hermite_blend" -> one(args, t -> 3 * t * t - 2 * t * t * t);
            case "clamp" -> three(args, (value, low, high) -> Math.clamp(value, Math.min(low, high),
                    Math.max(low, high)));
            case "lerp" -> three(args, (a, b, t) -> a + (b - a) * t);
            case "lerprotate" -> three(args, (a, b, t) -> a + minAngle(b - a) * t);
            case "random" -> two(args, (low, high) -> low
                    + ThreadLocalRandom.current().nextDouble() * (high - low));
            case "random_integer" -> two(args, (low, high) -> (double) ThreadLocalRandom.current().nextLong(
                    (long) Math.min(low, high), (long) Math.max(low, high) + 1));
            case "die_roll" -> three(args, (count, low, high) -> {
                double total = 0.0;
                for (int i = 0; i < (int) count; i++) {
                    total += low + ThreadLocalRandom.current().nextDouble() * (high - low);
                }
                return total;
            });
            default -> throw new ModelFormatException("the expression \"" + this.source
                    + "\" names a function there is none of: math." + name);
        };
    }

    private ModelExpression[] arguments() {
        final List<ModelExpression> args = new ArrayList<>();
        if (!accept(")")) {
            do {
                args.add(ternary());
            } while (accept(","));
            expect(")");
        }
        return args.toArray(ModelExpression[]::new);
    }

    private ModelExpression one(final ModelExpression[] args, final DoubleUnaryOperator op) {
        final ModelExpression a = arg(args, 0);
        return context -> op.applyAsDouble(a.evaluate(context));
    }

    private ModelExpression two(final ModelExpression[] args, final DoubleBinaryOperator op) {
        final ModelExpression a = arg(args, 0);
        final ModelExpression b = arg(args, 1);
        return context -> op.applyAsDouble(a.evaluate(context), b.evaluate(context));
    }

    private ModelExpression three(final ModelExpression[] args, final Ternary op) {
        final ModelExpression a = arg(args, 0);
        final ModelExpression b = arg(args, 1);
        final ModelExpression c = arg(args, 2);
        return context -> op.apply(a.evaluate(context), b.evaluate(context), c.evaluate(context));
    }

    private ModelExpression arg(final ModelExpression[] args, final int index) {
        if (index >= args.length) {
            throw new ModelFormatException("the expression \"" + this.source
                    + "\" gives a function too few values");
        }
        return args[index];
    }

    /* Works out once an operation of two values that never change. */
    private static ModelExpression folded(final ModelExpression a, final ModelExpression b,
                                          final ModelExpression together) {
        return a.constant() && b.constant() ? ModelExpression.constant(together.evaluate(null)) : together;
    }

    private boolean accept(final String token) {
        if (this.at < this.tokens.size() && this.tokens.get(this.at).equals(token)) {
            this.at++;
            return true;
        }
        return false;
    }

    private void expect(final String token) {
        if (!accept(token)) {
            throw new ModelFormatException("the expression \"" + this.source + "\" lacks a " + token);
        }
    }

    private String next() {
        if (this.at >= this.tokens.size()) {
            throw new ModelFormatException("the expression \"" + this.source + "\" ends too soon");
        }
        return this.tokens.get(this.at++);
    }

    private String peek() {
        return this.tokens.get(this.at);
    }

    private double number(final String token) {
        try {
            return Double.parseDouble(token);
        } catch (final NumberFormatException wrong) {
            throw new ModelFormatException("the expression \"" + this.source + "\" has what is not a number: "
                    + token);
        }
    }

    private static boolean isVariable(final String token) {
        return token.startsWith("variable.") || token.startsWith("v.") || token.startsWith("temp.")
                || token.startsWith("t.") || token.startsWith("context.") || token.startsWith("c.");
    }

    private static String variableName(final String token) {
        return token.substring(token.indexOf('.') + 1);
    }

    private static double minAngle(final double degrees) {
        double angle = degrees % 360.0;
        if (angle >= 180.0) {
            angle -= 360.0;
        } else if (angle < -180.0) {
            angle += 360.0;
        }
        return angle;
    }

    /* Words, numbers and operators, with what is between them let go. */
    private static List<String> tokenize(final String source) {
        final List<String> out = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            final char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isLetter(c) || c == '_') {
                final int start = i;
                while (i < source.length() && (Character.isLetterOrDigit(source.charAt(i))
                        || source.charAt(i) == '_' || source.charAt(i) == '.')) {
                    i++;
                }
                out.add(source.substring(start, i));
            } else if (Character.isDigit(c) || c == '.' && i + 1 < source.length()
                    && Character.isDigit(source.charAt(i + 1))) {
                final int start = i;
                while (i < source.length() && (Character.isDigit(source.charAt(i)) || source.charAt(i) == '.')) {
                    i++;
                }
                out.add(source.substring(start, i));
            } else {
                final String two = i + 1 < source.length() ? source.substring(i, i + 2) : "";
                if (two.equals("&&") || two.equals("||") || two.equals("==") || two.equals("!=")
                        || two.equals("<=") || two.equals(">=") || two.equals("??")) {
                    out.add(two);
                    i += 2;
                } else {
                    out.add(String.valueOf(c));
                    i++;
                }
            }
        }
        return out;
    }

    @FunctionalInterface
    private interface Ternary {
        double apply(double a, double b, double c);
    }

    /* The reading of a variable, kept by name so that {@code ??} can ask whether it was ever set. */
    private record VariableRead(String name) implements ModelExpression {

        @Override
        public double evaluate(final ExpressionContext context) {
            return context.variable(this.name);
        }
    }
}
