/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.sigma.sem.NamedType;
import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Opcode;
import java.util.List;

/**
 * Writes down one thing calling another.
 *
 * <p>Three shapes of the language are the same act underneath: calling a method, making an object, and a
 * constructor handing over to another constructor. Each of them puts whatever the call belongs to on the
 * stack, then the values it takes, then names what is being called. They are together because that order
 * is the thing that has to be right, and having written it once there is one place to be right in.
 *
 * <p>What a method fills in comes back the same way, which is why putting those answers away lives here
 * too rather than with whoever asked for the call.
 */
final class CallWriter {

    private final Emitter emitter;
    private final MethodBody body;
    private final ExpressionWriter values;

    CallWriter(final Emitter emitter, final MethodBody body, final ExpressionWriter values) {
        this.emitter = emitter;
        this.body = body;
        this.values = values;
    }

    /** How a method is named where it is called: what it belongs to, its name, what it takes and gives. */
    static IOperand.Method methodRef(final IMemberSymbol.MethodSymbol method) {
        return new IOperand.Method(method.owner().qualifiedName(), method.name(),
                Emitter.writtenParameters(method), method.returnType().describe());
    }

    void call(final IExpr.Call expression) {
        final IMemberSymbol resolved = this.emitter.model.callOf(expression);
        if (!(resolved instanceof IMemberSymbol.MethodSymbol method)) {
            return;
        }
        final IExpr callee = expression.callee();
        final boolean throughDelegate = "Invoke".equals(method.name())
                && method.owner().kind() == NamedType.Kind.DELEGATE;
        if (throughDelegate) {
            this.values.value(callee, null);
        } else if (!method.isStatic()) {
            if (callee instanceof IExpr.Member member) {
                this.values.receiver(member.target());
            } else {
                this.body.pushThis();
            }
        }
        this.arguments(expression.arguments(), method);
        this.body.emit(throughDelegate ? Opcode.CALLVIRT : Opcode.CALL, methodRef(method));
        this.storeOutward(expression.arguments(), method);
    }

    /** Making an object, which is calling a constructor and being left with what it made. */
    void created(final IExpr.New expression) {
        final IMemberSymbol chosen = this.emitter.model.callOf(expression);
        final IMemberSymbol.MethodSymbol constructor = chosen instanceof IMemberSymbol.MethodSymbol method
                ? method : null;
        this.arguments(expression.arguments(), constructor);
        final ITypeSymbol type = this.emitter.model.typeOf(expression);
        this.body.emit(Opcode.NEWOBJ, new IOperand.Constructor(type == null ? "object" : type.describe(),
                constructor == null ? List.of() : Emitter.writtenParameters(constructor)));
    }

    /** A constructor handing over to another one: of its own class, or of the class it is built on. */
    void chained(final NamedType type, final IDecl.ConstructorCall call) {
        final NamedType target = call.base() ? type.base() : type;
        if (target == null) {
            return;
        }
        this.body.emit(Opcode.LDTHIS);
        final IMemberSymbol.MethodSymbol chosen = constructorOf(target, call.arguments().size());
        this.arguments(call.arguments(), chosen);
        this.body.emit(Opcode.CALL, new IOperand.Method(target.qualifiedName(), AsmMethod.CONSTRUCTOR,
                chosen == null ? List.of() : Emitter.writtenParameters(chosen), "void"));
    }

    /** The values a call takes, in order, leaving out the ones it is going to fill in itself. */
    private void arguments(final List<IExpr> arguments, final IMemberSymbol.MethodSymbol method) {
        for (int i = 0; i < arguments.size(); i++) {
            final IMemberSymbol.ParameterSymbol parameter = method == null
                    || i >= method.parameters().size() ? null : method.parameters().get(i);
            if (parameter != null && parameter.outward()) {
                continue;
            }
            this.values.copied(arguments.get(i), parameter == null ? null : parameter.type());
        }
    }

    /*
     * What a method fills in comes back on the stack after its answer, the last one on top, so
     * they are put away from the last to the first.
     */
    private void storeOutward(final List<IExpr> arguments, final IMemberSymbol.MethodSymbol method) {
        for (int i = arguments.size() - 1; i >= 0; i--) {
            if (i >= method.parameters().size() || !method.parameters().get(i).outward()) {
                continue;
            }
            final IBinding binding = this.emitter.model.bindingOf(arguments.get(i));
            if (binding instanceof IBinding.Variable variable && this.body.kept(variable)) {
                final int held = this.body.hidden();
                this.body.emit(Opcode.STLOC, new IOperand.Slot(held));
                this.body.pushClosure();
                this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
                this.body.storeKept(variable);
            } else if (binding instanceof IBinding.Variable variable) {
                this.body.emit(Opcode.STLOC, new IOperand.Slot(this.body.slot(variable)));
            } else if (binding instanceof IBinding.Member member
                    && member.member() instanceof IMemberSymbol.FieldSymbol field) {
                this.body.emit(field.isStatic() ? Opcode.STSFLD : Opcode.STFLD,
                        new IOperand.Field(field.isStatic() ? field.owner().qualifiedName()
                                : this.body.ownerOf(field), field.name()));
            } else {
                this.body.emit(Opcode.POP);
            }
        }
    }

    /** The constructor of that class taking that many values, as a method the assembly can name. */
    private static IMemberSymbol.MethodSymbol constructorOf(final NamedType target, final int count) {
        for (final IMemberSymbol member : target.members()) {
            if (member instanceof IMemberSymbol.ConstructorSymbol constructor
                    && constructor.parameters().size() == count) {
                return new IMemberSymbol.MethodSymbol(target, AsmMethod.CONSTRUCTOR,
                        ITypeSymbol.Primitive.VOID, constructor.parameters(), constructor.modifiers());
            }
        }
        return null;
    }
}
