package com.kx.screen;

import static com.aliyun.orc.Sample.takeImageToText;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.PermissionUtils;
import com.kx.screenshot.detection.OnScreenShotDetection;
import com.kx.screenshot.detection.OnScreenShotNotifycationListener;
import com.kx.screenshot.detection.ScreenShotDetectionManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {
    private void avoidLauncherAgain() {
        // 避免从桌面启动程序后，会重新实例化入口类的activity
        if (!this.isTaskRoot()) { // 判断当前activity是不是所在任务栈的根
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                    finish();
                }
            }
        }
    }

    private OnScreenShotDetection mDetection;
    private ImageView mShotIv;
    private TextView mHintTv;
    private  String TAG = "=============";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        super.onCreate(savedInstanceState);
        avoidLauncherAgain();
        setContentView(R.layout.activity_main);
        mShotIv = findViewById(R.id.shot_iv);
        mHintTv = findViewById(R.id.hint_tv);

        boolean granted = PermissionUtils.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE);
        //7.0及以上必须申请存储权限才可以使用，否则无法获取到截图图片的uri
        PermissionUtils.permission(Manifest.permission.READ_EXTERNAL_STORAGE).callback(new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(@NonNull List<String> granted) {
                initScreenShot();
            }

            @Override
            public void onDenied(@NonNull List<String> deniedForever, @NonNull List<String> denied) {
                Toast.makeText(MainActivity.this, "无存储权限", Toast.LENGTH_LONG).show();
            }
        }).request();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
        super.onBackPressed();

    }
    private void initScreenShot() {
        //创建OnScreenShotDetection实现类对象
        mDetection = ScreenShotDetectionManager.create(this);
        //设置屏幕截图监听
        mDetection.setScreenShotChangeListener(new OnScreenShotNotifycationListener() {
            @Override
            public void onShot(String imagePath, Uri imageUri) {
                // imagePath 不能直接使用，由于安卓10系统及以上，限制了访问SD卡，需要使用ContentResolver访问。
                // 通过imagePath获取图片的Uri可以使用
                if (mShotIv != null) {
                    mShotIv.setImageURI(imageUri);
                }
                String content="";
                updateHint();
                Toast.makeText(MainActivity.this, "image path:" + imagePath.toString(), Toast.LENGTH_LONG).show();

                try {
                    ContentResolver contentResolver = getContentResolver();
                    InputStream inputStream = contentResolver.openInputStream(imageUri);
                    content = takeImageToText(inputStream);

                }catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Log.i(TAG,"path completed: " + imagePath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Toast.makeText(MainActivity.this, content, Toast.LENGTH_LONG).show();

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//                ClipData textCd = ClipData.newPlainText("kkk", imagePath.toString());
                ClipData textCd = ClipData.newPlainText("kkk",content);
                clipboard.setPrimaryClip(textCd);

               /*
                //                Toast.makeText(MainActivity.this, "图片路径 ：" + imagePath.toString(), Toast.LENGTH_LONG).show();
                Item item = null;
                //如果是文本信息
                if (clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    ClipData cdText = clipboard.getPrimaryClip();
                    item = cdText.getItemAt(0);
                    //此处是TEXT文本信息
                    if (item.getText() == null) {
                        Toast.makeText(getApplicationContext(), "剪贴板中无内容", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        Toast.makeText(getApplicationContext(), item.getText(), Toast.LENGTH_SHORT).show();
                    }
                }*/
            }
        });
    }

    private void updateHint() {
        String text = mHintTv.getText().toString();
        Integer integer = Integer.parseInt(text);
        integer++;
        mHintTv.setText(String.valueOf(integer));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDetection != null) {
            //开启屏幕截图监听
            mDetection.startScreenShotDetection();
        }
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (mDetection != null) {
//            //关闭屏幕截图监听
//            mDetection.stopScreenShotDetection();
//        }
//    }
}