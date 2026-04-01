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

Server script API documentation is available in [DOCS.md](DOCS.md).

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
