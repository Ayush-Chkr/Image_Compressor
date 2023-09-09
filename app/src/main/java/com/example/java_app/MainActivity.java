package com.example.java_app;

import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;


public class MainActivity extends AppCompatActivity {

    //Defining the various elements of the User Interface

    int qual;
    EditText quality;
    ImageView imgOriginal, imgCompressed;
    TextView txtOriginal, txtCompressed, txtHint;
    Button btnCompress, btnPick, btnSave;
    Uri buffer; //to save the path of the selected image.
    Calendar time;
    SimpleDateFormat sdf;
    Bitmap comp;
    byte[] buffer_bytes;

    private StorageVolume storageVolume; //for storage permissions
    int SELECT_PICTURE= 200;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //The below statements grants the permission to write files on the phone's storage.

        ActivityCompat.requestPermissions(this,
                new String[]{READ_MEDIA_IMAGES, WRITE_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);

        //Initializing the elements using their unique "id"'s .
        quality= findViewById(R.id.quality);
        imgOriginal= findViewById(R.id.imgOriginal);
        imgCompressed= findViewById(R.id.imgCompressed);
        txtOriginal= findViewById(R.id.txtOriginal);
        txtCompressed= findViewById(R.id.txtCompressed);
        txtHint= findViewById(R.id.txtHint);
        btnPick= findViewById(R.id.btnPick);
        btnCompress= findViewById(R.id.btnCompress);
        btnSave= findViewById(R.id.btnSave);

        //Statements to access the internal storage of phone.
        StorageManager storageManager = (StorageManager) getSystemService(STORAGE_SERVICE);
        storageVolume = storageManager.getStorageVolumes().get(0);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);


        //Giving functionality to the buttons by adding OnClickListener.
        btnPick.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                openGallery();

            }
        });

        btnCompress.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                // Displaying simple Toast message

                String Q= quality.getText().toString();

                if(Q.isEmpty()) {
                    qual= 50;
                    compress(buffer, qual);
                    if(buffer!=null) {
                        quality.setText("50");
                    }
                }
                else{

                    qual = Integer.parseInt(Q);
                    if(qual>=0 && qual<=100) {
                        if(qual<=3){        //Manipulating values here due to minor inconsistencies in compression when value is below 4.
                            qual= 96;
                        }
                        else {
                            qual = 100 - qual;
                        }
                        compress(buffer, qual);
                        quality.setTextColor(Color.BLACK);
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "Invalid Input! Try Again.", Toast.LENGTH_SHORT).show();
                        quality.setTextColor(Color.RED);
                    }

                }
                quality.clearFocus();


            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(buffer_bytes!=null) {
                    try {
                        saveImage();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Nothing to Save.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imgOriginal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(buffer!=null) {
                    Intent intent1 = new Intent();
                    intent1.setAction(Intent.ACTION_VIEW);
                    intent1.setDataAndType(buffer, "image/*");
                    intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent1);
                }
            }
        });

        imgCompressed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(buffer_bytes!=null) {
                    openImage();
                }

            }
        });


    }

    //saveImage() method to save the image in the Download directory of Internal Storage.
    private void saveImage() throws IOException {
        time= Calendar.getInstance();
        sdf= new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss ", Locale.getDefault());
        String Time_taken= sdf.format(time.getTime());

        String Storage_path= "/Download/"+Time_taken+".jpeg";
        Toast.makeText(getApplicationContext(), Storage_path,Toast.LENGTH_SHORT).show();
        String savePath= String.format(Locale.getDefault(), Storage_path);

        File fileOutput = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

            fileOutput = new File(storageVolume.getDirectory().getPath() + savePath);
            FileOutputStream fileOutputStream = new FileOutputStream(fileOutput);
            fileOutputStream.write(buffer_bytes);
            fileOutputStream.close();
        }
    }



    private void openImage() {
        File tempFile;
        try {
            tempFile = File.createTempFile("temp_image", ".jpg", getCacheDir());
            FileOutputStream out = new FileOutputStream(tempFile);
            comp.compress(Bitmap.CompressFormat.JPEG, qual, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Create an intent to view the image
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile), "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }


    //Compress() method to compress the selected image.
    private void compress(Uri originalUri, int x) {
        try {
            if(buffer!= null) {

                //Creating the bitmap of Image Selected
                Bitmap bitmap2 = BitmapFactory.decodeStream(getContentResolver().openInputStream(buffer));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                //Compressing the Image by converting it to JPEG format which is a kind of Lossy format.

                bitmap2.compress(Bitmap.CompressFormat.JPEG, x, byteArrayOutputStream);
                byte[] bytesArray = byteArrayOutputStream.toByteArray();
                buffer_bytes= bytesArray;

                Bitmap bitmapCompressedImage = BitmapFactory.decodeByteArray(bytesArray, 0, bytesArray.length);
                comp= bitmapCompressedImage;

                //Setting the Compressed Image Preview
                Drawable drawable = new BitmapDrawable(getResources(), bitmapCompressedImage);
                Glide.with(this).load(drawable).into(imgCompressed);
                //imgCompressed.setImageBitmap(bitmapCompressedImage);

                //Fetching the File Size information of the compressed image.
                long test= bytesArray.length;
                String compressedImageText = formatSize(test);
                txtCompressed.setText("Size: " + compressedImageText);
                Toast.makeText(getApplicationContext(), "Compression Successful!", Toast.LENGTH_SHORT).show();
                btnSave.setVisibility(View.VISIBLE);
                }
            else{
                Toast.makeText(getApplicationContext(), "No Image Selected !", Toast.LENGTH_SHORT).show();
            }
            } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }

        }

    //Method to open gallery of phone to select image.
    private void openGallery() {
        Intent gallery= new Intent();
        gallery.setType("image/*");
        gallery.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(gallery, "Select Picture"), SELECT_PICTURE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Code to select image from phone's storage
        if (requestCode == SELECT_PICTURE){
            Uri selectedImageUri= data.getData();
            buffer= selectedImageUri;
            if(null != selectedImageUri){

                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(buffer));

                    //Setting the Original Image Preview.
                    Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                    Glide.with(this).load(drawable).into(imgOriginal); //The glide dependancy works fine with large images.

                    //imgOriginal.setImageBitmap(bitmap); (didnt find this method useful for large sized files)

                    //Getting image size info
                    long originalImageSize = getImageSizeFromUri(this, selectedImageUri);
                    String originalImageSizeText = formatSize(originalImageSize);
                    txtOriginal.setText("Size: "+ originalImageSizeText);

                    //Sets the visibility of the Compress Button only when an image is selected.
                    btnCompress.setVisibility(View.VISIBLE);
                    txtHint.setVisibility(View.VISIBLE);

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }

    }

    //formatSize() method to display the size of image in common formats like KB or MB.
    private String formatSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(),"%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    //Method to get the Selected Image's Size using it URI
    private long getImageSizeFromUri(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream != null) {
                int imageSize = inputStream.available();
                inputStream.close();
                return imageSize;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }


}