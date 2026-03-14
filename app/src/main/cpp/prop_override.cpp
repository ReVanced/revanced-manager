// Library for overriding Android system properties via environment variables.
//
// Usage: LD_PRELOAD=prop_override.so PROP_dalvik.vm.heapsize=123M getprop dalvik.vm.heapsize
// Output: 123M
#include <string>
#include <cstring>
#include <cstdlib>
#include <dlfcn.h>

// Source: https://android.googlesource.com/platform/system/core/+/100b08a848d018eeb1caa5d5e7c7c2aaac65da15/libcutils/include/cutils/properties.h
#define PROP_VALUE_MAX 92
// This is the mangled name of "android::base::GetProperty".
#define GET_PROPERTY_MANGLED_NAME "_ZN7android4base11GetPropertyERKNSt3__112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_"

extern "C" typedef int (*property_get_ptr)(const char *, char *, const char *);
typedef std::string (*GetProperty_ptr)(const std::string &, const std::string &);

char *GetPropOverride(const std::string &key) {
    auto envKey = "PROP_" + key;

    return getenv(envKey.c_str());
}

// See: https://android.googlesource.com/platform/system/core/+/100b08a848d018eeb1caa5d5e7c7c2aaac65da15/libcutils/properties.cpp
extern "C" int property_get(const char *key, char *value, const char *default_value) {
    auto replacement = GetPropOverride(std::string(key));
    if (replacement) {
        int len = strnlen(replacement, PROP_VALUE_MAX);

        strncpy(value, replacement, len);
        return len;
    }

    static property_get_ptr original = NULL;
    if (!original) {
        // Get the address of the original function.
        original = reinterpret_cast<property_get_ptr>(dlsym(RTLD_NEXT, "property_get"));
    }

    return original(key, value, default_value);
}

// Defining android::base::GetProperty ourselves won't work because std::string has a slightly different "path" in the NDK version of the C++ standard library.
// We can get around this by forcing the function to adopt a specific name using the asm keyword.
std::string GetProperty(const std::string &, const std::string &) asm(GET_PROPERTY_MANGLED_NAME);


// See: https://android.googlesource.com/platform/system/libbase/+/1a34bb67c4f3ba0a1ea6f4f20ac9fe117ba4fe64/properties.cpp
// This isn't used for the properties we want to override, but property_get is deprecated so that could change in the future.
std::string GetProperty(const std::string &key, const std::string &default_value) {
    auto replacement = GetPropOverride(key);
    if (replacement) {
        return std::string(replacement);
    }

    static GetProperty_ptr original = NULL;
    if (!original) {
        original = reinterpret_cast<GetProperty_ptr>(dlsym(RTLD_NEXT, GET_PROPERTY_MANGLED_NAME));
    }

    return original(key, default_value);
}
