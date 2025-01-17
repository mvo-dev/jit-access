//
// Copyright 2021 Google LLC
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

package com.google.solutions.jitaccess.core;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Primary email address of a user.
 */
public class UserEmail implements Comparable<UserEmail> {
  public final @NotNull String email;

  public UserEmail(@NotNull String email) {
    Preconditions.checkNotNull(email, "email");
    this.email = email;
  }

  @Override
  public String toString() {
    return this.email;
  }

  // -------------------------------------------------------------------------
  // Equality.
  // -------------------------------------------------------------------------

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UserEmail userEmail = (UserEmail) o;
    return email.equals(userEmail.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email);
  }

  @Override
  public int compareTo(@NotNull UserEmail o) {
    return this.email.compareTo(o.email);
  }
}
