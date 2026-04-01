# Server Script Lifecycle

## Load Order

KweebecJS emits script events in this order:

1. `lang:register`
2. `blocks:register`
3. `items:register`
4. `recipes:register`

## Runtime Pipeline

Each load/reload cycle:

1. Clears previous runtime-generated state.
2. Clears `runtime_assets/`.
3. Executes all scripts from `server_scripts/`.
4. Generates JSON/lang/assets inside `runtime_assets/`.
5. Syncs `runtime_assets.zip`.
6. Loads only the KweebecJS runtime pack.

## Event Bus

Use:

```js
eventbus.addEventListener("<eventType>", (event) => {
    // event payload methods vary by event type
});
```

If a callback throws, KweebecJS logs:

```text
Error while dispatching JS event '<event>': <message>
```

## Namespacing Rules

- Registered custom item/block IDs are automatically prefixed with `KweebecJS_` in Java.
- If you already pass an ID starting with `KweebecJS_`, it is kept.
- You do not need to manually namespace normal `event.register(...)` IDs.

## Important Rule for Overrides

`items:register -> event.override(id, anyPatch)` now builds a full merged item definition:

- starts from original item properties
- applies only provided patch fields
- does not rely on self-parent inheritance
