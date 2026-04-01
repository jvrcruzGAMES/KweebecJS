package games.jvrcruz.kweebecjs.asset;

import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import games.jvrcruz.kweebecjs.system.SystemPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RuntimeItemAssetManager {
    private static final String SERVER_ITEM_DIRECTORY_PREFIX = "Server/Item/Items/";
    private static final String SERVER_BLOCK_DIRECTORY_PREFIX = "Server/Item/Block/Blocks/";
    private static final String ITEM_DIRECTORY_PREFIX = "Item/Items/";
    private static final String BLOCK_DIRECTORY_PREFIX = "Item/Block/Blocks/";
    private final Class<?> pluginClass;
    private final Supplier<PluginManifest> manifestSupplier;
    private final Supplier<String> assetPackNameSupplier;
    private final Consumer<String> infoLogger;
    private final BiConsumer<String, Throwable> warningLogger;
    private final Map<String, String> generatedAssets = new LinkedHashMap<>();
    private final Set<String> managedAssetPaths = new LinkedHashSet<>();
    private final Set<String> loadedItemAssetPaths = new LinkedHashSet<>();
    private final Set<String> loadedBlockAssetPaths = new LinkedHashSet<>();
    private final Map<String, String> lastAppliedGeneratedAssets = new LinkedHashMap<>();
    private String lastAppliedPackContentFingerprint;

    public RuntimeItemAssetManager(
            Class<?> pluginClass,
            Supplier<PluginManifest> manifestSupplier,
            Supplier<String> assetPackNameSupplier,
            Consumer<String> infoLogger,
            BiConsumer<String, Throwable> warningLogger
    ) {
        this.pluginClass = pluginClass;
        this.manifestSupplier = manifestSupplier;
        this.assetPackNameSupplier = assetPackNameSupplier;
        this.infoLogger = infoLogger;
        this.warningLogger = warningLogger;
    }

    public synchronized void putGeneratedAsset(String relativePath, String assetJson) {
        generatedAssets.put(normalizeRelativePath(relativePath), assetJson);
    }

    public synchronized void putGeneratedAssets(Map<String, String> assetsByPath) {
        for (Map.Entry<String, String> entry : assetsByPath.entrySet()) {
            putGeneratedAsset(entry.getKey(), entry.getValue());
        }
    }

    public synchronized void removeGeneratedAsset(String relativePath) {
        generatedAssets.remove(normalizeRelativePath(relativePath));
    }

    public synchronized void clearGeneratedAssets() {
        generatedAssets.clear();
    }

    public synchronized Map<String, String> snapshotGeneratedAssets() {
        return Map.copyOf(generatedAssets);
    }

    public synchronized void apply() throws IOException {
        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(pluginClass);
        Path runtimeAssetPackFile = SystemPaths.resolveRuntimeAssetPackFile(pluginClass);
        Files.createDirectories(runtimeAssetsDir);
        Files.deleteIfExists(runtimeAssetsDir.resolve("manifest.json"));

        Map<String, String> currentGeneratedAssets = Map.copyOf(generatedAssets);
        List<String> generatedRelativePaths = writeGeneratedAssets(runtimeAssetsDir, currentGeneratedAssets);
        ensureImmutablePackMarker(runtimeAssetsDir);
        RuntimeAssetPackUtils.syncArchive(runtimeAssetsDir, runtimeAssetPackFile);
        String assetPackName = assetPackNameSupplier.get();
        AssetModule assetModule = AssetModule.get();
        String currentPackFingerprint = computePackContentFingerprint(runtimeAssetsDir);
        boolean packMissing = assetModule.getAssetPack(assetPackName) == null;
        boolean packContentChanged = lastAppliedPackContentFingerprint == null
                || !lastAppliedPackContentFingerprint.equals(currentPackFingerprint);
        if (packMissing) {
            assetModule.registerPack(assetPackName, runtimeAssetPackFile, manifestSupplier.get(), false);
            lastAppliedPackContentFingerprint = currentPackFingerprint;
        } else if (packContentChanged) {
            assetModule.unregisterPack(assetPackName);
            assetModule.registerPack(assetPackName, runtimeAssetPackFile, manifestSupplier.get(), false);
            lastAppliedPackContentFingerprint = currentPackFingerprint;
        }
        List<String> generatedItemRelativePaths = new ArrayList<>(generatedRelativePaths.size());
        List<String> generatedBlockRelativePaths = new ArrayList<>(generatedRelativePaths.size());
        List<String> changedItemRelativePaths = new ArrayList<>(generatedRelativePaths.size());
        List<String> changedBlockRelativePaths = new ArrayList<>(generatedRelativePaths.size());
        for (String relativePath : generatedRelativePaths) {
            String normalizedPath = normalizeRelativePathForStoreMatch(relativePath);
            boolean changed = !currentGeneratedAssets.get(relativePath).equals(lastAppliedGeneratedAssets.get(relativePath));
            if (isItemAssetJsonPath(normalizedPath)) {
                generatedItemRelativePaths.add(relativePath);
                if (changed) {
                    changedItemRelativePaths.add(relativePath);
                }
            }
            if (isBlockAssetJsonPath(normalizedPath)) {
                generatedBlockRelativePaths.add(relativePath);
                if (changed) {
                    changedBlockRelativePaths.add(relativePath);
                }
            }
        }

        List<String> staleItemRelativePaths = stalePaths(loadedItemAssetPaths, generatedItemRelativePaths);
        List<String> staleBlockRelativePaths = stalePaths(loadedBlockAssetPaths, generatedBlockRelativePaths);
        List<Path> itemPathsToRemove = toAbsolutePaths(runtimeAssetsDir, mergePaths(staleItemRelativePaths, changedItemRelativePaths));
        List<Path> blockPathsToRemove = toAbsolutePaths(runtimeAssetsDir, mergePaths(staleBlockRelativePaths, changedBlockRelativePaths));
        List<Path> generatedItemPaths = toAbsolutePaths(runtimeAssetsDir, changedItemRelativePaths);
        List<Path> generatedBlockPaths = toAbsolutePaths(runtimeAssetsDir, changedBlockRelativePaths);

        if (!itemPathsToRemove.isEmpty()) {
            Item.getAssetStore().removeAssetWithPaths(assetPackName, itemPathsToRemove);
        }
        if (!blockPathsToRemove.isEmpty()) {
            BlockType.getAssetStore().removeAssetWithPaths(assetPackName, blockPathsToRemove);
        }

        AssetLoadResult<String, Item> loadResult = Item.getAssetStore().loadAssetsFromPaths(assetPackName, generatedItemPaths);
        if (loadResult.hasFailed()) {
            throw new IOException(
                    "Failed loading runtime item assets. Failed keys: "
                            + loadResult.getFailedToLoadKeys()
                            + ", failed paths: "
                            + loadResult.getFailedToLoadPaths()
            );
        }
        if (!generatedItemPaths.isEmpty() && loadResult.getLoadedAssets().isEmpty()) {
            throw new IOException(
                    "Runtime item assets were generated but none were loaded. Generated paths: " + generatedItemPaths
            );
        }
        AssetLoadResult<String, BlockType> blockLoadResult = BlockType.getAssetStore().loadAssetsFromPaths(assetPackName, generatedBlockPaths);
        if (blockLoadResult.hasFailed()) {
            throw new IOException(
                    "Failed loading runtime block assets. Failed keys: "
                            + blockLoadResult.getFailedToLoadKeys()
                            + ", failed paths: "
                            + blockLoadResult.getFailedToLoadPaths()
            );
        }
        if (!generatedBlockPaths.isEmpty() && blockLoadResult.getLoadedAssets().isEmpty()) {
            throw new IOException(
                    "Runtime block assets were generated but none were loaded. Generated paths: " + generatedBlockPaths
            );
        }
        loadedItemAssetPaths.clear();
        loadedItemAssetPaths.addAll(generatedItemRelativePaths);
        loadedBlockAssetPaths.clear();
        loadedBlockAssetPaths.addAll(generatedBlockRelativePaths);
        lastAppliedGeneratedAssets.clear();
        lastAppliedGeneratedAssets.putAll(currentGeneratedAssets);

        infoLogger.accept(
                "Loaded "
                        + loadResult.getLoadedAssets().size()
                        + " runtime item asset(s) and "
                        + blockLoadResult.getLoadedAssets().size()
                        + " runtime block asset(s) via asset store."
        );
    }

    public void remove() {
        try {
            String assetPackName = assetPackNameSupplier.get();
            BlockType.getAssetStore().removeAssetPack(assetPackName);
            Item.getAssetStore().removeAssetPack(assetPackName);
            loadedItemAssetPaths.clear();
            loadedBlockAssetPaths.clear();
            lastAppliedGeneratedAssets.clear();
            lastAppliedPackContentFingerprint = null;
            AssetModule assetModule = AssetModule.get();
            if (assetModule.getAssetPack(assetPackName) != null) {
                assetModule.unregisterPack(assetPackName);
            }
        } catch (RuntimeException e) {
            warningLogger.accept("Failed removing runtime item asset pack.", e);
        }
    }

    private List<String> writeGeneratedAssets(Path runtimeAssetsDir, Map<String, String> assetsByPath) throws IOException {
        List<String> generatedPaths = new ArrayList<>(assetsByPath.size());

        Set<String> stalePaths = new LinkedHashSet<>(managedAssetPaths);
        stalePaths.removeAll(assetsByPath.keySet());
        for (String stalePath : stalePaths) {
            Files.deleteIfExists(runtimeAssetsDir.resolve(stalePath));
            managedAssetPaths.remove(stalePath);
        }

        for (Map.Entry<String, String> entry : assetsByPath.entrySet()) {
            Path targetPath = runtimeAssetsDir.resolve(entry.getKey());
            Files.createDirectories(targetPath.getParent());
            String content = entry.getValue();
            boolean shouldWrite = true;
            if (Files.exists(targetPath)) {
                String existing = Files.readString(targetPath, StandardCharsets.UTF_8);
                shouldWrite = !existing.equals(content);
            }
            if (shouldWrite) {
                Files.writeString(
                        targetPath,
                        content,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                );
            }
            generatedPaths.add(entry.getKey());
            managedAssetPaths.add(entry.getKey());
        }
        return generatedPaths;
    }

    private String normalizeRelativePath(String relativePath) {
        return relativePath.replace('\\', '/');
    }

    private static String normalizeRelativePathForStoreMatch(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static boolean isItemAssetJsonPath(String path) {
        return path.endsWith(".json")
                && (path.startsWith(SERVER_ITEM_DIRECTORY_PREFIX) || path.startsWith(ITEM_DIRECTORY_PREFIX));
    }

    private static boolean isBlockAssetJsonPath(String path) {
        return path.endsWith(".json")
                && (path.startsWith(SERVER_BLOCK_DIRECTORY_PREFIX) || path.startsWith(BLOCK_DIRECTORY_PREFIX));
    }

    private static List<String> mergePaths(Set<String> existingPaths, List<String> newPaths) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(existingPaths);
        merged.addAll(newPaths);
        return new ArrayList<>(merged);
    }

    private static List<String> mergePaths(List<String> existingPaths, List<String> newPaths) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(existingPaths);
        merged.addAll(newPaths);
        return new ArrayList<>(merged);
    }

    private static List<String> stalePaths(Set<String> previousPaths, List<String> currentPaths) {
        LinkedHashSet<String> stale = new LinkedHashSet<>(previousPaths);
        stale.removeAll(new LinkedHashSet<>(currentPaths));
        return new ArrayList<>(stale);
    }

    private static List<Path> toAbsolutePaths(Path runtimeAssetsDir, List<String> relativePaths) {
        List<Path> result = new ArrayList<>(relativePaths.size());
        for (String relativePath : relativePaths) {
            result.add(runtimeAssetsDir.resolve(relativePath));
        }
        return result;
    }

    private static void ensureImmutablePackMarker(Path runtimeAssetsDir) throws IOException {
        Path immutableMarker = runtimeAssetsDir.resolve("CommonAssetsIndex.hashes");
        Path commonDir = runtimeAssetsDir.resolve("Common");
        List<String> hashLines = new ArrayList<>();
        if (Files.isDirectory(commonDir)) {
            try (var paths = Files.walk(commonDir)) {
                List<Path> files = paths
                        .filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(path -> commonDir.relativize(path).toString().replace('\\', '/')))
                        .toList();
                for (Path file : files) {
                    String relativeCommonPath = commonDir.relativize(file).toString().replace('\\', '/');
                    hashLines.add(sha256Hex(file) + " " + relativeCommonPath);
                }
            }
        }
        String content = hashLines.isEmpty()
                ? ""
                : String.join("\n", hashLines) + "\n";
        Files.writeString(
                immutableMarker,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private static String sha256Hex(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 algorithm support.", e);
        }
        try (var input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String computePackContentFingerprint(Path runtimeAssetsDir) throws IOException {
        List<Path> roots = List.of(
                runtimeAssetsDir.resolve("Common"),
                runtimeAssetsDir.resolve("Server").resolve("Languages")
        );
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 algorithm support.", e);
        }
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var paths = Files.walk(root)) {
                List<Path> files = paths
                        .filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(path -> runtimeAssetsDir.relativize(path).toString().replace('\\', '/')))
                        .toList();
                for (Path file : files) {
                    String relative = runtimeAssetsDir.relativize(file).toString().replace('\\', '/');
                    digest.update(relative.getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) '\n');
                    digest.update(sha256Hex(file).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) '\n');
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

}
