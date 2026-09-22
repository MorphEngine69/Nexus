# Contributing

## Branches

| Branch | Purpose |
|---|---|
| `main` | Stable state; releases are cut from here. Protected. |
| `dev` | Integration branch and the base for every task. Protected. |

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
in `gradle.properties`. A release is cut from `main` by pushing a `v{version}` tag;
the release workflow builds the jars and publishes a GitHub Release containing the
matching `CHANGELOG.md` section.
