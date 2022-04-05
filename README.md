# ecstatic-pilot
## The brain behind a autonomous robot

# Project architecture


- 
- qrcode: Implements CameraX ImageAnalyzer with ![https://github.com/zxing/zxing](zxing) to detect QRCodes
- socket: Enables communication with the webserver to exchange information.
- log: dumps the output of the Android logging system at runtime. 
- shell: Running UNIX commands as root on the Android operating system. (Only works on rooted phones.)
- settings: Android Preferences

- simple.bluetooth.terminal This sends commands to stop / start the engine. It essentially acts as a remote-control of a ![https://en.wikipedia.org/wiki/ESP32](ESP32) microcontroller.
# This project uses code from the following open-source projects:


-   ![https://github.com/kai-morich/SimpleBluetoothTerminal](SimpleBluetoothTerminal) to enable communicating with a ESP32 microcontroller over Bluetooth.
-   ![https://github.com/android/camera-samples/tree/main/CameraXBasic](CameraXBasic) Example project which uses the ![https://developer.android.com/training/camerax](CameraX) library.
-   ![https://github.com/nihui/ncnn-android-nanodet](ncnn-android-nanodet) NanoDet object detection, which depends on ![https://github.com/Tencent/ncnn](ncnn). I modified the code to only check for certain objects.
-   ![https://github.com/TakahikoKawasaki/nv-websocket-client](nv-websocket-client) ,which offers a High-quality WebSocket client implementation in Java. 
