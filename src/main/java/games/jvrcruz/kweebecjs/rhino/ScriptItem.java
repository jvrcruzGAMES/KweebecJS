package games.jvrcruz.kweebecjs.rhino;

public class ScriptItem {
    public final String ID;
    public final int amount;
    public final boolean explicitAmount;

    public ScriptItem(String id) {
        this(id, 1, false);
    }

    public ScriptItem(String id, int amount) {
        this(id, amount, true);
    }

    private ScriptItem(String id, int amount, boolean explicitAmount) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Item ID must not be blank.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Item amount must be greater than 0.");
        }
        this.ID = id;
        this.amount = amount;
        this.explicitAmount = explicitAmount;
    }

    public String getID() {
        return ID;
    }

    public int getAmount() {
        return amount;
    }

    public boolean hasExplicitAmount() {
        return explicitAmount;
    }
}
