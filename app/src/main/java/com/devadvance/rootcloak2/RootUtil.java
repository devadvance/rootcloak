package com.devadvance.rootcloak2;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class RootUtil {
    private static Shell.Interactive rootSession;
    private static boolean isSU;

    public RootUtil() {
        openRootShell();
    }

    public void runCommand(String command) {
        if (rootSession == null) {
            openRootShell();
        }

        rootSession.addCommand(command);
    }

    public boolean isSU() {
        return isSU;
    }

    private void openRootShell() {
        if (rootSession != null) {
            return;
        } else {
            Shell.OnCommandResultListener commandResultListener = new Shell.OnCommandResultListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                    isSU = (exitCode == Shell.OnCommandResultListener.SHELL_RUNNING);
                }
            };
            rootSession = new Shell.Builder()
                    .useSU()
                    .setWatchdogTimeout(10)
                    .open(commandResultListener);
        }
    }
}
