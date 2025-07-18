# Gradle Build Migration

This project has been migrated from Apache Ant to Gradle while maintaining identical build output and cross-platform compatibility.

## Prerequisites

- Java 21 or later
- Gradle 8.0 or later installed on the system

## Available Tasks

### Main Tasks

- `gradle dist` - Build complete distribution (default task)
- `gradle clean` - Clean build artifacts
- `gradle distclean` - Remove dist directory
- `gradle jar` - Build only the JAR file

### Platform-Specific Tasks
- `copyBats` - Copy .cmd files (Windows only)
- `copyShells` - Copy .sh files (Unix/Linux/macOS only)
- `copyLicenses` - Copy license files
- `copyResources` - Copy catalog, srx, and xmlfilter directories
- `jlinkImage` - Create modular runtime image with jlink

## Usage

### Build Distribution
```bash
gradle dist
```

### Clean and Rebuild
```bash
gradle clean dist
```

### Build Only JAR
```bash
gradle jar
```

## Cross-Platform Compatibility

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
