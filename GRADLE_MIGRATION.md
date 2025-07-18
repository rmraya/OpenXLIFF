# Gradle Build Migration

This project has been migrated from Apache Ant to Gradle while maintaining identical build output and cross-platform compatibility.

## Prerequisites

- Java 21 or later
- Gradle 8.0 or later installed on the system

## Available Tasks

### Main Tasks

- `gradle dist` - Build complete distribution (default task, always clean)
- `gradle clean` - Clean build artifacts (removes lib/openxliff.jar, bin/, dist/, build/, .gradle/)
- `gradle forceClean` - Force clean all build artifacts and caches
- `gradle distclean` - Remove dist directory
- `gradle jar` - Build only the JAR file (always clean, removes build/ afterwards)

**Note**: The `bin/` and `build/` directories may temporarily appear during compilation but are automatically cleaned up after task completion.

### Platform-Specific Tasks
- `copyBats` - Copy .cmd files (Windows only)
- `copyShells` - Copy .sh files (Unix/Linux/macOS only)
- `copyLicenses` - Copy license files
- `copyResources` - Copy catalog, srx, and xmlfilter directories
- `jlinkImage` - Create modular runtime image with jlink

## Usage

**Important**: All Gradle commands must be run from the project root directory (where `build.gradle` is located), not from subdirectories like `dist/`.

### Build Distribution
```bash
cd /path/to/OpenXLIFF
gradle dist
```

### Clean and Rebuild
```bash
cd /path/to/OpenXLIFF
gradle clean dist
```

### Build Only JAR
```bash
cd /path/to/OpenXLIFF
gradle jar
```

## Clean Build Features

This build is configured for **always clean builds** with no caching:

- **No Build Caching**: All tasks execute fresh each time
- **No Incremental Compilation**: Java compilation is never incremental
- **Automatic Cleanup**: Build directories are cleared before compilation
- **Fresh Compilation**: Every build starts from a clean state

### Clean Build Configuration

The build includes several mechanisms to ensure clean builds:

1. **Gradle Properties**: Caching disabled in `gradle.properties`
2. **Compiler Options**: Incremental compilation disabled
3. **Task Dependencies**: Every compilation runs `clean` first
4. **Force Clean**: Available for complete cleanup of all artifacts

The build automatically detects the operating system:
- **Windows**: Copies `.cmd` files to dist/
- **Unix/Linux/macOS**: Copies `.sh` files to dist/ and makes them executable

## Output Structure

The `dist/` directory contains:
- `bin/` - Custom JRE with java executable
- `lib/` - Required JAR files (excluding jrt-fs.jar)
- `catalog/` - XML catalogs
- `srx/` - Segmentation rules
- `xmlfilter/` - XML filter configurations
- `licenses/` - License information
- `LICENSE` - Main license file
- Platform-specific scripts (`.sh` or `.cmd`)

## Differences from Ant Build

1. **System Gradle**: Uses the Gradle installation on the system
2. **Improved Task Dependencies**: Better dependency management between tasks
3. **UTF-8 Encoding**: Explicitly configured for all source files

## Migration Benefits

- **Faster Builds**: Gradle's incremental compilation and caching
- **Better IDE Integration**: Enhanced support in modern IDEs
- **Parallel Execution**: Tasks can run in parallel where possible
- **Dependency Management**: Ready for future dependency management if needed
- **Modern Tooling**: Access to Gradle's extensive plugin ecosystem
