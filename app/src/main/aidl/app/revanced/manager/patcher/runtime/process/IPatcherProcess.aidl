// IPatcherProcess.aidl
package app.revanced.manager.patcher.runtime.process;

import app.revanced.manager.patcher.runtime.process.Parameters;
import app.revanced.manager.patcher.runtime.process.IPatcherEvents;

interface IPatcherProcess {
    // Makes the patcher process exit with code 0
    oneway void exit();
    // Starts patching.
    oneway void start(in Parameters parameters, IPatcherEvents events);
}