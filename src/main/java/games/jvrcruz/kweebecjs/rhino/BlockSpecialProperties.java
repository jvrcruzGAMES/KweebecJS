package games.jvrcruz.kweebecjs.rhino;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BlockSpecialProperties {
    private final Map<String, String> rawFields;
    private final Recipe recipe;

    private BlockSpecialProperties(Map<String, String> rawFields, Recipe recipe) {
        this.rawFields = Map.copyOf(rawFields);
        this.recipe = recipe;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, String> getRawFields() {
        return rawFields;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public static final class Builder {
        private final Map<String, String> rawFields = new LinkedHashMap<>();
        private Recipe recipe;

        private Builder() {
        }

        public Builder group(String value) {
            return stringField("Group", value);
        }

        public Builder blockListAssetId(String value) {
            return stringField("BlockListAssetId", value);
        }

        public Builder prefabListAssetId(String value) {
            return stringField("PrefabListAssetId", value);
        }

        public Builder blockParticleSetId(String value) {
            return stringField("BlockParticleSetId", value);
        }

        public Builder particles(BlockArray value) {
            return complex("Particles", value);
        }

        public Builder blockBreakingDecalId(String value) {
            return stringField("BlockBreakingDecalId", value);
        }

        public Builder connectedBlockRuleSet(BlockConnectedBlockRuleSet value) {
            return complex("ConnectedBlockRuleSet", value);
        }

        public Builder customModelScale(double value) {
            return numberField("CustomModelScale", value);
        }

        public Builder customModelAnimation(String value) {
            return stringField("CustomModelAnimation", value);
        }

        public Builder seats(BlockSeats value) {
            return complex("Seats", value);
        }

        public Builder beds(BlockBeds value) {
            return complex("Beds", value);
        }

        public Builder interactionHint(String value) {
            return stringField("InteractionHint", value);
        }

        public Builder interactionSoundEventId(String value) {
            return stringField("InteractionSoundEventId", value);
        }

        public Builder ambientSoundEventId(String value) {
            return stringField("AmbientSoundEventId", value);
        }

        public Builder isLooping(boolean value) {
            return booleanField("IsLooping", value);
        }

        public Builder isTrigger(boolean value) {
            return booleanField("IsTrigger", value);
        }

        public Builder isDoor(boolean value) {
            return booleanField("IsDoor", value);
        }

        public Builder allowsMultipleUsers(boolean value) {
            return booleanField("AllowsMultipleUsers", value);
        }

        public Builder damageToEntities(int value) {
            return integerField("DamageToEntities", value);
        }

        public Builder requiresAlphaBlending(boolean value) {
            return booleanField("RequiresAlphaBlending", value);
        }

        public Builder textureSideMask(String value) {
            return stringField("TextureSideMask", value);
        }

        public Builder transitionTexture(String value) {
            return stringField("TransitionTexture", value);
        }

        public Builder transitionToTag(String value) {
            return stringField("TransitionToTag", value);
        }

        public Builder transitionToGroups(BlockArray value) {
            return complex("TransitionToGroups", value);
        }

        public Builder randomRotation(String value) {
            return stringField("RandomRotation", value);
        }

        public Builder variantRotation(String value) {
            return stringField("VariantRotation", value);
        }

        public Builder supportDropType(String value) {
            return stringField("SupportDropType", value);
        }

        public Builder maxSupportDistance(int value) {
            return integerField("MaxSupportDistance", value);
        }

        public Builder ignoreSupportWhenPlaced(boolean value) {
            return booleanField("IgnoreSupportWhenPlaced", value);
        }

        public Builder effect(BlockArray value) {
            return complex("Effect", value);
        }

        public Builder hitboxType(String value) {
            return stringField("HitboxType", value);
        }

        public Builder interactionHitboxType(String value) {
            return stringField("InteractionHitboxType", value);
        }

        public Builder flags(BlockFlags value) {
            return complex("Flags", value);
        }

        public Builder light(BlockLight value) {
            return complex("Light", value);
        }

        public Builder textures(BlockArray value) {
            return complex("Textures", value);
        }

        public Builder tintUp(BlockArray value) {
            return complex("TintUp", value);
        }

        public Builder tintDown(BlockArray value) {
            return complex("TintDown", value);
        }

        public Builder tintNorth(BlockArray value) {
            return complex("TintNorth", value);
        }

        public Builder tintSouth(BlockArray value) {
            return complex("TintSouth", value);
        }

        public Builder tintWest(BlockArray value) {
            return complex("TintWest", value);
        }

        public Builder tintEast(BlockArray value) {
            return complex("TintEast", value);
        }

        public Builder customModelTexture(BlockArray value) {
            return complex("CustomModelTexture", value);
        }

        public Builder gathering(BlockGathering value) {
            return complex("Gathering", value);
        }

        public Builder support(BlockSupport value) {
            return complex("Support", value);
        }

        public Builder supporting(BlockSupporting value) {
            return complex("Supporting", value);
        }

        public Builder interactions(BlockInteractions value) {
            return complex("Interactions", value);
        }

        public Builder state(BlockState value) {
            return complex("State", value);
        }

        public Builder placementSettings(BlockPlacementSettings value) {
            return complex("PlacementSettings", value);
        }

        public Builder movementSettings(BlockMovementSettings value) {
            return complex("MovementSettings", value);
        }

        public Builder bench(BlockBench value) {
            return complex("Bench", value);
        }

        public Builder farming(BlockFarming value) {
            return complex("Farming", value);
        }

        public Builder blockEntity(BlockEntity value) {
            return complex("BlockEntity", value);
        }

        public Builder railConfig(BlockRailConfig value) {
            return complex("RailConfig", value);
        }

        public Builder conditionalSounds(BlockArray value) {
            return complex("ConditionalSounds", value);
        }

        public Builder complex(String fieldName, BlockJsonValue value) {
            if (fieldName == null || fieldName.isBlank()) {
                throw new IllegalArgumentException("fieldName must not be blank.");
            }
            if (value == null) {
                throw new IllegalArgumentException("value must not be null.");
            }
            rawFields.put(fieldName, value.toJson());
            return this;
        }

        public Builder recipe(Recipe value) {
            if (value == null) {
                throw new IllegalArgumentException("recipe must not be null.");
            }
            this.recipe = value;
            return this;
        }

        public BlockSpecialProperties build() {
            return new BlockSpecialProperties(rawFields, recipe);
        }

        private Builder stringField(String fieldName, String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(fieldName + " must not be blank.");
            }
            rawFields.put(fieldName, "\"" + escapeJson(value) + "\"");
            return this;
        }

        private Builder numberField(String fieldName, double value) {
            String formatted = value == (long) value ? Long.toString((long) value) : Double.toString(value);
            rawFields.put(fieldName, formatted);
            return this;
        }

        private Builder integerField(String fieldName, int value) {
            rawFields.put(fieldName, Integer.toString(value));
            return this;
        }

        private Builder booleanField(String fieldName, boolean value) {
            rawFields.put(fieldName, Boolean.toString(value));
            return this;
        }

        private static String escapeJson(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
