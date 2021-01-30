
cmake_minimum_required(VERSION 3.4.1)


set(jnilibs ${CMAKE_SOURCE_DIR}/libs)
set(libname learn_ffmpeg)

include_directories(include)


#link_directories(
#        ${jnilibs}/${ANDROID_ABI})



file(GLOB src-files
        ${CMAKE_SOURCE_DIR}/*.cpp
        ${CMAKE_SOURCE_DIR}/*.c
        ${CMAKE_SOURCE_DIR}/include/*.c
        ${CMAKE_SOURCE_DIR}/ffmpeg_cmd/*.c
        ${CMAKE_SOURCE_DIR}/ffmpeg_cmd/*.cpp
        )

add_library( # Sets the name of the library.
             ${libname}

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${src-files}
        )

#add_library(avutil
#        SHARED
#        IMPORTED)
#set_target_properties(avutil
#        PROPERTIES IMPORTED_LOCATION
#        ${jnilibs}/${ANDROID_ABI}/libavutil.so)
#
#add_library(swresample
#        SHARED
#        IMPORTED)
#set_target_properties(swresample
#        PROPERTIES IMPORTED_LOCATION
#        ${jnilibs}/${ANDROID_ABI}/libswresample.so)
#
#add_library(avcodec
#        SHARED
#        IMPORTED)
#set_target_properties(avcodec
#        PROPERTIES IMPORTED_LOCATION
#        ${jnilibs}/${ANDROID_ABI}/libavcodec.so)
#
#add_library(avfilter
#        SHARED
#        IMPORTED)
#set_target_properties(avfilter
#        PROPERTIES IMPORTED_LOCATION
#        ${jnilibs}/${ANDROID_ABI}/libavfilter.so)
#
#add_library(swscale
#        SHARED
#        IMPORTED)
#set_target_properties(swscale
#        PROPERTIES IMPORTED_LOCATION
#        ${jnilibs}/${ANDROID_ABI}/libswscale.so)
#
#add_library(avformat
#        SHARED
#        IMPORTED)
#set_target_properties(avformat
#        PROPERTIES IMPORTED_LOCATION
#        ${jnilibs}/${ANDROID_ABI}/libavformat.so)
#
#add_library(postproc
#        SHARED
#        IMPORTED)
#set_target_properties(postproc
#        PROPERTIES IMPORTED_LOCATION
#        ${jnilibs}/${ANDROID_ABI}/libpostproc.so)
#add_library(x264
#        SHARED
#        IMPORTED)
#set_target_properties(x264
#        PROPERTIES IMPORTED_LOCATION
#        ${jnilibs}/${ANDROID_ABI}/libx264.so)

add_library(ffmpeg
        SHARED
        IMPORTED)
set_target_properties(ffmpeg
        PROPERTIES IMPORTED_LOCATION
        ${jnilibs}/${ANDROID_ABI}/libffmpeg.so)

#set(third-party-libs
#        avformat
#        avcodec
#        avfilter
#        swresample
#        swscale
#        avutil
#        postproc
#        x264
#        )

set(third-party-libs
        ffmpeg
        )

set(native-libs
        android
        EGL
        GLESv3
        OpenSLES
        log
        m
        z
        )

target_link_libraries( # Specifies the target library.
                       ${libname}
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                       ${third-party-libs}
                       ${native-libs}
                       )



