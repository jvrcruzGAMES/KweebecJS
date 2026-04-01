package games.jvrcruz.kweebecjs.rhino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlockPropertiesOverride {
    private final Asset model;
    private final Asset texture;
    private final String drawType;
    private final String material;
    private final String opacity;
    private final String blockSoundSetId;
    private final String hitboxType;
    private final Boolean generateItem;
    private final String itemId;
    private final String itemDisplayNameKey;
    private final String itemDisplayDescriptionKey;
    private final Asset itemIcon;
    private final List<String> itemCategories;
    private final String itemPlayerAnimationsId;
    private final String itemSoundSetId;
    private final String itemQuality;
    private final Integer itemLevel;

    private BlockPropertiesOverride(
            Asset model,
            Asset texture,
            String drawType,
            String material,
            String opacity,
            String blockSoundSetId,
            String hitboxType,
            Boolean generateItem,
            String itemId,
            String itemDisplayNameKey,
            String itemDisplayDescriptionKey,
            Asset itemIcon,
            List<String> itemCategories,
            String itemPlayerAnimationsId,
            String itemSoundSetId,
            String itemQuality,
            Integer itemLevel
    ) {
        this.model = model;
        this.texture = texture;
        this.drawType = drawType;
        this.material = material;
        this.opacity = opacity;
        this.blockSoundSetId = blockSoundSetId;
        this.hitboxType = hitboxType;
        this.generateItem = generateItem;
        this.itemId = itemId;
        this.itemDisplayNameKey = itemDisplayNameKey;
        this.itemDisplayDescriptionKey = itemDisplayDescriptionKey;
        this.itemIcon = itemIcon;
        this.itemCategories = itemCategories;
        this.itemPlayerAnimationsId = itemPlayerAnimationsId;
        this.itemSoundSetId = itemSoundSetId;
        this.itemQuality = itemQuality;
        this.itemLevel = itemLevel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Asset getModel() {
        return model;
    }

    public Asset getTexture() {
        return texture;
    }

    public String getDrawType() {
        return drawType;
    }

    public String getMaterial() {
        return material;
    }

    public String getOpacity() {
        return opacity;
    }

    public String getBlockSoundSetId() {
        return blockSoundSetId;
    }

    public String getHitboxType() {
        return hitboxType;
    }

    public Boolean getGenerateItem() {
        return generateItem;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemDisplayNameKey() {
        return itemDisplayNameKey;
    }

    public String getItemDisplayDescriptionKey() {
        return itemDisplayDescriptionKey;
    }

    public Asset getItemIcon() {
        return itemIcon;
    }

    public List<String> getItemCategories() {
        return itemCategories;
    }

    public String getItemPlayerAnimationsId() {
        return itemPlayerAnimationsId;
    }

    public String getItemSoundSetId() {
        return itemSoundSetId;
    }

    public String getItemQuality() {
        return itemQuality;
    }

    public Integer getItemLevel() {
        return itemLevel;
    }

    public static final class Builder {
        private Asset model;
        private Asset texture;
        private String drawType;
        private String material;
        private String opacity;
        private String blockSoundSetId;
        private String hitboxType;
        private Boolean generateItem;
        private String itemId;
        private String itemDisplayNameKey;
        private String itemDisplayDescriptionKey;
        private Asset itemIcon;
        private final List<String> itemCategories = new ArrayList<>();
        private String itemPlayerAnimationsId;
        private String itemSoundSetId;
        private String itemQuality;
        private Integer itemLevel;

        private Builder() {
        }

        public Builder model(Asset value) {
            requireAsset(value, "model", "model", AssetTarget.BLOCK_MODEL);
            this.model = value;
            return this;
        }

        public Builder texture(Asset value) {
            requireAsset(value, "texture", "texture", AssetTarget.BLOCK_TEXTURE);
            this.texture = value;
            return this;
        }

        public Builder drawType(String value) {
            this.drawType = requireNonBlank(value, "drawType");
            return this;
        }

        public Builder material(String value) {
            this.material = requireNonBlank(value, "material");
            return this;
        }

        public Builder opacity(String value) {
            this.opacity = requireNonBlank(value, "opacity");
            return this;
        }

        public Builder blockSoundSetId(String value) {
            this.blockSoundSetId = requireNonBlank(value, "blockSoundSetId");
            return this;
        }

        public Builder hitboxType(String value) {
            this.hitboxType = requireNonBlank(value, "hitboxType");
            return this;
        }

        public Builder generateItem(boolean value) {
            this.generateItem = value;
            return this;
        }

        public Builder itemId(String value) {
            this.itemId = requireNonBlank(value, "itemId");
            return this;
        }

        public Builder itemDisplayNameKey(String value) {
            this.itemDisplayNameKey = requireNonBlank(value, "itemDisplayNameKey");
            return this;
        }

        public Builder itemDisplayDescriptionKey(String value) {
            this.itemDisplayDescriptionKey = requireNonBlank(value, "itemDisplayDescriptionKey");
            return this;
        }

        public Builder itemIcon(Asset value) {
            requireAsset(value, "texture", "itemIcon", AssetTarget.BLOCK_ITEM_ICON);
            this.itemIcon = value;
            return this;
        }

        public Builder itemCategory(String value) {
            this.itemCategories.add(requireNonBlank(value, "itemCategory"));
            return this;
        }

        public Builder itemPlayerAnimationsId(String value) {
            this.itemPlayerAnimationsId = requireNonBlank(value, "itemPlayerAnimationsId");
            return this;
        }

        public Builder itemSoundSetId(String value) {
            this.itemSoundSetId = requireNonBlank(value, "itemSoundSetId");
            return this;
        }

        public Builder itemQuality(String value) {
            this.itemQuality = requireNonBlank(value, "itemQuality");
            return this;
        }

        public Builder itemLevel(int value) {
            if (value <= 0) {
                throw new IllegalArgumentException("itemLevel must be greater than 0.");
            }
            this.itemLevel = value;
            return this;
        }

        public BlockPropertiesOverride build() {
            return new BlockPropertiesOverride(
                    model,
                    texture,
                    drawType,
                    material,
                    opacity,
                    blockSoundSetId,
                    hitboxType,
                    generateItem,
                    itemId,
                    itemDisplayNameKey,
                    itemDisplayDescriptionKey,
                    itemIcon,
                    Collections.unmodifiableList(new ArrayList<>(itemCategories)),
                    itemPlayerAnimationsId,
                    itemSoundSetId,
                    itemQuality,
                    itemLevel
            );
        }

        private static String requireNonBlank(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank.");
            }
            return value;
        }

        private static void requireAsset(Asset asset, String expectedType, String propertyName, AssetTarget expectedTarget) {
            if (asset == null) {
                throw new IllegalArgumentException(propertyName + " asset must not be null.");
            }
            if (!expectedType.equals(asset.getType())) {
                throw new IllegalArgumentException(
                        propertyName + " expects an asset of type '" + expectedType + "', got '" + asset.getType() + "'."
                );
            }
            if (asset.getTarget() != expectedTarget) {
                throw new IllegalArgumentException(
                        propertyName + " expects asset target '" + expectedTarget.name() + "', got '" + asset.getTarget().name() + "'."
                );
            }
        }
    }
}
