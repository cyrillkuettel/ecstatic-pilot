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

package li.garteroboter.pren.nanodet;

import android.content.res.AssetManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Surface;

public class NanoDetNcnn implements Parcelable

{
    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
    public native boolean openCamera(int facing);
    public native boolean closeCamera();
    public native boolean setOutputWindow(Surface surface);
    public native boolean setObjectReferenceAsGlobal(MainActivityNanodetNCNN mainActivityNanodetNCNN);




    public NanoDetNcnn() {
        // empty constructor. ( There wasnt even one )
    }
    protected NanoDetNcnn(Parcel in) {
    }

    public static final Creator<NanoDetNcnn> CREATOR = new Creator<NanoDetNcnn>() {
        @Override
        public NanoDetNcnn createFromParcel(Parcel in) {
            return new NanoDetNcnn(in);
        }

        @Override
        public NanoDetNcnn[] newArray(int size) {
            return new NanoDetNcnn[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    static {
        System.loadLibrary("nanodetncnn");
    }

}
