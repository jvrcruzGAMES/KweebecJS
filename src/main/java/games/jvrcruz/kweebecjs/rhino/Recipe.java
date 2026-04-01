package games.jvrcruz.kweebecjs.rhino;

public final class Recipe {
    private final Object[] args;

    private Recipe(Object[] args) {
        this.args = args;
    }

    public static Recipe create(Object[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Recipe.new(...) requires the same arguments as event.add(...).");
        }
        return new Recipe(args.clone());
    }

    public Object[] args() {
        return args.clone();
    }
}
