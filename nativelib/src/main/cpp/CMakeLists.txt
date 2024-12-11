cmake_minimum_required(VERSION 3.22.1)

project("nativelib")

# 引入 OpenCV 的头文件
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)

# 定义 OpenCV 库的路径
set(OpenCV_LIBS
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_core.so
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_imgproc.so
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_imgcodecs.so
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_features2d.so
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_photo.so
)

add_library(${CMAKE_PROJECT_NAME} SHARED
        nativelib.cpp LibAlpha.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        -ljnigraphics
        ${OpenCV_LIBS}
        log)