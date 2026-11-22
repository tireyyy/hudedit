# VoidSky (Stage 1) — Fabric, Minecraft 1.21.11

Client-side mod: black space sky (ignores day/night) with a GPU-procedural black hole + accretion rings,
and planets (Earth, Venus, Mars, Saturn with rings, Deep Space). Menu key: **V** (rebindable).

## Build via GitHub
1. Create a repository, upload the contents of this folder (keep `.github/`).
2. Open **Actions → Build VoidSky → Run workflow** (or just push).
3. When it finishes, download the `voidsky-jar` artifact — it contains `voidsky-0.1.0.jar`.
4. Put the jar and Fabric API (0.141.x+1.21.11) into `mods/`.

## Build locally
Gradle 8.14+ and JDK 21: `gradle build` → `build/libs/voidsky-0.1.0.jar`.

## Status
Stage 2 is unverified by a compiler. If the build fails, copy the error log back to the assistant.
