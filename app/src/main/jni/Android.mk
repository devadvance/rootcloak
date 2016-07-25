  LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

  LOCAL_SRC_FILES := rootcloak.c

  LOCAL_CFLAGS := -fPIC -shared

  LOCAL_LDLIBS := -llog

  LOCAL_MODULE := librootcloak

  include $(BUILD_SHARED_LIBRARY)
