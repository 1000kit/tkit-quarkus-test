package org.tkit.quarkus.test;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * This extension is deprecated. The annotation @WithDBData itself will activate the extension.
 * Remove the {@code @ExtendWith(DBImportExtension.class)} from your test class.
 *
 * @deprecated since 1.2.0, will be remove in 1.3.0.
 * @see WithDBData
 */
@Deprecated
public class DBImportExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, BeforeAllCallback, AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext context) throws Exception {

    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {

    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {

    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {

    }
}
