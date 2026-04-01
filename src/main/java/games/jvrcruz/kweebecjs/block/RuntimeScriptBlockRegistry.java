package games.jvrcruz.kweebecjs.block;

import games.jvrcruz.kweebecjs.asset.RuntimeItemAssetManager;
import games.jvrcruz.kweebecjs.rhino.Asset;
import games.jvrcruz.kweebecjs.rhino.AssetTarget;
import games.jvrcruz.kweebecjs.rhino.BlockProperties;
import games.jvrcruz.kweebecjs.rhino.BlockPropertiesOverride;
import games.jvrcruz.kweebecjs.rhino.BlockSpecialProperties;
import games.jvrcruz.kweebecjs.rhino.BlockSpecialPropertiesOverride;
import games.jvrcruz.kweebecjs.recipe.RuntimeRecipeRegistry;
import games.jvrcruz.kweebecjs.system.SystemPaths;
import org.mozilla.javascript.Wrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class RuntimeScriptBlockRegistry {
    private static final String NAMESPACE_PREFIX = "KweebecJS_";
    private static final String TRANSLATION_FILE_PREFIX = "KweebecJS.";
    private static final String GENERATED_BLOCKS_DIRECTORY = "Server/Item/Block/Blocks/KweebecJS";
    private static final String GENERATED_ITEMS_DIRECTORY = "Server/Item/Items/KweebecJS";
    private final Class<?> pluginClass;
    private final Supplier<String> modGroupSupplier;
    private final Supplier<String> modIdSupplier;
    private final RuntimeItemAssetManager runtimeItemAssetManager;
    private final RuntimeRecipeRegistry runtimeRecipeRegistry;
    private final Map<String, PendingBlockRegistration> pendingRegistrationsByBlockId = new LinkedHashMap<>();
    private final Map<String, BlockOverridePatch> pendingOverridesByBlockId = new LinkedHashMap<>();
    private final Set<String> generatedAssetPaths = new LinkedHashSet<>();

    public RuntimeScriptBlockRegistry(
            Class<?> pluginClass,
            Supplier<String> modGroupSupplier,
            Supplier<String> modIdSupplier,
            RuntimeItemAssetManager runtimeItemAssetManager,
            RuntimeRecipeRegistry runtimeRecipeRegistry
    ) {
        this.pluginClass = pluginClass;
        this.modGroupSupplier = modGroupSupplier;
        this.modIdSupplier = modIdSupplier;
        this.runtimeItemAssetManager = runtimeItemAssetManager;
        this.runtimeRecipeRegistry = runtimeRecipeRegistry;
    }

    public synchronized String registerFromArgs(Object[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: event.register(blockId, BlockProperties.new()...build(), BlockSpecialProperties.new()...build()?)");
        }

        String blockId = String.valueOf(args[0]);
        if (blockId == null || blockId.isBlank()) {
            throw new IllegalArgumentException("blockId must not be blank.");
        }
        Object propertiesArg = unwrap(args[1]);
        if (!(propertiesArg instanceof BlockProperties properties)) {
            throw new IllegalArgumentException("Second argument must be a BlockProperties instance.");
        }

        BlockSpecialProperties specialProperties = null;
        Object specialArg = args.length >= 3 ? unwrap(args[2]) : null;
        if (specialArg instanceof BlockSpecialProperties providedSpecialProperties) {
            specialProperties = providedSpecialProperties;
        } else if (args.length >= 3 && specialArg != null) {
            throw new IllegalArgumentException("Third argument must be a BlockSpecialProperties instance when provided.");
        }

        String namespacedBlockId = namespacedId(blockId);
        pendingRegistrationsByBlockId.put(
                namespacedBlockId,
                new PendingBlockRegistration(namespacedBlockId, properties, specialProperties)
        );
        return namespacedBlockId;
    }

    public synchronized String overrideFromArgs(Object[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: event.override(blockId, anyPatch)");
        }
        String rawBlockId = String.valueOf(args[0]);
        if (rawBlockId == null || rawBlockId.isBlank()) {
            throw new IllegalArgumentException("blockId must not be blank.");
        }
        String targetBlockId = resolveOverrideTargetId(rawBlockId);

        Object patchArg = unwrap(args[1]);
        BlockProperties blockPropertiesPatch = patchArg instanceof BlockProperties blockProperties ? blockProperties : null;
        BlockPropertiesOverride blockPropertiesOverridePatch = patchArg instanceof BlockPropertiesOverride override ? override : null;
        BlockSpecialProperties blockSpecialPatch = patchArg instanceof BlockSpecialProperties specialProperties ? specialProperties : null;
        BlockSpecialPropertiesOverride blockSpecialOverridePatch = patchArg instanceof BlockSpecialPropertiesOverride specialPropertiesOverride
                ? specialPropertiesOverride
                : null;
        if (blockPropertiesPatch == null
                && blockPropertiesOverridePatch == null
                && blockSpecialPatch == null
                && blockSpecialOverridePatch == null) {
            throw new IllegalArgumentException(
                    "anyPatch must be BlockProperties, BlockPropertiesOverride, BlockSpecialProperties, or BlockSpecialPropertiesOverride."
            );
        }

        pendingOverridesByBlockId.put(
                targetBlockId,
                new BlockOverridePatch(blockPropertiesPatch, blockPropertiesOverridePatch, blockSpecialPatch, blockSpecialOverridePatch)
        );
        return targetBlockId;
    }

    public synchronized void clearRegisteredBlocks() {
        for (String generatedAssetPath : generatedAssetPaths) {
            runtimeItemAssetManager.removeGeneratedAsset(generatedAssetPath);
        }
        generatedAssetPaths.clear();
        pendingRegistrationsByBlockId.clear();
        pendingOverridesByBlockId.clear();
    }

    public synchronized int applyRegisteredBlocks() throws IOException {
        if (pendingRegistrationsByBlockId.isEmpty() && pendingOverridesByBlockId.isEmpty()) {
            return 0;
        }

        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
        Path assetsDir = SystemPaths.resolveAssetsDir(pluginClass);
        Files.createDirectories(runtimeAssetsDir.resolve(GENERATED_BLOCKS_DIRECTORY));
        Files.createDirectories(runtimeAssetsDir.resolve(GENERATED_ITEMS_DIRECTORY));

        Map<String, String> generatedAssets = new LinkedHashMap<>();
        generatedAssetPaths.clear();

        for (PendingBlockRegistration registration : pendingRegistrationsByBlockId.values()) {
            if (registration.specialProperties() != null && registration.specialProperties().getRecipe() != null) {
                runtimeRecipeRegistry.addForRegisteredAsset(registration.blockId(), new Object[]{registration.specialProperties().getRecipe()});
            }

            if (Boolean.TRUE.equals(registration.properties().getGenerateItem())) {
                String itemId = registration.properties().getItemId() == null || registration.properties().getItemId().isBlank()
                        ? registration.blockId()
                        : registration.properties().getItemId();
                itemId = namespacedId(itemId);
                String itemAssetPath = GENERATED_ITEMS_DIRECTORY + "/" + itemId + ".json";
                String itemAssetJson = toAutoBlockItemJson(itemId, registration, assetsDir, runtimeAssetsDir);
                generatedAssets.put(itemAssetPath, itemAssetJson);
                generatedAssetPaths.add(itemAssetPath);
            } else {
                String blockAssetPath = GENERATED_BLOCKS_DIRECTORY + "/" + registration.blockId() + ".json";
                String blockAssetJson = toBlockJson(registration, assetsDir, runtimeAssetsDir);
                generatedAssets.put(blockAssetPath, blockAssetJson);
                generatedAssetPaths.add(blockAssetPath);
            }
        }

        for (Map.Entry<String, BlockOverridePatch> overrideEntry : pendingOverridesByBlockId.entrySet()) {
            String targetBlockId = overrideEntry.getKey();
            String blockAssetPath = GENERATED_BLOCKS_DIRECTORY + "/" + targetBlockId + ".json";
            String blockAssetJson = toBlockOverrideJson(targetBlockId, overrideEntry.getValue(), assetsDir, runtimeAssetsDir);
            generatedAssets.put(blockAssetPath, blockAssetJson);
            generatedAssetPaths.add(blockAssetPath);

            BlockOverridePatch patch = overrideEntry.getValue();
            boolean generateItem = resolveGenerateItemFlag(patch);
            if (generateItem) {
                String itemId = resolveOverrideItemId(targetBlockId, patch);
                String itemAssetPath = GENERATED_ITEMS_DIRECTORY + "/" + itemId + ".json";
                String itemAssetJson = toAutoBlockItemOverrideJson(itemId, targetBlockId, patch, assetsDir, runtimeAssetsDir);
                generatedAssets.put(itemAssetPath, itemAssetJson);
                generatedAssetPaths.add(itemAssetPath);
            }
        }

        runtimeItemAssetManager.putGeneratedAssets(generatedAssets);
        return pendingRegistrationsByBlockId.size() + pendingOverridesByBlockId.size();
    }

    private String toBlockJson(PendingBlockRegistration registration, Path assetsDir, Path runtimeAssetsDir) throws IOException {
        BlockProperties properties = registration.properties();
        List<String> fields = new ArrayList<>();

        String modelPath = copyAssetToRuntimePack(properties.getModel(), assetsDir, runtimeAssetsDir);
        String texturePath = copyAssetToRuntimePack(properties.getTexture(), assetsDir, runtimeAssetsDir);

        fields.add("  \"DrawType\": \"" + escapeJson(properties.getDrawType()) + "\"");
        fields.add("  \"Material\": \"" + escapeJson(properties.getMaterial()) + "\"");
        fields.add("  \"Opacity\": \"" + escapeJson(properties.getOpacity()) + "\"");
        fields.add("  \"Model\": \"" + escapeJson(modelPath) + "\"");
        fields.add("  \"CustomModel\": \"" + escapeJson(modelPath) + "\"");
        fields.add("""
                  "Textures": [
                    {
                      "All": "%s"
                    }
                  ]""".formatted(escapeJson(texturePath)));
        fields.add("""
                  "CustomModelTexture": [
                    {
                      "Weight": 1,
                      "Texture": "%s"
                    }
                  ]""".formatted(escapeJson(texturePath)));
        fields.add("  \"BlockSoundSetId\": \"" + escapeJson(properties.getBlockSoundSetId()) + "\"");
        if (properties.getHitboxType() != null) {
            fields.add("  \"HitboxType\": \"" + escapeJson(properties.getHitboxType()) + "\"");
        }

        if (registration.specialProperties() != null) {
            for (Map.Entry<String, String> entry : registration.specialProperties().getRawFields().entrySet()) {
                fields.add("  \"" + escapeJson(entry.getKey()) + "\": " + entry.getValue());
            }
        }

        return "{\n" + String.join(",\n", fields) + "\n}\n";
    }

    private String toBlockOverrideJson(
            String targetBlockId,
            BlockOverridePatch patch,
            Path assetsDir,
            Path runtimeAssetsDir
    ) throws IOException {
        List<String> fields = new ArrayList<>();

        BlockProperties blockPropertiesPatch = patch.blockPropertiesPatch();
        BlockPropertiesOverride blockPropertiesOverridePatch = patch.blockPropertiesOverridePatch();
        Asset model = blockPropertiesPatch != null
                ? blockPropertiesPatch.getModel()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getModel() : null;
        Asset texture = blockPropertiesPatch != null
                ? blockPropertiesPatch.getTexture()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getTexture() : null;
        if (model != null) {
            fields.add("  \"Model\": \"" + escapeJson(copyAssetToRuntimePack(model, assetsDir, runtimeAssetsDir)) + "\"");
            fields.add("  \"CustomModel\": \"" + escapeJson(copyAssetToRuntimePack(model, assetsDir, runtimeAssetsDir)) + "\"");
        }
        if (texture != null) {
            String texturePath = copyAssetToRuntimePack(texture, assetsDir, runtimeAssetsDir);
            fields.add("""
                      "Textures": [
                        {
                          "All": "%s"
                        }
                      ]""".formatted(escapeJson(texturePath)));
            fields.add("""
                      "CustomModelTexture": [
                        {
                          "Weight": 1,
                          "Texture": "%s"
                        }
                      ]""".formatted(escapeJson(texturePath)));
        }

        String drawType = blockPropertiesPatch != null
                ? blockPropertiesPatch.getDrawType()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getDrawType() : null;
        if (drawType != null) {
            fields.add("  \"DrawType\": \"" + escapeJson(drawType) + "\"");
        }
        String material = blockPropertiesPatch != null
                ? blockPropertiesPatch.getMaterial()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getMaterial() : null;
        if (material != null) {
            fields.add("  \"Material\": \"" + escapeJson(material) + "\"");
        }
        String opacity = blockPropertiesPatch != null
                ? blockPropertiesPatch.getOpacity()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getOpacity() : null;
        if (opacity != null) {
            fields.add("  \"Opacity\": \"" + escapeJson(opacity) + "\"");
        }
        String blockSoundSetId = blockPropertiesPatch != null
                ? blockPropertiesPatch.getBlockSoundSetId()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getBlockSoundSetId() : null;
        if (blockSoundSetId != null) {
            fields.add("  \"BlockSoundSetId\": \"" + escapeJson(blockSoundSetId) + "\"");
        }
        String hitboxType = blockPropertiesPatch != null
                ? blockPropertiesPatch.getHitboxType()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getHitboxType() : null;
        if (hitboxType != null) {
            fields.add("  \"HitboxType\": \"" + escapeJson(hitboxType) + "\"");
        }

        for (Map.Entry<String, String> entry : patch.resolveSpecialRawFields().entrySet()) {
            fields.add("  \"" + escapeJson(entry.getKey()) + "\": " + entry.getValue());
        }
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("event.override for block '" + targetBlockId + "' requires at least one patch field.");
        }
        return "{\n" + String.join(",\n", fields) + "\n}\n";
    }

    private String toAutoBlockItemJson(
            String itemId,
            PendingBlockRegistration registration,
            Path assetsDir,
            Path runtimeAssetsDir
    ) throws IOException {
        BlockProperties properties = registration.properties();
        List<String> fields = new ArrayList<>();

        String displayNameKey = properties.getItemDisplayNameKey() == null || properties.getItemDisplayNameKey().isBlank()
                ? defaultItemTranslationKey(itemId, "name")
                : properties.getItemDisplayNameKey();
        String displayDescriptionKey = properties.getItemDisplayDescriptionKey() == null || properties.getItemDisplayDescriptionKey().isBlank()
                ? defaultItemTranslationKey(itemId, "description")
                : properties.getItemDisplayDescriptionKey();
        appendTranslationProperties(fields, displayNameKey, displayDescriptionKey);

        Asset iconAsset = properties.getItemIcon() != null ? properties.getItemIcon() : properties.getTexture();
        String iconPath = properties.getItemIcon() != null
                ? copyAssetToRuntimePack(iconAsset, assetsDir, runtimeAssetsDir)
                : copyAssetToRuntimePackAs(iconAsset, AssetTarget.BLOCK_ITEM_ICON, assetsDir, runtimeAssetsDir);
        fields.add("  \"Icon\": \"" + escapeJson(iconPath) + "\"");

        if (!properties.getItemCategories().isEmpty()) {
            fields.add("  \"Categories\": [" + joinQuoted(properties.getItemCategories()) + "]");
        }
        if (properties.getItemQuality() != null) {
            fields.add("  \"Quality\": \"" + escapeJson(properties.getItemQuality()) + "\"");
        }
        if (properties.getItemLevel() != null) {
            fields.add("  \"ItemLevel\": " + properties.getItemLevel());
        }
        if (properties.getItemPlayerAnimationsId() != null) {
            fields.add("  \"PlayerAnimationsId\": \"" + escapeJson(properties.getItemPlayerAnimationsId()) + "\"");
        }
        if (properties.getItemSoundSetId() != null) {
            fields.add("  \"ItemSoundSetId\": \"" + escapeJson(properties.getItemSoundSetId()) + "\"");
        }
        String blockTypeJson = toBlockJson(registration, assetsDir, runtimeAssetsDir).trim();
        fields.add("  \"BlockType\": " + blockTypeJson);
        return "{\n" + String.join(",\n", fields) + "\n}\n";
    }

    private String toAutoBlockItemOverrideJson(
            String itemId,
            String targetBlockId,
            BlockOverridePatch patch,
            Path assetsDir,
            Path runtimeAssetsDir
    ) throws IOException {
        List<String> fields = new ArrayList<>();
        fields.add("  \"Parent\": \"" + escapeJson(itemId) + "\"");

        BlockProperties blockPropertiesPatch = patch.blockPropertiesPatch();
        BlockPropertiesOverride blockPropertiesOverridePatch = patch.blockPropertiesOverridePatch();
        Asset iconAsset = blockPropertiesPatch != null
                ? blockPropertiesPatch.getItemIcon()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getItemIcon() : null;
        if (iconAsset != null) {
            fields.add("  \"Icon\": \"" + escapeJson(copyAssetToRuntimePack(iconAsset, assetsDir, runtimeAssetsDir)) + "\"");
        }
        String displayNameKey = blockPropertiesPatch != null
                ? blockPropertiesPatch.getItemDisplayNameKey()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getItemDisplayNameKey() : null;
        String displayDescriptionKey = blockPropertiesPatch != null
                ? blockPropertiesPatch.getItemDisplayDescriptionKey()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getItemDisplayDescriptionKey() : null;
        appendTranslationProperties(fields, displayNameKey, displayDescriptionKey);
        List<String> categories = blockPropertiesPatch != null
                ? blockPropertiesPatch.getItemCategories()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getItemCategories() : List.of();
        if (!categories.isEmpty()) {
            fields.add("  \"Categories\": [" + joinQuoted(categories) + "]");
        }
        String quality = blockPropertiesPatch != null
                ? blockPropertiesPatch.getItemQuality()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getItemQuality() : null;
        if (quality != null) {
            fields.add("  \"Quality\": \"" + escapeJson(quality) + "\"");
        }
        Integer itemLevel = blockPropertiesPatch != null
                ? blockPropertiesPatch.getItemLevel()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getItemLevel() : null;
        if (itemLevel != null) {
            fields.add("  \"ItemLevel\": " + itemLevel);
        }
        String playerAnimationsId = blockPropertiesPatch != null
                ? blockPropertiesPatch.getItemPlayerAnimationsId()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getItemPlayerAnimationsId() : null;
        if (playerAnimationsId != null) {
            fields.add("  \"PlayerAnimationsId\": \"" + escapeJson(playerAnimationsId) + "\"");
        }
        String itemSoundSetId = blockPropertiesPatch != null
                ? blockPropertiesPatch.getItemSoundSetId()
                : blockPropertiesOverridePatch != null ? blockPropertiesOverridePatch.getItemSoundSetId() : null;
        if (itemSoundSetId != null) {
            fields.add("  \"ItemSoundSetId\": \"" + escapeJson(itemSoundSetId) + "\"");
        }
        String blockTypePatchJson = toBlockOverrideJson(targetBlockId, patch, assetsDir, runtimeAssetsDir).trim();
        fields.add("  \"BlockType\": " + blockTypePatchJson);
        return "{\n" + String.join(",\n", fields) + "\n}\n";
    }

    private String copyAssetToRuntimePack(Asset asset, Path assetsDir, Path runtimeAssetsDir) throws IOException {
        return asset.copyToRuntimePack(assetsDir, runtimeAssetsDir);
    }

    private String copyAssetToRuntimePackAs(Asset asset, AssetTarget target, Path assetsDir, Path runtimeAssetsDir) throws IOException {
        return asset.copyToRuntimePack(assetsDir, runtimeAssetsDir, target);
    }

    private static String joinQuoted(List<String> values) {
        List<String> quoted = new ArrayList<>(values.size());
        for (String value : values) {
            quoted.add("\"" + escapeJson(value) + "\"");
        }
        return String.join(", ", quoted);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String namespacedId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID must not be blank.");
        }
        return id.startsWith(NAMESPACE_PREFIX) ? id : NAMESPACE_PREFIX + id;
    }

    private void appendTranslationProperties(List<String> fields, String displayNameKey, String displayDescriptionKey) {
        String resolvedName = displayNameKey == null ? null : displayNameKey.trim();
        String resolvedDescription = displayDescriptionKey == null ? null : displayDescriptionKey.trim();
        if ((resolvedName == null || resolvedName.isBlank())
                && (resolvedDescription == null || resolvedDescription.isBlank())) {
            return;
        }
        List<String> translationFields = new ArrayList<>(2);
        if (resolvedName != null && !resolvedName.isBlank()) {
            translationFields.add("    \"Name\": \"" + escapeJson(resolvedName) + "\"");
        }
        if (resolvedDescription != null && !resolvedDescription.isBlank()) {
            translationFields.add("    \"Description\": \"" + escapeJson(resolvedDescription) + "\"");
        }
        fields.add("  \"TranslationProperties\": {\n" + String.join(",\n", translationFields) + "\n  }");
    }

    private String defaultItemTranslationKey(String itemId, String translationKey) {
        String rawItemId = itemId != null && itemId.startsWith(NAMESPACE_PREFIX)
                ? itemId.substring(NAMESPACE_PREFIX.length())
                : itemId;
        return TRANSLATION_FILE_PREFIX
                + normalizeTranslationToken(modGroupSupplier.get()).toLowerCase(java.util.Locale.ROOT)
                + "_"
                + normalizeTranslationToken(modIdSupplier.get()).toLowerCase(java.util.Locale.ROOT)
                + "_"
                + normalizeTranslationToken(rawItemId)
                + "."
                + normalizeTranslationToken(translationKey).toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeTranslationToken(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String normalized = value.trim().replaceAll("[^A-Za-z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+", "");
        normalized = normalized.replaceAll("_+$", "");
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private static Object unwrap(Object value) {
        Object current = value;
        while (current instanceof Wrapper wrapper) {
            Object unwrapped = wrapper.unwrap();
            if (unwrapped == current) {
                break;
            }
            current = unwrapped;
        }
        return current;
    }

    private String resolveOverrideTargetId(String rawId) {
        if (rawId.startsWith(NAMESPACE_PREFIX)) {
            return rawId;
        }
        String namespaced = namespacedId(rawId);
        if (pendingRegistrationsByBlockId.containsKey(namespaced)) {
            return namespaced;
        }
        return rawId;
    }

    private boolean resolveGenerateItemFlag(BlockOverridePatch patch) {
        BlockProperties full = patch.blockPropertiesPatch();
        if (full != null && full.getGenerateItem() != null) {
            return full.getGenerateItem();
        }
        BlockPropertiesOverride override = patch.blockPropertiesOverridePatch();
        if (override == null) {
            return false;
        }
        return override.getGenerateItem() == null || override.getGenerateItem();
    }

    private String resolveOverrideItemId(String targetBlockId, BlockOverridePatch patch) {
        BlockProperties full = patch.blockPropertiesPatch();
        BlockPropertiesOverride override = patch.blockPropertiesOverridePatch();
        String rawItemId = full != null ? full.getItemId() : override != null ? override.getItemId() : null;
        if (rawItemId == null || rawItemId.isBlank()) {
            return targetBlockId;
        }
        return rawItemId.startsWith(NAMESPACE_PREFIX) ? rawItemId : namespacedId(rawItemId);
    }

    private record PendingBlockRegistration(
            String blockId,
            BlockProperties properties,
            BlockSpecialProperties specialProperties
    ) {
    }

    private record BlockOverridePatch(
            BlockProperties blockPropertiesPatch,
            BlockPropertiesOverride blockPropertiesOverridePatch,
            BlockSpecialProperties blockSpecialPatch,
            BlockSpecialPropertiesOverride blockSpecialOverridePatch
    ) {
        private Map<String, String> resolveSpecialRawFields() {
            if (blockSpecialPatch != null) {
                return blockSpecialPatch.getRawFields();
            }
            if (blockSpecialOverridePatch != null) {
                return blockSpecialOverridePatch.getRawFields();
            }
            return Map.of();
        }
    }
}
