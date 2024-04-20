package utils;

import exeptions.DsmException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import requests.DsmAuth;

public class DsmUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(DsmUtils.class);

  private static final Map<Integer, String> codeErrors = new HashMap<>();

  static {
    codeErrors.put(101, "No parameter of API, method or version");
    codeErrors.put(102, "The requested API does not exist");
    codeErrors.put(103, "The requested method does not exist");
    codeErrors.put(104, "The requested version does not support the functionality");
    codeErrors.put(105, "The logged in session does not have permission");
    codeErrors.put(106, "Session timeout");
    codeErrors.put(107, "Session interrupted by duplicate login");
    codeErrors.put(400, "Invalid parameter of file operation");
    codeErrors.put(401, "Unknown error of file operation");
    codeErrors.put(402, "System is too busy");
    codeErrors.put(403, "Invalid user does this file operation");
    codeErrors.put(404, "Invalid group does this file operation");
    codeErrors.put(405, "Invalid user and group does this file operation");
    codeErrors.put(406, "Can’t get user/group information from the account server");
    codeErrors.put(407, "Operation not permitted");
    codeErrors.put(408, "No such file or directory");
    codeErrors.put(409, "Non-supported file system");
    codeErrors.put(410, "Failed to connect internet-based file system (ex: CIFS)");
    codeErrors.put(411, "Read-only file system");
    codeErrors.put(412, "Filename too long in the non-encrypted file system");
    codeErrors.put(413, "Filename too long in the encrypted file system");
    codeErrors.put(414, "File already exists");
    codeErrors.put(415, "Disk quota exceeded");
    codeErrors.put(416, "No space left on device");
    codeErrors.put(417, "Input/output error");
    codeErrors.put(418, "Illegal name or path");
    codeErrors.put(419, "Illegal file name");
    codeErrors.put(420, "Illegal file name on FAT file system");
    codeErrors.put(421, "Device or resource busy");
    codeErrors.put(599, "No such task of the file operation");
    codeErrors.put(1800,
        "There is no Content-Length information in the HTTP header or the received\n" +
            "size does not match the value of Content-Length information in the HTTP\n" +
            "header.");
    codeErrors.put(1801,
        "Wait too long, no date can be received from client (Default maximum wait time is 3600 seconds)");
    codeErrors.put(1802, "No filename information in the last part of file content.\n");
    codeErrors.put(1803, "Upload connection is cancelled.");
    codeErrors.put(1804, "Failed to upload too big file to FAT file system.");
    codeErrors.put(1805,
        "Can’t overwrite or skip the existed file, if no overwrite parameter is given.");
    codeErrors.put(900, "Failed to delete file(s)/folder(s). More information in <errors> object.");
    codeErrors.put(1200, "Failed to rename it. More information in <errors> object.");
    codeErrors.put(1000, "Failed to copy files/folders. More information in <errors> object.");
    codeErrors.put(1001, "Failed to move files/folders. More information in <errors> object.");
    codeErrors.put(1002,
        "An error occurred at the destination. More information in <errors> object.");
    codeErrors.put(1003,
        "Cannot overwrite or skip the existing file because no overwrite parameter is given.");
    codeErrors.put(1004,
        "File cannot overwrite a folder with the same name, or folder cannot overwrite a file with the same name.");
    codeErrors.put(1006,
        "Cannot copy/move file/folder with special characters to a FAT32 file system.");
    codeErrors.put(1007, "Cannot copy/move a file bigger than 4G to a FAT32 file system.");
    codeErrors.put(1100, "Failed to create a folder. More information in <errors> object");
    codeErrors.put(1101,
        "The number of folders to the parent folder would exceed the system limitation");
    codeErrors.put(2000, "Sharing link does not exist");
    codeErrors.put(2001, "Cannot generate sharing link because too many sharing links exist.");
    codeErrors.put(800, "A folder path of favorite folder is already added to user’s favorites");
    codeErrors.put(801,
        "A name of favorite folder conflicts with an existing folder path in the user’s favorites.");
    codeErrors.put(802, "There are too many favorites to be added");
    codeErrors.put(2002, "Failed to access sharing links.");
  }

  private DsmUtils() {
  }

  public static String manageErrorMessage(Integer code) {
    return Optional.ofNullable(codeErrors.get(code)).orElse("Unknown error");
  }

  public static String makePostRequest(String url, InputStream fileContent, String fileName,
                                       Map<String, String> params, DsmAuth auth)
      throws IOException {
    InputStreamBody inputStreamBody =
        new InputStreamBody(fileContent, ContentType.DEFAULT_BINARY, fileName);

    return makePostRequest(url, inputStreamBody, params, auth);
  }

  public static String makePostRequest(String url, String filePath, String fileName,
                                       Map<String, String> params, DsmAuth auth)
      throws IOException {
    FileBody fileBody = new FileBody(new File(filePath), ContentType.DEFAULT_BINARY, fileName);

    return makePostRequest(url, fileBody, params, auth);
  }

  public static String makePostRequest(String url, String filePath, Map<String, String> params,
                                       DsmAuth auth)
      throws IOException {
    FileBody fileBody =
        new FileBody(new File(filePath), ContentType.DEFAULT_BINARY, new File(filePath).getName());

    return makePostRequest(url, fileBody, params, auth);
  }

  private static String makePostRequest(String url, ContentBody body, Map<String, String> params,
                                        DsmAuth auth)
      throws IOException {
    CloseableHttpClient httpclient;
    if (auth.isBypassSSL()) {
      SSLConnectionSocketFactory socketFactory;
      try {
        socketFactory =
            new SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null,
                new TrustAllStrategy()).build(), NoopHostnameVerifier.INSTANCE);
      } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
        throw new RuntimeException(e);
      }
      httpclient =
          HttpClients.custom().setSSLSocketFactory(socketFactory).build();
    } else {
      httpclient = HttpClients.createDefault();
    }
    String result = "";
    try {
      HttpPost httppost = new HttpPost(url);

      MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
      params.forEach((key, value) -> multipartEntityBuilder.addPart(key,
          new StringBody(value, ContentType.TEXT_PLAIN)));
      multipartEntityBuilder.addPart("file", body);
      HttpEntity reqEntity = multipartEntityBuilder
          .setLaxMode()
          .build();

      httppost.setEntity(reqEntity);

      LOGGER.debug("executing request {}", httppost.getRequestLine());
      try (CloseableHttpResponse response = httpclient.execute(httppost)) {
        HttpEntity resEntity = response.getEntity();
        if (resEntity != null) {
          result = EntityUtils.toString(resEntity);
          LOGGER.debug(result);
        }
        EntityUtils.consume(resEntity);
      }
    } finally {
      httpclient.close();
    }
    return result;
  }

  public static File downloadFile(InputStream inputStream, String destinationPath)
      throws IOException {
    File file = new File(destinationPath);
    try (FileOutputStream fos = new FileOutputStream(file)) {
      int inByte;
      while ((inByte = inputStream.read()) != -1) {
        fos.write(inByte);
      }
    }

    return file;
  }

  public static String extractFileName(String path) {
    String completePath =
        Optional.ofNullable(path).orElseThrow(() -> new DsmException("Unable to get file name"));

    return completePath.substring(completePath.lastIndexOf('/') + 1);
  }

  public static String extractRootFolderPath(String filePath) {
    return Optional.of(filePath).orElse("").replace("/" + extractFileName(filePath), "");
  }
}
