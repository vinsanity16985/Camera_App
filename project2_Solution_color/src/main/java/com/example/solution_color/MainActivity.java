package com.example.solution_color;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.helpers.Constants;
import com.library.bitmap_utilities.BitMap_Helpers;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

//!!!!!!!!!!!!!!!!!
//Project 2
//Vincent Noca & Josh Cohen
//!!!!!!!!!!!!!!!!!

public class MainActivity extends AppCompatActivity  {

    private DisplayMetrics metrics;
    private int screenHeight;
    private int screenWidth;
    private Toolbar toolbar;
    private ImageView background_img;
    private String filePath;
    private Bitmap new_img;
    private Bitmap sketch_img;
    private Bitmap colorized_img;
    private ImageButton camera_button;
    private File img_file;
    private SharedPreferences myPrefs;

    /**
     * Initializes private variables upon start of the app
     * @param: Bundle savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sets main activity layout
        toolbar = (Toolbar)findViewById(R.id.my_toolbar);
        background_img = (ImageView)findViewById(R.id.background_default);

        //Used to get data from preferences
        myPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        //Gets the devices screen height and width
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;

        //Sets the appbar and removes the project title
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    /**
     * Will populate the menu with menu items if there are any
     * @param: Menu menu, The menu that will be populated with menu items
     * @return: boolean true, Indicates that the menu was populated
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if they are present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Determines which menu item was clicked by the user and calls necessary function to handle the click
     * @param: MenuItem item, The menu item that is clicked by the user
     * @return: boolean true, Indicates that a menu item was clicked
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent myIntent = new Intent(this, SettingsActivity.class);
                startActivity(myIntent);
                break;
            case R.id.menu_reset:
                //Reset to Default Image in Drawable folder
                reset();
                break;
            case R.id.menu_sketch:
                //Turn the image into a black and white sketch
                sketch();
                break;
            case R.id.menu_colorize:
                //Colorize the black and white sketch of the image
                colorize();
                break;
            case R.id.menu_share:
                //Share the image
                share();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * Onclick handler for ImageButton
     * Asks user for necessary permissions to access the camera and reading and writing to storage
     * @param: View view, view that is being handled
     */
    public void cameraPermission(View view){
        //Check API version of device
        int currentAPI = android.os.Build.VERSION.SDK_INT;
        if(currentAPI >= android.os.Build.VERSION_CODES.M){
            int permission = checkSelfPermission(Manifest.permission.CAMERA);
            int permission2 = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            int permission3 = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED && permission2 != PackageManager.PERMISSION_GRANTED && permission3 != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.MY_PERMISSIONS_REQUESTS);
                return;
            }
            startCamera();
        }
        else{
            startCamera();
        }
    }

    /**
     * Listens for the users response to permissions
     * Will start the camera app if the user allows it
     * @param: int requestCode, code passed by requestPermissions
     * @param: String[] permissions, the permissions requested
     * @param: int[] grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUESTS:
                // If request is cancelled, the result arrays are empty.
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                }
                else{
                    Toast.makeText(this, "Open Camera Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     *  Starts up device's built in camera app
     */
    public void startCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //Checks if there is an app that can take pictures before starting activity
        //Informs the user if they do not have a picture taking app
        if(cameraIntent.resolveActivity(getPackageManager()) != null){
            img_file = null;
            try{
                img_file = createImageFile();
            }catch(IOException e){
                e.printStackTrace();
            }
            if(img_file != null){
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(img_file));
                startActivityForResult(cameraIntent, Constants.TAKE_PICTURE);
            }
        }
        else{
            Toast.makeText(this, "You have no app that can take pictures.", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Will acquire information created and returned through an intent
     * @param: int requestCode, Code corresponding to user's initial intent
     * @param: int resultCode, Code corresponding to user's decision within the Intent
     * @param: Intent data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        //If user opens the camera to take a picture
        if(requestCode == Constants.TAKE_PICTURE){
            //If user takes a picture
            if(resultCode == RESULT_OK){
                //Acquires the image taken by the user and sets it as the background image
                new_img = Camera_Helpers.loadAndScaleImage(filePath, screenHeight, screenWidth);
                background_img.setImageBitmap(new_img);
            }
        }
    }

    /**
     * Creates an Image File that will store the Image taken by the user
     * Places the file in the File Systems External Public Directory
     * @return: JPEG Image File
     * @throws: IOException
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        filePath = image.getAbsolutePath();
        return image;
    }

    /**
     * Deletes images taken by the user through this app
     * Resets the background image to the default image and scales to fit screen
     */
    private void reset(){
        Camera_Helpers.delSavedImage(filePath);
        filePath = null;
        background_img.setImageResource(R.drawable.default_img);
        background_img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        background_img.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    /**
     * Creates a black and white sketch of the image taken by the user
     * Saves it to filepath
     */
    private void sketch() {
        if(filePath == null)
        {
            //do nothing
        }
        else {
            //Gets the int for sketchiness set in preferences
            //default is 50
            int sketchiness = Integer.parseInt(myPrefs.getString(getString(R.string.sketchiness_pref_title), ""));

            //Keeps sketchiness within the bounds 0 - 100
            if (sketchiness > 100) {
                sketchiness = 100;
            } else if (sketchiness < 0) {
                sketchiness = 0;
            }
            sketch_img = BitMap_Helpers.thresholdBmp(new_img, sketchiness);
            Camera_Helpers.saveProcessedImage(sketch_img, filePath);
            background_img.setImageBitmap(sketch_img);
        }
    }

    /**
     * Creates a colorized image of the black and white sketch of the user's image
     * Saves it to filepath
     */
    private void colorize(){
        if(filePath == null)
        {
            //do nothing
        }
        else {
            //Gets the int for saturation set in preferences
            //default is 50
            int saturation = Integer.parseInt(myPrefs.getString(getString(R.string.saturation_pref_title), ""));

            //Keeps saturation within bounds 0 - 100
            if (saturation > 100) {
                saturation = 100;
            } else if (saturation < 0) {
                saturation = 0;
            }
            //Gets rid of redundancy
            if (sketch_img == null) {
                sketch_img = BitMap_Helpers.thresholdBmp(new_img, saturation);
            }
            colorized_img = BitMap_Helpers.colorBmp(new_img, saturation);
            BitMap_Helpers.merge(colorized_img, sketch_img);
            Camera_Helpers.saveProcessedImage(colorized_img, filePath);
            background_img.setImageBitmap(colorized_img);
        }
    }

    /**
     * Shares the image on the screen to an app that the user selects
     * Has default subject and text which the user can change in settings
     */
    private void share(){
        if(filePath == null)
        {
            //do nothing
        }
        else {
            //The image, subject and text to be added to the intent
            File file = new File(filePath);
            Uri image_Uri = Uri.fromFile(file);
            String subject = myPrefs.getString(getString(R.string.share_subject_title), "");
            String text = myPrefs.getString(getString(R.string.share_text_title), "");
            Intent share_intent = new Intent(Intent.ACTION_SEND);

            //Puts data within the intent
            share_intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            share_intent.putExtra(Intent.EXTRA_TEXT, text);
            share_intent.putExtra(Intent.EXTRA_STREAM, image_Uri);
            share_intent.setType("*/*");

            //Starts the intent
            startActivity(Intent.createChooser(share_intent, getString(R.string.share_prompt)));
        }
    }
}

