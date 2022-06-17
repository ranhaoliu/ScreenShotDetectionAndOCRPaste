# ScreenShotDetection

#### Description
注意要把这个app 开成小窗模式才能使用

屏幕截图监听
获取手动截图时，截图图片路径的Uri
把图片上传到阿里云OCR文字识别接口返回提取到的文字并保存到手机粘贴版
[阿里通用文字识别](https://next.api.aliyun.com/api/ocr-api/2021-07-07/RecognizeGeneral?lang=JAVA&sdkStyle=dara&params={})
#### Software Architecture
截屏监听---> 发送截屏图片到阿里云通用文字识别服务器--->返回识别文字--->复制到粘贴板


### 代码使用:
[阿里通用文字识别](https://next.api.aliyun.com/api/ocr-api/2021-07-07/RecognizeGeneral?lang=JAVA&sdkStyle=dara&params={})
获取AK,
把AccessKeyId 和密钥替换到com.aliyun.orc.Sample.java 的takeImageToText方法的第一行
#### Installation

1.  权限。7.0及以上必须获取存储权限，否则虽然能监听到截图事件，但无法获取图片路径。
2.  截图回调的图片路径需要使用Uri对象，由于安卓10及以上调整访问SD权限，使用文件路径直接访问图片是访问不了的。
3.  监听。当app切到后台时，可关闭截图监听，切后前台时，打开监听。另外，部分敏感界面，如账号登录，个人消息等信息界面可考虑屏蔽监听截图功能。

#### 核心代码

        //step1: 创建OnScreenShotDetection实现类对象
        mDetection = ScreenShotDetectionManager.create(this);
        //step2: 设置屏幕截图监听
        mDetection.setScreenShotChangeListener(new OnScreenShotNotifycationListener() {
            @Override
            public void onShot(String imagePath, Uri imageUri) {
                // imagePath 不能直接使用，由于安卓10系统及以上，限制了访问SD卡，需要使用ContentResolver访问。
                // 通过imagePath获取图片的Uri可以使用
                if (mShotIv != null) {
                    mShotIv.setImageURI(imageUri);
                }
                updateHint();
                Toast.makeText(MainActivity.this, "图片路径 ：" + imagePath.toString(), Toast.LENGTH_LONG).show();
            }
        });


    @Override
    protected void onResume() {
        super.onResume();
        if (mDetection != null) {
            //开启屏幕截图监听
            mDetection.startScreenShotDetection();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDetection != null) {
            //关闭屏幕截图监听
            mDetection.stopScreenShotDetection();
        }
    }

 


