package io.github.xinye.webrtcguard;

import java.util.Arrays;

/** Deterministic argv transformation; no platform dependencies. */
final class ChromeArgs {
    private static final String PREFIX = "--" + ChromeGuardHook.SWITCH + "=";
    private ChromeArgs() { }

    static String[] withRestrictedIce(String[] original) {
        if (original != null) {
            for (String arg : original) {
                if (arg != null && arg.startsWith("--" + ChromeGuardHook.SWITCH)) {
                    // Never override a deliberate existing user/policy choice.
                    return original;
                }
            }
        }
        if (original == null || original.length == 0) {
            return new String[] { "_", PREFIX + ChromeGuardHook.VALUE };
        }
        String[] copy = Arrays.copyOf(original, original.length + 1);
        copy[copy.length - 1] = PREFIX + ChromeGuardHook.VALUE;
        return copy;
    }
}
