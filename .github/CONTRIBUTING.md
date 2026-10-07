# Contributing

## Branches

| Branch | Purpose |
|---|---|
| `main` | Stable state; releases are cut from here. Protected. |
| `dev` | Integration branch and the base for every task. Protected. |
| `mc/{version}` | An older Minecraft version, such as `mc/26.2`, kept after `main` and `dev` moved on. It gets fixes only. |

Work happens on branches named `{type}/GH-{issue}/{kebab-description}`, at most 50
characters. `type` is one of `feat`, `fix`, `refactor`, `perf`, `docs`, `test`,
`build`, `ci`, `chore`. Use `NO-ISSUE` when there is no issue.

```
feat/GH-12/fluid-pipes
chore/NO-ISSUE/update-gradle
```

Branches are kept up to date by rebasing, never by merging. Do not push directly to
`main` or `dev`; open a pull request instead.

## Commits

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
and are written in English:

```
type(scope): short imperative summary
```

The summary is at most 72 characters, lowercase, without a trailing period. Add a
body only to explain *why* a change was made. One commit, one logical change.

## Pull requests

- Target `dev`. Only release merges go into `main`.
- Keep the pull request small and focused.
- Reference the issue it closes (`Closes #12`).
- The build must be green before merging.
- Update `CHANGELOG.md` for anything a player can notice; purely technical changes
  need no entry.

## Code style

Checkstyle runs as part of the build (`config/checkstyle/checkstyle.xml`) and fails
it on violations. Import the same file into your IDE.

Run everything before opening a pull request:

```bash
./gradlew build
```

## Releases

Versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) and live
in `gradle.properties`. A release is cut from `main` (or from `mc/{version}` for an older
Minecraft version) by pushing a `{loader}-{minecraft}-v{version}` tag, for example
`neoforge-26.2-v1.0.0-beta.1`; the release workflow builds the jar and publishes a GitHub
Release containing the matching `CHANGELOG.md` section. A version with a hyphen, such as
`1.0.0-beta.1`, is published as a pre-release. The jar is named
`nexus-{loader}-{minecraft}-{version}.jar`.
