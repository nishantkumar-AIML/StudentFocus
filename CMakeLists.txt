cmake_minimum_required(VERSION 3.14)
project(student_focus VERSION 1.0.0 LANGUAGES CXX)

# Configure compiler flags for strict C++17 compliance
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Include CMake FetchContent module
include(FetchContent)

# Fetch sqlite_orm (v1.9.1) header-only library
FetchContent_Declare(
    sqlite_orm
    GIT_REPOSITORY https://github.com/fnc12/sqlite_orm.git
    GIT_TAG v1.9.1
)
FetchContent_MakeAvailable(sqlite_orm)

# Fetch nlohmann_json (v3.11.3) modern JSON library
FetchContent_Declare(
    nlohmann_json
    GIT_REPOSITORY https://github.com/nlohmann/json.git
    GIT_TAG v3.11.3
)
FetchContent_MakeAvailable(nlohmann_json)

# Fetch GLFW (v3.3.8) for window and input management
set(GLFW_BUILD_DOCS OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
FetchContent_Declare(
    glfw
    GIT_REPOSITORY https://github.com/glfw/glfw.git
    GIT_TAG 3.3.8
)
FetchContent_MakeAvailable(glfw)

# Fetch Dear ImGui (v1.91.4) for graphical interface
FetchContent_Declare(
    imgui
    GIT_REPOSITORY https://github.com/ocornut/imgui.git
    GIT_TAG v1.91.4
)
FetchContent_MakeAvailable(imgui)

# Find system SQLite3 library
find_package(SQLite3 REQUIRED)

# Define static library for Dear ImGui with GLFW and OpenGL3 backends
add_library(imgui_lib STATIC
    ${imgui_SOURCE_DIR}/imgui.cpp
    ${imgui_SOURCE_DIR}/imgui_draw.cpp
    ${imgui_SOURCE_DIR}/imgui_tables.cpp
    ${imgui_SOURCE_DIR}/imgui_widgets.cpp
    ${imgui_SOURCE_DIR}/backends/imgui_impl_glfw.cpp
    ${imgui_SOURCE_DIR}/backends/imgui_impl_opengl3.cpp
)
target_include_directories(imgui_lib PUBLIC
    ${imgui_SOURCE_DIR}
    ${imgui_SOURCE_DIR}/backends
)
target_link_libraries(imgui_lib PUBLIC glfw)

# Source files list
set(SRC_FILES
    src/main.cpp
    src/database/db_manager.cpp
    src/features/gamification.cpp
    src/features/pomodoro.cpp
    src/ai_engine/rag_manager.cpp
    src/ai_engine/ai_manager.cpp
    src/ui/theme.cpp
    src/ui/dashboard.cpp
)

# Define executable target
add_executable(student_focus ${SRC_FILES})

# Include headers path
target_include_directories(student_focus PRIVATE 
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Link targets
target_link_libraries(student_focus PRIVATE
    sqlite_orm::sqlite_orm
    nlohmann_json::nlohmann_json
    SQLite3::SQLite3
    imgui_lib
    glfw
)

# Platform specific linkages
if(APPLE)
    find_library(COCOA_FRAMEWORK Cocoa REQUIRED)
    find_library(IOKIT_FRAMEWORK IOKit REQUIRED)
    find_library(COREVIDEO_FRAMEWORK CoreVideo REQUIRED)
    find_library(OPENGL_FRAMEWORK OpenGL REQUIRED)
    target_link_libraries(student_focus PRIVATE
        ${COCOA_FRAMEWORK}
        ${IOKIT_FRAMEWORK}
        ${COREVIDEO_FRAMEWORK}
        ${OPENGL_FRAMEWORK}
    )
elseif(UNIX)
    find_package(OpenGL REQUIRED)
    target_link_libraries(student_focus PRIVATE OpenGL::GL)
endif()
