// IPatcherEvents.aidl
package app.revanced.manager;

// Interface used to send events backs to the main app process.
oneway interface IPatcherEvents {
    void log(String level, String msg);
    void patchSucceeded();
    void progress(String name, String state, String msg);
    // The patching process has ended. The exceptionStackTrace is null if it finished successfully.
    void finished(String exceptionStackTrace);
}