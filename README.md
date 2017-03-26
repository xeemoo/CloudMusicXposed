# CloudMusicXposed #

网易云音乐Android客户端自动签到插件

NeteaseCloudMusic for Android auto sign

## Screenshot ##

![](/screenshot/opo.png) 


## About ##

心血来潮写的自用插件，写完发现酷安上已经有同类的[插件](http://www.coolapk.com/apk/com.specher.music163)，而且刚发布不久。。。

本来只hook签到TextView，受了启发顺便把AboutActivity改了

更多版本等我手机网易云音乐更新了再去找资源ID


## BUG日志 ##

* 2017.03.26: Xposed提示找不到入口类，猜想可能是更新了Android Studio到2.3，混淆规则有变化，把主类名字给混淆了。-keep 主类，问题解决

------