lokki-android
=======================

Development
-----------
To develop this application, you will the [Android SDK](http://developer.android.com/sdk/index.html).


Build instructions
------------------

First, replace ApiUrl in ServerAPI.java with a valid URL leading to the server-side component.
Then run the following command in the project root to build the project:

```
$ ./gradlew build
```

### Debug build

To install the debug build on an emulator run the following command in the project root:

```
$ adb install -r App/build/outputs/apk/App-debug.apk
```

To install it on a device, connect a device via USB (make sure USB debugging is enabled on the device) and run the command:

```
$ adb install -rd App/build/outputs/apk/App-debug.apk
```


For further information see the [Android documentation](http://developer.android.com/tools/building/building-cmdline.html).

Note
----

Lokki is available to the open source community under Apache v2 license AS IS.

