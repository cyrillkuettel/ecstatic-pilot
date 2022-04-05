# ecstatic-pilot
## The brain behind a autonomous robot

# Project architecture (Java)

```
.
│  
└───nanodet: object detection.
│   
└───qrcode: Implements CameraX ImageAnalyzer with [zxing](https://github.com/zxing/zxing)  QRCodes
│   
└───socket: Enables communication with the webserver to exchange information.
│   
└───log: dumps the output of the Android logging system at runtime. 
│   
└───shell: Running UNIX commands as root on the Android operating system. (Works on rooted phones.)
│   
└───settings: Android Preferences
```


- simple.bluetooth.terminal This sends commands to stop / start the engine. It essentially acts as a remote-control of a [ESP32](https://en.wikipedia.org/wiki/ESP32) microcontroller.
# This project uses code from the following open-source projects:


-   [SimpleBluetoothTerminal](https://github.com/kai-morich/SimpleBluetoothTerminal) to enable communicating with a ESP32 microcontroller over Bluetooth.
-   [CameraXBasic](https://github.com/android/camera-samples/tree/main/CameraXBasic) Example project which uses the [CameraX](https://developer.android.com/training/camerax) library.
-   [ncnn-android-nanodet](https://github.com/nihui/ncnn-android-nanodet) NanoDet object detection, which depends on [ncnn](https://github.com/Tencent/ncnn). I modified the code to only check for certain objects.
-   [nv-websocket-client](https://github.com/TakahikoKawasaki/nv-websocket-client) ,which offers a High-quality WebSocket client implementation in Java. 
