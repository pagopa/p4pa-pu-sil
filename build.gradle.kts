import java.util.*

plugins {
  java
  id("org.springframework.boot") version "3.5.3"
  id("io.spring.dependency-management") version "1.1.7"
  jacoco
  id("org.sonarqube") version "6.2.0.5505"
  id("com.github.ben-manes.versions") version "0.52.0"
  id("org.openapi.generator") version "7.13.0"
  id("org.ajoberstar.grgit") version "5.3.2"
  id("com.gorylenko.gradle-git-properties") version "2.5.0"
  //code generation for soap webservices classes (via  jaxb)
  id("com.intershop.gradle.jaxb") version "7.0.1"
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
}

repositories {
  mavenCentral()
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1")
  }
}

val springDocOpenApiVersion = "2.8.9"
val openApiToolsVersion = "0.2.6"
val micrometerVersion = "1.5.1"
val httpClientVersion = "5.5"
val springWolfAsyncApiVersion = "1.13.0"
val podamVersion = "8.0.2.RELEASE"
val jaxbVersion = "4.0.5"
val jaxbApiVersion = "4.0.2"
val activationVersion = "2.1.3"
val wsdl4jVersion = "1.6.3"
val xmlSchemaVersion = "2.3.1"
val caffeineVersion = "3.2.1"
val javaJwtVersion = "4.5.0"
val jwksRsaVersion = "0.22.2"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-web-services")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
  implementation("org.springframework.cloud:spring-cloud-starter-stream-kafka")
  implementation("io.micrometer:micrometer-tracing-bridge-otel:$micrometerVersion")
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocOpenApiVersion")
  implementation("io.github.springwolf:springwolf-kafka:${springWolfAsyncApiVersion}")
  implementation("io.github.springwolf:springwolf-ui:${springWolfAsyncApiVersion}")
  implementation("io.github.springwolf:springwolf-cloud-stream:${springWolfAsyncApiVersion}")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("org.openapitools:jackson-databind-nullable:$openApiToolsVersion")
  implementation("org.apache.httpcomponents.client5:httpclient5:$httpClientVersion")

  // validation token jwt
  implementation("com.auth0:java-jwt:${javaJwtVersion}")
  implementation("com.auth0:jwks-rsa:${jwksRsaVersion}")

  //webservice soap
  implementation("wsdl4j:wsdl4j:$wsdl4jVersion")
  implementation("org.apache.ws.xmlschema:xmlschema-core:$xmlSchemaVersion")
  runtimeOnly("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")
  //jaxb
  jaxb("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")
  jaxb("com.sun.xml.bind:jaxb-xjc:$jaxbVersion")
  jaxb("com.sun.xml.bind:jaxb-jxc:$jaxbVersion")
  jaxb("com.sun.xml.bind:jaxb-core:$jaxbVersion")
  jaxb("jakarta.xml.bind:jakarta.xml.bind-api:$jaxbApiVersion")
  jaxb("jakarta.activation:jakarta.activation-api:$activationVersion")
  jaxbext("org.jvnet.jaxb:jaxb-plugin-annotate:3.0.2")
  jaxbext("org.slf4j:slf4j-simple:2.0.16") // see https://github.com/IntershopCommunicationsAG/jaxb-gradle-plugin/issues/37

  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  testAnnotationProcessor("org.projectlombok:lombok")

  //	Testing
  testImplementation("org.springframework.boot:spring-boot-starter-test")
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
  test {
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
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

configurations {
  compileClasspath {
    resolutionStrategy.activateDependencyLocking()
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
    "openApiGenerateNodeCheckout",
    "openApiGenerateLegacyPaymentNofication",
    "openApiGenerateActualizationLegacy",
    "jaxbJavaGenPuForOrganizationPayments",
    "jaxbJavaGenPuForOrganizationReconciliation",
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
  description = "description"

  generatorName.set("spring")
  inputSpec.set("$rootDir/openapi/p4pa-pu-sil.openapi.yaml")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.sil.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.sil.dto.generated")
  configOptions.set(mapOf(
    "dateLibrary" to "java8",
    "requestMappingMode" to "api_interface",
    "useSpringBoot3" to "true",
    "interfaceOnly" to "true",
    "useTags" to "true",
    "useBeanValidation" to "true",
    "generateConstructorWithAllArgs" to "true",
    "generatedConstructorWithRequiredArgs" to "true",
    "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
  ))
  typeMappings.set(mapOf(
    "CreateNotificationRequest" to "it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationRequest",
    "CreateNotificationResponse" to "it.gov.pagopa.pu.sendnotification.dto.generated.CreateNotificationResponse",
    "SendNotificationDTO" to "it.gov.pagopa.pu.sendnotification.dto.generated.SendNotificationDTO"
  ))
}

var targetEnv = when (Objects.requireNonNullElse(System.getProperty("targetBranch"), grgit.branch.current().name)) {
  "uat" -> "uat"
  "main" -> "main"
  else -> "develop"
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateP4PAAUTH") {
  group = "openapi"
  description = "description"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-auth/refs/heads/$targetEnv/openapi/p4pa-auth.openapi.yaml")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.auth.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.auth.dto.generated")
  configOptions.set(mapOf(
    "swaggerAnnotations" to "false",
    "openApiNullable" to "false",
    "dateLibrary" to "java8",
    "useSpringBoot3" to "true",
    "useJakartaEe" to "true",
    "serializationLibrary" to "jackson",
    "generateSupportingFiles" to "true",
    "generateConstructorWithAllArgs" to "true",
    "generatedConstructorWithRequiredArgs" to "true",
    "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
  ))
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateP4PASENDNOTIFICATION") {
  group = "openapi"
  description = "description"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-send-notification/refs/heads/$targetEnv/openapi/generated.openapi.json")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.sendnotification.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.sendnotification.dto.generated")
  configOptions.set(mapOf(
    "swaggerAnnotations" to "false",
    "openApiNullable" to "false",
    "dateLibrary" to "java8",
    "useSpringBoot3" to "true",
    "useJakartaEe" to "true",
    "serializationLibrary" to "jackson",
    "generateSupportingFiles" to "true",
    "generateConstructorWithAllArgs" to "true",
    "generatedConstructorWithRequiredArgs" to "true",
    "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
  ))
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGeneratePROCESSEXECUTION") {
  group = "openapi"
  description = "description"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-process-executions/refs/heads/$targetEnv/openapi/generated.openapi.json")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.processexecutions.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.processexecutions.dto.generated")
  typeMappings.set(mapOf(
    "LocalDateTime" to "java.time.LocalDateTime"
  ))
  configOptions.set(mapOf(
    "swaggerAnnotations" to "false",
    "openApiNullable" to "false",
    "dateLibrary" to "java8",
    "useSpringBoot3" to "true",
    "useJakartaEe" to "true",
    "serializationLibrary" to "jackson",
    "generateSupportingFiles" to "true",
    "generateConstructorWithAllArgs" to "true",
    "generatedConstructorWithRequiredArgs" to "true",
    "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
  ))
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateDEBTPOSITIONS") {
  group = "openapi"
  description = "description"

  generatorName.set("java")
  remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-debt-positions/refs/heads/$targetEnv/openapi/generated.openapi.json")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.pu.debtpositions.controller.generated")
  modelPackage.set("it.gov.pagopa.pu.debtpositions.dto.generated")
  typeMappings.set(mapOf(
    "LocalDateTime" to "java.time.LocalDateTime"
  ))
  configOptions.set(mapOf(
    "swaggerAnnotations" to "false",
    "openApiNullable" to "false",
    "dateLibrary" to "java8",
    "useSpringBoot3" to "true",
    "useJakartaEe" to "true",
    "serializationLibrary" to "jackson",
    "generateSupportingFiles" to "true",
    "generateConstructorWithAllArgs" to "true",
    "generatedConstructorWithRequiredArgs" to "true",
    "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
  ))
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateNodeCheckout") {
  group = "openapi"
  description = "description"

  generatorName.set("java")
  inputSpec.set("$rootDir/openapi/node_checkout.yaml")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.nodo.checkout.controller.generated")
  modelPackage.set("it.gov.pagopa.nodo.checkout.dto.generated")
  configOptions.set(mapOf(
    "swaggerAnnotations" to "false",
    "openApiNullable" to "false",
    "dateLibrary" to "java8",
    "useSpringBoot3" to "true",
    "useJakartaEe" to "true",
    "serializationLibrary" to "jackson",
    "generateSupportingFiles" to "true",
    "generateConstructorWithAllArgs" to "true",
    "generatedConstructorWithRequiredArgs" to "true",
    "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
  ))
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateORGANIZATION") {
  group = "AutomaticallyGeneratedCode"
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
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    )
  )
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateActualizationLegacy") {
  group = "openapi"
  description = "description"

  generatorName.set("java")
  inputSpec.set("$rootDir/openapi/amount-updates-legacy.yaml")
  outputDir.set("$projectDir/build/generated")
  apiPackage.set("it.gov.pagopa.actualization.legacy.controller.generated")
  modelPackage.set("it.gov.pagopa.actualization.legacy.dto.generated")
  configOptions.set(mapOf(
    "swaggerAnnotations" to "false",
    "openApiNullable" to "false",
    "dateLibrary" to "java8",
    "useSpringBoot3" to "true",
    "useJakartaEe" to "true",
    "serializationLibrary" to "jackson",
    "generateSupportingFiles" to "true",
    "generateConstructorWithAllArgs" to "true",
    "generatedConstructorWithRequiredArgs" to "true",
    "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
  ))
  library.set("resttemplate")
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateWORKFLOWHUB") {
  group = "AutomaticallyGeneratedCode"
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
      "WfExecutionConfig" to "com.fasterxml.jackson.databind.JsonNode",
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
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
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
      schema = file("$rootDir/src/main/resources/soap/wsdl/payments/puForOrganization-payments.wsdl")
      bindings = layout.files("$rootDir/src/main/resources/soap/wsdl/payments/puForOrganization-payments.xjb")
    }
  }
  javaGen {
    register("puForOrganizationReconciliation") {
      args = listOf("-wsdl")
      outputDir = file("$projectDir/build/generated/jaxb/java")
      schema = file("$rootDir/src/main/resources/soap/wsdl/reconciliation/puForOrganization-reconciliation.wsdl")
      bindings = layout.files("$rootDir/src/main/resources/soap/wsdl/reconciliation/puForOrganization-reconciliation.xjb")
    }
  }

  tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateREGISTRIES") {
    group = "openapi"
    description = "description"

    generatorName.set("java")
    remoteInputSpec.set("https://raw.githubusercontent.com/pagopa/p4pa-registries/refs/heads/$targetEnv/openapi/generated.openapi.json")
    outputDir.set("$projectDir/build/generated")
    apiPackage.set("it.gov.pagopa.pu.registries.controller.generated")
    modelPackage.set("it.gov.pagopa.pu.registries.dto.generated")
    configOptions.set(mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    ))
    library.set("resttemplate")
  }

  tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("openApiGenerateLegacyPaymentNofication") {
    group = "openapi"
    description = "description"

    generatorName.set("java")
    inputSpec.set("$rootDir/openapi/payment-notification-legacy.yaml")
    outputDir.set("$projectDir/build/generated")
    apiPackage.set("it.gov.pagopa.paymentnotification.legacy.controller.generated")
    modelPackage.set("it.gov.pagopa.paymentnotification.legacy.dto.generated")
    configOptions.set(mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "false",
      "dateLibrary" to "java8",
      "useSpringBoot3" to "true",
      "useJakartaEe" to "true",
      "serializationLibrary" to "jackson",
      "generateSupportingFiles" to "true",
      "generateConstructorWithAllArgs" to "true",
      "generatedConstructorWithRequiredArgs" to "true",
      "additionalModelTypeAnnotations" to "@lombok.experimental.SuperBuilder(toBuilder = true)"
    ))
    library.set("resttemplate")
  }
}
