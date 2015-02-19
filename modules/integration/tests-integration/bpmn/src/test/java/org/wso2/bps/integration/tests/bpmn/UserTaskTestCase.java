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
import org.testng.annotations.Test;
import org.wso2.bps.integration.common.clients.bpmn.ActivitiRestClient;
import org.wso2.bps.integration.common.utils.BPSMasterTest;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;

import java.io.File;


public class UserTaskTestCase extends BPSMasterTest {

    @Test(groups = {"wso2.bps.test.usertasks"}, description = "User Task Test", priority = 1, singleThreaded = true)
    public void UserTaskTestCase() throws Exception {
        init();
        ActivitiRestClient tester = new ActivitiRestClient(bpsServer.getInstance().getPorts().get("http"), bpsServer.getInstance().getHosts().get("default"));
        //deploying Package
        String filePath = FrameworkPathUtil.getSystemResourceLocation() + File.separator
                          + BPMNTestConstants.DIR_ARTIFACTS + File.separator
                          + BPMNTestConstants.DIR_BPMN + File.separator + "AssigneeIsEmpty.bar";

        String fileName = "AssigneeIsEmpty.bar";


        String[] deploymentResponse = tester.deployBPMNPackage(filePath, fileName);
        Assert.assertTrue("Deployment Successful", deploymentResponse[0].contains(BPMNTestConstants.CREATED));
        String[] deploymentCheckResponse = tester.getDeploymentInfoById(deploymentResponse[1]);
        Assert.assertTrue("Deployment Present", deploymentCheckResponse[2].contains(fileName));

        //Acquiring Process Definition ID to start Process Instance
        String[] definitionResponse = tester.findProcessDefinitionInfoById(deploymentResponse[1]);
        Assert.assertTrue("Search Success", definitionResponse[0].contains(BPMNTestConstants.OK));

        //Starting and Verifying Process Instance
        String[] processInstanceResponse = tester.startProcessInstanceByDefintionID(definitionResponse[1]);
        Assert.assertTrue("Process Instance Started", processInstanceResponse[0].contains(BPMNTestConstants.CREATED));
        String searchResponse = tester.searchProcessInstanceByDefintionID(definitionResponse[1]);
        Assert.assertTrue("Process Instance Present", searchResponse.contains(BPMNTestConstants.OK));

        tester.waitForTaskGeneration();

        //Acquiring TaskID to perform Task Related Tests
        String[] taskResponse = tester.findTaskIdByProcessInstanceID(processInstanceResponse[1]);
        Assert.assertTrue("Task ID Acquired", taskResponse[0].contains(BPMNTestConstants.OK));

        //Claiming a User Task
        String[] claimResponse = tester.claimTaskByTaskId(taskResponse[1]);
        Assert.assertTrue("User has claimed Task", claimResponse[0].contains(BPMNTestConstants.NO_CONTENT));
        String currentAssignee = tester.getAssigneeByTaskId(taskResponse[1]);
        Assert.assertTrue("User has been assigned", currentAssignee.contains(BPMNTestConstants.userClaim));

        //Delegating a User Task
        String delegateStatus = tester.delegateTaskByTaskId(taskResponse[1]);
        Assert.assertTrue("Task has been delegated", delegateStatus.contains(BPMNTestConstants.NO_CONTENT));
        currentAssignee = tester.getAssigneeByTaskId(taskResponse[1]);
        Assert.assertTrue("Testing Delegated User Matches Assignee", currentAssignee.equals(BPMNTestConstants.userDelegate));

        //Commenting on a user task
        String[] commentResponse = tester.addNewCommentOnTaskByTaskId(taskResponse[1], BPMNTestConstants.message);
        Assert.assertTrue("Comment Has been added", commentResponse[0].contains(BPMNTestConstants.CREATED));
        Assert.assertTrue("Comment is visible", commentResponse[1].contains(BPMNTestConstants.message));

        //resolving a User Task
        String status = tester.resolveTaskByTaskId(taskResponse[1]);
        String stateValue = tester.getDelegationsStateByTaskId(taskResponse[1]);
        Assert.assertTrue("Checking Delegation State", stateValue.equals("resolved"));

        //Deleting a Process Instance
        String deleteStatus = tester.deleteProcessInstanceByID(processInstanceResponse[1]);
        Assert.assertTrue("Process Instance Removed", deleteStatus.contains(BPMNTestConstants.NO_CONTENT));

        //Deleting the Deployment
        String undeployStatus = tester.unDeployBPMNPackage(deploymentResponse[1]);
        Assert.assertTrue("Package UnDeployed", undeployStatus.contains(BPMNTestConstants.NO_CONTENT));

    }


}
