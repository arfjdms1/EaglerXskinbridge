# EaglerX SkinBridge
Standalone Kotlin service for converting Eaglercraft `ABGR8` skins into PNGs, uploading them to MineSkin, and broadcasting signed texture payloads over Redis Pub/Sub.

## Overview
Eaglercraft skins are not native Minecraft Java signed textures. This service bridges that gap by:

- converting incoming Eaglercraft skin bytes from `ABGR8` into standard PNG data
- uploading the generated PNG to the MineSkin upload API
- caching generated PNGs and signed texture responses in Redis
- publishing completed skin signatures across the network through Redis Pub/Sub

The project no longer depends on the private `gg.scala.*` framework and builds as a plain JVM application with Gradle.

## Build
```bash
./gradlew shadowJar
```

The shaded output jar is written to `build/libs/`.

## Configuration
On first startup the application generates a `config.toml` file automatically if one does not already exist.

Relevant sections:

- `[mineskin]` for MineSkin API credentials and upload settings
- `[redis]` for Redis connection details and Pub/Sub channel names
- `[settings]` for worker concurrency
- `[cache]` for cache toggles and TTL values

`config.toml` is gitignored because it can contain secrets.

## Run
```bash
java -jar build/libs/archoss-skinbridge-1.0-SNAPSHOT.jar
```

The worker starts a Redis subscriber for conversion jobs and stays alive in the background until terminated.

## Notes
- Core ABGR8 byte-to-PNG conversion logic is preserved from the original implementation.
- The service uses `OkHttp` for MineSkin uploads, `Gson` for JSON parsing, `Jedis` for Redis access, and `toml4j` for configuration parsing.
