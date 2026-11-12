/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.computers.sigma.ast.CompilationUnit;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.ast.TypeRef;
import dev.jstech.computers.vm.listing.Shape;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the declarations in the tree into symbols.
 *
 * <p>It runs in two passes because types refer to each other in circles: the first gives every type
 * its name, the second gives each one its base, its interfaces and its members, by which point every
 * name it could mention already exists. Only then can a member's type be resolved without caring
 * what order the player wrote their classes in.
 *
 * <p>Names are found the way a file says they may be: a type sees the types of its own namespace and
 * of the namespaces around it, the types it is nested with, and what its file brought in with using,
 * one type or a whole namespace at a time. The language's own types live in namespaces of their own
 * under {@code System} and are brought in the same way; only the roots every program is made of, the
 * text and the object, are there without asking.
 */
public final class Declarations {

    private final BuiltIns builtIns;
    private final TypeRules rules;
    private final DiagnosticBag diagnostics;
    private final SemanticModel model;
    /** Every type the program declares, by its name with its namespace in front. */
    private final Map<String, NamedType> declared = new LinkedHashMap<>();
    private final Map<NamedType, IDecl.ITypeDecl> sources = new LinkedHashMap<>();
    private final Map<NamedType, String> files = new LinkedHashMap<>();
    /** What each type can see without saying the namespace: its own, and the ones its file brought in. */
    private final Map<NamedType, Scope> scopes = new LinkedHashMap<>();
    /** The type each nested type was declared inside, whose other nested types it may name plainly. */
    private final Map<NamedType, NamedType> outers = new LinkedHashMap<>();
    /** Where each method was written, so the override pass can report on the right line. */
    private final Map<IMemberSymbol.MethodSymbol, INode> methodNodes = new LinkedHashMap<>();
    /** The structs the program's tuples are, by name, in the order they were first written, as they go out. */
    private final Map<String, NamedType> tuples = new LinkedHashMap<>();
    /** How values lie in raw memory, which decides what a pointer may point at. */
    private final Layout layout;
    /** The pointers written in declarations, whose targets are asked once every struct knows its fields. */
    private final List<Pointee> pointees = new ArrayList<>();
    /** Every pointer type read so far, where it was written, so one read twice is spoken about once. */
    private final Set<TypeRef> pointersRead = new HashSet<>();
    /** The type being filled, whose scope decides what a bare name means. */
    private NamedType current;
    /** Whether a pointer may be written where types are being resolved now. */
    private boolean unsafeHere;
    /** Whether every type has been filled, after which a pointer's target is asked as it is written. */
    private boolean filled;
    /** The language and the version of it the build is held to, which decide what of the library it may name. */
    private LanguageLevel level = LanguageLevel.SIGMA_SHARP;
    private int version = SigmaVersions.NEWEST;

    /** The namespace a file's types live in, and what the file brought in with using. */
    private record Scope(String namespace, List<CompilationUnit.Using> usings) {
        static final Scope TOP = new Scope("", List.of());
    }

    /** A pointer written in a declaration, in that file, to that type. */
    private record Pointee(TypeRef at, ITypeSymbol type, String file) {
    }

    public Declarations(final BuiltIns builtIns, final TypeRules rules, final DiagnosticBag diagnostics,
                        final SemanticModel model) {
        this.builtIns = builtIns;
        this.rules = rules;
        this.diagnostics = diagnostics;
        this.model = model;
        this.layout = new Layout(rules);
    }

    /**
     * Holds the build to {@code version} of {@code level}: a type of the library that came in a later version is
     * reported where it was named, and a program's own type of the same name is its own and is left alone.
     */
    public void holdTo(final LanguageLevel level, final int version) {
        this.level = level;
        this.version = SigmaVersions.held(version);
    }

    /**
     * Reports {@code type}, named at {@code line}, {@code column}, when it is a type of the library that came in a
     * later version than the one the build is held to.
     */
    public void reportIfNewer(final NamedType type, final int line, final int column) {
        if (type == null || this.builtIns.type(type.name(), type.typeParameters().size()) != type) {
            return;
        }
        this.reportIfLater(type.name(), SigmaVersions.sinceType(this.level, type.name()), line, column);
    }

    /**
     * Reports a method of the library, called at {@code line}, {@code column}, when it was added to its type in a
     * later version than the one the build is held to. One whose whole type came later is not reported again here:
     * the type was, where it was named.
     */
    void reportIfNewer(final IMemberSymbol.MethodSymbol method, final int line, final int column) {
        final NamedType owner = method.owner();
        if (owner == null || this.builtIns.type(owner.name(), owner.typeParameters().size()) != owner) {
            return;
        }
        final List<String> takes = new ArrayList<>(method.parameters().size());
        // A method of a generic type is named as its type declares it, Func<T, bool>, not as one use fills it in.
        for (final IMemberSymbol.ParameterSymbol parameter : declaredForm(owner, method).parameters()) {
            takes.add((parameter.outward() ? "out " : "") + parameter.type().describe());
        }
        final int since = SigmaVersions.sinceMember(this.level, owner.name(), method.name(), String.join(", ", takes));
        if (since > SigmaVersions.sinceType(this.level, owner.name())) {
            this.reportIfLater(owner.name() + "." + method.name(), since, line, column);
        }
    }

    /**
     * Reports {@code name}, written at {@code line}, {@code column}, when what it names came in {@code since}, a
     * later version than the one the build is held to.
     */
    void reportIfLater(final String name, final int since, final int line, final int column) {
        if (since > this.version) {
            this.diagnostics.error(line, column, SigmaError.NEEDS_A_LATER_VERSION, name, this.level.mark(),
                    since, this.level.mark(), this.version);
        }
    }

    /** The language the build is in. */
    LanguageLevel level() {
        return this.level;
    }

    /** First pass: every type in the program gets its name and nothing else. */
    public void declare(final List<CompilationUnit> units) {
        for (final CompilationUnit unit : units) {
            this.diagnostics.setFile(unit.file());
            for (final CompilationUnit.Declared one : unit.declared()) {
                this.declareType(one.type(), unit.file(), new Scope(one.namespace(), unit.usings()), null);
            }
        }
        /*
         * Only once every name exists can a using be read: one without a star has to name a type, and
         * naming a namespace that way is the one mistake worth pointing out, since the star is all it
         * is missing. A using that names nothing at all is left alone; the names it was meant to bring
         * in are reported where they are used.
         */
        for (final CompilationUnit unit : units) {
            this.diagnostics.setFile(unit.file());
            for (final CompilationUnit.Using using : unit.usings()) {
                if (!using.all() && this.find(using.name(), -1) == null && this.isNamespace(using.name())) {
                    this.diagnostics.error(using.line(), using.column(), SigmaError.USING_NEEDS_STAR,
                            using.name(), using.name());
                }
            }
        }
    }

    private void declareType(final IDecl.ITypeDecl declaration, final String file, final Scope scope,
                             final NamedType outer) {
        final String name = declaration.name();
        final String prefix = outer != null ? outer.qualifiedName() : scope.namespace();
        final String qualified = prefix.isEmpty() ? name : prefix + "." + name;
        if (this.declared.containsKey(qualified) || this.builtIns.qualified(qualified, -1) != null) {
            this.diagnostics.error(declaration.line(), declaration.column(),
                    SigmaError.DUPLICATE_DECLARATION, qualified);
            return;
        }
        final NamedType.Kind kind = switch (declaration) {
            case IDecl.ClassDecl made -> switch (made.flavour()) {
                case CLASS -> NamedType.Kind.CLASS;
                case STRUCT -> NamedType.Kind.STRUCT;
                case RECORD -> NamedType.Kind.RECORD;
            };
            case IDecl.InterfaceDecl ignored -> NamedType.Kind.INTERFACE;
            case IDecl.EnumDecl ignored -> NamedType.Kind.ENUM;
            case IDecl.DelegateDecl ignored -> NamedType.Kind.DELEGATE;
        };
        final NamedType type = NamedType.of(name, kind, false);
        type.setNamespace(prefix);
        // Read now, so a class that stands on one declared further down already knows it may not.
        type.setStatic(declaration.modifiers().contains(IDecl.Modifier.STATIC) && kind == NamedType.Kind.CLASS);
        type.setSealed(declaration.modifiers().contains(IDecl.Modifier.SEALED) || type.isStatic());
        this.declared.put(qualified, type);
        this.sources.put(type, declaration);
        this.files.put(type, file);
        this.scopes.put(type, scope);
        if (outer != null) {
            this.outers.put(type, outer);
        }
        this.model.addDeclared(type);
        if (declaration instanceof IDecl.ClassDecl made) {
            for (final IDecl.IMemberDecl member : made.members()) {
                if (member instanceof IDecl.TypeMember nested) {
                    this.declareType(nested.type(), file, scope, type);
                }
            }
        }
    }

    /** The type a name means where {@code from} was written, or null when it means none. */
    public NamedType lookup(final String name, final NamedType from) {
        return this.lookup(name, from, -1);
    }

    /**
     * The type a name means where {@code from} was written, or null when it means none.
     *
     * <p>A name with its namespace in front means exactly that type. A bare name is looked for among
     * the types nested with the type it was written in, then in the namespace that type sits in and in
     * each namespace enclosing that one, then in what its file brought in with using, and last among
     * the roots that belong to no namespace. A built-in type with {@code arity} arguments is preferred
     * where the name has more than one; {@code -1} asks for whichever there is.
     */
    public NamedType lookup(final String name, final NamedType from, final int arity) {
        NamedType found = this.find(name, arity);
        if (found != null) {
            return found;
        }
        for (NamedType around = from; around != null; around = this.outers.get(around)) {
            found = this.declared.get(around.qualifiedName() + "." + name);
            if (found != null) {
                return found;
            }
        }
        final Scope scope = from == null ? Scope.TOP : this.scopes.getOrDefault(from, Scope.TOP);
        String enclosing = scope.namespace();
        while (!enclosing.isEmpty()) {
            found = this.find(enclosing + "." + name, arity);
            if (found != null) {
                return found;
            }
            final int dot = enclosing.lastIndexOf('.');
            enclosing = dot >= 0 ? enclosing.substring(0, dot) : "";
        }
        for (final CompilationUnit.Using using : scope.usings()) {
            if (using.all()) {
                found = this.find(using.name() + "." + name, arity);
                if (found == null) {
                    found = this.underneath(using.name(), name, arity);
                }
            } else if (using.name().equals(name) || using.name().endsWith("." + name)) {
                found = this.find(using.name(), arity);
            }
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * The type {@code name} names in a namespace nested under {@code prefix}, for a using that opened
     * that prefix with a star.
     *
     * <p>{@code using System.*;} opens what is under System as well as System itself, which is what
     * anyone writing it means: the console lives in System.IO and a program that brought in System
     * expects to be able to name it. The language's own types are found by asking where the name lives,
     * so this costs nothing; the program's own are looked for among what it declared, which is small.
     */
    private NamedType underneath(final String prefix, final String name, final int arity) {
        final String home = this.builtIns.homeOf(name);
        if (home != null && home.startsWith(prefix + ".")) {
            final NamedType found = this.find(home + "." + name, arity);
            if (found != null) {
                return found;
            }
        }
        for (final NamedType declared : this.declared.values()) {
            if (declared.name().equals(name) && declared.namespace().startsWith(prefix + ".")) {
                return declared;
            }
        }
        return null;
    }

    /** The type known by exactly that name with its namespace in front, the program's or the language's. */
    private NamedType find(final String qualified, final int arity) {
        final NamedType own = this.declared.get(qualified);
        if (own != null) {
            return own;
        }
        final NamedType built = this.builtIns.qualified(qualified, arity);
        // Naming one of the exceptions is using them, and their types then go out with the program.
        if (built != null && BuiltIns.EXCEPTIONS.containsKey(built.name())) {
            this.model.setUsesExceptions();
        }
        return built;
    }

    /**
     * Whether {@code prefix} is the start of some namespace: {@code Tools} for {@code Tools.Counter},
     * {@code System.IO} for the console. A type with types nested inside it reads as one as well, so
     * that {@code Outer.Inner} written from outside finds its way.
     */
    public boolean isNamespace(final String prefix) {
        for (final NamedType type : this.declared.values()) {
            final String namespace = type.namespace();
            if (namespace.equals(prefix) || namespace.startsWith(prefix + ".")) {
                return true;
            }
        }
        return this.builtIns.isNamespace(prefix);
    }

    /**
     * The namespace a bare name would be found in if the file brought it in, or null when it is in
     * none: what a message about an unknown name points the player to.
     */
    public String homeOf(final String name) {
        for (final NamedType type : this.declared.values()) {
            if (type.name().equals(name) && !type.namespace().isEmpty() && !this.outers.containsKey(type)) {
                return type.namespace();
            }
        }
        return this.builtIns.homeOf(name);
    }

    /** Reports a name that means nothing here, saying where it would be found when it is somewhere. */
    public void reportUnknown(final int line, final int column, final String name) {
        final String home = this.homeOf(name);
        if (home != null) {
            this.diagnostics.error(line, column, SigmaError.NEEDS_USING, name, home, home, home, name);
        } else {
            this.diagnostics.error(line, column, SigmaError.UNKNOWN_NAME, name);
        }
    }

    /** Second pass: every type gets what it is made of. */
    public void fill() {
        for (final Map.Entry<NamedType, IDecl.ITypeDecl> entry : this.sources.entrySet()) {
            this.diagnostics.setFile(this.files.get(entry.getKey()));
            this.current = entry.getKey();
            switch (entry.getValue()) {
                case IDecl.ClassDecl declaration -> this.fillClass(entry.getKey(), declaration);
                case IDecl.InterfaceDecl declaration -> this.fillInterface(entry.getKey(), declaration);
                case IDecl.EnumDecl declaration -> this.fillEnum(entry.getKey(), declaration);
                case IDecl.DelegateDecl declaration -> this.fillDelegate(entry.getKey(), declaration);
            }
        }
        // What a pointer written in a declaration points at is asked once every struct knows its fields.
        this.filled = true;
        for (final Pointee waiting : this.pointees) {
            if (waiting.file() != null) {
                this.diagnostics.setFile(waiting.file());
            }
            this.checkPointee(waiting.at(), waiting.type());
        }
        this.pointees.clear();
    }

    /**
     * Says whether pointers may be written where types are resolved from now on: in an unsafe method or block, and
     * anywhere in Sigma.
     */
    void unsafeHere(final boolean on) {
        this.unsafeHere = on;
    }

    /** The declaration a type came from, so the body checker can walk it. */
    public IDecl.ITypeDecl source(final NamedType type) {
        return this.sources.get(type);
    }

    /** The file a type was written in, so a message about it names the right one. */
    public String fileOf(final NamedType type) {
        return this.files.get(type);
    }

    /** The type {@code type} was declared inside, or null for one at the top of its namespace. */
    public NamedType outerOf(final NamedType type) {
        return this.outers.get(type);
    }

    private void fillClass(final NamedType type, final IDecl.ClassDecl declaration) {
        type.setAbstract(declaration.modifiers().contains(IDecl.Modifier.ABSTRACT));
        this.checkClassWords(type, declaration);
        for (final TypeRef base : declaration.bases()) {
            final ITypeSymbol resolved = this.resolve(base);
            if (!(resolved instanceof NamedType named)) {
                continue;
            }
            if (type.isStatic()) {
                this.diagnostics.error(base.line(), base.column(), SigmaError.STATIC_CLASS_BASE, base.describe());
            } else if (named.kind() == NamedType.Kind.INTERFACE) {
                type.addInterface(named);
            } else if (named.kind().classLike() && type.kind() == NamedType.Kind.STRUCT) {
                this.diagnostics.error(base.line(), base.column(), SigmaError.STRUCT_NO_BASE, base.describe());
            } else if (named.kind().classLike() && type.base() == null && standsBehind(type, named)) {
                // Linking it would close a loop, which every walk up the bases would then go round for ever.
                this.diagnostics.error(base.line(), base.column(), SigmaError.CIRCULAR_BASE, type.name(),
                        named.name());
            } else if (named.kind().classLike() && type.base() == null) {
                if (named.isSealed()) {
                    this.diagnostics.error(base.line(), base.column(), SigmaError.SEALED_BASE, named.name());
                }
                type.setBase(named);
            } else {
                this.diagnostics.error(base.line(), base.column(), SigmaError.INVALID_BASE, base.describe());
            }
        }
        for (final IDecl.IMemberDecl member : declaration.members()) {
            this.fillMember(type, member);
        }
        this.checkOperatorPairs(type, declaration);
    }

    /*
     * An operator is public and static and takes as many plain values as its sign has sides, one of them of the type
     * that declares it; one more or one less takes that type and gives it back.
     */
    private void checkOperator(final NamedType type, final IMemberSymbol.MethodSymbol method,
                               final IDecl.MethodDecl written) {
        final Operator operator = Operator.ofMethod(method.name());
        final boolean steps = operator == Operator.INCREMENT || operator == Operator.DECREMENT;
        final boolean shaped = operator != null && method.isStatic()
                && method.modifiers().contains(IDecl.Modifier.PUBLIC)
                && method.parameters().size() == (operator.isUnary() ? 1 : 2)
                && method.parameters().stream().noneMatch(taken -> taken.outward() || taken.many())
                && (!steps || method.returnType().equals(type) && method.parameters().getFirst().type().equals(type));
        if (!shaped) {
            this.diagnostics.error(written.line(), written.column(), SigmaError.OPERATOR_SHAPE,
                    operator == null ? written.name() : OPERATOR_WORD + operator.text());
            return;
        }
        if (method.parameters().stream().noneMatch(taken -> taken.type().equals(type))) {
            this.diagnostics.error(written.line(), written.column(), SigmaError.OPERATOR_TYPE, type.name(),
                    type.name());
        }
    }

    /* A comparison declared without its other half, which C# asks for: == with !=, < with >, <= with >=. */
    private void checkOperatorPairs(final NamedType type, final IDecl.ClassDecl declaration) {
        for (final IMemberSymbol member : type.members()) {
            if (!(member instanceof IMemberSymbol.MethodSymbol method) || !method.isOperator()) {
                continue;
            }
            final Operator operator = Operator.ofMethod(method.name());
            final Operator partner = operator == null ? null : operator.partner();
            if (partner != null && type.members().stream().noneMatch(other -> other instanceof IMemberSymbol
                    .MethodSymbol half && half.isOperator() && half.name().equals(partner.methodName())
                    && half.sameParameters(method))) {
                final INode at = this.methodNodes.getOrDefault(method, declaration);
                this.diagnostics.error(at.line(), at.column(), SigmaError.OPERATOR_PAIR, type.name(),
                        operator.text(), partner.text());
            }
        }
    }

    /* An indexer belongs to an object and takes one value in its brackets: one method reads it, the other writes. */
    private void checkIndexer(final NamedType type, final IMemberSymbol.MethodSymbol method,
                              final IDecl.MethodDecl written) {
        final int keys = method.name().equals(IDecl.INDEXER_SET) ? method.parameters().size() - 1
                : method.parameters().size();
        if (method.isStatic() || keys != 1 || method.parameters().getFirst().outward()
                || method.parameters().getFirst().many()) {
            this.diagnostics.error(written.line(), written.column(), SigmaError.INDEXER_SHAPE, type.name());
        }
    }

    /**
     * The words a class may not be written with: sealed and static are a class's, never a struct's, and a record may be
     * sealed but not static; neither goes with abstract, nor with each other, since a static class is sealed already.
     * A static class holds only static members, and only a method may be sealed among them.
     */
    private void checkClassWords(final NamedType type, final IDecl.ClassDecl declaration) {
        final Set<IDecl.Modifier> words = declaration.modifiers();
        for (final IDecl.Modifier word : List.of(IDecl.Modifier.SEALED, IDecl.Modifier.STATIC)) {
            if (!words.contains(word)) {
                continue;
            }
            if (declaration.flavour() == IDecl.ClassDecl.Flavour.STRUCT
                    || word == IDecl.Modifier.STATIC && declaration.flavour() == IDecl.ClassDecl.Flavour.RECORD) {
                this.diagnostics.error(declaration.line(), declaration.column(), SigmaError.MODIFIER_NOT_ALLOWED,
                        declaration.name(), word.text());
            } else if (words.contains(IDecl.Modifier.ABSTRACT)) {
                this.diagnostics.error(declaration.line(), declaration.column(), SigmaError.MODIFIER_NOT_ALLOWED,
                        word.text(), IDecl.Modifier.ABSTRACT.text());
            }
        }
        if (words.contains(IDecl.Modifier.SEALED) && words.contains(IDecl.Modifier.STATIC)) {
            this.diagnostics.error(declaration.line(), declaration.column(), SigmaError.MODIFIER_NOT_ALLOWED,
                    IDecl.Modifier.SEALED.text(), IDecl.Modifier.STATIC.text());
        }
        for (final IDecl.IMemberDecl member : declaration.members()) {
            if (member instanceof IDecl.TypeMember) {
                continue;
            }
            if (type.isStatic() && !member.modifiers().contains(IDecl.Modifier.STATIC)
                    && !member.modifiers().contains(IDecl.Modifier.CONST)) {
                this.diagnostics.error(member.line(), member.column(), SigmaError.STATIC_CLASS_MEMBER, member.name());
            }
            if (!(member instanceof IDecl.MethodDecl)) {
                this.refuseSealed(member);
            }
        }
    }

    /* A declaration that cannot be sealed, written sealed. */
    private void refuseSealed(final IDecl declaration) {
        if (declaration.modifiers().contains(IDecl.Modifier.SEALED)) {
            this.diagnostics.error(declaration.line(), declaration.column(), SigmaError.MODIFIER_NOT_ALLOWED,
                    declaration.name(), IDecl.Modifier.SEALED.text());
        }
    }

    private void fillMember(final NamedType type, final IDecl.IMemberDecl member) {
        // A method or a constructor written unsafe may take and give pointers; nothing else declared may hold one.
        this.unsafeHere = member.modifiers().contains(IDecl.Modifier.UNSAFE);
        try {
            this.fillMemberOf(type, member);
        } finally {
            this.unsafeHere = false;
        }
    }

    private void fillMemberOf(final NamedType type, final IDecl.IMemberDecl member) {
        switch (member) {
            case IDecl.FieldDecl field -> this.addUnique(type, new IMemberSymbol.FieldSymbol(
                    type, field.name(), this.resolve(field.type()), field.modifiers()), field);
            case IDecl.MethodDecl method -> {
                final IMemberSymbol.MethodSymbol made = this.methodOf(type, method);
                // An extension method lives in a static class at the top of a namespace, where a using reaches it.
                if (made.extendsItsFirst() && (!type.isStatic() || this.outers.containsKey(type) || !made.isStatic())) {
                    this.diagnostics.error(method.line(), method.column(), SigmaError.EXTENSION_PLACE, method.name());
                }
                if (made.isOperator()) {
                    this.checkOperator(type, made, method);
                }
                if (made.isIndexer()) {
                    this.checkIndexer(type, made, method);
                }
                this.addMethod(type, made, method);
            }
            case IDecl.ConstructorDecl constructor -> {
                this.refuseReceiver(constructor.parameters(), constructor.name());
                type.addMember(new IMemberSymbol.ConstructorSymbol(
                        type, this.parametersOf(constructor.parameters()), constructor.modifiers()));
            }
            case IDecl.PropertyDecl property -> this.addUnique(type, new IMemberSymbol.PropertySymbol(
                    type, property.name(), this.resolve(property.type()),
                    property.getter() != null, property.setter() != null, property.modifiers(),
                    property.setter() == null ? Set.of() : property.setter().modifiers()), property);
            case IDecl.EventDecl event -> this.addEvent(type, event);
            case IDecl.TypeMember ignored -> { } // a nested type was named in the first pass and is filled as itself
        }
    }

    private void addEvent(final NamedType type, final IDecl.EventDecl event) {
        final ITypeSymbol resolved = this.resolve(event.type());
        if (resolved instanceof NamedType handler && handler.kind() == NamedType.Kind.DELEGATE) {
            this.addUnique(type, new IMemberSymbol.EventSymbol(type, event.name(), handler,
                    event.modifiers()), event);
            return;
        }
        if (!this.rules.isError(resolved)) {
            this.diagnostics.error(event.type().line(), event.type().column(),
                    SigmaError.EVENT_NEEDS_DELEGATE, event.type().describe());
        }
    }

    /*
     * A field, a property and an event share one set of names, because they are all read the same
     * way at a use site. Methods are left out of this: telling them apart by parameters is the point.
     */
    private void addUnique(final NamedType type, final IMemberSymbol member, final INode declaration) {
        for (final IMemberSymbol existing : type.members()) {
            if (!(existing instanceof IMemberSymbol.MethodSymbol) && existing.name().equals(member.name())) {
                this.diagnostics.error(declaration.line(), declaration.column(),
                        SigmaError.DUPLICATE_DECLARATION, member.name());
                return;
            }
        }
        type.addMember(member);
    }

    /*
     * Whether two indexer setters take the same keys, whatever the values they write: a write is chosen by its keys
     * alone, so the second would never be the one chosen.
     */
    private static boolean sameIndexerKeys(final IMemberSymbol.MethodSymbol one, final IMemberSymbol.MethodSymbol two) {
        if (!IDecl.INDEXER_SET.equals(one.name()) || one.parameters().size() != two.parameters().size()) {
            return false;
        }
        for (int i = 0; i < one.parameters().size() - 1; i++) {
            if (!one.parameters().get(i).type().equals(two.parameters().get(i).type())) {
                return false;
            }
        }
        return true;
    }

    /*
     * Two methods may share a name as long as their parameters tell them apart; two that take the same
     * types are one method written twice, and a call could never say which it meant.
     */
    private void addMethod(final NamedType type, final IMemberSymbol.MethodSymbol method, final INode declaration) {
        for (final IMemberSymbol existing : type.members()) {
            if (existing instanceof IMemberSymbol.MethodSymbol other && other.name().equals(method.name())
                    && (other.sameParameters(method) || sameIndexerKeys(other, method))) {
                this.diagnostics.error(declaration.line(), declaration.column(),
                        SigmaError.DUPLICATE_DECLARATION, method.describe());
                return;
            }
        }
        type.addMember(method);
        /*
         * Where the method was written, kept here rather than on the symbol. The override rules are checked in a
         * pass of their own once every type is filled in, and by then only the symbols are left; resolving the
         * declaration again to find its line would report every unknown type in it a second time.
         */
        this.methodNodes.put(method, declaration);
    }

    private IMemberSymbol.MethodSymbol methodOf(final NamedType type, final IDecl.MethodDecl method) {
        return new IMemberSymbol.MethodSymbol(type, method.name(), this.resolve(method.returnType()),
                this.parametersOf(method.parameters()), method.modifiers());
    }

    /**
     * The parameters as symbols, and what is wrong with a params among them: it is the last, and an array the method
     * reads.
     */
    private List<IMemberSymbol.ParameterSymbol> parametersOf(final List<IDecl.Parameter> parameters) {
        return this.parametersOf(parameters, this.current);
    }

    /** The same, for parameters written inside {@code from}, whose scope decides what a bare type name means. */
    List<IMemberSymbol.ParameterSymbol> parametersOf(final List<IDecl.Parameter> parameters, final NamedType from) {
        final List<IMemberSymbol.ParameterSymbol> symbols = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            final IDecl.Parameter parameter = parameters.get(i);
            final ITypeSymbol type = this.resolve(parameter.type(), from);
            boolean many = parameter.many();
            final boolean receiver = parameter.receiver() && i == 0 && !many && !parameter.outward();
            if (parameter.receiver() && !receiver) {
                this.diagnostics.error(parameter.line(), parameter.column(), SigmaError.EXTENSION_FIRST,
                        parameter.name());
            }
            if (many && i != parameters.size() - 1) {
                this.diagnostics.error(parameter.line(), parameter.column(), SigmaError.PARAMS_LAST, parameter.name());
                many = false;
            } else if (many && (parameter.outward()
                    || !(type instanceof ITypeSymbol.ArrayType) && !this.rules.isError(type))) {
                this.diagnostics.error(parameter.line(), parameter.column(), SigmaError.PARAMS_ARRAY,
                        parameter.name());
                many = false;
            }
            symbols.add(new IMemberSymbol.ParameterSymbol(parameter.name(), type, parameter.outward(), many, receiver));
        }
        return symbols;
    }

    /* A parameter written this where no extension method can be, reported as out of its place. */
    void refuseReceiver(final List<IDecl.Parameter> parameters, final String where) {
        for (final IDecl.Parameter parameter : parameters) {
            if (parameter.receiver()) {
                this.diagnostics.error(parameter.line(), parameter.column(), SigmaError.EXTENSION_PLACE, where);
            }
        }
    }

    /**
     * The extension methods called {@code name} that code written in {@code from} reaches: those of the static classes
     * at the top of its own namespace and the ones around it, and of the namespaces its file brought in with using.
     */
    List<IMemberSymbol.MethodSymbol> extensionsNamed(final String name, final NamedType from) {
        final Scope scope = from == null ? Scope.TOP : this.scopes.getOrDefault(from, Scope.TOP);
        final List<IMemberSymbol.MethodSymbol> found = new ArrayList<>();
        for (final NamedType type : this.declared.values()) {
            if (!type.isStatic() || this.outers.containsKey(type) || !reaches(scope, type)) {
                continue;
            }
            for (final IMemberSymbol member : type.members()) {
                if (member instanceof IMemberSymbol.MethodSymbol method && method.extendsItsFirst()
                        && method.name().equals(name)) {
                    found.add(method);
                }
            }
        }
        return found;
    }

    /* Whether code under that scope reaches the types of {@code type}'s namespace without writing it out. */
    private static boolean reaches(final Scope scope, final NamedType type) {
        final String home = type.namespace();
        String around = scope.namespace();
        while (true) {
            if (around.equals(home)) {
                return true;
            }
            if (around.isEmpty()) {
                break;
            }
            final int dot = around.lastIndexOf('.');
            around = dot >= 0 ? around.substring(0, dot) : "";
        }
        for (final CompilationUnit.Using using : scope.usings()) {
            if (using.all() ? home.equals(using.name()) || home.startsWith(using.name() + ".")
                    : using.name().equals(type.fullName())) {
                return true;
            }
        }
        return false;
    }

    private void fillInterface(final NamedType type, final IDecl.InterfaceDecl declaration) {
        this.refuseSealed(declaration);
        declaration.methods().forEach(this::refuseSealed);
        declaration.methods().forEach(method -> this.refuseReceiver(method.parameters(), method.name()));
        for (final TypeRef base : declaration.bases()) {
            final ITypeSymbol resolved = this.resolve(base);
            if (resolved instanceof NamedType named && named.kind() == NamedType.Kind.INTERFACE
                    && carries(named, type)) {
                this.diagnostics.error(base.line(), base.column(), SigmaError.CIRCULAR_BASE, type.name(),
                        named.name());
            } else if (resolved instanceof NamedType named && named.kind() == NamedType.Kind.INTERFACE) {
                type.addInterface(named);
            } else if (!this.rules.isError(resolved)) {
                this.diagnostics.error(base.line(), base.column(), SigmaError.INVALID_BASE, base.describe());
            }
        }
        for (final IDecl.MethodDecl method : declaration.methods()) {
            type.addMember(this.methodOf(type, method));
        }
    }

    // Every name in an enum is a value of the enum, held by the type rather than by an instance.
    private void fillEnum(final NamedType type, final IDecl.EnumDecl declaration) {
        this.refuseSealed(declaration);
        for (final IDecl.EnumConstant constant : declaration.constants()) {
            this.addUnique(type, new IMemberSymbol.FieldSymbol(type, constant.name(), type,
                    Set.of(IDecl.Modifier.PUBLIC, IDecl.Modifier.STATIC, IDecl.Modifier.READONLY)), constant);
        }
    }

    private void fillDelegate(final NamedType type, final IDecl.DelegateDecl declaration) {
        this.refuseSealed(declaration);
        this.refuseReceiver(declaration.parameters(), declaration.name());
        type.setInvoke(new IMemberSymbol.MethodSymbol(type, "Invoke", this.resolve(declaration.returnType()),
                this.parametersOf(declaration.parameters()), Set.of(IDecl.Modifier.PUBLIC)));
    }

    /** Resolves a type as it was written. Reports what it cannot resolve and gives back the error type. */
    public ITypeSymbol resolve(final TypeRef reference) {
        return this.resolve(reference, this.current);
    }

    /** Resolves a type as it was written inside {@code from}, whose scope decides what a bare name means. */
    public ITypeSymbol resolve(final TypeRef reference, final NamedType from) {
        if (reference == null) {
            return ITypeSymbol.Special.ERROR;
        }
        final NamedType was = this.current;
        this.current = from;
        try {
            return this.resolveIn(reference);
        } finally {
            this.current = was;
        }
    }

    private ITypeSymbol resolveIn(final TypeRef reference) {
        ITypeSymbol resolved = this.resolveName(reference);
        for (int rank = 0; rank < reference.arrayRank(); rank++) {
            resolved = new ITypeSymbol.ArrayType(resolved);
        }
        return resolved;
    }

    private ITypeSymbol resolveName(final TypeRef reference) {
        if (reference.isTuple()) {
            return this.tupleType(reference);
        }
        if (reference.isPointer()) {
            return this.pointerType(reference);
        }
        final String name = reference.name();
        final ITypeSymbol.Primitive primitive = ITypeSymbol.Primitive.written(name);
        if (primitive != null) {
            return this.withoutArguments(reference, primitive);
        }
        final NamedType type = this.lookup(name, this.current, reference.arguments().size());
        if (type == null) {
            this.reportUnknown(reference.line(), reference.column(), name);
            return ITypeSymbol.Special.ERROR;
        }
        this.reportIfNewer(type, reference.line(), reference.column());
        if (type.typeParameters().size() != reference.arguments().size()) {
            this.diagnostics.error(reference.line(), reference.column(),
                    SigmaError.WRONG_TYPE_ARGUMENT_COUNT, name, type.typeParameters().size());
            return ITypeSymbol.Special.ERROR;
        }
        if (type.typeParameters().isEmpty()) {
            return type;
        }
        final List<ITypeSymbol> arguments = new ArrayList<>();
        for (final TypeRef argument : reference.arguments()) {
            arguments.add(this.resolve(argument));
        }
        return new ITypeSymbol.GenericType(type, arguments);
    }

    /* A tuple's type: from two to eight places, each holding a value, called what it was written with. */
    private ITypeSymbol tupleType(final TypeRef reference) {
        final List<ITypeSymbol> places = new ArrayList<>();
        boolean known = true;
        for (final TypeRef place : reference.arguments()) {
            final ITypeSymbol resolved = this.resolveIn(place);
            if (resolved == ITypeSymbol.Primitive.VOID) {
                this.diagnostics.error(place.line(), place.column(), SigmaError.TUPLE_PLACE_TYPE, place.describe());
            }
            known &= !this.rules.isError(resolved) && resolved != ITypeSymbol.Primitive.VOID;
            places.add(resolved);
        }
        if (places.size() < 2 || places.size() > MAX_TUPLE) {
            this.diagnostics.error(reference.line(), reference.column(), SigmaError.TUPLE_SIZE, places.size());
            return ITypeSymbol.Special.ERROR;
        }
        return known ? new ITypeSymbol.TupleType(this.tupleOf(places), reference.names()) : ITypeSymbol.Special.ERROR;
    }

    /** The struct a tuple of those places is, the same one for every tuple of them. */
    NamedType tupleOf(final List<ITypeSymbol> places) {
        final List<String> written = new ArrayList<>();
        for (final ITypeSymbol place : places) {
            written.add(place.describe());
        }
        return this.tuples.computeIfAbsent("ValueTuple<" + String.join(", ", written) + ">",
                name -> NamedType.tupleOf(name, places, this.builtIns.stringType()));
    }

    /** The structs the program's tuples are, in the order they were first written. */
    public List<NamedType> tuples() {
        return List.copyOf(this.tuples.values());
    }

    /*
     * "Node*": a pointer, written only in unsafe code. What it points at is asked once every struct is filled in,
     * since a struct that holds a pointer to itself is still being filled when the pointer is read.
     */
    private ITypeSymbol pointerType(final TypeRef reference) {
        final ITypeSymbol pointee = this.resolveIn(reference.arguments().getFirst());
        // A signature is read again when its body is checked, and what was wrong with it was said the first time.
        if (this.pointersRead.add(reference)) {
            if (!this.unsafeHere && this.level != LanguageLevel.SIGMA) {
                this.diagnostics.error(reference.line(), reference.column(), SigmaError.POINTERS_NEED_UNSAFE);
            }
            if (this.filled) {
                this.checkPointee(reference, pointee);
            } else {
                this.pointees.add(new Pointee(reference, pointee, this.files.get(this.current)));
            }
        }
        return this.rules.isError(pointee) ? ITypeSymbol.Special.ERROR : new ITypeSymbol.PointerType(pointee);
    }

    /** Reports a pointer to a type no pointer may point at. */
    private void checkPointee(final TypeRef at, final ITypeSymbol pointee) {
        if (!this.rules.isError(pointee) && !this.layout.pointable(pointee)) {
            this.diagnostics.error(at.line(), at.column(), SigmaError.POINTER_TARGET, pointee.describe());
        }
    }

    /** How values lie in raw memory, for the checks that ask where a field sits or what a type takes. */
    public Layout layout() {
        return this.layout;
    }

    private ITypeSymbol withoutArguments(final TypeRef reference, final ITypeSymbol resolved) {
        if (!reference.arguments().isEmpty()) {
            this.diagnostics.error(reference.line(), reference.column(),
                    SigmaError.WRONG_TYPE_ARGUMENT_COUNT, reference.name(), 0);
            return ITypeSymbol.Special.ERROR;
        }
        return resolved;
    }

    /** Reports every interface method a class said it would have and does not. */
    public void checkInterfaces() {
        for (final Map.Entry<NamedType, IDecl.ITypeDecl> entry : this.sources.entrySet()) {
            final NamedType type = entry.getKey();
            if (!type.kind().classLike()) {
                continue;
            }
            this.diagnostics.setFile(this.files.get(type));
            for (final NamedType face : type.interfaces()) {
                for (final IMemberSymbol required : face.allMembers()) {
                    if (required instanceof IMemberSymbol.MethodSymbol method && !this.hasMethod(type, method)) {
                        this.diagnostics.error(entry.getValue().line(), entry.getValue().column(),
                                SigmaError.MISSING_INTERFACE_MEMBER, type.name(), face.name(),
                                method.describe());
                    }
                }
            }
        }
    }

    /**
     * Reports every way a method and the one it stands over disagree about replacing it.
     *
     * <p>Until now a method that happened to carry an inherited one's name and parameters simply took its place,
     * silently, and the object's real type decided which ran. That is still what happens at run time, and it is
     * why nothing here changes a listing: what changes is that the replacement has to be written down. A method
     * that replaces one says {@code override}; the one underneath says {@code virtual} or {@code abstract}, or
     * there is nothing to replace and hiding it by accident is refused outright rather than obeyed.
     *
     * <p>Only the chain of base CLASSES is walked. Giving an interface the method it asked for is not replacing
     * anything, so it needs no word, which is what keeps every program written before this one compiling.
     */
    public void checkOverrides() {
        for (final Map.Entry<NamedType, IDecl.ITypeDecl> entry : this.sources.entrySet()) {
            final NamedType type = entry.getKey();
            if (!type.kind().classLike()) {
                continue;
            }
            this.diagnostics.setFile(this.files.get(type));
            for (final IMemberSymbol member : type.members()) {
                if (member instanceof IMemberSymbol.MethodSymbol method
                        && this.methodNodes.get(method) instanceof IDecl.MethodDecl written) {
                    this.checkOneOverride(type, method, written);
                }
            }
            this.checkAbstractsAnswered(type, entry.getValue());
        }
    }

    private void checkOneOverride(final NamedType type, final IMemberSymbol.MethodSymbol method,
                                  final IDecl.MethodDecl written) {
        final Set<IDecl.Modifier> words = method.modifiers();
        final boolean overrides = words.contains(IDecl.Modifier.OVERRIDE);
        final boolean virtual = words.contains(IDecl.Modifier.VIRTUAL);
        final boolean isAbstract = words.contains(IDecl.Modifier.ABSTRACT);
        final boolean sealed = words.contains(IDecl.Modifier.SEALED);
        /*
         * A static method belongs to the type and a struct is copied rather than pointed at, so neither has
         * anything for these words to mean: there is no object whose real type could decide.
         */
        if (method.isStatic() || type.kind() == NamedType.Kind.STRUCT) {
            final IDecl.Modifier offender = virtual ? IDecl.Modifier.VIRTUAL
                    : overrides ? IDecl.Modifier.OVERRIDE : isAbstract ? IDecl.Modifier.ABSTRACT
                    : sealed ? IDecl.Modifier.SEALED : null;
            if (offender != null) {
                this.diagnostics.error(written.line(), written.column(), SigmaError.MODIFIER_NOT_ALLOWED,
                        method.describe(), offender.text());
            }
            return;
        }
        // Sealed stops the replacing at an override, so it goes with override and with nothing left to fill.
        if (sealed && !overrides) {
            this.diagnostics.error(written.line(), written.column(), SigmaError.SEALED_NEEDS_OVERRIDE,
                    method.describe());
        } else if (sealed && isAbstract) {
            this.diagnostics.error(written.line(), written.column(), SigmaError.MODIFIER_NOT_ALLOWED,
                    IDecl.Modifier.SEALED.text(), IDecl.Modifier.ABSTRACT.text());
        }
        this.checkBody(type, method, written, isAbstract);
        final IMemberSymbol.MethodSymbol above = this.methodAbove(type, method);
        if (above == null) {
            if (overrides) {
                this.diagnostics.error(written.line(), written.column(),
                        SigmaError.OVERRIDE_WITHOUT_BASE, method.describe(), type.name());
            }
            return;
        }
        if (above.modifiers().contains(IDecl.Modifier.SEALED)) {
            this.diagnostics.error(written.line(), written.column(), SigmaError.SEALED_BASE,
                    above.owner().name() + "." + above.describe());
        } else if (!replaceable(above)) {
            this.diagnostics.error(written.line(), written.column(),
                    SigmaError.CANNOT_OVERRIDE, method.describe(), above.owner().name());
        } else if (!overrides) {
            this.diagnostics.error(written.line(), written.column(),
                    SigmaError.NEEDS_OVERRIDE, method.describe(), above.owner().name());
        }
    }

    /** A method with no body is abstract and lives in an abstract class; one with the word has no body. */
    private void checkBody(final NamedType type, final IMemberSymbol.MethodSymbol method,
                           final IDecl.MethodDecl written, final boolean isAbstract) {
        if (isAbstract) {
            if (written.body() != null) {
                this.diagnostics.error(written.line(), written.column(),
                        SigmaError.ABSTRACT_WITH_BODY, method.describe());
            }
            if (!type.isAbstract()) {
                this.diagnostics.error(written.line(), written.column(),
                        SigmaError.ABSTRACT_NEEDS_ABSTRACT_CLASS, method.describe(), type.name());
            }
        } else if (written.body() == null) {
            this.diagnostics.error(written.line(), written.column(),
                    SigmaError.MISSING_BODY, method.describe());
        }
    }

    /** A class that can be made has to answer every abstract method it inherits. */
    private void checkAbstractsAnswered(final NamedType type, final IDecl.ITypeDecl declaration) {
        if (type.isAbstract()) {
            return;
        }
        for (NamedType above = type.base(); above != null; above = above.base()) {
            for (final IMemberSymbol member : above.members()) {
                if (member instanceof IMemberSymbol.MethodSymbol method
                        && method.modifiers().contains(IDecl.Modifier.ABSTRACT)
                        && !this.answered(type, above, method)) {
                    this.diagnostics.error(declaration.line(), declaration.column(),
                            SigmaError.ABSTRACT_NOT_IMPLEMENTED, type.name(), method.describe());
                }
            }
        }
    }

    /** Whether some type between {@code type} and {@code owner} gives {@code wanted} a body. */
    private boolean answered(final NamedType type, final NamedType owner,
                             final IMemberSymbol.MethodSymbol wanted) {
        for (NamedType at = type; at != null && at != owner; at = at.base()) {
            for (final IMemberSymbol member : at.members()) {
                if (member instanceof IMemberSymbol.MethodSymbol candidate
                        && !candidate.modifiers().contains(IDecl.Modifier.ABSTRACT)
                        && candidate.name().equals(wanted.name())
                        && candidate.sameParameters(wanted)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The nearest method of the same shape on a base class, or nothing when this method stands alone. */
    private IMemberSymbol.MethodSymbol methodAbove(final NamedType type,
                                                   final IMemberSymbol.MethodSymbol method) {
        for (NamedType above = type.base(); above != null; above = above.base()) {
            for (final IMemberSymbol member : above.members()) {
                if (member instanceof IMemberSymbol.MethodSymbol candidate
                        && !candidate.isStatic()
                        && candidate.name().equals(method.name())
                        && candidate.sameParameters(method)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** Whether {@code type} already stands behind {@code candidate}, the candidate itself included. */
    private static boolean standsBehind(final NamedType type, final NamedType candidate) {
        final Set<NamedType> seen = new HashSet<>();
        for (NamedType at = candidate; at != null && seen.add(at); at = at.base()) {
            if (at == type) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code candidate} carries {@code type} among its interfaces at any depth, or is it. */
    private static boolean carries(final NamedType candidate, final NamedType type) {
        final Deque<NamedType> open = new ArrayDeque<>();
        open.push(candidate);
        final Set<NamedType> seen = new HashSet<>();
        while (!open.isEmpty()) {
            final NamedType at = open.pop();
            if (at == type) {
                return true;
            }
            if (seen.add(at)) {
                at.interfaces().forEach(open::push);
            }
        }
        return false;
    }

    /*
     * A method of a generic type as the type declares it, by its name and how many values it takes: the use a program
     * made of it has its type's parameters filled in. A type with no parameters declares the method as it is used.
     */
    private static IMemberSymbol.MethodSymbol declaredForm(final NamedType owner,
                                                           final IMemberSymbol.MethodSymbol method) {
        if (owner.typeParameters().isEmpty()) {
            return method;
        }
        for (final IMemberSymbol member : owner.members()) {
            if (member instanceof IMemberSymbol.MethodSymbol declared && declared.name().equals(method.name())
                    && declared.parameters().size() == method.parameters().size()) {
                return declared;
            }
        }
        return method;
    }

    /** Whether something may stand in this method's place: it said so, or it is already standing in one. */
    private static boolean replaceable(final IMemberSymbol.MethodSymbol method) {
        return method.modifiers().contains(IDecl.Modifier.VIRTUAL)
                || method.modifiers().contains(IDecl.Modifier.ABSTRACT)
                || method.modifiers().contains(IDecl.Modifier.OVERRIDE);
    }

    private boolean hasMethod(final NamedType type, final IMemberSymbol.MethodSymbol required) {
        for (final IMemberSymbol member : type.allMembers()) {
            if (member instanceof IMemberSymbol.MethodSymbol candidate
                    && candidate.owner().kind().classLike()
                    && candidate.name().equals(required.name())
                    && candidate.returnType().equals(required.returnType())
                    && candidate.sameParameters(required)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds where the runtime starts, and which of the two kinds of program this is.
     *
     * <p>A class that implements the script interface is one that stays up; a class with a static
     * {@code Main} is one that runs at a terminal, and from the second version that Main may give back the code
     * the program ends with and may be handed the program's arguments. A program is
     * exactly one of those: none and there is nothing to run, more than one and there is no saying
     * which. A class that does both is a script, because implementing the interface is the deliberate
     * act and a method called Main is only a name.
     */
    public void checkEntryPoint(final int line, final int column) {
        this.checkEntryPoint(line, column, null);
    }

    /**
     * The same, started at the class called {@code entry} when one is named. A program that declares a system starts
     * there, at the system, whatever services it holds beside it: those are started from the same sources, each at
     * its own class, when the system's build names them.
     */
    public void checkEntryPoint(final int line, final int column, final String entry) {
        final List<NamedType> systems = new ArrayList<>();
        final List<NamedType> scripts = new ArrayList<>();
        final List<NamedType> consoles = new ArrayList<>();
        for (final NamedType type : this.declared.values()) {
            if (type.kind() != NamedType.Kind.CLASS) {
                continue;
            }
            if (type.isOrDescendsFrom(this.builtIns.systemType())) {
                systems.add(type);
            } else if (type.isOrDescendsFrom(this.builtIns.scriptType())) {
                scripts.add(type);
            } else if (mainOf(type) != null) {
                consoles.add(type);
            }
        }
        if (entry != null) {
            for (final NamedType type : scripts) {
                if (type.name().equals(entry)) {
                    this.model.setEntryPoint(type, Shape.SCRIPT);
                    return;
                }
            }
            this.diagnostics.error(line, column, SigmaError.ENTRY_POINT, 0);
            return;
        }
        if (systems.size() == 1) {
            this.model.setEntryPoint(systems.getFirst(), Shape.SYSTEM);
            return;
        }
        if (scripts.size() + consoles.size() != 1) {
            this.diagnostics.error(line, column, SigmaError.ENTRY_POINT,
                    scripts.size() + consoles.size());
            return;
        }
        this.model.setEntryPoint(scripts.isEmpty() ? consoles.getFirst() : scripts.getFirst(),
                scripts.isEmpty() ? Shape.CONSOLE : Shape.SCRIPT);
        if (scripts.isEmpty()) {
            this.reportIfLaterMain(mainOf(consoles.getFirst()), line, column);
        }
    }

    /**
     * Reports a Main of a shape the first version did not start at: one that gives back the code the program ends
     * with, or one handed the program's arguments.
     */
    private void reportIfLaterMain(final IMemberSymbol.MethodSymbol main, final int line, final int column) {
        final boolean gives = main.returnType() != ITypeSymbol.Primitive.VOID;
        final boolean takes = !main.parameters().isEmpty();
        if (!gives && !takes) {
            return;
        }
        final INode at = this.methodNodes.get(main);
        this.reportIfLater((gives ? "int " : "") + MAIN + "(" + (takes ? ARGUMENTS : "") + ")", LATER_MAIN,
                at == null ? line : at.line(), at == null ? column : at.column());
    }

    /**
     * That class's Main: static, giving back nothing or the code the program ends with, taking nothing or the
     * program's arguments as an array of text; or null when it has none of those shapes. When it has more than one,
     * the one that takes nothing is where the program starts, as it is for the runtime.
     */
    public static IMemberSymbol.MethodSymbol mainOf(final NamedType type) {
        IMemberSymbol.MethodSymbol handed = null;
        for (final IMemberSymbol member : type.members()) {
            if (!(member instanceof IMemberSymbol.MethodSymbol method) || !MAIN.equals(method.name())
                    || !method.isStatic() || (method.returnType() != ITypeSymbol.Primitive.VOID
                    && method.returnType() != ITypeSymbol.Primitive.INT)) {
                continue;
            }
            if (method.parameters().isEmpty()) {
                return method;
            }
            if (handed == null && method.parameters().size() == 1 && !method.parameters().getFirst().outward()
                    && ARGUMENTS.equals(method.parameters().getFirst().type().describe())) {
                handed = method;
            }
        }
        return handed;
    }

    /** The name a program that runs at a terminal starts at. */
    public static final String MAIN = "Main";
    /** The type a Main that is handed the program's arguments takes them as. */
    public static final String ARGUMENTS = "string[]";
    /** The version a Main that gives back a code or takes the arguments came in. */
    private static final int LATER_MAIN = 2;
    /** The most places a tuple has, as C#'s written tuples have before they nest. */
    static final int MAX_TUPLE = 8;
    /** How a message writes an operator: the word, then its sign. */
    private static final String OPERATOR_WORD = "operator ";
}
