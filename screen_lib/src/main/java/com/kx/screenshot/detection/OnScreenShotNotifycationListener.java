package com.kx.screenshot.detection;

import android.net.Uri;

/**截图通知接口
 * Created by liuyu
 * on 2021/11/3
 */
public interface OnScreenShotNotifycationListener {

    void onShot(String imagePath, Uri imageUri);

}
