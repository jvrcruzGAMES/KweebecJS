# Getting Started

## What KweebecJS Does

KweebecJS lets you generate and patch Hytale server content through JavaScript:

- Items
- Blocks
- Recipes
- Language entries
- Asset files copied into a runtime asset pack

## Directory Layout

All script and asset authoring happens under:

```text
.hytale-server/KweebecJS/
```

Relevant folders:

- `server_scripts/` - your `.js` files
- `assets/` - source files loaded by `Asset.load(...)`
- `runtime_assets/` - generated files (managed by the mod)
- `runtime_assets.zip` - generated immutable pack loaded by the game

## First Script

Create `.hytale-server/KweebecJS/server_scripts/hello.js`:

```js
eventbus.addEventListener("lang:register", (event) => {
    event.add("en-US", "Hello_Item", "Hello Item");
});
```

Then run:

```text
kjsreload
```

## Exposed Events

- `lang:register`
- `blocks:register`
- `items:register`
- `recipes:register`

See each category file for full API details.
