package games.jvrcruz.kweebecjs;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import games.jvrcruz.kweebecjs.asset.RuntimeItemAssetManager;
import games.jvrcruz.kweebecjs.block.RuntimeScriptBlockRegistry;
import games.jvrcruz.kweebecjs.command.ReloadServerScriptsCommand;
import games.jvrcruz.kweebecjs.item.RuntimeScriptItemRegistry;
import games.jvrcruz.kweebecjs.lang.RuntimeLangRegistry;
import games.jvrcruz.kweebecjs.recipe.RuntimeRecipeRegistry;
import games.jvrcruz.kweebecjs.rhino.Asset;
import games.jvrcruz.kweebecjs.rhino.EventFunction;
import games.jvrcruz.kweebecjs.rhino.KweebecJSEnvironment;
import games.jvrcruz.kweebecjs.rhino.KweebecJSEventType;
import games.jvrcruz.kweebecjs.rhino.KweebecJSEvents;
import games.jvrcruz.kweebecjs.rhino.KweebecJSRuntime;
import games.jvrcruz.kweebecjs.system.SystemPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class KweebecjsPlugin extends JavaPlugin {
    private static final String SERVER_LISTENER_GROUP = "server";
    private final KweebecJSRuntime runtime = new KweebecJSRuntime();
    private final Supplier<String> runtimeAssetPackName = () -> getManifest().getGroup() + ":" + getManifest().getName();
    private final RuntimeRecipeRegistry runtimeRecipeRegistry = new RuntimeRecipeRegistry(KweebecjsPlugin.class, runtimeAssetPackName);
    private final RuntimeItemAssetManager runtimeItemAssetManager = new RuntimeItemAssetManager(
            KweebecjsPlugin.class,
            this::getManifest,
            runtimeAssetPackName,
            message -> getLogger().at(Level.INFO).log(message),
            (message, error) -> getLogger().at(Level.WARNING).log(message, error)
    );
    private final RuntimeScriptItemRegistry runtimeScriptItemRegistry = new RuntimeScriptItemRegistry(
            KweebecjsPlugin.class,
            () -> getManifest().getGroup(),
            () -> getManifest().getName(),
            runtimeItemAssetManager,
            runtimeRecipeRegistry
    );
    private final RuntimeScriptBlockRegistry runtimeScriptBlockRegistry = new RuntimeScriptBlockRegistry(
            KweebecjsPlugin.class,
            () -> getManifest().getGroup(),
            () -> getManifest().getName(),
            runtimeItemAssetManager,
            runtimeRecipeRegistry
    );
    private final RuntimeLangRegistry runtimeLangRegistry = new RuntimeLangRegistry(
            runtimeItemAssetManager,
            () -> getManifest().getGroup(),
            () -> getManifest().getName()
    );
    private KweebecJSEvents events;
    private Path serverScriptsDir;
    private Path assetsDir;

    public KweebecjsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        events = new KweebecJSEvents(message -> getLogger().at(Level.SEVERE).log(message));

        Path kweebecRootDir = SystemPaths.resolveKweebecRootDir(KweebecjsPlugin.class);
        serverScriptsDir = SystemPaths.resolveServerScriptsDir(KweebecjsPlugin.class);
        assetsDir = SystemPaths.resolveAssetsDir(KweebecjsPlugin.class);
        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(KweebecjsPlugin.class);

        try {
            Files.createDirectories(kweebecRootDir);
            Files.createDirectories(serverScriptsDir);
            Files.createDirectories(assetsDir);
            runtimeRecipeRegistry.clearRegisteredRecipes();
            runtimeLangRegistry.clearRegisteredEntries();
            runtimeScriptBlockRegistry.clearRegisteredBlocks();
            runtimeScriptItemRegistry.clearRegisteredItems();
            runtimeItemAssetManager.clearGeneratedAssets();
            runtimeItemAssetManager.remove();
            clearRuntimeAssetsStorage();
            getCommandRegistry().registerCommand(new ReloadServerScriptsCommand(this::reloadServerScripts));

            KweebecJSEnvironment serverEnvironment = new KweebecJSEnvironment(events, SERVER_LISTENER_GROUP, assetsDir, runtimeAssetsDir);

            int serverExecutedScripts = runtime.executeAll(serverScriptsDir, serverEnvironment);
            int registeredLangEntries = emitLangRegistrationEvent();
            int registeredBlocks = emitBlockRegistrationEvent();
            int registeredItems = emitItemRegistrationEvent();
            runtimeItemAssetManager.apply();
            int registeredRecipes = emitRecipeRegistrationEvent();
            getLogger().at(Level.INFO).log(
                    "KweebecJS started. Executed "
                            + serverExecutedScripts + " server script(s), registered "
                            + registeredLangEntries + " JS lang entry(ies), registered "
                            + registeredBlocks + " JS block(s), registered "
                            + registeredItems + " JS item(s), registered "
                            + registeredRecipes + " JS recipe patch(es), from " + serverScriptsDir.toAbsolutePath().normalize()
            );
        } catch (IOException e) {
            getLogger().at(Level.SEVERE).log(
                    "KweebecJS failed initializing or reading script directories under " + kweebecRootDir.toAbsolutePath().normalize(), e
            );
        } catch (RuntimeException e) {
            getLogger().at(Level.SEVERE).log("KweebecJS failed executing JS runtime.", e);
        }
    }

    @Override
    protected void shutdown() {
        runtimeItemAssetManager.remove();
        getLogger().at(Level.INFO).log("KweebecJS shut down.");
    }

    private int reloadServerScripts() throws IOException {
        if (events == null) {
            throw new IllegalStateException("KweebecJS events bus is not initialized.");
        }
        if (serverScriptsDir == null) {
            throw new IllegalStateException("KweebecJS server scripts directory is not initialized.");
        }
        if (assetsDir == null) {
            throw new IllegalStateException("KweebecJS assets directory is not initialized.");
        }

        Files.createDirectories(serverScriptsDir);
        Files.createDirectories(assetsDir);
        events.clearListeners(SERVER_LISTENER_GROUP);
        runtimeRecipeRegistry.clearRegisteredRecipes();
        runtimeLangRegistry.clearRegisteredEntries();
        runtimeScriptBlockRegistry.clearRegisteredBlocks();
        runtimeScriptItemRegistry.clearRegisteredItems();
        runtimeItemAssetManager.clearGeneratedAssets();
        runtimeItemAssetManager.remove();
        clearRuntimeAssetsStorage();
        Asset.clearLoadedAssets();

        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(KweebecjsPlugin.class);
        KweebecJSEnvironment serverEnvironment = new KweebecJSEnvironment(events, SERVER_LISTENER_GROUP, assetsDir, runtimeAssetsDir);
        int reloadedScripts = runtime.executeAll(serverScriptsDir, serverEnvironment);
        emitLangRegistrationEvent();
        emitBlockRegistrationEvent();
        emitItemRegistrationEvent();
        runtimeItemAssetManager.apply();
        emitRecipeRegistrationEvent();
        return reloadedScripts;
    }

    private int emitLangRegistrationEvent() {
        runtimeLangRegistry.clearRegisteredEntries();
        events.emit(KweebecJSEventType.LANG_REGISTER, Map.of(
                "add", (EventFunction) runtimeLangRegistry::addFromArgs
        ));
        return runtimeLangRegistry.applyRegisteredEntries();
    }

    private void clearRuntimeAssetsStorage() throws IOException {
        Path runtimeAssetsDir = SystemPaths.resolveRuntimeAssetsDir(KweebecjsPlugin.class);
        Path runtimeAssetPackFile = SystemPaths.resolveRuntimeAssetPackFile(KweebecjsPlugin.class);
        Files.deleteIfExists(runtimeAssetPackFile);
        deleteRecursivelyIfPresent(runtimeAssetsDir);
        Files.createDirectories(runtimeAssetsDir);
        Files.deleteIfExists(runtimeAssetPackFile);

        try (var paths = Files.list(runtimeAssetsDir)) {
            if (paths.findAny().isPresent()) {
                throw new IOException("runtime_assets directory is not empty after cleanup: " + runtimeAssetsDir);
            }
        }
    }

    private static void deleteRecursivelyIfPresent(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed clearing directory: " + directory, e);
                }
            });
        }
    }

    private int emitItemRegistrationEvent() throws IOException {
        events.emit(KweebecJSEventType.ITEMS_REGISTER, Map.of(
                "register", (EventFunction) runtimeScriptItemRegistry::registerFromArgs,
                "override", (EventFunction) runtimeScriptItemRegistry::overrideFromArgs
        ));
        return runtimeScriptItemRegistry.applyRegisteredItems();
    }

    private int emitBlockRegistrationEvent() throws IOException {
        events.emit(KweebecJSEventType.BLOCKS_REGISTER, Map.of(
                "register", (EventFunction) runtimeScriptBlockRegistry::registerFromArgs,
                "override", (EventFunction) runtimeScriptBlockRegistry::overrideFromArgs
        ));
        return runtimeScriptBlockRegistry.applyRegisteredBlocks();
    }

    private int emitRecipeRegistrationEvent() {
        events.emit(KweebecJSEventType.RECIPES_REGISTER, Map.of(
                "add", (EventFunction) runtimeRecipeRegistry::addFromArgs,
                "delete", (EventFunction) runtimeRecipeRegistry::deleteFromArgs,
                "override", (EventFunction) runtimeRecipeRegistry::overrideFromArgs
        ));
        return runtimeRecipeRegistry.applyRegisteredRecipes();
    }
}
