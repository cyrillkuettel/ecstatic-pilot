// This file is part of OpenCV project.
// It is subject to the license terms in the LICENSE file found in the top-level directory
// of this distribution and at http://opencv.org/license.html.
//
// Tencent is pleased to support the open source community by making WeChat QRCode available.
// Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
#include "precomp.hpp"
#include "wechat_qrcode.hpp"
#include "decodermgr.hpp"
#include "detector/align.hpp"
#include "detector/ssd_detector.hpp"
#include "opencv2/core.hpp"
#include "opencv2/core/utils/filesystem.hpp"
#include "scale/super_scale.hpp"
#include "zxing/result.hpp"

#define APPNAME "wechat_qrcode.cpp"
#include <sstream> // adding to print candidate_points size_type

JavaVM* javaVM_global;
jclass TerminalFragmentClass; // to access the class. (for calling static methods. Probably I won't
jobject TerminalFragmentObject; // to access the object.

namespace cv {
namespace wechat_qrcode {
class WeChatQRCode::Impl {
public:
    Impl() {}
    ~Impl() {}
    /**
     * @brief detect QR codes from the given image
     *
     * @param img supports grayscale or color (BGR) image.
     * @return vector<Mat> detected QR code bounding boxes.
     */
    std::vector<Mat> detect(const Mat& img);
    /**
     * @brief decode QR codes from detected points
     *
     * @param img supports grayscale or color (BGR) image.
     * @param candidate_points detected points. we name it "candidate points" which means no
     * all the qrcode can be decoded.
     * @param points succussfully decoded qrcode with bounding box points.
     * @return vector<string>
     */
    std::vector<std::string> decode(const Mat& img, std::vector<Mat>& candidate_points,
                                    std::vector<Mat>& points);
    int applyDetector(const Mat& img, std::vector<Mat>& points);
    Mat cropObj(const Mat& img, const Mat& point, Align& aligner);
    std::vector<float> getScaleList(const int width, const int height);
    std::shared_ptr<SSDDetector> detector_;
    std::shared_ptr<SuperScale> super_resolution_model_;
    bool use_nn_detector_, use_nn_sr_;
};

WeChatQRCode::WeChatQRCode(AAssetManager* mgr) {
    p = makePtr<WeChatQRCode::Impl>();

    p->use_nn_detector_ = true;
    p->detector_ = make_shared<SSDDetector>();
    auto ret = p->detector_->init(mgr);
    CV_Assert(ret == 0);

    p->use_nn_sr_ = true;
    p->super_resolution_model_ = make_shared<SuperScale>();
    ret = p->super_resolution_model_->init(mgr);
    CV_Assert(ret == 0);
}

vector<string> WeChatQRCode::detectAndDecode(InputArray img, OutputArrayOfArrays points) {
    CV_Assert(!img.empty());
    CV_CheckDepthEQ(img.depth(), CV_8U, "");

    if (img.cols() <= 20 || img.rows() <= 20) {
        return vector<string>();  // image data is not enough for providing reliable results
    }
    Mat input_img;
    int incn = img.channels();
    CV_Check(incn, incn == 1 || incn == 3 || incn == 4, "");
    if (incn == 3 || incn == 4) {
        cvtColor(img, input_img, COLOR_BGR2GRAY);
    } else {
        input_img = img.getMat();
    }
    auto candidate_points = p->detect(input_img);
    // DETECTION
/*
    __android_log_print(ANDROID_LOG_DEBUG, "wechat_qrcode", "printing candidate_points");
    std::stringstream ss;
    ss << candidate_points.size();
    std::string string1 = ss.str();
    if (string1 == "1") {
        __android_log_print(ANDROID_LOG_DEBUG, "wechat_qrcode candidate_points.size() ", "equals 1");
    }
    const char *cstr = string1.c_str();
    __android_log_print(ANDROID_LOG_DEBUG, "wechat_qrcode candidate_points.size()", "%s", cstr);
*/
    auto res_points = vector<Mat>();
    auto ret = p->decode(input_img, candidate_points, res_points); // DECODE
    // opencv type convert
    vector<Mat> tmp_points;
    if (points.needed()) {
        for (size_t i = 0; i < res_points.size(); i++) {
            Mat tmp_point;
            tmp_points.push_back(tmp_point);
            res_points[i].convertTo(((OutputArray)tmp_points[i]), CV_32FC2);
        }
        points.createSameSize(tmp_points, CV_32FC2);
        points.assign(tmp_points);
    }
    return ret;
}


// This is very messy to say the least.
// It should be possible to cache the instanceMethod_CallInJava no?

    JNIEnv *env2;
    jmethodID staticMethod_CallInJava;
    jmethodID instanceMethod_CallInJava;
    jstring jstrBuf;

    static jint JNI_VERSION = JNI_VERSION_1_4;
    void WeChatQRCode::invoke_java_method() {
        __android_log_print(ANDROID_LOG_DEBUG, APPNAME, " calling invoke_java_method");
        if (javaVM_global->GetEnv(reinterpret_cast<void**>(&env2), JNI_VERSION) != JNI_OK) {
            // I'm not 100% sure if this is necessary. Does it impact performance?
            __android_log_print(ANDROID_LOG_ERROR, APPNAME, " JNI_VERSION) != JNI_OK");
            return;
        }
        if (env2 == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, APPNAME, " env2 is nullptr");
        }
        instanceMethod_CallInJava = env2->GetMethodID(TerminalFragmentClass, "send",
                                                      "(Ljava/lang/String;)V"); // JNI type signature

        __android_log_print(ANDROID_LOG_DEBUG, APPNAME, " created instanceMethod_CallInJava with JNI");

        if (instanceMethod_CallInJava == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, APPNAME, " instanceMethod_CallInJava is NUll");
            return;
        } else {

            jstrBuf = env2->NewStringUTF("stop");
            if( !jstrBuf ) {
                __android_log_print(ANDROID_LOG_DEBUG, APPNAME,  "failed to create jstring." );
                return;
            }


            env2->CallVoidMethod(TerminalFragmentObject, instanceMethod_CallInJava, jstrBuf);

        }
    }


    vector<string> WeChatQRCode::Impl::decode(const Mat& img, vector<Mat>& candidate_points,
                                          vector<Mat>& points) {
    if (candidate_points.size() == 0) {
        return vector<string>();
    }
    // candidate_points.size > 0, this is an indication that there might me a qr code
    // let's test how reliable this works and log the result.
    __android_log_print(ANDROID_LOG_DEBUG, "wechat_qrcode", "qr_points.size() %u", candidate_points.size());

    WeChatQRCode::invoke_java_method();
    // it works, but unfortunately there are false positives.
    // Have to test if this is the case out in nature as well.


    vector<string> decode_results;
    for (auto& point : candidate_points) {
        Mat cropped_img;
        if (use_nn_detector_) {
            Align aligner;
            cropped_img = cropObj(img, point, aligner);
        } else {
            cropped_img = img;
        }
        // scale_list contains different scale ratios
        auto scale_list = getScaleList(cropped_img.cols, cropped_img.rows);
        for (auto cur_scale : scale_list) {
            Mat scaled_img =
                super_resolution_model_->processImageScale(cropped_img, cur_scale, use_nn_sr_);
            string result;
            DecoderMgr decodemgr;
            auto ret = decodemgr.decodeImage(scaled_img, use_nn_detector_, result);

            if (ret == 0) {

                __android_log_print(ANDROID_LOG_DEBUG, "wechat_qrcode", "This I think means we hvae fully detected a qr code. ");

                decode_results.push_back(result);
                points.push_back(point);
                break;
            }
        }
    }

    return decode_results;
}

vector<Mat> WeChatQRCode::Impl::detect(const Mat& img) {
    auto points = vector<Mat>();

    if (use_nn_detector_) {
        // use cnn detector
        auto ret = applyDetector(img, points);
        CV_Assert(ret == 0);
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, "wechat_qrcode|detect", "Warning: variable use_nn_detector_ is false");
        auto width = img.cols, height = img.rows;
        // if there is no detector, use the full image as input
        auto point = Mat(4, 2, CV_32FC1);
        point.at<float>(0, 0) = 0;
        point.at<float>(0, 1) = 0;
        point.at<float>(1, 0) = width - 1;
        point.at<float>(1, 1) = 0;
        point.at<float>(2, 0) = width - 1;
        point.at<float>(2, 1) = height - 1;
        point.at<float>(3, 0) = 0;
        point.at<float>(3, 1) = height - 1;
        points.push_back(point);
    }
    return points;
}

int WeChatQRCode::Impl::applyDetector(const Mat& img, vector<Mat>& points) {
    int img_w = img.cols;
    int img_h = img.rows;

    // hard code input size
    int minInputSize = 400;
    float resizeRatio = sqrt(img_w * img_h * 1.0 / (minInputSize * minInputSize));
    int detect_width = img_w / resizeRatio;
    int detect_height = img_h / resizeRatio;

    points = detector_->forward(img, detect_width, detect_height);

    return 0;
}

Mat WeChatQRCode::Impl::cropObj(const Mat& img, const Mat& point, Align& aligner) {
    // make some padding to boost the qrcode details recall.
    float padding_w = 0.1f, padding_h = 0.1f;
    auto min_padding = 15;
    auto cropped = aligner.crop(img, point, padding_w, padding_h, min_padding);
    return cropped;
}

// empirical rules
vector<float> WeChatQRCode::Impl::getScaleList(const int width, const int height) {
    if (width < 320 || height < 320) return {1.0, 2.0, 0.5};
    if (width < 640 && height < 640) return {1.0, 0.5};
    return {0.5, 1.0};
}




}  // namespace wechat_qrcode
}  // namespace cv