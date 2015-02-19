/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.bps.integration.tests.bpmn;

import junit.framework.Assert;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.ActivitiRule;
import junit.framework.Assert;
import org.activiti.engine.test.Deployment;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.activiti.*;
import org.wso2.bps.integration.common.clients.bpmn.ActivitiRestClient;
import org.wso2.bps.integration.common.utils.BPSMasterTest;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class JavaServiceTaskDeploymentUndeploymentTestCase extends BPSMasterTest {

    @BeforeClass
    public void EnvSetup() throws Exception {
        init();
        final String artifactLocation = FrameworkPathUtil.getSystemResourceLocation() + File.separator
                                        + BPMNTestConstants.DIR_ARTIFACTS + File.separator
                                        + BPMNTestConstants.DIR_BPMN + File.separator
                                        + "testArtifactid-1.0.jar";

        ServerConfigurationManager Loader = new ServerConfigurationManager(bpsServer);
        File javaArtifact = new File(artifactLocation);
        Loader.copyToComponentLib(javaArtifact);
        Loader.restartForcefully();

        //reintialising as session cookies and other configuration which expired during restart is reset
        init();
    }


    @Test(groups = {"wso2.bps.test.deploy.JavaServiceTask"}, description = "Deploy/UnDeploy Package Test", priority = 1, singleThreaded = true)

    public void deployUnDeployJavaServiceTaskBPMNPackage() throws Exception {
        init();
        ActivitiRestClient tester = new ActivitiRestClient(bpsServer.getInstance().getPorts().get("http")
                , bpsServer.getInstance().getHosts().get("default"));

        String filePath = FrameworkPathUtil.getSystemResourceLocation() + File.separator
                          + BPMNTestConstants.DIR_ARTIFACTS + File.separator
                          + BPMNTestConstants.DIR_BPMN + File.separator + "sampleJavaServiceTask.bar";

        String fileName = "sampleJavaServiceTask.bar";
        String[] deploymentResponse;

        deploymentResponse = tester.deployBPMNPackage(filePath, fileName);
        Assert.assertTrue("Deployment Successful", deploymentResponse[0].contains(BPMNTestConstants.CREATED));
        String[] deploymentCheckResponse = tester.getDeploymentInfoById(deploymentResponse[1]);
        Assert.assertTrue("Deployment Present", deploymentCheckResponse[2].contains(fileName));


        String[] definitionResponse = tester.findProcessDefinitionInfoById(deploymentResponse[1]);
        Assert.assertTrue("Search Success", definitionResponse[0].contains("200"));

        //Starting and Verifying Process Instance
        String[] processInstanceResponse = tester.startProcessInstanceByDefintionID(definitionResponse[1]);
        Assert.assertTrue("Process Instance Started", processInstanceResponse[0].contains(BPMNTestConstants.CREATED));
        String searchResponse = tester.searchProcessInstanceByDefintionID(definitionResponse[1]);
        Assert.assertTrue("Process Instance Present", searchResponse.contains(BPMNTestConstants.OK));

        //check for varible value if true
        String[] innovationResponse = tester.getValueOfVariableOfProcessInstanceById(processInstanceResponse[1], "executionState");
        Assert.assertTrue("Variable Present", innovationResponse[0].contains(BPMNTestConstants.OK));
        Assert.assertTrue("Variable Present", innovationResponse[1].contains("executionState"));
        Assert.assertTrue("Variable Value is True", innovationResponse[2].contains("true"));


        //Suspending the Process Instance
        String[] suspendResponse = tester.suspendProcessInstanceById(processInstanceResponse[1]);
        Assert.assertTrue("Process Instance has been suspended", suspendResponse[0].contains(BPMNTestConstants.OK));
        Assert.assertTrue("Process Instance has been suspended", suspendResponse[1].contains("true"));

        //Deleting a Process Instance
        String deleteStatus = tester.deleteProcessInstanceByID(processInstanceResponse[1]);
        Assert.assertTrue("Process Instance Removed", deleteStatus.contains(BPMNTestConstants.NO_CONTENT));

        String status = tester.unDeployBPMNPackage(deploymentResponse[1]);
        Assert.assertTrue("Package UnDeployed", status.contains(BPMNTestConstants.NO_CONTENT));
    }
}