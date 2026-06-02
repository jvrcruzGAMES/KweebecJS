eventbus.addEventListener("recipes:register", (event) => {
    event.add(
        [
            BenchRequirement.typeRequirement("Processing"),
            BenchRequirement.idRequirement("Salvagebench")
        ],
        [
            Item("Wood_Sticks", 1)
        ],
        Item("Ingredient_Stick", 2),
        1.0
    );
});
