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

#include "nanodet.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#define APPNAME "nanodetncnn.cpp"
#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

static int draw_unsupported(cv::Mat& rgb)
{
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat& rgb)
{
    // resolve moving average
    float avg_fps = 0.f;
    {
        static double t0 = 0.f;
        static float fps_history[10] = {0.f};

        double t1 = ncnn::get_current_time();
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0);
        t0 = t1;

        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        for (int i = 0; i < 10; i++)
        {
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

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

static NanoDet* g_nanodet = 0;
static ncnn::Mutex lock;

class MyNdkCamera : public NdkCameraWindow
{
public:
    virtual void on_image_render(cv::Mat& rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat& rgb) const
{
    // nanodet
    {
        ncnn::MutexLockGuard g(lock);

        if (g_nanodet)
        {
            std::vector<Object> objects;
            g_nanodet->detect(rgb, objects);

            g_nanodet->draw(rgb, objects);
        }
        else
        {
            draw_unsupported(rgb);
        }
    }

    draw_fps(rgb);
}

// using namespace std; // the orginal answer included this. I had to disable it because it
// caused issues with other variablespu

/**************************************************************
 * Global Variables to cache Java Class and Method IDs
 **************************************************************/

JNIEnv *env;

/**************************************************************
 * Declare JNI_VERSION for use in JNI_On
 * /JNI_OnUnLoad
 * Change value if a Java upgrade requires it
 **************************************************************/
static jint JNI_VERSION = JNI_VERSION_1_4;

static MyNdkCamera* g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "nanodetncnnn", "JNI_OnLoad");
    // https://stackoverflow.com/questions/10617735/in-jni-how-do-i-cache-the-class-methodid-and-fieldids-per-ibms-performance-r/13940735
    // Obtain the JNIEnv from the VM and confirm JNI_VERSION

    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION) != JNI_OK) {
            __android_log_print(ANDROID_LOG_ERROR, APPNAME, "error seems to indicate that JNI_VERSION) != JNI_OK");
        return JNI_ERR;
    }
    javaVM_global = vm; // important. This variable is critical for success.
                        // Is needed to ultimately access JNIEnv which gives access to Java Objects

    // Temporary local reference holder
    jclass tempLocalClassRef;

    tempLocalClassRef = env->FindClass("li/garteroboter/pren/nanodet/FragmentNanodet");
    // STEP 1/3 : Load the class id
    if (tempLocalClassRef == nullptr || env->ExceptionOccurred()) {
        env->ExceptionClear();
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "%s", "There was an error in invoke_class");
    }
    __android_log_print(ANDROID_LOG_DEBUG, APPNAME, "Assign the ClassId as a Global Reference");
    // STEP 2/3 : Assign the ClassId as a Global Reference
    BeanObject = (jclass) env->NewGlobalRef(tempLocalClassRef);

    // STEP 3/3 : Delete the no longer needed local reference
    env->DeleteLocalRef(tempLocalClassRef);

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_nanodet;
        g_nanodet = 0;
    }

    // Obtain the JNIEnv from the VM
    // NOTE: some re-do the JNI Version check here, but I find that redundant

    vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION);

    // Destroy the global references
   //  env->DeleteGlobalRef(BeanObject);

    // ... repeat for any other global references

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);

JNIEXPORT jboolean JNICALL Java_li_garteroboter_pren_nanodet_NanoDetNcnn_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu)
{
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
    {
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char* modeltypes[] =
    {
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
        {127.f, 127.f, 127.f},
        {127.f, 127.f, 127.f},
        {127.f, 127.f, 127.f},
        {103.53f, 116.28f, 123.675f}
    };

    const float norm_vals[][3] =
    {
        {1.f / 57.375f, 1.f / 57.12f, 1.f / 58.395f},
        {1.f / 57.375f, 1.f / 57.12f, 1.f / 58.395f},
        {1.f / 57.375f, 1.f / 57.12f, 1.f / 58.395f},
        {1.f / 128.f, 1.f / 128.f, 1.f / 128.f},
        {1.f / 128.f, 1.f / 128.f, 1.f / 128.f},
        {1.f / 128.f, 1.f / 128.f, 1.f / 128.f},
        {1.f / 57.375f, 1.f / 57.12f, 1.f / 58.395f}
    };

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];
    bool use_gpu = (int)cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0)
        {
            // no gpu
            delete g_nanodet;
            g_nanodet = 0;
        }
        else
        {
            if (!g_nanodet)
                g_nanodet = new NanoDet;
            g_nanodet->load(mgr, modeltype, target_size, mean_vals[(int)modelid], norm_vals[(int)modelid], use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);

JNIEXPORT jboolean JNICALL Java_li_garteroboter_pren_nanodet_NanoDetNcnn_openCamera(JNIEnv* env, jobject thiz, jint facing)
{
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int)facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();

JNIEXPORT jboolean JNICALL Java_li_garteroboter_pren_nanodet_NanoDetNcnn_closeCamera(JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL Java_li_garteroboter_pren_nanodet_NanoDetNcnn_setOutputWindow(JNIEnv* env, jobject thiz, jobject surface)
{
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}

}
