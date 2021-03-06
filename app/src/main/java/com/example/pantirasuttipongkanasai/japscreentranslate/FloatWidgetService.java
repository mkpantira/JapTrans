package com.example.pantirasuttipongkanasai.japscreentranslate;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;



import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.ColorInfo;
import com.google.api.services.vision.v1.model.DominantColorsAnnotation;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
//import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.ImageProperties;
import com.google.api.services.vision.v1.model.SafeSearchAnnotation;
import com.google.api.services.vision.v1.model.Vertex;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.example.pantirasuttipongkanasai.japscreentranslate.R.id.image;


public class FloatWidgetService extends Service {

    private WindowManager windowManager;
    private RelativeLayout floatwidgetView, removeView;
    private ImageView floatwidgetImg, removeImg;
    private Point szWindow = new Point();
    private int x_init_cord, y_init_cord, x_init_margin, y_init_margin;
    private boolean isLeft = true;
    public static int REQUEST_MEDIA_PROJECTION = 4567;
    private MediaProjectionManager mMediaProjectionManager;
    private boolean isOverlay = false;
    private int displayWidth;
    private int mDisplayHeight;
    private int mDensityDpi;
    public int mResultCode;
    public Intent mResultData;
    public MediaProjection mMediaProjection;
    private ImageReader mImageReader;
    public VirtualDisplay mVirtualDisplay;
    private final static String CLOUD_VISION_API_KEY = "AIzaSyCVAhzmQLI-f-Y18X81WzW8LGWQaXsNe68";
    private Feature feature;
    private String[] visionAPI = new String[]{"TEXT_DETECTION"};
    private String api = visionAPI[0];
    private List<String> result_words_list;
    private List<List<Vertex>> result_verticesList_list;

    public FloatWidgetService() {
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Log.d("test", "onCreate()");

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub


        mResultCode = intent.getIntExtra("mResultCode",0);
        mResultData = intent;


        Log.d("test","on start");
        if(startId == Service.START_STICKY) {
            handleStart();
            return super.onStartCommand(intent, flags, startId);
        }else{
            return  Service.START_NOT_STICKY;

        }

    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void handleStart() {
        Log.d("test","handle start");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        removeView = (RelativeLayout) inflater.inflate(R.layout.remove, null);
        WindowManager.LayoutParams paramRemove = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        paramRemove.gravity = Gravity.TOP | Gravity.LEFT;


        removeView.setVisibility(View.GONE);
        removeImg = removeView.findViewById(R.id.remove_img);
        windowManager.addView(removeView, paramRemove);


        floatwidgetView = (RelativeLayout) inflater.inflate(R.layout.floatingwidget, null);
        floatwidgetImg = floatwidgetView.findViewById(R.id.floating_widget_img);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;
        windowManager.addView(floatwidgetView, params);

        floatwidgetView.setOnTouchListener(new View.OnTouchListener() {
            long time_start = 0, time_end = 0;
            boolean isLongclick = false, inBounded = false;
            int remove_img_width = 0, remove_img_height = 0;

            Handler handler_longClick = new Handler();
            Runnable runnable_longClick = new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub

                    isLongclick = true;
                    removeView.setVisibility(View.VISIBLE);
                    floatwidget_longclick();
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) floatwidgetView.getLayoutParams();

                int x_cord = (int) event.getRawX();
                int y_cord = (int) event.getRawY();
                int x_cord_Destination, y_cord_Destination;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        time_start = System.currentTimeMillis();
                        handler_longClick.postDelayed(runnable_longClick, 600);

                        remove_img_width = removeImg.getLayoutParams().width;
                        remove_img_height = removeImg.getLayoutParams().height;

                        x_init_cord = x_cord;
                        y_init_cord = y_cord;

                        x_init_margin = layoutParams.x;
                        y_init_margin = layoutParams.y;

                        Log.d("Test", "Action Down");

                        break;
                    case MotionEvent.ACTION_MOVE:
                        int x_diff_move = x_cord - x_init_cord;
                        int y_diff_move = y_cord - y_init_cord;

                        x_cord_Destination = x_init_margin + x_diff_move;
                        y_cord_Destination = y_init_margin + y_diff_move;

                        if (isLongclick) {
                            int x_bound_left = szWindow.x / 2 - (int) (remove_img_width * 1.5);
                            int x_bound_right = szWindow.x / 2 + (int) (remove_img_width * 1.5);
                            int y_bound_top = szWindow.y - (int) (remove_img_height * 1.5);

                            if ((x_cord >= x_bound_left && x_cord <= x_bound_right) && y_cord >= y_bound_top) {
                                inBounded = true;

                                int x_cord_remove = (int) ((szWindow.x - (remove_img_height * 1.5)) / 2);
                                int y_cord_remove = (int) (szWindow.y - ((remove_img_width * 1.5) + getStatusBarHeight()));

                                if (removeImg.getLayoutParams().height == remove_img_height) {
                                    removeImg.getLayoutParams().height = (int) (remove_img_height * 1.5);
                                    removeImg.getLayoutParams().width = (int) (remove_img_width * 1.5);

                                    WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();
                                    param_remove.x = x_cord_remove;
                                    param_remove.y = y_cord_remove;

                                    windowManager.updateViewLayout(removeView, param_remove);
                                }

                                layoutParams.x = x_cord_remove + (Math.abs(removeView.getWidth() - floatwidgetView.getWidth())) / 2;
                                layoutParams.y = y_cord_remove + (Math.abs(removeView.getHeight() - floatwidgetView.getHeight())) / 2;

                                windowManager.updateViewLayout(floatwidgetView, layoutParams);
                                break;
                            } else {
                                inBounded = false;
                                removeImg.getLayoutParams().height = remove_img_height;
                                removeImg.getLayoutParams().width = remove_img_width;

                                WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();
                                int x_cord_remove = (szWindow.x - removeView.getWidth()) / 2;
                                int y_cord_remove = szWindow.y - (removeView.getHeight() + getStatusBarHeight());

                                param_remove.x = x_cord_remove;
                                param_remove.y = y_cord_remove;

                                windowManager.updateViewLayout(removeView, param_remove);
                            }

                        }


                        layoutParams.x = x_cord_Destination;
                        layoutParams.y = y_cord_Destination;

                        windowManager.updateViewLayout(floatwidgetView, layoutParams);
                        break;
                    case MotionEvent.ACTION_UP:
                        isLongclick = false;
                        removeView.setVisibility(View.GONE);
                        removeImg.getLayoutParams().height = remove_img_height;
                        removeImg.getLayoutParams().width = remove_img_width;
                        handler_longClick.removeCallbacks(runnable_longClick);


                        if (inBounded) {
                            stopService(new Intent(FloatWidgetService.this, FloatWidgetService.class));
                            inBounded = false;
                            break;
                        }


                        int x_diff = x_cord - x_init_cord;
                        int y_diff = y_cord - y_init_cord;

                        if (Math.abs(x_diff) < 5 && Math.abs(y_diff) < 5) {
                            time_end = System.currentTimeMillis();
                            if ((time_end - time_start) < 300) {
                                floatwidget_click();
                            }
                        }

                        y_cord_Destination = y_init_margin + y_diff;

                        int BarHeight = getStatusBarHeight();
                        if (y_cord_Destination < 0) {
                            y_cord_Destination = 0;
                        } else if (y_cord_Destination + (floatwidgetView.getHeight() + BarHeight) > szWindow.y) {
                            y_cord_Destination = szWindow.y - (floatwidgetView.getHeight() + BarHeight);
                        }
                        layoutParams.y = y_cord_Destination;

                        inBounded = false;
                        resetPosition(x_cord);

                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        WindowManager.LayoutParams paramsTxt = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        paramsTxt.gravity = Gravity.TOP | Gravity.LEFT;

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        // in case horizontal
        super.onConfigurationChanged(newConfig);

        if(windowManager == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) floatwidgetView.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {


            if(layoutParams.y + (floatwidgetView.getHeight() + getStatusBarHeight()) > szWindow.y){
                layoutParams.y = szWindow.y- (floatwidgetView.getHeight() + getStatusBarHeight());
                windowManager.updateViewLayout(floatwidgetView, layoutParams);
            }

            if(layoutParams.x != 0 && layoutParams.x < szWindow.x){
                resetPosition(szWindow.x);
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){


            if(layoutParams.x > szWindow.x){
                resetPosition(szWindow.x);
            }

        }

    }


    private void resetPosition(int x_cord_now) {
        if(x_cord_now <= szWindow.x / 2){
            isLeft = true;
            moveToLeft(x_cord_now);
        } else {
            isLeft = false;
            moveToRight(x_cord_now);
        }
    }

    private void moveToLeft(final int x_cord_now){
        final int x = szWindow.x - x_cord_now;

        new CountDownTimer(500, 5) {
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) floatwidgetView.getLayoutParams();
            public void onTick(long t) {
                long step = (500 - t)/5;
                mParams.x = 0 - (int)(double)bounceValue(step, x );
                windowManager.updateViewLayout(floatwidgetView, mParams);
            }
            public void onFinish() {
                mParams.x = 0;
                windowManager.updateViewLayout(floatwidgetView, mParams);
            }
        }.start();
    }
    private  void moveToRight(final int x_cord_now){
        new CountDownTimer(500, 5) {
            WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) floatwidgetView.getLayoutParams();
            public void onTick(long t) {
                long step = (500 - t)/5;
                mParams.x = szWindow.x + (int)(double)bounceValue(step, x_cord_now) - floatwidgetView.getWidth();
                windowManager.updateViewLayout(floatwidgetView, mParams);
            }
            public void onFinish() {
                mParams.x = szWindow.x - floatwidgetView.getWidth();
                windowManager.updateViewLayout(floatwidgetView, mParams);
            }
        }.start();
    }


    private double bounceValue(long step, long scale){
//        double value = scale * java.lang.Math.exp(-0.055 * step) * java.lang.Math.cos(0.08 * step);
        double value = scale * java.lang.Math.exp(-0.055 * step);
        return value;
    }

    private int getStatusBarHeight() {
        int statusBarHeight = (int) Math.ceil(25 * getApplicationContext().getResources().getDisplayMetrics().density);
        return statusBarHeight;
    }

    private void floatwidget_click(){
        Log.d("test","click on floating widget");


        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mDensityDpi = metrics.densityDpi;
        displayWidth = metrics.widthPixels;
        mDisplayHeight = metrics.heightPixels;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode,mResultData);
        mImageReader = ImageReader.newInstance(displayWidth, mDisplayHeight, ImageFormat.RGB_565, 2);
//        mImageReader = ImageReader.newInstance(displayWidth, mDisplayHeight, ImageFormat.JPEG, 2);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture", displayWidth, mDisplayHeight, mDensityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
        android.media.Image image = null;


        while(image == null){
            image = mImageReader.acquireLatestImage();
        }

        final android.media.Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int offset = 0;
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * displayWidth;
        String ImagePath;
        Uri URI;

        Bitmap bmp = Bitmap.createBitmap(displayWidth+rowPadding/pixelStride, mDisplayHeight, Bitmap.Config.RGB_565);
        bmp.copyPixelsFromBuffer(buffer);

        image.close();
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        mMediaProjection.stop();
//        floatwidgetImg.setImageBitmap(bmp);

        feature = new Feature();
        feature.setType(visionAPI[0]);
        feature.setMaxResults(10);
        callCloudVision(bmp, feature);

        image = null;

//        if(!isOverlay){
//            isOverlay = true;
//            Intent it = new Intent(FloatWidgetService.this, OverlayService.class);
//            startService(it);
//        }else {
//            isOverlay = false;
//            stopService(new Intent(FloatWidgetService.this, OverlayService.class));
//
//        }
    }

    private void floatwidget_longclick(){

        WindowManager.LayoutParams param_remove = (WindowManager.LayoutParams) removeView.getLayoutParams();
        int x_cord_remove = (szWindow.x - removeView.getWidth()) / 2;
        int y_cord_remove = szWindow.y - (removeView.getHeight() + getStatusBarHeight() );

        param_remove.x = x_cord_remove;
        param_remove.y = y_cord_remove;

        windowManager.updateViewLayout(removeView, param_remove);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Log.d("test","on destroy");

        if(floatwidgetView != null){
            windowManager.removeView(floatwidgetView);
        }


        if(removeView != null){
            windowManager.removeView(removeView);
        }

    }

    private void callCloudVision(final Bitmap bitmap, final Feature feature){
        final List<Feature> featureList = new ArrayList<>();
        featureList.add(feature);

        final List<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();

        AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
        annotateImageReq.setFeatures(featureList);
        annotateImageReq.setImage(getImageEncodeImage(bitmap));
        annotateImageRequests.add(annotateImageReq);

        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {

                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(annotateImageRequests);

                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d("TAG", "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d("TAG", "failed to make API request because of other IOException " + e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {

                String allSentence = result_words_list.get(0);
                int words_size = result_words_list.size()-1 ;

                List<String> sentence = new ArrayList<>();
                String temp_sentence = "";
                for (int count = 1; count <= words_size; count++){

                    if(count == 1) {
                        temp_sentence = result_words_list.get(count).toString();

                    }else {
                        Log.d("sentence",result_words_list.get(count).toString());
                        int x1A = result_verticesList_list.get(count-1).get(0).getX();
                        int x2A = result_verticesList_list.get(count-1).get(1).getX();
                        int x3A = result_verticesList_list.get(count-1).get(2).getX();
                        int x4A = result_verticesList_list.get(count-1).get(3).getX();

                        int y1A = result_verticesList_list.get(count-1).get(0).getY();
                        int y2A = result_verticesList_list.get(count-1).get(1).getY();
                        int y3A = result_verticesList_list.get(count-1).get(2).getY();
                        int y4A = result_verticesList_list.get(count-1).get(3).getY();

                        int x1B = result_verticesList_list.get(count).get(0).getX();
                        int x2B = result_verticesList_list.get(count).get(1).getX();
                        int x3B = result_verticesList_list.get(count).get(2).getX();
                        int x4B = result_verticesList_list.get(count).get(3).getX();

                        int y1B = result_verticesList_list.get(count).get(0).getY();
                        int y2B = result_verticesList_list.get(count).get(1).getY();
                        int y3B = result_verticesList_list.get(count).get(2).getY();
                        int y4B = result_verticesList_list.get(count).get(3).getY();

                        if(y1B > y4A){
                            //maybe new line
                            if((x4A > x4B) && ((y4A + (y4B - y1B) ) < y1B)){

                                //new line
                                sentence.add(temp_sentence);
                                temp_sentence = result_words_list.get(count).toString();

                            }else if ((y4A + (y4B - y1B) ) > y1B){
                                //same line
                                temp_sentence = temp_sentence + result_words_list.get(count).toString();

                            }else{
                                //same line
                                temp_sentence = temp_sentence + result_words_list.get(count).toString();
                            }

                        }else{
                            //same line
                            temp_sentence = temp_sentence + result_words_list.get(count).toString();

                        }

                    }


                }


                sentence.add(temp_sentence);


                Log.d("sentence",sentence.toString());

//                for(int i=0; i < sentence.size() ; i++) {
//                    Log.d("sentence", sentence.get(i));
//                }
            }



        }.execute();

    }
    @NonNull
    private com.google.api.services.vision.v1.model.Image getImageEncodeImage(Bitmap bitmap) {
        com.google.api.services.vision.v1.model.Image base64EncodedImage = new com.google.api.services.vision.v1.model.Image();

        // Convert the bitmap to a JPEG
        // Just in case it's a format that Android understands but Cloud Vision
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {

        AnnotateImageResponse imageResponses = response.getResponses().get(0);

        List<EntityAnnotation> entityAnnotations;

        String message = "";
        switch (api) {
            case "TEXT_DETECTION":
                entityAnnotations = imageResponses.getTextAnnotations();
                message = formatAnnotation(entityAnnotations);
                Log.d("result",entityAnnotations.toString());
                break;
        }
        return message;
    }

    private String formatAnnotation(List<EntityAnnotation> entityAnnotation) {
        String message = "{";
//        int numOfResult = 0;

        if (entityAnnotation != null) {
            result_words_list =  new ArrayList<>();
            result_verticesList_list = new ArrayList<>();
            for (EntityAnnotation entity : entityAnnotation) {
                result_words_list.add(entity.getDescription());
                result_verticesList_list.add(entity.getBoundingPoly().getVertices());

                message = message + "    " + entity.getDescription() + " " + entity.getBoundingPoly().getVertices();
                message += "\n";

            }
            Log.d("messages",message);
            Log.d("words",result_words_list.toString());
            Log.d("words",Integer.toString(result_words_list.size()));
            Log.d("vertices",Integer.toString(result_verticesList_list.size()));
            Log.d("vertices",result_verticesList_list.toString());
        } else {
            message = "Nothing Found";
        }
        return message;
    }

}
