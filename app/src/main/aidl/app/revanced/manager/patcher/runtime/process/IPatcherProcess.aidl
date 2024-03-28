// IPatcherProcess.aidl
package app.revanced.manager.patcher.runtime.process;

import app.revanced.manager.patcher.runtime.process.Parameters;
import app.revanced.manager.patcher.runtime.process.IPatcherEvents;

interface IPatcherProcess {
    // Returns BuildConfig.BUILD_ID, which is used to ensure the main app and runner process are running the same code.
    long buildId();
    // Makes the patcher process exit with code 0
    oneway void exit();
    // Starts patching.
    oneway void start(in Parameters parameters, IPatcherEvents events);
}