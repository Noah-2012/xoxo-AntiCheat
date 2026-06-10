package com.xoxoac.core;

public final class CheckResult {

    private static final CheckResult PASS = new CheckResult(false, "", "");

    private final boolean failed;
    private final String checkName;
    private final String details;

    private CheckResult(boolean failed, String checkName, String details) {
        this.failed = failed;
        this.checkName = checkName;
        this.details = details;
    }

    public static CheckResult pass() {
        return PASS;
    }

    public static CheckResult fail(String checkName, String details) {
        return new CheckResult(true, checkName, details);
    }

    public boolean failed() {
        return failed;
    }

    public String checkName() {
        return checkName;
    }

    public String details() {
        return details;
    }
}
