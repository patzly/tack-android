cmake_minimum_required(VERSION 4.0.2)
project("oboejava")

find_package(oboe REQUIRED CONFIG)

add_library(oboe-audio-engine SHARED oboe-audio-engine.cpp)

find_library(log-lib log)

target_link_libraries(
        oboe-audio-engine
        oboe::oboe
        ${log-lib}
)

# Make builds reproducible
target_compile_options(oboe-audio-engine PRIVATE
        "-ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}=."
        "-fno-ident"
)
target_link_options(oboe-audio-engine PRIVATE
        "-Wl,--build-id=none"
        "-Wl,--strip-all"
)
set(CMAKE_SORT_OUTPUT_ON ON)
set(CMAKE_TEMP_FILE_PREFIX .tmp)