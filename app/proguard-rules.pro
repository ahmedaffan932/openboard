# R8 rules for the release build (minifyEnabled true in app/build.gradle).
#
# Keep these rules narrow. Play Console's "DEX code optimization" check scores how much of the
# app's code is obfuscated, and every -keep here lowers that score. Each rule below exists
# because something looks the code up by name at runtime (native code, reflection or saved
# JSON), so renaming or removing it would break the app without a build error.
# Libraries (Gson, WorkManager, Firebase, ad SDKs, ML Kit...) bring their own rules.

# ---- Crash reports ----
# Keep line numbers so Crashlytics and Play Console can de-obfuscate stack traces with the
# mapping file from each release build (Crashlytics' Gradle plugin uploads it; the AAB carries
# it for Play). File names are replaced with "SourceFile".
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Native keyboard engine (libjni_latinime) ----
# jni_common.cpp registers the native methods with RegisterNatives, by hard-coded class name.
# RegisterNatives fails for the whole class if a single method is missing or renamed, and then
# the library does not load: no dictionaries, no suggestions. So these classes keep their names
# and all their native methods, even the ones Java never calls.
-keep class org.dslul.openboard.inputmethod.keyboard.ProximityInfo { native <methods>; }
-keep class org.dslul.openboard.inputmethod.latin.BinaryDictionary { native <methods>; }
-keep class org.dslul.openboard.inputmethod.latin.DicTraverseSession { native <methods>; }
-keep class org.dslul.openboard.inputmethod.latin.utils.BinaryDictionaryUtils { native <methods>; }
# Named in a native method signature, and BinaryDictionary.cpp reads its fields with GetFieldID.
-keep class org.dslul.openboard.inputmethod.latin.utils.WordInputEventForPersonalization { <fields>; }

# ---- Personal dictionaries ----
# DictionaryFacilitatorImpl finds the static "getDictionary" factory by name (getMethod).
# UserHistoryDictionary also builds its on-device file name from its simple class name, so a
# renamed class would orphan every user's learned words on update.
-keep class org.dslul.openboard.inputmethod.latin.UserBinaryDictionary {
    public static *** getDictionary(...);
}
-keep class org.dslul.openboard.inputmethod.latin.ContactsBinaryDictionary {
    public static *** getDictionary(...);
}
-keep class org.dslul.openboard.inputmethod.latin.personalization.UserHistoryDictionary {
    public static *** getDictionary(...);
}

# ---- Keyboard settings screens ----
# The framework PreferenceActivity creates settings fragments from a class name (prefs XML
# android:fragment, Preference.setFragment) through the public no-arg constructor.
-keepclassmembers public class * extends android.app.Fragment {
    public <init>();
}

# ---- JSON read and written with Gson ----
# Gson maps JSON keys to field names. History and favorites are saved as JSON in
# SharedPreferences, so renaming a field would make every existing user's saved history and
# favorites unreadable after the update. The others are parsed from bundled or Remote Config
# JSON and would silently come back empty.
-keep class org.dslul.openboard.translator.pro.classes.TranslateHistoryClass { <fields>; <init>(...); }
-keep class org.dslul.openboard.translator.pro.classes.WordList { <fields>; <init>(...); }
-keep class org.dslul.openboard.translator.pro.classes.Data { <fields>; <init>(...); }
-keep class org.dslul.openboard.translator.pro.notifications.LockscreenContent { <fields>; <init>(...); }
-keep class com.blongho.country_data.Country { <fields>; <init>(...); }
-keep class com.blongho.country_data.Currency { <fields>; <init>(...); }

# ---- Meta (Facebook) SDK: unsent app events saved on disk ----
# The SDK saves app events it hasn't sent yet (install attribution) with Java serialization,
# which records class names, field names and serialVersionUID. Its own rules don't keep them, so
# without this the next version can't read the file (ClassNotFoundException) and drops those
# events - on the first R8 update, then again on every release as the obfuscated names change.
-keepnames class com.facebook.appevents.** implements java.io.Serializable
-keepclassmembers class com.facebook.appevents.** implements java.io.Serializable {
    static final long serialVersionUID;
    !static !transient <fields>;
}

# ---- Play Billing API, for Meta's purchase auto-logging ----
# The Meta SDK reaches the Billing library by reflection (class and method names) to log
# purchases automatically. R8 otherwise renames or merges those classes (BillingClient is gone).
-keep class com.android.billingclient.api.** { *; }

# ---- Build warnings only ----
# Classes the ad SDKs reference but don't ship (an annotation, and two optional Pangle types).
-dontwarn com.facebook.infer.annotation.**
-dontwarn com.bytedance.sdk.openadsdk.core.model.NetExtParams$RenderType
-dontwarn com.bytedance.sdk.openadsdk.core.settings.TTSdkSettings$FETCH_REQUEST_SOURCE
