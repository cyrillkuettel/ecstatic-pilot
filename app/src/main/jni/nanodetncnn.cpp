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

#include "wechat_qrcode.hpp"
#include <string>
#define APPNAME "nanodetncnn.cpp"

#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON

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

static ncnn::Mutex lock;
cv::Ptr<cv::wechat_qrcode::WeChatQRCode> qr_detector;

class MyNdkCamera : public NdkCameraWindow
{
public:
    virtual void on_image_render(cv::Mat& rgb) const;
};

void draw(cv::Mat& rgb, std::vector<std::string>& qr_string, std::vector<cv::Mat>& qr_points)
{
    for(int cnt = 0; cnt < qr_string.size(); cnt++) {
        int x0 = qr_points[cnt].at<float>(0, 0);
        int y0 = qr_points[cnt].at<float>(0, 1);
        int x1 = qr_points[cnt].at<float>(1, 0);
        int y1 = qr_points[cnt].at<float>(2, 1);

        cv::rectangle(rgb, cv::Rect(cv::Point(x0,y0),cv::Point(x1,y1)), cv::Scalar(0,255,0), 2);

        char text[256];
        sprintf(text, "%s", qr_string[cnt].c_str());
        cv::putText(rgb, text, cv::Point(x0, y0-8), cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0,255,0), 1);
    }
}

void MyNdkCamera::on_image_render(cv::Mat& rgb) const
{
    std::vector<std::string> qr_string;
    std::vector<cv::Mat> qr_points;
    {
        ncnn::MutexLockGuard g(lock);

        cv::Mat bgr;
        cv::cvtColor(rgb,bgr,cv::COLOR_RGB2BGR);
        qr_string = qr_detector->detectAndDecode(bgr, qr_points);
        // somehow loop through qr_string and search for reasonable entries.

        //__android_log_print(ANDROID_LOG_DEBUG, "ncnn", "qr_string.size() %u", qr_string.size());
         if (!qr_string.empty() ) {
            __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "Calling Java method now. ");
            NanoDet::invoke_java_method();
         }


        draw(rgb,qr_string,qr_points);
    }

    draw_fps(rgb);
}

JavaVM* javaVM_global;
jclass MainActivityQRCodeNCNNClass; // to access the class. (for calling static methods. Probably I won't
jobject MainActivityQRCodeNCNNObject; // to access the object.
JNIEnv *env;
static jint JNI_VERSION = JNI_VERSION_1_4;
static MyNdkCamera* g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

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

    tempLocalClassRef = env->FindClass("li/garteroboter/pren/qrcodencnn/MainActivityQRCodeNCNN");


    // STEP 1/3 : Load the class id
    if (tempLocalClassRef == nullptr || env->ExceptionOccurred()) {
        env->ExceptionClear();
        __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "%s", "There was an error in invoke_class");
    }
    __android_log_print(ANDROID_LOG_DEBUG, APPNAME, "Assign the ClassId as a Global Reference");
    // STEP 2/3 : Assign the ClassId as a Global Reference
    MainActivityQRCodeNCNNClass = (jclass) env->NewGlobalRef(tempLocalClassRef);

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

        delete qr_detector;
        qr_detector = 0;
    }

    // Obtain the JNIEnv from the VM
    // NOTE: some re-do the JNI Version check here, but I find that redundant

    vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION);

    env->DeleteGlobalRef(MainActivityQRCodeNCNNClass);
    env->DeleteGlobalRef(MainActivityQRCodeNCNNObject);
    // ... repeat for any other global references

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL Java_li_garteroboter_pren_qrcodencnn_NanoDetNcnn_loadModel(JNIEnv* env, jobject thiz, jobject assetManager)
{
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    {
        ncnn::MutexLockGuard g(lock);
        qr_detector = cv::makePtr<cv::wechat_qrcode::WeChatQRCode>(mgr);
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL Java_li_garteroboter_pren_qrcodencnn_NanoDetNcnn_openCamera(JNIEnv* env, jobject thiz, jint facing)
{
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int)facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL Java_li_garteroboter_pren_qrcodencnn_NanoDetNcnn_closeCamera(JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL Java_li_garteroboter_pren_qrcodencnn_NanoDetNcnn_setOutputWindow(JNIEnv* env, jobject thiz, jobject surface)
{
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_li_garteroboter_pren_qrcodencnn_NanoDetNcnn_setObjectReferenceAsGlobal(JNIEnv *env, jobject thiz,
                                                                                                    jobject fragment_nanodet_object) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setObjectReferenceAsGlobal");

    MainActivityQRCodeNCNNObject = (jobject) env->NewGlobalRef(fragment_nanodet_object);

    return JNI_TRUE;
}

// variables to cache
// my intuition say that I should use the same *env variable as in nanodetncnn. But does it really matter? Never change a running system /s
JNIEnv *env2;


jmethodID staticMethod_CallInJava;
jmethodID instanceMethod_CallInJava;
jstring jstrBuf;

void NanoDet::invoke_java_method() {
    if (javaVM_global->GetEnv(reinterpret_cast<void**>(&env2), JNI_VERSION) != JNI_OK) {
        // I'm not 100% sure if this is necessary. Does it impact performance?
        __android_log_print(ANDROID_LOG_ERROR, APPNAME, " JNI_VERSION) != JNI_OK");
        return;
    }
    if (env2 == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, APPNAME, " env2 is nullptr");

    }
    instanceMethod_CallInJava = env2->GetMethodID(MainActivityQRCodeNCNNClass, "nonStaticDurchstich",
                                                  "(Ljava/lang/String;)V"); // JNI type signature
    if (instanceMethod_CallInJava == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, APPNAME, " instanceMethod_CallInJava is NUll");
        return;
    } else {

        jstrBuf = env2->NewStringUTF("test");
        if( !jstrBuf ) {
            __android_log_print(ANDROID_LOG_DEBUG, APPNAME,  "failed to create jstring." );
            return;
        }


       env2->CallVoidMethod(MainActivityQRCodeNCNNObject, instanceMethod_CallInJava, jstrBuf);

    }
}

}
