package games.jvrcruz.kweebecjs.rhino;

import games.jvrcruz.kweebecjs.recipe.BenchRequirementSpec;
import games.jvrcruz.kweebecjs.recipe.RecipeFilter;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import java.nio.file.Path;

public class KweebecJSEnvironment {
    private final KweebecJSEvents events;
    private final String listenerGroup;
    private final Path assetsDirectory;
    private final Path runtimeAssetsDirectory;

    public KweebecJSEnvironment(KweebecJSEvents events, String listenerGroup, Path assetsDirectory, Path runtimeAssetsDirectory) {
        this.events = events;
        this.listenerGroup = listenerGroup;
        this.assetsDirectory = assetsDirectory;
        this.runtimeAssetsDirectory = runtimeAssetsDirectory;
    }

    public ScriptableObject createScope(Context context) {
        ScriptableObject scope = (ScriptableObject) context.initStandardObjects();
        ScriptableObject.putProperty(scope, "eventbus", createEventBus(scope));
        ScriptableObject.putProperty(scope, "BenchRequirement", createBenchRequirementHelpers(scope));
        ScriptableObject.putProperty(scope, "Recipe", createRecipeHelper(scope));
        ScriptableObject.putProperty(scope, "RecipeFilter", createRecipeFilterHelper(scope));
        ScriptableObject.putProperty(scope, "Item", createItemHelper(scope));
        ScriptableObject.putProperty(scope, "AssetTarget", createAssetTargetHelper(scope));
        ScriptableObject.putProperty(scope, "Asset", createAssetHelper(scope));
        ScriptableObject.putProperty(scope, "BlockProperties", createBlockPropertiesHelper(scope));
        ScriptableObject.putProperty(scope, "BlockPropertiesOverride", createBlockPropertiesOverrideHelper(scope));
        ScriptableObject.putProperty(scope, "BlockSpecialProperties", createBlockSpecialPropertiesHelper(scope));
        ScriptableObject.putProperty(scope, "BlockSpecialPropertiesOverride", createBlockSpecialPropertiesOverrideHelper(scope));
        ScriptableObject.putProperty(scope, "BlockConnectedBlockRuleSet", createBlockConnectedBlockRuleSetHelper(scope));
        ScriptableObject.putProperty(scope, "BlockSeats", createBlockSeatsHelper(scope));
        ScriptableObject.putProperty(scope, "BlockBeds", createBlockBedsHelper(scope));
        ScriptableObject.putProperty(scope, "BlockFlags", createBlockFlagsHelper(scope));
        ScriptableObject.putProperty(scope, "BlockLight", createBlockLightHelper(scope));
        ScriptableObject.putProperty(scope, "BlockGathering", createBlockGatheringHelper(scope));
        ScriptableObject.putProperty(scope, "BlockSupport", createBlockSupportHelper(scope));
        ScriptableObject.putProperty(scope, "BlockSupporting", createBlockSupportingHelper(scope));
        ScriptableObject.putProperty(scope, "BlockInteractions", createBlockInteractionsHelper(scope));
        ScriptableObject.putProperty(scope, "BlockState", createBlockStateHelper(scope));
        ScriptableObject.putProperty(scope, "BlockPlacementSettings", createBlockPlacementSettingsHelper(scope));
        ScriptableObject.putProperty(scope, "BlockMovementSettings", createBlockMovementSettingsHelper(scope));
        ScriptableObject.putProperty(scope, "BlockBench", createBlockBenchHelper(scope));
        ScriptableObject.putProperty(scope, "BlockFarming", createBlockFarmingHelper(scope));
        ScriptableObject.putProperty(scope, "BlockEntity", createBlockEntityHelper(scope));
        ScriptableObject.putProperty(scope, "BlockRailConfig", createBlockRailConfigHelper(scope));
        ScriptableObject.putProperty(scope, "BlockArray", createBlockArrayHelper(scope));
        ScriptableObject.putProperty(scope, "ItemProperties", createItemPropertiesHelper(scope));
        ScriptableObject.putProperty(scope, "ItemPropertiesOverride", createItemPropertiesOverrideHelper(scope));
        ScriptableObject.putProperty(scope, "ItemSpecialProperties", createItemSpecialPropertiesHelper(scope));
        ScriptableObject.putProperty(scope, "ItemSpecialPropertiesOverride", createItemSpecialPropertiesOverrideHelper(scope));
        ScriptableObject.putProperty(scope, "ItemEntry", createItemEntryHelper(scope));
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

    private Scriptable createRecipeHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return Recipe.create(args);
            }
        });
        return helpers;
    }

    private BaseFunction createItemHelper(Scriptable scope) {
        return new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                if (args.length < 1) {
                    throw new IllegalArgumentException("Usage: Item(itemId, amount?)");
                }
                if (args.length < 2 || args[1] == null || args[1] == Undefined.instance) {
                    return new ScriptItem(Context.toString(args[0]));
                }
                return new ScriptItem(Context.toString(args[0]), ((Number) Context.jsToJava(args[1], Number.class)).intValue());
            }
        };
    }

    private Scriptable createAssetHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "load", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) {
                    throw new IllegalArgumentException("Usage: Asset.load(pathRelativeToAssetsRoot, AssetTarget.X)");
                }
                Object rawTarget = Context.jsToJava(args[1], Object.class);
                if (!(rawTarget instanceof AssetTarget target)) {
                    throw new IllegalArgumentException("Second argument must be an AssetTarget enum value.");
                }
                return Asset.load(assetsDirectory, runtimeAssetsDirectory, Context.toString(args[0]), target);
            }
        });
        return helpers;
    }

    private Scriptable createAssetTargetHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        for (AssetTarget target : AssetTarget.values()) {
            ScriptableObject.putProperty(helpers, target.name(), target);
        }
        return helpers;
    }

    private Scriptable createItemPropertiesHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return ItemProperties.builder();
            }
        });
        return helpers;
    }

    private Scriptable createItemPropertiesOverrideHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return ItemPropertiesOverride.builder();
            }
        });
        return helpers;
    }

    private Scriptable createItemEntryHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) {
                    throw new IllegalArgumentException("Usage: ItemEntry.new(itemId, ItemProperties.new()....build(), ItemSpecialProperties.new()....build()?)");
                }
                Object rawProperties = Context.jsToJava(args[1], Object.class);
                if (!(rawProperties instanceof ItemProperties properties)) {
                    throw new IllegalArgumentException("Second argument must be an ItemProperties instance.");
                }
                if (args.length < 3 || args[2] == null || args[2] == Undefined.instance) {
                    return new ItemEntry(Context.toString(args[0]), properties);
                }
                Object rawSpecialProperties = Context.jsToJava(args[2], Object.class);
                if (!(rawSpecialProperties instanceof ItemSpecialProperties specialProperties)) {
                    throw new IllegalArgumentException("Third argument must be an ItemSpecialProperties instance when provided.");
                }
                return new ItemEntry(Context.toString(args[0]), properties, specialProperties);
            }
        });
        return helpers;
    }

    private Scriptable createItemSpecialPropertiesHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return ItemSpecialProperties.builder();
            }
        });
        return helpers;
    }

    private Scriptable createItemSpecialPropertiesOverrideHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return ItemSpecialPropertiesOverride.builder();
            }
        });
        return helpers;
    }

    private Scriptable createBlockPropertiesHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return BlockProperties.builder();
            }
        });
        return helpers;
    }

    private Scriptable createBlockPropertiesOverrideHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return BlockPropertiesOverride.builder();
            }
        });
        return helpers;
    }

    private Scriptable createBlockSpecialPropertiesHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return BlockSpecialProperties.builder();
            }
        });
        return helpers;
    }

    private Scriptable createBlockSpecialPropertiesOverrideHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return BlockSpecialPropertiesOverride.builder();
            }
        });
        return helpers;
    }

    private Scriptable createBlockConnectedBlockRuleSetHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return BlockConnectedBlockRuleSet.builder();
            }
        });
        return helpers;
    }

    private Scriptable createBlockSeatsHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockSeats.builder()); }
    private Scriptable createBlockBedsHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockBeds.builder()); }
    private Scriptable createBlockFlagsHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockFlags.builder()); }
    private Scriptable createBlockLightHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockLight.builder()); }
    private Scriptable createBlockGatheringHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockGathering.builder()); }
    private Scriptable createBlockSupportHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockSupport.builder()); }
    private Scriptable createBlockSupportingHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockSupporting.builder()); }
    private Scriptable createBlockInteractionsHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockInteractions.builder()); }
    private Scriptable createBlockStateHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockState.builder()); }
    private Scriptable createBlockPlacementSettingsHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockPlacementSettings.builder()); }
    private Scriptable createBlockMovementSettingsHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockMovementSettings.builder()); }
    private Scriptable createBlockBenchHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockBench.builder()); }
    private Scriptable createBlockFarmingHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockFarming.builder()); }
    private Scriptable createBlockEntityHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockEntity.builder()); }
    private Scriptable createBlockRailConfigHelper(Scriptable scope) { return createBuilderHelper(scope, () -> BlockRailConfig.builder()); }

    private Scriptable createBlockArrayHelper(Scriptable scope) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return BlockArray.builder();
            }
        });
        return helpers;
    }

    private Scriptable createBuilderHelper(Scriptable scope, java.util.function.Supplier<Object> builderSupplier) {
        Scriptable helpers = Context.getCurrentContext().newObject(scope);
        ScriptableObject.putProperty(helpers, "new", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable callScope, Scriptable thisObj, Object[] args) {
                return builderSupplier.get();
            }
        });
        return helpers;
    }
}
