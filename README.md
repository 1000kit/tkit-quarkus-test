# tkit-quarkus-test

Quarkus versions:
* Tested with Quarkus 1.3.0.Final.
* For Quarkus 1.2.1.Final or older use [0.x version](https://gitlab.com/1000kit/quarkus/tkit-quarkus-test/-/tree/0.x)

tkit quarkus test extension

[![License](https://img.shields.io/badge/license-Apache--2.0-green?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.tkit.quarkus/tkit-quarkus-test?logo=java&style=for-the-badge)](https://maven-badges.herokuapp.com/maven-central/org.tkit.quarkus/tkit-quarkus-test)

Add this maven test dependency to the project.
```xml
<dependency>
    <groupId>org.lorislab.quarkus</groupId>
    <artifactId>quarkus-testcontainers</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

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


Add the dbimport docker image to the test containers docker-compose.yml file
```yaml
  tkit-parameter-db-import:
    container_name: tkit-parameter-db-import
    image: quay.io/tkit/dbimport:master
    environment:
      DB_URL: "jdbc:postgresql://tkit-parameter-db:5432/parameters?sslmode=disable"
      DB_USERNAME: "parameters"
      DB_PASSWORD: "parameters"
    ports:
      - "8811:8080"
    labels:
      - "test.Wait.forLogMessage.regex=.*Installed features:.*"
      - "test.Wait.forLogMessage.times=1"
      - "test.log=true"
      - "test.property.tkit.test.dbimport.url=$${url:tkit-parameter-db-import:8080}"
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

## Pipeline and tests

1. Build project, run the unit test and build native image: 
    * mvn clean package -Pnative -Dquarkus.native.container-build=true (1) 
2. Build the docker image
    * docker build
3. Run the integration test
    * mvn failsafe:integration-test failsafe:verify
4. Push the docker image 
    * docker push
    
(1) build native image on Linux: 
    * mvn clean package -Pnative        

## How to write the tests

Create abstract test class which will set up the docker test environment. The default location of the docker compose file
is `src/test/resources/docker-compose.yaml`

```java
@QuarkusTestResource(DockerComposeTestResource.class)
public abstract class AbstractTest {

    @DockerService("quarkus-test")
    protected DockerComposeService app;

    @BeforeEach
    public void init() {
        if (app != null) {
            RestAssured.port = app.getPort(8080);
        }
    }
}
```
Create a common test for unit and integration test
```java
public class ServiceRestControllerT extends AbstractTest {

    @Test
    public void serviceTest() {
        // ...        
    }
}
```
Unit test
```java
@QuarkusTest
public class ServiceRestControllerTest extends ServiceRestControllerT {

}
```
Integration test
```java
@DockerComposeTest
public class DeploymentRestControllerTestIT extends DeploymentRestControllerT {

}
```

## Maven settings
Unit test maven plugin
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>${surefire-plugin.version}</version>
    <configuration>
        <systemProperties>
            <com.arjuna.ats.arjuna.objectstore.objectStoreDir>${project.build.directory}/jta</com.arjuna.ats.arjuna.objectstore.objectStoreDir>
            <ObjectStoreEnvironmentBean.objectStoreDir>${project.build.directory}/jta</ObjectStoreEnvironmentBean.objectStoreDir>
            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
        </systemProperties>
    </configuration>
</plugin>
```
Integration test maven plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>${surefire-plugin.version}</version>
    <executions>
        <execution>
            <id>native</id>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
            <phase>integration-test</phase>
        </execution>
    </executions>
    <configuration>
        <systemPropertyVariables>
            <test.integration>true</test.integration>
        </systemPropertyVariables>
    </configuration>                
</plugin>
```
The system property `<test.integration>true</test.integration>` activate the integration test.

## Docker labels

## Docker labels

| label   | values | default | description |
|---|---|---|---|
| test.integration=true | `boolean` | `true` | enable the docker for the integration test |
| test.unit=true | `boolean` | `true` | enable the docker for the unit test |
| test.image.pull=DEFAULT | `string` | `DEFAULT,ALWAYS,MAX_AGE` | pull docker image before test |
| test.image.pull.max_age | `string` | `PT10` | only for the `MAX_AGE` pull docker image before test if older than duration. Default: 10s |
| test.Wait.forLogMessage.regex= | `string` | `null` | regex of the WaitStrategy for log messages |
| test.Wait.forLogMessage.times=1 | `int` | `1` | the number of times the pattern is expected in the WaitStrategy |
| test.Log=true | `boolean` | `true` | enabled log of the docker container |
| test.priority=100 | `int` | `100` | start priority |
| test.property.<name>=<value> | `string` | `null` | set the system property with <name> and <value> in the tests |
| test.env.<name>=<value> | `string` | `null` | set the environment variable with <name> and <value> in the docker container |

The value of the test.property.* or test.env.* supported this syntax:
* simple value: `123` result: 123
* host of the service: `$${host:<service>}` the host of the service `<service>`
* port of the service: `$${port:<service>:<port>}` the port number of the `<port>` of the `<service>` service
* url of the service: `$${url:<service>:<port>}` the url of the service `http://<service>:<port>`
* system property: `$${prop:<name>`}
* environment variable: `${env:<name>`}
 
 Example:
 ```bash
test.property.quarkus.datasource.url=jdbc:postgresql://$${host:postgres}:$${port:postgres:5432}/p6?sslmode=disable
```
The system property `quarkus.datasource.url` will be set to 
`jdbc:postgresql://localhost:125432/p6?sslmode=disable` if the docker image host of the 
postgres is `localhost` and tet containers dynamic port ot the container port `5432` is set to
`125432` value.

## Docker compose example

```yaml
version: "2"
services:
  postgres:
    container_name: postgres
    image: postgres:10.5
    environment:
      POSTGRES_DB: "p6"
      POSTGRES_USER: "p6"
      POSTGRES_PASSWORD: "p6"
    labels:
      - "test.Wait.forLogMessage.regex=.*database system is ready to accept connections.*\\s"
      - "test.Wait.forLogMessage.times=2"
      - "test.log=true"
      - "test.property.quarkus.datasource.url=jdbc:postgresql://$${host:postgres}:$${port:postgres:5432}/p6?sslmode=disable"
    ports:
      - "5433:5433"
    networks:
      - test
  tkit-parameter:
    container_name: tkit-parameter
    image: quay.io/tkit/tkit-parameter:latest
    ports:
      - "8080:8080"
    labels:
      - "test.unit=false"
      - "test.priority=101"
      - "test.image.pull=false"
      - "test.env.QUARKUS_DATASOURCE_URL=jdbc:postgresql://postgres:5432/p6?sslmode=disable"
    networks:
      - test
networks:
  test:
```

### Create a release

```bash
mvn semver-release:release-create
```

### Create a patch branch
```bash
mvn semver-release:patch-create -DpatchVersion=x.x.0
```
