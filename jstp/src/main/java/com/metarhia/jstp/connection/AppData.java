package com.metarhia.jstp.connection;

import com.github.zafarkhaja.semver.expr.ExpressionParser;
import java.io.Serializable;
import java.util.Objects;

public class AppData implements Serializable {

  private String name;
  private String version;

  public AppData() {
  }

  public AppData(String name) {
    this(name, null);
  }

  public AppData(String name, String version) {
    this.name = name;
    if (version != null) {
      setVersion(version);
    }
  }

  /**
   * @param app application to connect to as 'name' or 'name@version'
   *            where version is a valid semver version or range
   *            (must not be null)
   *
   * @return AppData instance from {@param app}
   */
  public static AppData valueOf(String app) {
    if (app == null || app.isEmpty()) {
      throw new IllegalArgumentException("Invalid(empty) app data to parse");
    }
    String[] appArgs = app.split("@");
    String appName = appArgs[0];
    if (appName.isEmpty()) {
      throw new IllegalArgumentException("Invalid(empty) application name");
    }
    String appVersion = appArgs.length > 1 ? appArgs[1] : null;
    return new AppData(appName, appVersion);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppData appData = (AppData) o;
    return Objects.equals(name, appData.name) &&
        Objects.equals(version, appData.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    // check that version is valid
    ExpressionParser.newInstance().parse(version);
    this.version = version;
  }

  /**
   * @return application data as 'name@version' or 'name' if version is null
   */
  public String getApp() {
    if (version != null) {
      return String.format("%s@%s", name, version);
    }
    return name;
  }
}
