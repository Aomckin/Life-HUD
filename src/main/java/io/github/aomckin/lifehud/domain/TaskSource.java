package io.github.aomckin.lifehud.domain;

/** Origin of a task in persisted task files. */
public enum TaskSource {
    DAILY("daily"),
    SPECIAL("special");

    private final String jsonValue;

    TaskSource(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    public String jsonValue() {
        return jsonValue;
    }

    public static TaskSource fromJson(String value) {
        for (TaskSource source : values()) {
            if (source.jsonValue.equals(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown task source: " + value);
    }
}