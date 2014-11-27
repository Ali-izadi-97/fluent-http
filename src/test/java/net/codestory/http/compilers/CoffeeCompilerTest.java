/**
 * Copyright (C) 2013-2014 all@code-story.net
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
package net.codestory.http.compilers;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;

import org.junit.*;
import org.junit.rules.*;

public class CoffeeCompilerTest {
  private static CoffeeCompiler compiler = new CoffeeCompiler(false);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void empty() {
    String js = compiler.compile(Paths.get("empty.coffee"), "");

    assertThat(js).isEqualTo("\n\n//# sourceMappingURL=empty.coffee.map");
  }

  @Test
  public void to_javascript() {
    String js = compiler.compile(Paths.get("file.coffee"), "life=42");

    assertThat(js).isEqualTo("var life;\n\nlife = 42;\n\n//# sourceMappingURL=file.coffee.map");
  }

  @Test
  public void dont_set_sourcemap_in_prod_mode() {
    CoffeeCompiler compiler = new CoffeeCompiler(true);

    String js = compiler.compile(Paths.get("file.coffee"), "life=42");

    assertThat(js).isEqualTo("var life;\n\nlife = 42;\n");
  }

  @Test
  public void invalid_script() {
    thrown.expect(CompilerException.class);
    thrown.expectMessage("Unable to compile invalid.coffee:1:1: error: unexpected ==");

    compiler.compile(Paths.get("invalid.coffee"), "===");
  }

  @Test
  public void report_line_number() {
    thrown.expectMessage("Unable to compile invalid.coffee:3:1: error: unexpected ==");

    compiler.compile(Paths.get("invalid.coffee"), "\n\n===");
  }

  @Test
  public void literate_coffee() {
    String js = compiler.compile(Paths.get("file.litcoffee"),
      "Comment text\n" +
        "\n" +
        "    life=42\n" +
        "\n"
    );

    assertThat(js).isEqualTo("var life;\n\nlife = 42;\n\n//# sourceMappingURL=file.litcoffee.map");
  }
}
