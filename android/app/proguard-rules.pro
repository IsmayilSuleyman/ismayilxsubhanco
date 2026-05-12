-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.subhanismayil.budget.**$$serializer { *; }
-keepclassmembers class com.subhanismayil.budget.** {
    *** Companion;
}
-keepclasseswithmembers class com.subhanismayil.budget.** {
    kotlinx.serialization.KSerializer serializer(...);
}
