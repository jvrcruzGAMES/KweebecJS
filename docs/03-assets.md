# Assets

## Global Helpers

- `Asset.load(relativePath, AssetTarget.X)`
- `AssetTarget` enum values:
  - `ITEM_MODEL`
  - `ITEM_TEXTURE`
  - `ITEM_ICON`
  - `BLOCK_MODEL`
  - `BLOCK_TEXTURE`
  - `BLOCK_ITEM_ICON`
  - `PARTICLE_TEXTURE`

## Supported Extensions

- Model: `.blockymodel`
- Texture: `.png`
- `.json` is explicitly rejected

## Path Rules

- Path must be relative to `.hytale-server/KweebecJS/assets`.
- Absolute paths are rejected.
- Escaping (`..`) is rejected.
- `assets/...` prefix is normalized away.

## Loading Semantics

- Assets are copied only when `Asset.load(...)` is called.
- Calls are idempotent for the same pair:
  - `AssetTarget`
  - normalized relative path
- Same asset is not re-copied unless content differs.

## Runtime Pack Target Directories

`AssetTarget` controls where files go inside `Common/`:

- `ITEM_MODEL` -> `Items/KweebecJS/Models/...`
- `ITEM_TEXTURE` -> `Items/KweebecJS/Textures/...`
- `ITEM_ICON` -> `Icons/ItemsGenerated/KweebecJS/...`
- `BLOCK_MODEL` -> `Blocks/KweebecJS/Models/...`
- `BLOCK_TEXTURE` -> `Blocks/KweebecJS/Textures/...`
- `BLOCK_ITEM_ICON` -> `Icons/ItemsGenerated/KweebecJS/...`
- `PARTICLE_TEXTURE` -> `Particles/KweebecJS/Textures/...`

## Example

```js
const swordModel = Asset.load("void_Sword/void_Mithril.blockymodel", AssetTarget.ITEM_MODEL);
const swordTexture = Asset.load("void_Sword/void_Mithril_Texture.png", AssetTarget.ITEM_TEXTURE);
const swordIcon = Asset.load("void_Sword/Weapon_Sword_Void_Mithril.png", AssetTarget.ITEM_ICON);
```
