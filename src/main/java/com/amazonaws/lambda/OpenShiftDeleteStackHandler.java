package com.amazonaws.lambda;

import java.util.Map;
import java.util.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import nz.govt.aws.playground.OpenShiftStackManager;

public class OpenShiftDeleteStackHandler implements RequestHandler<Map<String, Object> , Object> {

	static final Logger log = Logger.getLogger(OpenShiftDeleteStackHandler.class.getName());
	
    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
    	OpenShiftStackManager openShiftStackManager = new OpenShiftStackManager(input.get("region").toString());
        openShiftStackManager.deleteOpenShiftStacks();
        log.info("Delete OpenShift stacks task run");
        return "OpenShift stack deletion task completed.";
    }

}
