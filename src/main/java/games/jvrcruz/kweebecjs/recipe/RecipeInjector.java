package games.jvrcruz.kweebecjs.recipe;

import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import games.jvrcruz.kweebecjs.asset.RuntimeAssetPackUtils;
import games.jvrcruz.kweebecjs.rhino.ScriptItem;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class RecipeInjector {
    private static final String DELETED_RECIPE_BENCH_ID = "KweebecJS_DeletedRecipeBench";
    private final Field recipeIdField;
    private final Field materialTagField;
    private final Class<?> pluginClass;
    private final Supplier<String> assetPackNameSupplier;
    private final List<String> registeredRecipePaths = new ArrayList<>();

    public RecipeInjector(Class<?> pluginClass, Supplier<String> assetPackNameSupplier) {
        this.pluginClass = pluginClass;
        this.assetPackNameSupplier = assetPackNameSupplier;
        try {
            recipeIdField = CraftingRecipe.class.getDeclaredField("id");
            recipeIdField.setAccessible(true);
            materialTagField = MaterialQuantity.class.getDeclaredField("tag");
            materialTagField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Could not access recipe/material fields for registration.", e);
        }
    }

    public Object addFromArgs(Object[] args) {
        RecipeDefinition definition = parseDefinitionArgs(args);
        String recipeId = "KweebecJS_" + UUID.randomUUID().toString().replace("-", "");
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

        RecipeDefinition definition = parseDefinitionArgs(sliceArgs(args, 1));
        List<String> overriddenRecipeIds = new ArrayList<>();
        for (Map.Entry<String, CraftingRecipe> entry : CraftingRecipe.getAssetMap().getAssetMap().entrySet()) {
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
        for (Map.Entry<String, CraftingRecipe> entry : CraftingRecipe.getAssetMap().getAssetMap().entrySet()) {
            CraftingRecipe recipe = entry.getValue();
            if (!recipeFilter.matches(recipe)) {
                continue;
            }

            overrideRecipe(
                    entry.getKey(),
                    recipe.getInput(),
                    recipe.getPrimaryOutput(),
                    disabledBenchRequirements(),
                    recipe.getTimeSeconds()
            );
            deletedRecipeIds.add(entry.getKey());
        }
        return deletedRecipeIds;
    }

    private String registerRecipe(String recipeId, RecipeDefinition definition) {
        BenchRequirement[] requirements = definition.requirements();
        String[] inputIds = definition.inputIds();
        String outputId = definition.outputId();
        float timeSeconds = definition.timeSeconds();
        String diagramId = definition.diagramId();

        BenchRequirement[] finalRequirements = applyDiagramId(requirements, diagramId);
        MaterialQuantity[] inputs = new MaterialQuantity[inputIds.length];
        for (int i = 0; i < inputIds.length; i++) {
            inputs[i] = new MaterialQuantity(inputIds[i], null, null, 1, new BsonDocument());
        }
        MaterialQuantity primaryOutput = new MaterialQuantity(outputId, null, null, 1, new BsonDocument());
        overrideRecipe(recipeId, inputs, primaryOutput, finalRequirements, timeSeconds);
        return recipeId;
    }

    private void overrideRecipe(
            String recipeId,
            MaterialQuantity[] inputs,
            MaterialQuantity primaryOutput,
            BenchRequirement[] requirements,
            float timeSeconds
    ) {
        CraftingRecipe recipe = new CraftingRecipe(
                inputs,
                primaryOutput,
                new MaterialQuantity[]{primaryOutput},
                1,
                requirements,
                timeSeconds,
                false,
                0
        );
        setRecipeId(recipe, recipeId);
        loadRecipe(recipeId, recipe, inputs, primaryOutput, requirements, timeSeconds);
    }

    public void clearRegisteredRecipes() {
        if (registeredRecipePaths.isEmpty()) {
            return;
        }

        String assetPackName = assetPackNameSupplier.get();
        Path runtimeAssetPackFile = SystemPaths.resolveRuntimeAssetPackFile(pluginClass);
        try (var archiveFileSystem = RuntimeAssetPackUtils.openArchive(runtimeAssetPackFile)) {
            List<Path> archivePaths = new ArrayList<>(registeredRecipePaths.size());
            for (String relativePath : registeredRecipePaths) {
                archivePaths.add(archiveFileSystem.getPath("/" + relativePath.replace('\\', '/')));
            }
            CraftingRecipe.getAssetStore().removeAssetWithPaths(assetPackName, archivePaths);
        } catch (IOException e) {
            throw new IllegalStateException("Failed removing JS recipe assets from runtime pack.", e);
        }

        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
        for (String recipePath : registeredRecipePaths) {
            try {
                Files.deleteIfExists(runtimeAssetsDir.resolve(recipePath));
            } catch (IOException ignored) {
                // Best-effort cleanup; the asset store removal already detached the recipe.
            }
        }
        try {
            RuntimeAssetPackUtils.rebuildArchive(runtimeAssetsDir, runtimeAssetPackFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed rebuilding runtime asset archive after clearing JS recipes.", e);
        }
        registeredRecipePaths.clear();
    }

    private void loadRecipe(
            String recipeId,
            CraftingRecipe recipe,
            MaterialQuantity[] inputs,
            MaterialQuantity primaryOutput,
            BenchRequirement[] requirements,
            float timeSeconds
    ) {
        String recipeRelativePath = writeRecipeAsset(recipeId, inputs, primaryOutput, requirements, timeSeconds);
        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
        Path runtimeAssetPackFile = SystemPaths.resolveRuntimeAssetPackFile(pluginClass);
        try {
            RuntimeAssetPackUtils.rebuildArchive(runtimeAssetsDir, runtimeAssetPackFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed rebuilding runtime asset archive for JS recipe '" + recipeId + "'.", e);
        }

        try (var archiveFileSystem = RuntimeAssetPackUtils.openArchive(runtimeAssetPackFile)) {
            Path archiveRecipePath = archiveFileSystem.getPath("/" + recipeRelativePath.replace('\\', '/'));
            AssetLoadResult<String, CraftingRecipe> loadResult = CraftingRecipe.getAssetStore().loadAssetsFromPaths(
                    assetPackNameSupplier.get(),
                    List.of(archiveRecipePath)
            );
            if (loadResult.hasFailed()) {
                throw new IllegalStateException(
                        "Failed loading JS recipe '" + recipeId + "'. Failed keys: "
                                + loadResult.getFailedToLoadKeys()
                                + ", failed paths: "
                                + loadResult.getFailedToLoadPaths()
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed opening runtime asset archive for JS recipe '" + recipeId + "'.", e);
        }
        registeredRecipePaths.add(recipeRelativePath);
    }

    private RecipeDefinition parseDefinitionArgs(Object[] args) {
        BenchRequirement[] requirements = parseBenchRequirementsArgument(args);
        String[] inputIds = parseInputItemsArgument(args);
        String outputId = parseOutputItemArgument(args);
        Object specialArg = parseSpecialArgument(args);
        BenchType benchType = requirements[0].type;
        String diagramId = parseDiagramId(specialArg, benchType);
        float timeSeconds = parseTimeSeconds(specialArg, benchType);
        return new RecipeDefinition(requirements, inputIds, outputId, diagramId, timeSeconds);
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

    private String[] parseInputItemsArgument(Object[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("event.add requires second argument: [Item(...), ...]");
        }
        if (!(args[1] instanceof NativeArray itemsArray)) {
            throw new IllegalArgumentException("Second argument must be an array of Item instances.");
        }

        List<String> itemIds = new ArrayList<>();
        for (Object rawItem : itemsArray.toArray()) {
            Object unwrapped = unwrap(rawItem);
            if (!(unwrapped instanceof ScriptItem scriptItem)) {
                throw new IllegalArgumentException("Inputs array must contain only Item(...) instances.");
            }
            itemIds.add(scriptItem.getID());
        }
        return itemIds.toArray(new String[0]);
    }

    private String parseOutputItemArgument(Object[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("event.add requires third argument: Item(...)");
        }

        Object unwrapped = unwrap(args[2]);
        if (!(unwrapped instanceof ScriptItem scriptItem)) {
            throw new IllegalArgumentException("Third argument must be a single Item(...) instance.");
        }
        return scriptItem.getID();
    }

    private Object parseSpecialArgument(Object[] args) {
        if (args.length < 4) {
            return null;
        }
        Object specialArg = args[3];
        return specialArg == Scriptable.NOT_FOUND ? null : specialArg;
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

    private String writeRecipeAsset(
            String recipeId,
            MaterialQuantity[] inputs,
            MaterialQuantity primaryOutput,
            BenchRequirement[] requirements,
            float timeSeconds
    ) {
        try {
            Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
            Path recipeDir = runtimeAssetsDir.resolve(CraftingRecipe.getAssetStore().getPath());
            Files.createDirectories(recipeDir);

            String extension = CraftingRecipe.getAssetStore().getExtension();
            String relativePath = CraftingRecipe.getAssetStore().getPath() + "/" + recipeId + extension;
            Path recipePath = runtimeAssetsDir.resolve(relativePath);
            Files.writeString(
                    recipePath,
                    toRecipeJson(inputs, primaryOutput, requirements, timeSeconds),
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

    private String toRecipeJson(
            MaterialQuantity[] inputs,
            MaterialQuantity primaryOutput,
            BenchRequirement[] requirements,
            float timeSeconds
    ) {
        return """
                {
                  "Input": [%s],
                  "Output": [%s],
                  "PrimaryOutput": %s,
                  "OutputQuantity": 1,
                  "BenchRequirement": [%s],
                  "TimeSeconds": %s,
                  "KnowledgeRequired": false,
                  "RequiredMemoriesLevel": 1
                }
                """.formatted(
                joinMaterials(inputs),
                materialJson(primaryOutput),
                materialJson(primaryOutput),
                joinBenchRequirements(requirements),
                formatDouble(timeSeconds)
        );
    }

    private String joinMaterials(MaterialQuantity[] materials) {
        List<String> parts = new ArrayList<>(materials.length);
        for (MaterialQuantity material : materials) {
            parts.add(materialJson(material));
        }
        return String.join(", ", parts);
    }

    private String joinBenchRequirements(BenchRequirement[] requirements) {
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
        if (value instanceof Wrapper wrapper) {
            return wrapper.unwrap();
        }
        if (value instanceof NativeJavaObject javaObject) {
            return javaObject.unwrap();
        }
        return value;
    }

    private record RecipeDefinition(
            BenchRequirement[] requirements,
            String[] inputIds,
            String outputId,
            String diagramId,
            float timeSeconds
    ) {
    }
}
