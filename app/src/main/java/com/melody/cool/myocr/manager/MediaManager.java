package com.melody.cool.myocr.manager;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import com.melody.cool.myocr.R;
import com.melody.cool.myocr.manager.base.BaseManager;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaManager extends BaseManager {

    public Context mContext;

    public File mMainFolder;

    public File mInternalDataFolder;
    public File mExternalCacheFolder;

    public File mVideoThumbnailsFolder;

    public File mFontsFolder;

    public MediaManager(Context context){

        super(context);
        mContext = context;

        mMainFolder = new File(Environment.getExternalStorageDirectory(), "cool.myocr");
        mInternalDataFolder = mContext.getCacheDir();

        String mainFolder = mSharedPreferences.getString(mContext.getString(R.string.pref_settings_media_folder), "");
        File mainFolderFile = new File(mainFolder);
        if(mainFolderFile.exists() && mainFolderFile.isDirectory()){
            mMainFolder = new File(mainFolder);
        }

        mExternalCacheFolder = new File(mMainFolder, "Cache");

        mFontsFolder = new File(mMainFolder, "Fonts");
        mVideoThumbnailsFolder = new File(mMainFolder, "Video Thumbnails");


        mExternalCacheFolder.mkdirs();
        mVideoThumbnailsFolder.mkdirs();
        mFontsFolder.mkdirs();

        createNoMediaFile(mExternalCacheFolder);
        createNoMediaFile(mVideoThumbnailsFolder);

    }

    public void setMainFolder(File folder){

        if(folder.exists() && folder.isDirectory()){
            putString(mContext.getString(R.string.pref_settings_media_folder), folder.getAbsolutePath());
        }

    }

    public boolean duplicateFile(File sourceFile, File destinationFile){
        try {
            FileUtils.copyFile(sourceFile, destinationFile, true);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    public void addToGallery(String mime, File file){
        MediaScannerConnection.scanFile(mContext, new String[]{file.getPath()}, new String[]{mime}, null);
    }

    public static void deleteFolder(File folder){

        try {
            FileUtils.deleteDirectory(folder);
        } catch(Exception e){
            e.printStackTrace();
        }

    }

    public boolean createNoMediaFile(File directory){

        try{
            File noMedia = new File(directory, ".nomedia");
            if(!noMedia.exists()) {
                return noMedia.createNewFile();
            }
        } catch (Exception e){
            return false;
        }
        return false;
    }
}