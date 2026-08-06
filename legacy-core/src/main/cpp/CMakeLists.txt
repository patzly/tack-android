cmake_minimum_required(VERSION 3.22.1)
project("oboejava")

find_package(oboe REQUIRED CONFIG)

add_library(oboe-audio-engine SHARED oboe_audio_engine.cpp)

find_library(log-lib log)

target_link_libraries(
        oboe-audio-engine
        oboe::oboe
        ${log-lib}
)

# Make builds reproducible
target_compile_options(oboe-audio-engine PRIVATE
        "-ffile-prefix-map=${CMAKE_SOURCE_DIR}="
        "-ffile-prefix-map=${CMAKE_BINARY_DIR}="
        "-ffile-prefix-map=${ANDROID_TOOLCHAIN_ROOT}="
)
target_link_options(oboe-audio-engine PRIVATE
        "-Wl,--build-id=none"
        "-Wl,--strip-all"
)
set(CMAKE_SORT_OUTPUT_ON ON)
set(CMAKE_TEMP_FILE_PREFIX .tmp)