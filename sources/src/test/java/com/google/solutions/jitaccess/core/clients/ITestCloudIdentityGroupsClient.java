//
// Copyright 2024 Google LLC
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

package com.google.solutions.jitaccess.core.clients;

import com.google.solutions.jitaccess.core.*;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ITestCloudIdentityGroupsClient {
  private final String INVALID_CUSTOMER_ID = "Cinvalid";
  private static final GroupEmail TEST_GROUP_EMAIL = new GroupEmail(
    String.format(
      "jitaccess-test@%s",
      ITestEnvironment.CLOUD_IDENTITY_DOAMIN));

  @BeforeEach
  public void setUp() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    try {
      var groupId = new GroupId(
        client.getGroup(TEST_GROUP_EMAIL).getName(),
        TEST_GROUP_EMAIL);
      client.deleteGroup(groupId);
    }
    catch (AccessDeniedException ignored) {
      //
      // Group doesn't exist, ok.
      //
    }
  }

  //---------------------------------------------------------------------
  // getGroup.
  //---------------------------------------------------------------------

  @Test
  public void whenUnauthenticated_ThenGetGroupThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.INVALID_CREDENTIAL,
      new CloudIdentityGroupsClient.Options(INVALID_CUSTOMER_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      NotAuthenticatedException.class,
      () -> client.getGroup(new GroupEmail("test@example.com")));
  }

  @Test
  public void whenCallerLacksPermission_ThenGetGroupThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.NO_ACCESS_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      AccessDeniedException.class,
      () -> client.getGroup(new GroupEmail("test@example.com")));
  }

  @Test
  public void whenGroupNotFound_ThenGetThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      AccessDeniedException.class,
      () -> client.getGroup(new GroupEmail(String.format(
        "jitaccess-doesnotexist@%s",
        ITestEnvironment.CLOUD_IDENTITY_DOAMIN))));
  }

  @Test
  public void getGroup() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    client.createGroup(TEST_GROUP_EMAIL, "description");
    var group = client.getGroup(TEST_GROUP_EMAIL);

    assertEquals(TEST_GROUP_EMAIL.email, group.getGroupKey().getId());
  }

  //---------------------------------------------------------------------
  // createGroup.
  //---------------------------------------------------------------------

  @Test
  public void whenUnauthenticated_ThenCreateGroupThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.INVALID_CREDENTIAL,
      new CloudIdentityGroupsClient.Options(INVALID_CUSTOMER_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      NotAuthenticatedException.class,
      () -> client.createGroup(
        new GroupEmail("test@example.com"),
        "test group"));
  }

  @Test
  public void whenCustomerIdDoesNotMatchDomain_ThenCreateGroupThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.NO_ACCESS_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      IllegalArgumentException.class,
      () -> client.createGroup(
        new GroupEmail("doesnotexist@google.com"),
        "test group"));
  }

  @Test
  public void createGroupIsIdempotent() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    //
    // Delete group if it exists.
    //
    var oldId = client.createGroup(TEST_GROUP_EMAIL, "test group");
    client.deleteGroup(oldId);

    //
    // Create.
    //
    var createdId = client.createGroup(TEST_GROUP_EMAIL, "test group");
    assertNotEquals(oldId, createdId);

    //
    // Create again.
    //
    var sameId = client.createGroup(TEST_GROUP_EMAIL, "test group");
    assertNotEquals(oldId, createdId);
  }

  //---------------------------------------------------------------------
  // deleteGroup.
  //---------------------------------------------------------------------

  @Test
  public void whenUnauthenticated_ThenDeleteGroupThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.INVALID_CREDENTIAL,
      new CloudIdentityGroupsClient.Options(INVALID_CUSTOMER_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      NotAuthenticatedException.class,
      () -> client.deleteGroup(new GroupId("1", "test@example.com")));
  }

  @Test
  public void whenGroupIdInvalid_ThenDeleteGroupThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.NO_ACCESS_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      IllegalArgumentException.class,
      () -> client.deleteGroup(new GroupId("1", "doesnotexist@google.com")));
  }

  //---------------------------------------------------------------------
  // getMembership.
  //---------------------------------------------------------------------

  @Test
  public void whenGroupIdInvalid_ThenGetMembershipThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.NO_ACCESS_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      IllegalArgumentException.class,
      () -> client.getMembership(
        new GroupId("1", "doesnotexist@google.com"),
        ITestEnvironment.NO_ACCESS_USER));
  }

  @Test
  public void getMembership() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    var groupId = client.createGroup(TEST_GROUP_EMAIL, "test group");
    var id = client.addMembership(
      groupId,
      ITestEnvironment.TEMPORARY_ACCESS_USER,
      Instant.now().plusSeconds(300));
    var membership = client.getMembership(
      groupId,
      ITestEnvironment.TEMPORARY_ACCESS_USER);

    assertEquals(id.id(), membership.getName());
  }

  //---------------------------------------------------------------------
  // addMembership.
  //---------------------------------------------------------------------

  @Test
  public void whenUnauthenticated_ThenAddMembershipThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.INVALID_CREDENTIAL,
      new CloudIdentityGroupsClient.Options(INVALID_CUSTOMER_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      NotAuthenticatedException.class,
      () -> client.addMembership(
        new GroupId("1", "test@example.com"),
        new UserEmail("user@example.com"),
        Instant.now().plusSeconds(300)));
  }

  @Test
  public void whenGroupIdInvalid_ThenAddMembershipThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.NO_ACCESS_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      IllegalArgumentException.class,
      () -> client.addMembership(
        new GroupId("1", "doesnotexist@google.com"),
        new UserEmail("user@example.com"),
        Instant.now().plusSeconds(300)));
  }

  @Test
  public void addMembershipIsIdempotent() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);
    var groupId = client.createGroup(TEST_GROUP_EMAIL, "test group");
    var userEmail = ITestEnvironment.TEMPORARY_ACCESS_USER;

    //
    // Add member twice.
    //
    var expiry = Instant.now().plusSeconds(300);
    var id = client.addMembership(groupId, userEmail, expiry);
    var sameId = client.addMembership(groupId, userEmail, expiry);

    assertEquals(id, sameId);
  }

  @Test
  public void whenExpiryIsInThePast_ThenAddMembershipThrowsException() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    var groupId = client.createGroup(TEST_GROUP_EMAIL, "test group");

    assertThrows(
      IllegalArgumentException.class,
      () -> client.addMembership(
        groupId,
        ITestEnvironment.TEMPORARY_ACCESS_USER,
        Instant.now().minusSeconds(300)));
  }

  @Test
  public void whenMembershipExists_ThenAddMembershipUpdatesExpiry() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    var groupId = client.createGroup(TEST_GROUP_EMAIL, "test group");
    var userEmail = ITestEnvironment.TEMPORARY_ACCESS_USER;

    //
    // Add membership with an initial expiry.
    //
    var initialExpiry = Instant.now().plusSeconds(300);
    client.addMembership(groupId, userEmail, initialExpiry);

    assertEquals(
      initialExpiry.truncatedTo(ChronoUnit.SECONDS),
      Instant.parse(client
        .getMembership(groupId, userEmail)
        .getRoles()
        .get(0)
        .getExpiryDetail()
        .getExpireTime()));

    //
    // Change expiry.
    //
    var newExpiry = Instant.now().plusSeconds(400);
    client.addMembership(groupId, userEmail, newExpiry);

    assertEquals(
      newExpiry.truncatedTo(ChronoUnit.SECONDS),
      Instant.parse(client
        .getMembership(groupId, userEmail)
        .getRoles()
        .get(0)
        .getExpiryDetail()
        .getExpireTime()));
  }

  //---------------------------------------------------------------------
  // deleteMembership.
  //---------------------------------------------------------------------

  @Test
  public void whenUnauthenticated_ThenDeleteMembershipThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.INVALID_CREDENTIAL,
      new CloudIdentityGroupsClient.Options(INVALID_CUSTOMER_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      NotAuthenticatedException.class,
      () -> client.deleteMembership(
        new CloudIdentityGroupsClient.MembershipId("groups/1/memberships/1")));
  }

  @Test
  public void deleteMembershipIsIdempotent() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    var groupId = client.createGroup(TEST_GROUP_EMAIL, "test group");
    var id = client.addMembership(
      groupId,
      ITestEnvironment.TEMPORARY_ACCESS_USER,
      Instant.now().plusSeconds(300));

    //
    // Delete twice.
    //
    client.deleteMembership(id);
    client.deleteMembership(id);

    //
    // Check deletion was effective.
    //
    assertThrows(
      NotFoundException.class,
      () -> client.getMembership(id));
  }

  //---------------------------------------------------------------------
  // listMemberships.
  //---------------------------------------------------------------------

  @Test
  public void whenUnauthenticated_ThenListMembershipsThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.INVALID_CREDENTIAL,
      new CloudIdentityGroupsClient.Options(INVALID_CUSTOMER_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      NotAuthenticatedException.class,
      () -> client.listMemberships(TEST_GROUP_EMAIL));
  }

  @Test
  public void listMemberships() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    var groupId = client.createGroup(TEST_GROUP_EMAIL, "test group");
    client.addMembership(
      groupId,
      ITestEnvironment.TEMPORARY_ACCESS_USER,
      Instant.now().plusSeconds(300));

    var memberships = client.listMemberships(TEST_GROUP_EMAIL);
    assertEquals(2, memberships.size());

    var members = memberships
      .stream()
      .map(m -> m.getPreferredMemberKey().getId())
      .collect(Collectors.toSet());

    assertTrue(members.contains(ITestEnvironment.TEMPORARY_ACCESS_USER.email));
  }

  //---------------------------------------------------------------------
  // listMembershipsByUser.
  //---------------------------------------------------------------------

  @Test
  public void whenUnauthenticated_ThenListMembershipsByUserThrowsException() {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.INVALID_CREDENTIAL,
      new CloudIdentityGroupsClient.Options(INVALID_CUSTOMER_ID),
      HttpTransport.Options.DEFAULT);

    assertThrows(
      NotAuthenticatedException.class,
      () -> client.listMembershipsByUser(
        ITestEnvironment.TEMPORARY_ACCESS_USER));
  }

  @Test
  public void ListMembershipsByUser() throws Exception {
    var client = new CloudIdentityGroupsClient(
      ITestEnvironment.APPLICATION_CREDENTIALS,
      new CloudIdentityGroupsClient.Options(
        ITestEnvironment.CLOUD_IDENTITY_ACCOUNT_ID),
      HttpTransport.Options.DEFAULT);

    var memberships = client.listMembershipsByUser(
      ITestEnvironment.TEMPORARY_ACCESS_USER);
    assertEquals(0, memberships.size());
  }
}
