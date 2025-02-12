cmake_minimum_required(VERSION 3.22.1)

project("cryptlib")

# 指定 OpenSSL 的头文件目录（相对于当前 CMakeLists.txt 所在目录）
set(OPENSSL_INCLUDE_DIR "${CMAKE_SOURCE_DIR}/include")
# 指定 OpenSSL 的库目录，根据当前编译的 ABI 自动选择
set(OPENSSL_LIB_DIR "${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}")

# 添加 OpenSSL 的头文件目录
include_directories(${OPENSSL_INCLUDE_DIR})

add_library(${CMAKE_PROJECT_NAME} SHARED
        cryptlib.cpp)

# 导入 OpenSSL 静态库：libcrypto.a
add_library(crypto STATIC IMPORTED)
set_target_properties(crypto PROPERTIES
        IMPORTED_LOCATION "${OPENSSL_LIB_DIR}/libcrypto.a"
)

# 导入 OpenSSL 静态库：libssl.a
add_library(ssl STATIC IMPORTED)
set_target_properties(ssl PROPERTIES
        IMPORTED_LOCATION "${OPENSSL_LIB_DIR}/libssl.a"
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        crypto
        ssl
        android
        log)