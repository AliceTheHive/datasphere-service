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

package com.datasphere.server.domain.workbook.configurations.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Created by kyungtaak on 2016. 9. 11..
 */
@JsonTypeName("like")
public class LikeFilter extends Filter {

  /**
   * Like 구문, Wildcard 관련 문자('_', '%') 포함 <br/>
   * Escape 문자(기본값 '\') 지원
   *
   */
  String expr;

  @JsonIgnore
  String escapeChar = "\\";

  public LikeFilter() {
    // Empty Constructor
  }

  public LikeFilter(String field, String expr) {
    super(field);
    this.expr = expr;
  }

  public LikeFilter(String field, String expr, String ref) {
    super(field, ref);
    this.expr = expr;
  }

  @Override
  public boolean compare(Filter filter) {
    if(!(filter instanceof LikeFilter)) {
      return false;
    }

    if(expr != null && expr.equals(((LikeFilter) filter).getExpr())) {
      return true;
    }

    return false;
  }

  public String getExpr() {
    return expr;
  }

  public void setExpr(String expr) {
    this.expr = expr;
  }

  public String getEscapeChar() {
    return escapeChar;
  }

  public void setEscapeChar(String escapeChar) {
    this.escapeChar = escapeChar;
  }
}
