// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "nanodetplus.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <signal.h>

#define APPNAME "nanodetncnn.cpp"
#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

// Values loaded from Settings in onCreate of MainActivityNanodetNCNN

bool drawFps;
float modifiable_prob_threshold = 0.4f;
float modifiable_nms_threshold = modifiable_prob_threshold + 0.1f;




static int draw_unsupported(cv::Mat &rgb) {
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y),
                                cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat &rgb) {
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f) {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--) {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f) {
            return 0;
        }

        for (int i = 0; i < 10; i++) {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0;
    int x = rgb.cols - label_size.width;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y),
                                cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}


static NanoDetPlus *g_nanodetplus = 0;
static ncnn::Mutex lock;


class MyNdkCamera : public NdkCameraWindow {
public:
    virtual void on_image_render(cv::Mat &rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat &rgb) const {
    // nanodet
    {
        ncnn::MutexLockGuard g(lock);

        if (g_nanodetplus) {
            std::vector<Object> objects;
            // would be interesting to time this. How long does it take for this two functions to execute?
            g_nanodetplus->detect(rgb, objects, modifiable_prob_threshold, modifiable_nms_threshold);

            g_nanodetplus->draw(rgb, objects);
        } else {
            draw_unsupported(rgb);
        }
    }

// Checking the drawFps every time is not optimal, because it checks multiple times per second, which might impact performance.
// better: provide a custom implementation of g_camera. One without draw_fps. Then switch them out according to the preferences.
// I'd like to do that, but it's a lot of effort....
// to write this clean, probably requires a significant investment of time.
// I have an idea: encapsulate the draw_fps in a Class, and provide a different implementation of the class based on preferences.
// That doesn't really make sense for such a simple task.

    if (drawFps) {
        draw_fps(rgb);
    }
}

JNIEnv *env;


static jint JNI_VERSION = JNI_VERSION_1_4;

static MyNdkCamera *g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "nanodetncnnn", "JNI_OnLoad");
    // https://stackoverflow.com/questions/10617735/in-jni-how-do-i-cache-the-class-methodid-and-fieldids-per-ibms-performance-r/13940735
    // Obtain the JNIEnv from the VM and confirm JNI_VERSION
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION) != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, APPNAME,
                            "error seems to indicate that JNI_VERSION) != JNI_OK");
        return JNI_ERR;
    }
    javaVM_global = vm; // important. This variable is critical for success.
    // Is needed to ultimately access JNIEnv which gives access to Java Objects
    // Temporary local reference holder
    jclass tempLocalClassRef;
    jclass tempLocalClassRef2;

    tempLocalClassRef = env->FindClass("li/garteroboter/pren/nanodet/NanodetncnnActivity");
    tempLocalClassRef2 = env->FindClass("simple/bluetooth/terminal/TerminalFragment");


    // STEP 1/3 : Load the class id
    if (tempLocalClassRef == nullptr || env->ExceptionOccurred() || tempLocalClassRef2 == nullptr) {
        env->ExceptionClear();
        __android_log_print(ANDROID_LOG_ERROR, APPNAME, "%s", "There was an error in JNI_OnLoad");
    }
    __android_log_print(ANDROID_LOG_DEBUG, APPNAME, "Assign the ClassId as a Global Reference");
    // STEP 2/3 : Assign the ClassId as a Global Reference
    TerminalFragmentClass = (jclass) env->NewGlobalRef(tempLocalClassRef);
    MainActivityNanodetNCNNClass = (jclass) env->NewGlobalRef(tempLocalClassRef);

    // STEP 3/3 : Delete the no longer needed local reference
    env->DeleteLocalRef(tempLocalClassRef);
    env->DeleteLocalRef(tempLocalClassRef2);

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_nanodetplus;
        g_nanodetplus = 0;
    }

    // Obtain the JNIEnv from the VM
    // NOTE: some re-do the JNI Version check here, but I find that redundant

    vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION);


    env->DeleteGlobalRef(MainActivityNanodetNCNNClass);
    env->DeleteGlobalRef(MainActivityNanodetNCNNObject);
    env->DeleteGlobalRef(TerminalFragmentClass);
    env->DeleteGlobalRef(TerminalFragmentObject);
    // ... repeat for any other global references

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);

JNIEXPORT jboolean JNICALL
Java_li_garteroboter_pren_nanodet_NanoDetNcnn_loadModel(JNIEnv *env, jobject thiz,
                                                        jobject assetManager, jint modelid,
                                                        jint cpugpu) {
    if (modelid < 0 || modelid > 7 || cpugpu < 0 || cpugpu > 1) {
        return JNI_FALSE;
    }

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char *modeltypes[] =
            {
                    "plus-m_416.torchscript.ncnn",
                    "m",
                    "m-416",
                    "g",
                    "ELite0_320",
                    "ELite1_416",
                    "ELite2_512",
                    "RepVGG-A0_416"
            };

    const int target_sizes[] =
            {
                    416,
                    320,
                    416,
                    416,
                    320,
                    416,
                    512,
                    416
            };

    const float mean_vals[][3] =
            {
                    {103.53f, 116.28f, 123.675f},
                    {103.53f, 116.28f, 123.675f},
                    {103.53f, 116.28f, 123.675f},
                    {103.53f, 116.28f, 123.675f},
                    {127.f,   127.f,   127.f},
                    {127.f,   127.f,   127.f},
                    {127.f,   127.f,   127.f},
                    {103.53f, 116.28f, 123.675f}
            };

    const float norm_vals[][3] =
            {
                    {0.017429f, 0.017507f, 0.017125f},
                    {1.f / 57.375f, 1.f / 57.12f, 1.f / 58.395f},
                    {1.f / 57.375f, 1.f / 57.12f, 1.f / 58.395f},
                    {1.f / 57.375f, 1.f / 57.12f, 1.f / 58.395f},
                    {1.f / 128.f,   1.f / 128.f,  1.f / 128.f},
                    {1.f / 128.f,   1.f / 128.f,  1.f / 128.f},
                    {1.f / 128.f,   1.f / 128.f,  1.f / 128.f},
                    {1.f / 57.375f, 1.f / 57.12f, 1.f / 58.395f}
            };

    const char *modeltype = modeltypes[(int) modelid];
    int target_size = target_sizes[(int) modelid];
    bool use_gpu = (int) cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0) {
            // no gpu
            delete g_nanodetplus;
            g_nanodetplus = 0;
        } else {
            if (!g_nanodetplus)
                g_nanodetplus = new NanoDetPlus;
            g_nanodetplus->load(mgr, modeltype, target_size, mean_vals[(int) modelid],
                                norm_vals[(int) modelid], use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL
Java_li_garteroboter_pren_nanodet_NanoDetNcnn_openCamera(JNIEnv *env, jobject thiz, jint facing) {


    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, APPNAME, "openCamera %d", facing);

    g_camera->open((int) facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();

JNIEXPORT jboolean JNICALL
Java_li_garteroboter_pren_nanodet_NanoDetNcnn_closeCamera(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, APPNAME, "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL
Java_li_garteroboter_pren_nanodet_NanoDetNcnn_setOutputWindow(JNIEnv *env, jobject thiz,
                                                              jobject surface) {
    ANativeWindow *win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, APPNAME, "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_li_garteroboter_pren_nanodet_NanoDetNcnn_setObjectReferenceAsGlobal(JNIEnv *env, jobject thiz,
                                                                         jobject fragment_nanodet_object) {
    __android_log_print(ANDROID_LOG_DEBUG, APPNAME, "setObjectReferenceAsGlobal");

    MainActivityNanodetNCNNObject = (jobject) env->NewGlobalRef(fragment_nanodet_object);

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_simple_bluetooth_terminal_TerminalFragment_setObjectReferenceAsGlobal(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jobject terminalFragment) {
    __android_log_print(ANDROID_LOG_DEBUG, APPNAME, "setObjectReferenceAsGlobal");

    TerminalFragmentObject = (jobject) env->NewGlobalRef(terminalFragment);

    return JNI_TRUE;

}


JNIEXPORT jboolean JNICALL
Java_li_garteroboter_pren_nanodet_NanoDetNcnn_injectFPSPreferences(JNIEnv *env, jobject thiz,
                                                                   jboolean show_fps) {
    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "Call over JNI: injectFPSPreferences");

    if (g_camera) {
        drawFps = show_fps;
        return JNI_TRUE;
    } else {
        __android_log_print(ANDROID_LOG_ERROR, APPNAME, " g_camera is null. Cannot inject fps preferences");
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_li_garteroboter_pren_nanodet_NanoDetNcnn_injectProbThresholdSettings(JNIEnv *env,
                                                                          jobject thiz,
                                                                          jfloat prob_threshold_from_settings) {


    modifiable_prob_threshold = (float) prob_threshold_from_settings;

    __android_log_print(ANDROID_LOG_ERROR, APPNAME, "Current modifiable_prob_threshold = %f", modifiable_prob_threshold);

    return JNI_TRUE;

}
}