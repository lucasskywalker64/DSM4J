package requests.filestation.transfert;

import com.fasterxml.jackson.core.type.TypeReference;
import exeptions.DsmException;
import exeptions.DsmUploadException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import requests.DsmAbstractRequest;
import requests.DsmAuth;
import requests.filestation.DsmRequestParameters;
import responses.Response;
import responses.filestation.transfert.DsmUploadResponse;
import utils.DateUtils;
import utils.DsmUtils;

/**
 * Upload a file by RFC 1867, http://tools.ietf.org/html/rfc1867.
 * Note that each parameter is passed within each part but binary file data must be the last part.
 */
public class DsmUploadRequest extends DsmAbstractRequest<DsmUploadResponse> {

  private String destinationFolderPath;
  private Boolean createParents;
  private DsmRequestParameters.OverwriteBehaviour overwrite =
      DsmRequestParameters.OverwriteBehaviour.ERROR;
  private String filePath;
  private String destinationFileName;
  private InputStream fileContent;
  private LocalDateTime lastModifiedTime;
  private LocalDateTime createdTime;
  private LocalDateTime lastAccessedTime;

  public DsmUploadRequest(DsmAuth auth) {
    super(auth);
    this.apiName = "SYNO.FileStation.Upload";
    this.version = 2;
    this.method = "upload";
    this.path = "webapi/entry.cgi";
  }


  @Override
  protected TypeReference getClassForMapper() {
    return new TypeReference<Response<DsmUploadResponse>>() {
    };
  }

  public DsmUploadRequest setDestinationFolderPath(String destinationFolderPath) {
    this.destinationFolderPath = destinationFolderPath;
    return this;
  }

  public DsmUploadRequest createParentFolders(Boolean createParents) {
    this.createParents = createParents;
    return this;
  }

  public DsmUploadRequest overwrite(DsmRequestParameters.OverwriteBehaviour overwrite) {
    this.overwrite = overwrite;
    return this;
  }

  public DsmUploadRequest setFilePath(String filePath) {
    this.filePath = filePath;
    return this;
  }

  public DsmUploadRequest setDestinationFileName(String destinationFileName) {
    this.destinationFileName = destinationFileName;
    return this;
  }

  public DsmUploadRequest setFileContent(InputStream fileContent) {
    this.fileContent = fileContent;
    return this;
  }


  public DsmUploadRequest setLastModifiedTime(LocalDateTime lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  public DsmUploadRequest setCreatedTime(LocalDateTime createdTime) {
    this.createdTime = createdTime;
    return this;
  }

  public DsmUploadRequest setLastAccessedTime(LocalDateTime lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
    return this;
  }

  @Override
  public Response<DsmUploadResponse> call() {
    Map<String, String> params = new HashMap<>();
    params.put("path", Optional.ofNullable(this.destinationFolderPath)
        .orElseThrow(() -> new DsmException("you must define destination folder")));
    Optional.ofNullable(this.createParents)
        .ifPresent(c -> params.put("create_parents", c.toString()));

    if (!this.overwrite.equals(DsmRequestParameters.OverwriteBehaviour.ERROR)) {
      params.put("overwrite", String.valueOf(this.overwrite.getValue()));
    }

    if (Optional.ofNullable(this.fileContent).isPresent()) {
      if (Optional.ofNullable(this.filePath).isPresent()) {
        throw new DsmException(
            "you can't specify both file path and file content stream to upload");
      }
      if (!Optional.ofNullable(this.destinationFileName).isPresent()) {
        throw new DsmException("you musy define destination file name when using an upload stream");
      }
    } else {
      if (!new File(Optional.ofNullable(this.filePath)
          .orElseThrow(() -> new DsmException("you must define source file to upload"))).exists()) {
        throw new DsmUploadException("File does not exist");
      }
    }

    addDatesToRequest("atime", params, this.lastAccessedTime);
    addDatesToRequest("crtime", params, this.createdTime);
    addDatesToRequest("mtime", params, this.lastModifiedTime);

    try {
      String resp;
      if (Optional.ofNullable(this.fileContent).isPresent()) {
        resp = DsmUtils.makePostRequest(
            build(),
            this.fileContent,
            this.destinationFileName,
            params,
            auth
        );
      } else if (Optional.ofNullable(this.destinationFileName).isPresent()) {
        resp = DsmUtils.makePostRequest(
            build(),
            this.filePath,
            this.destinationFileName,
            params,
            auth
        );
      } else {
        resp = DsmUtils.makePostRequest(
            build(),
            this.filePath,
            params,
            auth
        );
      }
      return deserialize(resp);
    } catch (IOException e) {
      throw new DsmException(e);
    }
  }

  private void addDatesToRequest(String key, Map<String, String> params, LocalDateTime time) {
    Optional.ofNullable(time).ifPresent(
        c -> params.put(key, String.valueOf(DateUtils.convertLocalDateTimeToUnixTimestamp(c))));
  }

  @Override
  protected String build() {
    return auth.getHost() +
        (auth.getPort() == null
         ? ""
         : ":" + auth.getPort()) +
        "/" +
        getPath() +
        "?_sid=" +
        auth.getSid() + "&" +
        "api=" + getApiName() + "&" +
        "method=" + getMethod() + "&" +
        "version=" + getVersion();
  }
}
