# nctools Android app — ProGuard/R8 rules
-keepattributes Signature,InnerClasses,EnclosingMethod,Annotation

# Moshi
-keepclassmembers class * { @com.squareup.moshi.* <methods>; }
-dontwarn okhttp3.internal.platform.**

# pdfbox-android (heavy reflection)
-dontwarn org.apache.pdfbox.**
-keep class org.apache.pdfbox.** { *; }

# tess-two
-dontwarn com.googlecode.leptonic.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class eu.nctools.app.di.** { *; }

# Google Drive / Retrofit DTOs
-keep class eu.nctools.app.data.** { *; }
-keep class eu.nctools.app.model.** { *; }