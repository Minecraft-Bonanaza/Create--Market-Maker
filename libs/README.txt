Place Villager Commerce and Stock Market mod JARs in this directory for compile-time access.

Required for compilation (compileOnly):
- Create: Villager Commerce (VC) — drop the matching Brave New Globe version JAR here.
- Stock Market (SM) — optional at runtime, but needed here if you compile against its API.

These JARs are NOT bundled in the mod artifact. End users install them via the mod pack.

Example filenames (rename as needed):
  villagercommerce-<version>.jar
  stockmarket-<version>.jar

After adding JARs, run: .\gradlew.bat build --no-daemon
