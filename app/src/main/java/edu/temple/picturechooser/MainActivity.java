package edu.temple.picturechooser;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    ImageView baseImageView, dataImageView;
    Button baseButton, dataButton, submitButton;
    Uri baseImageUri, dataImageUri;
    TextView t;

    String testMessage = "test test";

    // which method am I using?
    boolean multipartBody = true;
    boolean file = false;

    private static final int PICK_BASE_IMAGE = 100;
    private static final int PICK_DATA_IMAGE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        baseImageView = findViewById(R.id.imageView);
        dataImageView = findViewById(R.id.imageView2);
        baseButton = findViewById(R.id.button);
        dataButton = findViewById(R.id.button2);
        submitButton = findViewById(R.id.button3);
        t = findViewById(R.id.textView);

        baseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_BASE_IMAGE);
            }
        });

        dataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_DATA_IMAGE);
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendInputsToApi();
                //sendMessageToApi(testMessage);
                getCapacity();
            }
        });
    }

    public void getCapacity() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        StegosaurusService client = retrofit.create(StegosaurusService.class);
        if(baseImageUri != null) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PICK_BASE_IMAGE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Call<String> call = client.howManyBytes(prepareFilePart("image", baseImageUri), false);
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        t.setText(response.body());
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Can't get capacity", Toast.LENGTH_SHORT).show();
                        Log.i("Throwable", t.toString());
                    }
                });
            }
            else
                Toast.makeText(MainActivity.this, "external storage: " + isExternalStorageWritable(), Toast.LENGTH_SHORT).show();

        }
    }

    public void sendMessageToApi(String message) {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        StegosaurusService client = retrofit.create(StegosaurusService.class);
        Call<String> call = client.echoResponse(message);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                t.setText(response.body());
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void sendInputsToApi(){
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("https://stegosaurus.ml")
                .addConverterFactory(GsonConverterFactory.create());

        Retrofit retrofit = builder.build();

        StegosaurusService client = retrofit.create(StegosaurusService.class);

        if (baseImageUri != null && dataImageUri != null) {

            if(multipartBody) {
                Call<String> call = client.insertPhoto(
                        prepareFilePart("base", baseImageUri),
                        prepareFilePart("data", dataImageUri),
                        "the key"
                );
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        t.setText(response.body());
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else if (file) {
                File baseFile = new File(baseImageUri.toString());
                File dataFile = new File(dataImageUri.toString());
                Call<String> call = client.insertPhotoFile(baseFile, dataFile, "the key");
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        t.setText(response.body());
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }


        }
        else
            Toast.makeText(this, "Please enter all inputs", Toast.LENGTH_SHORT).show();

    }

    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        File file = FileUtils.getFile(this, fileUri);

        // create RequestBody instance from file
        RequestBody requestFile =
                RequestBody.create(
                        MediaType.parse(getContentResolver().getType(fileUri)),
                        file
                );

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void openGallery(int resultCode){
        Intent gallery = new Intent (Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, resultCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == PICK_BASE_IMAGE) {
            baseImageUri = data.getData();
            baseImageView.setImageURI(baseImageUri);
        }

        if(resultCode == RESULT_OK && requestCode == PICK_DATA_IMAGE) {
            dataImageUri = data.getData();
            dataImageView.setImageURI(dataImageUri);
        }
    }
}
