package net.steamcrafted.servicetask_example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import net.steamcrafted.servicetask.model.ServiceTaskPojo;
import net.steamcrafted.servicetask.service.ServiceTask;
import net.steamcrafted.servicetask.service.ServiceTaskService;

import cz.msebera.android.httpclient.Header;


public class MainActivity extends AppCompatActivity {

    TextView tv;
    Button getButton;
    Button registerButton;
    String getRequestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView) findViewById(R.id.main_text);
        getButton = (Button) findViewById(R.id.main_button);
        getButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRequestId = new GetRequest().execute(MainActivity.this,
                    new GetRequest.Request("http://google.com", "GET"));
                registerButton.setEnabled(true);
            }
        });
        registerButton = (Button) findViewById(R.id.main_button_register);
        registerButton.setEnabled(getRequestId != null);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getRequestId != null)
                    new GetRequest().register(MainActivity.this, getRequestId, new ServiceTask.ServiceTaskCallback<GetRequest.Response>() {
                        @Override
                        public void afterAsync(GetRequest.Response result) {
                            if (result.success) {
                                setText("Success: " + result.data);
                            } else {
                                setText("Something went wrong!");
                            }
                        }
                    });
            }
        });

        if(savedInstanceState != null) onRestoreInstanceState(savedInstanceState);
    }

    public void setText(String text){
        tv.setText(text);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("getRequestId", getRequestId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        getRequestId = savedInstanceState.getString("getRequestId");
        System.out.println("ONRESTORE: " + getRequestId);
/*        if(getRequestId != null){
            new GetRequest().register(this, getRequestId, new ServiceTask.ServiceTaskCallback<Pair<String, Boolean>>() {
                @Override
                public void afterAsync(Pair<String, Boolean> result) {
                    tv.setText("onrestore: " + result.first);
                }
            });
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerButton.setEnabled(getRequestId != null);
    }

    @Override
    protected void onDestroy() {
        ServiceTaskService.unregisterReceivers(this);
        super.onDestroy();
    }

    public static class GetRequest extends ServiceTask<GetRequest.Request, GetRequest.Response> {

        public static class Request extends ServiceTaskPojo {
            public String url;
            public String method;

            public Request(String url, String method) {
                this.url = url;
                this.method = method;
            }
        }

        public static class Response extends ServiceTaskPojo {
            public String data;
            public boolean success;
            public Header[] headers;
        }

        @Override
        public Response onAsync(Request request) {
            final Response r = new Response();
            TextHttpResponseHandler callback = new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    r.success = false;
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    r.success = true;
                    r.data = responseString;
                    r.headers = headers;
                }
            };

            SyncHttpClient client = new SyncHttpClient();
            if(request.method.equalsIgnoreCase("GET")){
                client.get(request.url, callback);
            }else if(request.method.equalsIgnoreCase("POST")){
                client.post(request.url, callback);
            }else if(request.method.equalsIgnoreCase("PUT")){
                client.put(request.url, callback);
            }else if(request.method.equalsIgnoreCase("DELETE")){
                client.delete(request.url, callback);
            }else{
                client.get(request.url, callback);
            }

            return r;
        }
    }

}
