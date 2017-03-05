package com.freelancer.compressvideo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
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
import com.freelancer.compressvideo.databinding.ActivityMainBinding;
import com.freelancer.compressvideo.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_INPUT_VIDEO_PATH = 1;

    private Compressor mCompressor;
    private Handler handler = new Handler();
    private Context mContext;

    private String mInputVideoPath = "";
    private String mOutputVideoPath = "";

    private ActivityMainBinding mBinding;
    public static ObservableBoolean isCompress = new ObservableBoolean();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setMain(this);


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

    public void onCompressVideo() {
        if (isCompress.get()) {
            Toast.makeText(getApplicationContext(), R.string.compressing, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mBinding.getSize() == null) {
            Toast.makeText(getApplicationContext(), R.string.compree_please_input_size, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(mInputVideoPath)) {
            Toast.makeText(getApplicationContext(), R.string.no_video_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        isCompress.set(true);
        compressVideo();
    }

    public void onPickFile() {
        Intent mediaChooser = new Intent(Intent.ACTION_GET_CONTENT);
        mediaChooser.setType("video/*");
        startActivityForResult(mediaChooser, REQUEST_CODE_INPUT_VIDEO_PATH);
    }

    private void textAppend(String text) {
        if (!TextUtils.isEmpty(text)) {
            mBinding.tvLog.append(text + "\n");
            handler.post(() -> mBinding.scrollView.fullScroll(ScrollView.FOCUS_DOWN));
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
                mBinding.setInput(mInputVideoPath);
                Pattern p = Pattern.compile(".*" + File.separator + "(.*)\\..*");
                Matcher m = p.matcher(mInputVideoPath);
                if (m.matches()) {
                    mOutputVideoPath="";
                    mOutputVideoPath = "/mnt/sdcard/" + m.group(1) + "_out.mp4";
                    mBinding.setOutput(mOutputVideoPath);
                }
                break;
        }
    }

    private int getDuration() {
        MediaPlayer mp = MediaPlayer.create(this, Uri.parse(mInputVideoPath));
        return mp.getDuration();
    }

    public void compressVideo() {
        int duration = getDuration();
        int bitrate = calBitrate(duration / 1000, Integer.parseInt(mBinding.getSize().trim()));

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
                isCompress.set(false);
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
