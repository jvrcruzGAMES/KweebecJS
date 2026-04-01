# Blocks

## Event

Use `blocks:register`:

```js
eventbus.addEventListener("blocks:register", (event) => {
    // event.register(...)
    // event.override(...)
});
```

## Payload Methods

- `event.register(id, blockProperties, blockSpecialProperties?)`
- `event.override(id, anyPatch)`

Both return the final resolved block ID.

## Block Registration

### `BlockProperties` methods

- `model(Asset)` (required, `AssetTarget.BLOCK_MODEL`)
- `texture(Asset)` (required, `AssetTarget.BLOCK_TEXTURE`)
- `drawType(string)`
- `material(string)`
- `opacity(string)`
- `blockSoundSetId(string)`
- `hitboxType(string)`
- `generateItem(boolean)` (default `true`)
- `itemId(string)`
- `itemDisplayNameKey(string)`
- `itemDisplayDescriptionKey(string)`
- `itemIcon(Asset)` (`AssetTarget.BLOCK_ITEM_ICON`)
- `itemCategory(string)` (repeatable)
- `itemPlayerAnimationsId(string)`
- `itemSoundSetId(string)`
- `itemQuality(string)`
- `itemLevel(int)`
- `build()`

If translation keys are omitted for auto-generated block items, KweebecJS generates:

- `KweebecJS.kweebecjs_kweebecjs_<ITEM_ID>.name`
- `KweebecJS.kweebecjs_kweebecjs_<ITEM_ID>.description`

### Auto-generated block item

- Enabled by default.
- Uses block ID as item ID unless overridden with `itemId(...)`.
- Can be disabled with `generateItem(false)`.
- If `itemIcon(...)` is not set, texture is copied to block-item icon target.

## Block Special Properties

`BlockSpecialProperties` supports complex block JSON fields through typed helper builders.

### Simple methods

- `group`, `blockListAssetId`, `prefabListAssetId`
- `blockParticleSetId`, `blockBreakingDecalId`
- `customModelScale`, `customModelAnimation`
- `interactionHint`, `interactionSoundEventId`, `ambientSoundEventId`
- `isLooping`, `isTrigger`, `isDoor`, `allowsMultipleUsers`
- `damageToEntities`, `requiresAlphaBlending`
- `textureSideMask`, `transitionTexture`, `transitionToTag`
- `randomRotation`, `variantRotation`
- `supportDropType`, `maxSupportDistance`, `ignoreSupportWhenPlaced`
- `hitboxType`, `interactionHitboxType`
- `recipe(Recipe)`
- `build()`

### Complex methods

- `particles`, `effect`, `transitionToGroups`, `conditionalSounds`
- `textures`, `customModelTexture`
- `tintUp`, `tintDown`, `tintNorth`, `tintSouth`, `tintWest`, `tintEast`
- `flags`, `light`, `gathering`, `support`, `supporting`
- `interactions`, `state`, `placementSettings`, `movementSettings`
- `bench`, `farming`, `blockEntity`, `railConfig`
- `connectedBlockRuleSet`, `seats`, `beds`
- `complex(fieldName, BlockJsonValue)` for custom manual mapping

## Block Overrides

Single signature:

```js
event.override(blockId, anyPatch)
```

Accepted `anyPatch` types:

- `BlockProperties`
- `BlockPropertiesOverride`
- `BlockSpecialProperties`
- `BlockSpecialPropertiesOverride`

`Override` variants patch only provided fields.

## Complex Data Helpers

`BlockArray.new()` builder methods:

- `string(value)`
- `number(value)`
- `integer(value)`
- `bool(value)`
- `nullValue()`
- `object(BlockJsonValue)`
- `array(BlockArray)`
- `build()`

Each typed block object helper uses:

- `string(key, value)`
- `number(key, value)`
- `integer(key, value)`
- `bool(key, value)`
- `nullValue(key)`
- `object(key, BlockJsonValue)`
- `array(key, BlockArray)`
- `build()`

Helpers available:

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

## Example

```js
eventbus.addEventListener("blocks:register", (event) => {
    event.register(
        "Block_Thorium_Chest",
        BlockProperties.new()
            .model(Asset.load("BlameJared_Thorium_Chests/BlameJared_ThoriumChests_Thorium_Chest.blockymodel", AssetTarget.BLOCK_MODEL))
            .texture(Asset.load("BlameJared_Thorium_Chests/BlameJared_ThoriumChests_Thorium_Chest_Texture.png", AssetTarget.BLOCK_TEXTURE))
            .itemIcon(Asset.load("BlameJared_Thorium_Chests/BlameJared_ThoriumChests_Thorium_Chest_Texture.png", AssetTarget.BLOCK_ITEM_ICON))
            .itemDisplayNameKey("KweebecJS.kweebecjs_kweebecjs_Block_Thorium_Chest.name")
            .itemDisplayDescriptionKey("KweebecJS.kweebecjs_kweebecjs_Block_Thorium_Chest.description")
            .build(),
        BlockSpecialProperties.new()
            .flags(BlockFlags.new().bool("IsUsable", true).build())
            .interactions(BlockInteractions.new().string("Use", "Open_Container").build())
            .variantRotation("NESW")
            .build()
    );
});
```
