# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class org.cssnr.tibs3dprints.ui.home.HomeFragment$WebAppInterface {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

## Logging
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}


## Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.measurement.** { *; }
-dontwarn com.google.android.gms.measurement.**

-keep class com.google.firebase.analytics.FirebaseAnalytics { *; }
-keep class com.google.firebase.messaging.FirebaseMessagingService { *; }
-keep class com.google.firebase.messaging.RemoteMessage { *; }
-keep class org.cssnr.tibs3dprints.MyFirebaseMessagingService { *; }
