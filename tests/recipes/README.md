Recipe modification test scripts for KweebecJS.

Location:
- `.hytale-server/KweebecJS/server_scripts/tests/recipes/`

Why this path:
- `SystemPaths.resolveKweebecRootDir(...)` resolves to `.hytale-server/KweebecJS/`
- `SystemPaths.resolveServerScriptsDir(...)` resolves to `.hytale-server/KweebecJS/server_scripts/`
- Scripts placed here are discovered by the mod on `kjsreload`

Coverage:
- `01-add-salvage-stick-recipe.js`: adds a new recipe
- `02-override-copper-bar-furnace.js`: overrides a furnace recipe using exact output/input/bench matching
- `03-override-apple-pie-cooking.js`: overrides a recipe that requires bench categories
- `04-delete-aqua-cobble-salvage.js`: deletes a recipe path by filter

Asset references used for the filters come from `Assets.zip`, for example:
- `Server/Item/Items/Ingredient/Bar/Ingredient_Bar_Copper.json`
- `Server/Item/Items/Food/Food_Pie_Apple.json`
- `Server/Item/Recipes/Salvage/Salvage_Rock_Aqua_Cobble.json`

Usage:
1. Leave the scripts in this folder to load them together, or temporarily move out the ones you do not want.
2. Run `kjsreload`.
3. Verify the affected recipes in game.

Notes:
- `RecipeFilter.inputs(...)` must match exact item totals.
- `RecipeFilter.benchRequires(...)` must match exact bench `Type` and `Id`.
- Category-based recipes must include the expected categories in `benchRequires(...)`.
