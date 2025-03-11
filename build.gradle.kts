plugins {
  java
  id("org.springframework.boot") version "3.4.3"
  id("io.spring.dependency-management") version "1.1.7"
  jacoco
  id("org.sonarqube") version "6.0.1.5171"
  id("com.github.ben-manes.versions") version "0.51.0"
  id("org.openapi.generator") version "7.10.0"
  id("org.ajoberstar.grgit") version "5.3.0"
  id("com.gorylenko.gradle-git-properties") version "2.5.0"
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

val springDocOpenApiVersion = "2.8.5"
val openApiToolsVersion = "0.2.6"
val micrometerVersion = "1.4.3"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("io.micrometer:micrometer-tracing-bridge-otel:$micrometerVersion")
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocOpenApiVersion")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  implementation("org.openapitools:jackson-databind-nullable:$openApiToolsVersion")

  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  testAnnotationProcessor("org.projectlombok:lombok")

  //	Testing
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.mockito:mockito-core")
  testImplementation("org.projectlombok:lombok")
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
    "openApiGenerate",
    "openApiGenerateP4PAAUTH",
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

openApiGenerate {
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
}

var targetEnv = when (grgit.branch.current().name) {
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
