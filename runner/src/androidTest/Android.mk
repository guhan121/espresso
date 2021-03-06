# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

# build unit tests for android-support-test

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, java)

LOCAL_PACKAGE_NAME := AndroidSupportTestTests

LOCAL_MODULE_TAGS := tests

# SDK 10 needed for mockito/objnesis. Otherwise 8 would work
LOCAL_SDK_VERSION := 10

LOCAL_STATIC_JAVA_LIBRARIES := android-support-test-src mockito-target dexmaker hamcrest-library hamcrest-integration

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

