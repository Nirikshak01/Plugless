# Keep Gson models
-keepclassmembers class app.plugless.model.** { <fields>; }
-keep class app.plugless.model.** { *; }

# Keep ZXing
-keep class com.google.zxing.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
