package games.jvrcruz.kweebecjs.rhino;

public final class ItemSpecialPropertiesOverride {
    private final ItemSpecialProperties value;

    private ItemSpecialPropertiesOverride(ItemSpecialProperties value) {
        this.value = value;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ItemSpecialProperties getValue() {
        return value;
    }

    public static final class Builder {
        private final ItemSpecialProperties.Builder delegate = ItemSpecialProperties.builder();

        private Builder() {
        }

        public Builder droppedItemAnimation(String value) {
            delegate.droppedItemAnimation(value);
            return this;
        }

        public Builder reticle(String value) {
            delegate.reticle(value);
            return this;
        }

        public Builder quality(String value) {
            delegate.quality(value);
            return this;
        }

        public Builder itemLevel(int value) {
            delegate.itemLevel(value);
            return this;
        }

        public Builder utilityCompatible(boolean value) {
            delegate.utilityCompatible(value);
            return this;
        }

        public Builder primaryInteraction(String value) {
            delegate.primaryInteraction(value);
            return this;
        }

        public Builder secondaryInteraction(String value) {
            delegate.secondaryInteraction(value);
            return this;
        }

        public Builder ability1Interaction(String value) {
            delegate.ability1Interaction(value);
            return this;
        }

        public Builder typeTag(String value) {
            delegate.typeTag(value);
            return this;
        }

        public Builder familyTag(String value) {
            delegate.familyTag(value);
            return this;
        }

        public Builder itemSoundSetId(String value) {
            delegate.itemSoundSetId(value);
            return this;
        }

        public Builder maxDurability(int value) {
            delegate.maxDurability(value);
            return this;
        }

        public Builder durabilityLossOnHit(double value) {
            delegate.durabilityLossOnHit(value);
            return this;
        }

        public Builder signatureEnergyBonus(int value) {
            delegate.signatureEnergyBonus(value);
            return this;
        }

        public Builder clearWeaponEntityStat(String value) {
            delegate.clearWeaponEntityStat(value);
            return this;
        }

        public Builder damage(int value) {
            delegate.damage(value);
            return this;
        }

        public Builder recipe(Recipe value) {
            delegate.recipe(value);
            return this;
        }

        public ItemSpecialPropertiesOverride build() {
            return new ItemSpecialPropertiesOverride(delegate.build());
        }
    }
}
