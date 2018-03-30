package com.demo.cjh.facedemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facedetection.AFD_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.guo.android_extend.image.ImageConverter;

import java.util.ArrayList;
import java.util.List;

public class Register extends AppCompatActivity {

    private final String TAG = this.getClass().toString();
    private ImageView imageView;
    private ImageView imageView2;
    String photoPath ;

    private AFR_FSDKFace mAFR_FSDKFace ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);

        Intent intent = getIntent();
        photoPath = intent.getStringExtra("photoPath");
        Log.d("photoPath",photoPath);

        imageView = (ImageView) findViewById(R.id.id_image);
        imageView2 = (ImageView) findViewById(R.id.id_image2);

        Bitmap bp = App.decodeImage(photoPath);
        imageView.setImageBitmap(bp);

        // 把图形格式转化为软虹SDK使用的图像格式NV21
        byte[] data = new byte[bp.getWidth() * bp.getHeight() * 3 / 2];
        ImageConverter convert = new ImageConverter();
        convert.initial(bp.getWidth(), bp.getHeight(), ImageConverter.CP_PAF_NV21);
        if (convert.convert(bp, data)) {
            Log.d(TAG, "convert ok!");
        }
        convert.destroy();

        AFD_FSDKEngine FD_engine = new AFD_FSDKEngine();
        AFD_FSDKVersion FD_version = new AFD_FSDKVersion();

        // 用来存放检测到的人脸信息列表
        List<AFD_FSDKFace> FD_result = new ArrayList<AFD_FSDKFace>();

        // 初始化人脸检测引擎
        AFD_FSDKError FD_error = FD_engine.AFD_FSDK_InitialFaceEngine(FaceDB.appid,FaceDB.fd_key,AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);

        if(FD_error.getCode() != AFD_FSDKError.MOK ){
            Toast.makeText(Register.this, "FD初始化失败，错误码：" + FD_error.getCode(), Toast.LENGTH_SHORT).show();
        }

        // 输入的data数据为NV21格式，人脸检测返回结果保存在FD_result中
        FD_error = FD_engine.AFD_FSDK_StillImageFaceDetection(data,bp.getWidth(), bp.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, FD_result);


        // 画box框
        Bitmap bitmap = Bitmap.createBitmap(bp.getWidth(), bp.getHeight(), bp.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bp, 0, 0, null);

        Paint mPaint = new Paint();
        for (AFD_FSDKFace face : FD_result) {
            mPaint.setColor(Color.RED);
            mPaint.setStrokeWidth(10.0f);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(face.getRect(), mPaint);
        }
//            canvas.restore();

        bp = bitmap;
        imageView.setImageBitmap(bp);

        if(!FD_result.isEmpty()) {

            // 检测人脸特征信息
            AFR_FSDKVersion FR_version1 = new AFR_FSDKVersion();
            AFR_FSDKEngine FR_engine1 = new AFR_FSDKEngine();

            // 存放人脸特征信息
            AFR_FSDKFace FR_result1 = new AFR_FSDKFace();

            // 初始化
            AFR_FSDKError FR_error1 = FR_engine1.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);

            if(FR_error1.getCode() != AFR_FSDKError.MOK ){
                Toast.makeText(Register.this, "FR初始化失败，错误码：" + FD_error.getCode(), Toast.LENGTH_SHORT).show();
            }

            // 检测人脸特征
            FR_error1 = FR_engine1.AFR_FSDK_ExtractFRFeature(data,bp.getWidth(),bp.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(FD_result.get(0).getRect()), FD_result.get(0).getDegree(), FR_result1);

            if(FR_error1.getCode() != AFR_FSDKError.MOK){
                Toast.makeText(Register.this, "人脸特征无法检测，请换一张图片", Toast.LENGTH_SHORT).show();
            }else{

                mAFR_FSDKFace = FR_result1.clone(); // 复制

                //  裁剪
                int width = FD_result.get(0).getRect().width();
                int height = FD_result.get(0).getRect().height();
                Bitmap face_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas face_canvas = new Canvas(face_bitmap);
                face_canvas.drawBitmap(bp, FD_result.get(0).getRect(), new Rect(0, 0, width, height), null);

                // 显示
                imageView2.setImageBitmap(face_bitmap);

                // 添加人脸特征信息到脸库
                // App.mFaceDB.addFace("name",mAFR_FSDKFace);

            }
            // 销毁
            FR_error1 = FR_engine1.AFR_FSDK_UninitialEngine();


        }else{
            Toast.makeText(Register.this, "未检测到人脸", Toast.LENGTH_SHORT).show();
        }

        FD_error = FD_engine.AFD_FSDK_UninitialFaceEngine();




    }
}
