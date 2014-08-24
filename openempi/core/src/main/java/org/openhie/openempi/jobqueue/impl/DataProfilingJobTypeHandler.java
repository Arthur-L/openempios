package org.openhie.openempi.jobqueue.impl;

import java.util.Map;

import org.openhie.openempi.context.Context;
import org.openhie.openempi.jobqueue.JobParameterConstants;
import org.openhie.openempi.model.Entity;
import org.openhie.openempi.model.JobEntry;
import org.openhie.openempi.model.JobEntryEventLog;
import org.openhie.openempi.model.JobStatus;

public class DataProfilingJobTypeHandler extends AbstractJobTypeHandler
{

    public DataProfilingJobTypeHandler() {
        super();
    }

    public void run() {
        authenticateUser();
        JobEntry jobEntry = getJobEntry();
        Map<String,Object> params = extractParameters(jobEntry);
        logJobEntryParameters(params);
        String task = (String) params.get(JobParameterConstants.MATCHINGTASK_PARAM);
        if (task == null || task.length() == 0) {
            log.info("Unable to process the matching job with id " + jobEntry.getJobEntryId() + 
                    " since the specific matching task to perform was not specified.");
            return;
        }
        
        if (!task.equals(JobParameterConstants.MATCHINGTASK_INITIALIZATION) &&
                !task.equals(JobParameterConstants.MATCHINGTASK_LINKAGE)) {
            log.info("Unable to process the matching job with id " + jobEntry.getJobEntryId() + 
                    " since the specific matching task to perform is unknown: " + task);
            return;
        }
        
        if (task.equals(JobParameterConstants.MATCHINGTASK_INITIALIZATION)) {
            initializeMatchingAlgorithm(jobEntry);
        } else {
            generateAllLinks(jobEntry);
        }
    }

    private void initializeMatchingAlgorithm(JobEntry jobEntry) {
        
        try {
            Entity entity = jobEntry.getEntity();
            Context.getRecordManagerService().initializeRepository(entity);
            updateJobEntry(true, "Successfully completed the initialization of the matching service.");
        } catch (Exception e) {
            log.warn("Failed while initializing the matching service: " + e, e);
            updateJobEntry(false, "Failed to initialize the matching service due to: " + e.getMessage());
        }
    }

    private void generateAllLinks(JobEntry jobEntry) {
        
        try {
            Entity entity = jobEntry.getEntity();
            Context.getRecordManagerService().linkAllRecordPairs(entity);
            updateJobEntry(true, "Successfully completed linking all record pairs.");
        } catch (Exception e) {
            log.warn("Failed while linking all record pairs: " + e, e);
            updateJobEntry(false, "Failed to link all record pairs due to: " + e.getMessage());
        }
    }
    
    public void updateJobEntry(boolean success, String message) {
        java.util.Date completed = new java.util.Date();
        JobEntryEventLog event = new JobEntryEventLog();
        event.setDateCreated(completed);
        event.setLogMessage(message);

        JobEntry jobEntry = getJobEntry();
        jobEntry.setDateCompleted(completed);
        jobEntry.setItemsErrored(0);
        jobEntry.setItemsProcessed(0);
        jobEntry.setItemsSuccessful(0);
        jobEntry.setJobStatus(JobStatus.JOB_STATUS_COMPLETED);
        JobEntry updatedJob = getJobEntryDao().updateJobEntry(jobEntry);        
        getJobEntryDao().logJobEntryEvent(updatedJob,  event);
    }
}
