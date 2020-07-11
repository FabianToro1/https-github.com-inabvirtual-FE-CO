package ec.ingeint.util.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import ec.gob.sri.comprobantes.ws.ObjectFactory;
import ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantes;
import ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante;
import ec.gob.sri.comprobantes.ws.aut.RespuestaLote;

@WebService(name = "AutorizacionComprobantes", targetNamespace = "http://ec.gob.sri.ws.autorizacion")
@XmlSeeAlso({ ObjectFactory.class })
public interface AutorizacionComprobantesOffline extends AutorizacionComprobantes{
	
	@WebMethod
	@WebResult(name = "RespuestaAutorizacionComprobante", targetNamespace = "")
	@RequestWrapper(localName = "autorizacionComprobante", targetNamespace = "http://ec.gob.sri.ws.autorizacion", className = "ec.gob.sri.comprobantes.ws.AutorizacionComprobante")
	@ResponseWrapper(localName = "autorizacionComprobanteResponse", targetNamespace = "http://ec.gob.sri.ws.autorizacion", className = "ec.gob.sri.comprobantes.ws.AutorizacionComprobanteResponse")
	public RespuestaComprobante autorizacionComprobante(
			@WebParam(name = "claveAccesoComprobante", targetNamespace = "") String claveAccesoComprobante);

	@WebMethod
	@WebResult(name = "RespuestaAutorizacionLote", targetNamespace = "")
	@RequestWrapper(localName = "autorizacionComprobanteLote", targetNamespace = "http://ec.gob.sri.ws.autorizacion", className = "ec.gob.sri.comprobantes.ws.AutorizacionComprobanteLote")
	@ResponseWrapper(localName = "autorizacionComprobanteLoteResponse", targetNamespace = "http://ec.gob.sri.ws.autorizacion", className = "ec.gob.sri.comprobantes.ws.AutorizacionComprobanteLoteResponse")
	public RespuestaLote autorizacionComprobanteLote(
			@WebParam(name = "claveAccesoLote", targetNamespace = "") String claveAccesoLote);
	
}
