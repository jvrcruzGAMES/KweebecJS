package games.jvrcruz.kweebecjs.system;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SystemPaths {
    private SystemPaths() {
    }

    public static Path resolveKweebecRootDir(Class<?> pluginClass) {
        return resolveServerRootFromModsFolder(pluginClass).resolve("KweebecJS");
    }

    public static Path resolveServerScriptsDir(Class<?> pluginClass) {
        return resolveKweebecRootDir(pluginClass).resolve("server_scripts");
    }

    public static Path resolveRuntimeAssetsDir(Class<?> pluginClass) {
        return resolveKweebecRootDir(pluginClass).resolve("runtime_assets");
    }

    public static Path resolveRuntimeAssetPackFile(Class<?> pluginClass) {
        return resolveKweebecRootDir(pluginClass).resolve("runtime_assets.zip");
    }

    public static Path resolveAssetsDir(Class<?> pluginClass) {
        return resolveKweebecRootDir(pluginClass).resolve("assets");
    }

    public static Path resolveServerRootFromModsFolder(Class<?> pluginClass) {
        try {
            Path codeSourcePath = Paths.get(
                    pluginClass.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).toAbsolutePath().normalize();

            Path current = Files.isRegularFile(codeSourcePath) ? codeSourcePath.getParent() : codeSourcePath;
            while (current != null) {
                Path fileName = current.getFileName();
                if (fileName != null && "mods".equalsIgnoreCase(fileName.toString())) {
                    Path parent = current.getParent();
                    if (parent != null) {
                        return parent;
                    }
                }
                current = current.getParent();
            }
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalStateException("Could not resolve plugin code source for server root detection.", e);
        }

        throw new IllegalStateException("Could not resolve Hytale server root from mods folder.");
    }
}
