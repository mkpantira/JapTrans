package com.example.pantirasuttipongkanasai.japscreentranslate;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static Button activateButton;
    public static int OVERLAY_PERMISSION_REQ_CODE_FLOAT_WIDGET = 1234;
    public static int OVERLAY_PERMISSION_REQ_CODE_SCREENSHOT = 4567;
    public MediaProjectionManager mMediaProjectionManager;
    public int mScreenDensity;
    public int mResultCode;
    public Intent mResultData;
    private boolean  isGivePermissionRecordScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activateButton = (Button) findViewById(R.id.activate_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            //If the draw over permission is not available then open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE_FLOAT_WIDGET);

        } else {

            initializeView();
        }


    }

    private void initializeView() {
        Log.d("test","ini view");
        activateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("test","activate button");
                if(!isGivePermissionRecordScreen) {
                    Log.d("test","not give per yet");
                    Activity activity = MainActivity.this;
                    mMediaProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), OVERLAY_PERMISSION_REQ_CODE_SCREENSHOT);

                }else {
                    Log.d("test","already gave per");
                    Log.d("test","mresultcode" + mResultCode);
                    Log.d("test","mresultdata" + mResultData);
//                    mResultDataIntent it = new Intent(MainActivity.this, FloatWidgetService.class);
//                    startService(it);
                    Intent it = new Intent(MainActivity.this, FloatWidgetService.class);
                    it.putExtra("mResultCode",mResultCode);
                    it.putExtras(mResultData);
                    startService(it);
                }
                //finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == OVERLAY_PERMISSION_REQ_CODE_FLOAT_WIDGET) {

            initializeView();

        } else if( requestCode == OVERLAY_PERMISSION_REQ_CODE_SCREENSHOT) {
            Log.d("test","permission record");
            Log.d("test",resultCode + "");
            if(resultCode == Activity.RESULT_OK) {
                Log.d("test","psermission true");
                isGivePermissionRecordScreen = true;
                mResultCode = resultCode;
                mResultData = data;
                Log.d("test","mresultcode" + mResultCode);
                Log.d("test","mresultdata" + mResultData);
                Intent it = new Intent(MainActivity.this, FloatWidgetService.class);
                it.putExtra("mResultCode",resultCode);
                it.putExtras(data);
                startService(it);
            }else{
                Log.d("test","psermission false");
                isGivePermissionRecordScreen = false;
            }

        } else {

            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}