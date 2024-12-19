/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
package org.osgi.test.cases.webservice.junit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.osgi.framework.Constants.SERVICE_ID;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.webservice.runtime.WebserviceServiceRuntime;
import org.osgi.service.webservice.runtime.dto.EndpointDTO;
import org.osgi.service.webservice.runtime.dto.RuntimeDTO;
import org.osgi.service.webservice.whiteboard.WebserviceWhiteboardConstants;
import org.osgi.test.cases.webservice.webservices.WSEcho;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.support.map.Maps;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class EndpointRegistrationTests {
	
	/**
	 * A basic test that registers an endpoint as described in 160.1.3
	 * 
	 * @throws Exception
	 */
	@ParameterizedTest
	@ValueSource(classes = {Object.class, WSEcho.class})
	public void testSimpleWebserviceEndpointRegistration(Class<?> registrationType,
			@InjectBundleContext BundleContext ctx,
			@InjectService WebserviceServiceRuntime runtime) throws Exception {

		Hashtable<String, Object> props = new Hashtable<>();
		props.put(WebserviceWhiteboardConstants.WEBSERVICE_ENDPOINT_IMPLEMENTOR, Boolean.TRUE);
		props.put(WebserviceWhiteboardConstants.WEBSERVICE_ENDPOINT_ADDRESS, "http://127.0.0.1/echo");
		
		ServiceRegistration<?> reg = ctx.registerService(registrationType.getName(),
				new WSEcho(), props);
		
		// TODO improve this when it is specced
		boolean found = false;
		for(int i = 0; i < 30; i++) {
			RuntimeDTO dto = runtime.getRuntimeDTO();
			for(EndpointDTO e : dto.endpoints) {
				if(reg.getReference().getProperty(SERVICE_ID).equals(e.implementor.id)) {
					found = true;
					break;
				}
			}
			Thread.sleep(200);
		}
		
		assertTrue(found);
		
		String soapRequest = createSOAPMessage("http://127.0.0.1/echo", "echoAction", Maps.mapOf("textIn", "BANG"));
		
		String soapResponse = getSoapResponse("http://127.0.0.1:8080/echo", soapRequest);
		
		Document result = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(soapResponse)));
		
		Node node = result.getFirstChild();
		assertEquals("Envelope", node.getLocalName());
		
		node = node.getFirstChild();
		assertEquals("Body", node.getLocalName());
	}

	
	public String createSOAPMessage(String namespaceURI, String rootElement, Map<String, Object> childElements) throws Exception {
		
		Document message = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
//		<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:myNamespace="http://www.webserviceX.NET">
//        <SOAP-ENV:Header/>
//        <SOAP-ENV:Body>
//            <myNamespace:GetInfoByCity>
//                <myNamespace:USCity>New York</myNamespace:USCity>
//            </myNamespace:GetInfoByCity>
//        </SOAP-ENV:Body>
//    </SOAP-ENV:Envelope>
		
		
		Element envelope = message.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "soap:Envelope");
		message.appendChild(envelope);
		
		Element body = message.createElement("Body");
		body.setPrefix("soap");
		envelope.appendChild(body);
		
		Element request = message.createElementNS(namespaceURI, "ws:" + rootElement);
		body.appendChild(request);
		
		childElements.entrySet().stream()
			.map(e -> {
				Element element = message.createElement(e.getKey());
				element.setPrefix("ws");
				element.setTextContent(String.valueOf(e.getValue()));
				return element;
			})
			.forEach(request::appendChild);
		
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		StringWriter sw = new StringWriter();
		trans.transform(new DOMSource(message), new StreamResult(sw));
		return sw.toString();
	}
	
	public String getSoapResponse(String uri, String soapMessage) throws Exception {
		URL url = new URL(uri);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
		conn.setRequestMethod("POST");
		conn.addRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
		conn.addRequestProperty("Accept", "application/soap+xml; charset=utf-8");
		byte[] bytes = soapMessage.getBytes(UTF_8);
		conn.addRequestProperty("Content-Length", Integer.toString(bytes.length));
		conn.getOutputStream().write(bytes);
		
		assertEquals(200, conn.getResponseCode());
		
		StringBuilder sb = new StringBuilder();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), UTF_8))) {
			String line = br.readLine();
			while(line != null) {
				sb.append(line).append("\n");
			}
		}
		return sb.toString();
	}
}
