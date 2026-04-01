package games.jvrcruz.kweebecjs.rhino;

import java.util.Map;

public final class BlockSpecialPropertiesOverride {
    private final BlockSpecialProperties value;

    private BlockSpecialPropertiesOverride(BlockSpecialProperties value) {
        this.value = value;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, String> getRawFields() {
        return value.getRawFields();
    }

    public static final class Builder {
        private final BlockSpecialProperties.Builder delegate = BlockSpecialProperties.builder();

        private Builder() {
        }

        public Builder group(String value) { delegate.group(value); return this; }
        public Builder blockListAssetId(String value) { delegate.blockListAssetId(value); return this; }
        public Builder prefabListAssetId(String value) { delegate.prefabListAssetId(value); return this; }
        public Builder blockParticleSetId(String value) { delegate.blockParticleSetId(value); return this; }
        public Builder particles(BlockArray value) { delegate.particles(value); return this; }
        public Builder blockBreakingDecalId(String value) { delegate.blockBreakingDecalId(value); return this; }
        public Builder connectedBlockRuleSet(BlockConnectedBlockRuleSet value) { delegate.connectedBlockRuleSet(value); return this; }
        public Builder customModelScale(double value) { delegate.customModelScale(value); return this; }
        public Builder customModelAnimation(String value) { delegate.customModelAnimation(value); return this; }
        public Builder seats(BlockSeats value) { delegate.seats(value); return this; }
        public Builder beds(BlockBeds value) { delegate.beds(value); return this; }
        public Builder interactionHint(String value) { delegate.interactionHint(value); return this; }
        public Builder interactionSoundEventId(String value) { delegate.interactionSoundEventId(value); return this; }
        public Builder ambientSoundEventId(String value) { delegate.ambientSoundEventId(value); return this; }
        public Builder isLooping(boolean value) { delegate.isLooping(value); return this; }
        public Builder isTrigger(boolean value) { delegate.isTrigger(value); return this; }
        public Builder isDoor(boolean value) { delegate.isDoor(value); return this; }
        public Builder allowsMultipleUsers(boolean value) { delegate.allowsMultipleUsers(value); return this; }
        public Builder damageToEntities(int value) { delegate.damageToEntities(value); return this; }
        public Builder requiresAlphaBlending(boolean value) { delegate.requiresAlphaBlending(value); return this; }
        public Builder textureSideMask(String value) { delegate.textureSideMask(value); return this; }
        public Builder transitionTexture(String value) { delegate.transitionTexture(value); return this; }
        public Builder transitionToTag(String value) { delegate.transitionToTag(value); return this; }
        public Builder transitionToGroups(BlockArray value) { delegate.transitionToGroups(value); return this; }
        public Builder randomRotation(String value) { delegate.randomRotation(value); return this; }
        public Builder variantRotation(String value) { delegate.variantRotation(value); return this; }
        public Builder supportDropType(String value) { delegate.supportDropType(value); return this; }
        public Builder maxSupportDistance(int value) { delegate.maxSupportDistance(value); return this; }
        public Builder ignoreSupportWhenPlaced(boolean value) { delegate.ignoreSupportWhenPlaced(value); return this; }
        public Builder effect(BlockArray value) { delegate.effect(value); return this; }
        public Builder hitboxType(String value) { delegate.hitboxType(value); return this; }
        public Builder interactionHitboxType(String value) { delegate.interactionHitboxType(value); return this; }
        public Builder flags(BlockFlags value) { delegate.flags(value); return this; }
        public Builder light(BlockLight value) { delegate.light(value); return this; }
        public Builder textures(BlockArray value) { delegate.textures(value); return this; }
        public Builder tintUp(BlockArray value) { delegate.tintUp(value); return this; }
        public Builder tintDown(BlockArray value) { delegate.tintDown(value); return this; }
        public Builder tintNorth(BlockArray value) { delegate.tintNorth(value); return this; }
        public Builder tintSouth(BlockArray value) { delegate.tintSouth(value); return this; }
        public Builder tintWest(BlockArray value) { delegate.tintWest(value); return this; }
        public Builder tintEast(BlockArray value) { delegate.tintEast(value); return this; }
        public Builder customModelTexture(BlockArray value) { delegate.customModelTexture(value); return this; }
        public Builder gathering(BlockGathering value) { delegate.gathering(value); return this; }
        public Builder support(BlockSupport value) { delegate.support(value); return this; }
        public Builder supporting(BlockSupporting value) { delegate.supporting(value); return this; }
        public Builder interactions(BlockInteractions value) { delegate.interactions(value); return this; }
        public Builder state(BlockState value) { delegate.state(value); return this; }
        public Builder placementSettings(BlockPlacementSettings value) { delegate.placementSettings(value); return this; }
        public Builder movementSettings(BlockMovementSettings value) { delegate.movementSettings(value); return this; }
        public Builder bench(BlockBench value) { delegate.bench(value); return this; }
        public Builder farming(BlockFarming value) { delegate.farming(value); return this; }
        public Builder blockEntity(BlockEntity value) { delegate.blockEntity(value); return this; }
        public Builder railConfig(BlockRailConfig value) { delegate.railConfig(value); return this; }
        public Builder conditionalSounds(BlockArray value) { delegate.conditionalSounds(value); return this; }
        public Builder complex(String fieldName, BlockJsonValue value) { delegate.complex(fieldName, value); return this; }
        public Builder recipe(Recipe value) { delegate.recipe(value); return this; }

        public BlockSpecialPropertiesOverride build() {
            return new BlockSpecialPropertiesOverride(delegate.build());
        }
    }
}
