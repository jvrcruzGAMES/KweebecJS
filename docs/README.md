# KweebecJS Documentation

This folder contains the complete server script documentation split by category.

## Documentation Map

1. [Getting Started](./01-getting-started.md)
2. [Server Script Lifecycle](./02-server-script-lifecycle.md)
3. [Assets](./03-assets.md)
4. [Items](./04-items.md)
5. [Blocks](./05-blocks.md)
6. [Recipes](./06-recipes.md)
7. [Language Entries](./07-language.md)
8. [Reload and Runtime Asset Pack](./08-reload-and-runtime-pack.md)
9. [API Reference](./09-api-reference.md)
10. [Troubleshooting](./10-troubleshooting.md)

## Quick Start

- Create scripts in `.hytale-server/KweebecJS/server_scripts`.
- Put source assets in `.hytale-server/KweebecJS/assets`.
- Register listeners with `eventbus.addEventListener("<event>", (event) => { ... })`.
- Use `kjsreload` in game/server console after edits.
