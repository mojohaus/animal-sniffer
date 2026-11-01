# Agent guide for animal-sniffer

This file is for automated agents and human contributors working on this repository. It records
what the code and the POMs do not make obvious.

## Modules

|             Module             |                                       Contents                                       |
|--------------------------------|--------------------------------------------------------------------------------------|
| `animal-sniffer`               | The signature builder and checker; the only module with unit tests                   |
| `animal-sniffer-annotations`   | The `@IgnoreJRERequirement` annotation                                               |
| `animal-sniffer-maven-plugin`  | The Maven goals, with the integration tests in `src/it`                              |
| `animal-sniffer-enforcer-rule` | The Maven Enforcer rule, with its own integration tests                              |
| `animal-sniffer-ant-tasks`     | The Ant tasks, with integration tests (the invoker run is skipped in the module POM) |
| `java-boot-classpath-detector` | Finds the boot class path of a given Java home                                       |

## Build gates

- Main code must use the Java 8 API: mojo-parent sets `maven.compiler.release` to 8.
- Spotless runs on JDK 17 and later only. It applies the formatting in `process-sources` on a local
  build and checks it when the `CI` environment variable is set. It formats Java with
  palantir-java-format and also reformats Markdown, so run `mvn spotless:apply` after editing the
  README or this file.
- The plugin and the enforcer rule compile against Maven 3.9.16 (`mavenVersion`). The plugin
  declares a literal `<prerequisites><maven>3.9.0</maven>`; keep the two apart, so raising
  `mavenVersion` does not raise the prerequisite. Building the project itself needs Maven 3.9
  (`minimalMavenBuildVersion`).
- `.github/workflows/maven.yml` calls the shared `apache/maven-gh-actions-shared`
  `maven-verify.yml@v5` workflow. Branches in this repository build under the push event.

## Unit tests

- JUnit 5 only, in `animal-sniffer` (`SignatureCheckerTest`). The other modules are tested through
  integration tests.
- Prefer a unit test. Add an integration test only for what needs a real Maven build: toolchains,
  multi-module projects, dependency resolution or the plugin lifecycle.

## Integration tests

- The plugin's tests live in `animal-sniffer-maven-plugin/src/it`, one directory per test. They
  resolve from a local repository that the invoker plugin fills in `target/local-repo`, through
  `src/it/settings.xml`.
- Install the other modules first, then run a subset from the plugin module:

  ```shell
  mvn install -DskipTests -Dinvoker.skip
  cd animal-sniffer-maven-plugin
  mvn -P run-its verify -Dinvoker.test='fields,smoke-test,setup-*'
  ```

  `-Dinvoker.test` filters the `setup-*` projects too, so list the ones your tests need. `setup-parent`
  is the parent POM of the tests that use it.

- They run in parallel, one per CPU core (`invoker.parallelThreads` 1C), so a test must not depend
  on another test's output.

- The forked builds get `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 -XX:+UseSerialGC`, so every JVM
  prints a `Picked up JAVA_TOOL_OPTIONS` line on stderr.

- Write `verify.groovy` as `assert` statements. Invoker fails a script whose last expression is
  anything other than `true` or `null`, so a trailing `list.each { }` fails the test; use a `for`
  loop.

## Dependencies

- Deliberate pins in `.github/dependabot.yml`: the Maven artifacts stay below 3.10.0, the Maven
  3.9.x line being the plugin baseline.

## Pull requests

- Pull requests are squash- or rebase-merged. Release Drafter builds the release notes from the
  titles and labels, so the title should describe the change for a user.

