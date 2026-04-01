package games.jvrcruz.kweebecjs.rhino;

public class ScriptItem {
    public final String ID;

    public ScriptItem(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Item ID must not be blank.");
        }
        this.ID = id;
    }

    public String getID() {
        return ID;
    }
}
