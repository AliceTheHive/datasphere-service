/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datasphere.server.query.druid.aggregations;

import com.fasterxml.jackson.annotation.JsonTypeName;

import com.datasphere.server.query.druid.Aggregation;

/**
 * Created by aladin on 2016. 8. 9..
 */
@JsonTypeName("sum")
public class GenericSumAggregation implements Aggregation {

  String name;
  String fieldName;
  String fieldExpression;
  String inputType;

  public GenericSumAggregation(String name, String fieldName, String inputType) {
    this.name = name;
    this.fieldName = fieldName;
    this.inputType = inputType;
  }

  public GenericSumAggregation(String name, String fieldName, String fieldExpression, String inputType) {
    this.name = name;
    this.fieldName = fieldName;
    this.fieldExpression = fieldExpression;
    this.inputType = inputType;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getInputType() {
    return inputType;
  }

  public void setInputType(String inputType) {
    this.inputType = inputType;
  }

  public String getFieldExpression() {
    return fieldExpression;
  }

  public void setFieldExpression(String fieldExpression) {
    this.fieldExpression = fieldExpression;
  }

  @Override
  public String toString() {
    return "GenericSumAggregation{" +
            "name='" + name + '\'' +
            ", fieldName='" + fieldName + '\'' +
            ", inputType='" + inputType + '\'' +
            '}';
  }
}
