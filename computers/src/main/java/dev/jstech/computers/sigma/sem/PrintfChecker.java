/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.lex.TokenKind;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Rules on a call of {@code printf} or {@code sprintf}: the format written out, a value for every hole, each of
 * the kind its hole takes.
 *
 * <p>Both are among the calls written with no type in front of them, the way the languages of those machines
 * wrote theirs, and both languages have them, since whatever the smaller one takes the full one takes too.
 * Nothing new runs for either: a format is read here, while the program is compiled, and what is left is the
 * pieces joined together, handed to the console by printf and given back by sprintf, exactly what adding them up
 * by hand would have come to.
 */
@TextHolder
final class PrintfChecker {

    private final BodyScope scope;
    private final ExpressionChecker expressions;

    /* How many holes a format has and how many values a call gives, as the message counts them. */
    private static final TextKey ONE_HOLE = TextKey.of("jsc.sigma.printf_checker.one_hole", "1 hole");
    private static final TextKey HOLES = TextKey.of("jsc.sigma.printf_checker.holes", "%s holes");
    private static final TextKey ONE_VALUE = TextKey.of("jsc.sigma.printf_checker.one_value", "1 value");
    private static final TextKey VALUES = TextKey.of("jsc.sigma.printf_checker.values", "%s values");

    PrintfChecker(final BodyScope scope, final ExpressionChecker expressions) {
        this.scope = scope;
        this.expressions = expressions;
    }

    /**
     * Checks the call of {@code name} and records what it comes to. One that prints gives nothing back, so it is a
     * statement and not a value; one that does not is the text, and nothing is called for it.
     */
    ITypeSymbol check(final IExpr.Call call, final String name, final boolean prints) {
        final ITypeSymbol gives = prints ? ITypeSymbol.Primitive.VOID : this.scope.builtIns().stringType();
        final List<IExpr> values = call.arguments().subList(Math.min(1, call.arguments().size()),
                call.arguments().size());
        final List<ITypeSymbol> kinds = new ArrayList<>(values.size());
        for (final IExpr value : values) {
            kinds.add(this.expressions.check(value, null));
        }
        final IExpr first = call.arguments().isEmpty() ? null : call.arguments().getFirst();
        if (!(first instanceof IExpr.Literal written) || written.kind() != TokenKind.STRING_LITERAL) {
            if (first != null) {
                this.expressions.check(first, null);
            }
            this.scope.report(call.line(), call.column(), SigmaError.PRINTF_FORMAT_NOT_WRITTEN_OUT, name);
            return gives;
        }
        this.scope.model().setType(written, this.scope.builtIns().stringType());
        final PrintfFormat.Read format = PrintfFormat.read(String.valueOf(written.value()));
        if (format.problem() != null) {
            this.scope.report(written.line(), written.column(), SigmaError.PRINTF_BAD_FORMAT, name,
                    format.problem());
            return gives;
        }
        if (format.holes() != values.size()) {
            this.scope.report(call.line(), call.column(), SigmaError.PRINTF_WRONG_COUNT, name,
                    counted(format.holes(), ONE_HOLE, HOLES), counted(values.size(), ONE_VALUE, VALUES));
            return gives;
        }
        final List<Object> pieces = new ArrayList<>(format.pieces().size());
        int next = 0;
        for (final Object piece : format.pieces()) {
            if (!(piece instanceof PrintfFormat.Hole hole)) {
                pieces.add(piece);
                continue;
            }
            final IExpr value = values.get(next);
            final ITypeSymbol kind = kinds.get(next++);
            if (!this.scope.rules().isError(kind) && !this.takes(hole.wants(), kind)) {
                this.scope.report(value.line(), value.column(), SigmaError.PRINTF_WRONG_VALUE, name,
                        hole.letter(), hole.wants().words(), kind.describe());
            }
            pieces.add(value);
        }
        this.scope.model().setFormatted(call, pieces);
        if (prints) {
            this.scope.model().setCall(call, this.print());
        }
        return gives;
    }

    /** Whether a hole of that kind takes a value of that type. A whole number does for a fraction, as it did. */
    private boolean takes(final PrintfFormat.Wants wants, final ITypeSymbol kind) {
        return switch (wants) {
            case WHOLE_NUMBER -> this.scope.rules().isIntegral(kind) && kind != ITypeSymbol.Primitive.CHAR;
            case FRACTION -> this.scope.rules().isNumeric(kind) && kind != ITypeSymbol.Primitive.CHAR;
            case TEXT -> kind == this.scope.builtIns().stringType();
            case CHARACTER -> kind == ITypeSymbol.Primitive.CHAR;
        };
    }

    /** The console's own call that prints text, which is what every printf comes down to. */
    private IMemberSymbol print() {
        final NamedType console = this.scope.builtIns().type("Console", 0);
        for (final IMemberSymbol member : BodyScope.lookup(console, "Print")) {
            if (member instanceof IMemberSymbol.MethodSymbol method && method.parameters().size() == 1) {
                return method;
            }
        }
        throw new IllegalStateException("the console has no Print to print through");
    }

    private static Text counted(final int count, final TextKey one, final TextKey many) {
        return count == 1 ? one.text() : many.with(count);
    }
}
