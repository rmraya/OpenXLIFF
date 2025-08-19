
# Gradle Build Migration

This project has been migrated from Apache Ant to Gradle while maintaining identical build output and cross-platform compatibility.

## Prerequisites

- Java 21 or later
- Gradle 9.0 or later installed on the system

## Available Tasks

- `gradle dist` — Build complete distribution (default task)
- `gradle clean` — Clean build artifacts
- `gradle jar` — Build only the JAR file
- `gradle distclean` — Remove only the `dist` directory
- `gradle forceClean` — Force clean all artifacts including `.class` files

## Usage

**Important:** Run all commands from the project root directory.

### Build Distribution
```bash
gradle dist
```

### Clean and Rebuild
```bash
gradle clean dist
```

## Key Features

- **Always Clean Builds:** Every build starts fresh with no caching
- **Cross-Platform:** Automatically detects Windows/Unix and copies appropriate scripts
- **Modular Runtime:** Creates custom JRE with jlink in `dist/bin/`

## Output Structure

The `dist/` directory contains:
- `bin/` — Custom JRE with java executable
- `lib/` — Required JAR files
- `catalog/`, `srx/`, `xmlfilter/` — Configuration files
- `licenses/`, `LICENSE` — License information
- Platform-specific scripts (`.sh` or `.cmd`)

## Migration from Ant

| Ant Command | Gradle Equivalent |
|-------------|-------------------|
| `ant dist` | `gradle dist` |
| `ant clean` | `gradle clean` |
| `ant compile` | `gradle jar` |
| `ant distclean` | `gradle distclean` |

### Key Improvements
- **Efficient File Copying:** Uses glob patterns instead of copying files individually
- **Better Task Dependencies:** Improved build orchestration with `mustRunAfter`
- **Enhanced Cleanup:** Automatic removal of temporary directories
- **Modern Tooling:** Better IDE integration and plugin ecosystem
- **Cross-Platform jlink:** Automatic path separator detection
