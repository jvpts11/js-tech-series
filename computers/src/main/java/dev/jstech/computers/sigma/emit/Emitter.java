/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.lower.Lowerer;
import dev.jstech.computers.sigma.sem.BodyChecker;
import dev.jstech.computers.sigma.sem.BuiltIns;
import dev.jstech.computers.sigma.sem.Declarations;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.sigma.sem.NamedType;
import dev.jstech.computers.sigma.sem.SemanticModel;
import dev.jstech.computers.sigma.sem.TypeRules;
import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmType;
import dev.jstech.computers.vm.listing.Opcode;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a checked program into the assembly.
 *
 * <p>Everything it needs was worked out already: what each expression is, what each name stands for,
 * and which method each call resolved to. So this stage never decides anything about the language,
 * it only writes down what the earlier stages settled, which is why nothing here reports a mistake.
 *
 * <p>This part writes the shape of a program: its types, and for each one the methods it declares.
 * Filling a method in is two other writers' work, one for what the method does and one for the values
 * it works out, and both write into the same {@link MethodBody}.
 */
public final class Emitter {

    /*
     * What the stages before this one produced. The writers in this package read them and nothing
     * else does, and none of them changes while a program is being written.
     */
    final SemanticModel model;
    final TypeRules rules;
    final BuiltIns builtIns;
    final Declarations declarations;
    final DiagnosticBag diagnostics;
    /** What each method's statements came to once the shorthand was gone. */
    final Lowerer lowered;
    private final List<AsmMethod> synthesized = new ArrayList<>();
    /** What each method's lambdas keep hold of, worked out from the tree before anything is written. */
    private final Closures closures;
    private int lambdaCount;

    public Emitter(final SemanticModel model, final TypeRules rules, final BuiltIns builtIns,
                   final Declarations declarations, final DiagnosticBag diagnostics, final Lowerer lowered) {
        this.model = model;
        this.rules = rules;
        this.builtIns = builtIns;
        this.declarations = declarations;
        this.diagnostics = diagnostics;
        this.lowered = lowered;
        this.closures = new Closures();
    }

    /** Writes the whole program out. */
    public AsmProgram emit() {
        final AsmProgram program = new AsmProgram();
        if (this.model.entryPoint() != null) {
            program.setEntryPoint(this.model.entryPoint().qualifiedName(), this.model.shape());
        }
        for (final NamedType type : this.model.declaredTypes()) {
            final IDecl.ITypeDecl source = this.declarations.source(type);
            this.diagnostics.setFile(this.declarations.fileOf(type));
            switch (source) {
                case IDecl.ClassDecl declaration -> program.addType(this.emitClass(type, declaration));
                case IDecl.InterfaceDecl ignored -> program.addType(emitInterface(type));
                case IDecl.EnumDecl declaration -> program.addType(emitEnum(type, declaration));
                case IDecl.DelegateDecl declaration -> program.addType(this.emitDelegate(type, declaration));
                case null, default -> { }
            }
            for (final AsmType closure : this.closures.written()) {
                program.addType(closure);
            }
            this.closures.forget();
        }
        return program;
    }

    /** How a method's parameters are written down: each one's type, marked when it is one it fills in. */
    static List<String> writtenParameters(final IMemberSymbol.MethodSymbol method) {
        final List<String> written = new ArrayList<>();
        if (method == null) {
            return written;
        }
        for (final IMemberSymbol.ParameterSymbol parameter : method.parameters()) {
            written.add((parameter.outward() ? "out " : "") + parameter.type().describe());
        }
        return written;
    }

    /**
     * Writes the method a lambda becomes and puts it where it can be reached from, giving back its name.
     *
     * <p>With nothing of the surrounding method's own to keep, the lambda belongs to the type it was
     * written in. With something to keep, it belongs to the object holding that, so it can reach it.
     *
     * <p>Its name begins with a digit, so no name written in the source can ever be the same.
     */
    String lambdaMethod(final IExpr.Lambda lambda, final MethodBody around, final ITypeSymbol gives,
                        final List<String> written) {
        this.lambdaCount++;
        final String name = "0lambda" + this.lambdaCount;
        final Closure closure = around.closure();
        final MethodBody body = new MethodBody(this, around.owner(), gives);
        if (closure != null) {
            body.closureIsThis(closure);
        }
        body.parameters(lambda.parameters());
        if (lambda.block() != null) {
            this.statements(body).block(this.lowered.bodyOf(lambda));
        } else {
            new ExpressionWriter(this, body).value(lambda.body(), gives);
            body.emit(Opcode.RET);
        }
        final AsmMethod made = new AsmMethod(name, gives.describe(), written, false,
                body.slotCount(), body.finish());
        if (closure == null) {
            this.synthesized.add(made);
        } else {
            this.closures.typeOf(closure.type()).addMethod(made);
        }
        return name;
    }

    // types

    /** The kind a class-like type is written as: a struct and a record keep their own directive. */
    private static AsmType.Kind kindOf(final NamedType type) {
        return switch (type.kind()) {
            case STRUCT -> AsmType.Kind.STRUCT;
            case RECORD -> AsmType.Kind.RECORD;
            default -> AsmType.Kind.CLASS;
        };
    }

    private static AsmType emitInterface(final NamedType type) {
        final AsmType written = new AsmType(AsmType.Kind.INTERFACE, type.qualifiedName());
        for (final NamedType face : type.interfaces()) {
            written.addBase(face.qualifiedName());
        }
        for (final IMemberSymbol member : type.members()) {
            if (member instanceof IMemberSymbol.MethodSymbol method) {
                written.addMethod(new AsmMethod(method.name(), method.returnType().describe(),
                        writtenParameters(method), method.isStatic(), 0, null));
            }
        }
        return written;
    }

    private static AsmType emitEnum(final NamedType type, final IDecl.EnumDecl declaration) {
        final AsmType written = new AsmType(AsmType.Kind.ENUM, type.qualifiedName());
        int next = 0;
        for (final IDecl.EnumConstant constant : declaration.constants()) {
            final Integer given = constant.value() == null ? null
                    : BodyChecker.numberOf(constant.value());
            final int number = given == null ? next : given;
            written.addValue(new AsmType.Value(constant.name(), number));
            next = number + 1;
        }
        return written;
    }

    private AsmType emitClass(final NamedType type, final IDecl.ClassDecl declaration) {
        final AsmType written = new AsmType(kindOf(type), type.qualifiedName());
        if (type.base() != null) {
            written.addBase(type.base().qualifiedName());
        }
        for (final NamedType face : type.interfaces()) {
            written.addBase(face.qualifiedName());
        }
        final List<IDecl.FieldDecl> instanceStart = new ArrayList<>();
        final List<IDecl.FieldDecl> staticStart = new ArrayList<>();
        for (final IDecl.IMemberDecl member : declaration.members()) {
            switch (member) {
                case IDecl.FieldDecl field -> {
                    final ITypeSymbol held = this.declarations.resolve(field.type());
                    written.addField(new AsmType.Field(field.name(), held.describe(),
                            field.modifiers().contains(IDecl.Modifier.STATIC)));
                    // A struct field is never nothing: one declared without a value starts as an empty one.
                    final boolean starts = field.initializer() != null
                            || this.rules.named(held) instanceof NamedType named
                            && named.kind() == NamedType.Kind.STRUCT;
                    if (starts) {
                        (field.modifiers().contains(IDecl.Modifier.STATIC) ? staticStart : instanceStart)
                                .add(field);
                    }
                }
                case IDecl.PropertyDecl property -> written.addField(new AsmType.Field(property.name(),
                        this.declarations.resolve(property.type()).describe(),
                        property.modifiers().contains(IDecl.Modifier.STATIC)));
                case IDecl.EventDecl event -> written.addEvent(new AsmType.Event(event.name(),
                        this.declarations.resolve(event.type()).describe()));
                default -> { }
            }
        }
        for (final IDecl.IMemberDecl member : declaration.members()) {
            if (member instanceof IDecl.MethodDecl method && method.body() != null) {
                written.addMethod(this.emitMethod(type, method));
            } else if (member instanceof IDecl.ConstructorDecl constructor) {
                written.addMethod(this.emitConstructor(type, constructor, instanceStart));
            }
        }
        this.addSetUp(type, written, declaration, instanceStart, staticStart);
        /*
         * A lambda is a method the player did not write a name for, so it becomes one here, on the
         * type it was written inside.
         */
        for (final AsmMethod method : this.synthesized) {
            written.addMethod(method);
        }
        this.synthesized.clear();
        this.lambdaCount = 0;
        return written;
    }

    /*
     * A class with fields that start out holding something, and no constructor of its own, still has
     * to put those values there, and so does its static part: the first in a constructor that takes
     * nothing, the second in the type's set-up method. Neither can clash with a method written in the
     * source, because no name in the source starts with a dot.
     */
    private void addSetUp(final NamedType type, final AsmType written, final IDecl.ClassDecl declaration,
                          final List<IDecl.FieldDecl> instanceStart, final List<IDecl.FieldDecl> staticStart) {
        final boolean hasConstructor = declaration.members().stream()
                .anyMatch(member -> member instanceof IDecl.ConstructorDecl);
        if (!hasConstructor && !instanceStart.isEmpty()) {
            final MethodBody body = new MethodBody(this, type, ITypeSymbol.Primitive.VOID);
            this.statements(body).fieldStarts(instanceStart);
            written.addMethod(new AsmMethod(AsmMethod.CONSTRUCTOR, "void", List.of(), false,
                    body.slotCount(), body.finish()));
        }
        if (!staticStart.isEmpty()) {
            final MethodBody body = new MethodBody(this, type, ITypeSymbol.Primitive.VOID);
            this.statements(body).fieldStarts(staticStart);
            written.addMethod(new AsmMethod(AsmMethod.TYPE_SET_UP, "void", List.of(), true,
                    body.slotCount(), body.finish()));
        }
    }

    private AsmType emitDelegate(final NamedType type, final IDecl.DelegateDecl declaration) {
        final AsmType written = new AsmType(AsmType.Kind.DELEGATE, type.qualifiedName());
        written.setInvoke(new AsmMethod("Invoke", this.declarations.resolve(declaration.returnType())
                .describe(), writtenParameters(type.invoke()), false, 0, null));
        return written;
    }

    // methods

    private AsmMethod emitMethod(final NamedType type, final IDecl.MethodDecl method) {
        final ITypeSymbol returns = this.declarations.resolve(method.returnType());
        final MethodBody body = new MethodBody(this, type, returns);
        body.parameters(method.parameters());
        body.openClosure(this.closures.forMethod(type, this.lowered.capturesOf(method)));
        this.statements(body).block(this.lowered.bodyOf(method));
        return new AsmMethod(method.name(), returns.describe(), this.written(method.parameters()),
                method.modifiers().contains(IDecl.Modifier.STATIC), body.slotCount(), body.finish());
    }

    private AsmMethod emitConstructor(final NamedType type, final IDecl.ConstructorDecl constructor,
                                      final List<IDecl.FieldDecl> instanceStart) {
        final MethodBody body = new MethodBody(this, type, ITypeSymbol.Primitive.VOID);
        final ExpressionWriter values = new ExpressionWriter(this, body);
        final StatementWriter writes = new StatementWriter(this, body, values);
        body.parameters(constructor.parameters());
        /*
         * Chaining to another constructor of the same class means that one already put the starting
         * values in place, so doing it again here would undo whatever it decided.
         */
        if (constructor.chained() == null || constructor.chained().base()) {
            writes.fieldStarts(instanceStart);
        }
        if (constructor.chained() != null) {
            values.calls().chained(type, constructor.chained());
        }
        if (constructor.body() != null) {
            body.openClosure(this.closures.forMethod(type, this.lowered.capturesOf(constructor)));
            writes.block(this.lowered.bodyOf(constructor));
        }
        return new AsmMethod(AsmMethod.CONSTRUCTOR, "void", this.written(constructor.parameters()), false,
                body.slotCount(), body.finish());
    }

    /** The pair that fills a body in: what the method does, and, under that, the values it works out. */
    private StatementWriter statements(final MethodBody body) {
        return new StatementWriter(this, body, new ExpressionWriter(this, body));
    }

    private List<String> written(final List<IDecl.Parameter> parameters) {
        final List<String> names = new ArrayList<>();
        for (final IDecl.Parameter parameter : parameters) {
            names.add((parameter.outward() ? "out " : "")
                    + this.declarations.resolve(parameter.type()).describe());
        }
        return names;
    }
}
