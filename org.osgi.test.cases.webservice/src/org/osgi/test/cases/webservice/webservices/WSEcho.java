package org.osgi.test.cases.webservice.webservices;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

@WebService
public class WSEcho {
    @WebMethod(operationName = "echoAction", action = "echo")
    @WebResult
    public String echo(@WebParam(name = "textIn") String text) {
    	return text;
    }
}