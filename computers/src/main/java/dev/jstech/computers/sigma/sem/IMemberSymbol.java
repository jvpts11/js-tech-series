/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.ast.IDecl;
import java.util.List;
import java.util.Set;

/**
 * Something declared inside a type: a field, a method, a constructor, a property or an event.
 *
 * <p>Each one knows the type it belongs to, so a lookup that walked up through the base classes can
 * still say where what it found was written.
 */
public sealed interface IMemberSymbol {

    /** What the member is called; a constructor is called by its type's name. */
    String name();

    /** The type that declares it. */
    NamedType owner();

    /** The words it was declared with. */
    Set<IDecl.Modifier> modifiers();

    /** Whether it belongs to the type rather than to an instance of it. */
    default boolean isStatic() {
        return this.modifiers().contains(IDecl.Modifier.STATIC);
    }

    /**
     * One parameter of a method or a constructor. An outward one is filled in by the method rather
     * than read by it; one written with params is the last, an array a call may hand over one value at a time.
     */
    record ParameterSymbol(String name, ITypeSymbol type, boolean outward, boolean many, boolean receiver) {

        /** A parameter that takes one value, read or filled in. */
        public ParameterSymbol(final String name, final ITypeSymbol type, final boolean outward) {
            this(name, type, outward, false, false);
        }

        /** A parameter that may be written with params, and is not the value an extension method is called on. */
        public ParameterSymbol(final String name, final ITypeSymbol type, final boolean outward, final boolean many) {
            this(name, type, outward, many, false);
        }

        /** A parameter the method reads. */
        public static ParameterSymbol of(final String name, final ITypeSymbol type) {
            return new ParameterSymbol(name, type, false);
        }
    }

    /** A field. */
    record FieldSymbol(NamedType owner, String name, ITypeSymbol type, Set<IDecl.Modifier> modifiers)
            implements IMemberSymbol {

        /** Whether it may only be written where it is declared or in a constructor. */
        public boolean isReadOnly() {
            return this.modifiers.contains(IDecl.Modifier.READONLY);
        }

        /** Whether it is a const: a value known when compiling, written into every place that reads it. */
        public boolean isConstant() {
            return this.modifiers.contains(IDecl.Modifier.CONST);
        }

        /** A const belongs to its type, as C#'s does, without being written static. */
        @Override
        public boolean isStatic() {
            return this.isConstant() || this.modifiers.contains(IDecl.Modifier.STATIC);
        }
    }

    /** A method, including a delegate's invoke shape. */
    record MethodSymbol(NamedType owner, String name, ITypeSymbol returnType, List<ParameterSymbol> parameters,
                        Set<IDecl.Modifier> modifiers) implements IMemberSymbol {

        public MethodSymbol {
            parameters = List.copyOf(parameters);
        }

        /** Whether the other method takes the same types in the same order, which is what makes two the same. */
        public boolean sameParameters(final MethodSymbol other) {
            if (this.parameters.size() != other.parameters.size()) {
                return false;
            }
            for (int i = 0; i < this.parameters.size(); i++) {
                if (!this.parameters.get(i).type().equals(other.parameters.get(i).type())) {
                    return false;
                }
            }
            return true;
        }

        /** Whether its last parameter is written with params, so a call may hand that array over a value at a time. */
        public boolean takesMany() {
            return !this.parameters.isEmpty() && this.parameters.getLast().many();
        }

        /** Whether it gives an operator a meaning on the type that declares it. */
        public boolean isOperator() {
            return this.modifiers.contains(IDecl.Modifier.OPERATOR);
        }

        /** Whether it is one of the two methods an indexer is written as. */
        public boolean isIndexer() {
            return this.modifiers.contains(IDecl.Modifier.INDEXER);
        }

        /** Whether it is an extension method: its first parameter is the value it is called on, written this. */
        public boolean extendsItsFirst() {
            return !this.parameters.isEmpty() && this.parameters.getFirst().receiver();
        }

        /** How a diagnostic writes the method, with the types it takes. */
        public String describe() {
            final StringBuilder text = new StringBuilder(this.name).append('(');
            for (int i = 0; i < this.parameters.size(); i++) {
                text.append(i > 0 ? ", " : "").append(this.parameters.get(i).type().describe());
            }
            return text.append(')').toString();
        }
    }

    /** A constructor. */
    record ConstructorSymbol(NamedType owner, List<ParameterSymbol> parameters, Set<IDecl.Modifier> modifiers)
            implements IMemberSymbol {

        public ConstructorSymbol {
            parameters = List.copyOf(parameters);
        }

        @Override
        public String name() {
            return this.owner.name();
        }
    }

    /** A property in the short form, with whichever halves it was given. */
    record PropertySymbol(NamedType owner, String name, ITypeSymbol type, boolean readable, boolean writable,
                          Set<IDecl.Modifier> modifiers, Set<IDecl.Modifier> setterModifiers)
            implements IMemberSymbol {
    }

    /** An event, whose type is always a delegate. */
    record EventSymbol(NamedType owner, String name, NamedType delegateType, Set<IDecl.Modifier> modifiers)
            implements IMemberSymbol {
    }
}
