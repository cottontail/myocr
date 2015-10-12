package com.melody.cool.myocr.manager.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BaseManager {

    public Context mContext;
    public SharedPreferences mSharedPreferences;

    public BaseManager(Context context){
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public boolean getBoolean(String key, boolean value){
        return mSharedPreferences.getBoolean(key, value);
    }

    public float getFloat(String key, float value){
        return mSharedPreferences.getFloat(key, value);
    }

    public int getInt(String key, int value){
        return mSharedPreferences.getInt(key, value);
    }

    public long getLong(String key, long value){
        return mSharedPreferences.getLong(key, value);
    }

    public String getString(String key, String value){
        return mSharedPreferences.getString(key, value);
    }

    public List<String> getStringList(String key, List<String> value){

        List<String> stringList = new ArrayList<>();

        Set<String> defaultStringSet = new HashSet<>();
        if(value != null){
            for(String string : value){
                defaultStringSet.add(string);
            }
        }

        Set<String> stringSet = mSharedPreferences.getStringSet(key, defaultStringSet);
        for(String string : stringSet){
            stringList.add(string);
        }

        return stringList;

    }

    public void putBoolean(String key, boolean value){
        mSharedPreferences.edit().putBoolean(key, value).commit();
    }

    public void putFloat(String key, float value){
        mSharedPreferences.edit().putFloat(key, value).commit();
    }

    public void putInt(String key, int value){
        mSharedPreferences.edit().putInt(key, value).commit();
    }

    public void putLong(String key, long value){
        mSharedPreferences.edit().putLong(key, value).commit();
    }

    public void putString(String key, String value){
        mSharedPreferences.edit().putString(key, value).commit();
    }

    public void putStringList(String key, List<String> value){

        Set<String> stringSet = new HashSet<>();
        if(value != null){
            for(String string : value){
                stringSet.add(string);
            }
        }
        mSharedPreferences.edit().putStringSet(key, stringSet).commit();
    }

    public void clearPreference(String key){
        mSharedPreferences.edit().remove(key).commit();
    }

}
