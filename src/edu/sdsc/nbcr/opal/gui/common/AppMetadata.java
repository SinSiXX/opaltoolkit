package edu.sdsc.nbcr.opal.gui.common;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This bean class holds all the data necessary to create the submission form, it is also used by struts 
 * to place the inputs of the user when he submits the form.<br/><br/>
 * This class has the following fields:
 * <ul>
 * <li>String serviceName - the name of the service
 * <li>String usage - a description of its usage
 * <li>String [] info - some text to describe the command line
 * <li>String URL - the URL which correspond to this service end point
 * <li>ArgFlag [] argFlags - a list of ArgFlag objects
 * <li>ArgParam [] argParams - a list of ArgParam objects
 * <li>Group [] groups - a list of group to cluster ArgFlag and ArgParam for this service 
 * <li>String separator - the separator used for the various arguments
 * <li>String cmdLine - place holder used for keeping the command line submitted by the user using the web form
 * <li>FormFile [] files - place holder for the input file submitted by the user using the web form
 * <li>String jobId - place holder for the jobID once this job has been submitted to opal
 * <li>boolean addFile - place holder used by the submission form, if this field is true it means that the user does not wants to submit the job but it only wants to add an input file
 * </ul>
 * 
 * 
 * @author clem
 *
 */
public class AppMetadata extends ActionForm{
	
    
    protected Log log = LogFactory.getLog(Constants.PACKAGE);
    
	private String serviceName;
	private String usage;
	private String [] info;
	private String URL;
	private ArgFlag [] argFlags;
	private ArgParam [] argParams;
	private Group [] groups;
	private String separator;
	//these are to hold the values from the form 
	private String cmdLine;
	private FormFile [] files;
	private String jobId;
	private boolean addFile;
	


	/**
	 * Default constructor
	 */
	public AppMetadata() {
        serviceName = null;
        usage = null;
        info = null;
        URL = null;
        argFlags = null;
        argParams = null;
        separator = null;
        cmdLine = null;
        files = new FormFile[1];
        addFile = false;
	}
    
	/**
	 * It tells you if the current instance of the AppMetadata supports 
	 * the complex submission form
	 * 
	 * @return true is we have a complex submission form
	 * 
	 */
    public boolean isArgMetadataEnable() {
        if ( (argFlags == null) && (argParams == null) ) {
            return false;
        }
        else return true;
    }
    
    /**
     * a string representation of the current AppMetadata
     * 
     */
    public String toString(){
        String str = "URL: " + URL + "\n";
        str += "Usage: " + usage + "\n";
        str += "Info:\n";
        if ( info != null ) {
            for (int i = 0; i < info.length; i++){
                str += info[i] + "\n";
            }
        }
        if ((argFlags != null) || (argParams != null)) {
            //display also the args part
            str += "The types of the application are:\n";
        }
        if (argParams != null){
            str += "Parameters separator is " + separator + " and their type is:\n";
            for (int i = 0; i < argParams.length; i++){
                str += argParams[i].toString() + "\n";
            }
        }
        if (argFlags != null){
            str += "Flags:\n";
            for (int i = 0; i < argFlags.length; i++){
                str += argFlags[i].toString() + "\n";
            }
        }
        if ( groups != null ) {
        	str += "Groups:\n";
        	for (int i = 0; i < groups.length; i++){
                str += groups[i].toString() + "\n";
            }
        }
        return str;
    }
    
    /**
     * It returns the number of files submitted by the user in the ArgParam array
     * 
     * @return the number of files submitted by the user, -1 if no complex form is enable 
     */
    public int getNumArgFileSubmitted(){
        if (isArgMetadataEnable() ) {
            int numFile = 0;
            for (int i = 0; i < argParams.length; i++){
                if ( argParams[i].isFileUploaded() ) 
                    numFile++;
            }
            return numFile;
        }
        else return -1;
    }
    
    /**
     * It returns the i-th ArgParam value containing a file submitted by the user
     * 
     * @param position the index of the element that will be returned
     * @return the i-th ArgParam that contains a input file submitted by the user
     */
    public ArgParam getArgFileSubmitted(int position){
        int currentPosition = 0;
        for (int i = 0; i < argParams.length; i++ ) {
            if (argParams[i].isFileUploaded() ) {
                if (position == currentPosition) {
                    return argParams[i];
                }
                //we are not yet at the right position let's increment
                currentPosition++;
            }//if
        }//for
        return null;
    }
    
    /**
     * It returns the number of untagged parameters present in the data structure
     * 
     */
    public int getNumUnttagedParams(){
        int counter = 0;
        for (int i = 0; i < argParams.length; i++ ){
            if (argParams[i].getTag() == null )
                counter++;
        }
        return counter;
    }

    /**
     * It resets the place holder for the submitted values in the entire data structure.
     * 
     */
    public void reset(ActionMapping mapping, HttpServletRequest  request){
        if ( argFlags != null ) 
            for (int i = 0; i < argFlags.length; i++ ){
                argFlags[i].setSelected(false);
            }
        if ( argParams != null )
            for (int i = 0; i < argParams.length; i++ ){
                argParams[i].reset();
            }
    }
    
    /**
     * It returns the ArgFlag that has the id equals to id.
     * If it does not find an ArgFlag with an id equals to id it returns null
     * 
     */
    public ArgFlag getArgFlagId(String id){    	
    	for (int i = 0; i < argFlags.length; i++ ) {
    		if ( argFlags[i].getId().equals(id) )
    			return argFlags[i];
    	}
    	return null;
    }
    
    
    /**
     * It returns the ArgParam that has the id equals to id.
     * If it does not find an ArgParam with an id equals to id it returns null
     */    
    public ArgParam getArgParamId(String id){
    	for (int i = 0; i < argParams.length; i++ ) {
    		if ( argParams[i].getId().equals(id) )
    			return argParams[i];
    	}
    	return null;
    }


    /* -----    below only getter and setter methods   -------    */
    
    /**
     * @return the serviceName
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @param serviceName the serviceName to set
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * @return the usage
     */
    public String getUsage() {
        return usage;
    }

    /**
     * @param usage the usage to set
     */
    public void setUsage(String usage) {
        this.usage = usage;
    }

    /**
     * @return the info
     */
    public String[] getInfo() {
        return info;
    }

    /**
     * @param info the info to set
     */
    public void setInfo(String[] info) {
        this.info = info;
    }

    /**
     * @return the uRL
     */
    public String getURL() {
        return URL;
    }

    /**
     * @param url the uRL to set
     */
    public void setURL(String url) {
        URL = url;
    }

    /**
     * @return the argFlags
     */
    public ArgFlag[] getArgFlags() {
        return argFlags;
    }

    /**
     * @param argFlags the argFlags to set
     */
    public void setArgFlags(ArgFlag[] argFlags) {
        this.argFlags = argFlags;
    }

    /**
     * @return the argParams
     */
    public ArgParam[] getArgParams() {
        return argParams;
    }

    /**
     * @param argParams the argParams to set
     */
    public void setArgParams(ArgParam[] argParams) {
        this.argParams = argParams;
    }

    /**
     * @return the groups
     */
    public Group[] getGroups() {
        return groups;
    }

    /**
     * @param groups the groups to set
     */
    public void setGroups(Group[] groups) {
        this.groups = groups;
    }

    /**
     * @return the separator
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * @param separator the separator to set
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * @return the cmdLine
     */
    public String getCmdLine() {
        return cmdLine;
    }

    /**
     * @param cmdLine the cmdLine to set
     */
    public void setCmdLine(String cmdLine) {
        this.cmdLine = cmdLine;
    }

    /**
     * @return the files
     */
    public FormFile[] getFiles() {
        return files;
    }

    /**
     * @param files the files to set
     */
    public void setFiles(FormFile[] files) {
        this.files = files;
    }

    /**
     * @return the jobId
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @param jobId the jobId to set
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * @return the addFile
     */
    public boolean isAddFile() {
        return addFile;
    }

    /**
     * @param addFile the addFile to set
     */
    public void setAddFile(boolean addFile) {
        this.addFile = addFile;
    }
    
}
