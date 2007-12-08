<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">


<head>
	<title>Opal Server Deployment Information</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
 	<link href="css/style.css" media="all" rel="stylesheet" type="text/css" /> 
	<script language="javascript" type="text/javascript" >
	</script>
	<script src="js/scripts.js" language="javascript" type="text/javascript" ></script> 
</head>

<% 
   String systemServerHostname = request.getServerName(); 
   String systemIPAddress = (String) request.getAttribute("systemIPAddress"); 
   String systemUptime = (String) request.getAttribute("systemUptime"); 
   String systemBuildDate = (String) request.getAttribute("systemBuildDate"); 
   String opalVersion = (String) request.getAttribute("opalVersion");  
   String dbUrl = (String) request.getAttribute("dbURL");
   String dbUsername = (String) request.getAttribute("dbUsername");
   String dbDriver = (String) request.getAttribute("dbDriver");
   String opalWebsite = (String) request.getAttribute("opalWebsite");
   String opalDocumentation = (String) request.getAttribute("opalDocumentation");
   Boolean drmaa = (Boolean) request.getAttribute("drmaa");
   Boolean globus = (Boolean) request.getAttribute("globus");
   String globusGatekeeper = (String) request.getAttribute("globusGatekeeper");

   String submissionSystem = null;
   if (drmaa == true) {
       submissionSystem = "DRMAA on localscheduler";
   }else if (globus == true ) {
       submissionSystem = "Globus Gatekeeper";
   } else {
       submissionSystem = "Fork on local system";
   }
%>

<body> 
<div class="mainBody">

<!-- [headerInclude] -->
<%@ include file="header.jsp" %>

<!-- [/headerInclude] -->

<!-- BEGIN Body -->
<table border="0" cellpadding="0" cellspacing="0" width="950" class="boxContainer" align="center">
<tr>
<td width="15" height="15" class="boxTopLeft colColor"></td>
<td class="leftCol boxTop colColor">&nbsp;</td>
<td class="boxTop colColor">&nbsp;</td><td class="rightCol boxTop colColor">&nbsp;</td>
<td width="15" class="boxTopRight colColor"></td>
</tr>
<tr>
<td class="boxLeft colColor"><br /></td>
<td colspan="2" class="colColor">

<table width="500">
	<tr>
	 	<td colspan="2" class="leftCol boxBody colColor">
	    	<h2>System Basics</h2>
                </td>
 	</tr>
	<tr>
		<td class="boxBody colColor">
                Hostname
		</td>
		<td class="boxBody colColor">
		<%= systemServerHostname %>
		</td>
	</tr>
	<tr>
		<td class="boxBody colColor">
                IP Address
		</td>
		<td class="boxBody colColor">
		<%= systemIPAddress %>
		</td>
	</tr>
	<tr>
		<td class="boxBody colColor">
                Build Date
		</td>
		<td class="boxBody colColor">
		<%= systemBuildDate %>
		</td>
	</tr>
	<tr>
		<td class="boxBody colColor">
                Uptime
		</td>
		<td class="boxBody colColor">
		<%= systemUptime %>
		</td>
	</tr>
	<tr/>
	<tr>
	 	<td colspan="2" class="leftCol boxBody colColor"><h2>Opal Server Config</h2></td>
 	</tr>
    <tr>
        <td class="boxBody colColor">OPAL Version</td>
        <td class="boxBody colColor"><%= opalVersion %></td>
    </tr>   
 <!--  unsafe to display this infomation  <tr>
        <td class="boxBody colColor">Data base URL:</td>
        <td class="boxBody colColor"></td>
    </tr>
   <tr>
        <td class="boxBody colColor">Data base username:</td>
        <td class="boxBody colColor"></td>
    </tr>   -->
    <tr>
        <td class="boxBody colColor">Data base driver:</td>
        <td class="boxBody colColor"><%= dbDriver %></td>
    </tr>
    

    <tr>
        <td class="boxBody colColor">Submission system:</td>
        <td class="boxBody colColor"><%= submissionSystem %></td>
    </tr>
    <% if ( globus == true) { %>
    <tr>
        <td class="boxBody colColor">Gatekeeper address:</td>
        <td class="boxBody colColor"><%= globusGatekeeper %></td>
    </tr>
    
    
    <% } %>

</table>
</td>

<td class="colColor rightColVertBar">
<div>
<ul>
<li>Documentation</li>

	<ul>
	<li><a href="<%= opalWebsite %>">Opal home</a></li>
	<li><a href="<%= opalDocumentation %>">Installation instructions</a></li>
	</ul>

<li>Collaborators</li>


	<ul>
	<li><a href="http://nbcr.net/">NBCR</a></li>
	</ul>

	<ul>
	<li><a href="http://camera.calit2.net/">CAMERA</a></li>
	</ul>

	<ul>
	<li><a href="http://geongrid.org/">GEON</a></li>
	</ul>

	<ul>
	<li><a href="http://grid-devel.sdsc.edu/">Grid-Devel@SDSC</a></li>
	</ul>

<li>Feedback</li>

	<ul>
	<li><!-- <a href="http://grid-devel.sdsc.edu/gd/node/225">Mailing List</a> --></li>
	</ul>

</ul>
</div>
</td>

<td class="boxRight colColor"><br /></td>
</tr>
<tr>
<td width="15" height="15" class="boxBottomLeft colColor"></td>
<td class="leftCol boxBottom colColor"><br /></td>
<td class="boxBottom colColor"><br /></td><td class="rightCol boxBottom colColor"><br /></td>
<td width="15" class="boxBottomRight colColor"></td>
</tr>

</table>
<!-- END Body -->

<br />

<!-- BEGIN Footer -->

<!-- END Footer -->
</div>
</body>
</html>
