import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.HibernateException;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

import edu.sdsc.nbcr.opal.*;
import edu.sdsc.nbcr.opal.gui.common.*;

import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.client.Call;
import org.apache.axis.client.AxisClient;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.description.TypeDesc;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.MessageContext;


public class RegistryClient {
    public static void main(String[] args) {
	String reg_xml = "http://webbybacon.ucsd.edu/registry/registry.xml";
	Host [] hosts = getHosts(reg_xml);

	for (int i = 0; i < hosts.length; i++) {
	    System.out.println("SERVER: " + hosts[i].getName());
	    System.out.println("SERVER: " + hosts[i].getNumCpuTotal());
	}
    }

    public static Host [] getHosts(String url) {
	Stack hs = new Stack();

	try {
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document doc = db.parse(url);
	    doc.getDocumentElement().normalize();

	    NodeList nodeList = doc.getElementsByTagName("host");

	    for (int i = 0; i < nodeList.getLength(); i++) {
		Host h = new Host();
		Node fstNode = nodeList.item(i);

		if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
		    Element fstElmnt = (Element) fstNode;
		    NodeList nameElmntLst = fstElmnt.getElementsByTagName("name");
		    Element nameElmnt = (Element) nameElmntLst.item(0);
		    NodeList name = nameElmnt.getChildNodes();
		    h.setName(((Node) name.item(0)).getNodeValue());

		    NodeList numCpuTotalElmntLst = fstElmnt.getElementsByTagName("numCpuTotal");
		    Element numCpuTotalElmnt = (Element) numCpuTotalElmntLst.item(0);
		    NodeList numCpuTotal = numCpuTotalElmnt.getChildNodes();
		    h.setNumCpuTotal(Integer.parseInt(((Node) numCpuTotal.item(0)).getNodeValue()));

		    NodeList numCpuFreeElmntLst = fstElmnt.getElementsByTagName("numCpuFree");
		    Element numCpuFreeElmnt = (Element) numCpuFreeElmntLst.item(0);
		    NodeList numCpuFree = numCpuFreeElmnt.getChildNodes();
		    h.setNumCpuFree(Integer.parseInt(((Node) numCpuFree.item(0)).getNodeValue()));

		    NodeList numJobsRunningElmntLst = fstElmnt.getElementsByTagName("numJobsRunning");
		    Element numJobsRunningElmnt = (Element) numJobsRunningElmntLst.item(0);
		    NodeList numJobsRunning = numJobsRunningElmnt.getChildNodes();
		    h.setNumJobsRunning(Integer.parseInt(((Node) numJobsRunning.item(0)).getNodeValue()));

		    NodeList numJobsQueuedElmntLst = fstElmnt.getElementsByTagName("numJobsQueued");
		    Element numJobsQueuedElmnt = (Element) numJobsQueuedElmntLst.item(0);
		    NodeList numJobsQueued = numJobsQueuedElmnt.getChildNodes();
		    h.setNumJobsQueued(Integer.parseInt(((Node) numJobsQueued.item(0)).getNodeValue()));

		    NodeList servicesElmntLst = fstElmnt.getElementsByTagName("services");
		    Element servicesElmnt = (Element) servicesElmntLst.item(0);
		    NodeList services = servicesElmnt.getChildNodes();
		    h.setServices(((Node) services.item(0)).getNodeValue());

		    hs.push(h);
		}
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}

	Host [] hosts = new Host[hs.size()];

	Iterator it = hs.iterator();
	int i = 0;
	
	while (it.hasNext()) {
	    hosts[i] = (Host)it.next();
	    i++;
	}

	return hosts;
    }

}