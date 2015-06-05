LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := s2d
LOCAL_SRC_FILES := \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/Android.mk \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/Application.mk \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/hcd/hcd_bch.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/tnr/tnr_bonovo.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/tnr/bch_hcd.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/tnr/tnr \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/tnr/tnr_cus.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/tnr/tnr_qcv.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/tnr/tnr_gen.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/tnr/tnr_bch.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/tnr/tnr_ssl.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/jut/jut.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/s2d/s2d.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/bts/bt-ven.c \
	/home/supox/StudioProjects/spirit2_free/app/src/main/jni/bts/bt-hci.c \

LOCAL_C_INCLUDES += /home/supox/StudioProjects/spirit2_free/app/src/main/jni
LOCAL_C_INCLUDES += /home/supox/StudioProjects/spirit2_free/app/src/debug/jni

include $(BUILD_SHARED_LIBRARY)
