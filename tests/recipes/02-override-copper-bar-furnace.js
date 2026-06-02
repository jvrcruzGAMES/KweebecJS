eventbus.addEventListener("recipes:register", (event) => {
    const copperBarFilter = RecipeFilter.new()
        .outputs(Item("Ingredient_Bar_Copper", 1))
        .inputs([
            Item("Ore_Copper", 1)
        ])
        .benchRequires([
            BenchRequirement.typeRequirement("Processing"),
            BenchRequirement.idRequirement("Furnace")
        ]);

    event.override(
        copperBarFilter,
        [
            BenchRequirement.typeRequirement("Processing"),
            BenchRequirement.idRequirement("Furnace")
        ],
        [
            Item("Ore_Copper", 2)
        ],
        Item("Ingredient_Bar_Copper", 1),
        6.0
    );
});
