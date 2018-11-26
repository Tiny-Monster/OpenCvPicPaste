LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
OPENCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=off
include ..\..\..\..\native\jni\OpenCV.mk
LOCAL_MODULE    := stitcher
LOCAL_LDLIBS    += -llog
LOCAL_SRC_FILES := com_tinymonster_opencvpicpaste_OpenCVCPP.cpp
include $(BUILD_SHARED_LIBRARY)