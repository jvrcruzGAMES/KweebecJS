# Troubleshooting

## `anyPatch must be ...`

Cause:

- `event.override(id, anyPatch)` received a wrong object type.

Fix:

- For items use one of:
  - `ItemProperties`
  - `ItemPropertiesOverride`
  - `ItemSpecialProperties`
  - `ItemSpecialPropertiesOverride`
- For blocks use one of:
  - `BlockProperties`
  - `BlockPropertiesOverride`
  - `BlockSpecialProperties`
  - `BlockSpecialPropertiesOverride`

## `Common Asset ... doesn't exist`

Cause:

- Referenced model/texture/icon path in generated JSON does not match copied runtime path.

Fix:

- Always use `Asset.load(...)` for all referenced assets.
- Ensure correct `AssetTarget` is used for each field.

## Item shows translation key instead of text

Cause:

- Missing lang entry or wrong key/locale.

Fix:

- Add both locale entries in `lang:register`.
- Prefer `event.add(locale, itemId, value)` and let KweebecJS generate:
  - `KweebecJS.kweebecjs_kweebecjs_<ITEM_ID>.name`

## Custom item/block not visible in game

Checklist:

- Script ran without exceptions.
- `event.register(...)` returned expected final ID.
- Recipe output uses the final returned ID when needed.
- Assets (`model`, `texture`, `icon`) are loaded via `Asset.load(...)`.
- For block placement visuals, block model/texture is correctly defined.

## Recipes not overriding/deleting

Cause:

- `RecipeFilter` does not exactly match output/input totals and bench requirements.

Fix:

- Match the exact bench `Type` + `Id`.
- Include relevant categories in `benchRequires(...)`.
- Match exact input quantities with `Item(id, amount)`.

## Reload fails with runtime zip path error

Cause:

- `runtime_assets.zip` unavailable/locked/corrupted during reload.

Fix:

- Run `kjsreload` again.
- Verify filesystem permissions for `.hytale-server/KweebecJS/`.
- Ensure no external process is locking `runtime_assets.zip`.
