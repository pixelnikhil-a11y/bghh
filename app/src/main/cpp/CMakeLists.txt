cmake_minimum_required(VERSION 3.22.1)
project(llama_bridge)

# llama.cpp is NOT vendored in this repo (it's a large upstream C/C++ project
# with its own build). Clone it here before building:
#
#   cd app/src/main/cpp
#   git clone --depth 1 https://github.com/ggml-org/llama.cpp.git
#
# That gives you app/src/main/cpp/llama.cpp/ containing CMakeLists.txt,
# which the add_subdirectory() below picks up. This mirrors what the
# official llama.cpp Android example (examples/llama.android) does.

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)

add_subdirectory(llama.cpp)

add_library(llama_bridge SHARED
    llama_bridge.cpp
)

target_link_libraries(llama_bridge
    llama
    common
    android
    log
)

target_include_directories(llama_bridge PRIVATE
    llama.cpp/include
    llama.cpp/ggml/include
    llama.cpp/common
)
