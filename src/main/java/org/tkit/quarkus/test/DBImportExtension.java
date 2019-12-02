/*
 * Copyright 2019 1000kit.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tkit.quarkus.test;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * This junit5 extension is using the db-import service to import data in the database.
 * The data to for the import needs to be in the class-path.
 * Only the excel and csv data formats are supported.
 * The db-import service configuration property: tkit.test.dbimport.url, default value: http://docker:8811/
 *
 * @see WithDBData
 */
public class DBImportExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, BeforeAllCallback, AfterAllCallback {

    /**
     * The logger for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(DBImportExtension.class);

    /**
     * {@inheritDoc }
     */
    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        Method method = context.getRequiredTestMethod();
        WithDBData an = method.getAnnotation(WithDBData.class);
        if (an != null) {
            log.info("[DB-IMPORT] After method level data for {} data-source {}", method.getName(), an.value());
            deleteAllData(an);
        } else {
            WithDBData can = context.getRequiredTestClass().getAnnotation(WithDBData.class);
            if (can != null && can.rinseAndRepeat()) {
                log.info("[DB-IMPORT] After class level data(Rinse and Repeat) for {} data-source {}", method.getName(), can.value());
                deleteAllData(can);
            } else {
                log.debug("[DB-IMPORT] No WithDBData annotation found on class level {}", context.getRequiredTestClass().getName());
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        Method method = context.getRequiredTestMethod();
        WithDBData an = method.getAnnotation(WithDBData.class);
        if (an != null) {
            log.info("[DB-IMPORT] Init method level data for {} data-source {}", method.getName(), an.value());
            importAllData(an);
        } else {
            WithDBData can = context.getRequiredTestClass().getAnnotation(WithDBData.class);
            if (can != null && can.rinseAndRepeat()) {
                log.info("[DB-IMPORT] Init class level data(Rinse and Repeat) for {} data-source {}", method.getName(), can.value());
                importAllData(can);
            } else {
                log.debug("[DB-IMPORT] No WithDBData annotation found on class level {}", context.getRequiredTestClass().getName());
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Class<?> clazz = context.getRequiredTestClass();
        WithDBData an = clazz.getAnnotation(WithDBData.class);
        if (an != null) {
            log.info("[DB-IMPORT] After class level data for {} data-source {}", clazz.getName(), an.value());
            deleteAllData(an);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> clazz = context.getRequiredTestClass();
        WithDBData an = clazz.getAnnotation(WithDBData.class);
        if (an != null) {
            log.info("[DB-IMPORT] Init class level data for {} data-source {}", clazz.getName(), an.value());
            importAllData(an);
        }
    }

    /**
     * Deletes all data defined in the annotation.
     *
     * @param an the with db data annotation.
     */
    private void deleteAllData(WithDBData an) throws Exception {
        for (int i = 0; i < an.value().length; i++) {
            if (an.deleteAfterTest()) {
                String path = an.value()[i];
                URL fileUrl = this.getClass().getClassLoader().getResource(path);
                if (fileUrl != null) {
                    log.info("Truncate data via DBImport file {}", fileUrl);
                    if (isExcel(path)) {
                        deleteExcelData(fileUrl);
                    } else if (isCsv(path)) {
                        deleteCsvData(fileUrl);
                    }
                    log.info("[DB-IMPORT] Truncate data successfully {}", fileUrl);
                } else {
                    log.warn("[DB-IMPORT] Missing database import resource {} in the class-path.", path);
                }
            } else {
                log.info("[DB-IMPORT] no data deleted after test due to annotation value");
            }
        }
    }

    /**
     * Delete data from the database base on the csv file/directory.
     *
     * @param fileUrl the file URL.
     */
    public static void deleteCsvData(URL fileUrl) throws Exception {
        RestAssured.given().spec(requestSpecification())
                .contentType("application/x-www-form-urlencoded")
                .formParam("csv_path", fileUrl.getPath())
                .log().ifValidationFails()
                .when()
                .post("db/teardown/csv")
                .then().statusCode(200);
    }


    /**
     * Delete data from the database base on the excel table.
     *
     * @param fileUrl the file URL.
     */
    public static void deleteExcelData(URL fileUrl) {
        RestAssured.given().spec(requestSpecification())
                .contentType("application/excel")
                .body(createFile(fileUrl))
                .log().ifValidationFails()
                .when()
                .post("db/teardown/excel")
                .then().statusCode(200);
    }

    /**
     * Imports all data defined in the annotation.
     *
     * @param an the with db data annotation.
     */
    private void importAllData(WithDBData an) {
        for (int i = 0; i < an.value().length; i++) {
            String path = an.value()[i];
            URL fileUrl = this.getClass().getClassLoader().getResource(path);
            if (fileUrl != null) {
                log.info("[DB-IMPORT] Importing data via DBImport file {}", fileUrl);
                if (isExcel(path)) {
                    importExcelData(fileUrl, an.deleteBeforeInsert());
                    log.info("[DB-IMPORT] Imported Excel {} datasource {}", this.getClass().getSimpleName(), path);
                } else if (isCsv(path)) {
                    importCsvData(fileUrl, an.deleteBeforeInsert());
                    log.info("[DB-IMPORT] Imported CSV {} datasource {}", this.getClass().getSimpleName(), fileUrl);
                }
            } else {
                log.warn("[DB-IMPORT] Missing database import resource {} in the class-path.", path);
            }
        }
    }

    /**
     * The db-import request configuration.
     * Configuration property: tkit.test.dbimport.url, default value: http://docker:8811/
     *
     * @return the request specification.
     */
    public static RequestSpecification requestSpecification() {
        Config config = ConfigProvider.getConfig();
        String url = config.getOptionalValue("tkit.test.dbimport.url", String.class).orElse("http://docker:8811/");
        return new RequestSpecBuilder().setBaseUri(url).build();
    }

    /**
     * Returns {@code true} if the path is csv file/directory.
     *
     * @param path the file path.
     * @return {@code true} if the path is csv file/directory.
     */
    public static boolean isCsv(String path) {
        return path != null && (path.endsWith("csv") || path.endsWith("csv/"));
    }

    /**
     * Returns {@code true} if the path is excel file.
     *
     * @param path the file path.
     * @return {@code true} if the path is excel file.
     */
    public static boolean isExcel(String path) {
        return path != null && (path.endsWith(".xls") || path.endsWith(".xlsx"));
    }

    /**
     * Imports the Excel data from the URL.
     *
     * @param fileUrl            the file URL.
     * @param deleteBeforeInsert delete before insert flag.
     */
    public static void importExcelData(URL fileUrl, boolean deleteBeforeInsert) {
        RestAssured.given().spec(requestSpecification())
                .contentType("application/excel")
                .body(createFile(fileUrl))
                .log().ifValidationFails()
                .queryParam("cleanBefore", deleteBeforeInsert)
                .when()
                .post("db/import/excel")
                .prettyPeek()
                .then().statusCode(200);
    }

    /**
     * Imports the CSV data from the URL.
     *
     * @param fileUrl            the file URL.
     * @param deleteBeforeInsert delete before insert flag.
     */
    public static void importCsvData(URL fileUrl, boolean deleteBeforeInsert) {
        RestAssured.given().spec(requestSpecification())
                .formParam("csv_path", fileUrl.getPath())
                .log().ifValidationFails()
                .queryParam("cleanBefore", deleteBeforeInsert)
                .when()
                .post("db/import/csv")
                .prettyPeek()
                .then().statusCode(200);
    }


    /**
     * Creates the file object from the URL.
     *
     * @param fileUrl the file URL.
     * @return the corresponding file object.
     */
    private static File createFile(URL fileUrl) {
        try {
            return new File(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Wrong URI format. " + fileUrl, e);
        }
    }

}
