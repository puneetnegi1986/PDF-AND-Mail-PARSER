package com.pro;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

public class TextExtractor {
	
   public static final String EMAIL_FILE_PATH = "F:\\cvbqn19bu51s5ptqiddk1e2p9qnvf8qucoird201";
   public static final String ATTACHMENT_PATH="F:\\emailparserpoc";
   
	public static void main(String[] args) {
		
	    try{

	        String path =  extractEML(EMAIL_FILE_PATH);

	        generateTxtFromPDF(path);

	    }catch(Exception ex){
	        ex.printStackTrace();
	    }
	}

	private static void generateTxtFromPDF(String filename) {
		
		   Scanner file=null;
	       try (PDDocument document = PDDocument.load(new File(filename))) {

	            document.getClass();
	            if (!document.isEncrypted()) {				
	                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
	                stripper.setSortByPosition(true);
	                PDFTextStripper tStripper = new PDFTextStripper();
	                String pdfFileInText = tStripper.getText(document);
	                removeBlankLine(pdfFileInText);
	                cleanTextContent(pdfFileInText);
	                System.out.println(pdfFileInText);
	                 file= new Scanner(new StringReader(pdfFileInText));	                  
	                    getTotalDue(file);
	                    getcustomerID(file);
	                    getBalanceDue(file);
	                    readTableContent(new ArrayList<String>(), "\\s+",new Scanner(new StringReader(pdfFileInText)));
	            }

	        }catch(Exception e) {
	        	e.printStackTrace();
	        }finally {
	        	if(null!=file) {
	        		file.close();
	        	}
	        }
	}	
	
	private static String getcustomerID(Scanner  file) throws Exception {
		
		String lineData=null;
		while (file.hasNext()) {
			lineData = file.nextLine().trim();
			if (lineData.contains("CUSTOMER ID") || lineData.contains("CUSTOMERID")) {				
				lineData = file.nextLine().trim();
				break;
			}
		}
		System.out.println("Customer ID: "+lineData);
		return lineData;
	}
    
    private static String getTotalDue(Scanner file) throws Exception{ 
    	
    	String lineData=null;
    	String totalDue=null;
		while (file.hasNext()) {			
			lineData = file.nextLine().trim();
			if (lineData.contains("TOTAL DUE") || lineData.contains("TOTALDUE")) {
				lineData = file.nextLine().trim();	
						totalDue=parseCurrancy(lineData," ");
				break;
			}
		}
		System.out.println("TOTAL DUE: " + totalDue);
        return lineData;
        
    } 
    
    private static String getBalanceDue(Scanner file) throws Exception{ 
    	
    	String lineData=null;
    	String totalDue=null;
		while (file.hasNext()) {			
			lineData=file.nextLine().trim();
			if (lineData.contains("BALANCE DUE") || lineData.contains("BALANCEDUE")) {
				totalDue=parseCurrancy(lineData," ");
				break;
			}
		}
		System.out.println("BALANCE DUE: " + totalDue);
        return lineData;
        
    } 
    
    private static String parseCurrancy(String lineData, String dataDelimiter) {
    	
    	List<String> dataList=Arrays.asList(lineData.split(dataDelimiter));
        String  totalDue=null;
    	for(String data:dataList) {    		
    		if(data.contains("$")) {
    			totalDue = data.replaceAll("[^\\d.]+", "");
    		}
    	}
    	return totalDue;
    }
    
    private static String removeBlankLine(String pdfFileInText) {
    	
    	return pdfFileInText.replaceAll("(?m)^[ \t]*\r?\n", "");
    	
    }
    
    private static String cleanTextContent(String rawData){
        
    	rawData = rawData.replaceAll("[^\\x00-\\x7F]", "");
       
        rawData = rawData.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
                 
        return rawData;
    }
    
    
    public static String extractEML(String path) throws Exception{
        try {
            
        	String downloadedFilePath=null;
            Properties props = new Properties();
            Session mailSession = Session.getDefaultInstance(props, null);
            InputStream source = new FileInputStream(path);
            MimeMessage message = new MimeMessage(mailSession, source);
            System.out.println("Subject : " + message.getSubject());
            System.out.println("From : " + message.getFrom()[0]);
            System.out.println("--------------");
            System.out.println("Body : " +  message.getContent());
            String contentType = message.getContentType();
            if (contentType.contains("multipart")) {
                System.out.println("Multipart EMail File");
                Multipart multiPart = (Multipart) message.getContent();
                int numberOfParts = multiPart.getCount();
                System.out.println("Parts:::"+numberOfParts);
                for (int partCount = 0; partCount < numberOfParts; partCount++) {
                    MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                        String fileName = part.getFileName();
                        String extension="";
                        downloadedFilePath=ATTACHMENT_PATH+ File.separator + fileName;
                        int i=fileName.lastIndexOf(".");
                        if(i>0)
                        {
                            extension=fileName.substring(i+1);
                        }
                        if(extension.equalsIgnoreCase("pdf"))
                        {
                        	new File(downloadedFilePath);
                            part.saveFile(downloadedFilePath);
                        }
                    }
                }
            }
            return downloadedFilePath;
        }catch (Exception e){
            return null;
        }
    }
    
	public static void readTableContent(List<String> tableHeaderKeys, String delimiter, Scanner file) {

		tableHeaderKeys = Stream.of("INVOICE #", "DATE", "TOTAL DUE", "DUE DATE", "TERMS").collect(Collectors.toList());
		StringBuilder regex = new StringBuilder();
		delimiter = "[" + delimiter + "]";
		Map<String, List<String>> tableDataMap = new LinkedHashMap<>();

		for (String tableHeaderKey : tableHeaderKeys) {
			regex.append("(" + tableHeaderKey + ")").append(delimiter);
		}
		regex.delete(regex.lastIndexOf(delimiter), regex.length());
		System.out.println(regex.toString());
		Pattern pattern = Pattern.compile(regex.toString());
		while (file.hasNextLine()) {
			String line = file.nextLine();
			Matcher matcher = pattern.matcher(line);
			while (matcher.find()) {
				System.out.println("group 1: " + matcher.group(1));
				tableDataMap.put(matcher.group(1), new ArrayList<String>());
				System.out.println("group 2: " + matcher.group(2));
				tableDataMap.put(matcher.group(2), new ArrayList<String>());
				System.out.println("group 3: " + matcher.group(3));
				tableDataMap.put(matcher.group(2), new ArrayList<String>());
				System.out.println("group 4: " + matcher.group(4));
				tableDataMap.put(matcher.group(4), new ArrayList<String>());
				System.out.println("group 5: " + matcher.group(5));
				tableDataMap.put(matcher.group(5), new ArrayList<String>());
				readTable(tableDataMap,delimiter, file);
			}
		}
		tableDataMap.entrySet().forEach(entry->System.out.println(entry.getKey()+"  : "+entry.getValue().toString()));
	}
    
    public static void readTable(Map<String,List<String>> tableDataMap,String delimiter, Scanner file)  {
    	
    	String line=file.nextLine();
    	List<String> dataList=Arrays.asList(line.split(" "));
    	int index=0;
         for(Entry<String,List<String>> entry:tableDataMap.entrySet()) {
        	 entry.getValue().add(dataList.get(index));
        	 index++;
    	}	  	
    }

}
