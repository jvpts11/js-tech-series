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
import dev.jstech.computers.sigma.lower.TakenApart;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.vm.listing.AsmMethod;
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
            case IrStmt.At placed -> {
                this.body.at(placed.line(), placed.column());
                this.statement(placed.reduced());
            }
            case IrStmt.If branch -> this.branch(branch);
            case IrStmt.While loop -> this.whileLoop(loop);
            case IrStmt.DoWhile loop -> this.doLoop(loop);
            case IrStmt.For loop -> this.forLoop(loop);
            case IrStmt.ForEach loop -> this.forEach(loop);
            case IrStmt.Switch choice -> this.choice(choice);
            case IrStmt.Keep kept -> {
                this.values.value(kept.value(), null);
                this.body.emit(Opcode.DUP);
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
                this.body.emit(leaving.leaves() ? Opcode.LEAVE : Opcode.BR, new IOperand.Label(
                        leaving.continuing() ? this.body.continueTo() : this.body.breakTo()));
            }
            case IrStmt.Return give -> this.give(give);
            case IrStmt.Landing landing -> this.body.mark(this.body.landing(landing.key()));
            case IrStmt.Jump jump -> this.jump(jump);
            case IrStmt.Labeled labeled -> this.labeled(labeled);
            case IrStmt.Try guarded -> this.guarded(guarded);
            case null -> { }
        }
    }

    /*
     * INTERCAL's ignore and remember over the place the checker held it to: a local of this call, or one a lambda
     * keeps, which lives in the object the method shares with it; a field of an object; or a static field.
     */
    private void ignore(final IStmt.Ignore place) {
        final boolean on = !place.remember();
        final IBinding binding = this.emitter.model.bindingOf(place.place());
        if (binding instanceof IBinding.Variable variable) {
            if (this.body.kept(variable)) {
                this.body.pushClosure();
                this.body.emit(on ? Opcode.IGNORE_FLD : Opcode.REMEMBER_FLD, this.body.keptField(variable));
            } else {
                this.body.emit(on ? Opcode.IGNORE_LOC : Opcode.REMEMBER_LOC,
                        new IOperand.Slot(this.body.slot(variable)));
            }
            return;
        }
        if (!(binding instanceof IBinding.Member member)
                || !(member.member() instanceof IMemberSymbol.FieldSymbol field)) {
            return;
        }
        if (field.isStatic()) {
            this.body.emit(on ? Opcode.IGNORE_SFLD : Opcode.REMEMBER_SFLD,
                    new IOperand.Field(field.owner().qualifiedName(), field.name()));
            return;
        }
        if (place.place() instanceof IExpr.Member reached) {
            this.values.receiver(reached.target());
        } else {
            this.body.pushThis();
        }
        this.body.emit(on ? Opcode.IGNORE_FLD : Opcode.REMEMBER_FLD,
                new IOperand.Field(this.body.ownerOf(field), field.name()));
    }

    private void jump(final IrStmt.Jump jump) {
        this.body.letGo(jump.unlocks());
        this.body.emit(jump.leaves() ? Opcode.LEAVE : Opcode.BR, new IOperand.Label(this.body.landing(jump.key())));
    }

    /*
     * A labelled statement: gotos land before the jump past its region, so landing in a region switched off passes
     * it over as reaching it any other way does; and its way out, taken by a comefrom, is passed over itself while a
     * region around the comefrom is switched off.
     */
    private void labeled(final IrStmt.Labeled labeled) {
        final String name = labeled.source().label();
        this.body.mark(this.body.landing(labeled.source()));
        if (labeled.regionEnd() != null) {
            this.body.emit(Opcode.BRABST, new IOperand.RegionJump(name, this.body.landing(labeled.regionEnd())));
        }
        this.statement(labeled.statement());
        if (labeled.comeFrom() != null) {
            final String stays = this.body.label();
            for (final String region : labeled.guards()) {
                this.body.emit(Opcode.BRABST, new IOperand.RegionJump(region, stays));
            }
            this.jump(labeled.comeFrom());
            this.body.mark(stays);
        }
        if (labeled.endsHere()) {
            this.body.mark(this.body.landing(labeled.regionEnd()));
        }
    }

    /** A statement with nothing inside it left to reduce, written as it stands. */
    private void written(final IStmt statement) {
        this.body.at(statement.line(), statement.column());
        switch (statement) {
            case IStmt.LocalDecl local -> this.local(local);
            case IStmt.ExprStmt expression -> this.discard(expression.expression());
            case IStmt.Dispose dispose -> this.dispose(dispose);
            case IStmt.Abstain switched -> this.body.emit(switched.reinstate() ? Opcode.REINSTATE : Opcode.ABSTAIN,
                    new IOperand.Region(switched.label()));
            case IStmt.Ignore place -> this.ignore(place);
            case IStmt.LocalFunction function -> this.localFunction(function);
            case IStmt.Throw thrown -> this.thrown(thrown);
            case IStmt.Deconstruct taken -> {
                final TakenApart parts = this.emitter.lowered.takenApartOf(taken);
                if (parts != null) {
                    this.values.takenApart(parts);
                }
            }
            default -> { } // an empty statement, and nothing else reaches here
        }
    }

    /* A local function made: its lambda put in its variable, where the lambdas of the method keep it or in a slot. */
    private void localFunction(final IStmt.LocalFunction function) {
        final IBinding.Variable variable = this.emitter.model.declaredAt(function);
        if (variable == null) {
            return;
        }
        if (this.body.kept(variable)) {
            this.body.pushClosure();
            this.values.value(function.body(), variable.type());
            this.body.storeKept(variable);
            return;
        }
        final int place = this.body.slot(variable);
        this.values.value(function.body(), variable.type());
        this.body.emit(Opcode.STLOC, new IOperand.Slot(place));
    }

    private void local(final IStmt.LocalDecl local) {
        // A const is written into every place that reads it, so it is given no place and no instruction of its own.
        if (local.constant()) {
            return;
        }
        final IBinding.Variable variable = this.emitter.model.declaredAt(local);
        // One whose address is taken lives in the block the method made for it, which starts zeroed.
        if (variable != null && this.emitter.model.isInMemory(variable)) {
            if (local.initializer() != null) {
                this.values.copied(local.initializer(), variable.type());
                this.values.storeInMemory(variable);
            }
            return;
        }
        if (this.body.kept(variable)) {
            if (local.initializer() != null) {
                this.body.pushClosure();
                this.values.copied(local.initializer(), variable.type());
                this.body.storeKept(variable);
            } else if (variable != null && this.values.isStruct(variable.type())) {
                // The shared object does not start its fields as empty structs, so a captured struct local
                // declared without a value would stay null for the lambda that reads or writes it.
                this.body.pushClosure();
                this.emptyStruct(variable);
                this.body.storeKept(variable);
            }
            return;
        }
        final int place = this.body.slot(variable);
        if (local.initializer() == null) {
            if (variable != null && this.values.isStruct(variable.type())) {
                // A struct is never nothing: a local declared without a value starts as an empty one.
                this.emptyStruct(variable);
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
        // What a shorter shape was reduced to is what is written, and an assignment is one wherever it came from.
        final IExpr simpler = this.emitter.model.loweredOf(expression);
        if ((simpler == null ? expression : simpler) instanceof IExpr.Assign assign) {
            this.values.assign(assign, false);
            return;
        }
        this.values.value(expression, null);
        if (this.leavesAValue(expression)) {
            this.body.emit(Opcode.POP);
        }
    }

    /** Makes an empty value of the struct the variable holds, leaving it on the stack. */
    private void emptyStruct(final IBinding.Variable variable) {
        this.body.emit(Opcode.NEWOBJ, new IOperand.Constructor(variable.type().describe(), List.of()));
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
        this.length(loop);
        this.body.emit(Opcode.BGE, new IOperand.Label(end));

        this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(index));
        this.element(loop);
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

    private void length(final IrStmt.ForEach loop) {
        if (loop.overAnArray()) {
            this.body.emit(Opcode.LDLEN);
        } else {
            this.body.emit(Opcode.LDFLD, new IOperand.Field(loop.holder(), "Count"));
        }
    }

    private void element(final IrStmt.ForEach loop) {
        if (loop.overAnArray()) {
            this.body.emit(Opcode.LDELEM);
            return;
        }
        this.body.emit(Opcode.CALL,
                new IOperand.Method(loop.holder(), "Get", List.of("int"), loop.gives()));
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
            // The arm's landing, which a goto case or a goto default lands at too.
            final String start = this.body.landing(section.source());
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
        if (give.leaves()) {
            // Out of a protected stretch the answer waits in a place of its own while the finally blocks run.
            if (give.value() != null) {
                this.body.emit(Opcode.STLOC, new IOperand.Slot(this.body.answerSlot()));
            }
            this.body.emit(Opcode.LEAVE, new IOperand.Label(this.body.wayOut()));
            return;
        }
        this.body.emit(Opcode.RET);
    }

    /*
     * A try: what it protects, then each catch, which finds the exception it took on top and keeps it where a throw
     * that throws it on can find it, then the finally; every way out of the body and of a catch leaves for the end,
     * which runs the finally on the way. The catches answer what the body protects, innermost; the finally answers the
     * body and the catches together, so it runs whichever of them is left.
     */
    private void guarded(final IrStmt.Try guarded) {
        final String start = this.body.label();
        final String end = this.body.label();
        this.body.mark(start);
        this.statement(guarded.body());
        this.body.emit(Opcode.LEAVE, new IOperand.Label(end));
        String covered = this.body.label();
        final List<String> starts = new ArrayList<>();
        for (final IrStmt.Try.Catch one : guarded.catches()) {
            final String handler = this.body.label();
            starts.add(handler);
            this.body.mark(handler);
            this.caught(one);
            this.statement(one.body());
            this.body.emit(Opcode.LEAVE, new IOperand.Label(end));
        }
        if (!guarded.catches().isEmpty()) {
            this.body.mark(covered);
            for (int i = 0; i < starts.size(); i++) {
                final IrStmt.Try.Catch one = guarded.catches().get(i);
                this.body.protect(new AsmMethod.Handler(start, starts.getFirst(), one.type(), starts.get(i),
                        i + 1 < starts.size() ? starts.get(i + 1) : covered));
            }
        } else {
            covered = null;
        }
        if (guarded.cleanup() != null) {
            final String cleanup = this.body.label();
            final String through = this.body.label();
            this.body.mark(cleanup);
            this.statement(guarded.cleanup());
            this.body.emit(Opcode.ENDFINALLY);
            this.body.mark(through);
            this.body.protect(new AsmMethod.Handler(start, covered == null ? cleanup : covered, null, cleanup,
                    through));
        }
        this.body.mark(end);
    }

    /* The exception a catch took, kept where a throw that throws it on finds it, and in its local when it has one. */
    private void caught(final IrStmt.Try.Catch one) {
        final int kept = this.body.caughtSlot(one.source());
        this.body.emit(Opcode.STLOC, new IOperand.Slot(kept));
        final IBinding.Variable variable = one.variable();
        if (variable == null) {
            return;
        }
        if (this.body.kept(variable)) {
            this.body.pushClosure();
            this.body.emit(Opcode.LDLOC, new IOperand.Slot(kept));
            this.body.storeKept(variable);
        } else {
            this.body.emit(Opcode.LDLOC, new IOperand.Slot(kept));
            this.body.emit(Opcode.STLOC, new IOperand.Slot(this.body.slot(variable)));
        }
    }

    /* A throw: of an exception worked out here, or of the one the catch it stands in took, thrown on as it was. */
    private void thrown(final IStmt.Throw thrown) {
        final IStmt.Catch from = this.emitter.model.rethrowsFrom(thrown);
        if (from != null) {
            this.body.emit(Opcode.LDLOC, new IOperand.Slot(this.body.caughtSlot(from)));
            this.body.emit(Opcode.RETHROW);
            return;
        }
        this.values.value(thrown.value(), null);
        this.body.emit(Opcode.THROW);
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
        // The target names the field itself, so what goes on the stack is the object that holds it.
        this.values.receiverOf(target);
        this.body.emit(pushValue);
        this.body.emit(Opcode.STFLD, new IOperand.Field(this.body.ownerOf(field), field.name()));
    }
}
