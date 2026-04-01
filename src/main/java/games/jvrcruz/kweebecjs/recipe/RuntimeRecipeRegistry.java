package games.jvrcruz.kweebecjs.recipe;

import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import games.jvrcruz.kweebecjs.rhino.ScriptItem;
import games.jvrcruz.kweebecjs.rhino.Recipe;
import games.jvrcruz.kweebecjs.system.SystemPaths;
import org.bson.BsonDocument;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class RuntimeRecipeRegistry {
    private static final String DELETED_RECIPE_BENCH_ID = "KweebecJS_DeletedRecipeBench";
    private static final String PATCH_DIRECTORY = "KweebecJS";
    private final Field recipeIdField;
    private final Field materialTagField;
    private final Field primaryOutputQuantityField;
    private final Class<?> pluginClass;
    private final Supplier<String> assetPackNameSupplier;
    private final List<String> registeredRecipePaths = new ArrayList<>();
    private final Map<String, CraftingRecipe> stagedRecipesById = new java.util.LinkedHashMap<>();

    public RuntimeRecipeRegistry(Class<?> pluginClass, Supplier<String> assetPackNameSupplier) {
        this.pluginClass = pluginClass;
        this.assetPackNameSupplier = assetPackNameSupplier;
        try {
            recipeIdField = CraftingRecipe.class.getDeclaredField("id");
            recipeIdField.setAccessible(true);
            materialTagField = MaterialQuantity.class.getDeclaredField("tag");
            materialTagField.setAccessible(true);
            primaryOutputQuantityField = CraftingRecipe.class.getDeclaredField("primaryOutputQuantity");
            primaryOutputQuantityField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Could not access recipe/material fields for registration.", e);
        }
    }

    public Object addFromArgs(Object[] args) {
        RecipeDefinition definition = parseDefinitionArgs(normalizeAddArgs(args));
        String recipeId = createRuntimeRecipeId();
        return registerRecipe(recipeId, definition);
    }

    public Object addForRegisteredAsset(String outputItemId, Object[] args) {
        if (outputItemId == null || outputItemId.isBlank()) {
            throw new IllegalArgumentException("outputItemId must not be blank.");
        }
        Object[] normalized = normalizeAddArgs(args);
        if (normalized.length < 3) {
            throw new IllegalArgumentException("Recipe.new(...) for specialProperties.recipe requires at least 3 args.");
        }
        Object[] patched = normalized.clone();
        int outputAmount = 1;
        Object outputArg = unwrap(patched[2]);
        if (outputArg instanceof ScriptItem scriptItem) {
            outputAmount = scriptItem.getAmount();
        }
        patched[2] = new ScriptItem(outputItemId, outputAmount);
        RecipeDefinition definition = parseDefinitionArgs(patched);
        String recipeId = createRuntimeRecipeId();
        return registerRecipe(recipeId, definition);
    }

    public Object overrideFromArgs(Object[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("event.override requires a RecipeFilter and replacement recipe args.");
        }
        Object unwrappedFilter = unwrap(args[0]);
        if (!(unwrappedFilter instanceof RecipeFilter recipeFilter)) {
            throw new IllegalArgumentException("event.override(filter, ...) expects a RecipeFilter as the first argument.");
        }

        RecipeDefinition definition = parseDefinitionArgs(normalizeAddArgs(sliceArgs(args, 1)));
        List<String> overriddenRecipeIds = new ArrayList<>();
        Map<String, CraftingRecipe> candidateRecipes = new java.util.LinkedHashMap<>(CraftingRecipe.getAssetMap().getAssetMap());
        candidateRecipes.putAll(stagedRecipesById);
        for (Map.Entry<String, CraftingRecipe> entry : candidateRecipes.entrySet()) {
            if (!recipeFilter.matches(entry.getValue())) {
                continue;
            }
            registerRecipe(entry.getKey(), definition);
            overriddenRecipeIds.add(entry.getKey());
        }
        return overriddenRecipeIds;
    }

    public Object deleteFromArgs(Object[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("event.delete requires a RecipeFilter.");
        }
        Object unwrappedFilter = unwrap(args[0]);
        if (!(unwrappedFilter instanceof RecipeFilter recipeFilter)) {
            throw new IllegalArgumentException("event.delete(filter) expects a RecipeFilter.");
        }

        List<String> deletedRecipeIds = new ArrayList<>();
        Map<String, CraftingRecipe> candidateRecipes = new java.util.LinkedHashMap<>(CraftingRecipe.getAssetMap().getAssetMap());
        candidateRecipes.putAll(stagedRecipesById);
        for (Map.Entry<String, CraftingRecipe> entry : candidateRecipes.entrySet()) {
            CraftingRecipe recipe = entry.getValue();
            if (!recipeFilter.matches(recipe)) {
                continue;
            }

            BenchRequirement[] remainingRequirements = removeMatchedBenchRequirements(recipe, recipeFilter);
            CraftingRecipe replacement = new CraftingRecipe(
                    recipe.getInput(),
                    recipe.getPrimaryOutput(),
                    recipe.getOutputs(),
                    getPrimaryOutputQuantity(recipe),
                    remainingRequirements.length == 0 ? disabledBenchRequirements() : remainingRequirements,
                    recipe.getTimeSeconds(),
                    recipe.isKnowledgeRequired(),
                    recipe.getRequiredMemoriesLevel()
            );
            setRecipeId(replacement, entry.getKey());
            stageRecipe(entry.getKey(), replacement);
            deletedRecipeIds.add(entry.getKey());
        }
        return deletedRecipeIds;
    }

    private String registerRecipe(String recipeId, RecipeDefinition definition) {
        BenchRequirement[] requirements = definition.requirements();
        ScriptItem[] inputItems = definition.inputItems();
        ScriptItem outputItem = definition.outputItem();
        float timeSeconds = definition.timeSeconds();
        String diagramId = definition.diagramId();
        int requiredMemoriesLevel = definition.requiredMemoriesLevel();

        BenchRequirement[] finalRequirements = applyDiagramId(requirements, diagramId);
        MaterialQuantity[] inputs = new MaterialQuantity[inputItems.length];
        for (int i = 0; i < inputItems.length; i++) {
            inputs[i] = new MaterialQuantity(inputItems[i].getID(), null, null, inputItems[i].getAmount(), new BsonDocument());
        }
        MaterialQuantity primaryOutput = new MaterialQuantity(
                outputItem.getID(),
                null,
                null,
                outputItem.getAmount(),
                new BsonDocument()
        );
        overrideRecipe(recipeId, inputs, primaryOutput, outputItem.getAmount(), finalRequirements, timeSeconds, requiredMemoriesLevel);
        return recipeId;
    }

    private void overrideRecipe(
            String recipeId,
            MaterialQuantity[] inputs,
            MaterialQuantity primaryOutput,
            int primaryOutputQuantity,
            BenchRequirement[] requirements,
            float timeSeconds,
            int requiredMemoriesLevel
    ) {
        CraftingRecipe recipe = new CraftingRecipe(
                inputs,
                primaryOutput,
                new MaterialQuantity[]{primaryOutput},
                primaryOutputQuantity,
                requirements,
                timeSeconds,
                false,
                requiredMemoriesLevel
        );
        setRecipeId(recipe, recipeId);
        stageRecipe(recipeId, recipe);
    }

    public void clearRegisteredRecipes() {
        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
        Path patchDirectory = runtimeAssetsDir.resolve(CraftingRecipe.getAssetStore().getPath()).resolve(PATCH_DIRECTORY);
        List<String> discoveredRecipePaths = new ArrayList<>();
        if (Files.isDirectory(patchDirectory)) {
            try (var paths = Files.walk(patchDirectory)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    String relativePath = runtimeAssetsDir.relativize(path).toString().replace('\\', '/');
                    discoveredRecipePaths.add(relativePath);
                });
            } catch (IOException e) {
                throw new IllegalStateException("Failed scanning generated JS recipe assets.", e);
            }
        }

        List<String> recipePathsToClear = discoveredRecipePaths.isEmpty()
                ? List.copyOf(registeredRecipePaths)
                : discoveredRecipePaths;

        String assetPackName = assetPackNameSupplier.get();
        List<Path> recipePathsToRemoveFromStore = new ArrayList<>(recipePathsToClear.size());
        for (String recipePath : recipePathsToClear) {
            recipePathsToRemoveFromStore.add(runtimeAssetsDir.resolve(recipePath));
        }
        CraftingRecipe.getAssetStore().removeAssetWithPaths(assetPackName, recipePathsToRemoveFromStore);

        for (String recipePath : recipePathsToClear) {
            try {
                Files.deleteIfExists(runtimeAssetsDir.resolve(recipePath));
            } catch (IOException ignored) {
                // Best-effort cleanup; source removal is sufficient for future rebuilds.
            }
        }
        try {
            if (Files.isDirectory(patchDirectory)) {
                try (var paths = Files.walk(patchDirectory)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup.
                        }
                    });
                }
            }
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
        registeredRecipePaths.clear();
        stagedRecipesById.clear();
    }

    private void stageRecipe(String recipeId, CraftingRecipe recipe) {
        String recipeRelativePath = writeRecipeAsset(recipeId, recipe);
        registeredRecipePaths.add(recipeRelativePath);
        stagedRecipesById.put(recipeId, recipe);
    }

    public int applyRegisteredRecipes() {
        if (registeredRecipePaths.isEmpty()) {
            return 0;
        }

        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
        List<Path> recipePaths = new ArrayList<>(registeredRecipePaths.size());
        for (String recipeRelativePath : registeredRecipePaths) {
            recipePaths.add(runtimeAssetsDir.resolve(recipeRelativePath));
        }
        AssetLoadResult<String, CraftingRecipe> loadResult = CraftingRecipe.getAssetStore().loadAssetsFromPaths(
                assetPackNameSupplier.get(),
                recipePaths
        );
        if (loadResult.hasFailed()) {
            throw new IllegalStateException(
                    "Failed loading staged JS recipes. Failed keys: "
                            + loadResult.getFailedToLoadKeys()
                            + ", failed paths: "
                            + loadResult.getFailedToLoadPaths()
            );
        }
        return registeredRecipePaths.size();
    }

    private RecipeDefinition parseDefinitionArgs(Object[] args) {
        BenchRequirement[] requirements = parseBenchRequirementsArgument(args);
        ScriptItem[] inputItems = parseInputItemsArgument(args);
        ScriptItem outputItem = parseOutputItemArgument(args);
        Object specialArg = parseSpecialArgument(args);
        BenchType benchType = requirements[0].type;
        String diagramId = parseDiagramId(specialArg, benchType);
        float timeSeconds = parseTimeSeconds(specialArg, benchType);
        int requiredMemoriesLevel = parseRequiredMemoriesLevel(args);
        return new RecipeDefinition(requirements, inputItems, outputItem, diagramId, timeSeconds, requiredMemoriesLevel);
    }

    private BenchRequirement[] parseBenchRequirementsArgument(Object[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("event.add requires first argument: BenchRequirement[]");
        }
        if (!(args[0] instanceof NativeArray requirementsArray)) {
            throw new IllegalArgumentException("First argument must be an array of BenchRequirement instances.");
        }

        return new BenchRequirement[]{
                RecipeFilters.parseBenchRequirementSpecArray(requirementsArray)
        };
    }

    private ScriptItem[] parseInputItemsArgument(Object[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("event.add requires second argument: [Item(...), ...]");
        }
        if (!(args[1] instanceof NativeArray itemsArray)) {
            throw new IllegalArgumentException("Second argument must be an array of Item instances.");
        }

        List<ScriptItem> items = new ArrayList<>();
        for (Object rawItem : itemsArray.toArray()) {
            Object unwrapped = unwrap(rawItem);
            if (!(unwrapped instanceof ScriptItem scriptItem)) {
                throw new IllegalArgumentException("Inputs array must contain only Item(...) instances.");
            }
            items.add(scriptItem);
        }
        return items.toArray(new ScriptItem[0]);
    }

    private ScriptItem parseOutputItemArgument(Object[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("event.add requires third argument: Item(...)");
        }

        Object unwrapped = unwrap(args[2]);
        if (!(unwrapped instanceof ScriptItem scriptItem)) {
            throw new IllegalArgumentException("Third argument must be a single Item(...) instance.");
        }
        return scriptItem;
    }

    private Object parseSpecialArgument(Object[] args) {
        if (args.length < 4) {
            return null;
        }
        Object specialArg = args[3];
        return specialArg == Scriptable.NOT_FOUND ? null : specialArg;
    }

    private int parseRequiredMemoriesLevel(Object[] args) {
        if (args.length < 5 || args[4] == null || args[4] == Scriptable.NOT_FOUND) {
            return 1;
        }
        Object value = args[4];
        int parsedValue;
        if (value instanceof Number number) {
            parsedValue = number.intValue();
        } else {
            parsedValue = Integer.parseInt(String.valueOf(value));
        }
        if (parsedValue < 1) {
            throw new IllegalArgumentException("requiredMemoriesLevel must be greater than or equal to 1.");
        }
        return parsedValue;
    }

    private String parseDiagramId(Object specialArg, BenchType benchType) {
        if (benchType != BenchType.DiagramCrafting || specialArg == null) {
            return null;
        }

        String diagramId = String.valueOf(specialArg);
        if (diagramId.isBlank()) {
            throw new IllegalArgumentException("diagramID must be a non-blank string.");
        }
        return diagramId;
    }

    private float parseTimeSeconds(Object specialArg, BenchType benchType) {
        if (benchType != BenchType.Crafting && benchType != BenchType.Processing) {
            return 0.0f;
        }
        if (specialArg == null) {
            return 0.0f;
        }
        if (specialArg instanceof Number number) {
            return number.floatValue();
        }

        String rawValue = String.valueOf(specialArg);
        if (rawValue.isBlank()) {
            return 0.0f;
        }

        try {
            return Float.parseFloat(rawValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("specialArg must be a numeric processing time for " + benchType + " recipes.", e);
        }
    }

    private BenchRequirement[] applyDiagramId(BenchRequirement[] requirements, String diagramId) {
        if (diagramId == null || diagramId.isBlank()) {
            return requirements;
        }

        BenchRequirement[] mapped = new BenchRequirement[requirements.length];
        for (int i = 0; i < requirements.length; i++) {
            BenchRequirement requirement = requirements[i];
            if (requirement != null && requirement.type == BenchType.DiagramCrafting) {
                String[] categories = requirement.categories == null
                        ? new String[]{diagramId}
                        : appendIfMissing(requirement.categories, diagramId);
                mapped[i] = new BenchRequirement(requirement.type, requirement.id, categories, requirement.requiredTierLevel);
            } else {
                mapped[i] = requirement;
            }
        }
        return mapped;
    }

    private String[] appendIfMissing(String[] values, String value) {
        for (String existing : values) {
            if (value.equals(existing)) {
                return values;
            }
        }
        String[] updated = new String[values.length + 1];
        System.arraycopy(values, 0, updated, 0, values.length);
        updated[values.length] = value;
        return updated;
    }

    private void setRecipeId(CraftingRecipe recipe, String recipeId) {
        try {
            recipeIdField.set(recipe, recipeId);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not set recipe id: " + recipeId, e);
        }
    }

    private String writeRecipeAsset(String recipeId, CraftingRecipe recipe) {
        try {
            Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
            Path recipeDir = runtimeAssetsDir.resolve(CraftingRecipe.getAssetStore().getPath());
            Path patchDir = recipeDir.resolve(PATCH_DIRECTORY);
            Files.createDirectories(patchDir);

            String extension = CraftingRecipe.getAssetStore().getExtension();
            String relativePath = CraftingRecipe.getAssetStore().getPath() + "/" + PATCH_DIRECTORY + "/" + recipeId + extension;
            Path recipePath = runtimeAssetsDir.resolve(relativePath);
            Files.writeString(
                    recipePath,
                    toRecipeJson(recipe),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return relativePath;
        } catch (IOException e) {
            throw new IllegalStateException("Failed writing JS recipe asset '" + recipeId + "'.", e);
        }
    }

    private String toRecipeJson(CraftingRecipe recipe) {
        MaterialQuantity[] inputs = recipe.getInput();
        MaterialQuantity[] outputs = recipe.getOutputs();
        MaterialQuantity primaryOutput = recipe.getPrimaryOutput();
        BenchRequirement[] requirements = recipe.getBenchRequirement();
        return """
                {
                  "Input": [%s],
                  "Output": [%s],
                  "PrimaryOutput": %s,
                  "OutputQuantity": %d,
                  "BenchRequirement": [%s],
                  "TimeSeconds": %s,
                  "KnowledgeRequired": %s,
                  "RequiredMemoriesLevel": %d
                }
                """.formatted(
                joinMaterials(inputs),
                joinMaterials(outputs),
                materialJson(primaryOutput),
                getPrimaryOutputQuantity(recipe),
                joinBenchRequirements(requirements),
                formatDouble(recipe.getTimeSeconds()),
                Boolean.toString(recipe.isKnowledgeRequired()),
                recipe.getRequiredMemoriesLevel()
        );
    }

    private String joinMaterials(MaterialQuantity[] materials) {
        if (materials == null || materials.length == 0) {
            return "";
        }
        List<String> parts = new ArrayList<>(materials.length);
        for (MaterialQuantity material : materials) {
            parts.add(materialJson(material));
        }
        return String.join(", ", parts);
    }

    private String joinBenchRequirements(BenchRequirement[] requirements) {
        if (requirements == null || requirements.length == 0) {
            return "";
        }
        List<String> parts = new ArrayList<>(requirements.length);
        for (BenchRequirement requirement : requirements) {
            parts.add("""
                    {
                      "Type": "%s",
                      "Id": "%s",
                      "Categories": [%s],
                      "RequiredTierLevel": %d
                    }
                    """.formatted(
                    requirement.type.name(),
                    escapeJson(requirement.id),
                    joinQuoted(requirement.categories),
                    requirement.requiredTierLevel
            ));
        }
        return String.join(", ", parts);
    }

    private String materialJson(MaterialQuantity material) {
        List<String> fields = new ArrayList<>(4);
        if (material.getItemId() != null) {
            fields.add("\"ItemId\": \"" + escapeJson(material.getItemId()) + "\"");
        }
        if (material.getResourceTypeId() != null) {
            fields.add("\"ResourceTypeId\": \"" + escapeJson(material.getResourceTypeId()) + "\"");
        }
        String tag = getMaterialTag(material);
        if (tag != null) {
            fields.add("\"Tag\": \"" + escapeJson(tag) + "\"");
        }
        fields.add("\"Quantity\": " + material.getQuantity());
        return "{ " + String.join(", ", fields) + " }";
    }

    private String joinQuoted(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        List<String> quoted = new ArrayList<>(values.length);
        for (String value : values) {
            quoted.add("\"" + escapeJson(value) + "\"");
        }
        return String.join(", ", quoted);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String formatDouble(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }

    private BenchRequirement[] disabledBenchRequirements() {
        return new BenchRequirement[]{
                new BenchRequirement(BenchType.Crafting, DELETED_RECIPE_BENCH_ID, new String[0], 0)
        };
    }

    private BenchRequirement[] removeMatchedBenchRequirements(CraftingRecipe recipe, RecipeFilter recipeFilter) {
        List<BenchRequirement> expectedRequirements = recipeFilter.benchRequirements();
        BenchRequirement[] recipeRequirements = recipe.getBenchRequirement();
        if (expectedRequirements.isEmpty() || recipeRequirements == null || recipeRequirements.length == 0) {
            return new BenchRequirement[0];
        }

        List<BenchRequirement> remaining = new ArrayList<>();
        for (BenchRequirement actualRequirement : recipeRequirements) {
            boolean matched = expectedRequirements.stream().anyMatch(expected -> RecipeFilters.matchesBenchRequirement(actualRequirement, expected));
            if (!matched) {
                remaining.add(actualRequirement);
            }
        }
        return remaining.toArray(new BenchRequirement[0]);
    }

    private String createRuntimeRecipeId() {
        String rawId = UUID.randomUUID().toString().replace("-", "");
        if (rawId.isEmpty()) {
            return "KweebecJS_A";
        }
        return "KweebecJS_" + Character.toUpperCase(rawId.charAt(0)) + rawId.substring(1);
    }

    private int getPrimaryOutputQuantity(CraftingRecipe recipe) {
        try {
            return primaryOutputQuantityField.getInt(recipe);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read CraftingRecipe primaryOutputQuantity.", e);
        }
    }

    private Object[] sliceArgs(Object[] args, int startIndex) {
        Object[] sliced = new Object[Math.max(args.length - startIndex, 0)];
        System.arraycopy(args, startIndex, sliced, 0, sliced.length);
        return sliced;
    }

    private String getMaterialTag(MaterialQuantity material) {
        try {
            return (String) materialTagField.get(material);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read MaterialQuantity tag.", e);
        }
    }

    private Object unwrap(Object value) {
        Object current = value;
        while (true) {
            Object next = current;
            if (current instanceof Wrapper wrapper) {
                next = wrapper.unwrap();
            } else if (current instanceof NativeJavaObject javaObject) {
                next = javaObject.unwrap();
            }
            if (next == current) {
                return current;
            }
            current = next;
        }
    }

    private Object[] normalizeAddArgs(Object[] args) {
        if (args.length == 1) {
            Object unwrapped = unwrap(args[0]);
            if (unwrapped instanceof Recipe recipe) {
                return recipe.args();
            }
        }
        return args;
    }

    private record RecipeDefinition(
            BenchRequirement[] requirements,
            ScriptItem[] inputItems,
            ScriptItem outputItem,
            String diagramId,
            float timeSeconds,
            int requiredMemoriesLevel
    ) {
    }
}
