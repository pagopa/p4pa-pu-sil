# p4pa-pu-sil

This application belong to the **inbound/outbound** tier of the **Piattaforma Unitaria** product.

See [PU Microservice Architecture](https://pagopa.atlassian.net/wiki/spaces/SPAC/pages/1405845916/Architettura+microservizi) for more details.

## 🧱 Role

* To exchange data with SILs (`Sistema Informativo Locale`), or organizations' services:
  * It authorizes requests retrieving user info through [p4pa-auth](https://github.com/pagopa/p4pa-auth);


## 🌐 APIs
See [OpenAPI](openapi/generated-internal.openapi.json):
* APIs exposed towards internal microservices;
See [OpenAPI](openapi/generated-sil.openapi.json):
* APIs exposed towards the SILs.
Both exposed through the path:
* `/swagger-ui/index.html`

See [PagamentiTelematiciDovutiPagati WSDL](src/main/resources/soap/wsdl/payments/puForOrganization-payments.wsdl), exposed through the following path:
* `/soap/payments/PagamentiTelematiciDovutiPagati.wsdl`
  * Exposed to the SIL in order to handle payments data exchange.

See [PagamentiTelematiciPagatiRiconciliati WSDL](src/main/resources/soap/wsdl/payments/puForOrganization-payments.wsdl), exposed through the following path:
* `/soap/reconciliation/PagamentiTelematiciPagatiRiconciliati.wsdl`
  * Exposed to the SIL in order to handle reconciliation data exchange.

See [Postman collection](/postman/p4pa-pu-sil.postman_collection.json) and [Postman Environment](https://pagopa.atlassian.net/wiki/spaces/SPAC/pages/1094615081/Environment+collection+postman).

### 📌 Relevant internal APIs
* `GET /internal/payment-notification-push`: To notify a payment towards the SIL;
* `GET /internal/actualization`: To ask the SIL the payment notification fee.

### 📌 Relevant PagamentiTelematiciDovutiPagati SOAP endpoints
* `paaSILAutorizzaImportFlusso`: To retrieve a URl (towards [p4pa-fileshare](https://github.com/pagopa/p4pa-fileshare)) where to upload a file containing debt positions to import;
* `paaSILChiediStatoImportFlusso`: To get debt position file processing status;
* `paaSILImportaDovuto`: To create/update/cancel or ask for the pdf notice of a single debt position;
* `paaSILInviaDovuti`: To create one or more debt position and obtain a URL towards pagoPa checkout to pay it;
* `paaSILChiediPagati`: To verify the payment status of debt positions paid through `paaSILInviaDovuti`;
* `paaSILChiediPagatiConRicevuta`: As `paaSILChiediPagati`, it will return also the XML receipt (RT);
* `paaSILInviaCarrelloDovuti`: As `paaSILInviaDovuti`, extended to support multiple beneficiary organizations;
* `paaSILChiediEsitoCarrelloDovuti`: To verify the payment status of debt positions paid through `paaSILInviaCarrelloDovuti`;
* `paaSILPrenotaExportFlusso`: To request the export of paid installments data;
* `paaSILPrenotaExportFlussoIncrementaleConRicevuta`: As `paaSILPrenotaExportFlusso`, it allows to get the XML receipt (RT) and filter through receipt received date instead of the payment date;
* `paaSILChiediStatoExportFlusso`: To request the status of paid installments export processing;
* `paaSILVerificaAvviso`: To obtain a URL towards pagoPa checkout to pay a debt position already stored in PU.

### 📌 Relevant PagamentiTelematiciPagatiRiconciliati SOAP endpoints
* `pivotSILAutorizzaImportFlusso`: To retrieve a URL (towards [p4pa-fileshare](https://github.com/pagopa/p4pa-fileshare)) where to upload a file containing payment notifications to import;
* `pivotSILChiediStatoImportFlusso`: To get payment notifications file processing status;
* `pivotSILAutorizzaImportFlussoTesoreria`: To retrieve a URL (towards [p4pa-fileshare](https://github.com/pagopa/p4pa-fileshare)) where to upload a file containing treasuries to import;
* `pivotSILChiediStatoImportFlussoTesoreria`: To get treasuries file processing status;
* `pivotSILPrenotaExportFlussoRiconciliazione`: To request the export of classifications data;
* `pivotSILChiediStatoExportFlussoRiconciliazione`: To request the status of classifications export processing;
* `pivotSILChiediAccertamento`: To get an assessment;

### 📌 Common HTTP status returned:
* `401`: Invalid access token provided, thus a new login is required;
* `403`: Trying to access a not authorized resource.

## 🌐 AsyncAPIs
See [AsyncAPI](asyncapi/generated.asyncapi.json), exposed through the following path:
* `/springwolf/asyncapi-ui.html`

## 🔎 Monitoring
See available actuator endpoints through the following path:
* `/actuator`

### 📌 Relevant endpoints
* Health (provide an accessToken to see details): `/actuator/health`
  * Liveness: `/actuator/health/liveness`
  * Readiness: `/actuator/health/readiness`
* Metrics: `/actuator/metrics`
  * Prometheus: `/actuator/prometheus`

Further endpoints are exposed through the JMX console.

## ✏️ Logging
See [log configured pattern](/src/main/resources/logback-spring.xml).

## 🔗 Dependencies

### 🧩 Microservices
* [p4pa-auth](https://github.com/pagopa/p4pa-auth):
  * To validate access token and retrieve user info;
* [p4pa-classification](https://github.com/pagopa/p4pa-classification):
  * To access to domain data and operations;
* [p4pa-debt-positions](https://github.com/pagopa/p4pa-debt-positions):
  * To access to domain data and operations;
* [p4pa-organization](https://github.com/pagopa/p4pa-organization):
  * To access to domain data and operations;
* [p4pa-process-executions](https://github.com/pagopa/p4pa-process-executions):
  * To access to domain data and operations;
* [p4pa-pagopa-payments](https://github.com/pagopa/p4pa-pagopa-payments):
  * To get payment pdf notice;
* [p4pa-send-notification](https://github.com/pagopa/p4pa-send-notification):
  * To send notification using PagoPa SEND service;
* [p4pa-workflow-hub](https://github.com/pagopa/p4pa-workflow-hub):
  * To get workflow execution status;

### 🌍 External
* SIL - organization exposed services:
  * [actualization service openApi](openapi/amount-updates.yaml): To ask the SIL the notification fee;
    * [legacy openApi](openapi/amount-updates-legacy.yaml)
  * [payment push notification service openApi](openapi/payment-notification.yaml): To inform the SIL about the payment event (RT received);
    * [legacy openApi](openapi/payment-notification-legacy.yaml)
* pagoPA `Nodo dei Pagamenti SPC` - PagoPA services to handle payments:
  * [EC checkout openApi](openapi/node_checkout.yaml): To configure the payment session and return a URL where ask the user to proceed with it.

## 🔧 Configuration

See [application.yml](src/main/resources/application.yml) for each configurable property.

### 📌 Relevant configurations

#### 🌐 Application Server
| ENV               | DESCRIPTION                                                                     | DEFAULT               |
|-------------------|---------------------------------------------------------------------------------|-----------------------|
| SERVER_PORT       | Application server listening port                                               | 8080                  |
| SIL_WSDL_BASE_URL | Base URL through which the service is exposed, used to expose the WSDL document | http://localhost:8080 |

#### ✏️ Logging
| ENV                                   | DESCRIPTION                                                                                                                                                                     | DEFAULT |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| LOG_LEVEL_ROOT                        | Base level                                                                                                                                                                      | INFO    |
| LOG_LEVEL_PAGOPA                      | Base level of custom classes                                                                                                                                                    | INFO    |
| LOG_LEVEL_SPRING                      | Level applied to Spring framework                                                                                                                                               | INFO    |
| LOG_LEVEL_SPRING_BOOT_AVAILABILITY    | To print availability events                                                                                                                                                    | DEBUG   |
| LOGGING_LEVEL_API_REQUEST_EXCEPTION   | Level applied to APIs exception                                                                                                                                                 | INFO    |
| LOG_LEVEL_PERFORMANCE_LOG             | Level applied to [PerformanceLog](https://pagopa.atlassian.net/wiki/spaces/SPAC/pages/1540096383/Logging#2.2.-Log-di-performance)                                               | INFO    |
| LOG_LEVEL_PERFORMANCE_LOG_API_REQUEST | Level applied to [API Performance Log](https://pagopa.atlassian.net/wiki/spaces/SPAC/pages/1540096383/Logging#2.2.2.1.-Log-di-perfomance-per-le-API)                            | INFO    |
| LOG_LEVEL_PERFORMANCE_LOG_REST_INVOKE | Level applied to [REST invoke Performance Log](https://pagopa.atlassian.net/wiki/spaces/SPAC/pages/1540096383/Logging#2.2.2.2.-Log-di-performance-per-i-servizi-REST-integrati) | INFO    |

#### 🔁 Integrations

##### 📋 [Caching](https://pagopa.atlassian.net/wiki/spaces/SPAC/pages/1542128077/Caching)
| ENV                                 | DESCRIPTION                                     | DEFAULT |
|-------------------------------------|-------------------------------------------------|---------|
| CACHE_ORGANIZATION_SIZE             | Organization data cache size                    | 1000    |
| CACHE_ORGANIZATION_MINUTES          | Organization data cache retention (minutes)     | 60      |
| CACHE_TAXONOMY_SIZE                 | Taxonomy data cache size                        | 100     |
| CACHE_TAXONOMY_MINUTES              | Taxonomy data cache retention (minutes)         | 60      |
| CACHE_DEBT_POSITION_TYPE_SIZE       | DebtPositionType data cache size                | 100     |
| CACHE_DEBT_POSITION_TYPE_MINUTES    | DebtPositionType data cache retention (minutes) | 60      |
| CACHE_DEBTPOSITION_TYPE_ORG_SIZE    | DebtPositionTypeOrg cache size                  | 1000    |
| CACHE_DEBTPOSITION_TYPE_ORG_MINUTES | DebtPositionTypeOrg cache retention (minutes)   | 60      |

##### 🔗 REST
| ENV                                               | DESCRIPTION                               | DEFAULT |
|---------------------------------------------------|-------------------------------------------|---------|
| DEFAULT_REST_CONNECTION_POOL_SIZE                 | Default connection pool size              | 10      |
| DEFAULT_REST_CONNECTION_POOL_SIZE_PER_ROUTE       | Default connection pool size per route    | 5       |
| DEFAULT_REST_CONNECTION_POOL_TIME_TO_LIVE_MINUTES | Default connection pool TTL (minutes)     | 10      |
| DEFAULT_REST_TIMEOUT_CONNECT_MILLIS               | Default connection timeout (milliseconds) | 120000  |
| DEFAULT_REST_TIMEOUT_READ_MILLIS                  | Default read timeout (milliseconds)       | 120000  |

##### 🧩 Microservices
| ENV                                        | DESCRIPTION                                            | DEFAULT |
|--------------------------------------------|--------------------------------------------------------|---------|
| AUTH_BASE_URL                              | Auth microservice URL                                  |         |
| AUTH_MAX_ATTEMPTS                          | Auth API max attempts                                  | 3       |
| AUTH_WAIT_TIME_MILLIS                      | Auth retry waiting time (milliseconds)                 | 500     |
| AUTH_PRINT_BODY_WHEN_ERROR                 | To print body when an error occurs                     | true    |
| DEBT_POSITION_BASE_URL                     | DebtPosition microservice URL                          |         |
| DEBT_POSITION_MAX_ATTEMPTS                 | DebtPosition API max attempts                          | 3       |
| DEBT_POSITION_WAIT_TIME_MILLIS             | DebtPosition retry waiting time (milliseconds)         | 500     |
| DEBT_POSITION_PRINT_BODY_WHEN_ERROR        | To print body when an error occurs                     | true    |
| ORGANIZATION_BASE_URL                      | Organization microservice URL                          |         |
| ORGANIZATION_MAX_ATTEMPTS                  | Organization API max attempts                          | 3       |
| ORGANIZATION_WAIT_TIME_MILLIS              | Organization retry waiting time (milliseconds)         | 500     |
| ORGANIZATION_PRINT_BODY_WHEN_ERROR         | To print body when an error occurs                     | true    |
| SEND_NOTIFICATION_BASE_URL                 | SendNotification microservice URL                      |         |
| SEND_NOTIFICATION_MAX_ATTEMPTS             | SendNotification API max attempts                      | 3       |
| SEND_NOTIFICATION_WAIT_TIME_MILLIS         | SendNotification retry waiting time (milliseconds)     | 500     |
| SEND_NOTIFICATION_PRINT_BODY_WHEN_ERROR    | To print body when an error occurs                     | true    |
| PROCESS_EXECUTIONS_BASE_URL                | ProcessExecutions microservice URL                     |         |
| PROCESS_EXECUTIONS_MAX_ATTEMPTS            | ProcessExecutions API max attempts                     | 3       |
| PROCESS_EXECUTIONS_WAIT_TIME_MILLIS        | ProcessExecutions retry waiting time (milliseconds)    | 500     |
| PROCESS_EXECUTIONS_PRINT_BODY_WHEN_ERROR   | To print body when an error occurs                     | true    |
| CLASSIFICATION_BASE_URL                    | Classification microservice URL                        |         |
| CLASSIFICATION_MAX_ATTEMPTS                | Classification API max attempts                        | 3       |
| CLASSIFICATION_WAIT_TIME_MILLIS            | Classification retry waiting time (milliseconds)       | 500     |
| CLASSIFICATION_PRINT_BODY_WHEN_ERROR       | To print body when an error occurs                     | true    |
| PAGOPA_PAYMENTS_BASE_URL                   | PagoPaPayments microservice URL                        |         |
| PAGOPA_PAYMENTS_MAX_ATTEMPTS               | PagoPaPayments API max attempts                        | 3       |
| PAGOPA_PAYMENTS_WAIT_TIME_MILLIS           | PagoPaPayments retry waiting time (milliseconds)       | 500     |
| PAGOPA_PAYMENTS_PRINT_BODY_WHEN_ERROR      | To print body when an error occurs                     | true    |
| WORKFLOW_HUB_BASE_URL                      | Workflow-Hub microservice URL                          |         |
| WORKFLOW_HUB_MAX_ATTEMPTS                  | Workflow-Hub API max attempts                          | 3       |
| WORKFLOW_HUB_WAIT_TIME_MILLIS              | Workflow-Hub retry waiting time (milliseconds)         | 500     |
| WORKFLOW_HUB_PRINT_BODY_WHEN_ERROR         | To print body when an error occurs                     | true    |

##### 🌍 External services
| ENV                                        | DESCRIPTION                                            | DEFAULT |
|--------------------------------------------|--------------------------------------------------------|---------|
| PAGOPA_NODE_CHECKOUT_BASE_URL              | PagoPA checkout service URL                            |         |
| PAGOPA_NODE_CHECKOUT_MAX_ATTEMPTS          | PagoPA checkout API max attempts                       | 3       |
| PAGOPA_NODE_CHECKOUT_WAIT_TIME_MILLIS      | PagoPA checkout retry waiting time (milliseconds)      | 500     |
| PAGOPA_NODE_CHECKOUT_PRINT_BODY_WHEN_ERROR | To print body when an error occurs                     | true    |
| NOTIFICATION_PRICE_MAX_ATTEMPTS            | Actualization API max attempts                         | 3       |
| NOTIFICATION_PRICE_WAIT_TIME_MILLIS        | Actualization retry waiting time (milliseconds)        | 500     |
| NOTIFICATION_PRICE_PRINT_BODY_WHEN_ERROR   | To print body when an error occurs                     | true    |
| PAYMENT_NOTIFICATION_MAX_ATTEMPTS          | Payment notification API max attempts                  | 3       |
| PAYMENT_NOTIFICATION_WAIT_TIME_MILLIS      | Payment notification retry waiting time (milliseconds) | 500     |
| PAYMENT_NOTIFICATION_PRINT_BODY_WHEN_ERROR | To print body when an error occurs                     | true    |

##### 🌀 KAFKA
| ENV                                              | DESCRIPTION                                                        | DEFAULT   |
|--------------------------------------------------|--------------------------------------------------------------------|-----------|
| KAFKA_BINDER_BROKER                              | Comma separated list of brokers to which the Kafka binder connects |           |
| KAFKA_CONFIG_HEARTBEAT_INTERVAL_MS               | Hearth beat interval (milliseconds)                                | 3000      |
| KAFKA_CONFIG_SESSION_TIMEOUT_MS                  | Session timeout (milliseconds)                                     | 30000     |
| KAFKA_CONFIG_REQUEST_TIMEOUT_MS                  | Request timeout (milliseconds)                                     | 60000     |
| KAFKA_CONFIG_METADATA_MAX_AGE                    | Metadata max age (milliseconds)                                    | 180000    |
| KAFKA_CONFIG_SASL_MECHANISM                      | SASL mechanism                                                     | PLAIN     |
| KAFKA_CONFIG_SECURITY_PROTOCOL                   | Security protocol                                                  | SASL_SSL  |
| KAFKA_CONFIG_MAX_REQUEST_SIZE                    | Max request size                                                   | 1000000   |

###### 📤 KAFKA PRODUCERS
| ENV                                                | DESCRIPTION                                       | DEFAULT                    |
|----------------------------------------------------|---------------------------------------------------|----------------------------|
| KAFKA_TOPIC_REGISTRIES                             | Topic where to publish registry events            | p4pa-payhub-registries-evh |
| KAFKA_REGISTRIES_PRODUCER_SASL_JAAS_CONFIG         | JAAS Config string used to perform authentication |                            |
| KAFKA_REGISTRIES_PRODUCER_CONNECTION_MAX_IDLE_TIME | Max producer idle time (milliseconds)             | 180000                     |
| KAFKA_REGISTRIES_PRODUCER_RETRY_MS                 | Producer retry waiting time (milliseconds)        | 10000                      |
| KAFKA_REGISTRIES_PRODUCER_LINGER_MS                | Producer linger time (milliseconds)               | 2                          |
| KAFKA_REGISTRIES_PRODUCER_BATCH_SIZE               | Producer batch size                               | 16384                      |

#### 💼 Business logic
| ENV                                       | DESCRIPTION                                                                                                                                                                                                    | DEFAULT   |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| PUBLIC_BASE_URL_PU_SIL                    | Public base URL towards [p4pa-pu-sil](https://github.com/pagopa/p4pa-pu-sil) service                                                                                                                           |           |
| PUBLIC_BASE_URL_BFF                       | Public base URL towards [p4pa-bff](https://github.com/pagopa/p4pa-pu-bff) service                                                                                                                              |           |
| PUBLIC_BASE_URL_FILESHARE                 | Public base URL towards [p4pa-fileshare](https://github.com/pagopa/p4pa-fileshare) service                                                                                                                     |           |
| CHECKOUT_DEFAULT_CALLBACK_URL_OK          | Absolute url to default callback url for OK case on [checkout service](https://developer.pagopa.it/pago-pa/guides/sanp/ente-creditore/modalita-dintegrazione/integrazione-touch-point-dellec-con-checkout)     |           |
| CHECKOUT_DEFAULT_CALLBACK_URL_KO          | Absolute url to default callback url for ERROR case on [checkout service](https://developer.pagopa.it/pago-pa/guides/sanp/ente-creditore/modalita-dintegrazione/integrazione-touch-point-dellec-con-checkout)  |           |
| CHECKOUT_DEFAULT_CALLBACK_URL_CANCEL      | Absolute url to default callback url for CANCEL case on [checkout service](https://developer.pagopa.it/pago-pa/guides/sanp/ente-creditore/modalita-dintegrazione/integrazione-touch-point-dellec-con-checkout) |           |
| DEFAULT_AUTH_EXPIRATION_TIMEOUT_SECONDS   | default expiration for legay auth in seconds                                                                                                                                                                   | 300       |
| REST_AGID_INTEGRITY_PU_ISSUER             | AGID Data Integrity header - value for `issuer`                                                                                                                                                                | piattaforma-unitaria |
| REST_AGID_INTEGRITY_PU_EXPIRATION_MINUTES | AGID Data Integrity header - expiration of signature                                                                                                                                                           | 10        |
| REST_AGID_INTEGRITY_PU_PRIVATEKEY         | AGID Data Integrity header - private key                                                                                                                                                                       |           |
| REST_AGID_INTEGRITY_PU_PUBLICKEY          | AGID Data Integrity header - public key                                                                                                                                                                        |           |
 


## 🛠️ Getting Started

### 📝 Prerequisites

Ensure the following tools are installed on your machine:

1. **Java 21+**
2. **Gradle** (or use the Gradle wrapper included in the repository)
3. **Docker** (to build and run on an isolated environment, optional)

### 🔐 Write Locks

```sh
./gradlew dependencies --write-locks
```

### ⚙️ Build

```sh
./gradlew clean build
```

### 🧪 Test

#### 📌 JUnit
```sh
./gradlew test
```

### 🚀 Run local

```sh
./gradlew bootRun
```

### 🐳 Build & run through Docker
```sh
docker build -t <APP_NAME> .
docker run --env-file <ENV_FILE> <APP_NAME>
```

### ⚖️ Generate dependencies licenses
```sh
./gradlew generateLicenseReport
```
