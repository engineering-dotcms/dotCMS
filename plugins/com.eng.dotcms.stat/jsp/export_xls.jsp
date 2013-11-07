<%@page import="org.apache.poi.ss.util.CellRangeAddress"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.apache.poi.hssf.usermodel.HSSFCellStyle"%>
<%@page import="com.eng.dotcms.stat.bean.SectionReport"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.eng.dotcms.stat.bean.SectionSearchBean"%>
<%@page import="com.eng.dotcms.stat.ClickstreamStatisticsAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.plugin.business.PluginAPI"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@ include file="/html/plugins/com.eng.dotcms.stat/init.jsp" %>
<%@page import="java.io.FileOutputStream"%>
<%@page import="org.apache.poi.hssf.usermodel.HSSFCell"%>
<%@page import="org.apache.poi.hssf.usermodel.HSSFRow"%>
<%@page import="org.apache.poi.hssf.usermodel.HSSFSheet"%>
<%@page import="org.apache.poi.ss.usermodel.CreationHelper"%>
<%@page import="org.apache.poi.hssf.usermodel.HSSFWorkbook"%>
<%@page import="java.io.PrintWriter"%>
<%@ page trimDirectiveWhitespaces="true" %>

<%
String nastyError = "";
PluginAPI pAPI = APILocator.getPluginAPI();
String pluginId = "com.eng.dotcms.stat";

ClickstreamStatisticsAPI cssAPI = ClickstreamStatisticsAPI.getInstance();
SectionSearchBean search = new SectionSearchBean();


//Get parameter from request
String section = request.getParameter("section");
String year = request.getParameter("year");
String monthFrom = request.getParameter("monthFrom");
String monthTo = request.getParameter("monthTo");

//Validation
if(!UtilMethods.isSet(section))
	nastyError=LanguageUtil.get(pageContext, "search-stat-page-required");
else
	search.setSection(section);

try {
	if(UtilMethods.isSet(year))
		search.setYear(Integer.parseInt(year));
	else
		throw new Exception();
	
	if(UtilMethods.isSet(monthFrom))
		search.setMonthFrom(Integer.parseInt(monthFrom));
	else
		throw new Exception();
	
	if(UtilMethods.isSet(monthTo))
		search.setMonthTo(Integer.parseInt(monthTo));
	else
		throw new Exception();
} catch(Exception e) {
	nastyError = LanguageUtil.get(pageContext, "search-stat-numeric-validation");
}

List<SectionReport> reports = cssAPI.findSectionReport(search);

PrintWriter pr = null;
HSSFWorkbook wb  = new HSSFWorkbook();

CreationHelper createHelper = wb.getCreationHelper();
HSSFCellStyle cellStyle = wb.createCellStyle();
cellStyle.setDataFormat(
    createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

HSSFSheet sheet = wb.createSheet("section_data_export");

// Create a row and put some cells in it. Rows are 0 based.
HSSFRow row = sheet.createRow((short)0);

row.createCell(0).setCellValue(
     createHelper.createRichTextString("Data"));
row.createCell(1).setCellValue(
	     createHelper.createRichTextString("Pagina"));
row.createCell(2).setCellValue(
	     createHelper.createRichTextString("Visite"));

int counter = 1;
int hintsCounter = 0;
Date firstDate = reports.get(0).getDate();
for(SectionReport report: reports) {
	HSSFRow rowTemp = sheet.createRow((short)counter);
	
	HSSFCell cell = rowTemp.createCell(0);
	cell.setCellValue(report.getDate());
	cell.setCellStyle(cellStyle);
	
	rowTemp.createCell(1).setCellValue(report.getPage());
	
	rowTemp.createCell(2).setCellValue(report.getHints());
	
	if(!firstDate.equals(report.getDate())) {
		firstDate = report.getDate();
		counter++;
		HSSFRow rowTot = sheet.createRow((short)counter);
		rowTot.createCell(0).setCellValue("Totale "+hintsCounter);
		
		sheet.addMergedRegion(new CellRangeAddress(
	            counter, //first row (0-based)
	            counter, //last row  (0-based)
	            0, //first column (0-based)
	            2  //last column  (0-based)
	    ));
		
		hintsCounter = 0;
	} else {
		hintsCounter+=report.getHints();
	}
	
	counter++;
}


HSSFRow rowTot = sheet.createRow((short)counter);
rowTot.createCell(0).setCellValue("Totale "+hintsCounter);

sheet.addMergedRegion(new CellRangeAddress(
        counter, //first row (0-based)
        counter, //last row  (0-based)
        0, //first column (0-based)
        2  //last column  (0-based)
));


// Write the output to a file
response.setContentType("application/octet-stream; charset=UTF-8");
response.setHeader("Content-Disposition", "attachment; filename=section_data_export.xls");
wb.write(response.getOutputStream());
%> 