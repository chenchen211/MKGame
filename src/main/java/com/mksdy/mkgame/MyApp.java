package com.mksdy.mkgame;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.chenchen.CommonUtils.T;
import com.mksdy.mkgame.bean.BaseBean;
import com.mksdy.mkgame.bean.Line;
import com.mksdy.mkgame.bean.NewsJson;
import com.mksdy.mkgame.bean.User;
import com.mksdy.mkgame.bean.Version;
import com.mksdy.mkgame.utils.BankJsonParser;
import com.mksdy.mkgame.utils.Constants;
import com.mksdy.mkgame.utils.HttpResponseUtils;
import com.mksdy.mkgame.utils.SharedSetting;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/8/27.
 */
public class MyApp extends Application {

    private static final String TAG = "chenchen";
    public static User currentUser;

    public static Context appContext;

    public static int newVersion;
    public static int localVersion;
    public static String localVersionName;
    public static String newVersionName;
    public static File cacheDir;
    public static float density;
    public static SharedSetting sharedSetting;
    public static List<NewsJson.News> news = new ArrayList<>();
    public static Line lineData;
    @Override
    public void onCreate() {
        super.onCreate();
        appContext=this;
        checkVersion();
        initImageLoader();
        sharedSetting = SharedSetting.getInstance(this);
        if(SharedSetting.isLogin()){
            currentUser = new User(sharedSetting.getUsername());
            setCurrentUser();
//            startSession();
        }
        getNews();
        getDensity();
        BankJsonParser.getInstance(this);
    }

    private void getLineData() {
        HttpResponseUtils.getBean(Constants.Url.LINE, Line.class, new HttpResponseUtils.OnResponseListener() {
            @Override
            public void onSuccess(BaseBean bean) {
                lineData = ((Line) bean);
            }

            @Override
            public void onFailed(String error) {
                T.showShort(appContext,"网络请求失败");
            }
        });
    }

    private void getNews(){
        HttpResponseUtils.getBean(Constants.Url.NEWS, NewsJson.class, new HttpResponseUtils.OnResponseListener() {
            @Override
            public void onSuccess(BaseBean bean) {
                news = ((NewsJson) bean).getNews();
            }

            @Override
            public void onFailed(String error) {
                T.showShort(appContext,"获取公告失败");
            }
        });
    }
    private void setCurrentUser() {
        HttpResponseUtils.getBean(String.format(Constants.Url.USER_INFO,sharedSetting.getUsername()), User.class, new HttpResponseUtils.OnResponseListener() {
            @Override
            public void onSuccess(BaseBean bean) {
                User user = ((User) bean);
                currentUser = user;
            }

            @Override
            public void onFailed(String error) {
                Toast.makeText(appContext,"网络请求失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startSession() {
        HttpResponseUtils.getString(String.format(Constants.Url.START_SESSION,sharedSetting.getUsername()), new HttpResponseUtils.OnGetStringListener() {
            @Override
            public void getString(String s) {
                Log.i(TAG, "getString: "+s);
            }

            @Override
            public void onFailed(String e) {
                Log.i(TAG, "onFailed: "+e);
            }
        });
    }

    private void getDensity() {
        DisplayMetrics dm =getResources().getDisplayMetrics();
        density  = dm.density;        // 屏幕密度（像素比例：0.75/1.0/1.5/2.0）
    }

    private void checkVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            localVersion = packageInfo.versionCode; // 设置本地版本号
            localVersionName =  packageInfo.versionName;
            String url = Constants.Url.VERSION;
            HttpResponseUtils.getBean(url, Version.class, new HttpResponseUtils.OnResponseListener() {
                @Override
                public void onSuccess(BaseBean bean) {
                    Version version = ((Version) bean);
                    newVersion=Integer.parseInt(version.getVersionNum());
                    newVersionName=version.getVersionName();
                }

                @Override
                public void onFailed(String error) {

                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 初始化ImageLoader,参考http://blog.csdn.net/zenjj11/article/details/38728481
     */
    private void initImageLoader() {
        cacheDir = StorageUtils.getOwnCacheDirectory(this, "imageloader/Cache");
        DisplayImageOptions options = new DisplayImageOptions.Builder()
//                 .showImageOnLoading(R.mipmap.loading)
                //设置图片在下载期间显示的图片
//                 .showImageForEmptyUri(R.mipmap.loading)
//                 //设置图片Uri为空或是错误的时候显示的图片
//                 .showImageOnFail(R.mipmap.loading)
//                 //设置图片加载/解码过程中错误时候显示的图片
                .cacheInMemory(false)
                // 设置下载的图片是否缓存在内存中
                .cacheOnDisc(false)
                .displayer(new FadeInBitmapDisplayer(100))
                .considerExifParams(true)
                .displayer(new FadeInBitmapDisplayer(100))// 图片加载好后渐入的动画时间
                .displayer(new RoundedBitmapDisplayer(1)).build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration
                .Builder(this)
                .memoryCacheExtraOptions(480, 800) // maxwidth, max height，即保存的每个缓存文件的最大长宽
                .threadPoolSize(3)//线程池内加载的数量
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024)) // You can pass your own memory cache implementation/你可以通过自己的内存缓存实现
                .memoryCacheSize(5 * 1024 * 1024)  //内存缓存大小
                .discCacheSize(50 * 1024 * 1024)   //外部缓存大小
                .discCacheFileNameGenerator(new Md5FileNameGenerator())//将保存的时候的URI名称用MD5 加密
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .discCacheFileCount(100) //缓存的文件数量
                .discCache(new UnlimitedDiscCache(cacheDir))//自定义缓存路径
                .imageDownloader(new BaseImageDownloader(this, 5 * 1000, 30 * 1000)) // connectTimeout (5 s), readTimeout (30 s)超时时间
                .defaultDisplayImageOptions(options)
                .writeDebugLogs() // Remove for releaseapp
                .build();//开始构建
        ImageLoader.getInstance().init(config);
    }
}
