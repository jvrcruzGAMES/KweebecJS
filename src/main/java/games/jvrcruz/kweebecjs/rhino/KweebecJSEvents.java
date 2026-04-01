package games.jvrcruz.kweebecjs.rhino;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class KweebecJSEvents {
    private final Map<KweebecJSEventType, List<Listener>> listenersByEvent = new ConcurrentHashMap<>();
    private final Consumer<String> errorLogger;

    public KweebecJSEvents(Consumer<String> errorLogger) {
        this.errorLogger = errorLogger;
    }

    public void addEventListener(KweebecJSEventType eventType, Function callback, Scriptable scope, String listenerGroup) {
        listenersByEvent
                .computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>())
                .add(new Listener(callback, scope, listenerGroup));
    }

    public void clearListeners(String listenerGroup) {
        for (List<Listener> listeners : listenersByEvent.values()) {
            listeners.removeIf(listener -> listener.listenerGroup().equals(listenerGroup));
        }
    }

    public void emit(KweebecJSEventType eventType) {
        emit(eventType, Map.of());
    }

    public void emit(KweebecJSEventType eventType, Map<String, Object> payload) {
        List<Listener> listeners = listenersByEvent.get(eventType);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        boolean enteredHere = Context.getCurrentContext() == null;
        Context context = enteredHere ? Context.enter() : Context.getCurrentContext();
        try {
            for (Listener listener : listeners) {
                ScriptableObject eventObject = (ScriptableObject) context.newObject(listener.scope());
                ScriptableObject.putProperty(eventObject, "type", eventType.jsEventType());

                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    Object value = toJsValue(listener.scope(), entry.getValue());
                    ScriptableObject.putProperty(eventObject, entry.getKey(), value);
                }

                try {
                    listener.callback().call(context, listener.scope(), listener.scope(), new Object[]{eventObject});
                } catch (RuntimeException callbackError) {
                    errorLogger.accept("Error while dispatching JS event '" + eventType.jsEventType() + "': " + callbackError.getMessage());
                }
            }
        } finally {
            if (enteredHere) {
                Context.exit();
            }
        }
    }

    private record Listener(Function callback, Scriptable scope, String listenerGroup) {
    }

    private static Object toJsValue(Scriptable scope, Object value) {
        if (value instanceof EventFunction function) {
            return new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                    return function.call(args);
                }
            };
        }
        return Context.javaToJS(value, scope);
    }
}
