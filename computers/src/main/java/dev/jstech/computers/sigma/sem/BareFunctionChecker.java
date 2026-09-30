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
import dev.jstech.computers.sigma.lex.TokenKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checks a call of one of the calls written with no type in front of them, and works out the long way it stands
 * for.
 *
 * <p>Each is checked as a method of its own name, in the forms its table gives it, so a value of the wrong kind is
 * reported under the name the player wrote and not under a name they never saw. What it stands for is then the
 * library's own call or value, chosen the way a call written the long way would have chosen it, over the very
 * values the player handed the short one. That is the whole of what makes the listing the long way's, to the byte.
 */
final class BareFunctionChecker {

    private final BodyScope scope;
    private final CallChecker calls;
    /** Choosing the library's own version of the long way, which is the choice a call written that way makes. */
    private final Overloads overloads;
    private final PrintfChecker printf;

    /** How the forms of a function are declared: all of them belong to no object, like the calls they stand for. */
    private static final Set<IDecl.Modifier> OF_NO_OBJECT = Set.of(IDecl.Modifier.PUBLIC, IDecl.Modifier.STATIC);

    BareFunctionChecker(final BodyScope scope, final ExpressionChecker expressions, final CallChecker calls) {
        this.scope = scope;
        this.calls = calls;
        this.overloads = new Overloads(scope.rules());
        this.printf = new PrintfChecker(scope, expressions);
    }

    /**
     * The one of them a call names, or null when it names none of them, when the language it is written in does
     * not have it, or when the program has a variable or a member of its own under that name, which is then the
     * one being called.
     */
    BareFunctions.Function named(final IExpr.Call call) {
        if (!(call.callee() instanceof IExpr.Name name) || this.scope.scope().lookup(name.identifier()) != null) {
            return null;
        }
        final BareFunctions.Function function = BareFunctions.named(name.identifier());
        if (function == null || !function.in(this.scope.declarations().level())) {
            return null;
        }
        final NamedType around = this.scope.currentType();
        return around != null && !BodyScope.lookup(around, name.identifier()).isEmpty() ? null : function;
    }

    /** Checks a call of {@code function} and records what it stands for; the type is what the call gives back. */
    ITypeSymbol check(final IExpr.Call call, final BareFunctions.Function function) {
        this.scope.declarations().reportIfLater(function.name(), function.since(), call.line(), call.column());
        return switch (function.shape()) {
            case PRINTED -> this.printf.check(call, function.name(), true);
            case SCANNED -> this.printf.scan(call, function.name(),
                    this.scope.builtIns().type(function.owner(), 0), function.member());
            case FORMATTED -> this.printf.check(call, function.name(), false);
            case SAME_VALUES, ON_THE_FIRST, READ_OFF_THE_FIRST, FIXED_VALUE -> this.called(call, function);
        };
    }

    private ITypeSymbol called(final IExpr.Call call, final BareFunctions.Function function) {
        final NamedType owner = this.scope.builtIns().type(function.owner(), 0);
        final IMemberSymbol.MethodSymbol chosen = this.calls.callWith(this.forms(function, owner), call.arguments(),
                function.name(), call);
        if (chosen == null) {
            return ITypeSymbol.Special.ERROR;
        }
        final List<ITypeSymbol> takes = new ArrayList<>(chosen.parameters().size());
        for (final IMemberSymbol.ParameterSymbol parameter : chosen.parameters()) {
            takes.add(parameter.type());
        }
        final List<IExpr> values = call.arguments();
        switch (function.shape()) {
            case SAME_VALUES -> this.scope.model().setCall(call, this.longCall(owner, function.member(), takes, values));
            case FIXED_VALUE -> this.fixed(call, function, owner, takes);
            case ON_THE_FIRST -> this.onTheFirst(call, function, owner, takes);
            case READ_OFF_THE_FIRST -> this.readOffTheFirst(call, function, owner);
            case PRINTED, FORMATTED, SCANNED -> throw new IllegalStateException("a format is not called");
        }
        return chosen.returnType();
    }

    /**
     * {@code rand()} as {@code Random.Next(32768)} and {@code atoi(s)} as {@code Convert.ToInt(s, 0)}: the library's
     * call, handed the values and then the one value it is fixed to.
     */
    private void fixed(final IExpr.Call call, final BareFunctions.Function function, final NamedType owner,
                       final List<ITypeSymbol> takes) {
        final IExpr fixed = new IExpr.Literal(TokenKind.INT_LITERAL, function.fixed(), call.line(), call.column());
        this.scope.model().setType(fixed, ITypeSymbol.Primitive.INT);
        final List<IExpr> values = new ArrayList<>(call.arguments());
        values.add(fixed);
        final List<ITypeSymbol> types = new ArrayList<>(takes);
        types.add(ITypeSymbol.Primitive.INT);
        final IExpr.Call made = new IExpr.Call(call.callee(), List.copyOf(values), call.line(), call.column());
        final IMemberSymbol.MethodSymbol target = this.longCall(owner, function.member(), types, made.arguments());
        this.scope.model().setCall(made, target);
        this.scope.model().setType(made, target.returnType());
        this.scope.model().setLongWay(call, made);
    }

    /** {@code strstr(h, n)} as {@code h.IndexOf(n)}: the first value is what the call is made on. */
    private void onTheFirst(final IExpr.Call call, final BareFunctions.Function function, final NamedType owner,
                            final List<ITypeSymbol> takes) {
        final List<IExpr> rest = call.arguments().subList(1, call.arguments().size());
        final IExpr.Member member = new IExpr.Member(call.arguments().getFirst(), function.member(), call.line(),
                call.column());
        final IExpr.Call made = new IExpr.Call(member, List.copyOf(rest), call.line(), call.column());
        final IMemberSymbol.MethodSymbol target = this.longCall(owner, function.member(),
                takes.subList(1, takes.size()), rest);
        this.scope.model().setBinding(member, new IBinding.Member(target, target.returnType()));
        this.scope.model().setCall(made, target);
        this.scope.model().setType(made, target.returnType());
        this.scope.model().setLongWay(call, made);
    }

    /** {@code strlen(s)} as {@code s.Length}: a value read off the first value, and nothing called. */
    private void readOffTheFirst(final IExpr.Call call, final BareFunctions.Function function,
                                 final NamedType owner) {
        final List<IMemberSymbol> found = BodyScope.lookup(owner, function.member());
        if (found.isEmpty() || !(found.getFirst() instanceof IMemberSymbol.PropertySymbol value)) {
            throw new IllegalStateException("the library has no value " + owner.name() + "." + function.member());
        }
        final IExpr.Member made = new IExpr.Member(call.arguments().getFirst(), function.member(), call.line(),
                call.column());
        this.scope.model().setBinding(made, new IBinding.Member(value, value.type()));
        this.scope.model().setType(made, value.type());
        this.scope.model().setLongWay(call, made);
    }

    /**
     * The version of the library's own call that a call written the long way over values of those types would
     * have chosen. The table only names calls the library has, so finding none is the table being wrong.
     */
    private IMemberSymbol.MethodSymbol longCall(final NamedType owner, final String name,
                                                final List<ITypeSymbol> takes, final List<IExpr> values) {
        final List<IMemberSymbol.MethodSymbol> ways = new ArrayList<>();
        for (final IMemberSymbol member : BodyScope.lookup(owner, name)) {
            if (member instanceof IMemberSymbol.MethodSymbol method) {
                ways.add(method);
            }
        }
        final List<IMemberSymbol.MethodSymbol> fitting = this.overloads.best(ways, takes, values);
        if (fitting.isEmpty()) {
            throw new IllegalStateException("the library has no " + owner.name() + "." + name + " taking " + takes);
        }
        return fitting.getFirst();
    }

    /** The forms of a function as versions of one method, which is how its calls are checked and chosen between. */
    private List<IMemberSymbol.MethodSymbol> forms(final BareFunctions.Function function, final NamedType owner) {
        final List<IMemberSymbol.MethodSymbol> forms = new ArrayList<>(function.forms().size());
        for (final BareFunctions.Form form : function.forms()) {
            final List<String> types = form.types();
            final List<IMemberSymbol.ParameterSymbol> parameters = new ArrayList<>(types.size());
            for (int i = 0; i < types.size(); i++) {
                parameters.add(new IMemberSymbol.ParameterSymbol(form.nameOf(i), this.typeNamed(types.get(i)),
                        false));
            }
            forms.add(new IMemberSymbol.MethodSymbol(owner, function.name(), this.typeNamed(form.gives()),
                    parameters, OF_NO_OBJECT));
        }
        return forms;
    }

    /** The type a form writes by name: a number, a truth or nothing by its keyword, anything else by its name. */
    private ITypeSymbol typeNamed(final String name) {
        final ITypeSymbol.Primitive primitive = ITypeSymbol.Primitive.written(name);
        return primitive != null ? primitive : this.scope.builtIns().type(name, 0);
    }
}
