package games.jvrcruz.kweebecjs.rhino;

import java.util.Arrays;

public enum KweebecJSEventType {
    PLUGIN_START("plugin:start"),
    RECIPES_REGISTER("recipes:register"),
    PLUGIN_READY("plugin:ready"),
    PLUGIN_ERROR("plugin:error"),
    PLUGIN_SHUTDOWN("plugin:shutdown");

    private final String jsEventType;

    KweebecJSEventType(String jsEventType) {
        this.jsEventType = jsEventType;
    }

    public String jsEventType() {
        return jsEventType;
    }

    public static KweebecJSEventType fromJsEventType(String jsEventType) {
        return Arrays.stream(values())
                .filter(eventType -> eventType.jsEventType.equals(jsEventType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown eventType: " + jsEventType));
    }
}
