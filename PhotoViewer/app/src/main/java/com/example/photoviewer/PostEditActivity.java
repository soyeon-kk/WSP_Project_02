package com.example.photoviewer;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public class PostEditActivity extends AppCompatActivity {

    private EditText etTitle, etText;
    private ImageView ivPreview;

    private Uri selectedImageUri = null;
    private int postId;
    private String existingImageUrl;

    private final OkHttpClient client = new OkHttpClient();

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivPreview.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_edit);

        etTitle = findViewById(R.id.editTitle);
        etText  = findViewById(R.id.editText);
        ivPreview = findViewById(R.id.imagePreview);

        postId = getIntent().getIntExtra("post_id", -1);
        etTitle.setText(getIntent().getStringExtra("post_title"));
        etText.setText(getIntent().getStringExtra("post_text"));
        existingImageUrl = getIntent().getStringExtra("post_image");

        String normalized = FileUtils.normalizeImageUrl(existingImageUrl);
        if (normalized != null) Glide.with(this).load(normalized).into(ivPreview);

        findViewById(R.id.btnPickImage).setOnClickListener(v -> pickImage.launch("image/*"));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
    }

    private void save() {
        String title = etTitle.getText().toString();
        String text  = etText.getText().toString();

        MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("text", text);

        if (selectedImageUri != null)
            mb.addPart(FileUtils.imagePartFromUri(this, selectedImageUri));

        Request req = new Request.Builder()
                .url(FileUtils.BASE + "/api_root/Post/" + postId + "/")
                .put(mb.build())
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { e.printStackTrace(); }

            @Override public void onResponse(Call call, Response resp) {
                finish();
            }
        });
    }
}
