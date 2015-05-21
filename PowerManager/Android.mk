LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#LOCAL_JAVA_LIBRARIES := bouncycastle
#LOCAL_STATIC_JAVA_LIBRARIES := guava

LOCAL_DEX_PREOPT := false 

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SettingsDemo
#LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

LOCAL_DEX_PREOPT := false 

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
