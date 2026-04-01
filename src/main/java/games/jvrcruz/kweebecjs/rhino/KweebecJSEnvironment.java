package games.jvrcruz.kweebecjs.rhino;

import games.jvrcruz.kweebecjs.recipe.BenchRequirementSpec;
import games.jvrcruz.kweebecjs.recipe.RecipeFilter;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class KweebecJSEnvironment {
    private final KweebecJSEvents events;
    private final String listenerGroup;

    public KweebecJSEnvironment(KweebecJSEvents events, String listenerGroup) {
        this.events = events;
        this.listenerGroup = listenerGroup;
    }

    public ScriptableObject createScope(Context context) {
        ScriptableObject scope = (ScriptableObject) context.initStandardObjects();
        ScriptableObject.putProperty(scope, "eventbus", createEventBus(scope));
        ScriptableObject.putProperty(scope, "BenchRequirement", createBenchRequirementHelpers(scope));
        ScriptableObject.putProperty(scope, "RecipeFilter", createRecipeFilterHelper(scope));
        ScriptableObject.putProperty(scope, "Item", createItemHelper(scope));
        return scope;
    }

    private Scriptable createEventBus(Scriptable scope) {
        Scriptable eventBus = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(eventBus, "addEventListener", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) {
                    throw new IllegalArgumentException("Usage: eventbus.addEventListener(eventType, callback)");
                }

                String eventType = Context.toString(args[0]);
                if (eventType.isBlank()) {
                    throw new IllegalArgumentException("eventType must not be blank.");
                }
                if (!(args[1] instanceof Function callback)) {
                    throw new IllegalArgumentException("callback must be a function.");
                }

                KweebecJSEventType resolvedEventType = KweebecJSEventType.fromJsEventType(eventType);
                events.addEventListener(resolvedEventType, callback, scope, listenerGroup);
                return Undefined.instance;
            }
        });
        return eventBus;
    }

    private Scriptable createBenchRequirementHelpers(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "typeRequirement", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                if (args.length < 1) {
                    throw new IllegalArgumentException("Usage: BenchRequirement.typeRequirement(benchType)");
                }
                return BenchRequirementSpec.typeRequirement(args[0]);
            }
        });
        ScriptableObject.putProperty(helpers, "idRequirement", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                if (args.length < 1) {
                    throw new IllegalArgumentException("Usage: BenchRequirement.idRequirement(benchId)");
                }
                return BenchRequirementSpec.idRequirement(Context.toString(args[0]));
            }
        });
        ScriptableObject.putProperty(helpers, "category", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                if (args.length < 1) {
                    throw new IllegalArgumentException("Usage: BenchRequirement.category(category)");
                }
                return BenchRequirementSpec.category(Context.toString(args[0]));
            }
        });
        ScriptableObject.putProperty(helpers, "categoryRequirement", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                if (args.length < 1) {
                    throw new IllegalArgumentException("Usage: BenchRequirement.categoryRequirement(category)");
                }
                return BenchRequirementSpec.categoryRequirement(Context.toString(args[0]));
            }
        });
        return helpers;
    }

    private Scriptable createRecipeFilterHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return RecipeFilter.create();
            }
        });
        return helpers;
    }

    private BaseFunction createItemHelper(Scriptable scope) {
        return new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                if (args.length < 1) {
                    throw new IllegalArgumentException("Usage: Item(itemId)");
                }
                return new ScriptItem(Context.toString(args[0]));
            }
        };
    }
}
