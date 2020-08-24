/*
 * Copyright 2020 Google LLC
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package com.google.cloud.healthcare.fdamystudies.service;

import com.google.cloud.healthcare.fdamystudies.beans.AppOrgInfoBean;
import com.google.cloud.healthcare.fdamystudies.usermgmt.model.ActivityLog;

public interface CommonService {

  public String validatedUserAppDetailsByAllApi(
      String userId, String email, String appId, String orgId);

  public AppOrgInfoBean getUserAppDetailsByAllApi(
      String userId, String emailId, String appId, String orgId);

  public ActivityLog createActivityLog(String userId, String activityName, String activtyDesc);
}
