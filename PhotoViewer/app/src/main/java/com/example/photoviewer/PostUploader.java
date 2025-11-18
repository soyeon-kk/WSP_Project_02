package com.example.photoviewer;

import android.util.Log;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostUploader {
    //private static final String BASE_URL = "http://10.0.2.2:8000";
    private static final String BASE_URL = FileUtils.BASE;

    // 새 글 작성
    public static boolean uploadPost(String title, String text, String imagePath) {
        return sendMultipartRequest(BASE_URL + "/api_root/Post/", "POST", title, text, imagePath);
    }

    // 글 수정
    public static boolean updatePost(int postId, String title, String text, String imagePath) {
        return sendMultipartRequest(BASE_URL + "/api_root/Post/" + postId + "/", "PUT", title, text, imagePath);
    }

    // 글 삭제
    public static boolean deletePost(int postId) {
        try {
            URL url = new URL(BASE_URL + "/api_root/Post/" + postId + "/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("DELETE");
            int code = conn.getResponseCode();
            return (code == 200 || code == 204);
        } catch (Exception e) {
            Log.e("PostUploader", "Delete failed", e);
            return false;
        }
    }

    // Multipart 요청 공통 함수
    private static boolean sendMultipartRequest(String urlString, String method, String title, String text, String imagePath) {
        String boundary = "*****" + System.currentTimeMillis() + "*****";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            // text field
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"title\"" + lineEnd + lineEnd);
            dos.writeBytes(title + lineEnd);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"text\"" + lineEnd + lineEnd);
            dos.writeBytes(text + lineEnd);

            // image file
            if (imagePath != null && !imagePath.isEmpty()) {
                File sourceFile = new File(imagePath);
                if (sourceFile.exists()) {
                    FileInputStream fileInputStream = new FileInputStream(sourceFile);

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\""
                            + sourceFile.getName() + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);

                    int bytesAvailable = fileInputStream.available();
                    int bufferSize = Math.min(bytesAvailable, 1024 * 1024);
                    byte[] buffer = new byte[bufferSize];

                    int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bytesRead);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, 1024 * 1024);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    fileInputStream.close();
                }
            }

            dos.flush();
            dos.close();

            int responseCode = conn.getResponseCode();
            Log.d("PostUploader", "Response Code: " + responseCode);

            return (responseCode == 200 || responseCode == 201);
        } catch (Exception e) {
            Log.e("PostUploader", "Multipart error", e);
            return false;
        }
    }
}
