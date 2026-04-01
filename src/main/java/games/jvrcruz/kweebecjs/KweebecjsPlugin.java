package games.jvrcruz.kweebecjs;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import games.jvrcruz.kweebecjs.asset.RuntimeItemAssetManager;
import games.jvrcruz.kweebecjs.command.ReloadServerScriptsCommand;
import games.jvrcruz.kweebecjs.recipe.RecipeInjector;
import games.jvrcruz.kweebecjs.rhino.EventFunction;
import games.jvrcruz.kweebecjs.rhino.KweebecJSEnvironment;
import games.jvrcruz.kweebecjs.rhino.KweebecJSEventType;
import games.jvrcruz.kweebecjs.rhino.KweebecJSEvents;
import games.jvrcruz.kweebecjs.rhino.KweebecJSRuntime;
import games.jvrcruz.kweebecjs.system.SystemPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class KweebecjsPlugin extends JavaPlugin {
    private static final String SERVER_LISTENER_GROUP = "server";
    private final KweebecJSRuntime runtime = new KweebecJSRuntime();
    private final Supplier<String> runtimeAssetPackName = () -> getManifest().getGroup() + ":" + getManifest().getName();
    private final RecipeInjector recipeInjector = new RecipeInjector(KweebecjsPlugin.class, runtimeAssetPackName);
    private final RuntimeItemAssetManager runtimeItemAssetManager = new RuntimeItemAssetManager(
            KweebecjsPlugin.class,
            this::getManifest,
            runtimeAssetPackName,
            message -> getLogger().at(Level.INFO).log(message),
            (message, error) -> getLogger().at(Level.WARNING).log(message, error)
    );
    private KweebecJSEvents events;
    private Path serverScriptsDir;

    public KweebecjsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        events = new KweebecJSEvents(message -> getLogger().at(Level.SEVERE).log(message));

        Path kweebecRootDir = SystemPaths.resolveKweebecRootDir(KweebecjsPlugin.class);
        serverScriptsDir = SystemPaths.resolveServerScriptsDir(KweebecjsPlugin.class);

        try {
            Files.createDirectories(kweebecRootDir);
            Files.createDirectories(serverScriptsDir);
            runtimeItemAssetManager.apply();
            getCommandRegistry().registerCommand(new ReloadServerScriptsCommand(this::reloadServerScripts));

            KweebecJSEnvironment serverEnvironment = new KweebecJSEnvironment(events, SERVER_LISTENER_GROUP);

            int serverExecutedScripts = runtime.executeAll(serverScriptsDir, serverEnvironment);
            emitRecipeRegistrationEvent();
            events.emit(KweebecJSEventType.PLUGIN_READY, Map.of(
                    "serverScripts", serverExecutedScripts
            ));
            getLogger().at(Level.INFO).log(
                    "KweebecJS started. Executed "
                            + serverExecutedScripts + " server script(s) from " + serverScriptsDir.toAbsolutePath().normalize()
            );
        } catch (IOException e) {
            getLogger().at(Level.SEVERE).log(
                    "KweebecJS failed initializing or reading script directories under " + kweebecRootDir.toAbsolutePath().normalize(), e
            );
            events.emit(KweebecJSEventType.PLUGIN_ERROR, Map.of("message", "Failed initializing or reading script directories."));
        } catch (RuntimeException e) {
            getLogger().at(Level.SEVERE).log("KweebecJS failed executing JS runtime.", e);
            events.emit(KweebecJSEventType.PLUGIN_ERROR, Map.of("message", "Runtime execution failed."));
        }
    }

    @Override
    protected void shutdown() {
        runtimeItemAssetManager.remove();
        if (events != null) {
            events.emit(KweebecJSEventType.PLUGIN_SHUTDOWN);
        }
        getLogger().at(Level.INFO).log("KweebecJS shut down.");
    }

    private int reloadServerScripts() throws IOException {
        if (events == null) {
            throw new IllegalStateException("KweebecJS events bus is not initialized.");
        }
        if (serverScriptsDir == null) {
            throw new IllegalStateException("KweebecJS server scripts directory is not initialized.");
        }

        Files.createDirectories(serverScriptsDir);
        events.clearListeners(SERVER_LISTENER_GROUP);
        recipeInjector.clearRegisteredRecipes();

        KweebecJSEnvironment serverEnvironment = new KweebecJSEnvironment(events, SERVER_LISTENER_GROUP);
        int reloadedScripts = runtime.executeAll(serverScriptsDir, serverEnvironment);
        emitRecipeRegistrationEvent();
        return reloadedScripts;
    }

    private void emitRecipeRegistrationEvent() {
        events.emit(KweebecJSEventType.RECIPES_REGISTER, Map.of(
                "add", (EventFunction) recipeInjector::addFromArgs,
                "delete", (EventFunction) recipeInjector::deleteFromArgs,
                "override", (EventFunction) recipeInjector::overrideFromArgs
        ));
    }
}
