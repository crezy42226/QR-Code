package com.crezy.qrcodein;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.Map;

import static android.Manifest.permission_group.CAMERA;

public class MainActivity extends AppCompatActivity {

    private final String tag = "QRCGEN";
    private final int REQUEST_PERMISSION = 0xf0;

    private MainActivity self;
    private Snackbar snackbar;
    private Bitmap qrImage;

    private EditText txtQRText;
    private TextView txtSaveHint;
    private Button btnGenerate, btnReset,btnShare,btnscan;
    private ImageView imgResult;
    private ProgressBar loader;
    private AdView mAdView;
    public static TextView textViewresolt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, "ca-app-pub-3677695007403196~1751055394");
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);



        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},1);
        self = this;

        txtQRText   = (EditText)findViewById(R.id.txtQR);
        txtSaveHint = (TextView) findViewById(R.id.txtSaveHint);
        btnGenerate = (Button)findViewById(R.id.btnGenerate);
        btnReset    = (Button)findViewById(R.id.btnReset);
        imgResult   = (ImageView)findViewById(R.id.imgResult);
        loader      = (ProgressBar)findViewById(R.id.loader);
        textViewresolt= findViewById(R.id.txtresolt);
        btnscan = findViewById(R.id.btnscan);
        btnscan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ScanActivity.class);
                startActivity(intent);
            }
        });
        btnShare = findViewById(R.id.btnshare);

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Drawable drawable = imgResult.getDrawable();
                Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();


                try {

                    File file = new File(MainActivity.this.getExternalCacheDir(),"MyImage.png");
                    FileOutputStream fout= new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG,80,fout);
                    fout.flush();
                    fout.close();
                    file.setReadable(true,false);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    intent.setType("image/png");
                    startActivity(Intent.createChooser(intent,"share imge"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "error", Toast.LENGTH_SHORT).show();
                }catch (IOException e){

                    e.printStackTrace();

                }catch (Exception e){
                    e.printStackTrace();

                }

            }
        });

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                self.generateImage();
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                self.reset();
            }
        });

        imgResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                self.confirm("", "حفظ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveImage();
                    }
                });
            }
        });

        txtQRText.setHint("ضع الرابط او النص هنا");
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage();
            } else {
                alert("");
            }
        }
    }

    private void saveImage() {
        if (qrImage == null) {
            alert("تم الحفظ في الاستوديو.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
            return;
        }


        String fname = "qrcode-" + Calendar.getInstance().getTimeInMillis();
        boolean success = true;
        try {
            String result = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    qrImage,
                    fname,
                    "QRCode Image"
            );
            if (result == null) {
                success = false;
            } else {
                Log.e(tag, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        }

        if (!success) {
            alert("");
        } else {
            self.snackbar("تم الحفظ في الاستوديو.");
        }
    }

    private void alert(String message){
        AlertDialog dlg = new AlertDialog.Builder(self)
                .setTitle("QRCode Generator")
                .setMessage(message)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dlg.show();
    }

    private void confirm(String msg, String yesText, final AlertDialog.OnClickListener yesListener) {
        AlertDialog dlg = new AlertDialog.Builder(self)
                .setTitle("هل تريد حفظ الباركود")
                .setMessage(msg)
                .setNegativeButton("الغاء", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(yesText, yesListener)
                .create();
        dlg.show();
    }

    private void snackbar(String msg) {
        if (self.snackbar != null) {
            self.snackbar.dismiss();
        }

        self.snackbar = Snackbar.make(
                findViewById(R.id.mainBody),
                msg, Snackbar.LENGTH_SHORT);

        self.snackbar.show();
    }

    private void endEditing(){
        txtQRText.clearFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }


    private void generateImage(){
        final String text = txtQRText.getText().toString();
        if(text.trim().isEmpty()){
            alert("ok");
            return;
        }

        endEditing();
        showLoadingVisible(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int size = imgResult.getMeasuredWidth();
                if( size > 1){
                    Log.e(tag, "size is set manually");
                    size = 260;
                }

                Map<EncodeHintType, Object> hintMap = new EnumMap<>(EncodeHintType.class);
                hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                hintMap.put(EncodeHintType.MARGIN, 1);
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                try {
                    BitMatrix byteMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size,
                            size, hintMap);
                    int height = byteMatrix.getHeight();
                    int width = byteMatrix.getWidth();
                    self.qrImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    for (int x = 0; x < width; x++){
                        for (int y = 0; y < height; y++){
                            qrImage.setPixel(x, y, byteMatrix.get(x,y) ? Color.BLACK : Color.WHITE);
                        }
                    }

                    self.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            self.showImage(self.qrImage);
                            self.showLoadingVisible(false);
                            self.snackbar("QRCode telah dibuat");
                        }
                    });
                } catch (WriterException e) {
                    e.printStackTrace();
                    alert(e.getMessage());
                }
            }
        }).start();
    }

    private void showLoadingVisible(boolean visible){
        if(visible){
            showImage(null);
        }

        loader.setVisibility(
                (visible) ? View.VISIBLE : View.GONE
        );
    }

    private void reset(){
        txtQRText.setText("");
        showImage(null);
        endEditing();
    }

    private void showImage(Bitmap bitmap) {
        if (bitmap == null) {
            imgResult.setImageResource(android.R.color.transparent);
            qrImage = null;
            txtSaveHint.setVisibility(View.GONE);
        } else {
            imgResult.setImageBitmap(bitmap);
            txtSaveHint.setVisibility(View.VISIBLE);
        }
    }
    }

