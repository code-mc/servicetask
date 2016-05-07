package net.steamcrafted.servicetask.service;

import android.content.Context;

/**
 * Created by Wannes2 on 30/04/2016.
 */
public abstract class ServiceTask<U, T> {

    private ServiceTaskCallback<T> mCallback;

    public ServiceTask register(Context c, String id, ServiceTaskCallback<T> callback) {
        mCallback = callback;
        ServiceTaskService.register(c, id, this);
        return this;
    }

    public String execute(Context c, U data, ServiceTaskCallback<T> callback){
        mCallback = callback;
        return ServiceTaskService.execute(c, this, data);
    }

    public String execute(Context c, U data){
        return ServiceTaskService.execute(c, this, data);
    }

    public abstract T onAsync(U data);

    public boolean afterAsync(T result){
        if(mCallback != null){
            // System.out.println("found registered listener");
            mCallback.afterAsync(result);
            return true;
        }
        return false;
    }

    public interface ServiceTaskCallback<T> {
        void afterAsync(T result);
    }
}
