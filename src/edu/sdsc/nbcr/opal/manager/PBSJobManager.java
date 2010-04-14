package edu.sdsc.nbcr.opal.manager;

import java.util.Properties;
import java.util.Arrays;
import java.util.List;

import org.globus.gram.GramJob;

import org.apache.log4j.Logger;

import edu.sdsc.nbcr.opal.manager.pbsTorque.Job;

import edu.sdsc.nbcr.opal.AppConfigType;
import edu.sdsc.nbcr.opal.StatusOutputType;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

/**
 *
 * Implementation of an Opal Job Manager using DRMAA
 */
public class PBSJobManager implements OpalJobManager {

    // get an instance of a log4j logger
    private static Logger logger = Logger.getLogger(PBSJobManager.class.getName());

    private Properties props; // the container properties being passed
    private AppConfigType config; // the application configuration
    private StatusOutputType status; // current status
    private String handle; // the DRMAA job id for this submission
    private boolean started = false; // whether the execution has started
    private volatile boolean done = false; // whether the execution is complete

    private Job job; // job information after PBS submit

    /**
     * Initialize the Job Manager for a particular job
     *
     * @param props the properties file containing the value to configure this plugin
     * @param config the opal configuration for this application
     * @param handle manager specific handle to bind to, if this is a resumption. 
     * NULL,if this manager is being initialized for the first time.
     * 
     * @throws JobManagerException if there is an error during initialization
     */
    public void initialize(Properties props,
			   AppConfigType config,
			   String handle)
	throws JobManagerException {
	logger.info("called");

	this.props = props;
	this.config = config;
	this.handle = handle;

	// initialize status
	status = new StatusOutputType();
    }
    
    /**
     * General clean up, if need be 
     *
     * @throws JobManagerException if there is an error during destruction
     */
    public void destroyJobManager()
	throws JobManagerException {
	logger.info("called");

	// TODO: not sure what needs to be done here
	throw new JobManagerException("destroyJobManager() method not implemented");
    }
    
    /**
     * Launch a job with the given arguments. The input files are already staged in by
     * the service implementation, and the plug in can assume that they are already
     * there
     *
     * @param argList a string containing the command line used to launch the application
     * @param numProcs the number of processors requested. Null, if it is a serial job
     * @param workingDir String representing the working directory of this job on the local system
     * 
     * @return a plugin specific job handle to be persisted by the service implementation
     * @throws JobManagerException if there is an error during job launch
     */
    public String launchJob(String argList, 
			    Integer numProcs, 
			    String workingDir)
	throws JobManagerException {
	logger.info("called");

	// make sure we have all parameters we need
	if (config == null) {
	    String msg = "Can't find application configuration - "
		+ "Plugin not initialized correctly";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// create list of arguments
	String args = config.getDefaultArgs();
	if (args == null) {
	    args = argList;
	} else {
	    String userArgs = argList;
	    if (userArgs != null)
		args += " " + userArgs;
	}
	if (args != null) {
	    args = args.trim();
	}
	logger.debug("Argument list: " + args);

	// get the number of processors available
	String systemProcsString = props.getProperty("num.procs");
	int systemProcs = 0;
	if (systemProcsString != null) {
	    systemProcs = Integer.parseInt(systemProcsString);
	}

	// launch executable using PBS
	String cmd = null;

	if (config.isParallel()) {
	    // make sure enough processors are present for the job
	    if (numProcs == null) {
		String msg = "Number of processes unspecified for parallel job";
		logger.error(msg);
		throw new JobManagerException(msg);
	    } else if (numProcs.intValue() > systemProcs) {
		String msg = "Processors required - " + numProcs +
		    ", available - " + systemProcs;
		logger.error(msg);
		throw new JobManagerException(msg);
	    }

	    // check if the mpi.run property is set
	    String mpiRun = props.getProperty("mpi.run");
	    if (mpiRun == null) {
		String msg = "Can't find property mpi.run for running parallel job";
		logger.error(msg);
		throw new JobManagerException(msg);
	    }

	    // append arguments - needs to be this way to locate machinefile
	    cmd = mpiRun + 
		//		" -machinefile $TMPDIR/machines" +
		" -np " + numProcs + " " +
		config.getBinaryLocation();
	    if ((args != null) && (!(args.equals("")))) {
		cmd += " " + args;
	    }

	    logger.debug("CMD: " + args);
	} else {
	    // create command string and arguments for serial run
	    cmd = config.getBinaryLocation();
	    if ((args != null) && (!(args.equals("")))) {
		cmd += " " + args;
	    }
	    logger.debug("CMD: " + cmd);
	}

	// get the hard run limit
	long hardLimit = 0;
	if ((props.getProperty("opal.hard_limit") != null)) {
	    hardLimit = Long.parseLong(props.getProperty("opal.hard_limit"));
	    logger.warn("Property hard_limit is not supported by this job manager");
	}

	// launch the job using the above information
	try {
	    logger.debug("Working directory: " + workingDir);
	    
	    // create a submission script from the params
	    String script = createSubmissionScript(cmd,
						   workingDir);
	    job = new Job(config.getBinaryLocation(),
			  script);
	    if (config.isParallel()) {
		job.setNodes(numProcs.toString());
	    }

	    handle = job.queue();
	    logger.info("PBS job has been submitted with id " + handle);
	} catch (Exception e) {
	    String msg = "Error while running executable via DRMAA - " +
		e.getMessage();
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// TODO: need to figure out how to wait for activation

	// update status to active
	status.setCode(GramJob.STATUS_ACTIVE);
	status.setMessage("Execution in progress");

	// notify listeners that process is activated
	started = true;
	synchronized(this) {
	    this.notifyAll();
	}

	// return an identifier for this process
	return handle;
    }

    /**
     * Block until the job state is GramJob.STATUS_ACTIVE
     *
     * @return status for this job after blocking
     * @throws JobManagerException if there is an error while waiting for the job to be ACTIVE
     */
    public StatusOutputType waitForActivation() 
	throws JobManagerException {
	logger.info("called");

	// TODO: Need to figure out how to wait for activation

	// check if this process has been started already
	if (!started) {
	    String msg = "Can't wait for a process that hasn't be started";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}

	// poll till status is ACTIVE or ERROR
	while (!started) {
	    try {
		synchronized(this) {
		    this.wait();
		}
	    } catch (InterruptedException ie) {
		// minor exception - log exception and continue
		logger.error(ie.getMessage());
		continue;
	    }
	}

	return status;
    }

    /**
     * Block until the job finishes executing
     *
     * @return final job status
     * @throws JobManagerException if there is an error while waiting for the job to finish
     */
    public StatusOutputType waitForCompletion() 
	throws JobManagerException {
	logger.info("called");

	// check if this process has been started already
	if (!started) {
	    String msg = "Can't wait for a process that hasn't be started";
	    logger.error(msg);
	    throw new JobManagerException(msg);
	}


        // poll till status is COMPLETE
	String jobState = "";
	boolean gotStatus = true;
        while (true) {
            try {
                // sleep for 3 seconds
                Thread.sleep(3000);

                // print job status
                jobState = Job.getJobStatus(handle);
                logger.info("Received job status: " + jobState);
            } catch (Exception e) {
		// this is probably because keep_completed is not set
		// so PBS forgets about completed jobs
                String msg = "Can't wait for job to complete - " +
                    e.getMessage();
                logger.warn(msg);
		gotStatus = false;
		break;
            }
        }

	// update status
	if (!gotStatus) {
	    status.setCode(GramJob.STATUS_DONE);
	    status.setMessage("Execution complete - " + 
			      "check outputs to verify successful execution");
	} else {
	    if (jobState.equals("C")) {
		status.setCode(GramJob.STATUS_DONE);
		status.setMessage("Execution complete - " + 
				  "check outputs to verify successful execution");
	    } else {
		status.setCode(GramJob.STATUS_FAILED);
		status.setMessage("Execution failed - " + 
				  "final job status is " + jobState);
	    }
	}

	return status;
    }

    /**
     * Destroy this job
     * 
     * @return final job status
     * @throws JobManagerException if there is an error during job destruction
     */
    public StatusOutputType destroyJob()
	throws JobManagerException {
	logger.info("called");
	
	// TODO: need to figure out how to destroy job
	throw new JobManagerException("Destroy not supported for PBS jobs");
    }

    /**
     * Create a job submission script from input arguments
     */
    private String createSubmissionScript(String cmd,
					  String workingDir) 
	throws IOException {

	File tmpFile = File.createTempFile("/tmp", ".submit");
	PrintWriter pw = new PrintWriter(new FileWriter(tmpFile));

	// create the submission script
	pw.println("#!/bin/sh");
	pw.println("#PBS -e " + workingDir + File.separator + "stderr.txt");
	pw.println("#PBS -o " + workingDir + File.separator + "stdout.txt");
	pw.println("cd " + workingDir);
	pw.println(cmd);
	pw.close();

	// return path to submission script
	return tmpFile.getAbsolutePath();
    }
}
