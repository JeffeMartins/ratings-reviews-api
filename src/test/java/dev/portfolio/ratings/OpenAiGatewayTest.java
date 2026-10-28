package dev.portfolio.ratings;

import static org.assertj.core.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import dev.portfolio.ratings.ai.OpenAiGateway;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenAiGatewayTest {
  @Test
  void sendsStatelessRequestAndParsesMessageOutput() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var request = new AtomicReference<String>();
    server.createContext(
        "/v1/responses",
        exchange -> {
          request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] response =
              "{\"status\":\"completed\",\"output\":[{\"type\":\"reasoning\"},{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"Bom som; amostra pequena.\"}]}]}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    try {
      var gateway =
          new OpenAiGateway(
              "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
              "fake-test-key",
              "test-model");
      assertThat(gateway.analyze("[]")).contains("Bom som");
      assertThat(request.get())
          .contains("\"store\":false", "\"max_output_tokens\":500", "test-model");
    } finally {
      server.stop(0);
    }
  }

  @Test
  void rejectsIncompleteProviderResponse() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/responses",
        exchange -> {
          byte[] body =
              "{\"status\":\"incomplete\",\"output\":[]}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      assertThatThrownBy(
              () ->
                  new OpenAiGateway(
                          "http://127.0.0.1:" + server.getAddress().getPort(), "fake", "test")
                      .analyze("[]"))
          .isInstanceOf(IllegalStateException.class);
    } finally {
      server.stop(0);
    }
  }
}
