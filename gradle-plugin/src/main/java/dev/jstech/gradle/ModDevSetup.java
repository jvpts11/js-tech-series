/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.gradle;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.moddevgradle.dsl.ModDevExtension;
import net.neoforged.moddevgradle.dsl.ModModel;
import net.neoforged.moddevgradle.dsl.RunModel;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

/**
 * What the Core's plugin sets up in ModDevGradle's model once it is applied: the mod, of its main source set and its
 * tests', and its development runs, each made only when the build has none of that name, so a build's own runs win.
 * Reached only once ModDevGradle is applied, so a build without it never loads this class.
 */
final class ModDevSetup {

    /** The runs set up, by name. */
    static final List<String> RUNS = List.of("client", "server", "gameTestServer", "data", "clientTests");

    private static final String NEOFORGE = "neoForge";
    /* What ModDevGradle registers once a build enables modding by setting its version. */
    private static final String ENABLED = "__internal_modDevArtifactsWorkflow";

    private ModDevSetup() {
    }

    static void configure(final Project project, final JsTechExtension jstech, final SourceSet main,
                          final SourceSet gametest) {
        final ModDevExtension modDev = (ModDevExtension) project.getExtensions().getByName(NEOFORGE);
        // The game reaches the tests' source set once the build has set its version, below the plugins.
        project.afterEvaluate(evaluated -> {
            if (evaluated.getExtensions().findByName(ENABLED) != null) {
                modDev.addModdingDependenciesTo(gametest);
            }
        });
        final String modId = jstech.getModId().get();
        final ModModel mod = modDev.getMods().maybeCreate(modId);
        mod.sourceSet(main);
        mod.sourceSet(gametest);
        final NamedDomainObjectContainer<RunModel> runs = modDev.getRuns();
        made(runs, "client", run -> run.client());
        made(runs, "server", run -> {
            run.server();
            run.programArgument("--nogui");
        });
        made(runs, "gameTestServer", run -> {
            run.getType().set("gameTestServer");
            run.getSourceSet().set(gametest);
            run.getSystemProperties().put("neoforge.enabledGameTestNamespaces", jstech.getModId());
        });
        made(runs, "data", run -> {
            run.data();
            run.getProgramArguments().addAll(jstech.getModId().map(id -> List.of("--mod", id, "--all",
                    "--output", project.file("src/generated/resources").getAbsolutePath(),
                    "--existing", project.file("src/main/resources").getAbsolutePath())));
        });
        made(runs, "clientTests", run -> {
            run.client();
            run.getSourceSet().set(gametest);
            run.systemProperty("jstestkit.clienttests", "true");
            run.getGameDirectory().set(project.getLayout().getProjectDirectory().dir("run/clienttests"));
        });
    }

    /** The names of the runs the build has, or none when ModDevGradle is not applied. */
    static List<String> runNames(final Project project) {
        final Object found = project.getExtensions().findByName(NEOFORGE);
        if (!(found instanceof ModDevExtension modDev)) {
            return List.of();
        }
        final List<String> names = new ArrayList<>(modDev.getRuns().getNames());
        names.sort(String::compareTo);
        return names;
    }

    private static void made(final NamedDomainObjectContainer<RunModel> runs, final String name,
                             final RunSetup setup) {
        if (runs.findByName(name) == null) {
            runs.create(name, setup::setUp);
        }
    }

    /* How one run is set up. */
    @FunctionalInterface
    private interface RunSetup {

        void setUp(RunModel run);
    }
}
