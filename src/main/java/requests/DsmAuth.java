package requests;

import exeptions.DsmException;
import exeptions.DsmLoginException;
import java.util.Optional;
import java.util.Properties;

public class DsmAuth {

  private static final String HOST_KEY = "host";
  private static final String PORT_KEY = "port";
  private static final String USERNAME_KEY = "username";
  private static final String PASSWORD_KEY = "password";
  private static final String BYPASS_SSL_KEY = "bypass_ssl";

  private String host;
  private Integer port;
  private String userName;
  private String password;
  private String sid;
  private Boolean bypassSSL = false;

  public String getHost() {
    return host;
  }

  public DsmAuth setHost(String host) {
    this.host = host;
    return this;
  }

  public String getSid() {
    return sid;
  }

  public DsmAuth setSid(String sid) {
    this.sid = sid;
    return this;
  }

  public Integer getPort() {
    return port;
  }

  public DsmAuth setPort(Integer port) {
    this.port = port;
    return this;
  }

  public String getUserName() {
    return userName;
  }

  public DsmAuth setUserName(String userName) {
    this.userName = userName;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public DsmAuth setPassword(String password) {
    this.password = password;
    return this;
  }

  public boolean isBypassSSL() {
    return bypassSSL;
  }

  public DsmAuth setBypassSSL(Boolean bypassSSL) {
    this.bypassSSL = bypassSSL;
    return this;
  }

  public static DsmAuth of(String host, Integer port, String userName, String password,
                           Boolean bypassSSL) {
    DsmAuth dsmAuth = new DsmAuth()
        .setHost(Optional.ofNullable(host)
            .orElseThrow(() -> new DsmLoginException("Unable to find property : host")))
        .setUserName(Optional.ofNullable(userName)
            .orElseThrow(() -> new DsmLoginException("Unable to find property : userName")))
        .setPassword(Optional.ofNullable(password)
            .orElseThrow(() -> new DsmLoginException("Unable to find property : password")));

    Optional.ofNullable(port).ifPresent(dsmAuth::setPort);
    Optional.ofNullable(bypassSSL).ifPresent(dsmAuth::setBypassSSL);
    return dsmAuth;
  }

  public static DsmAuth fromProperties(Properties properties) {
    try {
      validate(properties);
      return DsmAuth.of(properties.getProperty(HOST_KEY),
          properties.getProperty(PORT_KEY) == null
          ? null
          :
          Integer.valueOf(properties.getProperty(PORT_KEY)),
          properties.getProperty(USERNAME_KEY), properties.getProperty(PASSWORD_KEY),
          properties.getProperty(BYPASS_SSL_KEY) == null
          ? null
          : Boolean.valueOf(properties.getProperty(BYPASS_SSL_KEY)));
    } catch (Exception exception) {
      throw new DsmException(exception);
    }
  }

  private static void validate(Properties properties) {
    String host = properties.getProperty(HOST_KEY) != null
                  ? properties.getProperty(HOST_KEY)
                  : null;
    String userName = properties.getProperty(USERNAME_KEY) != null
                      ? properties.getProperty(USERNAME_KEY)
                      : null;
    String password = properties.getProperty(PASSWORD_KEY) != null
                      ? properties.getProperty(PASSWORD_KEY)
                      : null;

    if (host != null && userName != null && password != null) {
      return;
    }

    throw new DsmLoginException(
        "None of the properties can't be empty : host=" + host + ", username=" +
            userName + ", password=" + password);
  }
}
