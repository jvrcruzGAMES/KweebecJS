# KweebecJS

A Hytale server mod built with Java.

## License

This project is proprietary and released under the custom
[`KweebecJS All Rights Reserved`](LICENSE.md).

Source code inspection is allowed, but distribution of the software, source
code, forks, patches, or modified versions is prohibited unless the recipient
is explicitly listed in [EXEMPTIONS.md](EXEMPTIONS.md) under a written License
Exemption from the copyright holder.

## Documentation

Server script API documentation is available in:

- [docs/README.md](docs/README.md) (category index)
- [DOCS.md](DOCS.md) (legacy entry point)

## Mod Features Summary

- JavaScript event system for runtime mod content: `lang:register`, `items:register`, `blocks:register`, `recipes:register`.
- Runtime asset pipeline based on `runtime_assets/` and in-game loading through asset pack registration (no zipfs dependency).
- Typed asset loading from JS via `Asset.load(path, AssetTarget.X)` with extension/type validation and idempotent caching.
- Item registration and partial/full overrides with builders:
  - `ItemEntry`, `ItemProperties`, `ItemPropertiesOverride`
  - `ItemSpecialProperties`, `ItemSpecialPropertiesOverride`
- Block registration and partial/full overrides with builders:
  - `BlockProperties`, `BlockPropertiesOverride`
  - `BlockSpecialProperties`, `BlockSpecialPropertiesOverride`
  - `BlockObject`/`BlockArray` helpers for complex structured properties
- Automatic `KweebecJS_` namespacing in Java for generated custom block/item IDs.
- Optional auto-generated block item support (with configurable icon and ability to disable generation).
- Runtime language entry generation through `lang:register`.
- Runtime recipe add/override/delete patching through `recipes:register` with workbench/category helpers.
- Reload command (`kweebecjsreload` / `kjsreload`) that fully undoes and rebuilds runtime-generated content.

## Building

```bash
./gradlew shadowJar
```

The output JAR will be in `build/libs/`.

## Deploying

```bash
./gradlew deployMod
```

Builds the fat JAR and copies it to the Hytale server `mods/` folder.

## Running

Use the included run configurations in your IDE:

- **Run Hytale Server** - Builds, deploys, and starts the server
- **Debug Hytale Server** - Same as Run, with remote debugger on port 5005
- **Build Mod** - Compiles without deploying or starting the server
