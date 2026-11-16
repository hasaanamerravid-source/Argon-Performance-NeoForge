package argon;

/**
 * Name of the mod loader, used in the log line and in the F3 debug line.
 *
 * <p>NeoForge is the only loader this build targets, so the name is a constant. It is kept
 * behind a method so the call sites read the same way they do on the other loaders and so a
 * second loader can be added without touching them.
 */
public final class LoaderNames {
    public static final String NAME = "NeoForge";

    private LoaderNames() {
    }

    public static String current() {
        return NAME;
    }
}
