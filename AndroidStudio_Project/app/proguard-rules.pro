-dontwarn android.support.**
-dontwarn com.google.android.gms.**
-keep class com.batsoft.boomjunkie.SaveData  {*; }
#below commands for including the line numbers in traces
-renamesourcefileattribute SourceFile    
-keepattributes SourceFile,LineNumberTable