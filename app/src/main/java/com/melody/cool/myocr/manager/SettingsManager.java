package com.melody.cool.myocr.manager;

import android.content.Context;
import com.melody.cool.myocr.R;
import com.melody.cool.myocr.manager.base.BaseManager;

public class SettingsManager extends BaseManager {

    public SettingsManager(Context context){
        super(context);
    }


    public boolean selectDocumentsApp(){
        return getBoolean(mContext.getString(R.string.pref_settings_select_documents_app), false);
    }

    public boolean showViewTime(){
        return getBoolean(mContext.getString(R.string.pref_settings_show_view_time), true);
    }

    public boolean hideViewerToolbar(){
        return getBoolean(mContext.getString(R.string.pref_settings_hide_viewer_toolbar), false);
    }

    public boolean rememberPassword(){
        return getBoolean(mContext.getString(R.string.pref_settings_remember_password), true);
    }

    public boolean notificationsEnabled(){
        return getBoolean(mContext.getString(R.string.pref_settings_enable_notifications), true);
    }

    public boolean blackberryModeEnabled(){
        return getBoolean(mContext.getString(R.string.pref_settings_blackberry_mode), false);
    }

    public boolean customVideoThumbnailEnabled(){
        return getBoolean(mContext.getString(R.string.pref_settings_custom_video_thumbnail), false);
    }

    public boolean betaUpdatesEnabled(){
        return getBoolean(mContext.getString(R.string.pref_settings_updates_beta), false);
    }

    public boolean showCircleStoryIcons(){
        return getBoolean(mContext.getString(R.string.pref_settings_show_circle_story_icons), false);
    }

    public void disableNotifications() {
        putBoolean(mContext.getString(R.string.pref_settings_enable_notifications), false);
    }

    public boolean experimentalBackgroundUploadsEnabled(){
        return getBoolean(mContext.getString(R.string.pref_settings_experimental_background_uploads), false);
    }

    public boolean experimentalSendQuickLaunch(){
        return getBoolean(mContext.getString(R.string.pref_settings_experimental_send_quick_launch), true);
    }



    public int getNotificationCheckInterval(){
        int interval = Integer.parseInt(getString(mContext.getString(R.string.pref_settings_notifications_interval), "30"));
        System.out.println("Notification Interval: " + interval);
        return interval;
    }


}