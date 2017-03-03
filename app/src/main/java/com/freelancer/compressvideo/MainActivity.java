package com.freelancer.compressvideo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AppCompatButton mBtnCompressVideo;
    private AppCompatTextView mTvLog;
    private FloatingActionButton mFabPickFile;
    private ScrollView mScrollView;

    private Compressor mCompressor;
    private static final Handler handler = new Handler();
    private Context mContext;

    private String mInputVideoPath = "/mnt/sdcard/main.mp4";
    private String mOutputVideoPath = "/mnt/sdcard/out.mp4";
    //    private String cmd = "-y -i " + mInputVideoPath + " -s 480x320 -r 20 -c:v libx264 -preset ultrafast -c:a copy -me_method zero -tune fastdecode -tune zerolatency -strict -2 -b:v 1000k -pix_fmt yuv420p " + mOutputVideoPath;
    private String cmd[] = {"-y", "-i", mInputVideoPath, "-s", "480x320", "-r", "20", "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "copy", "-me_method", "zero", "-tune", "fastdecode", "-tune", "zerolatency", "-strict", "-2", "-b:v", "1000k", "-pix_fmt", "yuv420p", mOutputVideoPath};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        mCompressor = new Compressor(this);
        mContext = this;
        mCompressor.loadBinary(new CompressListener() {
            @Override
            public void onLoadSuccess() {
                textAppend(getString(R.string.compress_load_library_succeed));
            }

            @Override
            public void onLoadFail(String reason) {
                textAppend(getString(R.string.compress_load_library_failed));
            }
        });
    }

    private void initViews() {
        mBtnCompressVideo = (AppCompatButton) findViewById(R.id.btn_compress_video);
        mTvLog = (AppCompatTextView) findViewById(R.id.tv_log);
        mFabPickFile = (FloatingActionButton) findViewById(R.id.fab_pick_file);
        mScrollView = (ScrollView) findViewById(R.id.scrollView);

        mBtnCompressVideo.setOnClickListener(this);
        mFabPickFile.setOnClickListener(this);
    }

    private void textAppend(String text) {
        if (!TextUtils.isEmpty(text)) {
            mTvLog.append(text + "\n");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_compress_video:
                if (TextUtils.isEmpty(mInputVideoPath)) {
                    Toast.makeText(MainActivity.this, R.string.no_video_tips, Toast.LENGTH_SHORT).show();
                }
                execCommand(cmd);
                break;
            case R.id.fab_pick_file:
                break;
        }
    }

    private void execCommand(String[] cmd) {
        File mFile = new File(mOutputVideoPath);
        if (mFile.exists()) {
            mFile.delete();
        }
        mCompressor.execCommand(cmd, new CompressListenerCallback() {
            @Override
            public void onExecSuccess(String message) {
                textAppend(getString(R.string.compress_succeed));
                Toast.makeText(getApplicationContext(), R.string.compress_succeed, Toast.LENGTH_SHORT).show();
                String result = getString(R.string.compress_result_input_output, mInputVideoPath
                        , getFileSize(mInputVideoPath), mOutputVideoPath, getFileSize(mOutputVideoPath));
                textAppend(result);

                new AlertDialog.Builder(mContext)
                        .setTitle(getString(R.string.compress_succeed))
                        .setMessage(result)
                        .setPositiveButton(getString(R.string.confirm), (dialogInterface, i) -> {
                            openFile(new File(mOutputVideoPath));
                            dialogInterface.dismiss();
                        }).setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                        .show();
            }

            @Override
            public void onExecFail(String reason) {
                textAppend(getString(R.string.compress_failed, reason));
                new AlertDialog.Builder(mContext)
                        .setTitle(getString(R.string.compress_failed))
                        .setMessage(getString(R.string.compress_failed))
                        .setPositiveButton(getString(R.string.confirm), (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                        })
                        .show();
            }

            @Override
            public void onExecProgress(String message) {
                textAppend(getString(R.string.compress_progress, message));
            }
        });
    }

    private String getFileSize(String path) {
        File f = new File(path);
        if (!f.exists()) {
            return "0 MB";
        } else {
            long size = f.length();
            return (size / 1024f) / 1024f + "MB";
        }
    }

    private void openFile(File file) {
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            String type = getMIMEType(file);
            intent.setDataAndType(Uri.fromFile(file), type);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, R.string.dont_have_app_to_open_file, Toast.LENGTH_SHORT).show();
        }
    }

    private String getMIMEType(File file) {
        String type = "*/*";
        String fName = file.getName();
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (end == "") return type;
        for (int i = 0; i < MIME_MapTable.length; i++) {
            if (end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    private final String[][] MIME_MapTable = {
            {".3gp", "video/3gpp"},
            {".apk", "application/vnd.android.package-archive"},
            {".asf", "video/x-ms-asf"},
            {".avi", "video/x-msvideo"},
            {".bin", "application/octet-stream"},
            {".bmp", "image/bmp"},
            {".c", "text/plain"},
            {".class", "application/octet-stream"},
            {".conf", "text/plain"},
            {".cpp", "text/plain"},
            {".doc", "application/msword"},
            {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls", "application/vnd.ms-excel"},
            {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe", "application/octet-stream"},
            {".gif", "image/gif"},
            {".gtar", "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h", "text/plain"},
            {".htm", "text/html"},
            {".html", "text/html"},
            {".jar", "application/java-archive"},
            {".java", "text/plain"},
            {".jpeg", "image/jpeg"},
            {".jpg", "image/jpeg"},
            {".js", "application/x-javascript"},
            {".log", "text/plain"},
            {".m3u", "audio/x-mpegurl"},
            {".m4a", "audio/mp4a-latm"},
            {".m4b", "audio/mp4a-latm"},
            {".m4p", "audio/mp4a-latm"},
            {".m4u", "video/vnd.mpegurl"},
            {".m4v", "video/x-m4v"},
            {".mov", "video/quicktime"},
            {".mp2", "audio/x-mpeg"},
            {".mp3", "audio/x-mpeg"},
            {".mp4", "video/mp4"},
            {".mpc", "application/vnd.mpohun.certificate"},
            {".mpe", "video/mpeg"},
            {".mpeg", "video/mpeg"},
            {".mpg", "video/mpeg"},
            {".mpg4", "video/mp4"},
            {".mpga", "audio/mpeg"},
            {".msg", "application/vnd.ms-outlook"},
            {".ogg", "audio/ogg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".pps", "application/vnd.ms-powerpoint"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop", "text/plain"},
            {".rc", "text/plain"},
            {".rmvb", "audio/x-pn-realaudio"},
            {".rtf", "application/rtf"},
            {".sh", "text/plain"},
            {".tar", "application/x-tar"},
            {".tgz", "application/x-compressed"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".wmv", "audio/x-ms-wmv"},
            {".wps", "application/vnd.ms-works"},
            {".xml", "text/plain"},
            {".z", "application/x-compress"},
            {".zip", "application/x-zip-compressed"},
            {"", "*/*"}
    };
}
