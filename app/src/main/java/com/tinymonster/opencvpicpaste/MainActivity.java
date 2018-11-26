package com.tinymonster.opencvpicpaste;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG="MainActivity";
    private ImageView ivImage;
    private Button bClickImage;
    private Button bDone;
    private Uri fileUri;
    private String FILE_LOCATION= Environment.getExternalStorageDirectory().getAbsolutePath()+"/OpencvStudy1/";//文件夹路径
    private static final int  CLICK_PHOTO=1;
    private Bitmap image;
    private List<Mat> clickedImages=new ArrayList<>();
    Mat src;//用于保存最新的一副照片
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED||
                ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            Log.e("MainActivity,请求权限"," ");
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},2);
        }else {
            Log.e("MainActivity,跳转到相机"," ");
            initView();
            Log.e("MainActivity","3");
//            if (!OpenCVLoader.initDebug()) {
//                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, MainActivity.this, mLoaderCallback);
//            } else {
//                Log.d(TAG, "OpenCV library found inside package. Using it!");
//                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//            }
            System.loadLibrary("opencv_java");
            System.loadLibrary("stitcher");
        }
    }
    private void initView(){
        ivImage=(ImageView)findViewById(R.id.ivImage);
        bClickImage=(Button)findViewById(R.id.bClickImage);
        bDone=(Button)findViewById(R.id.bDone);
        bClickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File imagesFolder=new File(FILE_LOCATION);
                imagesFolder.mkdirs();
                File image=new File(imagesFolder,"panorama"+System.currentTimeMillis()+".jpg");//创建一个文件
                fileUri=Uri.fromFile(image);//获取文件URI
                Logger.d("获取的文件URI="+fileUri.toString());
                intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);//设置图像文件名
                startActivityForResult(intent,CLICK_PHOTO);
            }
        });
        bDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(clickedImages.size()==0){
                    Toast.makeText(getApplicationContext(),"没有拍摄任何图像",Toast.LENGTH_SHORT).show();
                }else if(clickedImages.size()==1){
                    Toast.makeText(getApplicationContext(),"只拍摄到一幅图像",Toast.LENGTH_SHORT).show();
                    image=Bitmap.createBitmap(src.cols(),src.rows(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(src,image);
                    ivImage.setImageBitmap(image);
                }else {
                    //执行拼接操作
                    craetePanorama();
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 2:
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED
                        ||ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
                    Log.e("请求权限完成，跳转"," ");
                    initView();
                    System.loadLibrary("opencv_java");
                    System.loadLibrary("stitcher");
                }else {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},2);
                    Log.e("再次请求权限"," ");
                }
                break;
        }
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("MainActivity", "OpenCV loaded successfully");
                    System.loadLibrary("stitcher");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case CLICK_PHOTO:
                    try{
                        Logger.d("接收到一副照片");
                        Log.e(TAG,"接收到一张照片");
                        final InputStream inputStream=getContentResolver().openInputStream(fileUri);
                        final Bitmap selectedImage= BitmapFactory.decodeStream(inputStream);//InputStream->Bitmap
                        src =new Mat(selectedImage.getHeight(),selectedImage.getWidth(), CvType.CV_8UC4);
                        Imgproc.resize(src,src,new Size(src.rows()/4,src.cols()/4));//修改图像尺寸
                        Utils.bitmapToMat(selectedImage,src);
                        Imgproc.cvtColor(src,src,Imgproc.COLOR_BGR2RGB);
                        clickedImages.add(src);
                    }catch (FileNotFoundException e){
                        e.printStackTrace();
                    }
                break;
        }
    }
    private void craetePanorama(){
        new AsyncTask<Void,Void,Bitmap>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Bitmap doInBackground(Void... voids) {
                Mat srcRes=new Mat();
                Log.e(TAG,"clickedImages大小："+clickedImages.size());
                int success=OpenCVCPP.StitchPanorama(clickedImages.toArray(),clickedImages.size(),srcRes.getNativeObjAddr());
                clickedImages.clear();
                Log.e(TAG,"native返回结果："+success);
                if(success==0){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"合成失败",Toast.LENGTH_SHORT).show();
                        }
                    });
                    return null;
                }else {
                    Bitmap bitmap1=Bitmap.createBitmap(srcRes.cols(),srcRes.rows(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(srcRes,bitmap1);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"合成成功",Toast.LENGTH_SHORT).show();
                        }
                    });
                    return bitmap1;
                }

            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                ivImage.setImageBitmap(bitmap);
            }
        }.execute();
    }
}
