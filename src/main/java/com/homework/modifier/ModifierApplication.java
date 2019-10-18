package com.homework.modifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SpringBootApplication
public class ModifierApplication {

	public static final File xmlInputFilePath = new File(ModifierApplication.class.getResource("inventory").getFile());
	public static final File xmlOutputFilePath = new File(ModifierApplication.class.getResource("inventorymod").getFile());

	private static Map<String, String> resultMap;

	public static void main(String[] args) {
		SpringApplication.run(ModifierApplication.class, args);
		try {
			resultMap = jsonNodeParser();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		xmlParser();
	}

	//Method to parse JSON file using Jackson API and retrieve vin and trim values
	public static Map<String, String> jsonNodeParser() throws IOException {


		JsonNode node = new ObjectMapper().readTree(new File(ModifierApplication.class.getResource("test123").getFile()));
		JsonNode vehicles = node.get("vehicles");

		Iterator<Map.Entry<String, JsonNode>> itr = vehicles.fields();
		Map<String, String> resultMap = new HashMap<>();

		while(itr.hasNext()) {
			Map.Entry<String, JsonNode> elt = itr.next();
			JsonNode value = elt.getValue();
			String vin = value.get("vin").textValue();
			String trim = value.get("trim").textValue();
			resultMap.put(vin, trim);
		}
		System.out.println(resultMap);
		return resultMap;
	}

	//Method to parse XML file using data from JSON by building a DOM, traversing the tree, inserting override trim and outputting the updated XML file
	public static void xmlParser() {

		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(xmlInputFilePath);

			Node data = document.getElementsByTagName("data").item(0);
			NodeList nodeList = data.getChildNodes();

			for (int i = 0; i < nodeList.getLength(); i++) {
				Node element = nodeList.item(i);
				if ("vehicleSaveRequest".equals(element.getNodeName())) {
					NodeList nextNodeList = element.getChildNodes();
					for (int j = 0; j < nextNodeList.getLength(); j++) {
						Node nextElement = nextNodeList.item(j);
						if ("vin".equals(nextElement.getNodeName())) {
							for (String key : resultMap.keySet()) {
								if (nextElement.getTextContent().equals(key)) {
									for (Node node = nextElement.getNextSibling(); node != null; node = node.getNextSibling()) {
										if ("trim".equals(node.getNodeName())) {
											Text overrideNode = document.createTextNode(resultMap.get(key));
											Element overrideElement = document.createElement("override_trim");
											overrideElement.appendChild(overrideNode);
											element.insertBefore(overrideElement, node.getNextSibling());
									}
								}
							}
						}
					}
				}
			}
		}

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			DOMSource domSource = new DOMSource(document);
			StreamResult streamResult = new StreamResult(xmlOutputFilePath);

			//Transform the data
			transformer.transform(domSource, streamResult);

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (SAXException sae) {
			sae.printStackTrace();
		}
	}
}
