package com.freelancer.compressvideo;

/**
 * Created by ThongLe on 3/3/2017.
 */

public interface CompressListener {
    void onLoadSuccess();

    void onLoadFail(String reason);
}
