package com.example.photoviewer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import androidx.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public final class FileUtils {
    private FileUtils() {}

    //public static final String BASE = "http://10.0.2.2:8000"; // 에뮬레이터 → PC 로컬
    public static final String BASE = "https://soyeonkk.pythonanywhere.com";

    /** 텍스트 폼 데이터 */
    public static RequestBody textBody(String s) {
        return RequestBody.create(s == null ? "" : s, MediaType.parse("text/plain"));
    }

    /** 갤러리에서 받은 content:// URI → Multipart Part("image") */
    public static MultipartBody.Part imagePartFromUri(Context ctx, Uri uri) {
        try {
            ContentResolver cr = ctx.getContentResolver();
            String mime = cr.getType(uri);
            if (mime == null) mime = "application/octet-stream";

            String filename = queryDisplayName(cr, uri);
            if (filename == null) filename = "upload.jpg";

            byte[] bytes = readAllBytes(cr.openInputStream(uri));
            RequestBody body = RequestBody.create(bytes, MediaType.parse(mime));
            return MultipartBody.Part.createFormData("image", filename, body); // ← 필드명 반드시 image
        } catch (Exception e) {
            throw new RuntimeException("이미지 읽기 실패: " + uri, e);
        }
    }

    @Nullable
    private static String queryDisplayName(ContentResolver cr, Uri uri) {
        Cursor c = cr.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        if (c != null) {
            try { if (c.moveToFirst()) return c.getString(0); }
            finally { c.close(); }
        }
        return null;
    }

    private static byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        is.close();
        return bos.toByteArray();
    }

    /** 서버가 준 이미지 경로가 상대경로(/media/...)면 BASE 붙이고, 127.0.0.1이면 치환 */
    public static String normalizeImageUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        if (!url.startsWith("http")) return BASE + url;
        return url.replace("http://127.0.0.1:8000", BASE)
                .replace("http://localhost:8000", BASE);
    }
}
