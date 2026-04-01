# Language Entries

## Event

Use `lang:register`:

```js
eventbus.addEventListener("lang:register", (event) => {
    event.add("en-US", "Weapon_Sword_Void_Mithril", "Void Mithril Sword");
    event.add("en-US", "Weapon_Sword_Void_Mithril", "description", "A sword forged with void essence.");
});
```

## Payload Method

- `event.add(locale, itemIdOrFullKey, value)` (defaults to `name`)
- `event.add(locale, itemIdOrFullKey, translationKey, value)`

Returns `"<normalized-locale>:<normalized-key>"`.

## Locale Rules

- `_` is normalized to `-` (`pt_BR` -> `pt-BR`).
- Output files are generated in:
  - `Server/Languages/<locale>/KweebecJS.lang`

## Key Rules

- If `itemIdOrFullKey` contains `.` it is treated as a full key and used as provided.
- If `itemIdOrFullKey` is a raw ID (no `.`), KweebecJS generates:
  - `KweebecJS.kweebecjs_kweebecjs_<ITEM_ID>.<TRANSLATION_KEY>`
- `<TRANSLATION_KEY>` defaults to `name` for the 3-argument form.
- Aliases are still emitted for compatibility with common lookup variants.

## Value Rules

- Value must be non-blank.
- Newlines are escaped as `\n`.

## Best Practice

For generated items/blocks, prefer using raw IDs with `event.add(...)` and let KweebecJS generate keys.

Generated key pattern:

- `KweebecJS.kweebecjs_kweebecjs_<ID>.name`
- `KweebecJS.kweebecjs_kweebecjs_<ID>.description`
