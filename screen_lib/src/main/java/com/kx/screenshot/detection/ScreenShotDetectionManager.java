package com.kx.screenshot.detection;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentResolver.QUERY_ARG_SQL_SELECTION;

/**
 * 截图监听管理类
 * Created by liuyu
 * on 2021/10/29
 */
public class ScreenShotDetectionManager implements OnScreenShotDetection {

    private static final String TAG = "ScreenshotDetection";
    private boolean mIsHasScreenShotListener;
    /**
     * 读取媒体数据库时需要读取的列, 其中 WIDTH 和 HEIGHT 字段在 API 16 以后才有
     */
    private static final String[] MEDIA_PROJECTIONS = {
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.WIDTH,
            MediaStore.Images.ImageColumns.HEIGHT
    };
    /**
     * 截屏依据中的路径判断关键字
     */
    private static final String[] KEYWORDS = {
            "screenshot", "screen_shot", "screen-shot", "screen shot",
            "screencapture", "screen_capture", "screen-capture", "screen capture",
            "screencap", "screen_cap", "screen-cap", "screen cap"
    };
    private static Point mScreenRealSize;
    /**
     * 已回调过的路径
     */
    private final static List<String> sHasCallbackPaths = new ArrayList<String>();
    private WeakReference<Context> mWeakReference;
    private OnScreenShotNotifycationListener mListener;
    private long mStartListenTime;
    private Context mContext;
    /**
     * 内部存储器内容观察者
     */
    private MediaContentObserver mInternalObserver;
    /**
     * 外部存储器内容观察者
     */
    private MediaContentObserver mExternalObserver;
    /**
     * 运行在 UI 线程的 Handler, 用于运行监听器回调
     */
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    private ScreenShotDetectionManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("The context must not be null.");
        }
        mContext = context;
        mWeakReference = new WeakReference<Context>(context);
        // 获取屏幕真实的分辨率
        mScreenRealSize = getRealScreenSize(context);
    }

    public static OnScreenShotDetection create(Context context) {
        assertInMainThread();
        return new ScreenShotDetectionManager(context);
    }

    /**
     * 获取ContentResolver
     *
     * @return
     */
    private ContentResolver getContentResolver() {
        return mWeakReference.get().getContentResolver();
    }

    /**
     * 检查ContentResolver对象是否合法
     *
     * @return
     */
    private boolean checkedContentResolver() {
        return mWeakReference != null && mWeakReference.get() != null && mWeakReference.get().getContentResolver() != null;
    }

    /**
     * 启动监听
     */
    private void startListen() {
        assertInMainThread();
        //HasCallbackPaths.clear();
        // 记录开始监听的时间戳
        mStartListenTime = System.currentTimeMillis();
        // 创建内容观察者
        if (mExternalObserver == null) {
            mExternalObserver = new MediaContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mUiHandler);
        }
        // 注册内容观察者
        if (mInternalObserver == null) {
            mInternalObserver = new MediaContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, mUiHandler);
        }
        //Android Q(10) ContentObserver 不回调 onChange
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            // 注册内容观察者
            mContext.getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    true,
                    mInternalObserver
            );
            mContext.getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mExternalObserver
            );
        }

        if (checkedContentResolver()) {
            ContentResolver contentResolver = getContentResolver();
            contentResolver.registerContentObserver(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    true,
                    mInternalObserver
            );
            contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mExternalObserver
            );
        }
    }

    /**
     * 停止监听
     */
    private void stopListen() {
        assertInMainThread();
        // 注销内容观察者
        if (checkedContentResolver()) {
            try {
                ContentResolver contentResolver = getContentResolver();
                if (mInternalObserver != null) {
                    contentResolver.unregisterContentObserver(mInternalObserver);
                }
                if (mExternalObserver != null) {
                    contentResolver.unregisterContentObserver(mExternalObserver);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 清空数据
        mStartListenTime = 0;
    }
    /**
     * 获取屏幕分辨率
     */
    private Point getRealScreenSize() {
        Point screenSize = null;
        try {
            screenSize = new Point();
            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display defaultDisplay = windowManager.getDefaultDisplay();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                defaultDisplay.getRealSize(screenSize);
            } else {
                try {
                    Method mGetRawW = Display.class.getMethod("getRawWidth");
                    Method mGetRawH = Display.class.getMethod("getRawHeight");
                    screenSize.set(
                            (Integer) mGetRawW.invoke(defaultDisplay),
                            (Integer) mGetRawH.invoke(defaultDisplay)
                    );
                } catch (Exception e) {
                    screenSize.set(defaultDisplay.getWidth(), defaultDisplay.getHeight());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return screenSize;
    }
    private int dp2px(Context ctx, float dp) {
        float scale = ctx.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * 获取截屏并进行二次处理
     *
     * @param context
     * @param screenFilePath
     * @return
     */
    /*
    public Bitmap createScreenShotBitmap(Context context, String screenFilePath) {
        View v = LayoutInflater.from(context).inflate(R.layout.screen_view_layout, null);
        ImageView iv = (ImageView) v.findViewById(R.id.svl_pic);
        Bitmap bitmap = BitmapFactory.decodeFile(screenFilePath);
        iv.setImageBitmap(bitmap);
        //整体布局
        Point point = getRealScreenSize();
        v.measure(View.MeasureSpec.makeMeasureSpec(point.x, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(point.y, View.MeasureSpec.EXACTLY));

        v.layout(0, 0, point.x, point.y);
        //增加的高度120是二次编辑增加的高度（二维码及文本的高度）
        Bitmap result = Bitmap.createBitmap(v.getWidth(), v.getHeight() + dp2px(context, 120), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(result);
        c.drawColor(Color.WHITE);
        // Draw view to canvas
        v.draw(c);

        return result;
    }
*/

    /**
     * 处理媒体数据库的内容改变
     */
    @SuppressLint("Range")
    private void handleMediaContentChange(Uri contentUri) {
        Cursor cursor = null;

        try {
            // 数据改变时查询数据库中最后加入的一条数据
            if (checkedContentResolver()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q/*29*/) {
                    final Bundle bundle = new Bundle();
                    bundle.putString(QUERY_ARG_SQL_SELECTION, MediaStore.MediaColumns.SIZE + " > ?");
                    bundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, new String[]{"0"});
                    bundle.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, new String[]{MediaStore.Files.FileColumns._ID});
                    bundle.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
                    bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, 1);
                    bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, 0);
                    cursor = getContentResolver().query(contentUri, MEDIA_PROJECTIONS,
                            bundle,
                            null);
                } else {
                    cursor = getContentResolver().query(
                            contentUri,
                            MEDIA_PROJECTIONS,
                            null,
                            null,
                            MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1"
                    );
                }
            }
            if (cursor == null) {
                Log.e(TAG, "Deviant logic.");
                return;
            }
            if (!cursor.moveToFirst()) {
                Log.d(TAG, "Cursor no data.");
                return;
            }
            // 获取各列的索引
            int dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            int dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN);
            int widthIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH);
            int heightIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT);
            // 获取行数据
            String data = cursor.getString(dataIndex);
            long dateTaken = cursor.getLong(dateTakenIndex);
            int width = 0;
            int height = 0;
            if (widthIndex > 0 && heightIndex > 0) {
                width = cursor.getInt(widthIndex);
                height = cursor.getInt(heightIndex);
            }
            // data 是一个路径字符串
            // 处理获取到的第一行数据
            handleMediaRowData(data, dateTaken, width, height);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    private Point getImageSize(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        return new Point(options.outWidth, options.outHeight);
    }

    /**
     * 处理获取到的一行数据
     */
    private void handleMediaRowData(String data, long dateTaken, int width, int height) {
        if (checkScreenShot(data, dateTaken, width, height)) {
            Log.d(TAG, "ScreenShot: path = " + data + "; size = " + width + " * " + height
                    + "; date = " + dateTaken);
            if (mListener != null && !checkCallback(data)) {
                Uri imagetUri = null;
                if (checkedContentResolver()) {
                    imagetUri = getImageContentUri(getContentResolver(), data);
                }
                mListener.onShot(data, imagetUri);
            }
        } else {
            // 如果在观察区间媒体数据库有数据改变，又不符合截屏规则，则输出到 log 待分析
            Log.w(TAG, "Media content changed, but not screenshot: path = " + data
                    + "; size = " + width + " * " + height + "; date = " + dateTaken);
        }
    }

    /**
     * 将图片路径转换成uri
     *
     * @param contentResolver
     * @param path
     * @return
     */
    private Uri getImageContentUri(ContentResolver contentResolver, String path) {
        try {
            if (checkedContentResolver()) {
                Cursor cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ",
                        new String[]{path}, null);
                if (cursor != null && cursor.moveToFirst()) {
                    @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                    Uri baseUri = Uri.parse("content://media/external/images/media");
                    return Uri.withAppendedPath(baseUri, "" + id);
                } else {
                    // 如果图片不在手机的共享图片数据库，就先把它插入。
                    if (new File(path).exists()) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.DATA, path);
                        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断指定的数据行是否符合截屏条件
     */
    private boolean checkScreenShot(String data, long dateTaken, int width, int height) {
        /*
         * 判断依据一: 时间判断
         */
        // 如果加入数据库的时间在开始监听之前, 或者与当前时间相差大于10秒, 则认为当前没有截屏
        if (dateTaken < mStartListenTime || (System.currentTimeMillis() - dateTaken) > 10 * 1000) {
            return false;
        }
        /*
         * 判断依据二: 尺寸判断
         */
        if (mScreenRealSize != null) {
            // 如果图片尺寸超出屏幕, 则认为当前没有截屏
            if (!((width <= mScreenRealSize.x && height <= mScreenRealSize.y)
                    || (height <= mScreenRealSize.x && width <= mScreenRealSize.y))) {
                return false;
            }
        }
        /*
         * 判断依据三: 路径判断
         */
        if (TextUtils.isEmpty(data)) {
            return false;
        }
        data = data.toLowerCase();
        // 判断图片路径是否含有指定的关键字之一, 如果有, 则认为当前截屏了
        for (String keyWork : KEYWORDS) {
            if (data.contains(keyWork)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否已回调过, 某些手机ROM截屏一次会发出多次内容改变的通知; <br/
     * 删除一个图片也会发通知, 同时防止删除图片时误将上一张符合截屏规则的图片当做是当前截屏.
     */
    private boolean checkCallback(String imagePath) {
        if (sHasCallbackPaths.contains(imagePath)) {
            Log.d(TAG, "ScreenShot: imgPath has done"
                    + "; imagePath = " + imagePath);
            return true;
        }
        // 大概缓存15~20条记录便可
        if (sHasCallbackPaths.size() == 20) {
            for (int i = 0; i < 5; i++) {
                sHasCallbackPaths.remove(0);
            }
        }
        sHasCallbackPaths.add(imagePath);
        return false;
    }

    /**
     * 获取屏幕分辨率
     *
     * @param context
     */
    private Point getRealScreenSize(Context context) {
        Point screenSize = null;
        try {
            screenSize = new Point();
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display defaultDisplay = windowManager.getDefaultDisplay();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                defaultDisplay.getRealSize(screenSize);
            } else {
                DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
                screenSize.x = displayMetrics.widthPixels;
                screenSize.y = displayMetrics.heightPixels;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return screenSize;
    }

    private static void assertInMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            String methodMsg = null;
            if (elements != null && elements.length == 4) {
                methodMsg = elements[3].toString();
            }
            throw new IllegalStateException("Call the method must be in main thread: " + methodMsg);
        }
    }

    /**
     * 媒体内容观察者(观察媒体数据库的改变)
     */
    private class MediaContentObserver extends ContentObserver {
        private Uri mContentUri;

        public MediaContentObserver(Uri contentUri, Handler handler) {
            super(handler);
            mContentUri = contentUri;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            handleMediaContentChange(mContentUri);
        }
    }

    @Override
    public void startScreenShotDetection() {
        if (!mIsHasScreenShotListener) {
            startListen();
            mIsHasScreenShotListener = true;
        }
    }

    @Override
    public void stopScreenShotDetection() {
        if (mIsHasScreenShotListener) {
            stopListen();
            mIsHasScreenShotListener = false;
        }
    }

    /**
     * 设置截屏监听器
     */
    @Override
    public void setScreenShotChangeListener(OnScreenShotNotifycationListener listener) {
        mListener = listener;
    }
}