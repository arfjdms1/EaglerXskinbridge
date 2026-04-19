# EaglerXskinbridge 

![Velocity](https://img.shields.io/badge/Velocity-3.3.0+-blue.svg)
![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![SkinsRestorer](https://img.shields.io/badge/SkinsRestorer-v15+-green.svg)
![EaglercraftX](https://img.shields.io/badge/EaglercraftX-1.8.8-red.svg)

**EaglerXskinbridge** is a highly optimized, first-of-its-kind, direct-hook Velocity plugin that seamlessly synchronizes EaglercraftX custom skins to Java Edition players. 

Historically, Java Edition players would only see Eaglercraft players as default Steves or Alexes. This plugin listens to the proxy's internal Eaglercraft events, mathematically converts the raw pixel data into standard PNGs, uploads them to the MineSkin API for official Mojang cryptographic signatures, and injects them directly into **SkinsRestorer v15**.

If you run an Eaglercraft network alongside Java players, this plugin bridges the visual gap.

## Features

* **Direct-Hook Architecture:** No Redis, no external Python workers, no databases required. Everything happens natively in the Velocity proxy memory.
* **Custom Skin Support:** Intercepts the `EaglercraftRegisterSkinEvent`, extracts the raw `ABGR8` bytes sent by the Eaglercraft client, and reconstructs them into valid PNGs.
* **Preset Skin Support:** Fully supports all 24 Eaglercraft built-in preset skins (e.g., Developer Steve, Tennis Alex, Zombie, Notch) by embedding high-quality PNGs directly into the JAR.
* **Zero Main-Thread Lag:** Completely asynchronous processing. HTTP requests and byte-shifting math are offloaded to Velocity's background scheduler to ensure 0 TPS drops during login spikes.
* **Aggressive RAM Caching:** Prevents MineSkin rate-limits by locally hashing and caching signed skin data. If 100 players log in with the "Developer Steve" skin, the API is only queried *once*.
* **SkinsRestorer v15 Native:** Uses the modern v15 API (`SkinStorage`, `PlayerStorage`, `SkinApplier`) to instantly refresh player skins across the proxy.

## Prerequisites

Ensure your proxy server meets the following requirements:
* **Proxy:** Velocity 3.3.0+
* **Java:** Java 17 or higher
* **Required Plugin 1:** [EaglercraftXServer](https://modrinth.com/plugin/eaglercraftxserver) (Velocity version)
* **Required Plugin 2:** [SkinsRestorer](https://skinsrestorer.net/) (v15.0.0 or higher)

## Installation & Setup

1. **Download or Build the Plugin:**
   Grab the latest `.jar` from the Releases tab, or build it yourself from source (see below).
2. **Install:**
   Drop `EaglerXskinbridge-1.0-SNAPSHOT.jar` into your Velocity `plugins/` directory alongside `SkinsRestorer` and `eaglerxserver`.
3. **Restart Proxy:**
   Start or restart your Velocity proxy. A new configuration folder will be generated at `plugins/eaglerxskinbridge/config.toml`.
4. **Configure MineSkin API Key (CRITICAL):**
   * Go to [MineSkin.org](https://mineskin.org/) and generate a free API key.
   * Open `plugins/eaglerxskinbridge/config.toml`.
   * Set `api_key = "your_key_here"` under the `[mineskin]` section.
   * *Why? MineSkin heavily rate-limits unauthenticated requests. If you skip this step, skin uploads will fail during moderate/heavy player joins.*
5. **Restart Again:**
   Restart Velocity to apply the API key. You are done!

## Configuration (`config.toml`)

```toml
[mineskin]
# API key used for uploads to https://api.mineskin.org/generate/upload. (Highly Recommended)
api_key = "your_api_key_here"
# User-Agent header sent with MineSkin requests.
user_agent = "EaglercraftSkinBridge/v1.0"
# Dimensions of incoming Eaglercraft ABGR8 skin payloads.
default_width = 64
default_height = 64

[settings]
# Maximum number of concurrent MineSkin upload jobs processed by this worker.
max_concurrent_jobs = 5

[cache]
# Enables internal PNG and signed skin response caching.
enabled = true
# TTL for signed MineSkin responses, in minutes (Default: 7 days).
skin_cache_ttl_minutes = 10080
# TTL for generated PNG payloads, in minutes (Default: 7 days).
png_cache_ttl_minutes = 10080
# Preserve fallback hash behavior from the original service.
fallback_hash_enabled = true
```

## Building from Source

To compile the plugin yourself, clone the repository and run the Gradle shadowJar task:

```bash
git clone https://github.com/YourName/EaglerXskinbridge.git
cd EaglerXskinbridge
./gradlew shadowJar
```

The compiled plugin will be located at `build/libs/EaglerXskinbridge-1.0-SNAPSHOT.jar`.

## Contributing

Contributions, issues, and feature requests are welcome! Since Eaglercraft and SkinsRestorer are constantly evolving, community support is appreciated.

**Potential future features to contribute:**
* FNAW (3D Model) custom skin parsing.
* Cape synchronization bridge.
* Persistent database caching (MySQL/SQLite) instead of strictly in-memory.

## License

This project is open-source and available under standard open-source licenses. Feel free to fork, modify, and use it on your network!
