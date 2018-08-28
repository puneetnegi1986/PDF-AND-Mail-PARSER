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
	public static final String ATTACHMENT_PATH = "F:\\emailparserpoc";

	public static void main(String[] args) {

		try {

			String path = extractEML(EMAIL_FILE_PATH);

			generateTxtFromPDF(path);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 * @param filename
	 */
	private static void generateTxtFromPDF(String filename) {

		Scanner file = null;
		try (PDDocument document = PDDocument.load(new File(filename))) {

			document.getClass();
			if (!document.isEncrypted()) {
				PDFTextStripperByArea stripper = new PDFTextStripperByArea();
				stripper.setSortByPosition(true);
				PDFTextStripper tStripper = new PDFTextStripper();
				String pdfFileInText = tStripper.getText(document);
				pdfFileInText = removeBlankLine(pdfFileInText);
				pdfFileInText = cleanTextContent(pdfFileInText);
				System.out.println(pdfFileInText);
				file = new Scanner(new StringReader(pdfFileInText));
				getTotalDue(file);
				getcustomerID(file);
				getBalanceDue(file);
				String[] tableHeaderKeys = new String[] {"INVOICE #", "DATE", "TOTAL DUE", "DUE DATE", "TERMS"};						
				String[] tableDataRegex = new String[] {"\\d{5}-\\d{4}", "\\d{2}/\\d{2}/\\d{4}",
						"\\${1}[0-9,]+\\.\\d{2}", "\\d{2}/\\d{2}/\\d{4}", "[A-Za-z]+[\\s]+\\d+"};
				readTableContent(tableHeaderKeys, "\\s+", pdfFileInText, "\\s+",
						tableDataRegex);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != file) {
				file.close();
			}
		}
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private static String getcustomerID(Scanner file) throws Exception {

		String lineData = null;
		while (file.hasNext()) {
			lineData = file.nextLine().trim();
			if (lineData.contains("CUSTOMER ID") || lineData.contains("CUSTOMERID")) {
				lineData = file.nextLine().trim();
				break;
			}
		}
		System.out.println("Customer ID: " + lineData);
		return lineData;
	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private static String getTotalDue(Scanner file) throws Exception {

		String lineData = null;
		String totalDue = null;
		while (file.hasNext()) {
			lineData = file.nextLine().trim();
			if (lineData.contains("TOTAL DUE") || lineData.contains("TOTALDUE")) {
				lineData = file.nextLine().trim();
				totalDue = parseCurrancy(lineData, " ");
				break;
			}
		}
		System.out.println("TOTAL DUE: " + totalDue);
		return lineData;

	}

	/**
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	private static String getBalanceDue(Scanner file) throws Exception {

		String lineData = null;
		String totalDue = null;
		while (file.hasNext()) {
			lineData = file.nextLine().trim();
			if (lineData.contains("BALANCE DUE") || lineData.contains("BALANCEDUE")) {
				totalDue = parseCurrancy(lineData, " ");
				break;
			}
		}
		System.out.println("BALANCE DUE: " + totalDue);
		return lineData;

	}

	/**
	 * 
	 * @param lineData
	 * @param dataDelimiter
	 * @return
	 */
	private static String parseCurrancy(String lineData, String dataDelimiter) {

		List<String> dataList = Arrays.asList(lineData.split(dataDelimiter));
		String totalDue = null;
		for (String data : dataList) {
			if (data.contains("$")) {
				totalDue = data.replaceAll("[^\\d.]+", "");
			}
		}
		return totalDue;
	}

	/**
	 * 
	 * @param pdfFileInText
	 * @return
	 */
	private static String removeBlankLine(String pdfFileInText) {

		return pdfFileInText.replaceAll("(?m)^[ \t]*\r?\n", "");

	}

	/**
	 * 
	 * @param rawData
	 * @return
	 */
	private static String cleanTextContent(String rawData) {

		rawData = rawData.replaceAll("[^\\x00-\\x7F]", "");

		rawData = rawData.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

		return rawData;
	}

	/**
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static String extractEML(String path) throws Exception {
		try {

			String downloadedFilePath = null;
			Properties props = new Properties();
			Session mailSession = Session.getDefaultInstance(props, null);
			InputStream source = new FileInputStream(path);
			MimeMessage message = new MimeMessage(mailSession, source);
			System.out.println("Subject : " + message.getSubject());
			System.out.println("From : " + message.getFrom()[0]);
			System.out.println("--------------");
			System.out.println("Body : " + message.getContent());
			String contentType = message.getContentType();
			if (contentType.contains("multipart")) {
				System.out.println("Multipart EMail File");
				Multipart multiPart = (Multipart) message.getContent();
				int numberOfParts = multiPart.getCount();
				System.out.println("Parts:::" + numberOfParts);
				for (int partCount = 0; partCount < numberOfParts; partCount++) {
					MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
					if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
						String fileName = part.getFileName();
						String extension = "";
						downloadedFilePath = ATTACHMENT_PATH + File.separator + fileName;
						int i = fileName.lastIndexOf(".");
						if (i > 0) {
							extension = fileName.substring(i + 1);
						}
						if (extension.equalsIgnoreCase("pdf")) {
							new File(downloadedFilePath);
							part.saveFile(downloadedFilePath);
						}
					}
				}
			}
			return downloadedFilePath;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 
	 * @param tableHeaderKeys
	 * @param keyDelimiter
	 * @param pdfFileInText
	 * @param dataDelimiter
	 * @param tableValueRegex
	 * @return
	 * @throws Exception
	 */
	public static Map<String, List<String>> readTableContent(String[] tableHeaderKeys, String keyDelimiter, String pdfFileInText,
			String dataDelimiter, String[] tableValueRegex)throws Exception {

		Map<String, List<String>> tableDataMap = new LinkedHashMap<>();
		String regex = regexCreator(keyDelimiter, tableHeaderKeys);
		Pattern pattern = Pattern.compile(regex);
		try(Scanner file = new Scanner(new StringReader(pdfFileInText))){
		while (file.hasNextLine()) {
			String line = file.nextLine();
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				for (int i = 1; i <= tableHeaderKeys.length; i++) {
					tableDataMap.put(matcher.group(i), new ArrayList<String>());
				}
				readTable(tableDataMap, dataDelimiter, file, tableValueRegex);
			}
		}
	}
		System.out.println("----------------- Table Data ------------------------");
		tableDataMap.entrySet()
				.forEach(entry -> System.out.println(entry.getKey() + "  : " + entry.getValue().toString()));
		return tableDataMap;
	}

	/**
	 * 
	 * @param tableDataMap
	 * @param dataDelimiter
	 * @param file
	 * @param tableValueRegex
	 * @throws Exception
	 */
	public static void readTable(Map<String, List<String>> tableDataMap, String dataDelimiter, Scanner file,
			String[]tableValueRegex)throws Exception {

		String line = file.nextLine();
		String regex = regexCreator(dataDelimiter, tableValueRegex);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			int index = 1;
			for (Entry<String, List<String>> entry : tableDataMap.entrySet()) {
				entry.getValue().add(matcher.group(index));
				index++;
			}
		}
	}

	/**
	 * 
	 * @param delimiter
	 * @param regexList
	 * @return
	 * @throws Exception
	 */
	public static String regexCreator(String delimiter, String... regexList)throws Exception {

		StringBuilder regex = new StringBuilder();
		delimiter = "[" + delimiter + "]";
		for (String tableHeaderKey : regexList) {
			regex.append("(" + tableHeaderKey + ")").append(delimiter);
		}
		regex.delete(regex.lastIndexOf(delimiter), regex.length());

		return regex.toString();
	}
	
	/**
	 * 
	 * @param delimiter
	 * @param regexOfkey
	 * @param regexOfValue
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static Record lineRecordReader(String delimiter,String regexOfkey, String regexOfValue,Scanner file)throws Exception {
		
		Record record=null;
		String line = file.nextLine();
		String regex =regexCreator(delimiter,regexOfkey,regexOfValue);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(line);
		
		if (matcher.find()) {
			record= new Record();
			record.setKey(matcher.group(1));
			record.setValue(matcher.group(2));
		}
		return record;
	}
	
	public static class Record{
		
		String key;
		String value;
		
		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}
		/**
		 * @param key the key to set
		 */
		public void setKey(String key) {
			this.key = key;
		}
		/**
		 * @return the value
		 */
		public String getValue() {
			return value;
		}
		/**
		 * @param value the value to set
		 */
		public void setValue(String value) {
			this.value = value;
		}

	}

}
