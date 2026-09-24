/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.hardware.ArchitectureSpec;
import dev.jstech.computers.hardware.Architectures;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.Diagnostic;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.sigma.pack.Manifest;
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The verbs the Σ# toolchain brings to the prompt: one to compile a program, one to run it, and one
 * to wrap it up so somebody else can.
 *
 * <p>None exists until its package is installed, the way any other package's verbs do not. All are
 * ordinary shell commands with no window of their own, because writing, running and packaging a program
 * is done where the files are.
 */
@TextHolder
public final class SigmaCommands {

    /** The id of the compiler package, as the Mirror serves it. */
    public static final String COMPILER = "jsc:sgsc";

    /** The id of the runtime package. */
    public static final String RUNTIME = "jsc:sigma";

    /**
     * The id of the smaller language's compiler, which is a package of its own.
     *
     * <p>It is the toolchain the earliest machines can hold: a compiler and nothing else, since what it writes is
     * the assembly the machine already runs and needs no runtime brought along beside it.
     */
    public static final String SUBSET_COMPILER = "jsc:scc";

    /** The extension a program is written in, in each language, and the one it is compiled to. */
    private static final String SOURCE = "." + LanguageLevel.SIGMA_SHARP.sourceExtension();
    private static final String SUBSET_SOURCE = "." + LanguageLevel.SIGMA.sourceExtension();
    private static final String ASSEMBLY = ".asm";

    /* What more than one of the verbs says: a file written, and the files a runtime takes. */
    private static final TextKey WROTE = TextKey.of("jsc.cli.sigma.wrote", "wrote %s");
    private static final TextKey FILE_OF = TextKey.of("jsc.cli.sigma.file_of", "<file%s>");

    private SigmaCommands() {
    }

    /** Every verb, for the shell to register. */
    public static List<ICliCommand> all() {
        return List.of(
                new Compile(LanguageLevel.SIGMA_SHARP.compiler(), SOURCE, COMPILER, LanguageLevel.SIGMA_SHARP,
                        Architectures.oldestFor(LanguageLevel.SIGMA_SHARP).id()),
                new Compile(LanguageLevel.SIGMA.compiler(), SUBSET_SOURCE, SUBSET_COMPILER, LanguageLevel.SIGMA,
                        Architectures.oldestFor(LanguageLevel.SIGMA).id()),
                new Run(), new Pack());
    }

    /** Whether that package is installed on the computer. */
    public static boolean installed(final ICliComputer computer, final String id) {
        for (final ICliComputer.ProgramInfo program : computer.programs()) {
            if (id.equalsIgnoreCase(program.id())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compiles one or more source files into one assembly listing.
     *
     * <p>One command for both languages, because compiling is the same work: the language a source may be decides
     * what the compiler accepts, never what it writes. A program the smaller language takes compiles to the very
     * listing the bigger one would have written for it, which is what makes it a subset in fact and not in name.
     */
    @TextHolder
    static final class Compile implements ICliCommand {

        private final String verb;
        private final String extension;
        private final String packageId;
        private final LanguageLevel level;
        /*
         * The oldest machine this language runs on, which is where a program starts before what it turned out
         * to use is allowed to push it up. The smaller language exists for the oldest machines of all, so its
         * programs begin there; the full one begins where its own library does.
         */
        private final String baseline;

        /* The language's name goes in as it is written, the same in every language. */
        private static final TextKey SUMMARY = TextKey.of("jsc.cli.sigma.compile.summary",
                "compile a %s program into the assembly a machine runs");
        /* The source's extension, then the assembly's. */
        private static final TextKey USAGE = TextKey.of("jsc.cli.sigma.compile.usage",
                "<file%1$s> [more%1$s ...] [-o <out%2$s>] [--arch <architecture>]");
        private static final TextKey NO_ARCHITECTURE = TextKey.of("jsc.cli.sigma.compile.no_architecture",
                "no architecture is called '%s'; there is %s");
        private static final TextKey SIGMA_ONLY = TextKey.of("jsc.cli.sigma.compile.sigma_only",
                "%s runs Σ only; write it in Σ and build it with scc");
        private static final TextKey ONE_ERROR = TextKey.of("jsc.cli.sigma.compile.one_error",
                "1 error, nothing was written");
        private static final TextKey ERRORS = TextKey.of("jsc.cli.sigma.compile.errors",
                "%s errors, nothing was written");
        private static final TextKey RUN_IT = TextKey.of("jsc.cli.sigma.compile.run_it", "run it with: sigma run %s");

        Compile(final String verb, final String extension, final String packageId, final LanguageLevel level,
                final String baseline) {
            this.verb = verb;
            this.extension = extension;
            this.packageId = packageId;
            this.level = level;
            this.baseline = baseline;
        }

        @Override
        public String name() {
            return this.verb;
        }

        @Override
        public CommandGroup group() {
            return CommandGroup.PROGRAMMING;
        }

        @Override
        public Text summary() {
            return SUMMARY.with(this.level.full() ? "Σ#" : "Σ");
        }

        @Override
        public Text usage() {
            return USAGE.with(this.extension, ASSEMBLY);
        }

        @Override
        public CommandScope scope() {
            return CommandScope.everywhere().fromPackage(this.packageId);
        }

        @Override
        public void run(final CliContext ctx) {
            final List<String> paths = new ArrayList<>();
            String out = null;
            String arch = null;
            for (int i = 0; i < ctx.args().size(); i++) {
                final String arg = ctx.args().get(i);
                if ("-o".equals(arg)) {
                    i++;
                    out = i < ctx.args().size() ? ctx.args().get(i) : null;
                } else if ("--arch".equals(arg)) {
                    i++;
                    arch = i < ctx.args().size() ? ctx.args().get(i) : null;
                } else {
                    paths.add(arg);
                }
            }
            if (paths.isEmpty()) {
                ctx.out().error(CliTexts.USAGE.with(this.verb, this.usage()));
                return;
            }
            /*
             * Asked for by id or by the name it is written under, and refused before anything is compiled: a
             * listing built for an architecture nothing answers to would be a file no machine anywhere runs.
             */
            String architecture = this.baseline;
            if (arch != null) {
                final Optional<ArchitectureSpec> asked = Architectures.find(arch);
                if (asked.isEmpty()) {
                    ctx.out().error(CliTexts.SAID_BY.with(this.verb, NO_ARCHITECTURE.with(arch, architectureNames())));
                    return;
                }
                architecture = asked.get().id();
            }
            /*
             * The oldest machines run the smaller language and nothing else, and the way that is kept true is
             * here rather than at the machine: a listing they could load can only ever have been written from a
             * source they could have held. Refused before anything is compiled, with the way to do it.
             */
            if (this.level.full() && Architectures.X86_16.id().equals(architecture)) {
                ctx.out().error(CliTexts.SAID_BY.with(this.verb, SIGMA_ONLY.with(Architectures.X86_16.name())));
                return;
            }

            final List<SourceFile> sources = new ArrayList<>();
            for (final String path : paths) {
                final ICliComputer.FsResult read = ctx.computer().readFile(path);
                if (!read.ok()) {
                    ctx.out().error(CliTexts.SAID_BY.with(this.verb, read.message()));
                    return;
                }
                sources.add(new SourceFile(leaf(path), read.message().english()));
            }

            final SigmaCompiler.Result built = SigmaCompiler.compile(sources, architecture, this.level);
            final List<Diagnostic> diagnostics = built.diagnostics();
            final String assembly = built.ok() ? built.assembly() : null;
            for (final Diagnostic diagnostic : diagnostics) {
                if (diagnostic.isError()) {
                    ctx.out().error(diagnostic.format());
                } else {
                    ctx.out().dim(diagnostic.format());
                }
            }
            if (assembly == null) {
                ctx.computer().report(JscEvents.SIGMA_COMPILE_ERROR, "");
                final long errors = diagnostics.stream().filter(Diagnostic::isError).count();
                ctx.out().error(CliTexts.SAID_BY.with(this.verb, errors == 1 ? ONE_ERROR : ERRORS.with(errors)));
                return;
            }
            final String target = out != null ? out : compiled(paths.getFirst());
            final ICliComputer.FsResult written = ctx.computer().writeFile(target, assembly);
            if (!written.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(this.verb, written.message()));
                return;
            }
            ctx.out().ok(CliTexts.SAID_BY.with(this.verb, WROTE.with(target)));
            if (this.level == LanguageLevel.SIGMA && ctx.computer().platform() == Platform.MC_DOS) {
                ctx.computer().report(JscEvents.SIGMA_ON_DOS, "");
            }
            // Compiling is not running, and the prompt is the place to say how the second is done.
            ctx.out().dim(RUN_IT.with(target));
        }

        /** The architectures there are, by name, for the person who asked for one that is not there. */
        private static String architectureNames() {
            final List<String> names = new ArrayList<>();
            for (final ArchitectureSpec one : Architectures.all()) {
                names.add(one.name());
            }
            return String.join(", ", names);
        }

        /** The name a source file compiles to: the same name, with the assembly's extension. */
        static String compiled(final String path) {
            final int dot = path.lastIndexOf('.');
            final int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return dot > slash ? path.substring(0, dot) + ASSEMBLY : path + ASSEMBLY;
        }

        /** The file's own name, which is what a diagnostic quotes rather than the path to it. */
        private static String leaf(final String path) {
            final int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return slash < 0 ? path : path.substring(slash + 1);
        }
    }

    /**
     * What one runtime's verb runs: its name at the prompt, the package that brings it, the language
     * it speaks, and the kinds of file it takes.
     */
    record Runtime(String verb, String packageId, String language, List<String> extensions) {

        boolean takes(final String path) {
            final String lower = path.toLowerCase(Locale.ROOT);
            for (final String extension : this.extensions) {
                if (lower.endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }

        Text files() {
            return FILE_OF.with(String.join("|", this.extensions));
        }
    }

    static final Runtime SIGMA = new Runtime("sigma", RUNTIME, "Σ#", List.of(ASSEMBLY, SOURCE, SUBSET_SOURCE));

    /** Starts, stops and lists the programs one runtime is running on this computer. */
    @TextHolder
    static final class Run implements ICliCommand {

        private final Runtime runtime;

        /* The runtime's language and its files go in as they are written. */
        private static final TextKey SUMMARY = TextKey.of("jsc.cli.sigma.run.summary",
                "run a %s program, stop one, or list what is running");
        private static final TextKey USAGE = TextKey.of("jsc.cli.sigma.run.usage",
                "run %s [--heap <n>M] | stop <id> | ps");
        private static final TextKey RUN_USAGE = TextKey.of("jsc.cli.sigma.run.run_usage", "run %s [--heap <n>M]");
        private static final TextKey STOP_USAGE = TextKey.of("jsc.cli.sigma.run.stop_usage",
                "stop <id>   (as listed by '%s ps')");
        private static final TextKey NOT_A_PROGRAM = TextKey.of("jsc.cli.sigma.run.not_a_program",
                "%s is not a %s program");
        private static final TextKey NONE_RUNNING = TextKey.of("jsc.cli.sigma.run.none_running",
                "no %s programs are running");
        /* The spacing lines the words up over the columns of the rows below it. */
        private static final TextKey HEADER = TextKey.of("jsc.cli.sigma.run.header",
                "  id  name                 state      memory");
        private static final TextKey HELD_OF = TextKey.of("jsc.cli.sigma.run.held_of", "%s of %s");

        Run() {
            this(SIGMA);
        }

        Run(final Runtime runtime) {
            this.runtime = runtime;
        }

        @Override
        public String name() {
            return this.runtime.verb();
        }

        @Override
        public CommandGroup group() {
            return CommandGroup.PROGRAMMING;
        }

        @Override
        public Text summary() {
            return SUMMARY.with(this.runtime.language());
        }

        @Override
        public Text usage() {
            return USAGE.with(this.runtime.files());
        }

        @Override
        public CommandScope scope() {
            return CommandScope.everywhere().fromPackage(this.runtime.packageId());
        }

        @Override
        public void run(final CliContext ctx) {
            switch (ctx.arg(0).toLowerCase(Locale.ROOT)) {
                case "run", "start" -> this.start(ctx);
                case "stop", "kill" -> this.stop(ctx);
                case "ps", "list", "" -> this.list(ctx);
                default -> ctx.out().error(CliTexts.USAGE.with(this.runtime.verb(), this.usage()));
            }
        }

        private void start(final CliContext ctx) {
            final String path = ctx.arg(1);
            if (path.isEmpty()) {
                ctx.out().error(CliTexts.USAGE.with(this.runtime.verb(), RUN_USAGE.with(this.runtime.files())));
                return;
            }
            if (!this.runtime.takes(path)) {
                ctx.out().error(CliTexts.SAID_BY.with(this.runtime.verb(),
                        NOT_A_PROGRAM.with(path, this.runtime.language())));
                return;
            }
            int heapMb = 0;
            final List<String> arguments = new ArrayList<>();
            for (int i = 2; i < ctx.args().size(); i++) {
                if ("--heap".equals(ctx.args().get(i)) && i < ctx.args().size() - 1) {
                    heapMb = megabytes(ctx.args().get(++i));
                    continue;
                }
                // Everything else after the file is the program's own: what Program.Args reads.
                arguments.add(ctx.args().get(i));
            }
            final ICliComputer.OpResult started = ctx.computer().startSigma(path, heapMb, arguments);
            if (started.ok()) {
                ctx.out().ok(started.message());
            } else {
                ctx.out().error(started.message());
            }
        }

        private void stop(final CliContext ctx) {
            final int id = whole(ctx.arg(1));
            final String verb = this.runtime.verb();
            if (id < 0) {
                ctx.out().error(CliTexts.USAGE.with(verb, STOP_USAGE.with(verb)));
                return;
            }
            final ICliComputer.OpResult stopped = ctx.computer().stopSigma(id);
            if (stopped.ok()) {
                ctx.out().ok(stopped.message());
            } else {
                ctx.out().error(stopped.message());
            }
        }

        private void list(final CliContext ctx) {
            final List<ICliComputer.SigmaProcess> running =
                    new ArrayList<>(ctx.computer().sigmaProcesses());
            if (running.isEmpty()) {
                ctx.out().dim(NONE_RUNNING.with(this.runtime.language()));
                return;
            }
            ctx.out().info(HEADER);
            for (final ICliComputer.SigmaProcess process : running) {
                /* The columns are figures and names, the same in every language; only the memory is a sentence. */
                ctx.out().line(CliLine.of(
                        CliSpan.plain(Text.literal(String.format(Locale.ROOT, "  %-3d %-20s %-10s ",
                                process.id(), cut(process.name(), 20), cut(process.state(), 10)))),
                        CliSpan.plain(HELD_OF.with(kilobytes(process.heldBytes()),
                                kilobytes(process.heapBytes())))));
            }
        }

        /** "16M" and "16" both mean sixteen; anything else means the computer decides. */
        private static int megabytes(final String written) {
            final String text = written.toUpperCase(Locale.ROOT).replace("MB", "").replace("M", "");
            return Math.max(0, whole(text));
        }

        private static int whole(final String written) {
            try {
                return Integer.parseInt(written.trim());
            } catch (final NumberFormatException notANumber) {
                return -1;
            }
        }

        private static String kilobytes(final long bytes) {
            return bytes < 1024 ? bytes + " B" : (bytes / 1024) + " KB";
        }

        private static String cut(final String text, final int width) {
            return text.length() <= width ? text : text.substring(0, width - 1) + "~";
        }
    }

    /**
     * Wraps a program up so it can be handed to somebody else.
     *
     * <p>A package is one piece of readable text: its manifest and every file in it. That is the point.
     * Somebody about to install a stranger's program can open it and read the whole thing first, which
     * is not a thing you can say of most places software comes from.
     */
    @TextHolder
    static final class Pack implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.sigma.sgpack.summary",
                "start a package, and build one from what is here");
        private static final TextKey USAGE = TextKey.of("jsc.cli.sigma.sgpack.usage",
                "init [name] | build | publish [file] | unpublish <name>");
        /* The verbs as the usage lists them, each with what it does; the spacing keeps the second column straight. */
        private static final TextKey VERB_INIT = TextKey.of("jsc.cli.sigma.sgpack.verb.init",
                "  init [name]       write a %s to fill in");
        private static final TextKey VERB_BUILD = TextKey.of("jsc.cli.sigma.sgpack.verb.build",
                "  build             make the package the manifest describes");
        private static final TextKey VERB_PUBLISH = TextKey.of("jsc.cli.sigma.sgpack.verb.publish",
                "  publish [file]    put it on the network's Mirror");
        private static final TextKey VERB_UNPUBLISH = TextKey.of("jsc.cli.sigma.sgpack.verb.unpublish",
                "  unpublish <name>  take it back off");
        private static final TextKey ALREADY_HERE = TextKey.of("jsc.cli.sigma.sgpack.already_here",
                "%s is already here; edit it, or delete it to start over");
        private static final TextKey THEN_BUILD = TextKey.of("jsc.cli.sigma.sgpack.then_build",
                "edit it, then run 'sgpack build'");
        private static final TextKey NO_MANIFEST = TextKey.of("jsc.cli.sigma.sgpack.no_manifest",
                "no %s here; run 'sgpack init' first");
        private static final TextKey NOT_HERE = TextKey.of("jsc.cli.sigma.sgpack.not_here",
                "%s is named in the manifest but not here");
        private static final TextKey BUILT = TextKey.of("jsc.cli.sigma.sgpack.built",
                "built %s (%s files, %s bytes)");
        private static final TextKey NAME_IT = TextKey.of("jsc.cli.sigma.sgpack.name_it",
                "name the package to publish, or run this beside a %s");
        private static final TextKey UNPUBLISH_USAGE = TextKey.of("jsc.cli.sigma.sgpack.unpublish_usage",
                "unpublish <name>");

        @Override
        public String name() {
            return "sgpack";
        }

        @Override
        public CommandGroup group() {
            return CommandGroup.PROGRAMMING;
        }

        @Override
        public Text summary() {
            return SUMMARY.text();
        }

        @Override
        public Text usage() {
            return USAGE.text();
        }

        @Override
        public CommandScope scope() {
            return CommandScope.everywhere().fromPackage(RUNTIME);
        }

        @Override
        public void run(final CliContext ctx) {
            switch (ctx.arg(0).toLowerCase(Locale.ROOT)) {
                case "init" -> this.init(ctx);
                case "build" -> this.build(ctx);
                case "publish" -> this.publish(ctx);
                case "unpublish" -> this.unpublish(ctx);
                default -> {
                    ctx.out().error(CliTexts.USAGE.with(this.name(), this.usage()));
                    ctx.out().line(VERB_INIT.with(Manifest.FILE));
                    ctx.out().line(VERB_BUILD);
                    ctx.out().line(VERB_PUBLISH);
                    ctx.out().line(VERB_UNPUBLISH);
                }
            }
        }

        /** Writes a manifest for a project that has none, filled in as far as it can be guessed. */
        private void init(final CliContext ctx) {
            final ICliComputer computer = ctx.computer();
            if (computer.readFile(Manifest.FILE).ok()) {
                ctx.out().error(ALREADY_HERE.with(Manifest.FILE));
                return;
            }
            final String name = ctx.argCount() > 1 ? ctx.arg(1).toLowerCase(Locale.ROOT) : "program";
            final Manifest made = Manifest.fresh(name, computer.name().isEmpty()
                    ? "unsigned" : computer.name());
            final List<String> wrong = made.problems();
            if (!wrong.isEmpty()) {
                ctx.out().error(CliTexts.SAID_BY.with(this.name(), wrong.getFirst()));
                return;
            }
            final ICliComputer.FsResult written = computer.writeFile(Manifest.FILE, made.write());
            if (!written.ok()) {
                ctx.out().error(written.message());
                return;
            }
            ctx.out().ok(WROTE.with(Manifest.FILE));
            ctx.out().dim(THEN_BUILD);
        }

        /** Reads the manifest, gathers what it names, and writes the package out beside it. */
        private void build(final CliContext ctx) {
            final ICliComputer computer = ctx.computer();
            final ICliComputer.FsResult read = computer.readFile(Manifest.FILE);
            if (!read.ok()) {
                ctx.out().error(NO_MANIFEST.with(Manifest.FILE));
                return;
            }
            final Manifest manifest = Manifest.read(read.message().english());
            final Map<String, String> files = new LinkedHashMap<>();
            for (final String named : manifest.files()) {
                final ICliComputer.FsResult file = computer.readFile(named);
                if (!file.ok()) {
                    ctx.out().error(CliTexts.SAID_BY.with(this.name(), NOT_HERE.with(named)));
                    return;
                }
                files.put(named, file.message().english());
            }
            final Packed packed = new Packed(manifest, files);
            final List<String> wrong = packed.problems();
            if (!wrong.isEmpty()) {
                for (final String one : wrong) {
                    ctx.out().error(CliTexts.SAID_BY.with(Manifest.FILE, one));
                }
                return;
            }
            final ICliComputer.FsResult written =
                    computer.writeFile(packed.fileName(), packed.write());
            if (!written.ok()) {
                ctx.out().error(written.message());
                return;
            }
            ctx.out().ok(BUILT.with(packed.fileName(), files.size(), packed.size()));
        }

        /**
         * Puts a built package on the network's Mirror.
         *
         * <p>With no file named, it works out which one from the manifest here, so the usual way to
         * release something is two words after building it.
         */
        private void publish(final CliContext ctx) {
            String file = ctx.argCount() > 1 ? ctx.arg(1) : "";
            if (file.isEmpty()) {
                final ICliComputer.FsResult read = ctx.computer().readFile(Manifest.FILE);
                if (!read.ok()) {
                    ctx.out().error(NAME_IT.with(Manifest.FILE));
                    return;
                }
                final Manifest manifest = Manifest.read(read.message().english());
                file = new Packed(manifest, Map.of()).fileName();
            }
            final ICliComputer.OpResult published = ctx.computer().publishPackage(file);
            if (published.ok()) {
                ctx.computer().report(JscEvents.SIGMA_PUBLISHED, "");
            }
            report(ctx, published);
        }

        private void unpublish(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(this.name(), UNPUBLISH_USAGE));
                return;
            }
            report(ctx, ctx.computer().unpublishPackage(ctx.arg(1)));
        }

        private static void report(final CliContext ctx, final ICliComputer.OpResult result) {
            if (result.ok()) {
                ctx.out().ok(result.message());
            } else {
                ctx.out().error(result.message());
            }
        }
    }
}
