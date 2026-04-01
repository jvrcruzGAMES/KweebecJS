package games.jvrcruz.kweebecjs.rhino;

public final class ItemEntry {
    private final String id;
    private final ItemProperties properties;
    private final ItemSpecialProperties specialProperties;

    public ItemEntry(String id, ItemProperties properties) {
        this(id, properties, null);
    }

    public ItemEntry(String id, ItemProperties properties, ItemSpecialProperties specialProperties) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ItemEntry ID must not be blank.");
        }
        if (properties == null) {
            throw new IllegalArgumentException("ItemEntry properties must not be null.");
        }
        this.id = id;
        this.properties = properties;
        this.specialProperties = specialProperties;
    }

    public String getId() {
        return id;
    }

    public ItemProperties getProperties() {
        return properties;
    }

    public ItemSpecialProperties getSpecialProperties() {
        return specialProperties;
    }
}
