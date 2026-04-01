# Recipes

## Event

Use `recipes:register`:

```js
eventbus.addEventListener("recipes:register", (event) => {
    // event.add(...)
    // event.override(...)
    // event.delete(...)
});
```

## Payload Methods

- `event.add(...)`
- `event.override(recipeFilter, ...)`
- `event.delete(recipeFilter)`

## Core Helpers

- `Item(itemId, amount?)`
- `BenchRequirement.typeRequirement(benchType)`
- `BenchRequirement.idRequirement(benchId)`
- `BenchRequirement.category(categoryId)`
- `BenchRequirement.categoryRequirement(categoryId)` (alias)
- `Recipe.new(...sameArgsAsEventAdd)`
- `RecipeFilter.new()`

## `event.add(...)` Signature

```js
event.add(
    [benchRequirementSpecs],
    [inputItems],
    outputItem,
    specialArg,           // optional depending on bench type
    requiredMemoriesLevel // optional, defaults to 1
)
```

### `specialArg` by bench type

- `Crafting` / `Processing`: numeric time seconds
- `DiagramCrafting`: diagram id string
- others: usually `null` or omitted

## `event.override(...)`

Matches existing recipes by filter and rewrites each matched recipe definition with the provided args.

Returns an array of overridden recipe IDs.

```js
event.override(filter, [bench], [inputs], output, specialArg, requiredMemoriesLevel);
```

## `event.delete(...)`

Deletes recipe crafting paths matched by filter.

Returns an array of deleted recipe IDs.

Behavior:

- if only one bench requirement is matched and removed, remaining bench paths survive
- if none remain, recipe is effectively disabled

## `RecipeFilter`

Chainable methods:

- `outputs(Item(...))`
- `inputs([Item(...), ...])`
- `benchRequires([BenchRequirement..., ...])`

Matching rules:

- output matches primary output item (and quantity when explicitly set on `Item`)
- inputs match full per-item quantity totals
- bench requirement matches by `Type` + `Id`, and categories must be contained

## Example

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
        Item("Ingredient_Bar_Copper", 1),
        4.0
    );
});
```
