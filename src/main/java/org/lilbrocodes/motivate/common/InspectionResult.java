package org.lilbrocodes.motivate.common;

public class InspectionResult {
    private final boolean success;
    private final String message;

    public InspectionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean success() {
        return this.success;
    }

    public String message() {
        return this.message;
    }
}
