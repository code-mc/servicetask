# Service Task Library

The asynchronous Android library that eats configuration changes for breakfast.

## What

Service Task was build as a reliable replacement for the horrendous AsyncTask. Instead of using an object dependent on the current activity context (that will be recreated on configuration change) Service Task builds upon the robust Service API.

Your asynchronous work will be executed inside an IntentService which runs each task on a thread completely separated from the Activity you call it from. The produced result will be passed on to the main thread.

## Statistics

 * API 3+
 * ~10KB in size + ~180KB for the GSON dependency
 * Serializes data passed between threads

## API

API consists of 2 classes:

### `ServiceTask<Type1, Type2>`

Abstract template class. Extend this to implement your own async ServiceTask. You have to specify two types: the former for input data (what you give to the task) and one for the output data (what the task produces as a result). You can use any generic Java type for this. If you want to use your own class read the `ServiceTaskPojo` section.

`Type2 onAsync(Type1 data)` Everything that is executed inside this method will be running inside a separate thread. Has a single argument of the first specified type in your class. This will represent your input data. The return type is of the second specified type in your class.

`String execute(Context c, Type1 data)` Call this to start the task. Returns a `String` that contains a unique ID which you can use to interact with your running task.

`void register(Context c, String task_id, ServiceTaskCallback<Type2> callback)` Call this method to register a callback to a running task. You have to specify the task ID to register a callback. So always hold on to your task ID (see example).

### `ServiceTaskPojo`

Class you extend when you want to pass in your own Pojo objects to a ServiceTask's onAsync method. This class has the required proguard rules to work nicely with GSON (which serializes/deserializes your data between threads)

### `ServiceTaskCallback<Type2>`

Interface that you override to register as a callback to your `ServiceTask`.

## Example

The best way to explain the library is by showing you an example. Let's say we want to download an image file. The first thing you do is create a class specific to your task that extends the `ServiceTask` class. We'll call it `ImageDownloader`. The `ServiceTask` class is a templated class requiring 2 types: the type of your input data (which will be passed on to the background thread) and the type of the result produced by the thread.

````java
class ImageDownloader extends ServiceTask<String, String> {
    @Override
    public String onAsync(String url){
        // Path where the downloaded image will be saved
        final String file_path = "images/image.png";
        
        // Create a synchronous client
        SyncHttpClient client = new SyncHttpClient();
        client.get(url, new BinaryHttpResponseHandler() {
            @Override
            public void onSuccess(byte[] imageData) {
                // Save the data to disk
                Utils::save(file_path, imageData); // Some method that writes to disk
            }

            @Override
            public void onFailure(Throwable e, byte[] imageData) {
                
            }
        });
        // Return the location of the saved file
        return file_path;
    }
}
```

`ServiceTask` is an abstract class, Android Studio will ask you to implement the `onAsync` method. Everything inside this method will be executed inside a background thread.

To further expand on our example, let's call the `ImageDownloader` from an `Activity`:

```java
class MainActivity extends Activity {

    String getRequestId; // ID of our running task

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ...
        
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // We start the task and save its unique ID for later
                getRequestId = new ImageDownloader().execute(MainActivity.this,
                    "http://example.com/someimage.png");
                // Register the callback that will capture the result
                registerCallback();
            }
        });
        
        // ...
    }
    
    /*
     * We use a separate method for registering as it is something you'll
     * have to do multiple times.
     */
    private void registerCallback(){
        new ImageDownloader().register(MainActivity.this, getRequestId, new ServiceTask.ServiceTaskCallback<String>() {
            @Override
            public void afterAsync(String file_path) {
                // Here we can do something with the result on the UI thread
            }
        });
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // This is important, when the device is rotated we have to save the current
        // task id so we can re-register in onRestoreInstanceState, that way results
        // never get lost.
        outState.putString("getRequestId", getRequestId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Grab the task id
        getRequestId = savedInstanceState.getString("getRequestId");
        // We register again. Because of the device rotation the previously
        // registered callback got destroyed so it wouldn't be called
        registerCallback();
    }

}
```

This might look like a lot of code, but sadly there isn't a silver bullet for retaining a value on configuration change. At least this time you only have to retain a string and not a complete object. Or worse: completely restart the request.

After you put all this in place you're good to go. The library will handle everything else for you.

#License

Released under the [Apache 2.0 License](https://github.com/code-mc/servicetask/blob/master/license.md)