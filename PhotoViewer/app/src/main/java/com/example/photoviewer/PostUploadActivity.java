package com.example.photoviewer;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import okhttp3.MultipartBody;
import okhttp3.MultipartBody.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;

import java.io.IOException;

public class PostUploadActivity extends AppCompatActivity {

    private EditText etTitle, etText;
    private ImageView ivPreview;
    private Button btnPick, btnSubmit;

    private Uri selectedImageUri; // 갤러리에서 선택된 이미지 URI
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
        setContentView(R.layout.activity_post_upload);

        etTitle = findViewById(R.id.editTitle);
        etText  = findViewById(R.id.editText);
        ivPreview = findViewById(R.id.imagePreview);
        btnPick = findViewById(R.id.btnPickImage);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnPick.setOnClickListener(v -> pickImage.launch("image/*"));
        btnSubmit.setOnClickListener(v -> upload());
    }

    private void upload() {
        String title = etTitle.getText().toString();
        String text  = etText.getText().toString();

        Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("title", null, FileUtils.textBody(title))
                .addFormDataPart("text",  null, FileUtils.textBody(text));

        if (selectedImageUri != null) {
            mb.addPart(FileUtils.imagePartFromUri(this, selectedImageUri));
        }

        Request req = new Request.Builder()
                .url(FileUtils.BASE + "/api_root/Post/")
                .post(mb.build()) // 생성은 POST
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { e.printStackTrace(); }
            @Override public void onResponse(Call call, Response resp) throws IOException {
                // 201 Created 기대
            }
        });
    }
}
