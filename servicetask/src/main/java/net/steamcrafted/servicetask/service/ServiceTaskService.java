package net.steamcrafted.servicetask.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.google.gson.Gson;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Wannes2 on 30/04/2016.
 */
public class ServiceTaskService extends IntentService {

    private static final String ARG_DATA_TYPE       = "ARG_DATA_TYPE";
    private static final String ARG_DATA_JSON       = "ARG_DATA_JSON";
    private static final String ARG_CALLBACK_TYPE   = "ARG_CALLBACK_TYPE";
    private static final String ARG_SERVICE_TAG     = "ARG_SERVICE_TAG";
    private static final String ARG_RESULT_JSON     = "ARG_RESULT_JSON";
    private static final String ARG_RESULT_TYPE     = "ARG_RESULT_TYPE";
    private static final String ARG_FUNCTION        = "ARG_FUNCTION";
    private static final String ARG_RESULT_FUNCTION = "ARG_RESULT_FUNCTION";

    private static final int FUNCTION_EXEC  = 0;
    private static final int FUNCTION_QUERY = 1;

    private static final int FUNCTION_RESULT_DATA = 2;
    private static final int FUNCTION_RESULT_UNREGISTER = 3;

    private static int mId = 0;
    private static Map<String, BroadcastReceiver> mRegisteredReceivers = new HashMap<>();
    private static Map<String, String> mReveiverOwners = new HashMap<>();

    private static String generateTag(){
        mId += 1;
        return ServiceTaskService.class.getName() + mId;
    }

    public ServiceTaskService() {
        super("ServiceTaskService");
    }

    public static void unregisterReceivers(Context c){
        for(BroadcastReceiver r : mRegisteredReceivers.values()) c.getApplicationContext().unregisterReceiver(r);
        mRegisteredReceivers.clear();
    }

    public static void unregisterReceiver(Context c, String action){
        if(mRegisteredReceivers.containsKey(action)){
            c.getApplicationContext().unregisterReceiver(mRegisteredReceivers.get(action));
            mRegisteredReceivers.remove(action);
        }
    }

    public static String execute(final Context c, final ServiceTask callback, Object data){
        Gson gson = new Gson();
        String serialized = gson.toJson(data, data.getClass());
        String service_tag = generateTag();

        Intent service = new Intent(c.getApplicationContext(), ServiceTaskService.class);
        service.putExtra(ARG_CALLBACK_TYPE, callback.getClass().getName());
        service.putExtra(ARG_DATA_TYPE, data.getClass().getName());
        service.putExtra(ARG_DATA_JSON, serialized);
        service.putExtra(ARG_SERVICE_TAG, service_tag);
        service.putExtra(ARG_FUNCTION, FUNCTION_EXEC);

        registerReceiver(c, service_tag, callback);

        c.getApplicationContext().startService(service);
        return service_tag;
    }

    private static void registerReceiver(Context c, String service_tag, final ServiceTask callback) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(service_tag);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int function = intent.getExtras().getInt(ARG_RESULT_FUNCTION);
                if(function == FUNCTION_RESULT_DATA){
                    System.out.println("got data result inside receiver");
                    try {
                        boolean ok = callback.afterAsync(
                                new Gson().fromJson(
                                        intent.getExtras().getString(ARG_RESULT_JSON), Class.forName(intent.getExtras().getString(ARG_RESULT_TYPE))
                                )
                        );
                        if(ok) this.setResultCode(Activity.RESULT_OK);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }else if(function == FUNCTION_RESULT_UNREGISTER){
                    System.out.println("unregistering receiver");
                    unregisterReceiver(context, intent.getExtras().getString(ARG_SERVICE_TAG));
                }
            }
        };
        mRegisteredReceivers.put(service_tag, receiver);
        c.getApplicationContext().registerReceiver(receiver, intentFilter);

        // Query the service for any changes while we were unregistered
        Intent service = new Intent(c.getApplicationContext(), ServiceTaskService.class);
        service.putExtra(ARG_SERVICE_TAG, service_tag);
        service.putExtra(ARG_FUNCTION, FUNCTION_QUERY);
        c.getApplicationContext().startService(service);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int function = intent.getExtras().getInt(ARG_FUNCTION);
        if(function == FUNCTION_EXEC)
        {
            try {
                Class<?> callback_type      = Class.forName(intent.getExtras().getString(ARG_CALLBACK_TYPE));
                Class<?> data_type          = Class.forName(intent.getExtras().getString(ARG_DATA_TYPE));
                String service_tag    = intent.getExtras().getString(ARG_SERVICE_TAG);

                Object data = new Gson().fromJson(intent.getExtras().getString(ARG_DATA_JSON), data_type);

                Object callback = callback_type.newInstance();
                Method onAsync = callback_type.getDeclaredMethod("onAsync", data_type);

                Object result = onAsync.invoke(callback, data);
                String result_serialized = new Gson().toJson(result, result.getClass());
                String result_type = result.getClass().getName();
                sendDataBroadcast(result_serialized, result_type, service_tag);
            } catch (IllegalAccessException e){
                e.printStackTrace();
                System.err.println("Tried to invoke a ServiceTask using a class that can not be publicly accessed.\n" +
                        "Try making the class public by prepending its definition with 'public'.");
            } catch (InstantiationException e){
                e.printStackTrace();
                System.err.println("Tried to invoke a ServiceTask using a nested class.\n" +
                        "Try making the class static by prepending its definition with 'static'.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(function == FUNCTION_QUERY)
        {
            String service_tag = intent.getExtras().getString(ARG_SERVICE_TAG);
            if(inqueue(service_tag))
            {
                CachedResponse response = dequeue(service_tag);
                sendDataBroadcast(response.data, response.data_type, response.service_tag);
            }
        }
    }

    private void sendUnregisterBroadcast(String service_tag) {
        Intent unregister_message = new Intent();
        unregister_message.setAction(service_tag);
        unregister_message.putExtra(ARG_RESULT_FUNCTION, FUNCTION_RESULT_UNREGISTER);
        unregister_message.putExtra(ARG_SERVICE_TAG, service_tag);

        sendBroadcast(unregister_message);
    }

    private void sendDataBroadcast(final String data, final String data_type, final String service_tag){
        Intent response_message = new Intent();
        response_message.setAction(service_tag);
        response_message.putExtra(ARG_RESULT_JSON, data);
        response_message.putExtra(ARG_RESULT_TYPE, data_type);
        response_message.putExtra(ARG_SERVICE_TAG, service_tag);
        response_message.putExtra(ARG_RESULT_FUNCTION, FUNCTION_RESULT_DATA);

        sendOrderedBroadcast(response_message, null, new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                int result_code = getResultCode();

                // Result was handled by someone, don't save the response
                if (result_code != Activity.RESULT_CANCELED) {
                    System.out.println("someone got the result!");
                    sendUnregisterBroadcast(service_tag);
                    return;
                }
                // Queue the result for layer re-emission
                System.out.println("queueing the result...");
                queue(data, data_type, service_tag);
            }
        }, null, Activity.RESULT_CANCELED, null, null);
    }

    private static boolean inqueue(String service_tag){
        for(CachedResponse response : mQueue)
            if(response.service_tag.equals(service_tag))
                return true;
        return false;
    }

    private static CachedResponse dequeue(String service_tag) {
        for(CachedResponse response : mQueue){
            if(response.service_tag.equals(service_tag)){
                mQueue.remove(response);
                return response;
            }
        }
        return null;
    }

    private static final List<CachedResponse> mQueue = new ArrayList<>();
    private static void queue(String data, String data_type, String service_tag){
        mQueue.add(new CachedResponse(data, data_type, service_tag));
    }

    public static void register(Context c, String id, ServiceTask callback) {
        for(String s : mRegisteredReceivers.keySet()){
            if(s.equals(id)){
                c.getApplicationContext().unregisterReceiver(mRegisteredReceivers.get(s));
                registerReceiver(c, id, callback);
                return;
            }
        }
        registerReceiver(c, id, callback);
    }

    private static class CachedResponse {
        String data;
        String data_type;
        String service_tag;

        public CachedResponse(String data, String data_type, String service_tag) {
            this.data = data;
            this.data_type = data_type;
            this.service_tag = service_tag;
        }
    }
}
