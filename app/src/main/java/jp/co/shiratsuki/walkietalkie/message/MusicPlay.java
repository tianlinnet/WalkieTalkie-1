package jp.co.shiratsuki.walkietalkie.message;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.LocaleList;

import jp.co.shiratsuki.walkietalkie.bean.Music;
import jp.co.shiratsuki.walkietalkie.bean.MusicList;
import jp.co.shiratsuki.walkietalkie.bean.User;
import jp.co.shiratsuki.walkietalkie.constant.NetWork;
import jp.co.shiratsuki.walkietalkie.contentprovider.SPHelper;
import jp.co.shiratsuki.walkietalkie.utils.GsonUtils;
import jp.co.shiratsuki.walkietalkie.utils.LanguageUtil;
import jp.co.shiratsuki.walkietalkie.utils.LogUtils;
import jp.co.shiratsuki.walkietalkie.utils.NetworkUtil;
import jp.co.shiratsuki.walkietalkie.utils.UrlCheckUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音乐播放工具
 * Created at 2018/11/29 9:53
 *
 * @author LiYuliang
 * @version 1.0
 */

public class MusicPlay {

    private static final String TAG = "MusicPlay";

    private volatile static MusicPlay mVoicePlay;
    private ExecutorService mExecutorService;
    private Context mContext;
    private String serverHost;
    private volatile static List<MusicList> musicListList;
    private boolean flag = true;
    private int interval1 = 1000, interval2 = 3000;

    private MusicPlay(Context context) {
        this.mContext = context;
        this.mExecutorService = Executors.newCachedThreadPool();
        musicListList = Collections.synchronizedList(new ArrayList<>());
        User user = GsonUtils.parseJSON(SPHelper.getString("User", GsonUtils.convertJSON(new User())), User.class);
        if (user.getMessage_ip().equals("") || user.getMessage_port().equals("")) {
            serverHost = NetWork.MESSAGE_SERVER_IP;
        } else {
            serverHost = user.getMessage_ip();
        }
    }

    /**
     * 单例模式（懒汉式）
     *
     * @return MusicPlay对象
     */
    public static MusicPlay with(Context context) {
        if (mVoicePlay == null) {
            mVoicePlay = new MusicPlay(context);
        }
        return mVoicePlay;
    }

    public List<MusicList> getMusicListList() {
        return musicListList;
    }

    /**
     * 播放音乐
     */
    public void play() {
        LogUtils.d(TAG, "播放音乐");
        mExecutorService.execute(() -> start());
    }

    /**
     * 添加需要播放的音乐
     *
     * @param musicList 音乐列表对象
     * @param interval1 列表内间隔
     * @param interval2 音乐列表对象
     */
    public void addMusic(MusicList musicList, int interval1, int interval2) {
        LogUtils.d(TAG, "添加音乐，编号：" + musicList.getListNo());
        if (!musicListList.contains(musicList)) {
            // 不包含了这条异常信息
            LogUtils.d(TAG, "添加音乐，编号：" + musicList.getListNo() + "，列表中不包含该音乐，添加成功");
            musicListList.add(musicList);
            this.interval1 = interval1;
            this.interval2 = interval2;
        } else {
            LogUtils.d(TAG, "添加音乐，编号：" + musicList.getListNo() + "，列表中包含该音乐，添加失败");
        }
    }

    /**
     * 删除不需要播放的音乐（由于多线程操作会出问题，此处并不是真正移除，而是修改它的已播放次数为最大播放次数）
     *
     * @param listNo 异常ID
     */
    public void removeMusic(int listNo) {
        LogUtils.d(TAG, "移除音乐，编号：" + listNo);
        for (int i = 0; i < musicListList.size(); i++) {
            MusicList musicList = musicListList.get(i);
            if (musicList.getListNo() == listNo) {
                musicList.setAlreadyPlayCount(musicList.getPlayCount());
                for (int j = 0; j < musicList.getMusicList().size(); j++) {
                    Music music = musicList.getMusicList().get(j);
                    music.setAlreadyPlayCount(music.getPlayCount());
                }
            }
        }
    }

    /**
     * 开始播放音乐
     */
    private void start() {
        while (flag) {
            if (musicListList != null && musicListList.size() > 0) {
                try {
                    // 如果播放列表不为空且长度大于0
                    for (MusicList music : musicListList) {
                        LogUtils.d(TAG, "播放ID：" + music.getListNo() + ",已经播放次数：" + music.getAlreadyPlayCount());
                    }
                    List<MusicList> delList = new ArrayList<>();
                    for (int i = 0; i < musicListList.size(); i++) {
                        // 如果已经播放次数小于需要播放的次数，或者，需要播放的次数为-1（无限循环播放）且已播放次数（默认为0，除非用户手动取消播放，会设置成需要播放次数）不等于需要播放次数（-1）
                        if (musicListList.get(i).getAlreadyPlayCount() < musicListList.get(i).getPlayCount() ||
                                (musicListList.get(i).getPlayCount() == -1 && (musicListList.get(i).getAlreadyPlayCount() != musicListList.get(i).getPlayCount()))) {
                            playOneList(i);
                            LogUtils.d(TAG, "播放ID：" + musicListList.get(i).getListNo() + ",已经播放次数：" + musicListList.get(i).getAlreadyPlayCount());
                            int alreadyPlayCount = musicListList.get(i).getAlreadyPlayCount();
                            // 如果播放次数不为-1（为-1的话需要无限循环播放），播放次数加1
                            if (musicListList.get(i).getPlayCount() != -1) {
                                alreadyPlayCount = musicListList.get(i).getAlreadyPlayCount() + 1;
                            }
                            musicListList.get(i).setAlreadyPlayCount(alreadyPlayCount);
                            LogUtils.d(TAG, "播放ID：" + musicListList.get(i).getListNo() + ",已经播放次数：" + alreadyPlayCount);
                            // 如果需要播放次数不为-1（无限循环播放），而且已经播放次数大于等于需要播放次数，添加到删除列表中
                            if (musicListList.get(i).getPlayCount() != -1 && alreadyPlayCount >= musicListList.get(i).getPlayCount()) {
                                delList.add(musicListList.get(i));
                                // 通知页面布局更新
                                Intent intent1 = new Intent();
                                intent1.setAction("NO_LONGER_PLAYING");
                                intent1.putExtra("number", musicListList.get(i).getListNo());
                                mContext.sendBroadcast(intent1);
                            }
                            // 如果是列表末尾位置,等待interval2
                            if (i >= musicListList.size() - 1) {
                                // 列表整体循环播放间隔
                                LogUtils.d("Sleep", "睡眠等待interval2");
                                try {
                                    Thread.sleep(interval2);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                // 列表间播放间隔，等待interval1
                                LogUtils.d("Sleep", "睡眠等待interval1");
                                try {
                                    Thread.sleep(interval1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            LogUtils.d(TAG, "不满足播放条件");
                            delList.add(musicListList.get(i));
                            // 通知页面布局更新
                            Intent intent1 = new Intent();
                            intent1.setAction("NO_LONGER_PLAYING");
                            intent1.putExtra("number", musicListList.get(i).getListNo());
                            mContext.sendBroadcast(intent1);
                        }
                    }
                    musicListList.removeAll(delList);
                } catch (Exception e) {
                    LogUtils.d(TAG, "播放音乐发生错误");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 播放一个音乐列表
     *
     * @param position 音乐列表
     */
    private void playOneList(int position) {
        synchronized (MusicPlay.this) {
            MediaPlayer mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // CountDownLatch允许一个或多个线程等待其他线程执行完毕后再运行
            // CountDownLatch的构造函数接收int类型的参数作为计数器，若要等待N个点再执行后续逻辑，就传入N。
            // 这里的N可以是N个线程，也可以是N个执行步骤。
            // 当我们调用countDown( )方法时，N会减一。
            // 调用await( ) 方法来阻塞当前线程，直到N减为0。
            CountDownLatch mCountDownLatch = new CountDownLatch(1);

            mExecutorService.execute(() -> {
                try {
                    final int[] counter = {0};
                    if (musicListList.size() == 0) {
                        return;
                    }
                    List<Music> musicList = musicListList.get(position).getMusicList();
//                if (musicList.get(0).getPlayCount() != musicList.get(0).getAlreadyPlayCount()) {
                    // 通知主页面刷新布局
                    Intent intent = new Intent();
                    intent.setAction("CURRENT_PLAYING");
                    intent.putExtra("number", musicList.get(counter[0]).getListNo());
                    mContext.sendBroadcast(intent);

                    // 根据语言获取音乐路径
                    String filePath = getMusicPath(musicListList.get(position), musicList.get(counter[0]).getFilePath());
                    LogUtils.d(TAG, "检查文件是否存在，文件路径：" + filePath);
                    if (NetworkUtil.isNetworkAvailable(mContext) && UrlCheckUtil.checkUrlExist(filePath)) {
                        try {
//                            mMediaPlayer.reset();
                            mMediaPlayer.setDataSource(filePath);
                            mMediaPlayer.prepareAsync();
                            mMediaPlayer.setScreenOnWhilePlaying(true);
                        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
                            e.printStackTrace();
                        }
                        mMediaPlayer.setOnPreparedListener(mediaPlayer -> mMediaPlayer.start());
                        mMediaPlayer.setOnErrorListener((mediaPlayer, what, extra) -> {
                            // 遇到错误就重置MediaPlayer
                            LogUtils.d(TAG, "媒体文件获取异常，播放失败");
                            mediaPlayer.reset();
                            return false;
                        });
                        mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                            // 如果播放次数达到上限，则通知页面布局更新
                            if (musicList.get(counter[0]).getPlayCount() != -1 &&
                                    musicList.get(counter[0]).getAlreadyPlayCount() >= musicList.get(counter[0]).getPlayCount()) {
                                try {
                                    Intent intent1 = new Intent();
                                    intent1.setAction("NO_LONGER_PLAYING");
                                    intent1.putExtra("number", musicListList.get(counter[0]).getListNo());
                                    mContext.sendBroadcast(intent1);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            mediaPlayer.reset();
                            counter[0]++;
                            if (musicListList.size() == 0) {
                                return;
                            }
                            List<Music> musicList1 = musicListList.get(position).getMusicList();

//                    if (counter[0] < musicList1.size() && musicList.get(0).getPlayCount() != musicList.get(0).getAlreadyPlayCount()) {
                            if (counter[0] < musicList1.size()) {
                                String filePath1 = getMusicPath(musicListList.get(position), musicList.get(counter[0]).getFilePath());
                                if (NetworkUtil.isNetworkAvailable(mContext) && UrlCheckUtil.checkUrlExist(filePath1)) {
                                    try {
                                        mediaPlayer.setDataSource(filePath1);
                                        mediaPlayer.prepareAsync();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        mCountDownLatch.countDown();
                                    }
                                } else {
                                    mediaPlayer.release();
                                    mCountDownLatch.countDown();
                                }
                            } else {
                                mediaPlayer.release();
                                mCountDownLatch.countDown();
                            }
                        });
                    } else {
                        // 如果网络未连接或者音乐链接不存在
                        mCountDownLatch.countDown();
                    }

//                }
                } catch (Exception e) {
                    e.printStackTrace();
                    mCountDownLatch.countDown();
                }
            });

            try {
                mCountDownLatch.await();
                notifyAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 播放完毕通知主页面刷新布局
            Intent intent1 = new Intent();
            intent1.setAction("CURRENT_PLAYING");
            intent1.putExtra("number", -1);
            mContext.sendBroadcast(intent1);
        }
    }

    /**
     * 获取文件真正的路径
     *
     * @param musicList 音乐列表对象
     * @param fileName  音乐文件名
     * @return 音乐文件的全路径
     */
    private String getMusicPath(MusicList musicList, String fileName) {
        String directory = "";
        switch (LanguageUtil.getLanguageLocal(mContext)) {
            case "":
                // 手机设置的语言是跟随系统
                Locale locale;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    locale = LocaleList.getDefault().get(0);
                } else {
                    locale = Locale.getDefault();
                }
                String language = locale.getLanguage();
                switch (language) {
                    case "zh":
                        directory = musicList.getChineseFolder();
                        break;
                    case "ja":
                        directory = musicList.getJapaneseFolder();
                        break;
                    default:
                        directory = musicList.getEnglishFolder();
                        break;
                }
                break;
            case "zh":
                directory = musicList.getChineseFolder();
                break;
            case "ja":
                directory = musicList.getJapaneseFolder();
                break;
            case "en":
                directory = musicList.getEnglishFolder();
                break;
            default:
                break;
        }
        return "http://" + serverHost + "/" + directory + "/" + fileName;
    }

    public void release() {
        flag = false;
        mExecutorService.shutdown();
        mVoicePlay = null;
    }
}