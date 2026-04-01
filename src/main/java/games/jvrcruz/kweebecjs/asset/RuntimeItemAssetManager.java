package games.jvrcruz.kweebecjs.asset;

import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import games.jvrcruz.kweebecjs.system.SystemPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RuntimeItemAssetManager {
    private final Class<?> pluginClass;
    private final Supplier<PluginManifest> manifestSupplier;
    private final Supplier<String> assetPackNameSupplier;
    private final Consumer<String> infoLogger;
    private final BiConsumer<String, Throwable> warningLogger;
    private final Map<String, String> generatedAssets = new LinkedHashMap<>();
    private final Set<String> managedAssetPaths = new LinkedHashSet<>();

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

        List<String> generatedRelativePaths = writeGeneratedAssets(runtimeAssetsDir, Map.copyOf(generatedAssets));
        RuntimeAssetPackUtils.rebuildArchive(runtimeAssetsDir, runtimeAssetPackFile);
        String assetPackName = assetPackNameSupplier.get();
        AssetModule assetModule = AssetModule.get();
        if (assetModule.getAssetPack(assetPackName) != null) {
            assetModule.unregisterPack(assetPackName);
        }
        assetModule.registerPack(assetPackName, runtimeAssetPackFile, manifestSupplier.get(), false);
        Item.getAssetStore().removeAssetPack(assetPackName);

        try (var archiveFileSystem = RuntimeAssetPackUtils.openArchive(runtimeAssetPackFile)) {
            List<Path> generatedPaths = new ArrayList<>(generatedRelativePaths.size());
            for (String relativePath : generatedRelativePaths) {
                generatedPaths.add(archiveFileSystem.getPath("/" + relativePath.replace('\\', '/')));
            }

            AssetLoadResult<String, Item> loadResult = Item.getAssetStore().loadAssetsFromPaths(assetPackName, generatedPaths);
            if (loadResult.hasFailed()) {
                throw new IOException(
                        "Failed loading runtime item assets. Failed keys: "
                                + loadResult.getFailedToLoadKeys()
                                + ", failed paths: "
                                + loadResult.getFailedToLoadPaths()
                );
            }

            infoLogger.accept("Loaded " + loadResult.getLoadedAssets().size() + " runtime item asset(s) via asset store.");
        }
    }

    public void remove() {
        try {
            String assetPackName = assetPackNameSupplier.get();
            Item.getAssetStore().removeAssetPack(assetPackName);
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
            Files.writeString(
                    targetPath,
                    entry.getValue(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            generatedPaths.add(entry.getKey());
            managedAssetPaths.add(entry.getKey());
        }
        return generatedPaths;
    }

    private String normalizeRelativePath(String relativePath) {
        return relativePath.replace('\\', '/');
    }
}
