package net.steamcrafted.servicetask_example;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by Wannes2 on 7/05/2016.
 */
public class ServiceTaskApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}
