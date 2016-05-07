# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Wannes2\AppData\Local\Android\android-studio\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Reflection stuff
-keep class net.steamcrafted.servicetask.service.ServiceTask
-keep class * extends net.steamcrafted.servicetask.service.ServiceTask

-keepclassmembers class * extends net.steamcrafted.servicetask.service.ServiceTask {
    public ** onAsync(***);
}

-keepclassmembers class net.steamcrafted.servicetask.service.ServiceTask {
    public ** onAsync(***);
}

# Gson serialization measures
-keepattributes Signature
-keep class * extends net.steamcrafted.servicetask.model.ServiceTaskPojo { *; }
-keepclassmembers class * extends net.steamcrafted.servicetask.model.ServiceTaskPojo { *; }