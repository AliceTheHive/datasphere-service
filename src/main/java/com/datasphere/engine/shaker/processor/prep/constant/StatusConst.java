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

package com.datasphere.engine.shaker.processor.prep.constant;

import java.util.HashSet;
import java.util.Set;

public abstract class StatusConst {
    //Submit状态
    //准备
    public static final String SUB_PREP = "SUB_PREP";
    //成功
    public static final String SUB_SUCC = "SUB_SUCC";
    //失败
    public static final String SUB_FAIL = "SUB_FAIL";
    //Job状态
    //准备
    public static final String JOB_PREP = "JOB_PREP";
    //运行
    public static final String JOB_RUNN = "JOB_RUNN";
    //挂起
    public static final String JOB_SUSP = "JOB_SUSP";
    //成功
    public static final String JOB_SUCC = "JOB_SUCC";
    //杀死
    public static final String JOB_KILL = "JOB_KILL";
    //失败
    public static final String JOB_FAIL = "JOB_FAIL";

    public static final Set<String> SUB_STATUS_SET = new HashSet<String>() {{
        add(SUB_PREP); add(SUB_SUCC); add(SUB_FAIL);
    }};

    public static final Set<String> JOB_STATUS_SET = new HashSet<String>() {{
        add(JOB_PREP); add(JOB_RUNN); add(JOB_SUSP); add(JOB_SUCC); add(JOB_KILL); add(JOB_FAIL);
    }};
}