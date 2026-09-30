/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.Operator;
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
    private final ExpressionChecker expressions;
    private final CallChecker calls;
    /** Choosing the library's own version of the long way, which is the choice a call written that way makes. */
    private final Overloads overloads;
    private final PrintfChecker printf;

    /** How the forms of a function are declared: all of them belong to no object, like the calls they stand for. */
    private static final Set<IDecl.Modifier> OF_NO_OBJECT = Set.of(IDecl.Modifier.PUBLIC, IDecl.Modifier.STATIC);

    BareFunctionChecker(final BodyScope scope, final ExpressionChecker expressions, final CallChecker calls) {
        this.scope = scope;
        this.expressions = expressions;
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
            case COPIED_INTO, JOINED_ONTO -> this.assigned(call, function);
            case FILE_PRINTED -> this.filePrinted(call, function);
            case FILE_SCANNED -> this.fileScanned(call, function);
            case FORMATTED -> this.printf.check(call, function.name(), false);
            case SAME_VALUES, ON_THE_FIRST, ON_THE_FIRST_FIXED, ON_THE_LAST, READ_OFF_THE_FIRST, FIXED_VALUE ->
                    this.called(call, function);
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
            case ON_THE_FIRST_FIXED -> this.onTheFirstFixed(call, function, owner);
            case ON_THE_LAST -> this.onTheLast(call, function, owner, takes);
            case READ_OFF_THE_FIRST -> this.readOffTheFirst(call, function, owner);
            case PRINTED, FORMATTED, SCANNED, COPIED_INTO, JOINED_ONTO, FILE_PRINTED, FILE_SCANNED ->
                    throw new IllegalStateException("not a call of the library");
        }
        return chosen.returnType();
    }

    /**
     * {@code strcpy(out d, s)} as {@code d = s} and {@code strcat(ref d, s)} as {@code d = d + s}: in C's order, the
     * place first, handed with out when it is only written and with ref when it is read first, and the text second.
     * The long way names the same place again, so the assignment is the one a player would have written.
     */
    private ITypeSymbol assigned(final IExpr.Call call, final BareFunctions.Function function) {
        final NamedType text = this.scope.builtIns().stringType();
        final boolean joins = function.shape() == BareFunctions.Shape.JOINED_ONTO;
        final List<IExpr> arguments = call.arguments();
        if (arguments.size() != 2 || !(arguments.getFirst() instanceof IExpr.OutArgument place)
                || place.ref() != joins) {
            this.scope.report(call.line(), call.column(), SigmaError.NO_MATCHING_OVERLOAD, function.name());
            return text;
        }
        final ITypeSymbol held = joins ? this.expressions.refArgumentType(place) : this.expressions.check(place, text);
        final IExpr source = arguments.get(1);
        final ITypeSymbol given = this.expressions.check(source, text);
        if (this.scope.rules().isError(held) || this.scope.rules().isError(given)) {
            return text;
        }
        this.scope.expect(text, held, place);
        this.scope.expect(given, text, source);
        final IBinding binding = this.scope.model().bindingOf(place);
        if (binding == null) {
            return held;
        }
        final IExpr value;
        if (joins) {
            value = new IExpr.Binary(Operator.ADD, this.named(place, binding, held), source, call.line(),
                    call.column());
            this.scope.model().setType(value, text);
        } else {
            value = source;
        }
        final IExpr.Assign made = new IExpr.Assign(this.named(place, binding, held), Operator.ASSIGN, value,
                call.line(), call.column());
        this.scope.model().setType(made, held);
        this.scope.model().setLongWay(call, made);
        return held;
    }

    /**
     * {@code fprintf(f, format, ...)}: the file first, then printf's format and values, read while the program is
     * compiled; what they come to is written to the file, as {@code f.Write} of the joined text.
     */
    private ITypeSymbol filePrinted(final IExpr.Call call, final BareFunctions.Function function) {
        final NamedType file = this.scope.builtIns().type(function.owner(), 0);
        final IExpr target = this.fileHandedFirst(call, function, file);
        if (target == null) {
            return ITypeSymbol.Primitive.VOID;
        }
        final IExpr.Call text = new IExpr.Call(call.callee(),
                List.copyOf(call.arguments().subList(1, call.arguments().size())), call.line(), call.column());
        this.scope.model().setType(text, this.printf.check(text, function.name(), false));
        if (this.scope.model().formattedOf(text) != null) {
            this.onObject(call, target, function.member(), file, List.of(text),
                    List.of(this.scope.builtIns().stringType()));
        }
        return ITypeSymbol.Primitive.VOID;
    }

    /** {@code fscanf(f, format, out v)}: scanf's one value, read from the file handed first. */
    private ITypeSymbol fileScanned(final IExpr.Call call, final BareFunctions.Function function) {
        final NamedType file = this.scope.builtIns().type(function.owner(), 0);
        final IExpr source = this.fileHandedFirst(call, function, file);
        if (source == null) {
            return ITypeSymbol.Primitive.INT;
        }
        return this.printf.scan(call, call.arguments().subList(1, call.arguments().size()), source,
                function.name(), file, function.member());
    }

    /** The file a call on files is handed first, checked, or null when there is none to check. */
    private IExpr fileHandedFirst(final IExpr.Call call, final BareFunctions.Function function,
                                  final NamedType file) {
        if (call.arguments().isEmpty()) {
            this.scope.report(call.line(), call.column(), SigmaError.NO_MATCHING_OVERLOAD, function.name());
            return null;
        }
        final IExpr target = call.arguments().getFirst();
        this.scope.expect(this.expressions.check(target, file), file, target);
        return target;
    }

    /** A name for the place an argument handed over, bound and typed as the argument was. */
    private IExpr.Name named(final IExpr.OutArgument place, final IBinding binding, final ITypeSymbol type) {
        final IExpr.Name made = new IExpr.Name(place.name(), place.line(), place.column());
        this.scope.model().setBinding(made, binding);
        this.scope.model().setType(made, type);
        return made;
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

    /** {@code rewind(f)} as {@code f.Seek(0)}: a call on the first value, handed the one value it is fixed to. */
    private void onTheFirstFixed(final IExpr.Call call, final BareFunctions.Function function,
                                 final NamedType owner) {
        final IExpr fixed = new IExpr.Literal(TokenKind.INT_LITERAL, function.fixed(), call.line(), call.column());
        this.scope.model().setType(fixed, ITypeSymbol.Primitive.INT);
        this.onObject(call, call.arguments().getFirst(), function.member(), owner, List.of(fixed),
                List.of(ITypeSymbol.Primitive.INT));
    }

    /** {@code fputs(s, f)} as {@code f.Write(s)}: the last value is what the call is made on, as C puts the file. */
    private void onTheLast(final IExpr.Call call, final BareFunctions.Function function, final NamedType owner,
                           final List<ITypeSymbol> takes) {
        final int last = call.arguments().size() - 1;
        this.onObject(call, call.arguments().get(last), function.member(), owner,
                List.copyOf(call.arguments().subList(0, last)), takes.subList(0, last));
    }

    /** The long way of a call made on {@code receiver}, handed {@code values} of those types. */
    private void onObject(final IExpr.Call call, final IExpr receiver, final String name, final NamedType owner,
                          final List<IExpr> values, final List<ITypeSymbol> takes) {
        final IExpr.Member member = new IExpr.Member(receiver, name, call.line(), call.column());
        final IExpr.Call made = new IExpr.Call(member, values, call.line(), call.column());
        final IMemberSymbol.MethodSymbol target = this.longCall(owner, name, takes, values);
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
                        form.outward(i)));
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
