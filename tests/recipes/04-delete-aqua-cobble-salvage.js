eventbus.addEventListener("recipes:register", (event) => {
    const aquaCobbleSalvageFilter = RecipeFilter.new()
        .outputs(Item("Rubble_Aqua", 1))
        .inputs([
            Item("Rock_Aqua_Cobble", 1)
        ])
        .benchRequires([
            BenchRequirement.typeRequirement("Processing"),
            BenchRequirement.idRequirement("Salvagebench")
        ]);

    event.delete(aquaCobbleSalvageFilter);
});
