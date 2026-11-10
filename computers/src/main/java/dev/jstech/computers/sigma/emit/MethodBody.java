/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.lower.IrStmt;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.sigma.sem.NamedType;
import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Instruction;
import dev.jstech.computers.vm.listing.Opcode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The lines of one method, and the places it keeps its values in.
 *
 * <p>Two writers put lines here: one for what the method does, one for the values it works out. They
 * share this because they are writing the same method. A slot given to a name where it is declared is
 * the slot every later mention reads from; the label a loop marks is the label a jump inside it names;
 * the object a lambda shares with the method around it is reached the same way from either. Holding
 * all of that in one place is what lets the two writers be separate at all.
 */
final class MethodBody {

    private final Emitter emitter;
    private final List<Instruction> code = new ArrayList<>();
    private final Map<IBinding.Variable, Integer> places = new IdentityHashMap<>();
    private final Map<String, String> aliases = new HashMap<>();
    private final List<String> pending = new ArrayList<>();
    private final Deque<String> breaks = new ArrayDeque<>();
    private final Deque<String> continues = new ArrayDeque<>();
    /** What each enclosing lock took, innermost first, so a way out can name the same objects back. */
    private final Deque<IrStmt.Temporary> held = new ArrayDeque<>();
    /** Where each value the program never named ended up living. */
    private final Map<IrStmt.Temporary, Integer> temporaries = new IdentityHashMap<>();
    /** The label of each landing, by what it belongs to: a label, a comefrom, a switch's arm or a block's end. */
    private final Map<Object, String> landings = new IdentityHashMap<>();
    /** The protected stretches written so far, innermost first, by the labels they were made with. */
    private final List<AsmMethod.Handler> protecting = new ArrayList<>();
    /** The line of the source each instruction came from. */
    private final List<Integer> positions = new ArrayList<>();
    /** The column of the statement each instruction came from, which a build for the debugger writes down. */
    private final List<Integer> columns = new ArrayList<>();
    /** The place each catch keeps the exception it took, for a throw that throws it on. */
    private final Map<IStmt.Catch, Integer> caught = new IdentityHashMap<>();
    /** Where the address of each variable living in raw memory is kept, in the order they were first reached. */
    private final Map<IBinding.Variable, Integer> addresses = new IdentityHashMap<>();
    private final List<IBinding.Variable> inMemory = new ArrayList<>();
    private final NamedType owner;
    private final ITypeSymbol returns;
    private Closure closure;
    private int closureSlot = -1;
    private boolean closureIsThis;
    private int nextLabel;
    private int nextSlot;
    /** The line of the source the instructions being written now come from. */
    private int position;
    /** The column of the statement they come from. */
    private int column;
    /** The way out a return inside a protected stretch leaves for, made the first time one is written. */
    private String wayOut;
    /** Where such a return's answer waits while the finally blocks run, or -1 while none has. */
    private int answerSlot = -1;

    MethodBody(final Emitter emitter, final NamedType owner, final ITypeSymbol returns) {
        this.emitter = emitter;
        this.owner = owner;
        this.returns = returns;
    }

    /** The type the method was written in. */
    NamedType owner() {
        return this.owner;
    }

    /** What the method answers with. */
    ITypeSymbol returns() {
        return this.returns;
    }

    /** The object this method's lambdas share, or null when they keep nothing. */
    Closure closure() {
        return this.closure;
    }

    int slotCount() {
        return this.nextSlot;
    }

    /**
     * Makes the object the lambdas of this method share, and moves into it every parameter they
     * keep. Locals they keep are written straight into it when they are declared.
     */
    void openClosure(final Closure made) {
        if (made == null) {
            return;
        }
        this.closure = made;
        this.closureSlot = this.hidden();
        this.emit(Opcode.NEWOBJ, new IOperand.Constructor(made.type(), List.of()));
        this.emit(Opcode.STLOC, new IOperand.Slot(this.closureSlot));
        if (made.holdsThis()) {
            this.pushClosure();
            this.emit(Opcode.LDTHIS);
            this.emit(Opcode.STFLD, new IOperand.Field(made.type(), Closure.OUTER));
        }
        for (final Map.Entry<IBinding.Variable, String> field : made.fields().entrySet()) {
            final Integer at = this.places.get(field.getKey());
            if (at != null) {
                this.pushClosure();
                this.emit(Opcode.LDLOC, new IOperand.Slot(at));
                this.emit(Opcode.STFLD, new IOperand.Field(made.type(), field.getValue()));
            }
        }
    }

    /** Inside a lambda, the object the method shared with it is the object the lambda belongs to. */
    void closureIsThis(final Closure made) {
        this.closure = made;
        this.closureIsThis = true;
    }

    /** Whether a variable lives in the shared object rather than in a slot of its own. */
    boolean kept(final IBinding.Variable variable) {
        return this.closure != null && this.closure.fields().containsKey(variable);
    }

    /** Puts the shared object on the stack, for a read or a write of something kept in it. */
    void pushClosure() {
        if (this.closureIsThis) {
            this.emit(Opcode.LDTHIS);
        } else {
            this.emit(Opcode.LDLOC, new IOperand.Slot(this.closureSlot));
        }
    }

    void loadKept(final IBinding.Variable variable) {
        this.pushClosure();
        this.emit(Opcode.LDFLD,
                new IOperand.Field(this.closure.type(), this.closure.fields().get(variable)));
    }

    /** Puts a value already on the stack into the shared object. The object goes on first. */
    void storeKept(final IBinding.Variable variable) {
        this.emit(Opcode.STFLD, this.keptField(variable));
    }

    /** The field of the shared object a kept variable lives in. */
    IOperand.Field keptField(final IBinding.Variable variable) {
        return new IOperand.Field(this.closure.type(), this.closure.fields().get(variable));
    }

    /*
     * Inside a lambda the object the method belonged to is a field of the shared object, because
     * the lambda itself belongs to that shared object and not to the type the method was in.
     */
    void pushThis() {
        this.emit(Opcode.LDTHIS);
        if (this.closureIsThis) {
            this.emit(Opcode.LDFLD, new IOperand.Field(this.closure.type(), Closure.OUTER));
        }
    }

    void parameters(final List<IDecl.Parameter> parameters) {
        for (final IDecl.Parameter parameter : parameters) {
            this.slot(this.emitter.model.declaredAt(parameter));
        }
    }

    /** The name a member is written under here: left out when it belongs to the type being written. */
    String ownerOf(final IMemberSymbol member) {
        return member.owner() == this.owner ? null : member.owner().qualifiedName();
    }

    void emit(final Opcode opcode) {
        this.add(Instruction.of(opcode));
    }

    void emit(final Opcode opcode, final IOperand operand) {
        this.add(Instruction.of(opcode, operand));
    }

    String label() {
        this.nextLabel++;
        return "L" + this.nextLabel;
    }

    void mark(final String label) {
        this.pending.add(label);
    }

    int slot(final IBinding.Variable variable) {
        if (variable == null) {
            return this.nextSlot++;
        }
        final Integer known = this.places.get(variable);
        if (known != null) {
            return known;
        }
        final int given = this.nextSlot++;
        this.places.put(variable, given);
        return given;
    }

    /** A place with no name, for what a lowered loop needs to hold on to. */
    int hidden() {
        return this.nextSlot++;
    }

    /**
     * Where the address of a variable living in raw memory is kept. Its block is made where the method starts, so it
     * has one address for the whole call, as a C local has; a parameter's value is copied into it there.
     */
    int addressOf(final IBinding.Variable variable) {
        final Integer known = this.addresses.get(variable);
        if (known != null) {
            return known;
        }
        final int given = this.hidden();
        this.addresses.put(variable, given);
        this.inMemory.add(variable);
        return given;
    }

    /** Where a value the program never named ends up living, worked out the first time it is asked for. */
    int placeOf(final IrStmt.Temporary temporary) {
        return this.temporaries.computeIfAbsent(temporary, one -> this.hidden());
    }

    /** Where a way out of this loop goes, and where going round it again goes. */
    void enterLoop(final String again, final String end) {
        this.continues.push(again);
        this.breaks.push(end);
    }

    void leaveLoop() {
        this.breaks.pop();
        this.continues.pop();
    }

    /** The same for a choice, which a break leaves but a continue passes straight through. */
    void enterChoice(final String end) {
        this.breaks.push(end);
    }

    void leaveChoice() {
        this.breaks.pop();
    }

    String breakTo() {
        return this.breaks.peek();
    }

    String continueTo() {
        return this.continues.peek();
    }

    /** Remembers that a lock was taken on what is kept in that place. */
    void took(final IrStmt.Temporary place) {
        this.held.push(place);
    }

    void gaveBack() {
        this.held.pop();
    }

    /** Lets go of the locks a way out of the middle of something leaves behind, nearest first. */
    void letGo(final int locks) {
        int left = locks;
        for (final IrStmt.Temporary place : this.held) {
            if (left <= 0) {
                return;
            }
            this.emit(Opcode.LDLOC, new IOperand.Slot(this.placeOf(place)));
            this.emit(Opcode.MONITOR_EXIT);
            left--;
        }
    }

    /** Says the instructions written from here on come from the statement at that line and column; 0 says nothing. */
    void at(final int line, final int column) {
        if (line > 0) {
            this.position = line;
            this.column = column;
        }
    }

    /**
     * Says the instructions written from here on come from a part of the statement at that line and column. The column
     * moves only with the line, so a debugger stops at a statement where it starts, and a statement over several lines
     * stops at each line where the part on it starts.
     */
    void within(final int line, final int column) {
        if (line > 0 && line != this.position) {
            this.position = line;
            this.column = column;
        }
    }

    /** Adds a protected stretch; the ones inside it were added first, as they were written first. */
    void protect(final AsmMethod.Handler handler) {
        this.protecting.add(handler);
    }

    /** The place a catch keeps the exception it took. */
    int caughtSlot(final IStmt.Catch one) {
        return this.caught.computeIfAbsent(one, ignored -> this.hidden());
    }

    /** Where the answer of a return inside a protected stretch waits while the finally blocks it leaves run. */
    int answerSlot() {
        if (this.answerSlot < 0) {
            this.answerSlot = this.hidden();
        }
        return this.answerSlot;
    }

    /** The way out of the method a return inside a protected stretch leaves for, written once at the method's end. */
    String wayOut() {
        if (this.wayOut == null) {
            this.wayOut = this.label();
        }
        return this.wayOut;
    }

    /** The protected stretches, innermost first, each label read through to the one its line carries. */
    List<AsmMethod.Handler> handlers() {
        final List<AsmMethod.Handler> resolved = new ArrayList<>(this.protecting.size());
        for (final AsmMethod.Handler handler : this.protecting) {
            resolved.add(new AsmMethod.Handler(this.resolve(handler.start()), this.resolve(handler.end()),
                    handler.type(), this.resolve(handler.handler()), this.resolve(handler.through())));
        }
        return resolved;
    }

    /** The line of the source each instruction came from, in order. */
    List<Integer> positions() {
        return List.copyOf(this.positions);
    }

    /** The column of the statement each instruction came from, in order. */
    List<Integer> columns() {
        return List.copyOf(this.columns);
    }

    /**
     * The variables the source named and where each lives while the method runs, for a build for the debugger: the
     * parameters first, then the locals in the order they were given places, then those that never had a place of
     * their own (kept in the object the lambdas share, or in raw memory).
     */
    List<AsmMethod.Local> locals() {
        final List<AsmMethod.Local> named = new ArrayList<>();
        final Set<IBinding.Variable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<Map.Entry<IBinding.Variable, Integer>> bySlot = new ArrayList<>(this.places.entrySet());
        bySlot.sort(Map.Entry.comparingByValue());
        for (final Map.Entry<IBinding.Variable, Integer> place : bySlot) {
            this.name(place.getKey(), place.getValue(), named, seen);
        }
        if (this.closure != null) {
            for (final IBinding.Variable kept : this.closure.fields().keySet()) {
                this.name(kept, -1, named, seen);
            }
        }
        for (final IBinding.Variable held : this.inMemory) {
            this.name(held, -1, named, seen);
        }
        return named;
    }

    List<Instruction> finish() {
        // A return out of a protected stretch comes here once its finally blocks have run, and gives its answer.
        if (this.wayOut != null) {
            this.mark(this.wayOut);
            if (this.answerSlot >= 0) {
                this.emit(Opcode.LDLOC, new IOperand.Slot(this.answerSlot));
            }
            this.emit(Opcode.RET);
        }
        if (this.code.isEmpty() || !this.pending.isEmpty()
                || this.code.getLast().opcode() != Opcode.RET) {
            this.emit(Opcode.RET);
        }
        this.makeBlocks();
        final List<Instruction> resolved = new ArrayList<>();
        for (final Instruction instruction : this.code) {
            if (instruction.operand() instanceof IOperand.Label target) {
                resolved.add(new Instruction(instruction.label(), instruction.opcode(),
                        new IOperand.Label(this.resolve(target.name())), instruction.comment()));
            } else if (instruction.operand() instanceof IOperand.RegionJump target) {
                resolved.add(new Instruction(instruction.label(), instruction.opcode(),
                        new IOperand.RegionJump(target.name(), this.resolve(target.label())), instruction.comment()));
            } else {
                resolved.add(instruction);
            }
        }
        return resolved;
    }

    /**
     * The label of the landing of {@code key}, made the first time either a jump to it or the landing itself is
     * written: a goto may be written before the label it lands at.
     */
    String landing(final Object key) {
        return this.landings.computeIfAbsent(key, ignored -> this.label());
    }

    private void add(final Instruction instruction) {
        this.emitter.uses(instruction.opcode().since());
        Instruction written = instruction;
        if (!this.pending.isEmpty()) {
            written = written.labelled(this.pending.getFirst());
            for (int i = 1; i < this.pending.size(); i++) {
                this.aliases.put(this.pending.get(i), this.pending.getFirst());
            }
            this.pending.clear();
        }
        this.code.add(written);
        this.positions.add(this.position);
        this.columns.add(this.column);
    }

    /* Writes down where one named variable lives, once, unless its name is one the compiler made up. */
    private void name(final IBinding.Variable variable, final int slot, final List<AsmMethod.Local> named,
                      final Set<IBinding.Variable> seen) {
        if (!isSourceName(variable.name()) || !seen.add(variable)) {
            return;
        }
        final String type = variable.type().describe();
        /*
         * Inside a lambda, what it kept of the method around it is no parameter of its own, though it was one of that
         * method's; only the method's own parameters are shown with a call.
         */
        final boolean parameter = variable.parameter() && !(this.closureIsThis && this.kept(variable));
        if (this.addresses.containsKey(variable)) {
            named.add(new AsmMethod.Local(parameter, AsmMethod.Where.MEMORY, this.addresses.get(variable), "",
                    variable.name(), type));
        } else if (this.kept(variable)) {
            named.add(new AsmMethod.Local(parameter, AsmMethod.Where.KEPT, this.closureIsThis ? -1 : this.closureSlot,
                    this.closure.fields().get(variable), variable.name(), type));
        } else if (slot >= 0) {
            named.add(new AsmMethod.Local(parameter, AsmMethod.Where.SLOT, slot, "", variable.name(), type));
        }
    }

    /* Whether a name is one a source can hold: a letter or an underscore, then letters, digits and underscores. */
    private static boolean isSourceName(final String name) {
        if (name == null || name.isEmpty() || !(Character.isLetter(name.charAt(0)) || name.charAt(0) == '_')) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isLetterOrDigit(name.charAt(i)) && name.charAt(i) != '_') {
                return false;
            }
        }
        return true;
    }

    /*
     * The blocks of the variables living in raw memory, made before anything else the method does, each written at
     * the start once the whole method is known: one block for the whole call, freed when it returns. A parameter's
     * value is copied in, since a caller handed it in a slot.
     */
    private void makeBlocks() {
        if (this.inMemory.isEmpty()) {
            return;
        }
        final List<Instruction> made = new ArrayList<>();
        for (final IBinding.Variable variable : this.inMemory) {
            final int address = this.addresses.get(variable);
            made.add(Instruction.of(Opcode.LDC_I8,
                    new IOperand.I8(this.emitter.declarations.layout().sizeOf(variable.type()))));
            made.add(Instruction.of(Opcode.LOCALLOC));
            made.add(Instruction.of(Opcode.STLOC, new IOperand.Slot(address)));
            if (variable.parameter()) {
                made.add(Instruction.of(Opcode.LDLOC, new IOperand.Slot(address)));
                made.add(Instruction.of(Opcode.LDLOC, new IOperand.Slot(this.slot(variable))));
                this.emitter.raw.write(variable.type(), this, made::add);
            }
        }
        this.emitter.uses(Opcode.LOCALLOC.since());
        this.code.addAll(0, made);
        this.positions.addAll(0, Collections.nCopies(made.size(), 0));
        this.columns.addAll(0, Collections.nCopies(made.size(), 0));
    }

    /** Writes an instruction already made, as {@link RawAccess} hands them over. */
    void emit(final Instruction instruction) {
        this.add(instruction);
    }

    // Two labels can land on the same line, and a line carries one, so the rest point at it.
    private String resolve(final String label) {
        String at = label;
        while (this.aliases.containsKey(at)) {
            at = this.aliases.get(at);
        }
        return at;
    }
}
