package util;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class AzureBlobUtil {

    private static final String CONTAINER_URL = "https://yoonjiwonstorage.blob.core.windows.net/images-library";
    private static final String SAS_TOKEN = "sp=racwdl&st=2025-09-22T17:31:54Z&se=2032-02-07T01:46:54Z&spr=https&sv=2024-11-04&sr=c&sig=TygnVU9ocxbtfBnVBqjtepodkie0rq6nLigr7iHlnBA%3D";

    /** 업로드 후 공개 URL 반환 */
    public static String upload(String folder, String fileName, String contentType,
                                long contentLength, InputStream data) throws java.io.IOException {

        String blobPath = (folder == null || folder.isEmpty())
                ? fileName
                : folder.replaceAll("^/+", "").replaceAll("/+$", "") + "/" + fileName;

        String targetUrl = CONTAINER_URL + "/" + URLEncoder.encode(blobPath, "UTF-8")
                .replace("+", "%20") + "?" + SAS_TOKEN;

        try (CloseableHttpClient http = HttpClients.createDefault()) {
            HttpPut put = new HttpPut(targetUrl);
            put.setHeader("x-ms-blob-type", "BlockBlob");
            if (contentType != null && !contentType.isEmpty()) {
                put.setHeader("Content-Type", contentType);
            }

            InputStreamEntity entity = new InputStreamEntity(data, contentLength);
            put.setEntity(entity);

            try (CloseableHttpResponse resp = http.execute(put)) {
                int code = resp.getStatusLine().getStatusCode();
                if (code >= 200 && code < 300) {
                    return CONTAINER_URL + "/" + encodePath(blobPath);
                } else {
                    throw new RuntimeException("Azure upload failed: HTTP " + code);
                }
            }
        }
    }

    /** 다운로드/표시용 공개 URL */
    public static String publicUrl(String folder, String fileName) {
        String blobPath = (folder == null || folder.isEmpty())
                ? fileName
                : folder.replaceAll("^/+", "").replaceAll("/+$", "") + "/" + fileName;
        return CONTAINER_URL + "/" + encodePath(blobPath);
    }

    private static String encodePath(String path) {
        try {
            return URLEncoder.encode(path, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            throw new RuntimeException("Encoding error", e);
        }
    }
}
