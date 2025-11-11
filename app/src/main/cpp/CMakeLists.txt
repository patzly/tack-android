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