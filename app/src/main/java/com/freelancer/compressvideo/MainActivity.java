package com.freelancer.compressvideo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import com.freelancer.compressvideo.compressor.CompressListener;
import com.freelancer.compressvideo.compressor.CompressListenerCallback;
import com.freelancer.compressvideo.compressor.Compressor;
import com.freelancer.compressvideo.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int REQUEST_CODE_INPUT_VIDEO_PATH = 1;

    private AppCompatButton mBtnCompressVideo;
    private AppCompatTextView mTvLog;
    private FloatingActionButton mFabPickFile;
    private ScrollView mScrollView;
    private EditText mEdSize;

    private Compressor mCompressor;
    private static final Handler handler = new Handler();
    private Context mContext;

    private String mInputVideoPath = "";
    private String mOutputVideoPath = "/mnt/sdcard/out.mp4";
    //    private String cmd = "-y -i " + mInputVideoPath + " -s 480x320 -r 20 -c:v libx264 -preset ultrafast -c:a copy -me_method zero -tune fastdecode -tune zerolatency -strict -2 -b:v 1000k -pix_fmt yuv420p " + mOutputVideoPath;
    //    private String cmd = ffmpeg -i input_file.avi -codec:v libx264 -profile: high -preset slow -b:v 500k -maxrate 500k -bufsize 1000k -vf scale=-1:480 -threads 0 -codec:a libfdk_aac -b:a 128k output_file.mp4
//    private String cmd[] = {"-y", "-i", mInputVideoPath, "-r", "20", "-c:v", "libx264", "-preset", "ultrafast", "-profile:v", "high", "-c:a", "copy", "-me_method", "zero", "-tune", "fastdecode", "-tune", "zerolatency", "-strict", "-2", "-b:v", "1000k", "-threads", "4", "-pix_fmt", "yuv420p", mOutputVideoPath};
//    private String cmd[] = {"-i", mInputVideoPath, "-vcodec", "libx264", "-crf", "20", mOutputVideoPath};
//    private String cmd[] = {"-i", mInputVideoPath, "-c:v", "libx264", "-crf", "24", "-c:a", "aac", mOutputVideoPath};
//    private String cmd[] = {"-y", "-i", mInputVideoPath,"-s","640x480", "-r", "20", "-c:v", "libx264", "-preset", "ultrafast", "-profile:v", "high", "-c:a", "copy", "-me_method", "zero", "-tune", "fastdecode", "-tune", "zerolatency", "-strict", "-2", "-b:v", "1000k", "-threads", "4", "-pix_fmt", "yuv420p", mOutputVideoPath};

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
        mEdSize = (EditText) findViewById(R.id.ed_size);

        mBtnCompressVideo.setOnClickListener(this);
        mFabPickFile.setOnClickListener(this);
    }

    private void textAppend(String text) {
        if (!TextUtils.isEmpty(text)) {
            mTvLog.append(text + "\n");
            handler.post(() -> mScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) return;
        switch (requestCode) {
            case REQUEST_CODE_INPUT_VIDEO_PATH:
                Uri selectedImage = data.getData();
                mInputVideoPath = FileUtils.getPath(mContext, selectedImage);
                break;
        }
    }

    private int getDuration() {
        MediaPlayer mp = MediaPlayer.create(this, Uri.parse(mInputVideoPath));
        return mp.getDuration();
    }

    public void compressVideo() {
        int duration = getDuration();
        int bitrate = calBitrate(duration / 1000, Integer.parseInt(mEdSize.getText().toString().trim()));

        ArrayList<String> paras = new ArrayList<>();

        paras.add("-i");
        paras.add(mInputVideoPath);

        paras.add("-vcodec");
        paras.add("libx264");

        paras.add("-strict");
        paras.add("-2");

        paras.add("-acodec");
        paras.add("aac");

        paras.add("-s");
        paras.add("640x360");

        paras.add("-preset");
        paras.add("superfast");

        paras.add("-r");
        paras.add("24");

        paras.add("-b:v");
        paras.add(bitrate + "");

        paras.add("-b:a");
        paras.add("64K");

        paras.add("-maxrate");
        paras.add(bitrate + "");

        paras.add("-minrate");
        paras.add(bitrate + "");

        paras.add("-bufsize");
        paras.add(bitrate * 3 + "");

        paras.add(mOutputVideoPath);
        execCommand(paras.toArray(new String[paras.size()]));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_compress_video:
                compressVideo();
                break;
            case R.id.fab_pick_file:
                Intent mediaChooser = new Intent(Intent.ACTION_GET_CONTENT);
                mediaChooser.setType("video/*");
                startActivityForResult(mediaChooser, REQUEST_CODE_INPUT_VIDEO_PATH);
                break;
        }
    }

    private int calBitrate(int duration, int targetSize) {
        targetSize = targetSize * 1024 * 1024;
        int audiosize = (120 * 1000 / 8) * duration;
        return (targetSize - audiosize) * 8 / duration;
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
                        , FileUtils.getFileSize(mInputVideoPath), mOutputVideoPath, FileUtils.getFileSize(mOutputVideoPath));
                textAppend(result);

                new AlertDialog.Builder(mContext)
                        .setTitle(getString(R.string.compress_succeed))
                        .setMessage(result)
                        .setPositiveButton(getString(R.string.confirm), (dialogInterface, i) -> {
                            FileUtils.openFile(new File(mOutputVideoPath), mContext);
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
}
