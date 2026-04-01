package games.jvrcruz.kweebecjs.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class KweebecJSRuntime {
    public int executeAll(
            Path scriptsDirectory,
            KweebecJSEnvironment environment
    ) throws IOException {
        if (!Files.isDirectory(scriptsDirectory)) {
            return 0;
        }

        List<Path> scripts;
        try (Stream<Path> fileStream = Files.walk(scriptsDirectory)) {
            scripts = fileStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".js"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }

        if (scripts.isEmpty()) {
            Asset.clearLoadedAssets();
            return 0;
        }

        Asset.clearLoadedAssets();
        Context context = Context.enter();
        try {
            ScriptableObject scope = environment.createScope(context);
            for (Path scriptPath : scripts) {
                String source = Files.readString(scriptPath, StandardCharsets.UTF_8);
                String sourceName = scriptsDirectory.relativize(scriptPath).toString().replace('\\', '/');
                context.evaluateString(scope, source, sourceName, 1, null);
            }
        } finally {
            Context.exit();
        }

        return scripts.size();
    }
}
