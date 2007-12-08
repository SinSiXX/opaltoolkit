

package edu.sdsc.nbcr.opal.dashboard.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.util.PGInterval;

import edu.sdsc.nbcr.opal.dashboard.util.DateHelper;


public class DBManager {
    

    protected static Log log = LogFactory.getLog(DBManager.class.getName());

    
    private Connection conn = null;
    private String databaseUrl = null;
    private String driver = null;
    private String dbUserName = null;
    private String dbPassword = null;
    private String error = null;
    
    
    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public String getDriver() {
        return driver;
    }

    public String getDbUserName() {
        return dbUserName;
    }

    public DBManager( String databaseUrl, String driver, String dbUserName, String dbPassword) {
        super();

        this.databaseUrl = databaseUrl;
        this.driver = driver;
        this.dbUserName = dbUserName;
        this.dbPassword = dbPassword;
    }
    
    //default constructor
    public DBManager(){
        conn = null;
    }
    
    public boolean init() {
        try {
            Class.forName(driver).newInstance();
            conn = DriverManager.getConnection(databaseUrl, dbUserName, dbPassword);
            log.info("Connection to the database established");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            error = e.getMessage();
            conn = null;
            return false;
        }
    }
    
    
    public boolean isConnected(){
        if ( conn != null ) return true;
        else return false;
    }

    public void close() throws java.sql.SQLException{
        if (isConnected()) {
            conn.close();
        }
        
    }


    /**
     * returns the list of services available on the Opal server
     * The service has to be called at least once in order to be retrieved 
     * by the client 
     * 
     * @return an Array of strings
     * 
     */
    public String [] getServicesList(){
        ArrayList serviceList = new ArrayList();
        String query = "select service_name from job_status group by service_name;";
        if ( ! isConnected() ) return null;
        try {
            Statement sql = conn.createStatement();
            ResultSet rs = sql.executeQuery(query);
            for (; rs.next(); ) {
                serviceList.add(rs.getString("service_name"));
            }
        }catch (SQLException e ) {
            log.error("Error retreiving the list of services: " + e.getMessage(), e);
        }
        return (String[]) serviceList.toArray(new String[serviceList.size()]);
    }
    

    /**
     * legacy function it is here only for backward compatibility
     * 
     * @see getResult()
     * 
     * getHits return an array of double where every elements represent the number of 
     * hits received for the service from the startDate to the endDate
     * 
     * the returned array will have length equal to the number of days included between endDate
     * startDate
     * 
     * @param startDate
     * @param endDate
     * @param service
     * @return
     */
    public double [] getHits(Date startDate, Date endDate, String service){
        
        return getResults(startDate, endDate, service, "hits");
    }
    
    /**
     * @see getResults
     * 
     * @param startDate
     * @param endDate
     * @param service
     * @return
     */
    public double [] getError(Date startDate, Date endDate, String service){
        return getResults(startDate, endDate, service, "error");
    }
    
    /**
     * @see getResults
     *  
     * @param startDate
     * @param endDate
     * @param service
     * @return
     */
    public double [] getExectime(Date startDate, Date endDate, String service){
        return getResults(startDate, endDate, service, "exectime");
    }//getExectime
    

    
    /**
     * This is a generic functions which make a query and return an array of double containing a value 
     * for every day of the query. The value of the returned array depends on the type parameters. 
     * The return array will have the size equal to numberOfDay(endDate - startDate)
     * 
     * @param startDate the beginning of the time series
     * @param endDate the end of the time series
     * @param service the service you want to get the data from
     * @param type this can be:
     * <ul>
     * <li>hits: the number daily of hits received for the service</li>
     * <li>exectime: the daily average execution time</li>
     * <li>error: the daily number of failed job</li>
     * </ul>  
     * 
     * @return an array of values 
     */
    public double [] getResults(Date startDate, Date endDate, String service, String type){

        //creating the query
        int numberOfDays = DateHelper.getOffsetDays(endDate, startDate);
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");
        String startDateString = formatter.format(startDate);
        String endDateString = formatter.format(endDate);
        
        String query = null;
        if ( type.equals("hits") ) {
            query = "select to_date(start_time, 'Mon DD, YYYY'), count(*) " +
                "from job_status where service_name='" + service + "' " +
                " and to_date(start_time, 'Mon DD, YYYY') >= to_date('" + startDateString + "', 'Mon DD, YYYY') " +
                " and to_date(start_time, 'Mon DD, YYYY') <= to_date('" + endDateString + "', 'Mon DD, YYYY') " +
                "group by to_date(start_time, 'Mon DD, YYYY') " +
                "order by to_date(start_time, 'Mon DD, YYYY') desc;";
        }else if (type.equals("exectime") ) {
            query = "select to_date(start_time, 'Mon DD, YYYY'), sum(to_timestamp(last_update, 'Mon DD, YYYY HH:MI:SS AM') - to_timestamp(start_time, 'Mon DD, YYYY HH:MI:SS AM'))/count(*) as average " +
                "from job_status " +
                "where service_name='" + service + "' " +
                "and to_date(start_time, 'Mon DD, YYYY') >= to_date('" + startDateString + "', 'Mon DD, YYYY') " +
                "and to_date(start_time, 'Mon DD, YYYY') <= to_date('" + endDateString + "', 'Mon DD, YYYY') " +
                "and code=8 " +
                "group by to_date(start_time, 'Mon DD, YYYY') " +
                "order by to_date(start_time, 'Mon DD, YYYY') desc;" ;
        } else if (type.equals("error") ){
            query  = "select to_date(start_time, 'Mon DD, YYYY'), count(*) " +
            		"from job_status " +
            		"where service_name='" + service + "' " +
            		"and to_date(start_time, 'Mon DD, YYYY') >= to_date('" + startDateString + "', 'Mon DD, YYYY') " +
            		"and to_date(start_time, 'Mon DD, YYYY') <= to_date('" + endDateString + "', 'Mon DD, YYYY') " +
            		"and code=4 " +
            		"group by to_date(start_time, 'Mon DD, YYYY') " +
            		"order by to_date(start_time, 'Mon DD, YYYY') desc;";   
        }
        log.info("Going to get the " + type + " for the service: " + service + " and with start date: " + startDateString + " " +
                "and end date: " + endDateString + "  " + 
                "\nRunning the following query: " + query);
        
        //going to execute the query
        if ( ! isConnected() ) return null;
        try {
            Statement sql = conn.createStatement();
            ResultSet rs = sql.executeQuery(query);
            double [] values = new double[numberOfDays+1];
            int counter = numberOfDays;
            Date previousDate =  endDate;//we are gonna start from the current date (today)
            //now we have put the data from the result of the query into the return array
            while( rs.next() ) {
                Date date = rs.getDate("to_date");
                while ( ! DateHelper.compareDates(previousDate, date) ){
                    //since some day can have no hits we have to put zero in the array for those days
                    values[counter] = 0;
                    log.trace("Inserting a zero for date: " + previousDate + " on position: " + counter);
                    counter--;
                    previousDate = DateHelper.subtractDay(previousDate);
                }//if
                //we don't have a gap!
                if (type.equals("hits") ) {
                    values[counter] = (double) rs.getInt("count");                    
                }else if ( type.equals("exectime") ) {
                    PGInterval pginterval =  (PGInterval)rs.getObject("average");
                    double interval =  pginterval.getSeconds() + pginterval.getMinutes() * 60 + pginterval.getHours() * 60 * 60 + 
                    pginterval.getDays() * 24 * 60 * 60 + pginterval.getMonths() * 30 * 24 * 60 * 60;
                    values[counter] = interval;
                }else if ( type.equals("error") ) {
                    values[counter] = (double) rs.getInt("count"); 
                }
                log.trace("Inserting the value " + values[counter] + " for date: " + date + " on position: " + counter);
                //decrease the counter
                counter--;
                //and decrease the date
                previousDate = DateHelper.subtractDay(previousDate);
                
            }//while
            //everything went fine, let's log and return 
            String str = new String();
            for ( counter = 0; counter < values.length; counter++)
                str += values[counter] + ", ";
            log.info("The query on " + type + " with service " + service + " is returning values: " + str);
            return values;
        }catch (SQLException e ) {
            log.error("Error while querying for the " + type + " with service " + service + " : " + e.getMessage(), e);
            return null;
        }
    }

}
