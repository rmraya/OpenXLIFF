
# Gradle Build Migration

This project has been migrated from Apache Ant to Gradle while maintaining identical build output and cross-platform compatibility.

## Prerequisites

- Java 21 or later
- Gradle 8.0 or later installed on the system

## Available Tasks

### Main Tasks

- `gradle dist` — Build complete distribution (default task, always clean)
- `gradle clean` — Clean build artifacts (`lib/openxliff.jar`, `bin/`, `dist/`, `build/`, `.gradle/`)
- `gradle forceClean` — Force clean all build artifacts and caches (removes all build output and any stray `.class` files)
- `gradle distclean` — Remove only the `dist` directory
- `gradle jar` — Build only the JAR file (always clean, removes `build/` afterwards)

**Note:** The `bin/` and `build/` directories may temporarily appear during compilation but are automatically cleaned up after task completion.

### Platform-Specific Tasks

- `gradle copyBats` — Copy `.cmd` files to `dist/` (Windows only)
- `gradle copyShells` — Copy `.sh` files to `dist/` and make them executable (Unix/Linux/macOS only)
- `gradle copyLicenses` — Copy license files to `dist/licenses/`
- `gradle copyResources` — Copy `catalog/`, `srx/`, `xmlfilter/`, and `LICENSE` to `dist/`
- `gradle jlinkImage` — Create modular runtime image with jlink in `dist/`

## Usage

**Important:** All Gradle commands must be run from the project root directory (where `build.gradle` is located), not from subdirectories like `dist/`.

### Build Distribution

    cd /path/to/OpenXLIFF
    gradle dist

### Clean and Rebuild

    cd /path/to/OpenXLIFF
    gradle clean dist

### Build Only JAR

    cd /path/to/OpenXLIFF
    gradle jar

## Clean Build Features

This build is configured for **always clean builds** with no caching:

- **No Build Caching:** All tasks execute fresh each time (`gradle.properties` disables caching)
- **No Incremental Compilation:** Java compilation is never incremental
- **Automatic Cleanup:** Build directories are cleared before compilation and after tasks
- **Fresh Compilation:** Every build starts from a clean state

### Clean Build Configuration

The build includes several mechanisms to ensure clean builds:

1. **Gradle Properties:** Caching disabled in `gradle.properties`
2. **Compiler Options:** Incremental compilation disabled
3. **Task Dependencies:** Every compilation runs `clean` first
4. **Force Clean:** Available for complete cleanup of all artifacts

The build automatically detects the operating system:

- **Windows:** Copies `.cmd` files to `dist/`
- **Unix/Linux/macOS:** Copies `.sh` files to `dist/` and makes them executable

## Output Structure

The `dist/` directory contains:

- `bin/` — Custom JRE with java executable (from jlink)
- `lib/` — Required JAR files (excluding `jrt-fs.jar`)
- `catalog/` — XML catalogs
- `srx/` — Segmentation rules
- `xmlfilter/` — XML filter configurations
- `licenses/` — License information
- `LICENSE` — Main license file
- Platform-specific scripts (`.sh` or `.cmd`)

## Differences from Ant Build

1. **System Gradle:** Uses the Gradle installation on the system
2. **Improved Task Dependencies:** Better dependency management between tasks
3. **UTF-8 Encoding:** Explicitly configured for all source files
4. **Resource Copying:** Resource copying is split into separate tasks for clarity
5. **Extra Cleaning Tasks:** `forceClean` and `cleanupBuildDir` provide more aggressive cleanup than Ant

## Migration Benefits

- **Faster Builds:** Gradle can support incremental compilation and caching (disabled here for clean builds)
- **Better IDE Integration:** Enhanced support in modern IDEs
- **Parallel Execution:** Tasks can run in parallel where possible
- **Dependency Management:** Ready for future dependency management if needed
- **Modern Tooling:** Access to Gradle's extensive plugin ecosystem
