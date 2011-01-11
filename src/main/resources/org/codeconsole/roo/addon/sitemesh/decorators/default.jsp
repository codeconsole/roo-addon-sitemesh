<%@ taglib uri="http://www.opensymphony.com/sitemesh/decorator" prefix="decorator" %>
<%@ taglib prefix="util" tagdir="/WEB-INF/tags/util" %>
<html>
    <head>
    	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title><decorator:title default="Spring ROO" /></title>
        <decorator:head />
        <util:load-scripts />
    </head>
  	<body class="tundra spring">
   		<div id="wrapper">
		    <%@ include file="/WEB-INF/views/header.jspx" %>
		    <%@ include file="/WEB-INF/views/menu.jspx" %>  
		    <div id="main">
	    		<decorator:body />
		    	<%@ include file="/WEB-INF/views/footer.jspx" %>
		    </div>
		</div>
	</body>
</html>