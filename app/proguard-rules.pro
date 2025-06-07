# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepattributes Annotation
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

-keepclassmembers class org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

-keep class org.greenrobot.eventbus.android.AndroidComponentsImpl

-keep class com.messages.sms.textingapp.ai.messaging.data.Model.** { *; }
-keep class com.messages.sms.textingapp.ai.messaging.data.Database.Scheduled.ScheduledMessage.* { *; }
-keep class com.messages.sms.textingapp.ai.messaging.data.Database.Archived.ArchivedConversation.* { *; }
-keep class com.messages.sms.textingapp.ai.messaging.data.Database.Block.BlockConversation.* { *; }
-keep class com.messages.sms.textingapp.ai.messaging.data.Database.Notification.NotificationSetting.* { *; }
-keep class com.messages.sms.textingapp.ai.messaging.data.Database.Pin.PinMessage.* { *; }
-keep class com.messages.sms.textingapp.ai.messaging.data.Database.RecyclerBin.** { *; }
-keep class com.messages.sms.textingapp.ai.messaging.data.Database.Starred.StarredMessage.* { *; }
#-keep class com.messages.sms.textingapp.ai.messaging.Helper.MessageScheduler.* { *; }
-keep class com.messages.sms.textingapp.ai.messaging.Ui.Dialogs.ScheduleDialog.* { *; }
