/*
 *  Copyright (C) 2017 Bilibili
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.kanade.imageselector_core.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;

import com.kanade.imageselector_core.AbsBoxingViewFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * A helper to start camera.<br/>
 * used by {@link AbsBoxingViewFragment}
 *
 * @author ChenSL
 */
public class CameraPickerHelper {
    public static final int REQ_CODE_CAMERA = 0x2001;
    public static final int REQ_CODE_REC = 0x1774;
    private static final String STATE_SAVED_KEY = "com.kanade.imageselector_core.utils.CameraPickerHelper.saved_state";
    private static final int FILE_MAX_SIZE = 20971520;    //上传文件最大体积（字节）

    private String mSourceFilePath;
    private Uri mSourceFileUri;
    private File mOutputFile;
    private Callback mCallback;

    public interface Callback {
        void onFinish(@NonNull CameraPickerHelper helper);

        void onError(@NonNull CameraPickerHelper helper);
    }

    public CameraPickerHelper(@Nullable Bundle savedInstance) {
        if (savedInstance != null) {
            SavedState state = savedInstance.getParcelable(STATE_SAVED_KEY);
            if (state != null) {
                mOutputFile = state.mOutputFile;
                mSourceFilePath = state.mSourceFilePath;
            }
        }
    }

    public void setPickCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void onSaveInstanceState(Bundle out) {
        SavedState state = new SavedState();
        state.mOutputFile = mOutputFile;
        state.mSourceFilePath = mSourceFilePath;
        out.putParcelable(STATE_SAVED_KEY, state);
    }

    /**
     * start system camera to take a picture
     *
     * @param activity      not null if fragment is null.
     * @param fragment      not null if activity is null.
     * @param subFolderPath a folder in external DCIM,must start with "/".
     */
    public void startCamera(final Activity activity, final Fragment fragment, final String subFolderPath) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !takePhotoSecure(activity, fragment, subFolderPath)) {
            FutureTask<Boolean> task = BoxingExecutor.getInstance().runWorker(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        // try...try...try
                        Camera camera = Camera.open();
                        camera.release();
                    } catch (Exception e) {
                        BoxingLog.d("camera is not available.");
                        return false;
                    }
                    return true;
                }
            });
            try {
                if (task != null && task.get()) {
                    startCameraIntent(activity, fragment, subFolderPath, MediaStore.ACTION_IMAGE_CAPTURE, REQ_CODE_CAMERA);
                } else {
                    callbackError();
                }
            } catch (InterruptedException | ExecutionException ignore) {
                callbackError();
            }

        }
    }

    /**
     * start system camera to recording
     *
     * @param activity      not null if fragment is null.
     * @param fragment      not null if activity is null.
     * @param subFolderPath a folder in external DCIM, must start with "/".
     */
    public void startVideo(Activity activity, Fragment fragment, String subFolderPath) {
        String cameraOutDir = BoxingFileHelper.getExternalCamera(subFolderPath);
        try {
            if (BoxingFileHelper.createFile(cameraOutDir)) {
                mOutputFile = new File(cameraOutDir, String.valueOf(System.currentTimeMillis()) + ".mp4");
                mSourceFilePath = mOutputFile.getPath();
                mSourceFileUri = getFileUri(activity.getApplicationContext(), mOutputFile);

                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0.9);
                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 8000);
                intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, FILE_MAX_SIZE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mSourceFileUri);
                try {
                    startActivityForResult(activity, fragment, intent, REQ_CODE_REC);
                } catch (ActivityNotFoundException ignore) {
                    callbackError();
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            BoxingLog.d("create file" + cameraOutDir + " error.");
        }
    }

    private boolean takePhotoSecure(Activity activity, Fragment fragment, String subDir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                startCameraIntent(activity, fragment, subDir, MediaStore.ACTION_IMAGE_CAPTURE, REQ_CODE_CAMERA);
                return true;
            } catch (ActivityNotFoundException ignore) {
                return false;
            }
        }
        return false;
    }

    private void callbackFinish() {
        if (mCallback != null) {
            mCallback.onFinish(CameraPickerHelper.this);
        }
    }

    private void callbackError() {
        if (mCallback != null) {
            mCallback.onError(CameraPickerHelper.this);
        }
    }

    private void startActivityForResult(Activity activity, Fragment fragment, final Intent intent, final int reqCodeCamera) throws ActivityNotFoundException {
        if (fragment == null) {
            activity.startActivityForResult(intent, reqCodeCamera);
        } else {
            fragment.startActivityForResult(intent, reqCodeCamera);
        }
    }

    private void startCameraIntent(final Activity activity, final Fragment fragment, String subFolder,
                                   final String action, final int requestCode) {
        String cameraOutDir = BoxingFileHelper.getExternalDCIM(subFolder);
        try {
            if (BoxingFileHelper.createFile(cameraOutDir)) {
                mOutputFile = new File(cameraOutDir, String.valueOf(System.currentTimeMillis()) + ".jpg");
                mSourceFilePath = mOutputFile.getPath();
                mSourceFileUri = getFileUri(activity.getApplicationContext(), mOutputFile);
                Intent intent = new Intent(action);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mSourceFileUri);
                try {
                    startActivityForResult(activity, fragment, intent, requestCode);
                } catch (ActivityNotFoundException ignore) {
                    callbackError();
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            BoxingLog.d("create file" + cameraOutDir + " error.");
        }
    }

    private Uri getFileUri(@NonNull Context context, @NonNull File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context,
                    context.getApplicationContext().getPackageName() + ".file.provider", mOutputFile);
        } else {
            return Uri.fromFile(file);
        }
    }

    public String getSourceFilePath() {
        return mSourceFilePath;
    }

    public Uri getmSourceFileUri() {
        return mSourceFileUri;
    }

    /**
     * deal with the system camera's shot.
     */
    public boolean onActivityResult(int requestCode, final int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            callbackError();
            return false;
        }

        if (requestCode == REQ_CODE_CAMERA) {
            FutureTask<Boolean> task = BoxingExecutor.getInstance().runWorker(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return rotateImage(resultCode);
                }
            });
            try {
                if (task != null && task.get()) {
                    callbackFinish();
                } else {
                    callbackError();
                }
            } catch (InterruptedException | ExecutionException ignore) {
                callbackError();
            }
            return true;
        } else if (requestCode == REQ_CODE_REC) {
            mSourceFileUri = data.getData();
            if (mSourceFileUri == null) {
                return false;
            }
            callbackFinish();
        }
        return false;
    }

    private boolean rotateSourceFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            return false;
        }
        FileOutputStream outputStream = null;
        Bitmap bitmap = null;
        Bitmap outBitmap = null;
        try {
            int degree = BoxingExifHelper.getRotateDegree(file.getAbsolutePath());
            Matrix matrix = new Matrix();
            if (degree != 0) {
                matrix.postRotate(degree);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            outBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            outputStream = new FileOutputStream(file);
            outBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            return true;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    BoxingLog.d("IOException when output stream closing!");
                }
            }
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (outBitmap != null) {
                outBitmap.recycle();
            }
        }
    }

    private boolean rotateImage(int resultCode) throws IOException {
        return resultCode == Activity.RESULT_OK && rotateSourceFile(mOutputFile);
    }

    public void release() {
        mOutputFile = null;
    }

    private static class SavedState implements Parcelable {
        private File mOutputFile;
        private String mSourceFilePath;

        SavedState() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeSerializable(this.mOutputFile);
            dest.writeString(this.mSourceFilePath);
        }

        SavedState(Parcel in) {
            this.mOutputFile = (File) in.readSerializable();
            this.mSourceFilePath = in.readString();
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}
