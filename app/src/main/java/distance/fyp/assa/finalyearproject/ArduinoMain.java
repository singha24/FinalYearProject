package distance.fyp.assa.finalyearproject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.microedition.khronos.egl.EGLConfig;

public class ArduinoMain extends GvrActivity implements GvrView.StereoRenderer {

    ImageView frontSensor, backSensor, leftSensor, rightSensor, frontLeftSensor, backLeftSensor, backRightSensor, frontRightSensor;
    Handler bluetoothIn;

    private static String TAG = "ARDUINO_ACTIVITY";

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    private static int sensor0;
    private static int sensor1;
    private static int sensor2;
    private static int sensor3;
    private static int sensor4;
    private static int sensor5;
    private static int sensor6;
    private static int sensor7;

    /* Stereo panning of all sounds. This disables HRTF-based rendering. */
    public static final int STEREO_PANNING = 0;

    /* Renders all sounds over eight virtual loudspeakers arranged around
    the listener’s head. HRTF-based rendering is enabled. */
    public static final int BINAURAL_LOW_QUALITY = 1;

    public static final int INVALID_ID = -1;

    static String[] label = {"F", "B", "L", "R", "FL", "BL", "BR", "FR"};
    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("0000111f-0000-1000-8000-00805f9b34fb");

    // String for MAC address
    private static String address;


    private static GvrAudioEngine gvrAudioEngine;
    private volatile int sourceId = GvrAudioEngine.INVALID_ID;
    private static final String OBJECT_SOUND_FILE = "cube_sound.wav";
    private static final String BEEP = "beep.mp3";

    private SoundPositionObject frontSound;
    private SoundPositionObject backSound;
    private SoundPositionObject leftSound;
    private SoundPositionObject rigthSound;

    private SoundPositionObject frontRigthSound;
    private SoundPositionObject frontLeftSound;

    private SoundPositionObject backRightSound;
    private SoundPositionObject backLeftSound;

    private WebView webCamera;

    String URL = "http://192.168.0.16:8081";

    Document documentt;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_arduino_main);

        frontSensor = (ImageView) findViewById(R.id.front);
        backSensor = (ImageView) findViewById(R.id.back);
        leftSensor = (ImageView) findViewById(R.id.left);
        rightSensor = (ImageView) findViewById(R.id.right);
        frontLeftSensor = (ImageView) findViewById(R.id.frontLeft);
        backLeftSensor = (ImageView) findViewById(R.id.backLeft);
        backRightSensor = (ImageView) findViewById(R.id.backRight);
        frontRightSensor = (ImageView) findViewById(R.id.frontRight);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webCamera = (WebView) findViewById(R.id.url);


        webCamera.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        String newUA = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/20100101 Firefox/4.0";
        webCamera.getSettings().setUserAgentString(newUA);
        webCamera.getSettings().setJavaScriptEnabled(true);
        webCamera.getSettings().setDomStorageEnabled(true);
        webCamera.setWebViewClient(new WebViewClient() {


            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                handler.proceed("admin", "admin");
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                webCamera.loadUrl("javascript:(function() { " +
                        "document.getElementById(\"imgvideo\"); })()");
            }




        });

        webCamera.loadUrl("http://192.168.0.16:8081");


        frontSound = new SoundPositionObject(0.0f, 0.0f, 5.0f);
        backSound = new SoundPositionObject(0.0f, 0.0f, -10.0f);
        leftSound = new SoundPositionObject(-5.0f, 0.0f, 0.0f);
        rigthSound = new SoundPositionObject(5.0f, 0.0f, 0.0f);

        frontRigthSound = new SoundPositionObject(5.0f, 0.0f, 5.0f);
        frontLeftSound = new SoundPositionObject(-5.0f, 0.0f, 5.0f);

        backRightSound = new SoundPositionObject(5.0f, 0.0f, -10.0f);
        backLeftSound = new SoundPositionObject(-5.0f, 0.0f, -10f);


        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;

                    recDataString.append(readMessage);

                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // ex
                        //int dataLength = dataInPrint.length();                          //get length of data received

                        //Log.d("DATA", recDataString.toString());

                        String[] data = recDataString.toString().split("\\+");

                        sensor0 = Integer.parseInt(data[0]);
                        sensor1 = Integer.parseInt(data[1]);
                        sensor2 = Integer.parseInt(data[2]);
                        sensor3 = Integer.parseInt(data[3]);
                        sensor4 = Integer.parseInt(data[4]);
                        sensor5 = Integer.parseInt(data[5]);
                        sensor6 = Integer.parseInt(data[6]);
                        sensor7 = Integer.parseInt(data[7]);


                        int[] sensorDataArray = {sensor0, sensor1, sensor2, sensor3, sensor4, sensor5, sensor6, sensor7};
                        ImageView[] proximity = {frontSensor, backSensor, leftSensor, rightSensor, frontLeftSensor, backLeftSensor, backRightSensor, frontRightSensor};
                        SoundPositionObject[] soundPos = {frontSound, backSound, leftSound, rigthSound, frontLeftSound, backLeftSound, backRightSound, frontRigthSound};


                        for (int i = 0; i < sensorDataArray.length; i++) {

                            //proximity[i].setText(label[i] + " : " + sensorDataArray[i]);

                            if (sensorDataArray[i] > 0) {

                                if (sensorDataArray[i] < 10) {

                                    proximity[i].setImageResource(R.drawable.close);
                                    //play3DSound(soundPos[i]); //TODO

                                } else if (sensorDataArray[i] < 20) {

                                    proximity[i].setImageResource(R.drawable.mid);

                                } else if (sensorDataArray[i] < 50) {

                                    proximity[i].setImageResource(R.drawable.far);

                                } else {
                                    proximity[i].setImageDrawable(null);
                                }
                            }


                        }


                        recDataString.delete(0, recDataString.length());                    //clear all string data
                        // strIncom =" ";
                        dataInPrint = " ";
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        //getDeviceBTUUID();


    }


    /*private class WebViewClient extends android.webkit.WebViewClient {

        String javascript = "javascript: document.getElementById('imgvideo');";

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            handler.proceed("admin", "admin");
        }





    }*/


    public void play3DSound(SoundPositionObject pos) {

        gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        gvrAudioEngine.preloadSoundFile(BEEP);
        sourceId = gvrAudioEngine.createSoundObject(BEEP);
        gvrAudioEngine.setSoundObjectPosition(
                sourceId, pos.getX(), pos.getY(), pos.getZ());
        gvrAudioEngine.playSound(sourceId, false);

    }

    private void getDeviceBTUUID() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        Method getUuidsMethod = null;
        try {
            getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        ParcelUuid[] uuids = new ParcelUuid[0];
        try {
            uuids = (ParcelUuid[]) getUuidsMethod.invoke(adapter, null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        Log.d("UUID", uuids[0].toString());

        /*for (ParcelUuid uuid: uuids) {
            Log.d("UUID","UUID: " + uuid.getUuid().toString());
        }*/
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        Log.d("ADDRESS", address);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);


        try {
            btSocket = createBluetoothSocket(device);
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
            }
        }
        // Establish the Bluetooth socket connection.
        try {
            btSocket.connect();
            //Toast.makeText(this, "Connected to: " + btSocket.getRemoteDevice(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();

        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called

        mConnectedThread.write("x");  //TODO uncomment pls
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {

        gvrAudioEngine.setHeadRotation(0, 0, 0, 0);
        // Regular update call to GVR audio engine.
        gvrAudioEngine.update();

    }

    @Override
    public void onDrawEye(Eye eye) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {


    }

    protected void updateModelPosition() {


        // Update the sound location to match it with the new cube position.
        if (sourceId != GvrAudioEngine.INVALID_ID) {
            gvrAudioEngine.setSoundObjectPosition(
                    sourceId, 0.0f, 0.0f, 0.0f);
        }
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        // Start spatial audio playback of OBJECT_SOUND_FILE at the model position. The
                        // returned sourceId handle is stored and allows for repositioning the sound object
                        // whenever the cube position changes.
                        gvrAudioEngine.preloadSoundFile(OBJECT_SOUND_FILE);
                        sourceId = gvrAudioEngine.createSoundObject(OBJECT_SOUND_FILE);
                        gvrAudioEngine.setSoundObjectPosition(
                                sourceId, 0.0f, 0.0f, 0.0f);
                        gvrAudioEngine.playSound(sourceId, true /* looped playback */);
                        // Preload an unspatialized sound to be played on a successful trigger on the cube.
                    }
                })
                .start();

        updateModelPosition();

    }

    @Override
    public void onRendererShutdown() {

    }


    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    //Log.d("ARDUINO_DATA", readMessage);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);
                mmOutStream.flush(); //write bytes over BT connection via outstream

            } catch (IOException e) {
                //if you cannot write, close the application
                e.printStackTrace();
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
}
