/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.gradle;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * The Core's Gradle plugin, {@code dev.jstech.core}: a mod built on J's Core applies it beside ModDevGradle and gets
 * the Core as a dependency and its test kit in its tests, the development runs, the data generation into
 * {@code src/generated/resources} with its check, the check of its API against its photo, and its licence headers.
 *
 * <pre>{@code
 * plugins {
 *     id 'net.neoforged.moddev' version '2.0.141'
 *     id 'dev.jstech.core' version '0.6.0a'
 * }
 * }</pre>
 *
 * <p>Its GameTests and client tests live in a source set of their own, {@code gametest}, which the runs that test load
 * with the test kit and which the mod's jar never holds.
 */
public final class JsTechPlugin implements Plugin<Project> {

    /** The group the Core and its test kit are published under. */
    public static final String GROUP = "dev.jstech";
    /** The source set the GameTests and client tests live in. */
    public static final String GAMETEST = "gametest";
    /** The configuration the plugin's checks take the test kit's own classes from. */
    public static final String TOOLS = "jstechTools";

    private static final String GENERATED = "src/generated/resources";
    private static final String VERIFICATION = LifecycleBasePlugin.VERIFICATION_GROUP;

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        final JsTechExtension jstech = project.getExtensions().create("jstech", JsTechExtension.class);
        defaults(project, jstech);
        project.getRepositories().mavenLocal(local -> local.content(content -> content.includeGroup(GROUP)));
        final SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class)
                .getSourceSets();
        final SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        main.getResources().srcDir(GENERATED);
        main.getResources().exclude(GENERATED + "/.cache/**", ".cache/**");
        final SourceSet gametest = sourceSets.create(GAMETEST);
        dependencies(project, jstech, main, gametest);
        final Configuration tools = project.getConfigurations().create(TOOLS, configuration -> {
            configuration.setCanBeConsumed(false);
            configuration.setDescription("The test kit's classes the Core's plugin runs its checks with.");
        });
        project.getDependencies().addProvider(TOOLS, testKit(jstech), JsTechPlugin::alone);
        licenceTasks(project, jstech, sourceSets);
        apiTask(project, jstech, main, tools);
        dataTasks(project, tools);
        final TaskProvider<JsTechInfo> info = project.getTasks().register("jstechInfo", JsTechInfo.class, task -> {
            task.setGroup("help");
            task.setDescription("Says what the Core's plugin set the mod up with.");
            task.getModId().set(jstech.getModId());
            task.getCore().set(core(jstech));
            task.getTestKit().set(testKit(jstech));
            task.getRuns().set(List.of());
        });
        project.getPluginManager().withPlugin("net.neoforged.moddev", applied -> {
            ModDevSetup.configure(project, jstech, main, gametest);
            info.configure(task -> task.getRuns().set(project.provider(() -> ModDevSetup.runNames(project))));
        });
    }

    /** The plugin's own version: the version of J's Core it sets a mod on unless told another. */
    public static String version() {
        final Properties properties = new Properties();
        try (InputStream in = JsTechPlugin.class.getResourceAsStream("plugin.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties.getProperty("version", "unknown");
    }

    private static void defaults(final Project project, final JsTechExtension jstech) {
        jstech.getModId().convention(property(project, "mod_id").orElse(project.getName()));
        jstech.getModName().convention(property(project, "mod_name").orElse(jstech.getModId()));
        jstech.getCoreVersion().convention(version());
        jstech.getLicence().convention(property(project, "mod_license"));
        jstech.getHolder().convention(property(project, "mod_authors"));
        jstech.getApiPhotos().convention(project.getLayout().getProjectDirectory().dir("src/test/resources/api"));
        jstech.getApiClass().convention("\\w+Api\\.java");
    }

    private static void dependencies(final Project project, final JsTechExtension jstech, final SourceSet main,
                                     final SourceSet gametest) {
        project.getConfigurations().getByName(gametest.getImplementationConfigurationName())
                .extendsFrom(project.getConfigurations().getByName(main.getImplementationConfigurationName()));
        project.getDependencies().add(gametest.getImplementationConfigurationName(), main.getOutput());
        project.getDependencies().addProvider(main.getImplementationConfigurationName(), core(jstech));
        project.getDependencies().addProvider(gametest.getImplementationConfigurationName(), testKit(jstech));
        // The unit tests reach the kit's checks alone, none of the game it is built against.
        project.getDependencies().addProvider(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, testKit(jstech),
                JsTechPlugin::alone);
    }

    private static void licenceTasks(final Project project, final JsTechExtension jstech,
                                     final SourceSetContainer sourceSets) {
        final FileCollection sources = project.files(project.provider(() -> sourceSets.stream()
                .map(set -> set.getJava().getAsFileTree()).toList()));
        final TaskProvider<CheckLicenceHeaders> check = project.getTasks().register("checkLicenceHeaders",
                CheckLicenceHeaders.class, task -> {
                    task.setGroup(VERIFICATION);
                    task.setDescription("Fails when a source has no licence header naming the mod's licence.");
                    task.getSources().from(sources);
                    task.getLicence().set(jstech.getLicence());
                });
        project.getTasks().register("addLicenceHeaders", AddLicenceHeaders.class, task -> {
            task.setDescription("Writes the mod's licence header above every source that has none.");
            task.getSources().from(sources);
            task.getLicence().set(jstech.getLicence());
            task.getHolder().set(jstech.getHolder());
            task.getWork().set(jstech.getModName());
        });
        project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure(task -> task.dependsOn(check));
    }

    private static void apiTask(final Project project, final JsTechExtension jstech, final SourceSet main,
                                final Configuration tools) {
        final Provider<Directory> proposals = project.getLayout().getBuildDirectory().dir("api-photo");
        final TaskProvider<JavaExec> api = project.getTasks().register("checkApiSurface", JavaExec.class, task -> {
            task.setGroup(VERIFICATION);
            task.setDescription("Fails when the mod's API reaches past its promise or is not what its photo says.");
            task.setClasspath(tools);
            task.getMainClass().set("dev.jstech.testkit.check.ApiSurface");
            task.getArgumentProviders().add(() -> List.of(jstech.getModId().get(),
                    main.getJava().getSrcDirs().iterator().next().getAbsolutePath(),
                    jstech.getApiPhotos().get().getAsFile().getAbsolutePath(),
                    proposals.get().getAsFile().getAbsolutePath(), jstech.getApiClass().get()));
        });
        project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure(task -> task.dependsOn(api));
    }

    private static void dataTasks(final Project project, final Configuration tools) {
        final Provider<Directory> before = project.getLayout().getBuildDirectory().dir("generated-data-before");
        final TaskProvider<Sync> snapshot = project.getTasks().register("snapshotGeneratedData", Sync.class,
                task -> {
                    task.from(project.file(GENERATED), spec -> spec.exclude(".cache/**"));
                    task.into(before);
                });
        project.getTasks().matching(task -> task.getName().equals("runData"))
                .configureEach(task -> task.mustRunAfter(snapshot));
        project.getTasks().register("checkGeneratedData", JavaExec.class, task -> {
            task.setGroup(VERIFICATION);
            task.setDescription("Runs the data generation and fails when it wrote other files than the ones kept.");
            task.dependsOn(snapshot, project.getTasks().matching(other -> other.getName().equals("runData")));
            task.setClasspath(tools);
            task.getMainClass().set("dev.jstech.testkit.check.GeneratedData");
            task.args(before.get().getAsFile().getAbsolutePath(), project.file(GENERATED).getAbsolutePath());
        });
    }

    private static Provider<String> core(final JsTechExtension jstech) {
        return jstech.getCoreVersion().map(version -> GROUP + ":core:" + version);
    }

    private static Provider<String> testKit(final JsTechExtension jstech) {
        return jstech.getCoreVersion().map(version -> GROUP + ":testkit:" + version);
    }

    private static Provider<String> property(final Project project, final String name) {
        return project.getProviders().gradleProperty(name);
    }

    private static void alone(final Object dependency) {
        if (dependency instanceof ModuleDependency module) {
            module.setTransitive(false);
        }
    }
}
