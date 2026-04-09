cmake_minimum_required(VERSION 3.16)
project(valengine LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if(NOT CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Release)
endif()

add_compile_options(-Wall -Wextra -Wpedantic -Werror)

# Core static library
add_library(valengine STATIC
  src/types.cpp
  src/stats.cpp
  src/parser.cpp
  src/schema.cpp
  src/validator.cpp
)
target_include_directories(valengine PUBLIC include)

# JNI shared library (optional — only built if a JDK is found)
find_package(JNI)
if(JNI_FOUND)
  add_library(valengine_jni SHARED jni/valengine_jni.cpp)
  target_include_directories(valengine_jni PRIVATE
    ${JNI_INCLUDE_DIRS} include jni)
  target_link_libraries(valengine_jni PRIVATE valengine)
  set_target_properties(valengine_jni PROPERTIES OUTPUT_NAME valengine)
else()
  message(WARNING "JNI not found; skipping valengine_jni shared library.")
endif()

# Tests via FetchContent (requires network on first configure)
option(VALENGINE_BUILD_TESTS "Build googletest-based tests" ON)
if(VALENGINE_BUILD_TESTS)
  include(FetchContent)
  FetchContent_Declare(googletest
    GIT_REPOSITORY https://github.com/google/googletest.git
    GIT_TAG v1.14.0)
  set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
  FetchContent_MakeAvailable(googletest)

  enable_testing()
  add_executable(valengine_tests
    tests/test_parser.cpp
    tests/test_stats.cpp
    tests/test_validator.cpp
    tests/test_schema.cpp
  )
  target_link_libraries(valengine_tests PRIVATE valengine GTest::gtest_main)
  include(GoogleTest)
  gtest_discover_tests(valengine_tests)
endif()
