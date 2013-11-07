<%@page import="com.dotmarketing.util.WebKeys"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Test visualizzazione Termini</title>
</head>
<body>
	<div style="float: left; width: 28%; border: 1px solid black;">
		<jsp:include page="../terms/most_researched_terms.jsp">
			<jsp:param value="/pippo/pluto/index.html" name="URL" />
			<jsp:param value="term" name="searchParam" />
			<jsp:param value="1" name="com.dotmarketing.htmlpage.language" />
			<jsp:param value="10" name="limit" />
		</jsp:include>
	</div>	
</body>
</html>