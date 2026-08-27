# GeckoView and Compose keep their required rules through their dependencies.
# SnakeYAML's optional JavaBeans introspector is not present on Android.
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

# These classes are application entry points or are instantiated by reflection.
-dontoptimize
-keep class com.dwicao.dextra.DextraApplication { *; }
-keep class com.dwicao.dextra.MainActivity { *; }
-keep class com.dwicao.dextra.browser.BrowserViewModel { *; }
-keep class com.dwicao.dextra.browser.DownloadWorker { *; }
-keep class com.dwicao.dextra.data.BrowserDatabase { *; }
-keep class com.dwicao.dextra.data.BrowserDatabase_Impl { *; }
