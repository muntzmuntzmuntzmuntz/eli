package com.mememe.memessenger.processors;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.mememe.memessenger.MainActivity;
import com.mememe.memessenger.PaintView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

public class Save {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void execute(ContentResolver contentResolver, Context context, PaintView paintView){
        LocalDateTime time = LocalDateTime.now();

        paintView.setDrawingCacheEnabled(true);
        paintView.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(paintView.getDrawingCache());
        paintView.setDrawingCacheEnabled(false);

        try {
            File parent = new File(context.getFilesDir()+"/Memessenger/");

            if (!parent.exists())
                parent.mkdir();
////
//            if (!image.exists())
//                image.createNewFile();

            File image = new File(parent,time.toString());

            MediaStore.Images.Media.insertImage(contentResolver,bitmap,image.getName(),"Image saved from Memessenger. Create more and have fun!!!");
            Log.i(">> SAVE","CREATING FILE AT "+image.getPath());

        }catch (Exception e){
            Log.e(">> SAVE",""+e);
        }

    }
}
