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
add_link_options(-Wl,--strip-comment)
add_link_options(-Wl,--build-id=none)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -ffile-prefix-map=${CMAKE_SOURCE_DIR}=.")
set(CMAKE_SORT_OUTPUT_ON ON)
set(CMAKE_TEMP_FILE_PREFIX .tmp)