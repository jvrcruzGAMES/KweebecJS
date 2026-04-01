# Items

## Event

Use `items:register`:

```js
eventbus.addEventListener("items:register", (event) => {
    // event.register(...)
    // event.override(...)
});
```

## Payload Methods

- `event.register(itemEntry)`
- `event.override(itemId, anyPatch)`

Both return the final resolved item ID.

## Item Registration

Create entries with:

```js
ItemEntry.new(
    "Weapon_Sword_Void_Mithril",
    ItemProperties.new()...build(),
    ItemSpecialProperties.new()...build() // optional
)
```

If no translation keys are provided, KweebecJS generates:

- `KweebecJS.kweebecjs_kweebecjs_<ITEM_ID>.name`
- `KweebecJS.kweebecjs_kweebecjs_<ITEM_ID>.description`

### `ItemProperties` methods

- `parent(string)` (required)
- `displayNameKey(string)`
- `displayDescriptionKey(string)`
- `category(string)` (repeatable)
- `model(Asset)` (`AssetTarget.ITEM_MODEL`)
- `texture(Asset)` (`AssetTarget.ITEM_TEXTURE`)
- `icon(Asset)` (`AssetTarget.ITEM_ICON`)
- `playerAnimationsId(string)`
- `dropOnDeath(boolean)`
- `build()`

### `ItemSpecialProperties` methods

- `droppedItemAnimation(string)`
- `reticle(string)`
- `quality(string)`
- `itemLevel(int)`
- `utilityCompatible(boolean)`
- `primaryInteraction(string)`
- `secondaryInteraction(string)`
- `ability1Interaction(string)`
- `typeTag(string)` (repeatable)
- `familyTag(string)` (repeatable)
- `itemSoundSetId(string)`
- `maxDurability(int)`
- `durabilityLossOnHit(double)`
- `signatureEnergyBonus(int)`
- `clearWeaponEntityStat(string)` (repeatable)
- `damage(int)`
- `recipe(Recipe)`
- `build()`

`ItemSpecialProperties` supports all secondary options, but KweebecJS applies only fields compatible with the target item type.

## Item Overrides

Single signature:

```js
event.override(itemId, anyPatch)
```

Accepted `anyPatch` types:

- `ItemProperties`
- `ItemPropertiesOverride`
- `ItemSpecialProperties`
- `ItemSpecialPropertiesOverride`

`Override` variants are patch-only. Omitted fields are not patched.

### Merge Behavior

When overriding:

1. KweebecJS loads the original item definition.
2. Copies full original properties into a generated runtime JSON.
3. Applies only patch fields.
4. Writes final merged JSON (without relying on parent self-reference).

## Example

```js
eventbus.addEventListener("items:register", (event) => {
    const id = event.register(
        ItemEntry.new(
            "Weapon_Sword_Void_Mithril",
            ItemProperties.new()
                .parent("Weapon_Sword_Mithril")
                .displayNameKey("KweebecJS.kweebecjs_kweebecjs_Weapon_Sword_Void_Mithril.name")
                .displayDescriptionKey("KweebecJS.kweebecjs_kweebecjs_Weapon_Sword_Void_Mithril.description")
                .category("Items.Weapons")
                .model(Asset.load("void_Sword/void_Mithril.blockymodel", AssetTarget.ITEM_MODEL))
                .texture(Asset.load("void_Sword/void_Mithril_Texture.png", AssetTarget.ITEM_TEXTURE))
                .icon(Asset.load("void_Sword/Weapon_Sword_Void_Mithril.png", AssetTarget.ITEM_ICON))
                .playerAnimationsId("Sword")
                .build(),
            ItemSpecialProperties.new()
                .damage(22)
                .quality("Epic")
                .build()
        )
    );

    event.override(
        "Weapon_Sword_Mithril",
        ItemPropertiesOverride.new()
            .texture(Asset.load("void_Sword/void_Mithril_Texture.png", AssetTarget.ITEM_TEXTURE))
            .build()
    );
});
```
