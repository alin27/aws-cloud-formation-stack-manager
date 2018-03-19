package com.amazonaws.lambda;

import java.util.Map;
import java.util.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import nz.govt.aws.playground.OpenShiftStackManager;

public class OpenShiftCreateStackHandler implements RequestHandler<Map<String, Object> , Object> {

	static final Logger log = Logger.getLogger(OpenShiftCreateStackHandler.class.getName());
	
    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
    	OpenShiftStackManager openShiftStackManager = new OpenShiftStackManager(input.get("region").toString());
        openShiftStackManager.createOpenShiftStacks();
        log.info("Create OpenShift stacks task run");
        return "OpenShift stack creation task completed.";
    }

}
