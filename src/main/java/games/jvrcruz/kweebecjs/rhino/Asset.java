package games.jvrcruz.kweebecjs.rhino;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Asset {
    private static final Set<String> MODEL_EXTENSIONS = Set.of("blockymodel");
    private static final Set<String> TEXTURE_EXTENSIONS = Set.of("png");
    private static final String MODEL_TYPE = "model";
    private static final String TEXTURE_TYPE = "texture";
    private static final Map<String, Asset> LOADED_ASSETS_BY_KEY = new ConcurrentHashMap<>();

    private final String sourceRelativePath;
    private final String packRelativePath;
    private final String type;
    private final AssetTarget target;

    private Asset(String sourceRelativePath, String packRelativePath, String type, AssetTarget target) {
        this.sourceRelativePath = sourceRelativePath;
        this.packRelativePath = packRelativePath;
        this.type = type;
        this.target = target;
    }

    public static Asset load(Path assetsRootDirectory, Path runtimeAssetsDirectory, String relativePath, AssetTarget target) {
        if (assetsRootDirectory == null) {
            throw new IllegalArgumentException("Assets root directory is not configured.");
        }
        if (runtimeAssetsDirectory == null) {
            throw new IllegalArgumentException("Runtime assets directory is not configured.");
        }
        if (target == null) {
            throw new IllegalArgumentException("Asset target must not be null.");
        }
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Asset path must not be blank.");
        }

        Path normalizedRelativePath = normalizeRelativePath(relativePath);
        Path normalizedAssetsRoot = assetsRootDirectory.toAbsolutePath().normalize();
        Path resolvedPath = normalizedAssetsRoot.resolve(normalizedRelativePath).normalize();

        if (!resolvedPath.startsWith(normalizedAssetsRoot)) {
            throw new IllegalArgumentException("Asset path must stay inside assets root: " + relativePath);
        }
        if (!Files.isRegularFile(resolvedPath)) {
            throw new IllegalArgumentException("Asset not found: " + normalizedRelativePath.toString().replace('\\', '/'));
        }

        String fileName = resolvedPath.getFileName().toString();
        int extensionSeparatorIndex = fileName.lastIndexOf('.');
        if (extensionSeparatorIndex <= 0 || extensionSeparatorIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("Asset must have a supported file extension.");
        }

        String extension = fileName.substring(extensionSeparatorIndex + 1).toLowerCase(Locale.ROOT);
        if ("json".equals(extension)) {
            throw new IllegalArgumentException("JSON assets are not supported by Asset.load.");
        }

        String resolvedType;
        if (MODEL_EXTENSIONS.contains(extension)) {
            resolvedType = MODEL_TYPE;
        } else if (TEXTURE_EXTENSIONS.contains(extension)) {
            resolvedType = TEXTURE_TYPE;
        } else {
            throw new IllegalArgumentException("Unsupported asset type for extension: ." + extension);
        }
        if (!target.getContentType().equals(resolvedType)) {
            throw new IllegalArgumentException(
                    "Asset target '" + target.name() + "' expects content type '" + target.getContentType() + "', got '" + resolvedType + "'."
            );
        }

        String normalizedPath = normalizedRelativePath.toString().replace('\\', '/');
        String packRelativePath = target.getRuntimeTargetDirectory() + "/" + normalizedPath;
        String cacheKey = target.name() + ":" + normalizedPath;
        Asset created = new Asset(
                normalizedPath,
                packRelativePath,
                resolvedType,
                target
        );
        Asset existing = LOADED_ASSETS_BY_KEY.putIfAbsent(cacheKey, created);
        Asset asset = existing == null ? created : existing;
        if (existing == null) {
            try {
                asset.copyToRuntimePack(assetsRootDirectory, runtimeAssetsDirectory);
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Failed copying loaded asset to runtime assets.", e);
            }
        }
        return asset;
    }

    private static Path normalizeRelativePath(String relativePath) {
        Path path = Paths.get(relativePath).normalize();
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Asset path must be relative to the assets root.");
        }
        if (path.startsWith("..")) {
            throw new IllegalArgumentException("Asset path must not escape the assets root.");
        }
        if (path.getNameCount() > 0 && "assets".equalsIgnoreCase(path.getName(0).toString())) {
            path = path.getNameCount() == 1 ? Paths.get("") : path.subpath(1, path.getNameCount());
        }
        if (path.toString().isBlank()) {
            throw new IllegalArgumentException("Asset path must include a file name.");
        }
        return path;
    }

    public String getType() {
        return type;
    }

    public AssetTarget getTarget() {
        return target;
    }

    public String copyToRuntimePack(Path assetsRootDirectory, Path runtimeAssetsDir) throws java.io.IOException {
        return copyToRuntimePack(assetsRootDirectory, runtimeAssetsDir, target);
    }

    public String copyToRuntimePack(Path assetsRootDirectory, Path runtimeAssetsDir, AssetTarget targetOverride) throws java.io.IOException {
        if (targetOverride == null) {
            throw new IllegalArgumentException("Asset target override must not be null.");
        }
        if (!targetOverride.getContentType().equals(type)) {
            throw new IllegalArgumentException(
                    "Asset content type '" + type + "' cannot be copied as target '" + targetOverride.name() + "'."
            );
        }

        Path source = assetsRootDirectory.resolve(sourceRelativePath).normalize();
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Asset file no longer exists.");
        }

        String targetPackRelativePath = targetOverride.getRuntimeTargetDirectory() + "/" + sourceRelativePath;
        String runtimeRelativePath = "Common/" + targetPackRelativePath;
        Path destination = runtimeAssetsDir.resolve(runtimeRelativePath).normalize();
        Files.createDirectories(destination.getParent());
        if (!Files.exists(destination)) {
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
        } else {
            long mismatch = Files.mismatch(source, destination);
            if (mismatch != -1L) {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
        return targetPackRelativePath;
    }

    public static void clearLoadedAssets() {
        LOADED_ASSETS_BY_KEY.clear();
    }
}
