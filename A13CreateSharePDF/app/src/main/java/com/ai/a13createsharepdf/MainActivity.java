package com.ai.a13createsharepdf;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/*
Author : Aaviskar Infotech
Project inquiry accepted
Contact: aaviskarinfotech@gmail.com
Website: www.aaviskar.com
*/


public class MainActivity extends AppCompatActivity {

    EditText edt;
    Button btn;
    ConstraintLayout container;

    String[] required_permissions = new String[]{
            android.Manifest.permission.READ_MEDIA_IMAGES
    };

    boolean is_storage_image_permitted = false;

    String TAG = "Aaviskar";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edt=(EditText)findViewById(R.id.edt);
        btn = (Button) findViewById(R.id.btn);
        container = (ConstraintLayout) findViewById(R.id.container);

        //===== for study permission related to android 13
        //===== please watch our previous video..

        if (!is_storage_image_permitted) {
            requestPermissionStorageImages();
        }


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPDFandShare();
            }
        });

    }


    //~~~ Step 1
    public Bitmap captureScreenShot(View view)
    {
        Bitmap returnBitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(returnBitmap);
        Drawable bgdrawable=view.getBackground();
        if(bgdrawable!=null)
            bgdrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return returnBitmap;
    }

    //~~~ Step 2
    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }


    //~~~ Step 3
    public void createPDF(OutputStream ref_outst) {
        // create a new document
        PdfDocument document = new PdfDocument();

        /**Dimension For A4 Size Paper (1 inch = 72 points)**/
        int PDF_PAGE_WIDTH = 595; //8.26 Inch
        int PDF_PAGE_HEIGHT = 842; //11.69 Inch

        // create a page description
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, 1).create();

        // start a page
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        int desire_text_size=20;
        paint.setTextSize(desire_text_size);

        String text="Default Text aaviskarinfotech@gmail.com";
        if(edt.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Default Text will be printed",Toast.LENGTH_SHORT);
        }
        else {
            text=edt.getText().toString().trim();
        }

        float cord_x_text = 30;
        float cord_y_text = 50;

        canvas.drawText(text, cord_x_text, cord_y_text, paint);

        float cord_x_image = 30;
        float cord_y_image = 80;
        Bitmap screenshot = captureScreenShot(container);
        Bitmap resizedImage = getResizedBitmap(screenshot, 600);
        canvas.drawBitmap(resizedImage,cord_x_image,cord_y_image,paint);

        // finish the page
        document.finishPage(page);

        try {
            // write the document content
            document.writeTo(ref_outst);

            // close the document
            document.close();

            Toast.makeText(this, "PDF is saved", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //~~~ step 4
    public void createPDFandShare() {
        OutputStream outst;
        try {
            //=== scoped storage is support after Q
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver contentResolver = getContentResolver();
                ContentValues contentValues = new ContentValues();

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
                LocalDateTime now = LocalDateTime.now();
                String date_suffix="_"+dtf.format(now);
                Log.d(TAG,dtf.format(now));

                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Aaviskar"+date_suffix + ".pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOCUMENTS + File.separator + "AaviskarFolder");
                Uri pdfUri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues);

                outst = contentResolver.openOutputStream(Objects.requireNonNull(pdfUri));
                Objects.requireNonNull(outst);

                //code to create pdf
                createPDF(outst);

                //=== now intent to share image
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("application/pdf");
                share.putExtra(Intent.EXTRA_STREAM, pdfUri);
                startActivity(Intent.createChooser(share, "Share PDF"));

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    //~~~~~~ code for read storage media images starts ~~~~~~
    public void requestPermissionStorageImages() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, required_permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, required_permissions[0] + " Granted");
            is_storage_image_permitted = true;
        } else {
            //new android 13 code after onActivityResult is deprecated, now ActivityResultLauncher..
            request_permission_launcher_storage_images.launch(required_permissions[0]);
        }

    }

    private ActivityResultLauncher<String> request_permission_launcher_storage_images =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Log.d(TAG, required_permissions[0] + " Granted");
                            is_storage_image_permitted = true;

                        } else {
                            Log.d(TAG, required_permissions[0] + " Not Granted");
                            is_storage_image_permitted = false;
                        }
                    });
    //~~~~~~ code for read storage media images ends ~~~~~~

}