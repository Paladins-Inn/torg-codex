package de.paladinsinn.torg.codex.domain.model;

public enum ClearanceLevel {
    ALPHA("alpha", "α"),
    BETA("beta", "β"),
    GAMMA("gamma", "γ"),
    DELTA("delta", "Δ"),
    OMEGA("omega", "Ω");

    private final String fullName;
    private final String symbol;

    ClearanceLevel(String fullName, String symbol) {
        this.fullName = fullName;
        this.symbol = symbol;
    }

    public String getFullName() {
        return fullName;
    }

    public String getSymbol() {
        return symbol;
    }

    public static ClearanceLevel fromDb(String value) {
        if (value == null || value.isBlank()) {
            return ALPHA;
        }

        for (ClearanceLevel clearanceLevel : values()) {
            if (clearanceLevel.symbol.equals(value)
                    || clearanceLevel.name().equalsIgnoreCase(value)
                    || clearanceLevel.fullName.equalsIgnoreCase(value)) {
                return clearanceLevel;
            }
        }

        throw new IllegalArgumentException("Unknown ClearanceLevel value: " + value);
    }
}
