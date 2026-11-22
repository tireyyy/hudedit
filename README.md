# VoidHotbar

Client-side Fabric mod for Minecraft 1.21.11 (Java 21). Replaces the vanilla hotbar element
(via Fabric's `HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, ...)`) with minimalist
rounded graphite slots, a violet active slot, five animation modes and a small settings screen.
No mixins, no server component, no vanilla textures modified (Sodium / Iris / resource packs unaffected).

## Build
1. Install JDK 21.
2. Generate the Gradle wrapper once (wrapper jar is not included):  `gradle wrapper --gradle-version 9.2.1`
   (or copy `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` from the official Fabric example mod).
3. `./gradlew build`  (Windows: `gradlew.bat build`)
4. JAR: `build/libs/voidhotbar-1.0.0.jar` (not the `-sources` one). Put it into `mods/` together with Fabric API.

Dev run: `./gradlew runClient`.

## Use
Press **K** in-game (rebindable: Controls -> VoidHotbar) to open settings. Colour fields accept
`#RRGGBB`, `RRGGBB` or `R,G,B`. Config file: `config/voidhotbar.json`.
