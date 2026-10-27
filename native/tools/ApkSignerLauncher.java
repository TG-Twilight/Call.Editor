import com.android.apksigner.ApkSignerTool;

/** Preserves a Unicode key alias when Windows cannot encode it in native arguments. */
public final class ApkSignerLauncher {
    private static final String ALIAS_OPTION = "--ks-key-alias";
    private static final String ALIAS_ENV = "REVIA_APK_KEY_ALIAS";
    private static final String ALIAS_MARKER = "env:" + ALIAS_ENV;

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || !"sign".equals(args[0])) {
            throw new IllegalArgumentException("ApkSignerLauncher only supports sign mode.");
        }

        int aliasIndex = -1;
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith(ALIAS_OPTION + "=")) {
                throw new IllegalArgumentException("Use a separate --ks-key-alias environment marker.");
            }
            if (ALIAS_OPTION.equals(args[i])) {
                if (aliasIndex != -1 || i + 1 >= args.length
                        || !ALIAS_MARKER.equals(args[i + 1])) {
                    throw new IllegalArgumentException(
                            "Expected exactly one --ks-key-alias env:REVIA_APK_KEY_ALIAS.");
                }
                aliasIndex = ++i;
            }
        }
        if (aliasIndex == -1) {
            throw new IllegalArgumentException("Missing --ks-key-alias env:REVIA_APK_KEY_ALIAS.");
        }

        String alias = System.getenv(ALIAS_ENV);
        if (alias == null || alias.isEmpty()) {
            throw new IllegalArgumentException("REVIA_APK_KEY_ALIAS is missing from the environment.");
        }
        String[] forwarded = args.clone();
        forwarded[aliasIndex] = alias;
        ApkSignerTool.main(forwarded);
    }
}
