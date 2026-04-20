import com.github.jk1.license.filter.*
import com.github.jk1.license.render.*
import java.util.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  java
  id("org.springframework.boot") version "4.0.5"
  id("io.spring.dependency-management") version "1.1.7"
  jacoco
  id("org.sonarqube") version "7.2.3.7755"
  id("com.github.ben-manes.versions") version "0.53.0"
  id("org.openapi.generator") version "7.21.0"
  id("org.ajoberstar.grgit") version "5.3.2"
  id("com.gorylenko.gradle-git-properties") version "2.5.7"
  //code generation for soap webservices classes (via  jaxb)
  id("com.intershop.gradle.jaxb") version "8.0.1"
  id("com.github.jk1.dependency-license-report") version "3.1.2"
}

group = "it.gov.pagopa.payhub"
version = "0.0.1"
description = "p4pa-pu-sil"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
  compileClasspath {
    resolutionStrategy.activateDependencyLocking()
  }
}

licenseReport {
  renderers =
    arrayOf(XmlReportRenderer("third-party-libs.xml", "Back-End Libraries"))
  outputDir = "$projectDir/dependency-licenses"
  filters = arrayOf(SpdxLicenseBundleNormalizer())
}
tasks.classes {
  finalizedBy(tasks.generateLicenseReport)
}

repositories {
  mavenCentral()
}

val springDocOpenApiVersion = "3.0.2"
val janinoVersion = "3.1.12"
val openApiToolsVersion = "0.2.10"
val micrometerVersion = "1.6.4"
val httpClientVersion = "5.6"
val httpCoreVersion = "5.4.2"
val springWolfAsyncApiVersion = "1.20.0"
val springWolfUiAsyncApiVersion = "1.20.0"
val podamVersion = "8.0.2.RELEASE"
val jaxbVersion = "4.0.7"
val jaxbApiVersion = "4.0.5"
val activationVersion = "2.1.4"
val xmlSchemaVersion = "2.3.2"
val caffeineVersion = "3.2.3"
val javaJwtVersion = "4.5.1"
val jwksRsaVersion = "0.23.0"
val bouncycastleVersion = "1.83"
val nimbusJoseJwtVersion = "10.9"
val commonsLang3Version = "3.20.0"
val lz4JavaVersion = "1.10.4"

// fix cve
val jackson3CoreVersion = "3.1.1"

val springCloudDepsVersion = "2025.1.1"

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudDepsVersion")
  }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
  implementation("org.springframework.boot:spring-boot-starter-restclient")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.data:spring-data-commons")
  implementation("org.springframework.boot:spring-boot-starter-web-services")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("com.nimbusds:nimbus-jose-jwt:$nimbusJoseJwtVersion")
  implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
  implementation("org.springframework.cloud:spring-cloud-starter-stream-kafka") {
    exclude(group = "org.lz4", module = "lz4-java")
  }
  implementation("at.yawk.lz4:lz4-java:$lz4JavaVersion")
  implementation("io.micrometer:micrometer-tracing-bridge-otel:$micrometerVersion")
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocOpenApiVersion") {
    exclude(group = "org.apache.commons", module = "commons-lang3")
  }
  implementation("org.apache.commons:commons-lang3:$commonsLang3Version")
  implementation("org.codehaus.janino:janino:$janinoVersion")
  implementation("io.github.springwolf:springwolf-kafka:${springWolfAsyncApiVersion}") {
    exclude(group = "org.lz4", module = "lz4-java")
  }
  implementation("io.github.springwolf:springwolf-ui:${springWolfAsyncApiVersion}")
  implementation("io.github.springwolf:springwolf-cloud-stream:${springWolfAsyncApiVersion}")
  implementation("org.openapitools:jackson-databind-nullable:$openApiToolsVersion")
  implementation("org.apache.httpcomponents.client5:httpclient5:$httpClientVersion")
  implementation("org.apache.httpcomponents.core5:httpcore5:$httpCoreVersion")

  // validation token jwt
  implementation("com.auth0:java-jwt:${javaJwtVersion}")
  implementation("com.auth0:jwks-rsa:${jwksRsaVersion}")
  implementation("org.bouncycastle:bcprov-jdk18on:${bouncycastleVersion}")

  //webservice soap
  implementation("org.apache.ws.xmlschema:xmlschema-core:$xmlSchemaVersion")
  runtimeOnly("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")
  //jaxb
  jaxb("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")
  jaxb("com.sun.xml.bind:jaxb-xjc:$jaxbVersion")
  jaxb("com.sun.xml.bind:jaxb-jxc:$jaxbVersion")
  jaxb("com.sun.xml.bind:jaxb-core:$jaxbVersion")
  jaxb("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbApiVersion")
  jaxb("jakarta.activation:jakarta.activation-api:$activationVersion")
  jaxbext("com.github.jaxb-xew-plugin:jaxb-xew-plugin:2.1")
  jaxbext("org.jvnet.jaxb:jaxb-plugins:4.0.0")

  // CVE fix
  implementation("tools.jackson.core:jackson-core:$jackson3CoreVersion")

  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  testAnnotationProcessor("org.projectlombok:lombok")

  //	Testing
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  testImplementation("org.springframework.boot:spring-boot-starter-security-test")
  testImplementation("org.mockito:mockito-core")
  testImplementation("org.projectlombok:lombok")
  testImplementation("uk.co.jemos.podam:podam:${podamVersion}")
}

tasks.withType<Test> {
  useJUnitPlatform()
  finalizedBy(tasks.jacocoTestReport)
}

val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
  mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}
tasks {
  jar {
      from("${rootProject.projectDir}") {
          include("LICENSE.md")
          into("META-INF")
      }
  }
  test {
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
    testLogging.events = setOf(TestLogEvent.FAILED)
    testLogging.exceptionFormat = TestExceptionFormat.FULL
  }
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required = true
  }
}

val projectInfo = mapOf(
  "artifactId" to project.name,
  "version" to project.version
)

tasks {
  val processResources by getting(ProcessResources::class) {
    filesMatching("**/application.yml") {
      expand(projectInfo)
    }
  }
}

tasks.compileJava {
  dependsOn("dependenciesBuild")
}

tasks.register("dependenciesBuild") {
  group = "AutomaticallyGeneratedCode"
  description = "grouping all together automatically generate code tasks"

  dependsOn(
    "openApiGeneratePUSIL",
    "openApiGenerateP4PAAUTH",
    "openApiGenerateP4PASENDNOTIFICATION",
    "openApiGeneratePROCESSEXECUTION",
    "openApiGenerateDEBTPOSITIONS",
    "openApiGenerateORGANIZATION",
    "openApiGenerateREGISTRIES",
    "openApiGenerateWORKFLOWHUB",
    "openApiGenerateFILESHARE",
    "openApiGeneratePAGOPAPAYMENTS",
    "openApiGenerateNodeCheckout",
    "openApiGeneratePaymentNotification",
    "openApiGenerateLegacyPaymentNotification",
    "openApiGenerateActualization",
    "openApiGenerateLegacyActualization",
    "jaxbJavaGenPuForOrganizationPayments",
    "jaxbJavaGenPuForOrganizationReconciliation",
    "openApiGenerateCLASSIFICATION"
  )
}

configure<SourceSetContainer> {
  named("main") {
    java.srcDir("$projectDir/build/generated/src/main/java")
  }
}

springBoot {
  buildInfo()
  mainClass.value("it.gov.pagopa.pu.sil.Pu2SilApplication")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGeneratePUSIL") {
  group = "openapi"
  description = "openapi"

  generatorName.set("spring")
  inputSpec.set("$rootDir/openapi/p4pa-pu-sil.openapi.yaml")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.sil.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.sil.dto.generated")
  configOptions.set(
    mapOf(
      "dateLibrary" to "java8",
      "requestMappingMode" to "api_interface",
      "useSpringBoot3" to "true",
      "interfaceOnly" to "true",
      "useTags" to "true",
      "useBeanValidation" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  typeMappings.set(
    mapOf(
      "CreateNotificationRequest" to "it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest",
      "CreateNotificationResponse" to "it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse",
      "SendNotificationDTO" to "it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO",
      "LegalFactDownloadMetadataDTO" to "it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDownloadMetadataDTO",
      "ImportFileType" to "it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFile.IngestionFlowFileTypeEnum",
      "ExportFileType" to "it.gov.pagopa.pu.processexecutions.dto.generated.ExportFile.ExportFileTypeEnum",
      "ImportStatusType" to "it.gov.pagopa.pu.processexecutions.dto.generated.IngestionFlowFileStatus",
      "DebtPositionDTO" to "it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO",
      "PaidInstallmentDTO" to "it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentDTO",
      "OffsetDateTimeIntervalFilter" to "it.gov.pagopa.pu.processexecutions.dto.generated.OffsetDateTimeIntervalFilter",
      "ClassificationsExportFileFilter" to "it.gov.pagopa.pu.processexecutions.dto.generated.ClassificationsExportFileFilter",
      "PersonDTO" to "it.gov.pagopa.pu.debtpositions.dto.generated.PersonDTO",
      "InstallmentStatus" to "it.gov.pagopa.pu.debtpositions.dto.generated.InstallmentStatus",
      "PaymentOptionType" to "it.gov.pagopa.pu.debtpositions.dto.generated.PaymentOptionType",
      "Action" to "it.gov.pagopa.pu.debtpositions.dto.generated.Action",
      "PersonEntityType" to "it.gov.pagopa.pu.debtpositions.dto.generated.PersonEntityType",
      "LegalFactCategoryDTO" to "it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactCategoryDTO",
      "LegalFactDTO" to "it.gov.pagopa.pu.sendnotification.dto.generated.LegalFactDTO"
    )
  )
}

var targetEnv = when (Objects.requireNonNullElse(
  System.getProperty("targetBranch"),
  grgit.branch.current().name
)) {
  "uat" -> "uat"
  "main" -> "main"
  else -> "develop"
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateP4PAAUTH") {
  group = "openapi"
  description = "openapi"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-auth/refs/heads/$targetEnv/openapi/p4pa-auth.openapi.yaml")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.auth.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.auth.dto.generated")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "serializableModel" to "true",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true",
      "useBeanValidation" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateP4PASENDNOTIFICATION") {
  group = "openapi"
  description = "openapi"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-send-notification/refs/heads/$targetEnv/openapi/generated.openapi.json")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.sendnotification.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.sendnotification.dto.generated")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "serializableModel" to "true",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true",
      "useBeanValidation" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGeneratePROCESSEXECUTION") {
  group = "openapi"
  description = "openapi"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-process-executions/refs/heads/$targetEnv/openapi/generated.openapi.json")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.processexecutions.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.processexecutions.dto.generated")
  typeMappings.set(
    mapOf(
      "LocalDateTime" to "java.time.LocalDateTime"
    )
  )
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "serializableModel" to "true",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "useBeanValidation" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateDEBTPOSITIONS") {
  group = "openapi"
  description = "openapi"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-debt-positions/refs/heads/$targetEnv/openapi/generated.openapi.json")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.debtpositions.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.debtpositions.dto.generated")
  typeMappings.set(
    mapOf(
      "LocalDateTime" to "java.time.LocalDateTime"
    )
  )
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "serializableModel" to "true",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true",
      "useBeanValidation" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateNodeCheckout") {
  group = "openapi"
  description = "openapi"

  generatorName.set("java")
  inputSpec.set("$rootDir/openapi/external/node_checkout.yaml")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.nodo.checkout.controller.generated")
  modelPackage.set("it.gov.pagopa.nodo.checkout.dto.generated")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "serializableModel" to "true",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true",
      "useBeanValidation" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateORGANIZATION") {
  group = "openapi"
  description = "openapi"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-organization/refs/heads/$targetEnv/openapi/generated.openapi.json")
  outputDir.set("$projectDir/build/generated")
  invokerPackage.set("it.gov.pagopa.pu.organization.generated")
  apiPackage.set("it.gov.pagopa.pu.organization.client.generated")
  modelPackage.set("it.gov.pagopa.pu.organization.dto.generated")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "serializableModel" to "true",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true",
      "useBeanValidation" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateLegacyActualization") {
  group = "openapi"
  description = "openapi"

  generatorName.set("java")
  inputSpec.set("$rootDir/openapi/external/amount-updates-legacy.yaml")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.actualization.legacy.controller.generated")
  modelPackage.set("it.gov.pagopa.actualization.legacy.dto.generated")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "serializableModel" to "true",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true",
      "useBeanValidation" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}


tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateActualization") {
  group = "openapi"
  description = "openapi"

  generatorName.set("java")
  inputSpec.set("$rootDir/openapi/external/amount-updates.yaml")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.actualization.controller.generated")
  modelPackage.set("it.gov.pagopa.actualization.dto.generated")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "serializableModel" to "true",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true",
      "useBeanValidation" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateWORKFLOWHUB") {
  group = "openapi"
  description = "openapi"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-workflow-hub/refs/heads/$targetEnv/openapi/p4pa-workflow-hub.openapi.yaml")
  outputDir.set("$projectDir/build/generated")
  invokerPackage.set("it.gov.pagopa.pu.workflowhub.generated")
  apiPackage.set("it.gov.pagopa.pu.workflowhub.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.workflowhub.dto.generated")
  typeMappings.set(
    mapOf(
      "DebtPositionDTO" to "it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO",
      "IngestionFlowFileType" to "String",
      "WfExecutionConfig" to "tools.jackson.databind.JsonNode",
      "ExportFileType" to "String",
      "WorkflowTypeOrg" to "String",
      "ScheduleEnum" to "String",
      "WorkflowExecutionStatus" to "String"
    )
  )
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "serializableModel" to "true",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "useOneOfInterfaces" to "true",
      "useBeanValidation" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "enumPropertyNaming" to "original",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}

jaxb {
  javaGen {
    register("puForOrganizationPayments") {
      args = listOf("-wsdl")
      outputDir = file("$projectDir/build/generated/jaxb/java")
      schema =
        file("$rootDir/src/main/resources/soap/wsdl/payments/puForOrganization-payments.wsdl")
      bindings =
        layout.files("$rootDir/src/main/resources/soap/wsdl/payments/puForOrganization-payments.xjb")
    }
  }
  javaGen {
    register("puForOrganizationReconciliation") {
      args = listOf("-wsdl")
      outputDir = file("$projectDir/build/generated/jaxb/java")
      schema =
        file("$rootDir/src/main/resources/soap/wsdl/reconciliation/puForOrganization-reconciliation.wsdl")
      bindings =
        layout.files("$rootDir/src/main/resources/soap/wsdl/reconciliation/puForOrganization-reconciliation.xjb")
    }
  }

  tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateREGISTRIES") {
    group = "openapi"
    description = "openapi"

    generatorName.set("java")
    remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-registries/refs/heads/$targetEnv/openapi/generated.openapi.json")
    outputDir.set("$projectDir/build/generated")
    apiPackage.set("it.gov.pagopa.pu.registries.controller.generated")
    modelPackage.set("it.gov.pagopa.pu.registries.dto.generated")
    configOptions.set(
      mapOf(
        "swaggerAnnotations" to "false",
        "openApiNullable" to "false",
        "dateLibrary" to "java8",
        "serializableModel" to "true",
        "useSpringBoot3" to "true",
        "useJakartaEe" to "true",
        "useOneOfInterfaces" to "true",
        "useBeanValidation" to "true",
        "serializationLibrary" to "jackson",
        "generateSupportingFiles" to "true",
        "generateConstructorWithAllArgs" to "true",
        "generatedConstructorWithRequiredArgs" to "true",
        "enumPropertyNaming" to "original",
        "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
      )
    )
    library.set("resttemplate")
  }

  tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateFILESHARE") {
    group = "openapi"
    description = "openapi"

    generatorName.set("java")
    remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-fileshare/refs/heads/$targetEnv/openapi/p4pa-fileshare.openapi.yaml")
    outputDir.set("$projectDir/build/generated")
    apiPackage.set("it.gov.pagopa.pu.fileshare.controller.generated")
    modelPackage.set("it.gov.pagopa.pu.fileshare.dto.generated")
    typeMappings.set(
      mapOf(
        "StartNotificationResponse" to "String"
      )
    )
    configOptions.set(
      mapOf(
        "swaggerAnnotations" to "false",
        "openApiNullable" to "false",
        "dateLibrary" to "java8",
        "serializableModel" to "true",
        "useSpringBoot3" to "true",
        "useJakartaEe" to "true",
        "useOneOfInterfaces" to "true",
        "useBeanValidation" to "true",
        "serializationLibrary" to "jackson",
        "generateSupportingFiles" to "true",
        "useAbstractionForFiles" to "true",
        "generateConstructorWithAllArgs" to "true",
        "generatedConstructorWithRequiredArgs" to "true",
        "enumPropertyNaming" to "original",
        "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
      )
    )
    library.set("resttemplate")
  }

  tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGeneratePaymentNotification") {
    group = "openapi"
    description = "openapi"

    generatorName.set("java")
    inputSpec.set("$rootDir/openapi/external/payment-notification.yaml")
    outputDir.set("$projectDir/build/generated")
    apiPackage.set("it.gov.pagopa.paymentnotification.controller.generated")
    modelPackage.set("it.gov.pagopa.paymentnotification.dto.generated")
    configOptions.set(
      mapOf(
        "swaggerAnnotations" to "false",
        "openApiNullable" to "false",
        "dateLibrary" to "java8",
        "serializableModel" to "true",
        "useSpringBoot3" to "true",
        "useJakartaEe" to "true",
        "useOneOfInterfaces" to "true",
        "useBeanValidation" to "true",
        "serializationLibrary" to "jackson",
        "generateSupportingFiles" to "true",
        "generateConstructorWithAllArgs" to "true",
        "generatedConstructorWithRequiredArgs" to "true",
        "enumPropertyNaming" to "original",
        "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
      )
    )
    library.set("resttemplate")
  }

  tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateLegacyPaymentNotification") {
    group = "openapi"
    description = "openapi"

    generatorName.set("java")
    inputSpec.set("$rootDir/openapi/external/payment-notification-legacy.yaml")
    outputDir.set("$projectDir/build/generated")
    apiPackage.set("it.gov.pagopa.paymentnotification.legacy.controller.generated")
    modelPackage.set("it.gov.pagopa.paymentnotification.legacy.dto.generated")
    configOptions.set(
      mapOf(
        "swaggerAnnotations" to "false",
        "openApiNullable" to "false",
        "dateLibrary" to "java8",
        "serializableModel" to "true",
        "useSpringBoot3" to "true",
        "useJakartaEe" to "true",
        "useOneOfInterfaces" to "true",
        "useBeanValidation" to "true",
        "serializationLibrary" to "jackson",
        "generateSupportingFiles" to "true",
        "generateConstructorWithAllArgs" to "true",
        "generatedConstructorWithRequiredArgs" to "true",
        "enumPropertyNaming" to "original",
        "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
      )
    )
    library.set("resttemplate")
  }

  tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateCLASSIFICATION") {
    group = "openapi"
    description = "openapi"

    generatorName.set("java")
    remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-classification/refs/heads/$targetEnv/openapi/generated.openapi.json")
    outputDir.set("$projectDir/build/generated")
    invokerPackage.set("it.gov.pagopa.pu.classification.generated")
    apiPackage.set("it.gov.pagopa.pu.classification.client.generated")
    modelPackage.set("it.gov.pagopa.pu.classification.dto.generated")
    configOptions.set(
      mapOf(
        "swaggerAnnotations" to "false",
        "openApiNullable" to "false",
        "dateLibrary" to "java8",
        "serializableModel" to "true",
        "useSpringBoot3" to "true",
        "useJakartaEe" to "true",
        "useOneOfInterfaces" to "true",
        "useBeanValidation" to "true",
        "serializationLibrary" to "jackson",
        "generateSupportingFiles" to "true",
        "generateConstructorWithAllArgs" to "true",
        "generatedConstructorWithRequiredArgs" to "true",
        "enumPropertyNaming" to "original",
        "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
      )
    )
    library.set("resttemplate")
    typeMappings.set(
      mapOf(
        "LocalDateTime" to "java.time.LocalDateTime"
      )
    )
  }

  tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGeneratePAGOPAPAYMENTS") {
    group = "openapi"
    description = "openapi"

    generatorName.set("java")
    remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-pagopa-payments/refs/heads/$targetEnv/openapi/p4pa-pagopa-payments.openapi.yaml")
    outputDir.set("$projectDir/build/generated")
    invokerPackage.set("it.gov.pagopa.pu.pagopapayments.generated")
    apiPackage.set("it.gov.pagopa.pu.pagopapayments.client.generated")
    modelPackage.set("it.gov.pagopa.pu.pagopapayments.dto.generated")
    typeMappings.set(
      mapOf(
        "DebtPositionDTO" to "it.gov.pagopa.pu.debtpositions.dto.generated.DebtPositionDTO",
      )
    )
    configOptions.set(
      mapOf(
        "swaggerAnnotations" to "false",
        "openApiNullable" to "false",
        "dateLibrary" to "java8",
        "useSpringBoot3" to "true",
        "serializableModel" to "true",
        "useJakartaEe" to "true",
        "useOneOfInterfaces" to "true",
        "useBeanValidation" to "true",
        "serializationLibrary" to "jackson",
        "generateSupportingFiles" to "true",
        "useAbstractionForFiles" to "true",
        "generateConstructorWithAllArgs" to "true",
        "generatedConstructorWithRequiredArgs" to "true",
        "enumPropertyNaming" to "original",
        "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
      )
    )
    library.set("resttemplate")
  }
}
