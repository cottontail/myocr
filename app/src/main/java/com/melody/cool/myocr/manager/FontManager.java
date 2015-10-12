package com.melody.cool.myocr.manager;

import android.content.Context;
import android.util.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FontManager extends MediaManager {

    public FontManager(Context context) {

        super(context);

    }

    public File getFontFile(String fontName){

        String filename = fontName + ".ttf";
        return new File(mFontsFolder, filename);

    }

    public boolean isFontDownloaded(String fontName){

        File fontFile = getFontFile(fontName);
        return fontFile.exists() && fontFile.length() > 0;

    }

    public void deleteAllFonts(){

        List<Pair<String,File>> fontFiles = getDownloadedFonts();
        for(Pair<String,File> pair : fontFiles){
            pair.second.delete();
        }

    }

    public List<Pair<String,File>> getDownloadedFonts() {
        List<Pair<String,File>> fontFiles = new ArrayList<Pair<String,File>>();
        File[] files = mFontsFolder.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {
                if(file.getName().endsWith(".ttf")){
                    String name = file.getName().replace(".ttf", "");
                    fontFiles.add(new Pair<String, File>(name, file));
                }
            }
        }
        return fontFiles;
    }

}
