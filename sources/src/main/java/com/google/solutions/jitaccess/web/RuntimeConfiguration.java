//
// Copyright 2022 Google LLC
//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.google.solutions.jitaccess.web;

import com.google.solutions.jitaccess.core.clients.*;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

class RuntimeConfiguration {
  enum Catalog {
    /**
     * Use Policy Analyzer API. Requires an SCC subscription.
     */
    POLICYANALYZER,

    /**
     * Use Asset Inventory API.
     */
    ASSETINVENTORY
  }

  private final Function<String, String> readSetting;

  public RuntimeConfiguration(@NotNull Map<String, String> settings) {
    this(key -> settings.get(key));
  }

  public RuntimeConfiguration(Function<String, String> readSetting) {
    this.readSetting = readSetting;

    this.scope = new StringSetting(
        List.of("RESOURCE_SCOPE"),
        String.format("projects/%s", this.readSetting.apply("GOOGLE_CLOUD_PROJECT")));
    this.customerId = new StringSetting(
        List.of("RESOURCE_CUSTOMER_ID"),
        null);
    this.catalog = new EnumSetting<Catalog>(
        Catalog.class,
        List.of("RESOURCE_CATALOG"),
        Catalog.POLICYANALYZER);

    //
    // Activation settings.
    //
    this.activationTimeout = new DurationSetting(
        List.of("ELEVATION_DURATION", "ACTIVATION_TIMEOUT"),
        ChronoUnit.MINUTES,
        Duration.ofHours(2));
    this.activationRequestTimeout = new DurationSetting(
        List.of("ACTIVATION_REQUEST_TIMEOUT"),
        ChronoUnit.MINUTES,
        Duration.ofHours(1));
    this.justificationPattern = new StringSetting(
        List.of("JUSTIFICATION_PATTERN"),
        ".*");
    this.justificationHint = new StringSetting(
        List.of("JUSTIFICATION_HINT"),
        "Bug or case number");
    this.minNumberOfReviewersPerActivationRequest = new IntSetting(
        List.of("ACTIVATION_REQUEST_MIN_REVIEWERS"),
        1);
    this.maxNumberOfReviewersPerActivationRequest = new IntSetting(
        List.of("ACTIVATION_REQUEST_MAX_REVIEWERS"),
        10);
    this.maxNumberOfPrivilegesPerSelfApproval = new IntSetting(
        List.of("ACTIVATION_REQUEST_MAX_ROLES"),
        10);
    this.availableProjectsQuery = new StringSetting(
        List.of("AVAILABLE_PROJECTS_QUERY"),
        this.catalog.getValue() == Catalog.ASSETINVENTORY
            ? "state:ACTIVE"
            : null);

    //
    // Backend service id (Cloud Run only).
    //
    this.backendServiceId = new StringSetting(List.of("IAP_BACKEND_SERVICE_ID"), null);

    //
    // Notification settings.
    //
    this.timeZoneForNotifications = new ZoneIdSetting(List.of("NOTIFICATION_TIMEZONE"));
    this.topicName = new StringSetting(List.of("NOTIFICATION_TOPIC"), null);

    //
    // SMTP settings.
    //
    this.smtpHost = new StringSetting(List.of("SMTP_HOST"), "smtp.gmail.com");
    this.smtpPort = new IntSetting(List.of("SMTP_PORT"), 587);
    this.smtpEnableStartTls = new BooleanSetting(List.of("SMTP_ENABLE_STARTTLS"), true);
    this.smtpSenderName = new StringSetting(List.of("SMTP_SENDER_NAME"), "JIT Access");
    this.smtpSenderAddress = new StringSetting(List.of("SMTP_SENDER_ADDRESS"), null);
    this.smtpUsername = new StringSetting(List.of("SMTP_USERNAME"), null);
    this.smtpPassword = new StringSetting(List.of("SMTP_PASSWORD"), null);
    this.smtpSecret = new StringSetting(List.of("SMTP_SECRET"), null);
    this.smtpExtraOptions = new StringSetting(List.of("SMTP_OPTIONS"), null);

    //
    // Mail formatting settings.
    //
    this.internalsMailAddressPattern = new StringSetting(List.of("MAIL_INTERNALS_PATTERN"), "(.*)");
    this.internalsMailAddressTransform = new StringSetting(List.of("MAIL_INTERNALS_TRANSFORM"), "%s");
    this.externalsMailAddressPattern = new StringSetting(List.of("MAIL_EXTERNALS_PATTERN"), null);
    this.externalsMailAddressTransform = new StringSetting(List.of("MAIL_EXTERNALS_TRANSFORM"), null);

    //
    // Backend settings.
    //
    this.backendConnectTimeout = new DurationSetting(
        List.of("BACKEND_CONNECT_TIMEOUT"),
        ChronoUnit.SECONDS,
        Duration.ofSeconds(5));
    this.backendReadTimeout = new DurationSetting(
        List.of("BACKEND_READ_TIMEOUT"),
        ChronoUnit.SECONDS,
        Duration.ofSeconds(20));
    this.backendWriteTimeout = new DurationSetting(
        List.of("BACKEND_WRITE_TIMEOUT"),
        ChronoUnit.SECONDS,
        Duration.ofSeconds(5));
  }

  // -------------------------------------------------------------------------
  // Settings.
  // -------------------------------------------------------------------------

  /**
   * Scope (within the resource hierarchy) that this application manages
   * access for.
   */
  public final @NotNull StringSetting scope;

  /**
   * Cloud Identity/Workspace customer ID.
   */
  public final @NotNull StringSetting customerId;

  /**
   * Catalog implementation to use.
   */
  public final @NotNull EnumSetting<Catalog> catalog;

  /**
   * Topic (within the resource hierarchy) that binding information will
   * publish to.
   */
  public final @NotNull StringSetting topicName;

  /**
   * Duration for which an activated role remains activated.
   */
  public final @NotNull DurationSetting activationTimeout;

  /**
   * Time allotted for reviewers to approve an activation request.
   */
  public final @NotNull DurationSetting activationRequestTimeout;

  /**
   * Regular expression that justifications must satisfy.
   */
  public final @NotNull StringSetting justificationPattern;

  /**
   * Hint (or description) for users indicating what kind of justification they
   * need to supply.
   */
  public final @NotNull StringSetting justificationHint;

  /**
   * Zone to apply to dates when sending notifications.
   */
  public final @NotNull ZoneIdSetting timeZoneForNotifications;

  /**
   * SMTP server for sending notifications.
   */
  public final @NotNull StringSetting smtpHost;

  /**
   * SMTP port for sending notifications.
   */
  public final @NotNull IntSetting smtpPort;

  /**
   * Enable StartTLS.
   */
  public final @NotNull BooleanSetting smtpEnableStartTls;

  /**
   * Human-readable sender name used for notifications.
   */
  public final @NotNull StringSetting smtpSenderName;

  /**
   * Email address used for notifications.
   */
  public final @NotNull StringSetting smtpSenderAddress;

  /**
   * SMTP username.
   */
  public final @NotNull StringSetting smtpUsername;

  /**
   * SMTP password. For Gmail, this should be an application-specific password.
   */
  public final @NotNull StringSetting smtpPassword;

  /**
   * Path to a SecretManager secret that contains the SMTP password.
   * For Gmail, this should be an application-specific password.
   *
   * The path must be in the format projects/x/secrets/y/versions/z.
   */
  public final @NotNull StringSetting smtpSecret;

  /**
   * Extra JavaMail options.
   */
  public final @NotNull StringSetting smtpExtraOptions;

  /**
   * Regex pattern for capturing the email address of internals to the
   * organization.
   */
  public final StringSetting internalsMailAddressPattern;

  /**
   * String format expression for transforming groups captured by regex pattern
   * into email addresses for internals.
   */
  public final StringSetting internalsMailAddressTransform;

  /**
   * Regex pattern for capturing the email address of externals to the
   * organization.
   */
  public final StringSetting externalsMailAddressPattern;

  /**
   * String format expression for transforming groups captured by regex pattern
   * into email addresses for externals.
   */
  public final StringSetting externalsMailAddressTransform;

  /**
   * Backend Service Id for token validation
   */
  public final @NotNull StringSetting backendServiceId;

  /**
   * Minimum number of reviewers foa an activation request.
   */
  public final @NotNull IntSetting minNumberOfReviewersPerActivationRequest;

  /**
   * Maximum number of reviewers foa an activation request.
   */
  public final @NotNull IntSetting maxNumberOfReviewersPerActivationRequest;

  /**
   * Maximum number of (JIT-) privileges that can be activated at once.
   */
  public final @NotNull IntSetting maxNumberOfPrivilegesPerSelfApproval;

  /**
   * In some cases listing all available projects is not working fast enough and
   * times out,
   * so this method is available as alternative.
   * The format is the same as Google Resource Manager API requires for the query
   * parameter, for example:
   * - parent:folders/{folder_id}
   * - parent:organizations/{organization_id}
   */
  public final @NotNull StringSetting availableProjectsQuery;

  /**
   * Connect timeout for HTTP requests to backends.
   */
  public final @NotNull DurationSetting backendConnectTimeout;

  /**
   * Read timeout for HTTP requests to backends.
   */
  public final @NotNull DurationSetting backendReadTimeout;

  /**
   * Write timeout for HTTP requests to backends.
   */
  public final @NotNull DurationSetting backendWriteTimeout;

  public boolean isSmtpConfigured() {
    var requiredSettings = List.of(smtpHost, smtpPort, smtpSenderName, smtpSenderAddress);
    return requiredSettings.stream().allMatch(s -> s.isValid());
  }

  public boolean isSmtpAuthenticationConfigured() {
    return this.smtpUsername.isValid() &&
        (this.smtpPassword.isValid() || this.smtpSecret.isValid());
  }

  public @NotNull Map<String, String> getSmtpExtraOptionsMap() {
    var map = new HashMap<String, String>();

    if (this.smtpExtraOptions.isValid()) {
      for (var kvp : this.smtpExtraOptions.getValue().split(",")) {
        var parts = kvp.split("=");
        if (parts.length == 2) {
          map.put(parts[0].trim(), parts[1].trim());
        }
      }
    }

    return map;
  }

  public @NotNull Set<String> getRequiredOauthScopes() {
    var scopes = new HashSet<String>();

    scopes.add(ResourceManagerClient.OAUTH_SCOPE);
    scopes.add(PolicyAnalyzerClient.OAUTH_SCOPE);
    scopes.add(AssetInventoryClient.OAUTH_SCOPE);
    scopes.add(IamCredentialsClient.OAUTH_SCOPE);
    scopes.add(SecretManagerClient.OAUTH_SCOPE);

    if (this.catalog.getValue() == RuntimeConfiguration.Catalog.ASSETINVENTORY) {
      scopes.add(DirectoryGroupsClient.OAUTH_SCOPE);
    }

    return scopes;
  }

  // -------------------------------------------------------------------------
  // Inner classes.
  // -------------------------------------------------------------------------

  public abstract class Setting<T> {
    private final Collection<String> keys;
    private final T defaultValue;

    protected abstract T parse(String value);

    protected Setting(Collection<String> keys, T defaultValue) {
      this.keys = keys;
      this.defaultValue = defaultValue;
    }

    public T getValue() {
      for (var key : this.keys) {
        var value = readSetting.apply(key);
        if (value != null) {
          value = value.trim();
          if (!value.isEmpty()) {
            return parse(value);
          }
        }
      }

      if (this.defaultValue != null) {
        return this.defaultValue;
      } else {
        throw new IllegalStateException("No value provided for " + this.keys);
      }
    }

    public boolean isValid() {
      try {
        getValue();
        return true;
      } catch (Exception ignored) {
        return false;
      }
    }
  }

  public class StringSetting extends Setting<String> {
    public StringSetting(Collection<String> keys, String defaultValue) {
      super(keys, defaultValue);
    }

    @Override
    protected String parse(String value) {
      return value;
    }
  }

  public class IntSetting extends Setting<Integer> {
    public IntSetting(Collection<String> keys, Integer defaultValue) {
      super(keys, defaultValue);
    }

    @Override
    protected @NotNull Integer parse(@NotNull String value) {
      return Integer.parseInt(value);
    }
  }

  public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(Collection<String> keys, Boolean defaultValue) {
      super(keys, defaultValue);
    }

    @Override
    protected @NotNull Boolean parse(String value) {
      return Boolean.parseBoolean(value);
    }
  }

  public class DurationSetting extends Setting<Duration> {
    private final ChronoUnit unit;

    public DurationSetting(Collection<String> keys, ChronoUnit unit, Duration defaultValue) {
      super(keys, defaultValue);
      this.unit = unit;
    }

    @Override
    protected Duration parse(@NotNull String value) {
      return Duration.of(Integer.parseInt(value), this.unit);
    }
  }

  public class ZoneIdSetting extends Setting<ZoneId> {
    public ZoneIdSetting(Collection<String> keys) {
      super(keys, ZoneOffset.UTC);
    }

    @Override
    protected @NotNull ZoneId parse(@NotNull String value) {
      return ZoneId.of(value);
    }
  }

  public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final Class<E> enumClass;

    public EnumSetting(
        Class<E> enumClass,
        Collection<String> keys,
        E defaultValue) {
      super(keys, defaultValue);
      this.enumClass = enumClass;
    }

    @Override
    protected @NotNull E parse(@NotNull String value) {
      return E.valueOf(this.enumClass, value.trim().toUpperCase());
    }
  }
}
