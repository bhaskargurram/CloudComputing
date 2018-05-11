package com.nyu.cloudcomputing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.services.s3.AmazonS3Client;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PICK_IMAGE = 1;

    // File size 80KB
    private static final int MAX_FILE_SIZE = 80000;

    Button processImageButton;
    ImageView imageView;
    RelativeLayout spinner, placeHolder;
    DrawerLayout mDrawerLayout;

    File imageFile = null;
    String modelValue;
    final int semiTransparentGrey = Color.argb(155, 185, 185, 185);

    GoogleSignInAccount account;

    private final int[] modelImageViewIds = {R.id.image_view_udnie,
            R.id.image_view_wreck,
            R.id.image_view_wave,
            R.id.image_view_scream,
            R.id.image_view_rain_princess,
            R.id.image_view_la_muse,};

    View.OnClickListener modelOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            for(int id: modelImageViewIds){
                ((ImageView) findViewById(id)).setColorFilter(semiTransparentGrey, PorterDuff.Mode.SRC_ATOP);
            }
            modelValue = v.getTag().toString();
            ((ImageView) v).clearColorFilter();
            //todo add border to view
        }
    };

    View.OnClickListener selectImageClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_container);

        account = getIntent().getParcelableExtra("account");

        setTitle(account.getDisplayName());

        mDrawerLayout = findViewById(R.id.drawer_layout);

        for(int id: modelImageViewIds){
            ((ImageView) findViewById(id)).setColorFilter(semiTransparentGrey, PorterDuff.Mode.SRC_ATOP);
            findViewById(id).setOnClickListener(modelOnClickListener);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        //  menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        switch (menuItem.getItemId()){
                            case R.id.saved_images:
                                break;
                            case R.id.send_image:
                                break;
                            case R.id.about_the_team:
                                break;
                        }

                        return true;
                    }
                });

        processImageButton = findViewById(R.id.button_process_image);
        imageView = findViewById(R.id.image);
        placeHolder = findViewById(R.id.placeHolder);
        spinner = findViewById(R.id.progressBar);

        imageView.setOnClickListener(selectImageClickListener);
        placeHolder.setOnClickListener(selectImageClickListener);

        processImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imageFile != null) {
                    showSpinner();
                    uploadWithTransferUtility(imageFile);
                }else {
                    Toast.makeText(getApplicationContext(), "Select image first", Toast.LENGTH_LONG).show();
                }
            }
        });

        init();
    }

    private void init(){
        AWSMobileClient.getInstance().initialize(this).execute();
        modelValue = "udnie.ckpt";
        ((ImageView) findViewById(modelImageViewIds[0])).clearColorFilter();
    }

    public void uploadWithTransferUtility(final File file) {

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();


        final String key = "user-images/" + Util.getUniqueFileName() + ".jpg";

        TransferObserver uploadObserver =
                transferUtility.upload(
                        "ccprojectbucket",
                        key,
                        file);

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    String url = "https://pe5enlntb0.execute-api.us-east-1.amazonaws.com/Test/processimage?key=" + key;
                    url += "&user_id=" + account.getId();
                    url += "&user_name=" + account.getDisplayName().replaceAll("\\s+","");
                    url += "&file_size=" + file.length();
                    url += "&model_name=" + modelValue;

                    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

                    // prepare the Request
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(TAG, response);
                                    response = response.replace("\"", "");
                                    response = "output/" + response;
                                    downloadWithTransferUtility(response);
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, error.toString());
                            showPlaceholder();
                            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
                        }
                    });

                    stringRequest.setRetryPolicy(new RetryPolicy() {
                        @Override
                        public int getCurrentTimeout() {
                            return 50000;
                        }

                        @Override
                        public int getCurrentRetryCount() {
                            return 50000;
                        }

                        @Override
                        public void retry(VolleyError error) throws VolleyError {

                        }
                    });

                    // add it to the RequestQueue
                    queue.add(stringRequest);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d(TAG, "Uploading-ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "Uploading-key:" + key + "-error:" + ex.toString());
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d(TAG, "Bytes Transferrred: " + uploadObserver.getBytesTransferred());
        Log.d(TAG, "Bytes Total: " + uploadObserver.getBytesTotal());
    }

    private void downloadWithTransferUtility(final String key) {

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();


        final String fileName = "/sdcard/cc-project/"+Util.getUniqueFileName()+".jpg";

        TransferObserver downloadObserver =
                transferUtility.download(
                        "ccprojectbucket",
                        key,
                        new File(fileName));

        // Attach a listener to the observer to get state update and progress notifications
        downloadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    Bitmap bmImg = BitmapFactory.decodeFile(fileName);
                    imageView.setImageBitmap(bmImg);
                    showImage();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d(TAG, "   Downloading-ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "Downloading-key:" + key + "-error:" + ex.toString());
                showPlaceholder();
                Toast.makeText(getApplicationContext(), "Error Occured", Toast.LENGTH_LONG).show();
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == downloadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d("YourActivity", "Bytes Transferrred: " + downloadObserver.getBytesTransferred());
        Log.d("YourActivity", "Bytes Total: " + downloadObserver.getBytesTotal());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                handleLogout();
                return true;
            case android.R.id.home:
                if(mDrawerLayout.isDrawerOpen(GravityCompat.START)){
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                }else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleLogout() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient mClient = GoogleSignIn.getClient(this, gso);
        mClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu_items, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != -1){
            return;
        }
        if(requestCode == PICK_IMAGE) {
            Uri fileUri = data.getData();
            File file = new File(Util.getPath(this, fileUri));
            if(file.length() > MAX_FILE_SIZE){
                Toast.makeText(getApplicationContext(), "File size should be less than " + MAX_FILE_SIZE / 1000 + "KB." ,Toast.LENGTH_LONG).show();
                return;
            }
            Log.d(TAG, file.toString());
            imageFile = file;
            Bitmap bmImg = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            imageView.setImageBitmap(bmImg);
            showImage();
        }
    }

    private void showSpinner(){
        placeHolder.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
    }

    private void showImage(){
        spinner.setVisibility(View.GONE);
        placeHolder.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
    }

    private void showPlaceholder(){
        spinner.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        placeHolder.setVisibility(View.VISIBLE);
    }
}
