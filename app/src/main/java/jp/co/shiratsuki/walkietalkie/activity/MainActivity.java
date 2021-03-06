package jp.co.shiratsuki.walkietalkie.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.kevin.crop.UCrop;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jp.co.shiratsuki.walkietalkie.BuildConfig;
import jp.co.shiratsuki.walkietalkie.R;
import jp.co.shiratsuki.walkietalkie.activity.appmain.CropActivity;
import jp.co.shiratsuki.walkietalkie.activity.base.BaseActivity;
import jp.co.shiratsuki.walkietalkie.activity.loginregister.LoginRegisterActivity;
import jp.co.shiratsuki.walkietalkie.activity.settings.SetLanguageActivity;
import jp.co.shiratsuki.walkietalkie.activity.settings.SetMessageServerActivity;
import jp.co.shiratsuki.walkietalkie.activity.settings.SetPersonalInfoActivity;
import jp.co.shiratsuki.walkietalkie.activity.settings.SetVoiceServerActivity;
import jp.co.shiratsuki.walkietalkie.activity.version.QrCodeShareActivity;
import jp.co.shiratsuki.walkietalkie.activity.version.VersionsActivity;
import jp.co.shiratsuki.walkietalkie.bean.NormalResult;
import jp.co.shiratsuki.walkietalkie.bean.User;
import jp.co.shiratsuki.walkietalkie.bean.WebSocketData;
import jp.co.shiratsuki.walkietalkie.bean.version.Version;
import jp.co.shiratsuki.walkietalkie.broadcast.BaseBroadcastReceiver;
import jp.co.shiratsuki.walkietalkie.constant.Constants;
import jp.co.shiratsuki.walkietalkie.constant.NetWork;
import jp.co.shiratsuki.walkietalkie.contentprovider.SPHelper;
import jp.co.shiratsuki.walkietalkie.fragment.ChatRoomFragment;
import jp.co.shiratsuki.walkietalkie.fragment.ContactsFragment;
import jp.co.shiratsuki.walkietalkie.fragment.MalfunctionFragment;
import jp.co.shiratsuki.walkietalkie.interfaces.OnPictureSelectedListener;
import jp.co.shiratsuki.walkietalkie.network.ExceptionHandle;
import jp.co.shiratsuki.walkietalkie.network.NetClient;
import jp.co.shiratsuki.walkietalkie.network.NetworkSubscriber;
import jp.co.shiratsuki.walkietalkie.permission.floatwindow.FloatWindowManager;
import jp.co.shiratsuki.walkietalkie.service.IVoiceCallback;
import jp.co.shiratsuki.walkietalkie.service.IVoiceService;
import jp.co.shiratsuki.walkietalkie.service.VoiceService;
import jp.co.shiratsuki.walkietalkie.service.WebSocketService;
import jp.co.shiratsuki.walkietalkie.utils.ActivityController;
import jp.co.shiratsuki.walkietalkie.utils.ApkUtils;
import jp.co.shiratsuki.walkietalkie.utils.BitmapUtils;
import jp.co.shiratsuki.walkietalkie.utils.FileUtil;
import jp.co.shiratsuki.walkietalkie.utils.GsonUtils;
import jp.co.shiratsuki.walkietalkie.utils.LogUtils;
import jp.co.shiratsuki.walkietalkie.utils.MathUtils;
import jp.co.shiratsuki.walkietalkie.utils.NetworkUtil;
import jp.co.shiratsuki.walkietalkie.utils.NotificationsUtil;
import jp.co.shiratsuki.walkietalkie.utils.StatusBarUtil;
import jp.co.shiratsuki.walkietalkie.utils.ViewUtil;
import jp.co.shiratsuki.walkietalkie.utils.WifiUtil;
import jp.co.shiratsuki.walkietalkie.widget.dialog.DownLoadDialog;
import jp.co.shiratsuki.walkietalkie.widget.MyProgressBar;
import jp.co.shiratsuki.walkietalkie.widget.NoScrollViewPager;
import jp.co.shiratsuki.walkietalkie.widget.SelectPicturePopupWindow;
import jp.co.shiratsuki.walkietalkie.widget.dialog.CommonWarningDialog;
import jp.co.shiratsuki.walkietalkie.widget.dialog.UpgradeVersionDialog;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 主页面
 * Created at 2019/1/11 17:16
 *
 * @author Li Yuliang
 * @version 1.0
 */

public class MainActivity extends BaseActivity implements SelectPicturePopupWindow.OnSelectedListener {

    private static final String TAG = "MainActivity";
    private Context mContext;
    private DrawerLayout drawerLayout;
    private NavigationView navigation;
    private NoScrollViewPager viewPager;
    private List<FrameLayout> menus;
    private LinearLayout llMain, llNotification;
    private ImageView ivIcon, ivUserIcon, ivNewVersion;
    private TextView tvNotification, tvCompanyName, tvDepartment, tvUserName;
    private MyReceiver myReceiver;
    private IVoiceService iVoiceService;
    private WebSocketService webSocketService;

    private CommonWarningDialog commonWarningDialog;

    private boolean isInRoom = false, isSpeaking = false, isUseSpeaker = false;
    private TextView tvSSID, tvIp;
    private Button btnSpeak, btnSpeaker;

    private Vibrator vibrator;

    private static final int GALLERY_REQUEST_CODE = 0;
    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int GET_UNKNOWN_APP_SOURCES = 2;
    protected static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    protected static final int REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 102;
    protected static final int INSTALL_PACKAGES_REQUEST_CODE = 103;
    private String mTempPhotoPath;
    private Uri mDestinationUri;
    private SelectPicturePopupWindow mSelectPicturePopupWindow;
    private OnPictureSelectedListener mOnPictureSelectedListener;

    private String versionFileName, latestVersionMD5, latestVersionLog, apkDownloadPath;
    private MyProgressBar myProgressBar;
    private TextView tvCompletedSize, tvTotalSize;
    private float apkSize, completedSize;

    private boolean isAutoCheck = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        initView();
        initBroadcastReceiver();

        User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);
        if (user.getUser_name().equals("") || user.getCompany().equals("") || user.getDepartment_name().equals("")) {
            // 补充个人信息
            showUpdateInfoDialog();
        } else {
            initWebSocketService();
            initVoiceService();
        }

        // 初始化耳机按键标记
        SPHelper.save("KEY_STATUS_UP", true);
        // 初始化异常声音是否允许播放的标记
        SPHelper.save("CanPlay", true);

        // 设置裁剪图片结果监听
        setOnPictureSelectedListener(onPictureSelectedListener);
        mDestinationUri = Uri.fromFile(new File(getExternalFilesDir("Icons"), "cropImage.jpeg"));
        mTempPhotoPath = Environment.getExternalStorageDirectory() + File.separator + "photo.jpeg";
        mSelectPicturePopupWindow = new SelectPicturePopupWindow(mContext, (findViewById(android.R.id.content)));
        mSelectPicturePopupWindow.setOnSelectedListener(this);

        checkNewVersion();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStringEvent(String msg) {
        LogUtils.d(TAG, "走了更新语言的方法");
        ViewUtil.updateViewLanguage(findViewById(android.R.id.content));
        // 更新异常信息列表的语言
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        MalfunctionFragment malfunctionFragment;
        for (Fragment fragment : fragmentList) {
            if (fragment instanceof MalfunctionFragment) {
                malfunctionFragment = (MalfunctionFragment) fragment;
                if (malfunctionFragment.isAdded()) {
                    malfunctionFragment.refreshList();
                }
                break;
            }
        }
    }

    /**
     * 显示更新个人信息的页面
     */
    private void showUpdateInfoDialog() {
        if (commonWarningDialog == null) {
            commonWarningDialog = new CommonWarningDialog(mContext, getString(R.string.notification_update_info));
            commonWarningDialog.setCancelable(false);
            commonWarningDialog.setOnDialogClickListener(new CommonWarningDialog.OnDialogClickListener() {
                @Override
                public void onOKClick() {
                    // 进入个人信息设置页面
                    openActivity(SetPersonalInfoActivity.class);
                }

                @Override
                public void onCancelClick() {
                    ActivityController.finishActivity(MainActivity.this);
                }
            });
        }
        if (!commonWarningDialog.isShowing()) {
            commonWarningDialog.show();
        }
    }

    /**
     * 初始化并绑定WebSocketService
     */
    private void initWebSocketService() {
        Intent intent = new Intent(mContext, WebSocketService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, serviceConnection1, BIND_AUTO_CREATE);
    }

    /**
     * 初始化并绑定VoiceService
     */
    private void initVoiceService() {
        Intent intent = new Intent(mContext, VoiceService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, serviceConnection2, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showOrHideNotification();

        User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);
        if (user.getUser_name().equals("") || user.getCompany().equals("") || user.getDepartment_name().equals("")) {
            // 补充个人信息
            showUpdateInfoDialog();
        }

        if (WifiUtil.WifiConnected(mContext)) {
            tvSSID.setText(WifiUtil.getSSID(mContext));
            tvIp.setText(WifiUtil.getLocalIPAddress());
        }

        tvUserName.setText(user.getUser_name());
        tvCompanyName.setText(user.getCompany());
        tvDepartment.setText(user.getDepartment_name());

        String photoPath = ("http://" + NetWork.SERVER_HOST_MAIN + ":" + NetWork.SERVER_PORT_MAIN + user.getIcon_url()).replace("\\", "/");
        SPHelper.save("userIconPath", photoPath);
        // 加载头像
        RequestOptions options = new RequestOptions().error(R.drawable.photo_user).placeholder(R.drawable.photo_user).dontAnimate();
        Glide.with(this).load(photoPath).apply(options).into(ivUserIcon);
        Glide.with(this).load(photoPath).apply(options).into(ivIcon);

    }

    /**
     * 检查新版本
     */
    private void checkNewVersion() {
        int myVersionCode = ApkUtils.getVersionCode(mContext);
        Version version = GsonUtils.parseJSON(SPHelper.getString("version", GsonUtils.convertJSON(new Version())), Version.class);
        int latestVersionCode = version.getVersionCode();
        String latestVersionName = version.getVersionName();
        versionFileName = version.getVersionFileName();
        latestVersionMD5 = version.getMd5Value();
        latestVersionLog = version.getVersionLog();
        apkDownloadPath = version.getVersionUrl().replace("\\", "/");
        if (myVersionCode < latestVersionCode) {
            // 当前版本小于服务器最新版本
            ivNewVersion.setVisibility(View.VISIBLE);
            UpgradeVersionDialog upgradeVersionDialog = new UpgradeVersionDialog(mContext);
            upgradeVersionDialog.setCancelable(false);
            ((TextView) upgradeVersionDialog.findViewById(R.id.tv_versionLog)).setText(latestVersionLog);
            ((TextView) upgradeVersionDialog.findViewById(R.id.tv_currentVersion)).setText(ApkUtils.getVersionName(mContext));
            ((TextView) upgradeVersionDialog.findViewById(R.id.tv_latestVersion)).setText(latestVersionName);
            ((TextView) upgradeVersionDialog.findViewById(R.id.title_name)).setText(String.format(getString(R.string.update_version), latestVersionName));
            upgradeVersionDialog.setOnDialogClickListener(new UpgradeVersionDialog.OnDialogClickListener() {
                @Override
                public void onOKClick() {
                    if (isDownloaded()) {
                        if (commonWarningDialog == null) {
                            commonWarningDialog = new CommonWarningDialog(mContext, getString(R.string.warning_redownload));
                            commonWarningDialog.setButtonText(getString(R.string.DownloadAgain), getString(R.string.InstallDirectly));
                            commonWarningDialog.setCancelable(false);
                            commonWarningDialog.setOnDialogClickListener(new CommonWarningDialog.OnDialogClickListener() {
                                @Override
                                public void onOKClick() {
                                    //直接安装
                                    File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), versionFileName);
                                    checkIsAndroidO(file);
                                }

                                @Override
                                public void onCancelClick() {
                                    //下载最新的版本程序
                                    downloadApk();
                                }
                            });
                        }
                        if (!commonWarningDialog.isShowing()) {
                            commonWarningDialog.show();
                        }
                    } else {
                        //下载最新的版本程序
                        downloadApk();
                    }

                }

                @Override
                public void onCancelClick() {
                    upgradeVersionDialog.dismiss();
                }
            });
            upgradeVersionDialog.show();
        } else {
            if (isAutoCheck) {
                // 如果是刚进入APP自动检查更新的话不显示通知，并改变标记位
                isAutoCheck = false;
            } else {
                showToast(R.string.YourAppIsTheLatestVersion);
            }
            ivNewVersion.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 判断是否已经下载过该文件
     *
     * @return boolean
     */
    private boolean isDownloaded() {
        File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + File.separator + versionFileName);
        LogUtils.d("MD5", file.getPath());
        return file.isFile() && latestVersionMD5.equals(FileUtil.getFileMD5(file));
    }

    /**
     * 下载新版本程序
     */
    private void downloadApk() {
        if (apkDownloadPath.equals(Constants.EMPTY)) {
            showToast("下载路径有误，请联系客服");
        } else {
            DownLoadDialog progressDialog = new DownLoadDialog(mContext);
            myProgressBar = progressDialog.findViewById(R.id.progressbar_download);
            TextView tvUpdateLog = progressDialog.findViewById(R.id.tv_updateLog);
            tvUpdateLog.setText(latestVersionLog);
            tvCompletedSize = progressDialog.findViewById(R.id.tv_completedSize);
            tvTotalSize = progressDialog.findViewById(R.id.tv_totalSize);
            progressDialog.setCancelable(false);
            progressDialog.show();
            NetClient.downloadFileProgress(apkDownloadPath, (currentBytes, contentLength, done) -> {
                //获取到文件的大小
                apkSize = MathUtils.formatFloat((float) contentLength / 1024f / 1024f, 2);
                tvTotalSize.setText(String.format(getString(R.string.file_size_m), String.valueOf(apkSize)));
                //已完成大小
                completedSize = MathUtils.formatFloat((float) currentBytes / 1024f / 1024f, 2);
                tvCompletedSize.setText(String.format(getString(R.string.file_size_m), String.valueOf(completedSize)));
                myProgressBar.setProgress(MathUtils.formatFloat(completedSize / apkSize * 100, 1));
                if (done) {
                    progressDialog.dismiss();
                }
            }, new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                    //处理下载文件
                    if (response.body() != null) {
                        try {
                            InputStream is = response.body().byteStream();
                            //定义下载后文件的路径和名字，例如：/Download/JiangSuMetter_1.0.1.apk
                            File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + File.separator + versionFileName);
                            FileOutputStream fos = new FileOutputStream(file);
                            BufferedInputStream bis = new BufferedInputStream(is);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = bis.read(buffer)) != -1) {
                                fos.write(buffer, 0, len);
                            }
                            fos.close();
                            bis.close();
                            is.close();
                            checkIsAndroidO(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showToast("下载出错，" + e.getMessage() + "，请联系管理员");
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                    progressDialog.dismiss();
                    showToast("下载出错，" + t.getMessage() + "，请联系管理员");
                }
            });
        }
    }

    /**
     * 安装apk
     *
     * @param file 需要安装的apk
     */
    private void installApk(File file) {
        //先验证文件的正确性和完整性（通过MD5值）
        if (file.isFile() && latestVersionMD5.equals(FileUtil.getFileMD5(file))) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".fileProvider", file);//在AndroidManifest中的android:authorities值
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            }
            startActivity(intent);
        } else {
            showToast("File error");
        }
    }

    /**
     * Android8.0需要处理未知应用来源权限问题,否则直接安装
     */
    private void checkIsAndroidO(File file) {
        if (Build.VERSION.SDK_INT >= 26) {
            boolean b = getPackageManager().canRequestPackageInstalls();
            if (b) {
                installApk(file);
            } else {
                //请求安装未知应用来源的权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUEST_CODE);
            }
        } else {
            installApk(file);
        }
    }

    @Override
    protected void setStatusBar() {
        int mColor = getResources().getColor(R.color.colorBluePrimary);
        StatusBarUtil.setColorForDrawerLayout(this, findViewById(R.id.drawer_layout), mColor);
    }

    /**
     * 加载头像
     *
     * @param photoPath 头像路径
     */
    private void showUserIcon(String photoPath) {
        LogUtils.d(NetClient.TAG_POST, "图片路径：" + photoPath);
        SPHelper.save("userIconPath", photoPath);
        if (photoPath != null) {
            RequestOptions options = new RequestOptions().error(R.drawable.photo_user).placeholder(R.drawable.photo_user).dontAnimate();
            Glide.with(this).load(photoPath).apply(options).into(ivUserIcon);
            Glide.with(this).load(photoPath).apply(options).into(ivIcon);
        }
    }

    private void initView() {

        navigation = findViewById(R.id.navigation);
        ViewGroup.LayoutParams params = navigation.getLayoutParams();
        params.width = mWidth * 4 / 5;
        navigation.setLayoutParams(params);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.addDrawerListener(drawerListener);

        menus = new ArrayList<>();
        FrameLayout fl_a = findViewById(R.id.fl_a);
        menus.add(fl_a);
        FrameLayout fl_b = findViewById(R.id.fl_b);
        menus.add(fl_b);
        FrameLayout fl_c = findViewById(R.id.fl_c);
        menus.add(fl_c);
        for (FrameLayout frameLayout : menus) {
            frameLayout.setOnClickListener(onClickListener);
        }

        viewPager = findViewById(R.id.viewPager);
        //设置页面可以左右滑动
        viewPager.setNoScroll(false);
        //拦截左右滑动事件
        viewPager.setIntercept(true);
        viewPager.setAdapter(viewPagerAdapter);
        //设置Fragment预加载，非常重要,可以保存每个页面fragment已有的信息,防止切换后原页面信息丢失
        viewPager.setOffscreenPageLimit(menus.size());
        // 默认选中第一个
        menus.get(0).setSelected(true);
        //viewPager添加滑动监听，用于控制TextView的展示
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                for (FrameLayout menu : menus) {
                    menu.setSelected(false);
                }
                menus.get(position).setSelected(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        llMain = findViewById(R.id.ll_main);
        ivIcon = findViewById(R.id.iv_icon);
        ivUserIcon = findViewById(R.id.iv_userIcon);
        ivNewVersion = findViewById(R.id.ivNewVersion);
        ivIcon.setOnClickListener(onClickListener);
        llNotification = findViewById(R.id.ll_notification);
        llNotification.setOnClickListener(onClickListener);
        ivUserIcon.setOnClickListener(onClickListener);

        tvNotification = findViewById(R.id.tvNotification);
        tvCompanyName = findViewById(R.id.tvCompanyName);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvUserName = findViewById(R.id.tvUserName);

        tvSSID = findViewById(R.id.tvSSID);
        tvIp = findViewById(R.id.tvIp);
        btnSpeak = findViewById(R.id.btnSpeak);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnSpeak.setOnClickListener(onClickListener);
        btnSpeaker.setOnClickListener(onClickListener);

        findViewById(R.id.llPersonalInformation).setOnClickListener(onClickListener);
        findViewById(R.id.llLanguageSettings).setOnClickListener(onClickListener);
        findViewById(R.id.llVoiceServer).setOnClickListener(onClickListener);
        findViewById(R.id.llMessageServer).setOnClickListener(onClickListener);
        findViewById(R.id.llWifiSetting).setOnClickListener(onClickListener);
        findViewById(R.id.llVersion).setOnClickListener(onClickListener);
        findViewById(R.id.llUpdate).setOnClickListener(onClickListener);
        findViewById(R.id.llShare).setOnClickListener(onClickListener);
        findViewById(R.id.ll_change_account).setOnClickListener(onClickListener);
        findViewById(R.id.ll_sign_out).setOnClickListener(onClickListener);
    }

    /**
     * ViewPager适配器
     */
    private FragmentStatePagerAdapter viewPagerAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
        @Override
        public int getCount() {
            return menus.size();
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                // 联系人
                case 0:
                    return new ContactsFragment();
                // 聊天室
                case 1:
                    return new ChatRoomFragment();
                // 异常信息
                case 2:
                    return new MalfunctionFragment();
                default:
                    break;
            }
            return null;
        }
    };

    private DrawerLayout.DrawerListener drawerListener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            llMain.scrollTo(-(int) (navigation.getWidth() * slideOffset), 0);
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {

        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {

        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }
    };

    private void initBroadcastReceiver() {
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("RECEIVE_MALFUNCTION");
        intentFilter.addAction("CURRENT_PLAYING");
        intentFilter.addAction("NO_LONGER_PLAYING");
        intentFilter.addAction("KEY_DOWN");
        intentFilter.addAction("KEY_UP");
        intentFilter.addAction("WIFI_CONNECTED");
        intentFilter.addAction("WIFI_DISCONNECTED");
        intentFilter.addAction("MESSAGE_WEBSOCKET_CLOSED");
        intentFilter.addAction("UPDATE_CONTACTS");
        intentFilter.addAction("UPDATE_CONTACTS_ROOM");
        intentFilter.addAction("UPDATE_SPEAK_STATUS");
        intentFilter.addAction("P2P_VOICE_REQUEST_ACCEPT");
        intentFilter.addAction("P2P_VOICE_REQUEST_ERROR");
        intentFilter.addAction("REMOTE_LOGIN");
        mContext.registerReceiver(myReceiver, intentFilter);
    }

    /**
     * 显示或隐藏“打开悬浮窗”的提示
     */
    private void showOrHideNotification() {
        //检查悬浮窗权限（Android8.0及以上需要手动打开悬浮窗权限，否则即使打开通知权限，Toast也不会显示）
        if (FloatWindowManager.getInstance().checkPermission(mContext)) {
            //悬浮窗权限已经打开，检查通知权限
            tvNotification.setText(R.string.NotificationPermission);
            if (NotificationsUtil.isNotificationEnabled(mContext)) {
                llNotification.setVisibility(View.GONE);
            } else {
                llNotification.setVisibility(View.VISIBLE);
            }
        } else {
            //悬浮窗权限未打开
            tvNotification.setText(R.string.FloatWindowPermission);
            llNotification.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return false;
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * onServiceConnected和onServiceDisconnected运行在UI线程中
     */
    private ServiceConnection serviceConnection1 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            WebSocketService.WebSocketServiceBinder binder = (WebSocketService.WebSocketServiceBinder) service;
            webSocketService = binder.getService();
            LogUtils.d(TAG, "绑定Service成功");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            webSocketService = null;
        }
    };

    /**
     * onServiceConnected和onServiceDisconnected运行在UI线程中
     */
    private ServiceConnection serviceConnection2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iVoiceService = IVoiceService.Stub.asInterface(service);
            try {
                iVoiceService.registerCallback(iVoiceCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            LogUtils.d(TAG, "绑定Service成功");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iVoiceService = null;
        }
    };

    /**
     * 被调用的方法运行在Binder线程池中，需要在主线程中更新UI
     */
    private IVoiceCallback iVoiceCallback = new IVoiceCallback.Stub() {

        @Override
        public void onOverMaxTalker(String roomId) {
            runOnUiThread(new Thread(() -> {
                showToast(R.string.OverMaxTalker);
            }));
        }

        @Override
        public void enterRoomSuccess() {
            // 加入房间成功
            runOnUiThread(new Thread(() -> {
                isInRoom = true;
                showToast(getString(R.string.InChatRoom));
                List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
                ChatRoomFragment chatRoomFragment;
                for (Fragment fragment : fragmentList) {
                    if (fragment instanceof ChatRoomFragment) {
                        chatRoomFragment = (ChatRoomFragment) fragment;
                        if (chatRoomFragment.isAdded()) {
                            chatRoomFragment.enterRoom();
                        }
                        break;
                    }
                }
            }));
        }

        @Override
        public void leaveRoomSuccess() {
            LogUtils.d(TAG, "成功离开了聊天室");
            // 离开房间成功
            runOnUiThread(new Thread(() -> {
                // 重置房间按钮
                isInRoom = false;
                // 重置说话按钮
                isSpeaking = false;
                btnSpeak.setBackgroundResource(R.drawable.icon_speak_pressed);
                // 重置扬声器按钮
                isUseSpeaker = false;
                btnSpeaker.setBackgroundResource(R.drawable.icon_speaker_pressed);

                User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);
                user.setInroom(false);
                SPHelper.save("User", GsonUtils.convertJSON(user));

                // 清空房间联系人列表
                LogUtils.d(TAG, "清空房间联系人列表");
                List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
                ChatRoomFragment chatRoomFragment;
                for (Fragment fragment : fragmentList) {
                    if (fragment instanceof ChatRoomFragment) {
                        chatRoomFragment = (ChatRoomFragment) fragment;
                        if (chatRoomFragment.isAdded()) {
                            chatRoomFragment.clearUserList();
                            chatRoomFragment.exitRoom();
                        }
                        break;
                    }
                }

                SPHelper.save("KEY_STATUS_UP", true);
                SPHelper.save("CanPlay", true);
                showToast(getString(R.string.ExitChatRoom));
            }));
        }

        @Override
        public void leaveGroupSuccess() {
            // 离开房间成功
            runOnUiThread(new Thread(() -> {
                // 重置房间按钮
                isInRoom = false;
                // 重置说话按钮
                isSpeaking = false;
                btnSpeak.setBackgroundResource(R.drawable.icon_speak_pressed);
                // 重置扬声器按钮
                isUseSpeaker = false;
                btnSpeaker.setBackgroundResource(R.drawable.icon_speaker_pressed);
                List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
                // 清空房间联系人列表
                ChatRoomFragment chatRoomFragment;
                for (Fragment fragment : fragmentList) {
                    if (fragment instanceof ChatRoomFragment) {
                        chatRoomFragment = (ChatRoomFragment) fragment;
                        if (chatRoomFragment.isAdded()) {
                            chatRoomFragment.clearUserList();
                            chatRoomFragment.exitRoom();
                        }
                        break;
                    }
                }
                // 清空大厅联系人列表
                ContactsFragment contactsFragment;
                for (Fragment fragment : fragmentList) {
                    if (fragment instanceof ContactsFragment) {
                        contactsFragment = (ContactsFragment) fragment;
                        if (contactsFragment.isAdded()) {
                            contactsFragment.clearUserList();
                        }
                        break;
                    }
                }

                SPHelper.save("KEY_STATUS_UP", true);
                showToast(getString(R.string.DisconnectServer));
            }));
        }

        @Override
        public void startRecordSuccess() {
            // 打开麦克风成功
            runOnUiThread(new Thread(() -> {
                isSpeaking = true;
                btnSpeak.setBackgroundResource(R.drawable.icon_speak_normal);
            }));
        }

        @Override
        public void stopRecordSuccess() {
            // 关闭麦克风成功
            runOnUiThread(new Thread(() -> {
                isSpeaking = false;
                btnSpeak.setBackgroundResource(R.drawable.icon_speak_pressed);
            }));
        }

        @Override
        public void useSpeakerSuccess() {
            // 使用扬声器成功
            runOnUiThread(new Thread(() -> {
                isUseSpeaker = true;
                btnSpeaker.setBackgroundResource(R.drawable.icon_speaker_normal);
            }));
        }

        @Override
        public void useEarpieceSuccess() {
            // 使用听筒成功
            runOnUiThread(new Thread(() -> {
                isUseSpeaker = false;
                btnSpeaker.setBackgroundResource(R.drawable.icon_speaker_pressed);
            }));
        }

        @Override
        public void removeUser(String ipAddress, String name) {
            // 发送到主线程更新UI
            runOnUiThread(() -> {
                LogUtils.d(TAG, "移除用户，刷新ContactsFragment联系人列表");
                List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
                ChatRoomFragment chatRoomFragment;
                for (Fragment fragment : fragmentList) {
                    if (fragment instanceof ChatRoomFragment) {
                        chatRoomFragment = (ChatRoomFragment) fragment;
                        if (chatRoomFragment.isAdded()) {
                            chatRoomFragment.removeUser(ipAddress);
                        }
                        break;
                    }
                }
            });
        }
    };

    private View.OnClickListener onClickListener = (view) -> {
        Intent intent;
        switch (view.getId()) {
            case R.id.iv_icon:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.ll_notification:
                intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                break;
            case R.id.fl_a:
            case R.id.fl_b:
            case R.id.fl_c:
                for (int i = 0; i < menus.size(); i++) {
                    menus.get(i).setSelected(false);
                    menus.get(i).setTag(i);
                }
                //设置选择效果
                view.setSelected(true);
                //参数false代表瞬间切换，true表示平滑过渡
                viewPager.setCurrentItem((Integer) view.getTag(), false);
                break;
            case R.id.btnSpeak:
                vibrator.vibrate(50);
                // 打开/关闭麦克风
                if (isSpeaking) {
                    if (!isInRoom) {
                        showToast(getString(R.string.OutChatRoom));
                    }
                    if (iVoiceService != null) {
                        try {
                            iVoiceService.stopRecord();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (!isInRoom) {
                        showToast(getString(R.string.OutChatRoom));
                    }
                    if (iVoiceService != null) {
                        try {
                            iVoiceService.startRecord();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case R.id.btnSpeaker:
                vibrator.vibrate(50);
                // 使用扬声器/听筒
                if (isInRoom) {
                    if (iVoiceService != null) {
                        try {
                            if (isUseSpeaker) {
                                iVoiceService.useEarpiece();
                            } else {
                                iVoiceService.useSpeaker();
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    showToast(getString(R.string.OutChatRoom));
                }
                break;
            case R.id.iv_userIcon:
                // 头像设置
                showSelectPicturePopupWindow(findViewById(R.id.drawer_layout));
                break;
            case R.id.llPersonalInformation:
                // 个人信息设置
                openActivity(SetPersonalInfoActivity.class);
                break;
            case R.id.llLanguageSettings:
                // 语言设置
                openActivity(SetLanguageActivity.class);
                break;
            case R.id.llVoiceServer:
                // 语音服务器设置
                openActivity(SetVoiceServerActivity.class);
                break;
            case R.id.llMessageServer:
                // 消息服务器设置
                openActivity(SetMessageServerActivity.class);
                break;
            case R.id.llWifiSetting:
                // 无线网设置
                Intent wifiSettingsIntent = new Intent("android.settings.WIFI_SETTINGS");
                startActivity(wifiSettingsIntent);
                break;
            case R.id.llVersion:
                // 版本查看
                openActivity(VersionsActivity.class);
                break;
            case R.id.llUpdate:
                // 版本更新
                checkNewVersion();
                break;
            case R.id.llShare:
                // 版本分享
                openActivity(QrCodeShareActivity.class);
                break;
            case R.id.ll_change_account:
                // 切换账号
                // 关闭相关服务
                stopService();
                Intent intent1 = new Intent(MainActivity.this, LoginRegisterActivity.class);
                intent1.setAction("SwitchAccount");
                startActivity(intent1);
                ActivityController.finishActivity(this);
                overridePendingTransition(R.anim.left_in, R.anim.right_out);
                break;
            case R.id.ll_sign_out:
                // 标记为用户正常退出房间
                SPHelper.save("NormalExit", true);
                // 退出程序
                ActivityController.exit(this);
                break;
            default:
                break;
        }
    };

    /**
     * 给别人打电话
     *
     * @param userId 用户ID
     */
    public void callOthers(String userId) {
        if (iVoiceService != null) {
            if (!isInRoom) {
                try {
                    iVoiceService.callOthers(userId);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                showToast(R.string.ExitCurrentRoom);
            }
        } else {
            showToast(R.string.ServerDisconnected);
        }
    }

    /**
     * 聊天页面按钮点击事件
     */
    public void clickEnterExitBtn(String roomId) {
        vibrator.vibrate(50);
        // 进入/退出聊天
        if (isInRoom) {
            // 标记为用户正常退出房间
            SPHelper.save("NormalExit", true);
            if (iVoiceService != null) {
                try {
                    iVoiceService.leaveRoom();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (!WifiUtil.WifiConnected(mContext)) {
                showToast(getString(R.string.please_connect_wifi));
            } else {
                if (iVoiceService != null) {
                    try {
                        if (roomId.equals("")) {
                            roomId = NetWork.WEBRTC_SERVER_ROOM;
                        }
                        iVoiceService.enterRoom(roomId);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class MyReceiver extends BaseBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            super.onReceive(context, intent);
            if (intent != null && intent.getAction() != null) {
                List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
                switch (intent.getAction()) {
                    case "RECEIVE_MALFUNCTION": {
                        // 收到异常信息
                        viewPager.setCurrentItem(2);
                        WebSocketData webSocketData = (WebSocketData) intent.getSerializableExtra("data");
                        MalfunctionFragment malfunctionFragment;
                        for (Fragment fragment : fragmentList) {
                            if (fragment instanceof MalfunctionFragment) {
                                malfunctionFragment = (MalfunctionFragment) fragment;
                                if (malfunctionFragment.isAdded()) {
                                    malfunctionFragment.receiveMalfunction(webSocketData);
                                }
                                break;
                            }
                        }
                    }
                    break;
                    case "CURRENT_PLAYING": {
                        int listNo = intent.getIntExtra("number", -1);
                        MalfunctionFragment malfunctionFragment;
                        for (Fragment fragment : fragmentList) {
                            if (fragment instanceof MalfunctionFragment) {
                                malfunctionFragment = (MalfunctionFragment) fragment;
                                if (malfunctionFragment.isAdded()) {
                                    malfunctionFragment.setCurrentPlaying(listNo);
                                }
                                break;
                            }
                        }
                    }
                    break;
                    case "NO_LONGER_PLAYING": {
                        int listNo = intent.getIntExtra("number", -1);
                        MalfunctionFragment malfunctionFragment;
                        for (Fragment fragment : fragmentList) {
                            if (fragment instanceof MalfunctionFragment) {
                                malfunctionFragment = (MalfunctionFragment) fragment;
                                if (malfunctionFragment.isAdded()) {
                                    if (listNo != -1) {
                                        // 不再播放某一条异常信息
                                        malfunctionFragment.refreshMalfunction(listNo, false, true);
                                    }
                                }
                                break;
                            }
                        }
                    }
                    break;
                    case "KEY_DOWN":
                    case "KEY_UP":
                        vibrator.vibrate(50);
                        if (!isInRoom) {
                            MainActivity.this.showToast(getString(R.string.OutChatRoom));
                        }
                        break;
                    case "WIFI_DISCONNECTED":
                        // 清除TextView内容，弹出Dialog
                        tvIp.setText("");
                        tvSSID.setText("");
                        break;
                    case "WIFI_CONNECTED":
                        // 取消显示Dialog
                        // TextView显示网络信息
                        if (WifiUtil.WifiConnected(mContext)) {
                            tvSSID.setText(WifiUtil.getSSID(mContext));
                            tvIp.setText(WifiUtil.getLocalIPAddress());
                        }
                        break;
                    case "MESSAGE_WEBSOCKET_CLOSED": {
                        // 异常信息推送的WebSocket断开了，清空列表
                        MalfunctionFragment malfunctionFragment;
                        for (Fragment fragment : fragmentList) {
                            if (fragment instanceof MalfunctionFragment) {
                                malfunctionFragment = (MalfunctionFragment) fragment;
                                if (malfunctionFragment.isAdded()) {
                                    malfunctionFragment.clearMalfunctionList();
                                }
                                break;
                            }
                        }
                    }
                    break;
                    case "UPDATE_CONTACTS_ROOM": {
                        // 刷新联系人列表
                        LogUtils.d(TAG, "刷新ChatRoomFragment联系人列表");
                        ChatRoomFragment chatRoomFragment;
                        for (Fragment fragment : fragmentList) {
                            if (fragment instanceof ChatRoomFragment) {
                                chatRoomFragment = (ChatRoomFragment) fragment;
                                if (chatRoomFragment.isAdded()) {
                                    ArrayList<User> users = intent.getParcelableArrayListExtra("userList");
                                    for (int i = 0; i < users.size(); i++) {
                                        LogUtils.d(TAG, "联系人数量：" + users.size() + "," + users.get(i).getUser_id());
                                    }
                                    chatRoomFragment.refreshList(users);
                                }
                                break;
                            }
                        }
                        // 更改手机音量键调节的音量类型
                        int streamType = intent.getIntExtra("VolumeControlStream", AudioManager.STREAM_RING);
                        LogUtils.d(TAG, "修改当前调节的音量类型为：" + streamType);
                        setVolumeControlStream(streamType);
                    }
                    break;
                    case "UPDATE_SPEAK_STATUS": {
                        // 刷新联系人列表
                        if (isInRoom) {
                            LogUtils.d(TAG, "刷新ChatRoomFragment联系人列表");
                            ChatRoomFragment chatRoomFragment;
                            for (Fragment fragment : fragmentList) {
                                if (fragment instanceof ChatRoomFragment) {
                                    chatRoomFragment = (ChatRoomFragment) fragment;
                                    if (chatRoomFragment.isAdded()) {
                                        ArrayList<User> users = intent.getParcelableArrayListExtra("userList");
                                        for (int i = 0; i < users.size(); i++) {
                                            LogUtils.d(TAG, "联系人数量：" + users.size() + "," + users.get(i).getUser_id());
                                        }
                                        chatRoomFragment.refreshList(users);
                                    }
                                    break;
                                }
                            }
                            // 更改手机音量键调节的音量类型
                            int streamType = intent.getIntExtra("VolumeControlStream", AudioManager.STREAM_RING);
                            LogUtils.d(TAG, "修改当前调节的音量类型为：" + streamType);
                            setVolumeControlStream(streamType);
                        }
                    }
                    break;
                    case "UPDATE_CONTACTS": {
                        // 刷新联系人列表
                        LogUtils.d(TAG, "刷新ContactsFragment联系人列表");
                        ContactsFragment contactsFragment;
                        for (Fragment fragment : fragmentList) {
                            if (fragment instanceof ContactsFragment) {
                                contactsFragment = (ContactsFragment) fragment;
                                if (contactsFragment.isAdded()) {
                                    ArrayList<User> users = intent.getParcelableArrayListExtra("userList");
                                    for (int i = 0; i < users.size(); i++) {
                                        LogUtils.d(TAG, "联系人数量：" + users.size() + "," + users.get(i).getUser_id());
                                    }
                                    contactsFragment.refreshList(users);
                                }
                                break;
                            }
                        }
                    }
                    break;
                    case "P2P_VOICE_REQUEST_ACCEPT":
                        // 一对一通话接受请求，Fragment定位到聊天室
                        isInRoom = true;
                        viewPager.setCurrentItem(1, false);
                        ChatRoomFragment chatRoomFragment;
                        for (Fragment fragment : fragmentList) {
                            if (fragment instanceof ChatRoomFragment) {
                                chatRoomFragment = (ChatRoomFragment) fragment;
                                if (chatRoomFragment.isAdded()) {
                                    chatRoomFragment.setRoomId(intent.getStringExtra("roomId"));
                                }
                                break;
                            }
                        }
                        break;
                    case "P2P_VOICE_REQUEST_ERROR":
                        // 一对一通话请求，对方不在线或者忙线
                        switch (intent.getStringExtra("errorMsg")) {
                            case "ERROR_BUSY":
                                MainActivity.this.showToast(getString(R.string.TargetBusy));
                                break;
                            case "ERROR_OFFLINE":
                                MainActivity.this.showToast(R.string.TargetOffline);
                                break;
                            default:
                                break;
                        }
                        break;
                    case "REMOTE_LOGIN":
                        // 账号在异地登录
                        // 标记为用户正常退出房间
                        SPHelper.save("NormalExit", true);
                        // 关闭相关服务
                        stopService();
                        Intent intent1 = new Intent(MainActivity.this, LoginRegisterActivity.class);
                        intent1.setAction("RemoteLogin");
                        startActivity(intent1);
                        ActivityController.finishActivity(MainActivity.this);
                        overridePendingTransition(R.anim.left_in, R.anim.right_out);
                        break;
                    default:
                        break;
                }
            }
        }

    }

    protected void showSelectPicturePopupWindow(View parent) {
        mSelectPicturePopupWindow.showPopupWindow(parent);
    }

    /**
     * 设置图片选择的回调监听
     *
     * @param l 监听器
     */
    public void setOnPictureSelectedListener(OnPictureSelectedListener l) {
        this.mOnPictureSelectedListener = l;
    }

    @Override
    public void onSelected(View v, int position) {
        switch (position) {
            case 0:
                // "拍照"按钮被点击了
                takePhoto();
                break;
            case 1:
                // "从相册选择"按钮被点击了
                pickFromGallery();
                break;
            case 2:
                // "取消"按钮被点击了
                mSelectPicturePopupWindow.dismissPopupWindow();
                break;
            default:
                break;
        }
    }

    /**
     * 打开相机拍照
     */
    private void takePhoto() {
        // Permission was added in API Level 16
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    getString(R.string.permission_write_storage_rationale),
                    REQUEST_STORAGE_WRITE_ACCESS_PERMISSION);
        } else {
            mSelectPicturePopupWindow.dismissPopupWindow();
            Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //下面这句指定调用相机拍照后的照片存储的路径
            takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mTempPhotoPath)));
            startActivityForResult(takeIntent, CAMERA_REQUEST_CODE);
            overridePendingTransition(R.anim.right_in, R.anim.left_out);
        }
    }

    /**
     * 从相册选择照片
     */
    private void pickFromGallery() {
        // Permission was added in API Level 16
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    getString(R.string.permission_read_storage_rationale),
                    REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
            mSelectPicturePopupWindow.dismissPopupWindow();
            Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
            // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型"
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            startActivityForResult(pickIntent, GALLERY_REQUEST_CODE);
            overridePendingTransition(R.anim.right_in, R.anim.left_out);
        }
    }

    /**
     * 图片裁剪完成的监听
     */
    private OnPictureSelectedListener onPictureSelectedListener = (fileUri, bitmap) -> {
        String filePath = fileUri.getEncodedPath();
        String imagePath = Uri.decode(filePath);
        LogUtils.d(TAG, "imagePath:" + imagePath);
        SPHelper.save("UserIconPath", imagePath);
        String time = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)).format(new Date());
        // 以“.webp”格式作为图片扩展名
        String type = "webp";
        // 将本软件的包路径 + 文件名拼接成图片绝对路径
        User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);
        String newFile = getExternalFilesDir("Icons") + "/" + user.getUser_id() + "_" + time + "." + type;
        BitmapUtils.compressPicture(imagePath, newFile);
        uploadUserIcon(new File(newFile));
    };

    /**
     * 上传或更新头像
     *
     * @param file 头像文件
     */
    private void uploadUserIcon(File file) {
        User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);
        RequestBody description = RequestBody.create(MediaType.parse("text/plain"), user.getUser_id());
        // 创建 RequestBody，用于封装构建RequestBody
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        // MultipartBody.Part  和后端约定好Key，这里的partName是用image
        MultipartBody.Part body = MultipartBody.Part.createFormData("uploadfile", file.getName(), requestFile);
        // 执行请求
        Observable<NormalResult> normalResultObservable = NetClient.getInstances(NetClient.getBaseUrlProject()).getNjMeterApi().uploadUserIcon(description, body);
        normalResultObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new NetworkSubscriber<NormalResult>(mContext, getClass().getSimpleName()) {

            @Override
            public void onStart() {
                super.onStart();
                //接下来可以检查网络连接等操作
                if (!NetworkUtil.isNetworkAvailable(mContext)) {
                    showToast(getString(R.string.NetworkUnavailable));
                    if (!isUnsubscribed()) {
                        unsubscribe();
                    }
                } else {
                    showLoadingDialog(mContext, getString(R.string.updating), true);
                }
            }

            @Override
            public void onError(ExceptionHandle.ResponseThrowable responseThrowable) {
                cancelDialog();
                showToast(responseThrowable.message);
            }

            @Override
            public void onNext(NormalResult normalResult) {
                cancelDialog();
                if (normalResult == null) {
                    showToast("头像更新失败");
                } else {
                    String result = normalResult.getResult();
                    String photoPath = ("http://" + NetWork.SERVER_HOST_MAIN + ":" + NetWork.SERVER_PORT_MAIN + "/" + normalResult.getMessage()).replace("\\", "/");
                    // 更新存储的User对象
                    User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);
                    user.setIcon_url(normalResult.getMessage());
                    SPHelper.save("User", GsonUtils.convertJSON(user));
                    switch (result) {
                        case Constants.SUCCESS:
                            showToast(R.string.UploadSuccess);
                            showUserIcon(photoPath);
                            break;
                        case Constants.FAIL:
                            showToast(R.string.ServerError);
                            break;
                        default:
                            showToast(R.string.UnknownError);
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CAMERA_REQUEST_CODE:
                    // 调用相机拍照
                    File temp = new File(mTempPhotoPath);
                    startCropActivity(Uri.fromFile(temp));
                    overridePendingTransition(R.anim.left_in, R.anim.right_out);
                    break;
                case GALLERY_REQUEST_CODE:
                    // 直接从相册获取
                    startCropActivity(data.getData());
                    overridePendingTransition(R.anim.left_in, R.anim.right_out);
                    break;
                case UCrop.REQUEST_CROP:
                    // 裁剪图片结果
                    handleCropResult(data);
                    break;
                case UCrop.RESULT_ERROR:
                    // 裁剪图片错误
                    handleCropError(data);
                    break;
                case GET_UNKNOWN_APP_SOURCES:
                    // 从安装未知来源文件的设置页面返回
                    File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), versionFileName);
                    checkIsAndroidO(file);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 裁剪图片方法实现
     *
     * @param uri 图片路径
     */
    public void startCropActivity(Uri uri) {
        UCrop.of(uri, mDestinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(512, 512)
                .withTargetActivity(CropActivity.class)
                .start(this);
    }

    /**
     * 处理剪切成功的返回值
     *
     * @param result 返回值
     */
    private void handleCropResult(Intent result) {
        deleteTempPhotoFile();
        final Uri resultUri = UCrop.getOutput(result);
        if (null != resultUri && null != mOnPictureSelectedListener) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mOnPictureSelectedListener.onPictureSelected(resultUri, bitmap);
            overridePendingTransition(R.anim.left_in, R.anim.right_out);
        } else {
            showToast(R.string.CutError);
        }
    }

    /**
     * 处理剪切失败的返回值
     *
     * @param result 返回值
     */
    private void handleCropError(Intent result) {
        deleteTempPhotoFile();
        Throwable cropError = UCrop.getError(result);
        if (cropError != null) {
            showToast(cropError.getMessage());
        } else {
            showToast(R.string.CutError);
        }
    }

    /**
     * 删除拍照临时文件
     */
    private void deleteTempPhotoFile() {
        File tempFile = new File(mTempPhotoPath);
        if (tempFile.exists() && tempFile.isFile()) {
            boolean deleteResult = tempFile.delete();
            if (deleteResult) {
                LogUtils.d("文件删除成功");
            }
        }
    }

    /**
     * 请求权限
     * 如果权限被拒绝过，则提示用户需要权限
     */
    protected void requestPermission(String permission, String rationale, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(permission)) {
                showAlertDialog(getString(R.string.permission_title_rationale), rationale, (dialog, which) -> requestPermissions(new String[]{permission}, requestCode), getString(R.string.label_ok));
            } else {
                requestPermissions(new String[]{permission}, requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            LogUtils.d(TAG, "[Permission] " + permissions[i] + " is " + (grantResults[i] == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                finish();
                break;
            }
        }
        switch (requestCode) {
            case REQUEST_STORAGE_READ_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery();
                }
                break;
            case REQUEST_STORAGE_WRITE_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto();
                }
                break;
            case INSTALL_PACKAGES_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), versionFileName);
                    installApk(file);
                } else {
                    //  Android8.0以上引导用户手动开启安装权限
                    if (Build.VERSION.SDK_INT >= 26) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                        startActivityForResult(intent, GET_UNKNOWN_APP_SOURCES);
                    }
                }
                break;
            default:
                break;
        }
    }

    protected void showAlertDialog(String title, String message, DialogInterface.OnClickListener onPositiveButtonClickListener, String positiveText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText, onPositiveButtonClickListener);
        builder.show();
    }

    /**
     * 关闭服务
     */
    private void stopService() {
        if (webSocketService != null) {
            try {
                unbindService(serviceConnection1);
                webSocketService.stopSelf();
            } catch (Exception e) {
                e.printStackTrace();
            }
            webSocketService = null;
        }
        if (iVoiceService != null) {
            try {
                iVoiceService.stopRecord();
                iVoiceService.leaveGroup();
                iVoiceService.unRegisterCallback(iVoiceCallback);
                unbindService(serviceConnection2);
                iVoiceService.stopSelf();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            iVoiceService = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService();
        if (myReceiver != null) {
            mContext.unregisterReceiver(myReceiver);
        }
    }

}
