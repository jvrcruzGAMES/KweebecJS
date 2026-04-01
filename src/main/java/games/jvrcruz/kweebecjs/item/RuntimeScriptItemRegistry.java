package games.jvrcruz.kweebecjs.item;

import games.jvrcruz.kweebecjs.asset.RuntimeItemAssetManager;
import games.jvrcruz.kweebecjs.rhino.Asset;
import games.jvrcruz.kweebecjs.rhino.ItemEntry;
import games.jvrcruz.kweebecjs.rhino.ItemProperties;
import games.jvrcruz.kweebecjs.rhino.ItemPropertiesOverride;
import games.jvrcruz.kweebecjs.rhino.ItemSpecialProperties;
import games.jvrcruz.kweebecjs.rhino.ItemSpecialPropertiesOverride;
import games.jvrcruz.kweebecjs.recipe.RuntimeRecipeRegistry;
import games.jvrcruz.kweebecjs.system.SystemPaths;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import org.mozilla.javascript.Wrapper;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class RuntimeScriptItemRegistry {
    private static final String NAMESPACE_PREFIX = "KweebecJS_";
    private static final String TRANSLATION_FILE_PREFIX = "KweebecJS.";
    private static final String GENERATED_ITEMS_DIRECTORY = "Server/Item/Items/KweebecJS";
    private static final String GENERATED_COMMON_ASSETS_DIRECTORY = "Common/KweebecJS/Generated";
    private final Class<?> pluginClass;
    private final Supplier<String> modGroupSupplier;
    private final Supplier<String> modIdSupplier;
    private final RuntimeItemAssetManager runtimeItemAssetManager;
    private final RuntimeRecipeRegistry runtimeRecipeRegistry;
    private final Map<String, ItemEntry> pendingEntriesById = new LinkedHashMap<>();
    private final Map<String, ItemOverridePatch> pendingOverridesById = new LinkedHashMap<>();
    private final Set<String> generatedItemJsonPaths = new LinkedHashSet<>();

    public RuntimeScriptItemRegistry(
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
        Object entryArg = args.length >= 1 ? unwrap(args[0]) : null;
        if (!(entryArg instanceof ItemEntry entry)) {
            throw new IllegalArgumentException("event.register(itemEntry) expects an ItemEntry instance.");
        }
        String namespacedId = namespacedId(entry.getId());
        pendingEntriesById.put(
                namespacedId,
                new ItemEntry(namespacedId, entry.getProperties(), entry.getSpecialProperties())
        );
        return namespacedId;
    }

    public synchronized String overrideFromArgs(Object[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: event.override(itemId, anyPatch)");
        }
        String rawId = String.valueOf(args[0]);
        if (rawId == null || rawId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank.");
        }
        String targetId = resolveOverrideTargetId(rawId);

        Object patchArg = unwrap(args[1]);
        ItemProperties itemPropertiesPatch = patchArg instanceof ItemProperties itemProperties ? itemProperties : null;
        ItemPropertiesOverride itemPropertiesOverridePatch = patchArg instanceof ItemPropertiesOverride override ? override : null;
        ItemSpecialProperties itemSpecialPatch = patchArg instanceof ItemSpecialProperties specialProperties ? specialProperties : null;
        ItemSpecialPropertiesOverride itemSpecialOverridePatch = patchArg instanceof ItemSpecialPropertiesOverride specialPropertiesOverride
                ? specialPropertiesOverride
                : null;
        if (itemPropertiesPatch == null
                && itemPropertiesOverridePatch == null
                && itemSpecialPatch == null
                && itemSpecialOverridePatch == null) {
            throw new IllegalArgumentException(
                    "anyPatch must be ItemProperties, ItemPropertiesOverride, ItemSpecialProperties, or ItemSpecialPropertiesOverride."
            );
        }

        ItemOverridePatch incomingPatch = new ItemOverridePatch(
                itemPropertiesPatch,
                itemPropertiesOverridePatch,
                itemSpecialPatch,
                itemSpecialOverridePatch
        );
        ItemOverridePatch existingPatch = pendingOverridesById.get(targetId);
        pendingOverridesById.put(targetId, existingPatch == null ? incomingPatch : existingPatch.merge(incomingPatch));
        return targetId;
    }

    public synchronized void clearRegisteredItems() {
        for (String generatedItemJsonPath : generatedItemJsonPaths) {
            runtimeItemAssetManager.removeGeneratedAsset(generatedItemJsonPath);
        }
        generatedItemJsonPaths.clear();
        pendingEntriesById.clear();
        pendingOverridesById.clear();

        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
        deleteDirectoryIfPresent(runtimeAssetsDir.resolve(GENERATED_COMMON_ASSETS_DIRECTORY));
    }

    public synchronized int applyRegisteredItems() throws IOException {
        if (pendingEntriesById.isEmpty() && pendingOverridesById.isEmpty()) {
            return 0;
        }

        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
        Path assetsDir = SystemPaths.resolveAssetsDir(pluginClass);
        Files.createDirectories(runtimeAssetsDir.resolve(GENERATED_ITEMS_DIRECTORY));
        Files.createDirectories(runtimeAssetsDir.resolve(GENERATED_COMMON_ASSETS_DIRECTORY));

        Map<String, String> generatedJsonByPath = new LinkedHashMap<>();
        generatedItemJsonPaths.clear();

        for (ItemEntry entry : pendingEntriesById.values()) {
            String itemPath = GENERATED_ITEMS_DIRECTORY + "/" + entry.getId() + ".json";
            String json = toItemJson(entry, assetsDir, runtimeAssetsDir);
            generatedJsonByPath.put(itemPath, json);
            generatedItemJsonPaths.add(itemPath);
            if (entry.getSpecialProperties() != null && entry.getSpecialProperties().getRecipe() != null) {
                runtimeRecipeRegistry.addForRegisteredAsset(entry.getId(), new Object[]{entry.getSpecialProperties().getRecipe()});
            }
        }

        for (Map.Entry<String, ItemOverridePatch> overrideEntry : pendingOverridesById.entrySet()) {
            String targetId = overrideEntry.getKey();
            String itemPath = GENERATED_ITEMS_DIRECTORY + "/" + targetId + ".json";
            String json = toItemOverrideJson(targetId, overrideEntry.getValue(), assetsDir, runtimeAssetsDir);
            generatedJsonByPath.put(itemPath, json);
            generatedItemJsonPaths.add(itemPath);
        }

        runtimeItemAssetManager.putGeneratedAssets(generatedJsonByPath);
        return pendingEntriesById.size() + pendingOverridesById.size();
    }

    private String toItemJson(ItemEntry entry, Path assetsDir, Path runtimeAssetsDir) throws IOException {
        ItemProperties properties = entry.getProperties();
        List<String> fields = new ArrayList<>();
        fields.add("  \"Parent\": \"" + escapeJson(properties.getParent()) + "\"");

        String displayNameKey = properties.getDisplayNameKey() != null
                ? properties.getDisplayNameKey().trim()
                : defaultItemTranslationKey(entry.getId(), "name");
        String displayDescriptionKey = properties.getDisplayDescriptionKey() != null
                ? properties.getDisplayDescriptionKey().trim()
                : defaultItemTranslationKey(entry.getId(), "description");
        appendTranslationProperties(fields, displayNameKey, displayDescriptionKey);

        if (!properties.getCategories().isEmpty()) {
            fields.add("  \"Categories\": [" + joinQuoted(properties.getCategories()) + "]");
        }

        if (properties.getModel() != null) {
            String modelPath = copyAssetToRuntimePack(properties.getModel(), assetsDir, runtimeAssetsDir);
            fields.add("  \"Model\": \"" + escapeJson(modelPath) + "\"");
        }
        if (properties.getTexture() != null) {
            String texturePath = copyAssetToRuntimePack(properties.getTexture(), assetsDir, runtimeAssetsDir);
            fields.add("  \"Texture\": \"" + escapeJson(texturePath) + "\"");
        }
        if (properties.getIcon() != null) {
            String iconPath = copyAssetToRuntimePack(properties.getIcon(), assetsDir, runtimeAssetsDir);
            fields.add("  \"Icon\": \"" + escapeJson(iconPath) + "\"");
        }

        if (properties.getPlayerAnimationsId() != null) {
            fields.add("  \"PlayerAnimationsId\": \"" + escapeJson(properties.getPlayerAnimationsId()) + "\"");
        }
        if (properties.getDropOnDeath() != null) {
            fields.add("  \"DropOnDeath\": " + properties.getDropOnDeath());
        }
        appendSpecialProperties(fields, properties, entry.getSpecialProperties());

        return "{\n" + String.join(",\n", fields) + "\n}\n";
    }

    private String copyAssetToRuntimePack(Asset asset, Path assetsDir, Path runtimeAssetsDir) throws IOException {
        return asset.copyToRuntimePack(assetsDir, runtimeAssetsDir);
    }

    private String toItemOverrideJson(
            String targetId,
            ItemOverridePatch patch,
            Path assetsDir,
            Path runtimeAssetsDir
    ) throws IOException {
        BsonDocument baseDocument = loadBaseItemDocument(targetId, assetsDir, runtimeAssetsDir);
        BsonDocument patchDocument = buildItemOverridePatchDocument(
                targetId,
                patch,
                assetsDir,
                runtimeAssetsDir,
                baseDocument
        );
        if (patchDocument.isEmpty()) {
            throw new IllegalArgumentException("event.override for item '" + targetId + "' requires at least one patch field.");
        }
        BsonDocument mergedDocument = mergeTopLevel(baseDocument, patchDocument);
        return mergedDocument.toJson() + "\n";
    }

    private BsonDocument buildItemOverridePatchDocument(
            String targetId,
            ItemOverridePatch patch,
            Path assetsDir,
            Path runtimeAssetsDir,
            BsonDocument baseDocument
    ) throws IOException {
        List<String> fields = new ArrayList<>();
        ItemProperties itemPropertiesPatch = patch.itemPropertiesPatch();
        ItemPropertiesOverride itemPropertiesOverridePatch = patch.itemPropertiesOverridePatch();

        String explicitParent = null;
        if (itemPropertiesPatch != null && itemPropertiesPatch.getParent() != null && !itemPropertiesPatch.getParent().isBlank()) {
            explicitParent = itemPropertiesPatch.getParent();
        } else if (itemPropertiesOverridePatch != null && itemPropertiesOverridePatch.getParent() != null && !itemPropertiesOverridePatch.getParent().isBlank()) {
            explicitParent = itemPropertiesOverridePatch.getParent();
        }
        if (explicitParent != null) {
            fields.add("  \"Parent\": \"" + escapeJson(explicitParent) + "\"");
        }

        appendItemPropertyFields(fields, itemPropertiesPatch, itemPropertiesOverridePatch, assetsDir, runtimeAssetsDir);
        appendSpecialProperties(
                fields,
                resolveItemPropertyContextForOverride(targetId, baseDocument, itemPropertiesPatch, itemPropertiesOverridePatch),
                patch.resolveSpecialPatch()
        );
        if (fields.isEmpty()) {
            return new BsonDocument();
        }
        return BsonDocument.parse("{\n" + String.join(",\n", fields) + "\n}\n");
    }

    private void appendItemPropertyFields(
        List<String> fields,
        ItemProperties properties,
        ItemPropertiesOverride propertiesOverride,
        Path assetsDir,
        Path runtimeAssetsDir
    ) throws IOException {
        String displayNameKey = properties != null
                ? properties.getDisplayNameKey()
                : propertiesOverride != null ? propertiesOverride.getDisplayNameKey() : null;
        String displayDescriptionKey = properties != null
                ? properties.getDisplayDescriptionKey()
                : propertiesOverride != null ? propertiesOverride.getDisplayDescriptionKey() : null;
        appendTranslationProperties(fields, displayNameKey, displayDescriptionKey);

        List<String> categories = properties != null
                ? properties.getCategories()
                : propertiesOverride != null ? propertiesOverride.getCategories() : List.of();
        if (!categories.isEmpty()) {
            fields.add("  \"Categories\": [" + joinQuoted(categories) + "]");
        }

        Asset model = properties != null
                ? properties.getModel()
                : propertiesOverride != null ? propertiesOverride.getModel() : null;
        if (model != null) {
            String modelPath = copyAssetToRuntimePack(model, assetsDir, runtimeAssetsDir);
            fields.add("  \"Model\": \"" + escapeJson(modelPath) + "\"");
        }

        Asset texture = properties != null
                ? properties.getTexture()
                : propertiesOverride != null ? propertiesOverride.getTexture() : null;
        if (texture != null) {
            String texturePath = copyAssetToRuntimePack(texture, assetsDir, runtimeAssetsDir);
            fields.add("  \"Texture\": \"" + escapeJson(texturePath) + "\"");
        }

        Asset icon = properties != null
                ? properties.getIcon()
                : propertiesOverride != null ? propertiesOverride.getIcon() : null;
        if (icon != null) {
            String iconPath = copyAssetToRuntimePack(icon, assetsDir, runtimeAssetsDir);
            fields.add("  \"Icon\": \"" + escapeJson(iconPath) + "\"");
        }

        String playerAnimationsId = properties != null
                ? properties.getPlayerAnimationsId()
                : propertiesOverride != null ? propertiesOverride.getPlayerAnimationsId() : null;
        if (playerAnimationsId != null) {
            fields.add("  \"PlayerAnimationsId\": \"" + escapeJson(playerAnimationsId) + "\"");
        }

        Boolean dropOnDeath = properties != null
                ? properties.getDropOnDeath()
                : propertiesOverride != null ? propertiesOverride.getDropOnDeath() : null;
        if (dropOnDeath != null) {
            fields.add("  \"DropOnDeath\": " + dropOnDeath);
        }
    }

    private static ItemProperties resolveItemPropertyContextForOverride(
            String targetId,
            BsonDocument baseDocument,
            ItemProperties properties,
            ItemPropertiesOverride propertiesOverride
    ) {
        if (properties != null) {
            return properties;
        }
        String baseParent = readString(baseDocument, "Parent");
        ItemProperties.Builder builder = ItemProperties.builder()
                .parent(propertiesOverride != null
                        && propertiesOverride.getParent() != null
                        && !propertiesOverride.getParent().isBlank()
                        ? propertiesOverride.getParent()
                        : baseParent != null && !baseParent.isBlank() ? baseParent : targetId);
        if (propertiesOverride != null && propertiesOverride.getPlayerAnimationsId() != null) {
            builder.playerAnimationsId(propertiesOverride.getPlayerAnimationsId());
        } else {
            String playerAnimationsId = readString(baseDocument, "PlayerAnimationsId");
            if (playerAnimationsId != null && !playerAnimationsId.isBlank()) {
                builder.playerAnimationsId(playerAnimationsId);
            } else if (targetId.contains("Weapon_Sword")) {
                builder.playerAnimationsId("Sword");
            }
        }
        if (propertiesOverride != null && !propertiesOverride.getCategories().isEmpty()) {
            for (String category : propertiesOverride.getCategories()) {
                builder.category(category);
            }
        } else {
            for (String category : readStringArray(baseDocument, "Categories")) {
                builder.category(category);
            }
        }
        return builder.build();
    }

    private BsonDocument loadBaseItemDocument(String targetId, Path assetsDir, Path runtimeAssetsDir) throws IOException {
        ItemEntry pendingEntry = pendingEntriesById.get(targetId);
        if (pendingEntry != null) {
            return BsonDocument.parse(toItemJson(pendingEntry, assetsDir, runtimeAssetsDir));
        }

        Item originalItem = Item.getAssetMap().getAssetMap().get(targetId);
        if (originalItem == null) {
            throw new IllegalArgumentException("event.override target item '" + targetId + "' does not exist.");
        }

        Path originalPath = Item.getAssetMap().getPath(targetId);
        if (originalPath != null && Files.exists(originalPath)) {
            try {
                return BsonDocument.parse(Files.readString(originalPath, StandardCharsets.UTF_8));
            } catch (RuntimeException parseException) {
                // Fall back to codec output when source JSON cannot be read as plain JSON.
            }
        }

        BsonValue encodedValue = Item.CODEC.encode(originalItem, new ExtraInfo());
        if (encodedValue == null || !encodedValue.isDocument()) {
            throw new IllegalStateException("Could not serialize base item '" + targetId + "' to a document.");
        }
        return BsonDocument.parse(encodedValue.asDocument().toJson());
    }

    private static BsonDocument mergeTopLevel(BsonDocument baseDocument, BsonDocument patchDocument) {
        BsonDocument merged = BsonDocument.parse(baseDocument.toJson());
        for (Map.Entry<String, BsonValue> patchField : patchDocument.entrySet()) {
            merged.put(patchField.getKey(), patchField.getValue());
        }
        return merged;
    }

    private static String readString(BsonDocument document, String key) {
        BsonValue value = document.get(key);
        if (value instanceof BsonString stringValue) {
            return stringValue.getValue();
        }
        return null;
    }

    private static List<String> readStringArray(BsonDocument document, String key) {
        BsonValue value = document.get(key);
        if (!(value instanceof BsonArray array)) {
            return List.of();
        }
        List<String> values = new ArrayList<>(array.size());
        for (BsonValue element : array) {
            if (element instanceof BsonString stringValue) {
                values.add(stringValue.getValue());
            }
        }
        return values;
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
            throw new IllegalArgumentException("Item ID must not be blank.");
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

    private static void appendSpecialProperties(
            List<String> fields,
            ItemProperties itemProperties,
            ItemSpecialProperties specialProperties
    ) {
        if (specialProperties == null) {
            return;
        }

        boolean weaponSupported = isWeaponItem(itemProperties);
        boolean swordSupported = isSwordItem(itemProperties);

        if (specialProperties.getDroppedItemAnimation() != null) {
            fields.add("  \"DroppedItemAnimation\": \"" + escapeJson(specialProperties.getDroppedItemAnimation()) + "\"");
        }
        if (specialProperties.getReticle() != null && weaponSupported) {
            fields.add("  \"Reticle\": \"" + escapeJson(specialProperties.getReticle()) + "\"");
        }
        if (specialProperties.getQuality() != null) {
            fields.add("  \"Quality\": \"" + escapeJson(specialProperties.getQuality()) + "\"");
        }
        if (specialProperties.getItemLevel() != null) {
            fields.add("  \"ItemLevel\": " + specialProperties.getItemLevel());
        }
        if (specialProperties.getUtilityCompatible() != null) {
            fields.add("""
                      "Utility": {
                        "Compatible": %s
                      }""".formatted(Boolean.toString(specialProperties.getUtilityCompatible())));
        }
        if ((specialProperties.getInteractionPrimary() != null
                || specialProperties.getInteractionSecondary() != null
                || specialProperties.getInteractionAbility1() != null) && weaponSupported) {
            fields.add(buildInteractionsJson(
                    specialProperties.getInteractionPrimary(),
                    specialProperties.getInteractionSecondary(),
                    specialProperties.getInteractionAbility1()
            ));
        }
        if (!specialProperties.getTypeTags().isEmpty() || !specialProperties.getFamilyTags().isEmpty()) {
            fields.add(buildTagsJson(specialProperties.getTypeTags(), specialProperties.getFamilyTags()));
        }
        if (specialProperties.getItemSoundSetId() != null) {
            fields.add("  \"ItemSoundSetId\": \"" + escapeJson(specialProperties.getItemSoundSetId()) + "\"");
        }
        if (specialProperties.getMaxDurability() != null) {
            fields.add("  \"MaxDurability\": " + specialProperties.getMaxDurability());
        }
        if (specialProperties.getDurabilityLossOnHit() != null) {
            fields.add("  \"DurabilityLossOnHit\": " + formatFloat(specialProperties.getDurabilityLossOnHit()));
        }
        if ((specialProperties.getSignatureEnergyBonus() != null
                || !specialProperties.getWeaponEntityStatsToClear().isEmpty()) && weaponSupported) {
            fields.add(buildWeaponJson(
                    specialProperties.getSignatureEnergyBonus(),
                    specialProperties.getWeaponEntityStatsToClear()
            ));
        }
        if (specialProperties.getDamage() != null && swordSupported) {
            fields.add(buildSwordInteractionVarsJson(specialProperties.getDamage()));
        }
    }

    private static boolean isWeaponItem(ItemProperties properties) {
        if (properties.getParent().startsWith("Template_Weapon_")) {
            return true;
        }
        if ("Sword".equalsIgnoreCase(properties.getPlayerAnimationsId())) {
            return true;
        }
        for (String category : properties.getCategories()) {
            if (category != null && category.startsWith("Items.Weapons")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSwordItem(ItemProperties properties) {
        if ("Sword".equalsIgnoreCase(properties.getPlayerAnimationsId())) {
            return true;
        }
        return properties.getParent().contains("Weapon_Sword");
    }

    private static String buildInteractionsJson(String primary, String secondary, String ability1) {
        List<String> interactionFields = new ArrayList<>(3);
        if (primary != null) {
            interactionFields.add("    \"Primary\": \"" + escapeJson(primary) + "\"");
        }
        if (secondary != null) {
            interactionFields.add("    \"Secondary\": \"" + escapeJson(secondary) + "\"");
        }
        if (ability1 != null) {
            interactionFields.add("    \"Ability1\": \"" + escapeJson(ability1) + "\"");
        }
        return "  \"Interactions\": {\n" + String.join(",\n", interactionFields) + "\n  }";
    }

    private static String buildTagsJson(List<String> typeTags, List<String> familyTags) {
        List<String> tagFields = new ArrayList<>(2);
        if (!typeTags.isEmpty()) {
            tagFields.add("    \"Type\": [" + joinQuoted(typeTags) + "]");
        }
        if (!familyTags.isEmpty()) {
            tagFields.add("    \"Family\": [" + joinQuoted(familyTags) + "]");
        }
        return "  \"Tags\": {\n" + String.join(",\n", tagFields) + "\n  }";
    }

    private static String buildWeaponJson(Integer signatureEnergyBonus, List<String> entityStatsToClear) {
        List<String> weaponFields = new ArrayList<>(2);
        if (!entityStatsToClear.isEmpty()) {
            weaponFields.add("    \"EntityStatsToClear\": [" + joinQuoted(entityStatsToClear) + "]");
        }
        if (signatureEnergyBonus != null) {
            weaponFields.add("""
                            "StatModifiers": {
                              "SignatureEnergy": [
                                {
                                  "Amount": %d,
                                  "CalculationType": "Additive"
                                }
                              ]
                            }""".formatted(signatureEnergyBonus));
        }
        return "  \"Weapon\": {\n" + String.join(",\n", weaponFields) + "\n  }";
    }

    private static String formatFloat(double value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String buildSwordInteractionVarsJson(int baseDamage) {
        int swingLeft = baseDamage;
        int swingRight = baseDamage;
        int swingDown = scaleDamage(baseDamage, 1.8);
        int thrust = scaleDamage(baseDamage, 2.6);
        int vortexSpin = scaleDamage(baseDamage, 1.9);
        int vortexStab = scaleDamage(baseDamage, 5.6);
        return """
                  "InteractionVars": {
                    "Swing_Left_Damage": {
                      "Interactions": [
                        {
                          "Parent": "Weapon_Sword_Primary_Swing_Left_Damage",
                          "DamageCalculator": {
                            "BaseDamage": {
                              "Physical": %d
                            }
                          },
                          "DamageEffects": {
                            "WorldSoundEventId": "SFX_Sword_T2_Impact",
                            "LocalSoundEventId": "SFX_Sword_T2_Impact"
                          }
                        }
                      ]
                    },
                    "Swing_Right_Damage": {
                      "Interactions": [
                        {
                          "Parent": "Weapon_Sword_Primary_Swing_Right_Damage",
                          "DamageCalculator": {
                            "BaseDamage": {
                              "Physical": %d
                            }
                          },
                          "DamageEffects": {
                            "WorldSoundEventId": "SFX_Sword_T2_Impact",
                            "LocalSoundEventId": "SFX_Sword_T2_Impact"
                          }
                        }
                      ]
                    },
                    "Swing_Down_Damage": {
                      "Interactions": [
                        {
                          "Parent": "Weapon_Sword_Primary_Swing_Down_Damage",
                          "DamageCalculator": {
                            "BaseDamage": {
                              "Physical": %d
                            }
                          },
                          "DamageEffects": {
                            "WorldSoundEventId": "SFX_Sword_T2_Impact",
                            "LocalSoundEventId": "SFX_Sword_T2_Impact"
                          }
                        }
                      ]
                    },
                    "Thrust_Damage": {
                      "Interactions": [
                        {
                          "Parent": "Weapon_Sword_Primary_Thrust_Damage",
                          "DamageCalculator": {
                            "BaseDamage": {
                              "Physical": %d
                            }
                          },
                          "EntityStatsOnHit": [
                            {
                              "EntityStatId": "SignatureEnergy",
                              "Amount": 3
                            }
                          ],
                          "DamageEffects": {
                            "WorldSoundEventId": "SFX_Sword_T2_Impact",
                            "LocalSoundEventId": "SFX_Sword_T2_Impact"
                          }
                        }
                      ]
                    },
                    "Vortexstrike_Spin_Damage": {
                      "Interactions": [
                        {
                          "Parent": "Weapon_Sword_Signature_Vortexstrike_Spin_Damage",
                          "DamageCalculator": {
                            "BaseDamage": {
                              "Physical": %d
                            }
                          },
                          "EntityStatsOnHit": [],
                          "DamageEffects": {
                            "WorldSoundEventId": "SFX_Sword_T2_Impact",
                            "LocalSoundEventId": "SFX_Sword_T2_Impact"
                          }
                        }
                      ]
                    },
                    "Vortexstrike_Stab_Damage": {
                      "Interactions": [
                        {
                          "Parent": "Weapon_Sword_Signature_Vortexstrike_Stab_Damage",
                          "DamageCalculator": {
                            "BaseDamage": {
                              "Physical": %d
                            }
                          },
                          "EntityStatsOnHit": [],
                          "DamageEffects": {
                            "WorldSoundEventId": "SFX_Sword_T2_Impact",
                            "LocalSoundEventId": "SFX_Sword_T2_Impact"
                          }
                        }
                      ]
                    }
                  }""".formatted(swingLeft, swingRight, swingDown, thrust, vortexSpin, vortexStab);
    }

    private static int scaleDamage(int baseDamage, double scale) {
        return Math.max(1, (int) Math.round(baseDamage * scale));
    }

    private static void deleteDirectoryIfPresent(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed deleting generated item runtime assets.", e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed scanning generated item runtime assets for cleanup.", e);
        }
    }

    private String resolveOverrideTargetId(String rawId) {
        if (rawId.startsWith(NAMESPACE_PREFIX)) {
            return rawId;
        }
        String namespaced = namespacedId(rawId);
        if (pendingEntriesById.containsKey(namespaced)) {
            return namespaced;
        }
        return rawId;
    }

    private record ItemOverridePatch(
            ItemProperties itemPropertiesPatch,
            ItemPropertiesOverride itemPropertiesOverridePatch,
            ItemSpecialProperties itemSpecialPatch,
            ItemSpecialPropertiesOverride itemSpecialOverridePatch
    ) {
        private ItemOverridePatch merge(ItemOverridePatch other) {
            return new ItemOverridePatch(
                    other.itemPropertiesPatch != null ? other.itemPropertiesPatch : itemPropertiesPatch,
                    other.itemPropertiesOverridePatch != null ? other.itemPropertiesOverridePatch : itemPropertiesOverridePatch,
                    other.itemSpecialPatch != null ? other.itemSpecialPatch : itemSpecialPatch,
                    other.itemSpecialOverridePatch != null ? other.itemSpecialOverridePatch : itemSpecialOverridePatch
            );
        }

        private ItemSpecialProperties resolveSpecialPatch() {
            return itemSpecialPatch != null ? itemSpecialPatch
                    : itemSpecialOverridePatch != null ? itemSpecialOverridePatch.getValue()
                    : null;
        }
    }
}
