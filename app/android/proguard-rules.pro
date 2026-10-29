# Preserve Rhino's interpreter used by NewPipeExtractor on Android.
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.tools.**
# Rhino also bundles optional Java SE integrations. Android has no JavaBeans or Dynalink APIs;
# NewPipeExtractor invokes Rhino in interpreted mode and does not use those optional classes.
-dontwarn java.beans.**
-dontwarn jdk.dynalink.**
