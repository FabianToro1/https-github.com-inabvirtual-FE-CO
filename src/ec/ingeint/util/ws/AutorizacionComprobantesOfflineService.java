package ec.ingeint.util.ws;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


@WebServiceClient(name = "AutorizacionComprobantesOfflineService", targetNamespace = "http://ec.gob.sri.ws.autorizacion", wsdlLocation = "")
public class AutorizacionComprobantesOfflineService extends Service{
	private static URL __getWsdlLocation() {
		if (AUTORIZACIONCOMPROBANTESSERVICE_EXCEPTION != null) {
			throw AUTORIZACIONCOMPROBANTESSERVICE_EXCEPTION;
		}
		return AUTORIZACIONCOMPROBANTESSERVICE_WSDL_LOCATION;
	}

	private final static URL AUTORIZACIONCOMPROBANTESSERVICE_WSDL_LOCATION;
	private final static WebServiceException AUTORIZACIONCOMPROBANTESSERVICE_EXCEPTION;

	private final static QName AUTORIZACIONCOMPROBANTESSERVICE_QNAME = new QName(
			"http://ec.gob.sri.ws.autorizacion",
			"AutorizacionComprobantesOfflineService");

	static {
		URL url = null;
		WebServiceException e = null;
		try {
			url = new URL(
					"https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl");
		} catch (MalformedURLException ex) {
			e = new WebServiceException(ex);
		}
		AUTORIZACIONCOMPROBANTESSERVICE_WSDL_LOCATION = url;
		AUTORIZACIONCOMPROBANTESSERVICE_EXCEPTION = e;
	}

	public AutorizacionComprobantesOfflineService() {
		super(__getWsdlLocation(), AUTORIZACIONCOMPROBANTESSERVICE_QNAME);
	}

	public AutorizacionComprobantesOfflineService(URL wsdlLocation) {
		super(wsdlLocation, AUTORIZACIONCOMPROBANTESSERVICE_QNAME);
	}

	public AutorizacionComprobantesOfflineService(URL wsdlLocation, QName serviceName) {
		super(wsdlLocation, serviceName);
	}

	public AutorizacionComprobantesOfflineService(URL wsdlLocation, QName serviceName,
			WebServiceFeature... features) {
		super(wsdlLocation, serviceName, features);
	}

	public AutorizacionComprobantesOfflineService(URL wsdlLocation,
			WebServiceFeature... features) {
		super(wsdlLocation, AUTORIZACIONCOMPROBANTESSERVICE_QNAME, features);
	}

	public AutorizacionComprobantesOfflineService(WebServiceFeature... features) {
		super(__getWsdlLocation(), AUTORIZACIONCOMPROBANTESSERVICE_QNAME,
				features);
	}

	@WebEndpoint(name = "AutorizacionComprobantesPort")
	public AutorizacionComprobantesOffline getAutorizacionComprobantesPort() {
		return super
				.getPort(new QName("http://ec.gob.sri.ws.autorizacion",
						"AutorizacionComprobantesOfflinePort"),
						AutorizacionComprobantesOffline.class);
	}

	@WebEndpoint(name = "AutorizacionComprobantesOfflinePort")
	public AutorizacionComprobantesOffline getAutorizacionComprobantesPort(
			WebServiceFeature... features) {
		return super.getPort(new QName("http://ec.gob.sri.ws.autorizacion",
				"AutorizacionComprobantesOfflinePort"),
				AutorizacionComprobantesOffline.class, features);
	}

}
