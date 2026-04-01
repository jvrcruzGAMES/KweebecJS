package games.jvrcruz.kweebecjs.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public class   ReloadServerScriptsCommand extends CommandBase {
    private final ScriptReloader scriptReloader;

    public ReloadServerScriptsCommand(ScriptReloader scriptReloader) {
        super("kweebecjsreload", "Reloads only KweebecJS server scripts.");
        this.scriptReloader = scriptReloader;
        addAliases("kjsreload");
    }

    @Override
    protected void executeSync(CommandContext context) {
        try {
            int reloaded = scriptReloader.reload();
            context.sendMessage(Message.raw("KweebecJS: reloaded " + reloaded + " server script(s)."));
        } catch (Exception e) {
            context.sendMessage(Message.raw("KweebecJS: failed to reload server scripts: " + e.getMessage()));
        }
    }

    @FunctionalInterface
    public interface ScriptReloader {
        int reload() throws Exception;
    }
}
