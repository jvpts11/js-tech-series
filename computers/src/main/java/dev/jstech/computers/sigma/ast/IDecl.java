/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.ast;

import java.util.List;
import java.util.Set;

/**
 * Everything a program declares: the types at the top of a file and the members inside them.
 *
 * <p>The language requires all code to live in a class, so a file is a list of type declarations and
 * nothing else. A method with no body is recorded rather than rejected here, because whether that is
 * allowed depends on where it sits: it is how an interface declares one, and a mistake in a class.
 */
public sealed interface IDecl extends INode {

    /** The method an indexer reads with, as C# names it. */
    String INDEXER_GET = "get_Item";
    /** The method an indexer writes with, handed the value last, as C# names it. */
    String INDEXER_SET = "set_Item";

    /** The access and storage words a declaration may carry. */
    enum Modifier {
        PUBLIC("public"),
        PRIVATE("private"),
        PROTECTED("protected"),
        INTERNAL("internal"),
        STATIC("static"),
        READONLY("readonly"),
        VIRTUAL("virtual"),
        OVERRIDE("override"),
        ABSTRACT("abstract"),
        /** A value known when compiling, written into every place that reads it; a field with it is static. */
        CONST("const"),
        /** A class nothing stands on, or an override that nothing replaces further down. */
        SEALED("sealed"),
        /** A method that gives an operator a meaning on the type that declares it: what operator is written as. */
        OPERATOR("operator"),
        /** One of the two methods an indexer is written as, the one that reads and the one that writes. */
        INDEXER("this"),
        /** A method whose body, and whose parameters and answer, may hold pointers. */
        UNSAFE("unsafe");

        private final String text;

        Modifier(final String text) {
            this.text = text;
        }

        /** How the modifier is written. */
        public String text() {
            return this.text;
        }
    }

    /** What the declaration is called. */
    String name();

    /** The words it was declared with. */
    Set<Modifier> modifiers();

    /** A type declared at the top of a file. */
    sealed interface ITypeDecl extends IDecl {
    }

    /** A declaration inside a type. */
    sealed interface IMemberDecl extends IDecl {
    }

    /**
     * One parameter of a method, a constructor, a delegate or a lambda.
     *
     * <p>An outward parameter is one the method fills in rather than reads, which is how a method
     * gives back an answer and a value at the same time. One written with {@code params} is the last, an array a
     * call may hand over as the values one by one. One written with {@code this} is the first of an extension method,
     * the value the method is called on as if it were its own.
     */
    record Parameter(boolean outward, TypeRef type, String name, boolean many, boolean receiver, int line,
                     int column) implements INode {

        /** A parameter that takes one value. */
        public Parameter(final boolean outward, final TypeRef type, final String name, final int line,
                         final int column) {
            this(outward, type, name, false, false, line, column);
        }
    }

    /**
     * A class, with the base type and interfaces it was written with.
     *
     * <p>A struct and a record are written here too, told apart by their flavour: a struct is a value,
     * copied whenever it is stored or handed over, and stands on no class; a record is a class whose
     * components the parser has already turned into readonly fields, a constructor, and the members
     * that compare and print it by value.
     */
    record ClassDecl(Set<Modifier> modifiers, String name, List<TypeRef> bases, List<IMemberDecl> members,
                     Flavour flavour, int line, int column) implements ITypeDecl {

        /** Which of the three class-like declarations this is. */
        public enum Flavour {
            CLASS,
            STRUCT,
            RECORD
        }
    }

    /** A type declared inside another, which is named through it: {@code Outer.Inner}. */
    record TypeMember(ITypeDecl type, int line, int column) implements IMemberDecl {

        @Override
        public String name() {
            return this.type.name();
        }

        @Override
        public Set<Modifier> modifiers() {
            return this.type.modifiers();
        }
    }

    /** An interface and the methods it requires. */
    record InterfaceDecl(Set<Modifier> modifiers, String name, List<TypeRef> bases, List<MethodDecl> methods,
                         int line, int column) implements ITypeDecl {
    }

    /** One name in an enum; {@code value} is null when the number was left to follow the previous one. */
    record EnumConstant(String name, IExpr value, int line, int column) implements INode {
    }

    /** An enum, backed by int. */
    record EnumDecl(Set<Modifier> modifiers, String name, List<EnumConstant> constants, int line, int column)
            implements ITypeDecl {
    }

    /** A delegate type: the shape of a method that can be handed around. */
    record DelegateDecl(Set<Modifier> modifiers, TypeRef returnType, String name, List<Parameter> parameters,
                        int line, int column) implements ITypeDecl {
    }

    /** A field; {@code initializer} is null when it was declared without one. */
    record FieldDecl(Set<Modifier> modifiers, TypeRef type, String name, IExpr initializer, int line, int column)
            implements IMemberDecl {
    }

    /** A method; {@code body} is null when it was declared without one. */
    record MethodDecl(Set<Modifier> modifiers, TypeRef returnType, String name, List<Parameter> parameters,
                      IStmt.Block body, int line, int column) implements IMemberDecl {
    }

    /** The call of another constructor that a constructor may start with. */
    record ConstructorCall(boolean base, List<IExpr> arguments, int line, int column) implements INode {
    }

    /** A constructor; {@code chained} is null when it does not start by calling another one. */
    record ConstructorDecl(Set<Modifier> modifiers, String name, List<Parameter> parameters,
                           ConstructorCall chained, IStmt.Block body, int line, int column) implements IMemberDecl {
    }

    /** One half of a property, with the access it was given if it differs from the property's. */
    record Accessor(Set<Modifier> modifiers, int line, int column) implements INode {
    }

    /**
     * A property in the short form, either accessor of which may be absent, or written with an arrow,
     * {@code string Name => "Lantern OS";}: read only, its {@code value} one the compiler knows, so reading it once
     * when the object is made is reading it every time.
     */
    record PropertyDecl(Set<Modifier> modifiers, TypeRef type, String name, Accessor getter, Accessor setter,
                        IExpr value, int line, int column) implements IMemberDecl {

        /** A property in the short form, with no arrow. */
        public PropertyDecl(final Set<Modifier> modifiers, final TypeRef type, final String name,
                            final Accessor getter, final Accessor setter, final int line, final int column) {
            this(modifiers, type, name, getter, setter, null, line, column);
        }

        /** Whether it was written with an arrow and its value, rather than with get and set. */
        public boolean arrow() {
            return this.value != null;
        }
    }

    /** An event, whose type is a delegate. */
    record EventDecl(Set<Modifier> modifiers, TypeRef type, String name, int line, int column)
            implements IMemberDecl {
    }
}
