# ImageSelector

![ImageSelector](http://shields.hust.cc/ImageSelector-0.2.3-blue.svg)

该项目修改自[bilibili-boxing](https://github.com/Bilibili/boxing)(其实是给自己的项目用而作的修改)

主要修改如下：

- popupwindow的动画重写(原动画由于未知原因在项目里会失效)
- 选择视频界面提供了录像功能(由于项目要求，录像清晰度比较低，暂未提供修改接口)
- 修复原项目在本机没有图片、视频时一直loading的bug
- 不会强制在DCIM下生成一个bilibili/boxing文件夹
- 顶栏样式修改



```groovy
// 核心库
compile 'com.kanade:imageselector-core:0.2.3'

// 带UI实现
compile 'com.kanade:imageselector-imply:0.2.3'
```

