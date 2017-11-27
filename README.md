# CloudMusicXposed #

网易云音乐Android客户端插件

- 自动签到
- 阻止签到按钮打开商城 (支持v4.2.0，其他版本未测试)
- 阻止程序期启动页广告: 阻止fragment的创建，启动速度明显加快 (支持v4.2.0，其他版本未测试)
- 去除因版权问题无法分享歌词图片的限制 (支持v4.1.1及以上，v4.1.1以下未测试)

NeteaseCloudMusic for Android auto sign

**Download:**  [CloudMusicXposed_v2.1](https://github.com/XF-zhjnc/CloudMusicXposed/raw/master/app/cloudmusicxposed_v2.1.apk)

## Screenshot ##

![](/screenshot/op.png) 
![](/screenshot/sharelyc.png) 


## About ##

自用插件，不会适配太多版本，需要请fork代码自行编译

## TODO List ##

这里是即将要添加的功能，如果您有新的想法，可以开issues，如果您实现了该功能，欢迎PR。

1. TODO  添加通过颜色代码设置主题色的快捷入口
2. TODO  去除升级提示
3. TODO  解锁vip专属歌词分享模板

## BUG日志 ##

* 2017.03.26: Xposed提示找不到入口类，猜想可能是更新了Android Studio到2.3，混淆规则有变化，把主类名字给混淆了。-keep 主类，问题解决
* 2017.04.16: 想实现在Xposed管理器里面点击插件跳转到网易云音乐关于页面，然而关于页面没有任何的action等，遇到`Permission Denial`，无法简单的构建Intent启动。问题未解决
* 2017.05.03：新增用遍历`TextView`方法找控件的方式支持更多版本。但网易云音乐的“当前版本”`TextView`不是使用`setText()`而是本身有文本再`append()`它的版本号，只能通过寻找另一个易获取的控件再通过界面层级分析得到该`TextView`，添加本插件信息时要等它`append()`完(即contains("V"))后再添加，否则它的版本号`append()`在插件信息后
* 2017.05.03： `AboutActivity`中用于启动该Activity的静态方法未能call成功
* 2017.08.06： 歌词分享功能在v4.1.3版本中失效，原因: 要hook的类包名改了（从`ui.LyricView`中的分享按钮往下找）
* 2017.08.07： 切换颜色的Listener是匿名内部类，应寻求别的有效方法
* 2017.11.27： 诈尸更新，只适配v4.2.0

------