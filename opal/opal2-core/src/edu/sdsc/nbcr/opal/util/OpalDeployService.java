// OpalInfoServlet.java
//
// Servlet that implements the opal dashboard
//
// 11/28/07   - created, Luca Clementi
//


package edu.sdsc.nbcr.opal.util;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationListener;

import org.apache.axis.utils.StringUtils;
import org.apache.axis.AxisFault;
import org.apache.axis.client.AdminClient;

import edu.sdsc.nbcr.opal.state.HibernateUtil;
import edu.sdsc.nbcr.opal.state.ServiceStatus;
//TODO no good using .gui. package here
import edu.sdsc.nbcr.opal.gui.common.OPALService;
import edu.sdsc.nbcr.opal.gui.common.GetServiceListHelper;
import edu.sdsc.nbcr.common.TypeDeserializer;
import edu.sdsc.nbcr.opal.AppConfigType;



import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.lang.Character;

/** 
 *
 * This class implements all the business logic behind the Opal dashboard.
 * 
 * @author clem
 *
 */
public class OpalDeployService extends HttpServlet {

    protected static Log logger = LogFactory.getLog(OpalDeployService.class.getName());
    protected static Deployer deplo = null;
    protected static String wsddDirectory = null;
    
    

    public final void init(ServletConfig config) throws ServletException {
        super.init(config);

        logger.info("Loading OpalDeployService (init method).");
        
        //-------     initializing the DB connection    -----
        java.util.Properties props = new java.util.Properties();
        String propsFileName = "opal.properties";
        String tomcatUrl = "";
        String deployPath = "";
        try {
            // load runtime properties from properties file
            props.load(OpalDeployService.class.getClassLoader().getResourceAsStream(propsFileName));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Unable to load opal.properties: " + e.getMessage());
            //pointless to go on!!;
            return;
        }

        if (props.getProperty("tomcat.url") != null ) {
            tomcatUrl = props.getProperty("tomcat.url");
        }else{
            logger.error("unable to get tomcat.url");
            return;
        }

        if (props.getProperty("opal.deploy.path") != null ) {
            deployPath = props.getProperty("opal.deploy.path");
        }
        if (deployPath.length() == 0) {
            logger.error("opal.deploy.path is not present! Fix your opal.properties.");
            return;
        }


        wsddDirectory = config.getServletContext().getRealPath("/WEB-INF/wsdd/");

        File deployPathFile = new File(deployPath);
        if (! deployPathFile.exists() ) {
            //let's create it
            deployPathFile.mkdir();
        }
        //TODO check if it's a file istead of a dir

            logger.error("passing by here");
        deplo = new Deployer();
            logger.error("passing by here1");
        deplo.setValues(tomcatUrl, deployPathFile);
            logger.error("passing by here2");
        deplo.setName("OpalDeployer");
            logger.error("passing by here3");
        deplo.start();
            logger.error("passing by here4");
 
    }//class init



    public class Deployer extends Thread {

        //protected static Log logger = LogFactory.getLog(OpalDeployService.class.getName());
 
        private String axisAdminUrl;
        private String opalUrl;
        private File deployPathFile;


        public void setValues(String tomcatUrl, File path) {
            opalUrl = tomcatUrl + "/opal2/services";
            axisAdminUrl = tomcatUrl + "/opal2/servlet/AxisServlet";
            deployPathFile = path;
        }

        
        public void run() {
            //get list of deploied services and undeploy them

            logger.info("starting thread OpalDeployer" );
            while (true) {
                try {
                    URL url = new URL(axisAdminUrl);
                    HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                    urlConn.setConnectTimeout(1000);
                    this.sleep(2000);
                    urlConn.connect();
                    break;
                } catch (IOException e) {
                    logger.info("Axis admin is not ready yet");
                } catch (Exception e) {
                    logger.error("problem while waiting for server startup: " + e);
                    return;
                }
            }

            logger.info("initDeployServlet: axis URL: " + axisAdminUrl );
            logger.info("initDeployServlet: deploy path: " + deployPathFile );
            GetServiceListHelper helper = new GetServiceListHelper();
            helper.setBasePrivateURL(opalUrl);
            helper.setBasePublicURL(opalUrl);
            OPALService [] servicesList = helper.getOpalServiceList();
            if ( servicesList == null ) {
                logger.error("Unable to parse the service list from the server");
                return;
            }
            for(OPALService service : servicesList){
                logger.info("undeploying service: " + service.getServiceName());
                undeploy(service.getServiceName());
            }
    
            //now deploy new services
            File [] deployFileList = deployPathFile.listFiles();
            for (File configFile : deployFileList){
                logger.info("deploying service: " + configFile);
                try{deploy(configFile.getCanonicalPath());}
                catch (Exception e){
                    //this should never happen
                    logger.error("configFile does not exist: " + e);
                }
            }

            FileAlterationObserver observer = new FileAlterationObserver(deployPathFile);
            observer.addListener(new DeployListener());
            try{
                observer.initialize();
                while ( true ){
                    observer.checkAndNotify();
                    this.sleep(1000);
                }//while
            }catch(Exception e){
                logger.error("Deployer failed to monitor the deployment directory: " + e);
            }
            //FileAlterationMonitor monitor = new FileAlterationMonitor(1000);
            //monitor.addObserver(observer);
            //monitor.start();
            
        }//run
    

        //TODO make this file
        public void deploy(String appConfig){
    
            // get the location of the application configuration
            logger.info("deploy with appconfig: " + appConfig);
            
            //TODO move this to the init section    
            //reading wsdd template
            String wsddTemplate = wsddDirectory + "/opal_deploy.wsdd";
            // check to make sure that the WSDD template exists
            String templateData = null;
            try {
                File f = new File(wsddTemplate);
                byte[] data = new byte[(int) f.length()];
                FileInputStream fIn = new FileInputStream(f);
                fIn.read(data);
                fIn.close();
                templateData = new String(data);
            } catch (Exception e){
                logger.error("unable to read wsdd template file: " + e);
                return;
            }
            //--end TODO
    
    
            // check to make sure that the application configuration exists
            File configFile = new File(appConfig);
            if (!configFile.exists()) {
                logger.error("Application configuration file " + 
            		 appConfig + " does not exist");
            }
            
            // check to make sure that the file points to valid application
            // configuration
            AppConfigType config = null;
            try {
                config = (AppConfigType) TypeDeserializer.getValue(appConfig,
            					new AppConfigType());;
            } catch (Exception e) {
                logger.error(e);
                logger.error("appConfiguration is invalid: " + appConfig);
                return;
            }
            
            // get the service name and the version number from the app config
            String serviceName = null;
            String configVersion = null;
            if (config.getMetadata() != null) {
                configVersion = config.getMetadata().getVersion();
                serviceName = config.getMetadata().getAppName();
            }
            if ( serviceName == null ) {
                logger.error("Unable to get service name for file " + appConfig );
                return;
            }
            // set the final service name
            if (configVersion != null) {
                serviceName += "_" + configVersion;
            }
            logger.info("Service name used for deployment: " + serviceName);
            
            // replace SERVICE_NAME with actual service name
            String finalData = templateData.replaceAll("@SERVICE_NAME@", serviceName);
            
            // set the location of the config file
            String configLoc = appConfig.replace('\\', '/');
            finalData = finalData.replaceAll("@CONFIG_LOCATION@", configLoc);
            logger.info("Using location of config file: " + configLoc);
            
            // location of final WSDD - also supplied by build.xml
            try{
                File wsddFinal = File.createTempFile("wsdd_" + serviceName,".xml");
                FileOutputStream fOut = new FileOutputStream(wsddFinal);
                fOut.write(finalData.getBytes());
                fOut.close();
                if ( runAxisAdmin(wsddFinal.getCanonicalPath() ) ) {
                    // updating service status in database
                    logger.info("Updating service status in database to ACTIVE");
                    ServiceStatus serviceStatus = new ServiceStatus();
                    serviceStatus.setServiceName(serviceName);
                    serviceStatus.setStatus(ServiceStatus.STATUS_ACTIVE);
                    HibernateUtil.saveServiceStatus(serviceStatus);
                    //TODO delete wsddfinal
                    //wsddFinal.delete();
                }            
            }catch (Exception e) {
                logger.error("Deploy: failing while writing wsdd file " + e);
            }
        }
    
    
    
        public void undeploy(String serviceName){
            // get the service name
            logger.info("Undeploy called. ServiceName set to: " + serviceName);
        
            // get the version number - optional
            //TODO move this to the init section    
            //reading wsdd template
            String wsddTemplate = wsddDirectory + "/opal_undeploy.wsdd";
            // check to make sure that the WSDD template exists
            String templateData = null;
            try{
                File f = new File(wsddTemplate);
                byte[] data = new byte[(int) f.length()];
                FileInputStream fIn = new FileInputStream(f);
                fIn.read(data);
                fIn.close();
                templateData = new String(data);
            }catch (Exception e){
                logger.error("Unable to read the wsdd undeploy: " + e);
                return;
            }
            
            //--end TODO
            String finalData = templateData.replaceAll("@SERVICE_NAME@", serviceName);
            try{ 
                File wsddFinal = File.createTempFile("wsdd_" + serviceName,".xml");
                FileOutputStream fOut = new FileOutputStream(wsddFinal);
                fOut.write(finalData.getBytes());
                fOut.close();
                if ( runAxisAdmin(wsddFinal.getCanonicalPath() ) ) {
                    logger.info("Updating service status in database to INACTIVE");
                    ServiceStatus serviceStatus = new ServiceStatus();
                    serviceStatus.setServiceName(serviceName);
                    serviceStatus.setStatus(ServiceStatus.STATUS_INACTIVE);
                    HibernateUtil.saveServiceStatus(serviceStatus);
                    //TODO delete wsddfinal
                    //wsddFinal.delete();
                }
            }catch (Exception e) {
                logger.error("Failing while writing wsdd file " + e);
            }
    
        }
    
    
    
        /**
         * give the wsdd file path it invoke the AxisAdmin client to 
         * preform the requested action
         */
        boolean runAxisAdmin(String wsddFilePath){
    
            logger.info("AxisAdmin: called with wsdd path:" + wsddFilePath);
            String [] args = {"-l" + axisAdminUrl, wsddFilePath};
     
            try {
                AdminClient admin = new AdminClient();
                String result = admin.process(args);
                if (result != null) {
                    logger.info( StringUtils.unescapeNumericChar(result) );
                    return true;
                } else {
                    logger.error("AxisAdmin failed for unknown reason");
                    return false;
                }
            } catch (AxisFault ae) {
                logger.error("AxisAdmin fault: " + ae.dumpToString());
                return false;
            } catch (Exception e) {
                logger.error("AxisAdmin exception: " + e.getMessage());
                return false;
            }
        }
    }//class Deployer

        /**
         * this class implements the action that will be taken
         * when a file is modifed in the deploy directory
         */
        public class DeployListener implements FileAlterationListener{

            public void onStart(final FileAlterationObserver observer){}
            public void onStop(final FileAlterationObserver observer){}
            public void onDirectoryCreate(final File directory){}
            public void onDirectoryChange(final File directory){}
            public void onDirectoryDelete(final File directory){}

            
            public void onFileCreate(final File file){
                logger.info("Autodeploy file: " + file);
            }

            public void onFileChange(final File file){
                logger.info("AppConfig modified (nothing will be done): " + file);
            }

            public void onFileDelete(final File file){
                logger.info("Undeploying file: " + file);
            }

        }//class DeployListner


    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
        //deplo.destroy();
    }

}
