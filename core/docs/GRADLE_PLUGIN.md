# The Gradle plugin

How a mod built on J's Core sets its build up with one line: the Core's Gradle plugin, `dev.jstech.core`.

## What it does

*Added 2026-10-21.*

Applied beside ModDevGradle, the plugin gives the mod:

- **The dependency**: J's Core in `implementation`, at the plugin's own version unless told another, and the Core's
  test kit ([Testing](TESTING.md)) where the tests are.
- **A source set for the tests**, `gametest` (`src/gametest/java`): the GameTests and client tests, compiled
  against the mod and the test kit, loaded by the runs that test, and never in the mod's jar.
- **The runs**: `client`, `server`, `data`, `gameTestServer` (the tests' source set, the mod's GameTests) and
  `clientTests` (the client tests, in `run/clienttests`). A run the build already declares is left as it is.
- **The data generation**: the data run writes to `src/generated/resources`, which the mod's resources include, and
  `checkGeneratedData` runs it and fails when it wrote other files than the ones kept.
- **The check of the API**: `checkApiSurface`, part of `check`, follows every name the mod's promised faces say
  and compares the API with its photo in `src/test/resources/api` ([Testing](TESTING.md#the-apis-surface)).
- **The licence headers**: `checkLicenceHeaders`, part of `check`, fails when a source has no licence header naming
  the mod's licence; `addLicenceHeaders` writes the header above every source that has none.
- `jstechInfo` says what the plugin set up.

## Setting it up

*Added 2026-10-21.*

For now the plugin, the Core and the test kit are published to the local Maven repository only:

```
./gradlew :core:publishToMavenLocal :testkit:publishToMavenLocal :gradle-plugin:publishToMavenLocal
```

`settings.gradle`:

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

`build.gradle`:

```groovy
plugins {
    id 'net.neoforged.moddev' version '2.0.141'
    id 'dev.jstech.core' version '0.6.0a'
}

neoForge {
    version = '21.1.248'
}
```

`gradle.properties` gives the plugin what it needs, each also settable in a `jstech { }` block:

| Property | `jstech` | What it is |
|---|---|---|
| `mod_id` | `modId` | the mod's id, for its runs and its GameTests' namespace; the project's name when absent |
| `mod_name` | `modName` | the work a licence header says a file is part of |
| `mod_license` | `licence` | the SPDX id every header names; with none, any header passes |
| `mod_authors` | `holder` | whose the copyright is, in a header written new |
| | `coreVersion` | the Core and test kit's version; the plugin's own |
| | `apiPhotos` | the folder of the API's photo; `src/test/resources/api` |
| | `apiClass` | how the API class's file is named; any `*Api.java` |

The mod's id is read as the plugin is applied, so it is set in `gradle.properties` rather than in the block.

## What can go wrong

- **`dev.jstech:core` or `dev.jstech:testkit` cannot be found.** They are not published to the local Maven
  repository at the plugin's version: publish them, or set `jstech.coreVersion` to one that is.
- **`checkApiSurface` fails on a mod that promises nothing yet.** A source sits in a package named `api`, which is
  the promise; move it out, or keep the promise with a photo.
- **The tests' classes are not found by the GameTest server.** They are in `src/main/java` instead of
  `src/gametest/java`, or the build declares its own `gameTestServer` run that loads the main source set only.
