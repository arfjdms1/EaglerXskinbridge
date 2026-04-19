# Versioning & Committing Guidelines

To maintain a clean and understandable history for **EaglerXskinbridge**, please adhere to the following versioning and commit message standards.

## 📌 Versioning System (SemVer)

We use [Semantic Versioning (SemVer)](https://semver.org/) for release versions. 
Version numbers are formatted as **`vMAJOR.MINOR.PATCH`** (e.g., `v1.0.0`).

* **MAJOR (`v1.0.0` -> `v2.0.0`):** Incompatible API changes, major architectural rewrites, or dropping support for older Java/Velocity/SkinsRestorer versions.
* **MINOR (`v1.0.0` -> `v1.1.0`):** Adding new features in a backward-compatible manner (e.g., adding FNAW 3D skin support, Cape support).
* **PATCH (`v1.0.0` -> `v1.0.1`):** Backward-compatible bug fixes, performance improvements, or minor logging changes.

*Note: Append `-SNAPSHOT` to the version in `build.gradle.kts` and `velocity-plugin.json` for development builds that are not yet officially released (e.g., `1.1.0-SNAPSHOT`).*

## 💬 Commit Message Convention

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. This makes it easy to automatically generate changelogs and understand the project history at a glance.

**Format:**
```text
<type>(<optional scope>): <description>

[optional body]

[optional footer(s)]
```

### Allowed Types:
* **`feat:`** A new feature (correlates with a MINOR version bump).
* **`fix:`** A bug fix (correlates with a PATCH version bump).
* **`docs:`** Documentation only changes (e.g., updating `README.md`).
* **`style:`** Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc).
* **`refactor:`** A code change that neither fixes a bug nor adds a feature.
* **`perf:`** A code change that improves performance.
* **`test:`** Adding missing tests or correcting existing tests.
* **`chore:`** Changes to the build process or auxiliary tools and libraries.

### Examples:
* `feat: add support for Eaglercraft capes`
* `fix(cache): resolve issue where cache TTL was ignored`
* `docs: update setup instructions in README`
* `chore: bump SkinsRestorer API dependency to v15.13.0`

### Rules:
1. **Use the imperative mood** in the description (e.g., "add feature" not "added feature").
2. **Keep the first line under 72 characters.**
3. **Reference issues** in the footer if applicable (e.g., `Fixes #12`).
