/**
 * Copyright (C) 2013 all@code-story.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package net.codestory.http.io;

import java.io.*;
import java.nio.charset.*;

public class Resources {
  private static final int BUF_SIZE = 0x1000; // 4K

  private Resources() {
    // Static utility class
  }

  public static String toString(String name, Charset charset) throws IOException {
    try (InputStream stream = ClassLoader.getSystemResourceAsStream(name)) {
      if (null == stream) {
        return "";
      }

      StringBuilder string = new StringBuilder();
      char[] buffer = new char[BUF_SIZE];

      try (Reader from = new InputStreamReader(stream, charset)) {
        int count;
        while (-1 != (count = from.read(buffer))) {
          string.append(buffer, 0, count);
        }
      }

      return string.toString();
    }
  }
}