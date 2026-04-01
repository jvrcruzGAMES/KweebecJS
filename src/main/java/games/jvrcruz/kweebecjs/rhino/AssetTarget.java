package games.jvrcruz.kweebecjs.rhino;

public enum AssetTarget {
    ITEM_MODEL("model", "Items/KweebecJS/Models"),
    ITEM_TEXTURE("texture", "Items/KweebecJS/Textures"),
    ITEM_ICON("texture", "Icons/ItemsGenerated/KweebecJS"),
    BLOCK_MODEL("model", "Blocks/KweebecJS/Models"),
    BLOCK_TEXTURE("texture", "Blocks/KweebecJS/Textures"),
    BLOCK_ITEM_ICON("texture", "Icons/ItemsGenerated/KweebecJS"),
    PARTICLE_TEXTURE("texture", "Particles/KweebecJS/Textures");

    private final String contentType;
    private final String runtimeTargetDirectory;

    AssetTarget(String contentType, String runtimeTargetDirectory) {
        this.contentType = contentType;
        this.runtimeTargetDirectory = runtimeTargetDirectory;
    }

    public String getContentType() {
        return contentType;
    }

    public String getRuntimeTargetDirectory() {
        return runtimeTargetDirectory;
    }
}
