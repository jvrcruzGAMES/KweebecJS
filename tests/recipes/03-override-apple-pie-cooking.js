eventbus.addEventListener("recipes:register", (event) => {
    const applePieFilter = RecipeFilter.new()
        .outputs(Item("Food_Pie_Apple"))
        .inputs([
            Item("Ingredient_Dough", 1),
            Item("Plant_Fruit_Apple", 3),
            Item("Ingredient_Spices", 1)
        ])
        .benchRequires([
            BenchRequirement.typeRequirement("Crafting"),
            BenchRequirement.idRequirement("Cookingbench"),
            BenchRequirement.categoryRequirement("Baked")
        ]);

    event.override(
        applePieFilter,
        [
            BenchRequirement.typeRequirement("Crafting"),
            BenchRequirement.idRequirement("Cookingbench"),
            BenchRequirement.categoryRequirement("Baked")
        ],
        [
            Item("Ingredient_Dough", 1),
            Item("Plant_Fruit_Apple", 4),
            Item("Ingredient_Spices", 1)
        ],
        Item("Food_Pie_Apple", 1),
        7.5
    );
});
