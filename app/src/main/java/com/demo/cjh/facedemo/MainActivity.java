package com.demo.cjh.facedemo;

import android.annotation.SuppressLint;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.arcsoft.facerecognition.AFR_FSDKEngine;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().toString();

    private static final int REQUEST_CODE_IMAGE_CAMERA = 1;  // 相机
    private static final int REQUEST_CODE_IMAGE_OP = 2;  // 相册
    private static final int REQUEST_CODE_OP = 3; //

    private Button button1 ;
    private Button button2 ;
    private Button button_test ;
    private Button button_test2 ;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        new Thread(new Runnable() {
            @Override
            public void run() {

                App.mFaceDB.loadFaces();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"脸库加载完成",Toast.LENGTH_SHORT).show();

                    }
                });
            }
        }).start();

    }

    String photoPath ; // 照片路径

    private void init() {
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        button_test = (Button) findViewById(R.id.id_btn3);
        button_test2 = (Button) findViewById(R.id.id_btn4);

        button1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 人脸注册
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("请选择注册方式")
                        .setItems(new String[]{"相册","相机"},new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case 1:
                                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                        ContentValues values = new ContentValues(1);
                                        // 设置图片保存路径 ，默认在Pictures
                                        //values.put(MediaStore.Images.Media.DATA, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+"123.jpg");
                                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                                        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                                        App.setCaptureImage(uri);
                                        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                                        startActivityForResult(intent, REQUEST_CODE_IMAGE_CAMERA);
                                        break;
                                    case 0:


                                        break;
                                    default:;
                                }
                            }
                        }).show();

            }
        });
        button2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 人脸识别

                if( App.mFaceDB.mRegister.isEmpty() ) {
                    Toast.makeText(MainActivity.this, "没有注册人脸，请先注册！", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("请选择相机")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setItems(new String[]{"后置相机", "前置相机"}, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startDetector(which);
                                }
                            })
                            .show();
                }

            }
        });

        button_test.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // 获取SD卡状态
                String state = Environment.getExternalStorageState();
                if(state.equals(Environment.MEDIA_MOUNTED)){
                    photoPath = Environment.getExternalStorageDirectory() + "/face.png";
                    File imageDir = new File(photoPath);
                    if(!imageDir.exists()){
                        // 根据地址生成新的文件
                        try {
                            imageDir.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    //Uri uri = Uri.fromFile(imageDir);
                    Uri uri =  FileProvider.getUriForFile(MainActivity.this, "com.demo.cjh.facedemo.fileprovider", imageDir);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
                    startActivityForResult(intent,11);
                }else {
                    Toast.makeText(MainActivity.this,"SD卡未插入",Toast.LENGTH_SHORT).show();
                }



            }
        });

        button_test2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("请选择相机")
                        .setItems(new String[]{"后置相机", "前置相机"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent it = new Intent(MainActivity.this, Detecter.class);
                                it.putExtra("Camera", which);
                                startActivityForResult(it, REQUEST_CODE_OP);
                            }
                        })
                        .show();
            }
        });
    }

    private void startDetector(int camera) {
        Intent it = new Intent(MainActivity.this, DetecterActivity.class);
        it.putExtra("Camera", camera);
        startActivityForResult(it, REQUEST_CODE_OP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_CODE_IMAGE_CAMERA : //相机
                Uri mPath = App.getCaptureImage();
                Log.d("TAG","Path = "+requestCode);

                String path = getImagePath(mPath,null);
                Bitmap bmp = App.decodeImage(path);
                startRegister(bmp, path);
                Log.d("Path","path = "+path);
                break;

            case REQUEST_CODE_IMAGE_OP :

                break;

            case 11:
                if(resultCode == Activity.RESULT_OK){
                    Intent intent = new Intent(MainActivity.this,Register.class);
                    intent.putExtra("photoPath",photoPath);
                    startActivityForResult(intent,22);
                    //Toast.makeText(MainActivity.this,getImagePath(data.getData(),null),Toast.LENGTH_SHORT).show();
                }
                break;
            case 22:
                Toast.makeText(MainActivity.this,"OK",Toast.LENGTH_SHORT).show();

        }


    }
    /**
     * 获取当前系统时间并格式化
     * @return
     */
    @SuppressLint("SimpleDateFormat")
    public static String getStringToday() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 通过uri和selection来获取真实的图片路径
     * */
    private String getImagePath(Uri uri,String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /**
     * @param mBitmap
     */
    private void startRegister(Bitmap mBitmap, String file) {
        Intent it = new Intent(MainActivity.this, RegisterActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("imagePath", file);
        it.putExtras(bundle);
        startActivityForResult(it, REQUEST_CODE_OP);
    }


}
