cmake_minimum_required(VERSION 3.22.1)

project("nativelib")

# 定义 OpenCV 的路径
set(OpenCV_DIR  /Users/sqsong/Public/OpenCV/OpenCV-android-sdk/sdk/native/jni)
set(OpenCV_LIBS ${CMAKE_SOURCE_DIR}/opencv/libs/${ANDROID_ABI}/libopencv_java4.so)
# 查找 OpenCV 包
find_package(OpenCV REQUIRED)
# 引入 OpenCV 的头文件
include_directories(${CMAKE_SOURCE_DIR}/opencv/jni/include)

add_library(${CMAKE_PROJECT_NAME} SHARED
        nativelib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        -ljnigraphics
        ${OpenCV_LIBS}
        log)