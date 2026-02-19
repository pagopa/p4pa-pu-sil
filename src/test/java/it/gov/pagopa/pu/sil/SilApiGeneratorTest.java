package it.gov.pagopa.pu.sil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.github.springwolf.core.asyncapi.schemas.converters.SchemaTitleModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import it.gov.pagopa.pu.sil.util.CertUtilsTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.json.JsonAssert;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, addFilters = false)
@TestPropertySource(properties = {
  "logging.level.org.springdoc.core.utils.SpringDocAnnotationsUtils=OFF",
  "springwolf.enabled=false",
  "springdoc.pathsToMatch=/sil/**"
})
@Slf4j
class SilApiGeneratorTest {

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("rest.integrity-data.private-key", () -> CertUtilsTest.PRIVATE_KEY);
  }

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void init() {
    // removing ModelConverters configured by SpringWolf which will cause the setting of the title in each schema
    boolean openapi31 = true;
    ModelConverters modelConverters = ModelConverters.getInstance(openapi31);
    modelConverters.getConverters().stream()
      .filter(SchemaTitleModelConverter.class::isInstance)
      .forEach(modelConverters::removeConverter);
  }

  @Test
  void generateAndVerifyCommit() throws Exception {
    MvcResult result = mockMvc.perform(
        get("/v3/api-docs")
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
      ).andExpect(status().isOk())
      .andReturn();

    String openApiResult = result.getResponse().getContentAsString()
      .replace("\r", "")
      .replaceAll("\"http://localhost(?::[0-9]+)?\"", "\"https://\\${hostname}/sil\"")
      .replace("\"/sil/", "\"/");

    Assertions.assertTrue(openApiResult.startsWith("{\n  \"openapi\" : \"3."));

    Path openApiGeneratedPath = Path.of("openapi/generated-sil.openapi.json");
    boolean toStore=true;
    if(Files.exists(openApiGeneratedPath)){
      String storedOpenApi = Files.readString(openApiGeneratedPath);
      try {
        JsonAssert.comparator(JsonCompareMode.STRICT).assertIsMatch(storedOpenApi, openApiResult);
        toStore=false;
      } catch (Throwable e){
        log.info("Observed the following changes: {}", e.getMessage());
      }
    }
    if(toStore){
      Files.writeString(openApiGeneratedPath, openApiResult, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    String gitStatus = execCmd("git", "status");
    Assertions.assertFalse(gitStatus.contains("openapi/generated-sil.openapi.json"), "Generated OpenApi not committed");
  }

  public static String execCmd(String... cmd) throws java.io.IOException {
    java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}
