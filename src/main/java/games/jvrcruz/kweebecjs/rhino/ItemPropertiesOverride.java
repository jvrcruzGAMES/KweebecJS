package games.jvrcruz.kweebecjs.rhino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ItemPropertiesOverride {
    private final String parent;
    private final String displayNameKey;
    private final String displayDescriptionKey;
    private final List<String> categories;
    private final Asset model;
    private final Asset texture;
    private final Asset icon;
    private final String playerAnimationsId;
    private final Boolean dropOnDeath;

    private ItemPropertiesOverride(
            String parent,
            String displayNameKey,
            String displayDescriptionKey,
            List<String> categories,
            Asset model,
            Asset texture,
            Asset icon,
            String playerAnimationsId,
            Boolean dropOnDeath
    ) {
        this.parent = parent;
        this.displayNameKey = displayNameKey;
        this.displayDescriptionKey = displayDescriptionKey;
        this.categories = categories;
        this.model = model;
        this.texture = texture;
        this.icon = icon;
        this.playerAnimationsId = playerAnimationsId;
        this.dropOnDeath = dropOnDeath;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getParent() {
        return parent;
    }

    public String getDisplayNameKey() {
        return displayNameKey;
    }

    public String getDisplayDescriptionKey() {
        return displayDescriptionKey;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Asset getModel() {
        return model;
    }

    public Asset getTexture() {
        return texture;
    }

    public Asset getIcon() {
        return icon;
    }

    public String getPlayerAnimationsId() {
        return playerAnimationsId;
    }

    public Boolean getDropOnDeath() {
        return dropOnDeath;
    }

    public static final class Builder {
        private String parent;
        private String displayNameKey;
        private String displayDescriptionKey;
        private final List<String> categories = new ArrayList<>();
        private Asset model;
        private Asset texture;
        private Asset icon;
        private String playerAnimationsId;
        private Boolean dropOnDeath;

        private Builder() {
        }

        public Builder parent(String value) {
            this.parent = requireNonBlank(value, "parent");
            return this;
        }

        public Builder displayNameKey(String value) {
            this.displayNameKey = requireNonBlank(value, "displayNameKey");
            return this;
        }

        public Builder displayDescriptionKey(String value) {
            this.displayDescriptionKey = requireNonBlank(value, "displayDescriptionKey");
            return this;
        }

        public Builder category(String value) {
            this.categories.add(requireNonBlank(value, "category"));
            return this;
        }

        public Builder model(Asset value) {
            requireAsset(value, "model", "model", AssetTarget.ITEM_MODEL);
            this.model = value;
            return this;
        }

        public Builder texture(Asset value) {
            requireAsset(value, "texture", "texture", AssetTarget.ITEM_TEXTURE);
            this.texture = value;
            return this;
        }

        public Builder icon(Asset value) {
            requireAsset(value, "texture", "icon", AssetTarget.ITEM_ICON);
            this.icon = value;
            return this;
        }

        public Builder playerAnimationsId(String value) {
            this.playerAnimationsId = requireNonBlank(value, "playerAnimationsId");
            return this;
        }

        public Builder dropOnDeath(boolean value) {
            this.dropOnDeath = value;
            return this;
        }

        public ItemPropertiesOverride build() {
            return new ItemPropertiesOverride(
                    parent,
                    displayNameKey,
                    displayDescriptionKey,
                    Collections.unmodifiableList(new ArrayList<>(categories)),
                    model,
                    texture,
                    icon,
                    playerAnimationsId,
                    dropOnDeath
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
