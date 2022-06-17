package com.kx.screenshot.detection;


/**截图监听管理接口
 * Created by liuyu
 * on 2021/11/2
 */
public interface OnScreenShotDetection {


    void startScreenShotDetection();

    void stopScreenShotDetection();

    void setScreenShotChangeListener(OnScreenShotNotifycationListener listener);

}
