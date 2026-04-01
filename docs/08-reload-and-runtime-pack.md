# Reload and Runtime Asset Pack

## Reload Command

- Primary: `kweebecjsreload`
- Alias: `kjsreload`

It reloads only KweebecJS-generated content.

## What Reload Does

1. Clears current script listeners.
2. Clears generated recipes/lang/items/blocks state.
3. Clears `runtime_assets/`.
4. Clears in-memory loaded `Asset.load(...)` cache.
5. Re-executes `server_scripts`.
6. Regenerates runtime files.
7. Updates `runtime_assets.zip`.
8. Re-registers only the KweebecJS runtime asset pack.

## Asset Pack Notes

- KweebecJS manages:
  - `.hytale-server/KweebecJS/runtime_assets/`
  - `.hytale-server/KweebecJS/runtime_assets.zip`
- The mod does not need to modify the base Hytale assets zip.
- `CommonAssetsIndex.hashes` is generated from runtime `Common/` files to keep the pack immutable in the asset system.

## Performance/Flicker Notes

- Runtime pack updates are synchronized incrementally.
- Reload is scoped to KweebecJS runtime content.
- It should not intentionally reload global Hytale asset sources.
