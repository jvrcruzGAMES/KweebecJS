package games.jvrcruz.kweebecjs.rhino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ItemSpecialProperties {
    private final String droppedItemAnimation;
    private final String reticle;
    private final String quality;
    private final Integer itemLevel;
    private final Boolean utilityCompatible;
    private final String interactionPrimary;
    private final String interactionSecondary;
    private final String interactionAbility1;
    private final List<String> typeTags;
    private final List<String> familyTags;
    private final String itemSoundSetId;
    private final Integer maxDurability;
    private final Double durabilityLossOnHit;
    private final Integer signatureEnergyBonus;
    private final List<String> weaponEntityStatsToClear;
    private final Integer damage;
    private final Recipe recipe;

    private ItemSpecialProperties(
            String droppedItemAnimation,
            String reticle,
            String quality,
            Integer itemLevel,
            Boolean utilityCompatible,
            String interactionPrimary,
            String interactionSecondary,
            String interactionAbility1,
            List<String> typeTags,
            List<String> familyTags,
            String itemSoundSetId,
            Integer maxDurability,
            Double durabilityLossOnHit,
            Integer signatureEnergyBonus,
            List<String> weaponEntityStatsToClear,
            Integer damage,
            Recipe recipe
    ) {
        this.droppedItemAnimation = droppedItemAnimation;
        this.reticle = reticle;
        this.quality = quality;
        this.itemLevel = itemLevel;
        this.utilityCompatible = utilityCompatible;
        this.interactionPrimary = interactionPrimary;
        this.interactionSecondary = interactionSecondary;
        this.interactionAbility1 = interactionAbility1;
        this.typeTags = typeTags;
        this.familyTags = familyTags;
        this.itemSoundSetId = itemSoundSetId;
        this.maxDurability = maxDurability;
        this.durabilityLossOnHit = durabilityLossOnHit;
        this.signatureEnergyBonus = signatureEnergyBonus;
        this.weaponEntityStatsToClear = weaponEntityStatsToClear;
        this.damage = damage;
        this.recipe = recipe;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getDroppedItemAnimation() {
        return droppedItemAnimation;
    }

    public String getReticle() {
        return reticle;
    }

    public String getQuality() {
        return quality;
    }

    public Integer getItemLevel() {
        return itemLevel;
    }

    public Boolean getUtilityCompatible() {
        return utilityCompatible;
    }

    public String getInteractionPrimary() {
        return interactionPrimary;
    }

    public String getInteractionSecondary() {
        return interactionSecondary;
    }

    public String getInteractionAbility1() {
        return interactionAbility1;
    }

    public List<String> getTypeTags() {
        return typeTags;
    }

    public List<String> getFamilyTags() {
        return familyTags;
    }

    public String getItemSoundSetId() {
        return itemSoundSetId;
    }

    public Integer getMaxDurability() {
        return maxDurability;
    }

    public Double getDurabilityLossOnHit() {
        return durabilityLossOnHit;
    }

    public Integer getSignatureEnergyBonus() {
        return signatureEnergyBonus;
    }

    public List<String> getWeaponEntityStatsToClear() {
        return weaponEntityStatsToClear;
    }

    public Integer getDamage() {
        return damage;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public static final class Builder {
        private String droppedItemAnimation;
        private String reticle;
        private String quality;
        private Integer itemLevel;
        private Boolean utilityCompatible;
        private String interactionPrimary;
        private String interactionSecondary;
        private String interactionAbility1;
        private final List<String> typeTags = new ArrayList<>();
        private final List<String> familyTags = new ArrayList<>();
        private String itemSoundSetId;
        private Integer maxDurability;
        private Double durabilityLossOnHit;
        private Integer signatureEnergyBonus;
        private final List<String> weaponEntityStatsToClear = new ArrayList<>();
        private Integer damage;
        private Recipe recipe;

        private Builder() {
        }

        public Builder droppedItemAnimation(String value) {
            this.droppedItemAnimation = requireNonBlank(value, "droppedItemAnimation");
            return this;
        }

        public Builder reticle(String value) {
            this.reticle = requireNonBlank(value, "reticle");
            return this;
        }

        public Builder quality(String value) {
            this.quality = requireNonBlank(value, "quality");
            return this;
        }

        public Builder itemLevel(int value) {
            if (value <= 0) {
                throw new IllegalArgumentException("itemLevel must be greater than 0.");
            }
            this.itemLevel = value;
            return this;
        }

        public Builder utilityCompatible(boolean value) {
            this.utilityCompatible = value;
            return this;
        }

        public Builder primaryInteraction(String value) {
            this.interactionPrimary = requireNonBlank(value, "primaryInteraction");
            return this;
        }

        public Builder secondaryInteraction(String value) {
            this.interactionSecondary = requireNonBlank(value, "secondaryInteraction");
            return this;
        }

        public Builder ability1Interaction(String value) {
            this.interactionAbility1 = requireNonBlank(value, "ability1Interaction");
            return this;
        }

        public Builder typeTag(String value) {
            this.typeTags.add(requireNonBlank(value, "typeTag"));
            return this;
        }

        public Builder familyTag(String value) {
            this.familyTags.add(requireNonBlank(value, "familyTag"));
            return this;
        }

        public Builder itemSoundSetId(String value) {
            this.itemSoundSetId = requireNonBlank(value, "itemSoundSetId");
            return this;
        }

        public Builder maxDurability(int value) {
            if (value <= 0) {
                throw new IllegalArgumentException("maxDurability must be greater than 0.");
            }
            this.maxDurability = value;
            return this;
        }

        public Builder durabilityLossOnHit(double value) {
            if (value < 0) {
                throw new IllegalArgumentException("durabilityLossOnHit must be greater than or equal to 0.");
            }
            this.durabilityLossOnHit = value;
            return this;
        }

        public Builder signatureEnergyBonus(int value) {
            this.signatureEnergyBonus = value;
            return this;
        }

        public Builder clearWeaponEntityStat(String value) {
            this.weaponEntityStatsToClear.add(requireNonBlank(value, "clearWeaponEntityStat"));
            return this;
        }

        public Builder damage(int value) {
            if (value <= 0) {
                throw new IllegalArgumentException("damage must be greater than 0.");
            }
            this.damage = value;
            return this;
        }

        public Builder recipe(Recipe value) {
            if (value == null) {
                throw new IllegalArgumentException("recipe must not be null.");
            }
            this.recipe = value;
            return this;
        }

        public ItemSpecialProperties build() {
            return new ItemSpecialProperties(
                    droppedItemAnimation,
                    reticle,
                    quality,
                    itemLevel,
                    utilityCompatible,
                    interactionPrimary,
                    interactionSecondary,
                    interactionAbility1,
                    Collections.unmodifiableList(new ArrayList<>(typeTags)),
                    Collections.unmodifiableList(new ArrayList<>(familyTags)),
                    itemSoundSetId,
                    maxDurability,
                    durabilityLossOnHit,
                    signatureEnergyBonus,
                    Collections.unmodifiableList(new ArrayList<>(weaponEntityStatsToClear)),
                    damage,
                    recipe
            );
        }

        private static String requireNonBlank(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank.");
            }
            return value;
        }
    }
}
