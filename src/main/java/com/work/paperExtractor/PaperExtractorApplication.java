package com.work.paperExtractor;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaperExtractorApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(PaperExtractorApplication.class, args);

		ArrayList<String> doiListdblp = new ArrayList();

		ArrayList<String> qArr = new ArrayList();
		qArr.add("Biological+graph+neural+network");

		PaperExtractorApplication demo = new PaperExtractorApplication();
		// demo.hitSciHub(doiListdblp);
		demo.getFromScholar();

		if (false) {

			for (String qVal : qArr) {
				try {
					URL dblpurl = new URL("https://dblp.org/search/publ/api?q=" + qVal + "&h=1000&format=json");
					HttpURLConnection conn = (HttpURLConnection) dblpurl.openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("Accept", "application/json");
					if (conn.getResponseCode() != 200) {
						throw new RuntimeException("Failed : HTTP Error code : " + conn.getResponseCode());
					}

					String inline = "";
					Scanner scanner = new Scanner(dblpurl.openStream());

					// Write all the JSON data into a string using a scanner
					while (scanner.hasNext()) {
						inline += scanner.nextLine();
					}

					// Close the scanner
					scanner.close();

					// Using the JSON simple library parse the string into a json object

					JSONParser parser = new JSONParser();
					JSONObject data_obj = (JSONObject) parser.parse(inline);

					// Get the required object from the above created object
					JSONObject obj = (JSONObject) data_obj.get("result");

					JSONObject hit = (JSONObject) obj.get("hits");

					JSONArray arr = (JSONArray) hit.get("hit");

					for (int i = 0; i < arr.size(); i++) {

						JSONObject new_obj = (JSONObject) arr.get(i);
						JSONObject info = (JSONObject) new_obj.get("info");

						if (info.get("doi") != null) {
							doiListdblp.add((String) info.get("doi"));
						}

						// System.out.println("title :" + info.get("title"));

					}

				} catch (Exception e) {
					System.out.println("Exception in NetClientGet:- " + e);
				}
			}
		}

	}

	public void getFromScholar() throws InterruptedException {
		int pageNo = 10;
		ArrayList<String> qArr = new ArrayList();
		qArr.add("Biological graph neural network");

		System.setProperty("webdriver.chrome.driver", "chromedriver.exe");

		WebDriver driver = new ChromeDriver();

		List<String> resultsTitle = new ArrayList<>();
		List<String> resultsLink = new ArrayList<>();
		ArrayList<String> doiList = new ArrayList<>();
		ArrayList<String> pdfList = new ArrayList<>();
		List<String> directList = new ArrayList<>();
		PaperExtractorApplication demo = new PaperExtractorApplication();

		for (String qVal : qArr) {

			for (int i = 0; i < 1; i++) {

				driver.get("https://scholar.google.com/scholar?start=" + pageNo + "&q=" + qVal
						+ "&hl=en&as_sdt=0%2C5&as_ylo=2015&as_yhi=2022");

				Thread.sleep(2000);

				List<WebElement> allLinks = driver.findElements(By.tagName("a"));

				for (WebElement link : allLinks) {
					// if(!link.getText().contentEquals("Save") &&
					// !link.getText().contentEquals("Cite")) {

					// System.out.println(link.getText() + " - " + link.getAttribute("href"));
					// }
					if (!link.getText().contentEquals("Save") && !link.getText().contentEquals("Cite")
							&& !link.getAttribute("href").contains("https://scholar.google.com")) {

						if (link.getText().length() > 15) {

							System.out.println(link.getText() + " - " + link.getAttribute("href"));
							resultsLink.add(link.getAttribute("href"));
							resultsTitle.add(link.getText());

						}
					}

				}

				pageNo = pageNo + 10;
			}

		}
		for (String link : resultsLink) {
			if (link.contains("http://doi.org")) {
				doiList.add(link);
			}

			else {
				driver.get(link);

				Thread.sleep(2000);

				List<WebElement> allLinks = driver.findElements(By.tagName("a"));
				for (WebElement weblink : allLinks) {

					if (weblink.getAttribute("href") != null
							&& weblink.getAttribute("href").startsWith("https://doi.org")
							&& !weblink.getText().contains("CrossRef")) {
						System.out.println(weblink.getAttribute("href"));
						doiList.add(weblink.getAttribute("href"));
					} else if (link.startsWith("https://arxiv.org/") && weblink.getAttribute("href") != null
							&& weblink.getText().contains("PDF")) {
						pdfList.add(weblink.getAttribute("href"));
					} else {
						directList.add(link);
					}
				}
			}

		}
		
		driver.quit();

		demo.hitSciHub(doiList);
		demo.getPDFfiles(pdfList);
	}

	public void hitSciHub(ArrayList<String> oldList) throws InterruptedException {

		ArrayList<String> doiList = new ArrayList();

		for (String element : oldList) {

			if (!doiList.contains(element)) {

				doiList.add(element);
			}
		}

		ArrayList<String> noFoundList = new ArrayList();
		System.setProperty("webdriver.chrome.driver", "chromedriver.exe");

		Map<String, Object> prefs = new HashMap<String, Object>();

		// Use File.separator as it will work on any OS
		prefs.put("download.default_directory",
				System.getProperty("user.dir") + File.separator + "externalFiles" + File.separator + "downloadFiles");

		// Adding cpabilities to ChromeOptions
		ChromeOptions options = new ChromeOptions();
		options.setExperimentalOption("prefs", prefs);

		WebDriver driver = new ChromeDriver(options);
		driver.get("https://sci-hub.se/");

		Thread.sleep(2000);

		for (String url : doiList) {

			WebElement searchBox = driver.findElement(By.id("request"));
			searchBox.clear();
			searchBox.sendKeys(url);
			searchBox.submit();
			Thread.sleep(2000);
			try {
				WebElement pdf = driver.findElement(By.id("pdf"));
				WebElement buttons = driver.findElement(By.id("buttons"));
				if (buttons.isDisplayed()) {
					List<WebElement> c = buttons.findElements(By.xpath("./child::*"));
					// iterate child nodes
					for (WebElement i : c) {
						i.click();
						// getText() to get text for child nodes
						System.out.println(i.getText());
					}

				}
			} catch (Exception e) {
				noFoundList.add(url);
			}
			driver.navigate().back();

//			System.out.println(pdf.getText());

		}

		for (String notF : noFoundList) {
			System.out.println("Not found in Sci Hub:" + notF);
		}
		
		driver.quit();

	}

	public void getPDFfiles(ArrayList<String> pdfList) {

		ArrayList<String> doiList = new ArrayList();

		for (String element : pdfList) {

			if (!doiList.contains(element)) {

				doiList.add(element);
			}
		}

		ArrayList<String> noFoundList = new ArrayList();
		System.setProperty("webdriver.chrome.driver", "chromedriver.exe");

		Map<String, Object> prefs = new HashMap<String, Object>();

		// Use File.separator as it will work on any OS
		prefs.put("download.default_directory",
				System.getProperty("user.dir") + File.separator + "externalFiles" + File.separator + "downloadFiles");
		prefs.put("plugins.always_open_pdf_externally", true);
		prefs.put("download.prompt_for_download", false);
		

		// Adding cpabilities to ChromeOptions
		ChromeOptions options = new ChromeOptions();
		options.setExperimentalOption("prefs", prefs);

		WebDriver driver = new ChromeDriver(options);

		for (String uri : doiList) {

			try {
				driver.get(uri);

				Thread.sleep(2000);

//				WebElement pdf = driver.findElement(By.id("download"));
//				WebElement buttons = driver.findElement(By.id("buttons"));
//				if (buttons.isDisplayed()) {
//					List<WebElement> c = buttons.findElements(By.xpath("./child::*"));
//					// iterate child nodes
//					for (WebElement i : c) {
//						i.click();
//						// getText() to get text for child nodes
//						System.out.println(i.getText());
//					}
//
//				}
			} catch (Exception e) {
				noFoundList.add(uri);
			}

//			System.out.println(pdf.getText());
		}

		for (

		String notF : noFoundList) {
			System.out.println("Not found in Sci Hub:" + notF);
		}
		driver.quit();

	}
}
