/*
 * Copyright 2019 tkit.org.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that this test execution requires data import before it is started. This requires a test deployment with DBImport enabled - @link DeploymentBuilder#withDBImportFeature()
 * 
 * If you put this annotation on class, then by default the import will be executed before 1st test in this class, and cleanup after last test. If you specify the <code>rinseAndRepeat</code> flag, then it will be executed for every test method.
 *
 * Value should be array of string representing paths to {@code .xls} files with DBUnit data or directories with {@code .csv} files, relative to maven classroot(src/test/resources|src/test/java).
 *
 * Example: <code>@WithDBData("data/test.xls")</code> would try to find a file <code>PROJECT_ROOT/src/test/resources/data/test.xls</code> or
 * <code>PROJECT_ROOT/src/test/java/data/test.xls</code>
 *
 * If you want to import csv files, they must be stored in folder named "/csv". Apart form this remember about creating table-ordering.txt file in "/csv" where you specify an order in which data should be imported
 *
 * @author mmajchra
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface WithDBData {
    
    /**
     * Paths to DBUnit xls files or csv directories
     * @return relative path to the import file
     */
    String[] value();
    
    /**
     * Should the existing data be deleted in the table that is to imported?
     * @return true if data should be deleted, false otherwise
     */
    boolean deleteBeforeInsert() default false;
    
    /**
     * Should the test data in all affected tables be deleted after test?
     * @return true if data should be deleted, false otherwise
     */
    boolean deleteAfterTest() default false;
    
    /**
     * Should the DB data import be executed for every test method in this class? Same as annotating every method with this annotation.
     * @return true if db import should be executed for every test case method again, false otherwise
     */
    boolean rinseAndRepeat() default false;
}
