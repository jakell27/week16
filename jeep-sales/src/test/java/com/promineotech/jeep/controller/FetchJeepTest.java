package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import com.promineotech.jeep.Constants;
import com.promineotech.jeep.controller.support.FetchJeepTestSupport;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.service.JeepSalesService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = {"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
    "classpath:flyway/migrations/V1.1__Jeep_Data.sql"}, config = @SqlConfig(encoding = "utf-8"))
class FetchJeepTest extends FetchJeepTestSupport {

  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  @Sql(
      scripts = {"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
          "classpath:flyway/migrations/V1.1__Jeep_Data.sql"},
      config = @SqlConfig(encoding = "utf-8"))

  class TestsThatDoNotPolluteTheApplicationContext extends FetchJeepTestSupport {
    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int serverPort;

    @Test
    void testThatJeepsAreReturnedWhenAValidModelAndTrimAreSupplied() {
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Sport";
      String uri =
          String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);

      ResponseEntity<List<Jeep>> response =
          restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      List<Jeep> actual = response.getBody();
      List<Jeep> expected = buildExpected();

      assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testThatErrorMessageIsReturnedWhenUnknownTrimIsSupplied() {
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Unknown Value";
      String uri =
          String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);
      // connection made
      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
      // Not found 404 is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      // error message is returned
      Map<String, Object> error = response.getBody();

      assertErrorMessageValid(error, HttpStatus.NOT_FOUND);
    }
  }

  static Stream<Arguments> parametersForInvalidInput() {
    // @formatter:off
    return Stream.of(
        arguments("WRANGLER", "sdfa", "Trim contains non-alphanumeric characters"),
        arguments("WRANGLER", "C".repeat(Constants.TRIM_MAX_LENGTH + 1), "Trim length, too long"),
        arguments("INVALID", "Sport", "Model is not enum value")
        //@formatter:on
    );

  }
  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  @Sql(
      scripts = {"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
          "classpath:flyway/migrations/V1.1__Jeep_Data.sql"},
      config = @SqlConfig(encoding = "utf-8"))

  class TestsThatPolluteTheApplicationContext extends FetchJeepTestSupport {
    @Autowired
    private TestRestTemplate restTemplate;
    @LocalServerPort
    private int serverPort;

    @MockBean
    private JeepSalesService jeepSalesService;
    @Test
    void testThatUnplannedErrorResultsInA500Status() {
      JeepModel model = JeepModel.WRANGLER;
      String trim = "invalid";
      String uri =
          String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);
     
      doThrow(new RuntimeException("ouch")).when(jeepSalesService).fetchJeeps(model, trim);
      
      // connection made
      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(uri, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});
      // internal server error 500 is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      // error message is returned
      Map<String, Object> error = response.getBody();

      assertErrorMessageValid(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
  




  /**
   * @param error
   */
  protected void assertErrorMessageValid(Map<String, Object> error, HttpStatus status) {
    //@formatter:off
    assertThat(error)
      .containsKey("message")
      .containsEntry("status code", status.value())
      .containsEntry("uri", "/jeeps")
      .containsKey("timestamp")
      .containsEntry("reason", status.getReasonPhrase());
    //@formatter:on
  }


}
