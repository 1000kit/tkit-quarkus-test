# tkit-quarkus-test

tkit quarkus test extension

[![License](https://img.shields.io/badge/license-Apache--2.0-green?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.tkit.quarkus/tkit-quarkus-test?logo=java&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/org.tkit.quarkus/tkit-quarkus-test)

## DB Import

If you are testing backend service you will probably want to have some test data ready. 
You can quickly enable import of test data from Excel by adding dbimport feature to your test containers and annotating 
your test class or test method with@WithDBData. The annotation specifies a path to XLS file that should be imported and 
optionally whether you want to delete existing data before import. See javadoc for more info. If you have @WithDBData 
on class, its data will be imported before first test execution. The data needs to be valid Excel file with DBunit 
structure(table per sheet). How it works: If you enable dbimport feature, the DeploymentBuilder will inject DBunit and 
it's dependencies into your war file along with a simple rest service that handle the file upload and import(rs path dbimport). 
In a @Before hook of your test method, tkit checks if a @WithDBData annotation is present and will try to upload the 
given file to the dbimport rest service.


Add the dbimport docker image to the test containers 
```java
DB_IMPORT = new FixedHostPortGenericContainer("quay.io/tkit/dbimport:master")
           .withFixedExposedPort(8811, 8080)
           .withNetwork(NETWORK)
           .withNetworkAliases("dbimport")
           .withEnv("DB_PASSWORD", "parameters")
           .withEnv("DB_USERNAME", "parameters")
           .withEnv("DB_URL", "jdbc:postgresql://parameters-db:5432/parameters?sslmode=disable")
           .waitingFor(Wait.forLogMessage(".*Installed features:.*", 1))
           .withLogConsumer(TestContainerLogger.create("dbimport"));
```

Put the annotation to the test method
```java
@Test
@WithDBData({"parameters-testdata.xls"})
public void testImportData() {
    given()
            .when()
            .contentType(MediaType.APPLICATION_JSON)
            .queryParam("applicationId", "app1")
            .queryParam("parameterKey", "param")
            .get("/v2/parameters")
            .prettyPeek()
            .then()
            .statusCode(Response.Status.OK.getStatusCode());
}
```

## Test containers logger
```java
APP_DOCKER_IMAGE = new FixedHostPortGenericContainer(image)
        .withFixedExposedPort(8080, 8080)
        .withNetwork(NETWORK)
        .withNetworkAliases("app")
        .waitingFor(Wait.forLogMessage(".*Installed features:.*", 1))
        .withLogConsumer(TestContainerLogger.create("app"));
```
### Create a release

```bash
mvn semver-release:release-create
```

### Create a patch branch
```bash
mvn semver-release:patch-create -DpatchVersion=x.x.0
```
