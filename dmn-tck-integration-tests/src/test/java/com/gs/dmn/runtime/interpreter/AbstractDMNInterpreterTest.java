/*
 * Copyright 2016 Goldman Sachs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.gs.dmn.runtime.interpreter;

import com.gs.dmn.DMNModelRepository;
import com.gs.dmn.dialect.DMNDialectDefinition;
import com.gs.dmn.feel.lib.FEELLib;
import com.gs.dmn.feel.lib.StandardFEELLib;
import com.gs.dmn.log.BuildLogger;
import com.gs.dmn.log.Slf4jBuildLogger;
import com.gs.dmn.runtime.Assert;
import com.gs.dmn.runtime.DMNRuntimeException;
import com.gs.dmn.runtime.Pair;
import com.gs.dmn.serialization.DMNConstants;
import com.gs.dmn.serialization.DMNReader;
import com.gs.dmn.serialization.PrefixNamespaceMappings;
import com.gs.dmn.tck.TCKUtil;
import com.gs.dmn.tck.TestCasesReader;
import com.gs.dmn.transformation.DMNTransformer;
import com.gs.dmn.transformation.ToSimpleNameTransformer;
import com.gs.dmn.transformation.basic.BasicDMN2JavaTransformer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.omg.dmn.tck.marshaller._20160719.TestCases;
import org.omg.dmn.tck.marshaller._20160719.TestCases.TestCase;
import org.omg.dmn.tck.marshaller._20160719.TestCases.TestCase.ResultNode;
import org.omg.spec.dmn._20180521.model.TDefinitions;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gs.dmn.tck.TestCasesReader.DEFAULT_TEST_CASE_FILE_EXTENSION;
import static com.gs.dmn.tck.TestCasesReader.isTCKFile;
import static org.junit.Assert.assertTrue;

public abstract class AbstractDMNInterpreterTest {
    private static final BuildLogger LOGGER = new Slf4jBuildLogger(LoggerFactory.getLogger(AbstractDMNInterpreterTest.class));
    private static final boolean IGNORE_ERROR_RESULT = true;

    private final DMNReader reader = new DMNReader(LOGGER, false);
    private final TestCasesReader testCasesReader = new TestCasesReader(LOGGER);

    protected DMNInterpreter interpreter;
    private DMNTransformer dmnTransformer;
    private BasicDMN2JavaTransformer basicTransformer;
    private FEELLib lib;

    protected void doSingleModelTest(String dmnFileName, String... testSuffixes) {
        this.doMultipleModelsTest(Arrays.asList(dmnFileName), testSuffixes);
    }

    protected void doMultipleModelsTest(List<String> dmnFileNames, String... testSuffixes) {
        String errorMessage = String.format("Tested failed for diagram '%s'", dmnFileNames);
        try {
            // Read DMN files
            List<Pair<TDefinitions, PrefixNamespaceMappings>> pairs = readModels(dmnFileNames);
            DMNModelRepository repository = new DMNModelRepository(pairs);

            // Transform definitions
            dmnTransformer = new ToSimpleNameTransformer(LOGGER);
            repository = dmnTransformer.transform(repository);

            // Set-up execution
            this.interpreter = getDialectDefinition().createDMNInterpreter(repository);
            this.basicTransformer = interpreter.getBasicDMNTransformer();
            this.lib = interpreter.getFeelLib();

            // Check test files
            for (String dmnFileName: dmnFileNames) {
                if (testSuffixes == null || testSuffixes.length == 0) {
                    URL inputPathURL = getClass().getClassLoader().getResource(getTestCasesInputPath()).toURI().toURL();
                    File inputPathFolder = new File(inputPathURL.getFile());
                    List<String> testFileNames = new ArrayList<>();
                    List<TestCases> testCasesList = new ArrayList<>();
                    for (File child : inputPathFolder.listFiles()) {
                        if (isTCKFile(child) && child.getName().startsWith(dmnFileName)) {
                            TestCases testCases = testCasesReader.read(child);
                            testFileNames.add(child.getName());
                            testCasesList.add(testCases);
                        }
                    }
                    doTest(testFileNames, testCasesList, interpreter, repository);
                } else {
                    List<String> testFileNames = new ArrayList<>();
                    List<TestCases> testCasesList = new ArrayList<>();
                    for (String testSuffix : testSuffixes) {
                        String testPathName = getTestCasesInputPath() + "/" + dmnFileName + testSuffix + DEFAULT_TEST_CASE_FILE_EXTENSION;
                        URL testURL = getClass().getClassLoader().getResource(testPathName).toURI().toURL();
                        TestCases testCases = testCasesReader.read(testURL);
                        testCasesList.add(testCases);
                        testFileNames.add(new File(testURL.getFile()).getName());
                    }
                    doTest(testFileNames, testCasesList, interpreter, repository);
                }
            }
        } catch (Exception e) {
            throw new DMNRuntimeException(errorMessage, e);
        }
    }

    protected void doTest(List<String> testCaseFileNameList, List<TestCases> testCasesList, DMNInterpreter interpreter, DMNModelRepository repository) {
        // Check all TestCases
        Pair<DMNModelRepository, List<TestCases>> result = dmnTransformer.transform(repository, testCasesList);
        List<TestCases> actualTestCasesList = result.getRight();
        for (int i = 0; i < actualTestCasesList.size(); i++) {
            String testCaseFileName = testCaseFileNameList.get(i);
            TestCases testCases = actualTestCasesList.get(i);
            for (TestCase testCase : testCases.getTestCase()) {
                doTest(testCaseFileName, testCase, interpreter);
            }
        }
    }

    private void doTest(String testCaseFileName, TestCase testCase, DMNInterpreter interpreter) {
        TCKUtil tckUtil = new TCKUtil(basicTransformer, (StandardFEELLib) lib);

        List<ResultNode> resultNode = testCase.getResultNode();
        for (ResultNode res : resultNode) {
            Object expectedValue = null;
            Result actualResult;
            Object actualValue = null;
            String message = String.format("Unexpected result in test case in file '%s' test case '%s' for result node '%s'", testCaseFileName, testCase.getId(), res.getName());
            try {
                expectedValue = tckUtil.expectedValue(testCase, res);
                actualResult = tckUtil.evaluate(interpreter, testCase, res);
                actualValue = Result.value(actualResult);
            } catch (Exception e) {
                LOGGER.error(ExceptionUtils.getStackTrace(e));
                if (!IGNORE_ERROR_RESULT && !res.isErrorResult()) {
                    e.printStackTrace();
                    DMNRuntimeException dmnRuntimeException = new DMNRuntimeException(message, e);
                    assertTrue(dmnRuntimeException.getMessage() + ". " + e.getMessage() + String.format(".  Expected '%s' actual '%s'", expectedValue, actualValue), res.isErrorResult());
                }
            }
            Assert.assertEquals(message, expectedValue, actualValue);
        }
    }

    protected abstract DMNDialectDefinition getDialectDefinition();

    protected abstract String getDMNInputPath();

    protected abstract String getTestCasesInputPath();

    private List<Pair<TDefinitions, PrefixNamespaceMappings>> readModels(List<String> dmnFileNames) throws Exception {
        List<Pair<TDefinitions, PrefixNamespaceMappings>> pairs = new ArrayList<>();
        for (String dmnFileName: dmnFileNames) {
            String dmnPathName = getDMNInputPath() + "/" + dmnFileName + DMNConstants.DMN_FILE_EXTENSION;
            URL dmnFileURL = getClass().getClassLoader().getResource(dmnPathName).toURI().toURL();
            Pair<TDefinitions, PrefixNamespaceMappings> pair = reader.read(dmnFileURL);
            pairs.add(pair);
        }
        return pairs;
    }
}
