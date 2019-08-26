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

package com.datasphere.server.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.datasphere.server.common.exception.GlobalErrorCodes.BAD_REQUEST_CODE;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Bad Request")
public class BadRequestException extends MetatronException {

  public BadRequestException(String message) {
    super(BAD_REQUEST_CODE, message);
  }

  public BadRequestException(Throwable cause) {
    super(BAD_REQUEST_CODE, cause);
  }

  public BadRequestException(String message, Throwable cause) {
    super(BAD_REQUEST_CODE, message, cause);
  }
}
