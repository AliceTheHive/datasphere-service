/*
 * Copyright 2019, Huahuidata, Inc.
 * DataSphere is licensed under the Mulan PSL v1.
 * You can use this software according to the terms and conditions of the Mulan PSL v1.
 * You may obtain a copy of Mulan PSL v1 at:
 * http://license.coscl.org.cn/MulanPSL
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 * See the Mulan PSL v1 for more details.
 */

package com.datasphere.server.common.saml;

import org.springframework.security.saml.SAMLCredential;

import com.datasphere.server.domain.user.User;

public class SAMLBaiduUserMapper extends SAMLUserMapper{

  @Override
  public User createUser(SAMLCredential samlCredential) {
    User baiduUser = new User();
    if(samlCredential != null){
    		baiduUser.setTel(getAttributeValue(samlCredential, "mobilePhone"));
    		baiduUser.setFullName(getAttributeValue(samlCredential, "displayName"));
    		baiduUser.setEmail(getAttributeValue(samlCredential, "email"));
    }
    return baiduUser;
  }
}