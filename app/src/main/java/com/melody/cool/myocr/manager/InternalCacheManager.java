package com.melody.cool.myocr.manager;

import android.content.Context;



import java.io.File;
import java.util.*;

public class InternalCacheManager extends MediaManager {

    public File mRequestsFolder;
    public File mCacheFolder;
    public File mTempFolder;

    public InternalCacheManager(Context context){

        super(context);

        mRequestsFolder = new File(mInternalDataFolder, "Requests");
        mCacheFolder = new File(mInternalDataFolder, "Cache");
        mTempFolder = new File(mCacheFolder, "Temp");

        mRequestsFolder.mkdirs();
        mCacheFolder.mkdirs();
        mTempFolder.mkdirs();

    }

    public File getCacheEndpointParamsFile(String endpointName){
        endpointName = endpointName.replace("/", ".");
        endpointName = endpointName.replace("*", "GLOBAL");
        return new File(mRequestsFolder, "params~" + endpointName);
    }

    public File getCacheEndpointHeadersFile(String endpointName){
        endpointName = endpointName.replace("/", ".");
        endpointName = endpointName.replace("*", "GLOBAL");
        return new File(mRequestsFolder, "headers~" + endpointName);
    }

    public File getCacheFile(String payloadName){
        return new File(mCacheFolder, "payload~" + payloadName);
    }


    public File getInternalTempFile(){
        return new File(mTempFolder, UUID.randomUUID().toString() + ".tmp");
    }

    public File getExternalTempFile(){
        return new File(mExternalCacheFolder, UUID.randomUUID().toString() + ".tmp");
    }


    public void cleanCache(final boolean clearAll){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.HOUR, -24);
                Date expireDate = calendar.getTime();

                List<File> cacheFiles = getFiles(mCacheFolder);
                for(File file : cacheFiles){
                    if (clearAll || file.lastModified() < expireDate.getTime()) {
                        file.delete();
                    }
                }

                List<File> externalCacheFiles = getFiles(mExternalCacheFolder);
                for(File file : externalCacheFiles){
                    if (clearAll || file.lastModified() < expireDate.getTime()) {
                        file.delete();
                    }
                }

            }
        }).start();

    }

    public void clearDynamicEndpoints(){

        List<File> cacheFiles = getFiles(mRequestsFolder);
        for(File file : cacheFiles){
            file.delete();
        }

    }

    private List<File> getFiles(File directory) {

        ArrayList<File> fileList = new ArrayList<File>();
        File[] files = directory.listFiles();

        if(files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    fileList.addAll(getFiles(file));
                } else {
                    fileList.add(file);
                }
            }
        }

        return fileList;

    }

}