// PloterServlet.java
//
// simple java servlet that generates graph images based
// on url arguments.  Data is retrieved from a mysql 
// database storing opal data usage
//
// Luca Clementi
//
// 11/20/07 - created


package edu.sdsc.nbcr.opal.dashboard.servlet;


import edu.sdsc.nbcr.opal.dashboard.persistence.DBManager;
import edu.sdsc.nbcr.opal.dashboard.util.DateHelper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;

import java.io.*;

//import java.sql;
import java.text.SimpleDateFormat;
import java.util.regex.*;


/** 
 * 
 *  See the doPost function documentation
 *  
 */

public class PloterServlet extends HttpServlet
{  
    
    // get an instance of the log4j Logger
    protected static Log log = LogFactory.getLog(PloterServlet.class.getName());

    private DBManager dbManager = null;
    private boolean initialized = false;
    Color defaultBGColor = new Color(255, 255, 255); // white
    private static final String exectime = "exectime";
    private static final String hits = "hits";
    private static final String error = "error";

    /**
     * returns basic information as to what the servlet does
     */
    public final String getServletInfo() {
        return "generates a graph image of gama usage based on input parameters";
    }

    /**
     * initialized the servlet reads the opal.properties file and 
     * try to connects to database and sets corresponding class
     * variables
     */
    public final void init(ServletConfig config) throws ServletException {
        super.init(config);

        dbManager = (DBManager) config.getServletContext().getAttribute("dbManager");
        if ( dbManager == null){
            initialized = false;
        } else initialized = true;
    }
    
    /**
     * closes servlet and cleans up connection
     */
    public void destroy() {

        System.out.println("Closing database connection ...");
        if (initialized == true && dbManager != null) {
            try {
                dbManager.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * same as doPost
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
           throws ServletException, IOException
    {  
	doPost(request, response);
    }

    /**
     * generates the image based on url parameters </br>
     * http://server/servlet?width=300&height=200&duration=7&...
     * </br>
     * <ul>
     * <li>width: the width of the image</li>
     * <li>height: the height of the image</li>
     * <li>startDate and endDate: time interval for the plotting the date
     * should be formatted following YYYYMMDD, if this parameters are not present 
     * the default is the last month</li>
     * <li>servicesName: it specifies the services you want to plot 
     * (for multiple services it sould be repeated multiple times)</li>
     * <li>type: it specifies the type of chart that you what to display 
     * (hits for display the number of hits, exectime to display the average execution time)</li>
     * </ul>  
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Color bg = calculateColor(request.getParameter("bgcolor"));

        //
        int width = 300;
        int height = 200;
        Date startDate = null, endDate = null;
        // this can be hits or exectime, defaults is hits
        String type = "hits"; // default;
        String tmp;
        String [] servicesName = null;

        // first ensure the DB connection is valid
        if (! initialized) {
            doError("No connection to the database", request, response);
            return;
        }
        
        // set up the parameters 
        //
        try {
            width = Integer.parseInt(request.getParameter("width"));
            height = Integer.parseInt(request.getParameter("height"));
        } catch (Exception ex) {
            //TODO 
            return;
        }

        
        //TODO handle errors
        tmp = request.getParameter("type");
        if (tmp.equals(hits) || tmp.equals(exectime) || tmp.equals(error) )
            type = tmp;

        servicesName = request.getParameterValues("servicesName");
        if ( servicesName == null ) {
            servicesName = dbManager.getServicesList();
        }

        //begin and end date
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");
        if ( startDateStr != null )
            startDate = DateHelper.parseDate(startDateStr);
        if ( endDateStr != null )
            endDate = DateHelper.parseDate(endDateStr);
        
        if ( (startDate == null) || (endDate == null)) {
            //no date provided let's put the default
            log.error("No start and end date provided.");
            endDate = DateHelper.getEndDate();
            startDate = DateHelper.getStartDate();
        }
        
        // ok, so all the parameters are now parsed...
        //let's get the data out of the DB 
        log.info("Going to generate a chart (" + type + ") " + width + "x"
                + height + " with beginning date " + startDate + " and end date " + endDate);
        String xAxisTitle = "";
        String yAxisTitle = "";
        String title = "";
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeries series = null;
        double [] temp = null;
        for ( int i = 0; i < servicesName.length; i++ ) {
            temp = dbManager.getResults(startDate, endDate, servicesName[i], type);
            
            
            if ( (temp != null) ){ //&& (temp.length != duration) ) { 
                //we have the data from the DB let's create a time series  
                series = new TimeSeries(servicesName[i], Day.class);
                Date iterator = (Date) startDate.clone();
                for (int k = 0; k < temp.length; k++){
                    series.add(new Day(iterator), temp[k]);
                    iterator = DateHelper.addDays(iterator, 1);
                }
            }//if
            else {//something went wrong
                if (temp == null) {
                    log.error("The query to the data base for service: " + servicesName[i] + " returned a null set of values");
                }else {//temp.length != duration something wrong
                    log.error("The data returned from the DB for the service: " + servicesName[i] + " was invalid.");
                }
            }
            dataset.addSeries(series);
        }//for
        
        //built the graph
        xAxisTitle = "Date";
        
        if ( type.equals(hits) ) {
            yAxisTitle = "Numbers of hits";
            title = "Jobs executed per day";
        }else if ( type.equals(exectime) ) {
            yAxisTitle = "Execution time";
            title = "Daily average execution time for a job submission";
        }else if ( type.equals(error) ) {
            yAxisTitle = "Number of error";
            title = "Number of failed executions per day";
        }
        
        //drawing the image and returning
        JFreeChart chart = ChartFactory.createTimeSeriesChart( title, xAxisTitle, yAxisTitle, dataset,
                true, true, false);
        XYPlot plot = chart.getXYPlot();
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-dd-yyyy"));
        BufferedImage bufferedImage = chart.createBufferedImage(width, height);
        if (bufferedImage == null) {
            log.error("The buffered immage is null maybe you are headless!!");
            log.error("your head is " + GraphicsEnvironment.isHeadless());
        }
        byte [] imageByte = EncoderUtil.encode(bufferedImage, ImageFormat.JPEG);
        response.setContentType("image/jpeg" );
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(imageByte);
        outputStream.flush();
        return;
    }

    
    private void doError(String errorMsg, HttpServletRequest req, HttpServletResponse res){
        RequestDispatcher dispatcher;
        //TODO set a properties with an error 
        log.error("We had an error: " + errorMsg);
        req.setAttribute("error", errorMsg);
        dispatcher = getServletContext().getRequestDispatcher("/error.jsp");
        try { dispatcher.forward(req, res); }
        catch (Exception e ) { 
            log.error("Impossible to forward to the error page...Don't know what else I can do....", e);
            
        }
        return;
    }
    
    

    

    
    /**
     * return a new color from a color hex string "#000000" eg is black and
     * "#ffffff" is white
     * 
     * @param bgcolor
     *            string representation of a hex color (as typically found in
     *            css)
     * @returns Color indicated by hex bgcolor or white if the color string is
     *          bad.
     */
    private Color calculateColor(String bgcolor) {
        int red, green, blue;
        boolean valid = false;

        // validate color first
        try {
            if (bgcolor != null && bgcolor.matches("#[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]")) {
                log.info("color valid");
                valid = true;
            }
        } catch (PatternSyntaxException e) {
        }

        if (valid == false) {
            return defaultBGColor;
        }

        String tmp;
        tmp = bgcolor.substring(1, 3);
        log.info("tmp = " + tmp);
        red = Integer.valueOf(tmp, 16).intValue();
        log.info("int = " + red);

        tmp = bgcolor.substring(3, 5);
        log.info("tmp = " + tmp);
        green = Integer.valueOf(tmp, 16).intValue();
        log.info("int = " + green);

        tmp = bgcolor.substring(5, 7);
        log.info("tmp = " + tmp);
        blue = Integer.valueOf(tmp, 16).intValue();
        log.info("int = " + blue);

        return new Color(red, green, blue);
    }
    
    
    
    private XYDataset createDataset() {

        TimeSeries s1 = new TimeSeries("L&G European Index Trust", Month.class);
        s1.add(new Month(2, 2001), 181.8);
        s1.add(new Month(3, 2001), 167.3);
        s1.add(new Month(4, 2001), 153.8);
        s1.add(new Month(5, 2001), 167.6);
        s1.add(new Month(6, 2001), 158.8);
        s1.add(new Month(7, 2001), 148.3);
        s1.add(new Month(8, 2001), 153.9);
        s1.add(new Month(9, 2001), 142.7);
        s1.add(new Month(10, 2001), 123.2);
        s1.add(new Month(11, 2001), 131.8);
        s1.add(new Month(12, 2001), 139.6);
        s1.add(new Month(1, 2002), 142.9);
        s1.add(new Month(2, 2002), 138.7);
        s1.add(new Month(3, 2002), 137.3);
        s1.add(new Month(4, 2002), 143.9);
        s1.add(new Month(5, 2002), 139.8);
        s1.add(new Month(6, 2002), 137.0);
        s1.add(new Month(7, 2002), 132.8);

        TimeSeries s2 = new TimeSeries("L&G UK Index Trust", Month.class);
        s2.add(new Month(2, 2001), 129.6);
        s2.add(new Month(3, 2001), 123.2);
        s2.add(new Month(4, 2001), 117.2);
        s2.add(new Month(5, 2001), 124.1);
        s2.add(new Month(6, 2001), 122.6);
        s2.add(new Month(7, 2001), 119.2);
        s2.add(new Month(8, 2001), 116.5);
        s2.add(new Month(9, 2001), 112.7);
        s2.add(new Month(10, 2001), 101.5);
        s2.add(new Month(11, 2001), 106.1);
        s2.add(new Month(12, 2001), 110.3);
        s2.add(new Month(1, 2002), 111.7);
        s2.add(new Month(2, 2002), 111.0);
        s2.add(new Month(3, 2002), 109.6);
        s2.add(new Month(4, 2002), 113.2);
        s2.add(new Month(5, 2002), 111.6);
        s2.add(new Month(6, 2002), 108.8);
        s2.add(new Month(7, 2002), 101.6);

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);
        dataset.addSeries(s2);

        //dataset.setDomainIsPointsInTime(true);

        return dataset;

    }
	   
}


