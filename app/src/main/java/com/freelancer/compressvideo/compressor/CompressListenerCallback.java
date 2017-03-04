package com.freelancer.compressvideo.compressor;

/**
 * Created by ThongLe on 3/3/2017.
 */

public interface CompressListenerCallback {
    void onExecSuccess(String message);

    void onExecFail(String reason);

    void onExecProgress(String message);
}
