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
package net.codestory.http.payload;

import net.codestory.http.Cookie;
import net.codestory.http.Cookies;
import net.codestory.http.Request;
import net.codestory.http.Response;
import net.codestory.http.misc.Env;
import net.codestory.http.templating.Site;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.codestory.http.constants.HttpStatus.FORBIDDEN;
import static net.codestory.http.constants.HttpStatus.MOVED_PERMANENTLY;
import static net.codestory.http.constants.HttpStatus.NOT_FOUND;
import static net.codestory.http.constants.HttpStatus.NOT_MODIFIED;
import static net.codestory.http.constants.HttpStatus.OK;
import static net.codestory.http.constants.HttpStatus.SEE_OTHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PayloadWriterTest {
  Request request = mock(Request.class);
  Response response = mock(Response.class);
  Cookies cookies = mock(Cookies.class);

  PayloadWriter writer = new PayloadWriter(new Env(), new Site(), request, response);

  @Before
  public void setupContext() throws IOException {
    when(request.cookies()).thenReturn(cookies);
    when(response.outputStream()).thenReturn(new ByteArrayOutputStream());
  }

  @Test
  public void support_string() throws IOException {
    Payload payload = new Payload("Hello");

    assertThat(payload.code()).isEqualTo(200);
    assertThat(writer.getData(payload, "/")).isEqualTo("Hello".getBytes(UTF_8));
    assertThat(writer.getContentType(payload, "/")).isEqualTo("text/html;charset=UTF-8");
  }

  @Test
  public void support_byte_array() throws IOException {
    byte[] bytes = "Hello".getBytes(UTF_8);

    Payload payload = new Payload(bytes);

    assertThat(writer.getData(payload, "/")).isSameAs(bytes);
    assertThat(writer.getContentType(payload, "/")).isEqualTo("application/octet-stream");
  }

  @Test
  public void support_bean_to_json() throws IOException {
    Payload payload = new Payload(new Person("NAME", 42));

    assertThat(writer.getData(payload, "/")).isEqualTo("{\"name\":\"NAME\",\"age\":42}".getBytes(UTF_8));
    assertThat(writer.getContentType(payload, "/")).isEqualTo("application/json;charset=UTF-8");
  }

  @Test
  public void support_custom_content_type() throws IOException {
    Payload payload = new Payload("text/plain", "Hello");

    assertThat(writer.getData(payload, "/")).isEqualTo("Hello".getBytes(UTF_8));
    assertThat(writer.getContentType(payload, "/")).isEqualTo("text/plain");
  }

  @Test
  public void support_stream() throws IOException {
    Payload payload = new Payload("text/plain", new ByteArrayInputStream("Hello".getBytes()));

    assertThat(writer.getData(payload, "/")).isEqualTo("Hello".getBytes(UTF_8));
    assertThat(writer.getContentType(payload, "/")).isEqualTo("text/plain");
  }

  @Test
  public void support_present_optional() throws IOException {
    Payload payload = new Payload("text/plain", Optional.of("TEXT"));

    assertThat(writer.getData(payload, "/")).isEqualTo("TEXT".getBytes(UTF_8));
    assertThat(writer.getContentType(payload, "/")).isEqualTo("text/plain");
  }

  @Test
  public void json_cookie() {
    Payload payload = Payload.ok();

    payload.withCookie("person", new Person("Bob", 42));

    Cookie cookie = payload.cookies().get(0);
    assertThat(cookie.name()).isEqualTo("person");
    assertThat(cookie.value()).isEqualTo("{\"name\":\"Bob\",\"age\":42}");
  }

  @Test
  public void support_absent_optional() throws IOException {
    Payload payload = new Payload("text/plain", Optional.empty());
    writer.write(payload);

    verify(response).setStatus(NOT_FOUND);
    verify(response).setContentLength(0);
    verifyNoMoreInteractions(response);
  }

  @Test
  public void redirect() throws IOException {
    Payload payload = Payload.seeOther("/url");
    writer.write(payload);

    verify(response).setValue("Location", "/url");
    verify(response).setStatus(SEE_OTHER);
    verify(response).setContentLength(0);
    verifyNoMoreInteractions(response);
  }

  @Test
  public void forbidden() throws IOException {
    Payload payload = Payload.forbidden();
    writer.write(payload);

    verify(response).setStatus(FORBIDDEN);
    verify(response).setContentLength(0);
    verifyNoMoreInteractions(response);
  }

  @Test
  public void permanent_move() throws IOException {
    Payload payload = Payload.movedPermanently("/url");
    writer.write(payload);

    verify(response).setValue("Location", "/url");
    verify(response).setStatus(MOVED_PERMANENTLY);
    verify(response).setContentLength(0);
    verifyNoMoreInteractions(response);
  }

  @Test
  public void last_modified() throws IOException {
    Payload payload = new Payload(Paths.get("hello.md"));
    writer.write(payload);

    verify(response).setValue(eq("Last-Modified"), anyString());
  }

  @Test
  public void etag() throws IOException {
    Payload payload = new Payload("Hello");
    writer.write(payload);

    verify(response).setStatus(OK);
    verify(response).setValue("ETag", "8b1a9953c4611296a827abf8c47804d7");
  }

  @Test
  public void not_modified() throws IOException {
    when(request.header("If-None-Match")).thenReturn("8b1a9953c4611296a827abf8c47804d7");

    Payload payload = new Payload("Hello");
    writer.write(payload);

    verify(response).setStatus(NOT_MODIFIED);
  }

  @Test
  public void head() throws IOException {
    when(request.method()).thenReturn("HEAD");

    Payload payload = new Payload("Hello");
    writer.write(payload);

    verify(response).setStatus(OK);
    verify(response, never()).setContentLength(anyInt());
    verify(response, never()).outputStream();
  }

  static class Person {
    String name;
    int age;

    Person(String name, int age) {
      this.name = name;
      this.age = age;
    }
  }
}
