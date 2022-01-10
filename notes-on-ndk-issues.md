
Very important notes if you're having problems with NDK, Build / gradle whatever. 


Try different NDK versions. 
Update the Gradle plugin. This is actually more tricky than one might thinkg
Copy the gradle from an existing project. for example:
cp /home/demo/ncnn-android-nanodet/gradle/wrapper/gradle-wrapper.properties .
Then restart. Android studio will prompt you to update the gradle plugin. You may have to select the version in File -> Settings -> build, execution and development -> gradle. There, you may have to select the gradle-wrapper.properties. It should recognize it automatically though. 

change the cmake version (I think this finally solved it. For old projects, you have to try different versions. I downgraded to /home/demo/Android/Sdk/cmake/3.10.2.4988404 ) 

path to cmake is in local.properties. Note however, you shall not add the ndk.dir to the local.properties -> depercated

Another tip, I'm not quite sure if this is it, but when you have trouble with ndk versin: File -> project structure -> SDK Location -> click on download Android NDK
