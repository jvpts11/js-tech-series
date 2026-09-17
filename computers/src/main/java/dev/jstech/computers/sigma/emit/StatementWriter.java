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
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.lower.IrStmt;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Opcode;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes down what a method does: the lines for each statement of the reduced program.
 *
 * <p>Every decision about the language has been made before this. How many locks a way out lets go
 * of, what a walk over a collection asks that collection for, whether each turn of it takes a copy:
 * all of that was settled while the program was reduced. What is left here is where things go on the
 * stack and which label a jump names.
 *
 * <p>The values themselves are somebody else's: a statement says what happens, and reaches for the
 * writer of values wherever it needs one worked out.
 */
final class StatementWriter {

    private final Emitter emitter;
    private final MethodBody body;
    private final ExpressionWriter values;

    StatementWriter(final Emitter emitter, final MethodBody body, final ExpressionWriter values) {
        this.emitter = emitter;
        this.body = body;
        this.values = values;
    }

    void block(final IrStmt block) {
        for (final IrStmt statement : ((IrStmt.Block) block).statements()) {
            this.statement(statement);
        }
    }

    /**
     * Puts the starting values of fields in place, for a constructor or for a type's set-up.
     *
     * <p>A struct field is never nothing, so one declared without a value starts as an empty one
     * rather than as a missing reference.
     */
    void fieldStarts(final List<IDecl.FieldDecl> fields) {
        for (final IDecl.FieldDecl field : fields) {
            final ITypeSymbol type = this.emitter.declarations.resolve(field.type());
            final boolean fieldIsStatic = field.modifiers().contains(IDecl.Modifier.STATIC);
            if (!fieldIsStatic) {
                this.body.emit(Opcode.LDTHIS);
            }
            if (field.initializer() == null) {
                this.body.emit(Opcode.NEWOBJ, new IOperand.Constructor(type.describe(), List.of()));
            } else {
                this.values.copied(field.initializer(), type);
            }
            this.body.emit(fieldIsStatic ? Opcode.STSFLD : Opcode.STFLD,
                    new IOperand.Field(fieldIsStatic ? this.body.owner().qualifiedName() : null,
                            field.name()));
        }
    }

    private void statement(final IrStmt statement) {
        switch (statement) {
            case IrStmt.Block inner -> this.block(inner);
            case IrStmt.Source written -> this.written(written.written());
            case IrStmt.If branch -> this.branch(branch);
            case IrStmt.While loop -> this.whileLoop(loop);
            case IrStmt.DoWhile loop -> this.doLoop(loop);
            case IrStmt.For loop -> this.forLoop(loop);
            case IrStmt.ForEach loop -> this.forEach(loop);
            case IrStmt.Switch choice -> this.choice(choice);
            case IrStmt.Keep kept -> {
                this.values.value(kept.value(), null);
                if (kept.leaves()) {
                    this.body.emit(Opcode.DUP);
                }
                this.body.emit(Opcode.STLOC, new IOperand.Slot(this.body.placeOf(kept.place())));
            }
            case IrStmt.MonitorEnter taken -> {
                this.body.emit(Opcode.MONITOR_ENTER);
                this.body.took(taken.place());
            }
            case IrStmt.MonitorExit given -> {
                this.body.emit(Opcode.LDLOC, new IOperand.Slot(this.body.placeOf(given.place())));
                this.body.emit(Opcode.MONITOR_EXIT);
                this.body.gaveBack();
            }
            case IrStmt.Break leaving -> {
                this.body.letGo(leaving.unlocks());
                this.body.emit(Opcode.BR, new IOperand.Label(
                        leaving.continuing() ? this.body.continueTo() : this.body.breakTo()));
            }
            case IrStmt.Return give -> this.give(give);
            case null -> { }
        }
    }

    /** A statement with nothing inside it left to reduce, written as it stands. */
    private void written(final IStmt statement) {
        switch (statement) {
            case IStmt.LocalDecl local -> this.local(local);
            case IStmt.ExprStmt expression -> this.discard(expression.expression());
            case IStmt.Dispose dispose -> this.dispose(dispose);
            default -> { } // an empty statement, and nothing else reaches here
        }
    }

    private void local(final IStmt.LocalDecl local) {
        final IBinding.Variable variable = this.emitter.model.declaredAt(local);
        if (this.body.kept(variable)) {
            if (local.initializer() != null) {
                this.body.pushClosure();
                this.values.copied(local.initializer(), variable.type());
                this.body.storeKept(variable);
            }
            return;
        }
        final int place = this.body.slot(variable);
        if (local.initializer() == null) {
            if (variable != null && this.values.isStruct(variable.type())) {
                // A struct is never nothing: a local declared without a value starts as an empty one.
                this.body.emit(Opcode.NEWOBJ,
                        new IOperand.Constructor(variable.type().describe(), List.of()));
                this.body.emit(Opcode.STLOC, new IOperand.Slot(place));
            }
            return;
        }
        this.values.copied(local.initializer(), variable == null ? null : variable.type());
        this.body.emit(Opcode.STLOC, new IOperand.Slot(place));
    }

    /*
     * An expression written as a statement is there for what it does, so whatever it leaves
     * behind is thrown away. An assignment is told beforehand, so it never puts it there at all.
     */
    private void discard(final IExpr expression) {
        if (expression instanceof IExpr.Assign assign) {
            this.values.assign(assign, false);
            return;
        }
        this.values.value(expression, null);
        if (this.leavesAValue(expression)) {
            this.body.emit(Opcode.POP);
        }
    }

    private boolean leavesAValue(final IExpr expression) {
        final ITypeSymbol type = this.emitter.model.typeOf(expression);
        return type != null && type != ITypeSymbol.Primitive.VOID
                && type != ITypeSymbol.Special.ERROR;
    }

    private void branch(final IrStmt.If statement) {
        final String otherwise = this.body.label();
        this.values.value(statement.condition(), ITypeSymbol.Primitive.BOOL);
        this.body.emit(Opcode.BRFALSE, new IOperand.Label(otherwise));
        this.statement(statement.then());
        if (statement.otherwise() == null) {
            this.body.mark(otherwise);
            return;
        }
        final String end = this.body.label();
        this.body.emit(Opcode.BR, new IOperand.Label(end));
        this.body.mark(otherwise);
        this.statement(statement.otherwise());
        this.body.mark(end);
    }

    private void whileLoop(final IrStmt.While loop) {
        final String top = this.body.label();
        final String end = this.body.label();
        this.body.mark(top);
        this.values.value(loop.condition(), ITypeSymbol.Primitive.BOOL);
        this.body.emit(Opcode.BRFALSE, new IOperand.Label(end));
        this.inLoop(top, end, loop.body());
        this.body.emit(Opcode.BR, new IOperand.Label(top));
        this.body.mark(end);
    }

    private void doLoop(final IrStmt.DoWhile loop) {
        final String top = this.body.label();
        final String again = this.body.label();
        final String end = this.body.label();
        this.body.mark(top);
        this.inLoop(again, end, loop.body());
        this.body.mark(again);
        this.values.value(loop.condition(), ITypeSymbol.Primitive.BOOL);
        this.body.emit(Opcode.BRTRUE, new IOperand.Label(top));
        this.body.mark(end);
    }

    private void forLoop(final IrStmt.For loop) {
        for (final IrStmt initializer : loop.initializers()) {
            this.statement(initializer);
        }
        final String top = this.body.label();
        final String again = this.body.label();
        final String end = this.body.label();
        this.body.mark(top);
        if (loop.condition() != null) {
            this.values.value(loop.condition(), ITypeSymbol.Primitive.BOOL);
            this.body.emit(Opcode.BRFALSE, new IOperand.Label(end));
        }
        this.inLoop(again, end, loop.body());
        this.body.mark(again);
        for (final IExpr update : loop.updates()) {
            this.discard(update);
        }
        this.body.emit(Opcode.BR, new IOperand.Label(top));
        this.body.mark(end);
    }

    /*
     * A foreach is a counted loop over the thing it walks, which is why the assembly has no
     * instruction of its own for it.
     */
    private void forEach(final IrStmt.ForEach loop) {
        final ITypeSymbol source = loop.kind();
        final int held = this.body.hidden();
        final int index = this.body.hidden();
        this.values.value(loop.source(), null);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(held));
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(0));
        this.body.emit(Opcode.STLOC, new IOperand.Slot(index));

        final String top = this.body.label();
        final String again = this.body.label();
        final String end = this.body.label();
        this.body.mark(top);
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(index));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
        this.length(source);
        this.body.emit(Opcode.BGE, new IOperand.Label(end));

        this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(index));
        this.element(source);
        if (loop.copies()) {
            // the loop's own copy: changing it changes nothing in the collection
            this.body.emit(Opcode.COPY);
        }
        this.body.emit(Opcode.STLOC, new IOperand.Slot(this.body.slot(loop.walker())));

        this.inLoop(again, end, loop.body());
        this.body.mark(again);
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(index));
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(1));
        this.body.emit(Opcode.ADD);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(index));
        this.body.emit(Opcode.BR, new IOperand.Label(top));
        this.body.mark(end);
    }

    private void length(final ITypeSymbol source) {
        if (source instanceof ITypeSymbol.ArrayType) {
            this.body.emit(Opcode.LDLEN);
        } else {
            this.body.emit(Opcode.LDFLD,
                    new IOperand.Field(this.emitter.builtIns.listType().name(), "Count"));
        }
    }

    private void element(final ITypeSymbol source) {
        if (source instanceof ITypeSymbol.ArrayType) {
            this.body.emit(Opcode.LDELEM);
            return;
        }
        final ITypeSymbol held = this.emitter.rules.elementOf(source);
        this.body.emit(Opcode.CALL, new IOperand.Method(this.emitter.builtIns.listType().name(), "Get",
                List.of("int"), held == null ? "object" : held.describe()));
    }

    private void inLoop(final String again, final String end, final IrStmt body) {
        this.body.enterLoop(again, end);
        this.statement(body);
        this.body.leaveLoop();
    }

    /*
     * Every label is tested first and the sections follow, so a section is entered only by being
     * chosen and a run of labels can share the lines under them.
     */
    private void choice(final IrStmt.Switch choice) {
        final ITypeSymbol type = this.emitter.model.typeOf(choice.value());
        final int held = this.body.hidden();
        this.values.value(choice.value(), null);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(held));

        final String end = this.body.label();
        final List<String> starts = new ArrayList<>();
        String fallback = end;
        for (final IrStmt.Switch.Section section : choice.sections()) {
            final String start = this.body.label();
            starts.add(start);
            if (section.fallback()) {
                fallback = start;
            }
            for (final IExpr label : section.labels()) {
                this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
                this.values.value(label, type);
                this.body.emit(Opcode.BEQ, new IOperand.Label(start));
            }
        }
        this.body.emit(Opcode.BR, new IOperand.Label(fallback));

        this.body.enterChoice(end);
        for (int i = 0; i < choice.sections().size(); i++) {
            this.body.mark(starts.get(i));
            for (final IrStmt statement : choice.sections().get(i).statements()) {
                this.statement(statement);
            }
        }
        this.body.leaveChoice();
        this.body.mark(end);
    }

    private void give(final IrStmt.Return give) {
        if (give.value() != null) {
            this.values.copied(give.value(), this.body.returns());
        }
        // The answer sits under the objects let go of, so it is still on top when the method leaves.
        this.body.letGo(give.unlocks());
        this.body.emit(Opcode.RET);
    }

    // Freeing an object leaves the place that held it empty, so the reference is cleared as well.
    private void dispose(final IStmt.Dispose statement) {
        this.values.value(statement.target(), null);
        this.body.emit(Opcode.DISPOSE);
        final IBinding binding = this.emitter.model.bindingOf(statement.target());
        if (binding instanceof IBinding.Variable variable && this.body.kept(variable)) {
            this.body.pushClosure();
            this.body.emit(Opcode.LDNULL);
            this.body.storeKept(variable);
        } else if (binding instanceof IBinding.Variable variable) {
            this.body.emit(Opcode.LDNULL);
            this.body.emit(Opcode.STLOC, new IOperand.Slot(this.body.slot(variable)));
        } else if (binding instanceof IBinding.Member member
                && member.member() instanceof IMemberSymbol.FieldSymbol field) {
            this.storeField(statement.target(), field, Opcode.LDNULL);
        }
    }

    private void storeField(final IExpr target, final IMemberSymbol.FieldSymbol field,
                            final Opcode pushValue) {
        if (field.isStatic()) {
            this.body.emit(pushValue);
            this.body.emit(Opcode.STSFLD,
                    new IOperand.Field(field.owner().qualifiedName(), field.name()));
            return;
        }
        this.values.receiver(target);
        this.body.emit(pushValue);
        this.body.emit(Opcode.STFLD, new IOperand.Field(this.body.ownerOf(field), field.name()));
    }
}
