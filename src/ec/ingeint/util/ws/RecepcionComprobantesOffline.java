package ec.ingeint.util.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import ec.gob.sri.comprobantes.ws.ObjectFactory;
import ec.gob.sri.comprobantes.ws.RecepcionComprobantes;
import ec.gob.sri.comprobantes.ws.RespuestaSolicitud;

@WebService(name = "RecepcionComprobantesOffline", targetNamespace = "http://ec.gob.sri.ws.recepcion")
@XmlSeeAlso({ ObjectFactory.class })
public interface RecepcionComprobantesOffline extends RecepcionComprobantes{

	@WebMethod
	@WebResult(name = "RespuestaRecepcionComprobante", targetNamespace = "")
	@RequestWrapper(localName = "validarComprobante", targetNamespace = "http://ec.gob.sri.ws.recepcion", className = "ec.gob.sri.comprobantes.ws.ValidarComprobante")
	@ResponseWrapper(localName = "validarComprobanteResponse", targetNamespace = "http://ec.gob.sri.ws.recepcion", className = "ec.gob.sri.comprobantes.ws.ValidarComprobanteResponse")
	public RespuestaSolicitud validarComprobante(
			@WebParam(name = "xml", targetNamespace = "") byte[] xml);
	
}
