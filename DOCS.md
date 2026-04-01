# KweebecJS Server Scripts

This document describes the current `server_scripts` API exposed by KweebecJS.

Server scripts are loaded from:

```text
.hytale-server/KweebecJS/server_scripts
```

Scripts are plain JavaScript files executed by the server. The main current use case is recipe manipulation through the `recipes:register` event.

Only one JS event is currently exposed:

- `recipes:register`

## Basic Structure

Register listeners with `eventbus.addEventListener(eventName, callback)`.

Example:

```js
eventbus.addEventListener("recipes:register", (event) => {
    // Your logic here
});
```

## Available Events

### `recipes:register`

Used to add, override, and delete recipes.

Example:

```js
eventbus.addEventListener("recipes:register", (event) => {
    event.add(
        [
            BenchRequirement.typeRequirement("Crafting"),
            BenchRequirement.idRequirement("Workbench"),
            BenchRequirement.category("Workbench_Survival")
        ],
        [
            Item("Ingredient_Stick"),
            Item("Rock_Stone")
        ],
        Item("Bench_Campfire"),
        0.0
    );
});
```

## Global Helpers

### `Item(itemId)`

Creates an item reference used in recipes and filters.

Example:

```js
const stick = Item("Ingredient_Stick");
```

### `BenchRequirement`

Used to build bench requirement arrays.

Available methods:

- `BenchRequirement.typeRequirement(benchType)`
- `BenchRequirement.idRequirement(benchId)`
- `BenchRequirement.category(categoryId)`
- `BenchRequirement.categoryRequirement(categoryId)`

Example:

```js
[
    BenchRequirement.typeRequirement("Processing"),
    BenchRequirement.idRequirement("Campfire")
]
```

`categoryRequirement(...)` is an alias of `category(...)`.

### `RecipeFilter`

Used by `event.override(...)` and `event.delete(...)`.

Create one with:

```js
const filter = RecipeFilter.new();
```

Available methods:

- `filter.outputs(Item)`
- `filter.inputs([Item, ...])`
- `filter.benchRequires([BenchRequirement, ...])`

Example:

```js
const torchFilter = RecipeFilter.new()
    .outputs(Item("Furniture_Crude_Torch"))
    .inputs([
        Item("Ingredient_Fibre"),
        Item("Ingredient_Tree_Sap"),
        Item("Ingredient_Stick")
    ])
    .benchRequires([
        BenchRequirement.typeRequirement("Crafting"),
        BenchRequirement.idRequirement("Fieldcraft"),
        BenchRequirement.category("Tools")
    ]);
```

Filter behavior:

- `outputs(...)` matches the recipe primary output item
- `inputs(...)` matches the full input item set
- `benchRequires(...)` matches one bench requirement entry
- all configured parts of the filter must match

## Recipe API

Inside `recipes:register`, the `event` object exposes:

- `event.add(...)`
- `event.override(...)`
- `event.delete(...)`

### `event.add(BenchRequirements, [Inputs...], output, specialArg, requiredMemoriesLevel)`

Registers a new recipe.

Arguments:

1. `BenchRequirements`
   An array built with `BenchRequirement.*`
2. `[Inputs...]`
   Array of `Item(...)`
3. `output`
   A single `Item(...)`
4. `specialArg`
   Meaning depends on recipe type:
   - `DiagramCrafting`: diagram ID string
   - `Crafting`: time in seconds
   - `Processing`: time in seconds
   - `StructuralCrafting`: usually `null`
5. `requiredMemoriesLevel`
   Optional integer, defaults to `1`

Examples:

Crafting recipe:

```js
event.add(
    [
        BenchRequirement.typeRequirement("Crafting"),
        BenchRequirement.idRequirement("Workbench"),
        BenchRequirement.category("Workbench_Survival")
    ],
    [
        Item("Ingredient_Stick"),
        Item("Rock_Stone")
    ],
    Item("Bench_Campfire"),
    0.0,
    1
);
```

Processing recipe:

```js
event.add(
    [
        BenchRequirement.typeRequirement("Processing"),
        BenchRequirement.idRequirement("Campfire")
    ],
    [
        Item("Ingredient_Stick"),
        Item("Ingredient_Stick")
    ],
    Item("Ingredient_Charcoal"),
    2.5,
    1
);
```

Diagram recipe:

```js
event.add(
    [
        BenchRequirement.typeRequirement("DiagramCrafting"),
        BenchRequirement.idRequirement("Armory"),
        BenchRequirement.category("Weapons.Club")
    ],
    [
        Item("Ingredient_Bar_Copper"),
        Item("Ingredient_Leather_Light")
    ],
    Item("Weapon_Club_Copper"),
    "Weapons.Club",
    1
);
```

Structural recipe:

```js
event.add(
    [
        BenchRequirement.typeRequirement("StructuralCrafting"),
        BenchRequirement.idRequirement("Builders"),
        BenchRequirement.categoryRequirement("Bench")
    ],
    [
        Item("Rock_Stone"),
        Item("Rock_Stone")
    ],
    Item("Rock_Stone_Brick"),
    null,
    1
);
```

### `event.override(recipeFilter, BenchRequirements, [Inputs...], output, specialArg, requiredMemoriesLevel)`

Finds every recipe matching the filter and overrides each one with the provided recipe definition.

Arguments:

1. `recipeFilter`
2. Same positional recipe arguments as `event.add(...)`

Example:

```js
const copperBarFilter = RecipeFilter.new()
    .outputs(Item("Ingredient_Bar_Copper"))
    .inputs([
        Item("Ingredient_Stick"),
        Item("Ore_Copper")
    ])
    .benchRequires([
        BenchRequirement.typeRequirement("Processing"),
        BenchRequirement.idRequirement("Furnace")
    ]);

event.override(
    copperBarFilter,
    [
        BenchRequirement.typeRequirement("Processing"),
        BenchRequirement.idRequirement("Campfire")
    ],
    [
        Item("Ore_Copper"),
        Item("Ingredient_Charcoal")
    ],
    Item("Ingredient_Bar_Copper"),
    4.0,
    1
);
```

### `event.delete(recipeFilter)`

Finds every recipe matching the filter and deletes the matching crafting path.

Current behavior:

- if a recipe has multiple bench requirements and the filter matches one of them, only the matching bench requirement is removed
- if no bench requirements remain after deletion, the recipe is hidden completely

Example: remove the player-inventory torch recipe while keeping the workbench recipe:

```js
const playerTorchFilter = RecipeFilter.new()
    .outputs(Item("Furniture_Crude_Torch"))
    .inputs([
        Item("Ingredient_Fibre"),
        Item("Ingredient_Tree_Sap"),
        Item("Ingredient_Stick")
    ])
    .benchRequires([
        BenchRequirement.typeRequirement("Crafting"),
        BenchRequirement.idRequirement("Fieldcraft"),
        BenchRequirement.category("Tools")
    ]);

event.delete(playerTorchFilter);
```

## Complete Example

```js
eventbus.addEventListener("recipes:register", (event) => {
    const copperBarFilter = RecipeFilter.new()
        .outputs(Item("Ingredient_Bar_Copper"))
        .inputs([
            Item("Ingredient_Stick"),
            Item("Ore_Copper")
        ])
        .benchRequires([
            BenchRequirement.typeRequirement("Processing"),
            BenchRequirement.idRequirement("Furnace")
        ]);

    const playerTorchFilter = RecipeFilter.new()
        .outputs(Item("Furniture_Crude_Torch"))
        .inputs([
            Item("Ingredient_Fibre"),
            Item("Ingredient_Tree_Sap"),
            Item("Ingredient_Stick")
        ])
        .benchRequires([
            BenchRequirement.typeRequirement("Crafting"),
            BenchRequirement.idRequirement("Fieldcraft"),
            BenchRequirement.category("Tools")
        ]);

    event.add(
        [
            BenchRequirement.typeRequirement("Processing"),
            BenchRequirement.idRequirement("Campfire")
        ],
        [
            Item("Ingredient_Stick"),
            Item("Ingredient_Stick")
        ],
        Item("Ingredient_Charcoal"),
        2.5,
        1
    );

    event.override(
        copperBarFilter,
        [
            BenchRequirement.typeRequirement("Processing"),
            BenchRequirement.idRequirement("Campfire")
        ],
        [
            Item("Ore_Copper"),
            Item("Ingredient_Charcoal")
        ],
        Item("Ingredient_Bar_Copper"),
        4.0,
        1
    );

    event.delete(playerTorchFilter);
});
```

## Reloading Scripts

Use:

```text
kjsreload
```

or:

```text
kweebecjsreload
```

Reload behavior:

- clears server-script listeners
- undoes previously generated JS recipe patches
- re-executes `server_scripts`
- reapplies current JS recipe patches

This prevents recipe duplication across reloads and ensures add/override/delete patches are recomputed from the current scripts.
