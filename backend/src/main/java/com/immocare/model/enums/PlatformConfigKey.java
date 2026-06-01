package com.immocare.model.enums;

public enum PlatformConfigKey {
    APP_NAME("app.name", "ImmoCare"),
    DEFAULT_COUNTRY("default.country", "BE");

    private final String key;
    private final String defaultValue;

    PlatformConfigKey(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Parcourt l'enum pour trouver si la chaîne 'textKey' correspond à une clé
     * connue.
     * 
     * @return la valeur par défaut, ou null si la clé n'est pas répertoriée.
     */
    public static String getDefaultValueOrNull(String textKey) {
        for (PlatformConfigKey config : values()) {
            if (config.getKey().equals(textKey)) {
                return config.getDefaultValue();
            }
        }
        return null;
    }
}
