package io.battery;

/**
 * Definition of Spring profile names for the application domain.
 */
public abstract class ProfileNames {
    public static final String OFFLINE = "offline";

    public static final String ONLINE = "!offline";

    public static final String NOSHELL = "noshell";

    public static final String SHELL = "!noshell";

    public static final String DEV = "dev";

    private ProfileNames() {
    }
}
