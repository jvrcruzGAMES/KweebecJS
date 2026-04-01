# API Reference

This is the complete JS API exposed by `KweebecJSEnvironment`.

## Global Objects

- `eventbus`
- `BenchRequirement`
- `Recipe`
- `RecipeFilter`
- `Item` (function)
- `AssetTarget`
- `Asset`
- `BlockProperties`
- `BlockPropertiesOverride`
- `BlockSpecialProperties`
- `BlockSpecialPropertiesOverride`
- `BlockConnectedBlockRuleSet`
- `BlockSeats`
- `BlockBeds`
- `BlockFlags`
- `BlockLight`
- `BlockGathering`
- `BlockSupport`
- `BlockSupporting`
- `BlockInteractions`
- `BlockState`
- `BlockPlacementSettings`
- `BlockMovementSettings`
- `BlockBench`
- `BlockFarming`
- `BlockEntity`
- `BlockRailConfig`
- `BlockArray`
- `ItemProperties`
- `ItemPropertiesOverride`
- `ItemSpecialProperties`
- `ItemSpecialPropertiesOverride`
- `ItemEntry`

## Event Bus

- `eventbus.addEventListener(eventType, callback)`

`eventType` values:

- `"lang:register"`
- `"blocks:register"`
- `"items:register"`
- `"recipes:register"`

`lang:register` payload:

- `event.add(locale, itemIdOrFullKey, value)`
- `event.add(locale, itemIdOrFullKey, translationKey, value)`

When `itemIdOrFullKey` is a raw ID, generated key format is:

- `KweebecJS.kweebecjs_kweebecjs_<ITEM_ID>.<TRANSLATION_KEY>`

## Recipe and Bench Helpers

- `BenchRequirement.typeRequirement(benchType)`
- `BenchRequirement.idRequirement(benchId)`
- `BenchRequirement.category(categoryId)`
- `BenchRequirement.categoryRequirement(categoryId)`
- `Recipe.new(...eventAddArgs)`
- `RecipeFilter.new()`

`RecipeFilter` instance methods:

- `.outputs(Item(...))`
- `.inputs([Item(...), ...])`
- `.benchRequires([BenchRequirement..., ...])`

## Item Function

- `Item(itemId, amount?)`

## Asset API

- `Asset.load(relativePath, assetTarget)`
- `AssetTarget` enum:
  - `ITEM_MODEL`
  - `ITEM_TEXTURE`
  - `ITEM_ICON`
  - `BLOCK_MODEL`
  - `BLOCK_TEXTURE`
  - `BLOCK_ITEM_ICON`
  - `PARTICLE_TEXTURE`

## Item Builders

- `ItemEntry.new(id, itemProperties, itemSpecialProperties?)`
- `ItemProperties.new() -> builder`
- `ItemPropertiesOverride.new() -> builder`
- `ItemSpecialProperties.new() -> builder`
- `ItemSpecialPropertiesOverride.new() -> builder`

See [Items](./04-items.md) for all item builder methods.

## Block Builders

- `BlockProperties.new() -> builder`
- `BlockPropertiesOverride.new() -> builder`
- `BlockSpecialProperties.new() -> builder`
- `BlockSpecialPropertiesOverride.new() -> builder`
- `BlockArray.new() -> builder`
- typed object helpers (`BlockFlags.new()`, etc.) return generic block-object builders

See [Blocks](./05-blocks.md) for full methods.

## Typed Block Object Builder API

All typed block object builders share:

- `.string(key, value)`
- `.number(key, value)`
- `.integer(key, value)`
- `.bool(key, value)`
- `.nullValue(key)`
- `.object(key, blockJsonValue)`
- `.array(key, blockArray)`
- `.build()`

`BlockArray` builder:

- `.string(value)`
- `.number(value)`
- `.integer(value)`
- `.bool(value)`
- `.nullValue()`
- `.object(blockJsonValue)`
- `.array(blockArray)`
- `.build()`
