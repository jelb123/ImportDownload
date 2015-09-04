package au.edu.unsw.sltf.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.annotation.Resource;
import javax.jws.WebService;

@WebService(endpointInterface = "au.edu.unsw.sltf.services.ImportDownloadServicesPortType")
public class ImportDownloadServicesPortTypeImpl implements ImportDownloadServicesPortType {
	
	private ObjectFactory objectFactory = new ObjectFactory();
	
	@Resource    
	private WebServiceContext wsCtxt;
	
	@Override
	public String downloadFile(String eventSetId, String fileType)
			throws ImportDownloadFault_Exception {
		
	    MessageContext msgCtxt = wsCtxt.getMessageContext();
	    HttpServletRequest request =
	        (HttpServletRequest)msgCtxt.get(MessageContext.SERVLET_REQUEST);

	    String hostName = request .getServerName();
	    int port = request .getServerPort();
	    
	    System.out.println(hostName + port);
		
		fileType = fileType.toLowerCase();
		File file = new File(System.getProperty("catalina.home") + File.separator + "webapps" + File.separator + "ROOT" + File.separator + "filteredFiles" + File.separator + eventSetId + "." + fileType);
		
		if (!file.exists()) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "The input eventSetId does not exist in the server";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("InvalidEventSetId"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
			
		} else if (!(fileType.equals("csv") || fileType.equals("xml") || fileType.equals("html"))) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "The input fileType must be either CSV, XML or HTML";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("InvalidFileType"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
		}
		
		return "http://" + hostName + ":" + port + "/filteredFiles/" + file.getName();
	}

	
	// TODO DO THE SERVICE FAULTS STUFF
	
	@Override
	public String importMarketData(String sec, String startDate,
			String endDate, String dataSourceURL)
			throws ImportDownloadFault_Exception {
		
		if (!sec.matches("^[A-Z]{3,4}$")) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "The input SEC must only consist of 3-4 upper case letters";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("InvalidSECCode"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
		}
		
		Date newStartDate = null;
		Date newEndDate = null;
		
		try {
			newStartDate = new SimpleDateFormat("dd-MM-yyyy").parse(startDate);
			newEndDate = new SimpleDateFormat("dd-MM-yyyy").parse(endDate);
		} catch (ParseException e) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "The format of one of the input dates is incorrect. Must be dd-MM-yyyy";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("InvalidDates"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
		}
		
		if (newStartDate.after(newEndDate)) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "The start date must be before the end date";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("InvalidDates"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
		}
		
		if (dataSourceURL.matches("^http://.+\\.csv$")) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "The input url is in the wrong format";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("InvalidURL"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
			
		}
						
		InputStreamReader csvStream = null;
		BufferedReader csvFile = null;
		BufferedWriter bw = null;
		
		Date today = new Date();
		String fileName = sec + "-" + new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss").format(today).toString();
		
		boolean hasEntry = false;
		try {
			URL csvURL = new URL(dataSourceURL);
			URLConnection urlConn = csvURL.openConnection();
			csvStream = new InputStreamReader(urlConn.getInputStream());
			csvFile = new BufferedReader(csvStream);
			
			File file = new File(System.getProperty("catalina.home") + File.separator + "webapps" + File.separator + "ROOT" + File.separator + "filteredFiles" + File.separator + fileName + ".csv");
			file.getParentFile().mkdirs();
			
			String[] fullName = file.getName().split("\\.");
			fileName = fullName[0];
			
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			bw = new BufferedWriter(fw);
			
			
			String commentLine = csvFile.readLine();
			bw.write(commentLine + "\n");
			
			String line = csvFile.readLine();
			String[] marketData;

			while (line != null) {
				marketData = line.split(",");
				Date dataDate = new SimpleDateFormat("dd-MMM-yyyy").parse(marketData[1]);
				if(marketData[0].equals(sec) && newStartDate.before(dataDate) && newEndDate.after(dataDate)) {
					bw.write(line + "\n");
					hasEntry = true;
				}
				line = csvFile.readLine();
			}
			 
		} catch (MalformedURLException e) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "The input URL is not reachable";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("InvalidURL"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
		} catch (IOException e) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "The file cannot be read";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("ProgramError"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
		} catch (ParseException e) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "A date within the file is in the wrong format";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("ProgramError"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
		} catch (Exception e) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "Unkown Exception";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("ProgramError"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
		} finally {
			try {
				csvStream.close();
			} catch (IOException e) {
				ImportDownloadFault fault = objectFactory.createImportDownloadFault();
				
				String message = "The file cant be read";
				
				fault.setFaultType(ImportDownloadFaultType.fromValue("ProgramError"));
				fault.setFaultMessage(message);
				
				throw new ImportDownloadFault_Exception(message, fault);
			}
			try {
				csvFile.close();
			} catch (IOException e) {
				ImportDownloadFault fault = objectFactory.createImportDownloadFault();
				
				String message = "The file cant be read";
				
				fault.setFaultType(ImportDownloadFaultType.fromValue("ProgramError"));
				fault.setFaultMessage(message);
				
				throw new ImportDownloadFault_Exception(message, fault);
			}
			try {
				bw.close();
			} catch (IOException e) {
				ImportDownloadFault fault = objectFactory.createImportDownloadFault();
				
				String message = "The written file wont close";
				
				fault.setFaultType(ImportDownloadFaultType.fromValue("ProgramError"));
				fault.setFaultMessage(message);
				
				throw new ImportDownloadFault_Exception(message, fault);
			}
		}
		
		if (hasEntry == false) {
			ImportDownloadFault fault = objectFactory.createImportDownloadFault();
			
			String message = "The input SEC code and Date combination are not in the file";
			
			fault.setFaultType(ImportDownloadFaultType.fromValue("InvalidSECCode"));
			fault.setFaultMessage(message);
			
			throw new ImportDownloadFault_Exception(message, fault);
		}
		
		return fileName;
	}

}
