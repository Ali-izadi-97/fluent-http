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
package net.codestory.http;

import java.io.*;
import java.net.*;
import java.util.*;

import net.codestory.http.dev.*;
import net.codestory.http.errors.*;
import net.codestory.http.routes.*;

import com.sun.net.httpserver.*;

public class WebServer {
  private final HttpServer server;
  private final DevMode devMode = new DevMode();
  private final RouteCollection routes = new RouteCollection(devMode);

  public WebServer() {
    try {
      server = HttpServer.create();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create http server", e);
    }
  }

  public WebServer configure(Configuration configuration) {
    configuration.configure(routes);
    devMode.setLastConfiguration(configuration);
    return this;
  }

  public WebServer startOnRandomPort() {
    Random random = new Random();

    for (int i = 0; i < 20; i++) {
      try {
        int port = 8183 + random.nextInt(1000);
        start(port);
        return this;
      } catch (Exception e) {
        System.err.println("Unable to bind server: " + e);
      }
    }

    throw new IllegalStateException("Unable to start server");
  }

  public WebServer start(int port) {
    try {
      server.bind(new InetSocketAddress(port), 0);
      server.createContext("/", this::onRequest);
      server.start();
    } catch (IOException e) {
      throw new IllegalStateException("Unable to bind the web server on port " + port);
    }
    return this;
  }

  public int port() {
    return server.getAddress().getPort();
  }

  public void stop() {
    server.stop(0);
  }

  protected void onRequest(HttpExchange exchange) {
    try {
      applyRoutes(exchange);
    } catch (Exception e) {
      try {
        onError(exchange, e);
      } catch (IOException ioe) {
        e.printStackTrace(); // Cannot do much
      }
    } finally {
      exchange.close();
    }
  }

  protected void applyRoutes(HttpExchange exchange) throws IOException {
    if (!routes.apply(exchange)) {
      onPageNotFound(exchange);
    }
  }

  protected void onError(HttpExchange exchange, Exception e) throws IOException {
    if (DevMode.isDevMode()) {
      errorPage(500, e).writeTo(exchange);
    } else {
      errorPage(500, null).writeTo(exchange);
    }
  }

  protected void onPageNotFound(HttpExchange exchange) throws IOException {
    errorPage(404, null).writeTo(exchange);
  }

  protected Payload errorPage(int code, Exception e) throws IOException {
    return new ErrorPage(code, e).payload();
  }
}
