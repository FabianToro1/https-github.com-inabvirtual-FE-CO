package org.globalqss.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FileUtils;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.globalqss.model.GenericXMLSignature;
import org.globalqss.model.X_SRI_AccessCode;
import org.globalqss.model.X_SRI_Authorization;

import ec.gob.sri.comprobantes.ws.Comprobante;
// import ec.gob.sri.comprobantes.ws.Comprobante.Mensajes;	// Collides
// import ec.gob.sri.comprobantes.ws.Mensaje;	// Collides
import ec.gob.sri.comprobantes.ws.RecepcionComprobantes;
import ec.gob.sri.comprobantes.ws.RecepcionComprobantesService;
import ec.gob.sri.comprobantes.ws.RespuestaSolicitud;
import ec.gob.sri.comprobantes.ws.RespuestaSolicitud.Comprobantes;
import ec.gob.sri.comprobantes.ws.aut.Autorizacion;
import ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantes;
import ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesService;
import ec.gob.sri.comprobantes.ws.aut.Mensaje;
import ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante;
import ec.gob.sri.comprobantes.ws.aut.Autorizacion.Mensajes;
import ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante.Autorizaciones;
import ec.ingeint.util.ws.AutorizacionComprobantesOfflineService;
import ec.ingeint.util.ws.RecepcionComprobantesOfflineService;

import org.w3c.dom.Document;

import es.mityc.firmaJava.libreria.ConstantesXADES;
import es.mityc.firmaJava.libreria.xades.DataToSign;
import es.mityc.firmaJava.libreria.xades.XAdESSchemas;
import es.mityc.javasign.EnumFormatoFirma;
import es.mityc.javasign.xml.refs.AllXMLToSign;
import es.mityc.javasign.xml.refs.InternObjectToSign;
import es.mityc.javasign.xml.refs.ObjectToSign;


/**
 *	Utils for LEC FE Xml
 *
 *  @author Jesus Garcia - globalqss - Quality Systems & Solutions - http://globalqss.com
 *	@version $Id: LEC_FE_UtilsXml.java,v 1.0 2013/05/27 23:01:26 cruiz Exp $
 */

public class LEC_FE_UtilsXml extends GenericXMLSignature
{
	
	String m_responseReception;
	String m_responseAutorization;
	String m_responseReceptionClass;
	String m_responseAutorizationClass;

	public LEC_FE_UtilsXml() {
		
		setOnTesting(MSysConfig.getBooleanValue("QSSLEC_FE_EnPruebas", false, Env.getAD_Client_ID(Env.getCtx())));
		
		m_responseReceptionClass=MSysConfig.getValue("QSSLEC_FE_ResponseReceptionClass", "", Env.getAD_Client_ID(Env.getCtx()));
		m_responseAutorizationClass=MSysConfig.getValue("QSSLEC_FE_ResponseAutorizationClass", "", Env.getAD_Client_ID(Env.getCtx()));

		m_responseReception=m_responseReceptionClass.substring(m_responseReceptionClass.lastIndexOf(".")+1, m_responseReceptionClass.length())+serviceWord;
		m_responseAutorization=m_responseAutorizationClass.substring(m_responseAutorizationClass.lastIndexOf(".")+1, m_responseAutorizationClass.length())+serviceWord;
				
        if (m_responseReceptionClass == null || m_responseAutorizationClass == null) {
        	String msg = "No existe la configuración para las variables \"QSSLEC_FE_ResponseReceptionClass\" y/o \"QSSLEC_FE_ResponseAutorizationClass\"";
        	System.out.println(msg);
        	throw new AdempiereException(msg);
		}

		setEnvType(ambienteProduccion);
		if (isOnTesting())
			setEnvType(ambienteCertificacion);
		
		setDeliveredType(LEC_FE_UtilsXml.emisionNormal);
		setCodeAccessType(LEC_FE_UtilsXml.claveAccesoAutomatica);
		
		// if (IsUseContingency) {
		// 	setDeliveredType(LEC_FE_UtilsXml.emisionContingencia);
		// 	setCodeAccessType(LEC_FE_UtilsXml.claveAccesoContingencia);
		// }
		
		if (! isOnTesting()) {
			setUrlWSRecepcionComprobantes(MSysConfig.getValue("QSSLEC_FE_SRIURLWSProdRecepcionComprobante", null, Env.getAD_Client_ID(Env.getCtx())));
			setUrlWSAutorizacionComprobantes(MSysConfig.getValue("QSSLEC_FE_SRIURLWSProdAutorizacionComprobante", null, Env.getAD_Client_ID(Env.getCtx())));
		} else {
			setUrlWSRecepcionComprobantes(MSysConfig.getValue("QSSLEC_FE_SRIURLWSCertRecepcionComprobante", null, Env.getAD_Client_ID(Env.getCtx())));
			setUrlWSAutorizacionComprobantes(MSysConfig.getValue("QSSLEC_FE_SRIURLWSCertAutorizacionComprobante", null, Env.getAD_Client_ID(Env.getCtx())));
		}
		
		setSriWSTimeout(MSysConfig.getIntValue("QSSLEC_FE_SRIWebServiceTimeout", 0, Env.getAD_Client_ID(Env.getCtx())));
		
		setAttachXml(MSysConfig.getBooleanValue("QSSLEC_FE_DebugEnvioRecepcion", false, Env.getAD_Client_ID(Env.getCtx())));

		// setPKCS12_Resource(MSysConfig.getValue("QSSLEC_FE_RutaCertificadoDigital", null, Env.getAD_Client_ID(Env.getCtx()), getAD_Org_ID()));
		// setPKCS12_Password(MSysConfig.getValue("QSSLEC_FE_ClaveCertificadoDigital", null, Env.getAD_Client_ID(Env.getCtx()), getAD_Org_ID()));

		setFolderRaiz(MSysConfig.getValue("QSSLEC_FE_RutaGeneracionXml", null, Env.getAD_Client_ID(Env.getCtx())));	// Segun SysConfig + Formato
		
		//crea los directorios para los archivos xml
		(new File(getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesGenerados + File.separator)).mkdirs();
		(new File(getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesFirmados + File.separator)).mkdirs();
		(new File(getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesTransmitidos + File.separator)).mkdirs();
		(new File(getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesRechazados + File.separator)).mkdirs();
		(new File(getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesAutorizados + File.separator)).mkdirs();
		(new File(getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesNoAutorizados + File.separator)).mkdirs();

		(new File(getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesEnProceso + File.separator)).mkdirs();
	}
	
	public void main(String[] args) {
    
		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();
        signature.execute();
        
    }
	
    @Override
    public DataToSign createDataToSign() {
        DataToSign dataToSign = new DataToSign();
        dataToSign.setXadesFormat(EnumFormatoFirma.XAdES_BES);
        dataToSign.setEsquema(XAdESSchemas.XAdES_132);
        dataToSign.setXMLEncoding(XmlEncoding);
        dataToSign.setEnveloped(true);
        dataToSign.addObject(new ObjectToSign(new InternObjectToSign("comprobante"), "contenido comprobante", null, "text/xml", null));
        dataToSign.setParentSignNode("comprobante");
        Document docToSign = getDocument(getResource_To_Sign());
        dataToSign.setDocument(docToSign);
        dataToSign.addObject(new ObjectToSign(new AllXMLToSign(), "Contenido comprobante", null, "text/xml", null));
        return dataToSign;
    }
    
    public  String respuestaRecepcionComprobante(String file_name) {
        
    	String msg = null;
    	
		try	{
    		
    	System.out.println("@Verificando Conexion servicio recepcion SRI@" + (isOnTesting ? "PRUEBAS " : "PRODUCCION"));
    	if (! existeConexion(m_responseReception)) {
        	msg = "Error no hay conexion al servicio recepcion SRI: " + (isOnTesting ? "PRUEBAS " : "PRODUCCION");
        	return msg;
		}
        	
        System.out.println("@Sending Xml@ -> " + file_name);
        // Enviar a Recepcion Comprobante SRI
        byte[] bytes = getBytesFromFile(file_name);

        RespuestaSolicitud respuestasolicitud = validarComprobante(bytes);
        if (respuestasolicitud == null) {
        	msg = "Error falla consumiendo el servicio recepcion SRI: " + (isOnTesting ? "PRUEBAS " : "PRODUCCION");
        	return msg;
		}
        		
        msg = respuestasolicitud.getEstado();
        System.out.println("@Recepcion SRI@ -> " + msg);
        Comprobantes comprobantes = respuestasolicitud.getComprobantes();
        for (Comprobante comprobante : comprobantes.getComprobante()) {
        	//
        	Comprobante.Mensajes mensajes = comprobante.getMensajes();
        	for (ec.gob.sri.comprobantes.ws.Mensaje mensaje : mensajes.getMensaje()) {
	    		if (mensaje.getTipo().equals(mensajeInformativo))
	    			// Ignore Informativo
	    			continue;
        		msg = respuestasolicitud.getEstado() + ConstantesXADES.GUION + mensaje.getTipo() + ConstantesXADES.GUION + mensaje.getIdentificador() + ConstantesXADES.GUION + mensaje.getMensaje() + ConstantesXADES.GUION + mensaje.getInformacionAdicional();
	    		System.out.println("@Mensaje Xml@ -> " + msg);
        	}
        	// TODO Extraer y guardar comprobante xml en file_name
        	file_name = getFilename(this, folderComprobantesTransmitidos);
        	FileUtils.writeStringToFile(new File(file_name), comprobante.toString());
	    }
        if (! respuestasolicitud.getEstado().equals(recepcionRecibida)) {
        	msg = respuestasolicitud.getEstado() + ConstantesXADES.GUION + comprobantes.getComprobante().toString() + ConstantesXADES.GUION + msg;
        	return msg;
		}else{
			msg= recepcionRecibida;
		}
        
    	//
    	} catch (InvocationTargetException ite) {
    		msg = ite.getLocalizedMessage();
    		if (msg == null)
    			msg = ite.toString();
    		
    		System.out.println("@Bypass Exception@ -> " + msg);
    		return null;
    	} catch (SecurityException se) {
    		msg = se.getLocalizedMessage();
    		if (msg == null)
    			msg = se.toString();
    		
    		System.out.println("@Bypass Exception@ -> " + msg);
    		return null;
    	} catch (Exception e) {
    		msg = e.getLocalizedMessage();
    		if (msg == null)
    			msg = e.toString();
    		
    		return msg;
		}
    	
    	return msg;
	}
    
    public String respuestaAutorizacionComprobante(X_SRI_AccessCode ac, X_SRI_Authorization a, String accesscode) {
    	
    	Boolean isAutorizacion = false;
    	String msg = null;
    	String file_name = null;
    	int comprobantesEncontrados = 0;
    	try	{
    		
        System.out.println("@Verificando Conexion servicio autorizacion SRI@" + (isOnTesting ? "PRUEBAS " : "PRODUCCION"));
        if (! existeConexion(m_responseAutorization)) {
        	msg = "Advertencia no hay conexion al servicio autorizacion SRI, ¡Comprobante Enviado!: " + (isOnTesting ? "PRUEBAS " : "PRODUCCION");
        	System.out.println(msg);
		}
        msg = null;
        System.out.println("@Authorizing Xml@ -> " + accesscode);
        RespuestaComprobante respuestacomprobante = autorizacionComprobante(accesscode);
        
        if (respuestacomprobante == null) {
        	msg = "Advertencia falla consumiendo el servicio recepcion SRI: " + (isOnTesting ? "PRUEBAS " : "PRODUCCION");
        	System.out.println(msg);
        	throw new AdempiereException(msg);
		}
        
	    if (!respuestacomprobante.getNumeroComprobantes().equals(null)|| !respuestacomprobante.getNumeroComprobantes().equals("")){
	    	comprobantesEncontrados = Integer.valueOf(respuestacomprobante.getNumeroComprobantes());
	    }
	    
	    Autorizaciones autorizaciones = respuestacomprobante.getAutorizaciones();
	    
	    // Procesar Respuesta Autorizacion SRI
	    for (Autorizacion autorizacion : autorizaciones.getAutorizacion()) {
	    	String prueba = autorizacion.getComprobante();
	    	System.out.println(prueba);
	    	// msg = autorizaciones.getAutorizacion().get(0).getEstado();
	    	msg = autorizacion.getEstado() + ConstantesXADES.GUION + autorizacion.getNumeroAutorizacion() + ConstantesXADES.GUION + autorizacion.getFechaAutorizacion().toString();
	    	System.out.println("@Autorizacion Xml@ -> " + msg);
	        //
	    	Mensajes mensajes = autorizacion.getMensajes();
	    	for (Mensaje mensaje : mensajes.getMensaje()) {
	    		if (mensaje.getTipo().equals(mensajeInformativo))
	    			// Ignore Informativo
	    			continue;
	    		msg = autorizacion.getEstado() + ConstantesXADES.GUION + mensaje.getTipo() + ConstantesXADES.GUION + mensaje.getIdentificador() + ConstantesXADES.GUION + mensaje.getMensaje() + ConstantesXADES.GUION + mensaje.getInformacionAdicional();
	    		System.out.println("@Mensaje Xml@ -> " + msg);
	    		a.setSRI_ErrorCode_ID(LEC_FE_Utils.getErrorCode(mensaje.getIdentificador()));
	    		a.saveEx();
	    	}
	    	//
	    	if (autorizacion.getEstado().equals(comprobanteAutorizado)) {
	    		
	    		msg = null;
	    		isAutorizacion = true;

	    		// Update AccessCode
	    		if (ac.getCodeAccessType().equals(claveAccesoAutomatica)) {
	    			ac.setValue(autorizacion.getNumeroAutorizacion());
	    			ac.saveEx();
	    		}	
	    		
	    		// Adaptation long date format
	    		Calendar fechaAutorizacion = Calendar.getInstance();
	    		fechaAutorizacion.set(autorizacion.getFechaAutorizacion().getYear(), autorizacion.getFechaAutorizacion().getMonth()-1, autorizacion.getFechaAutorizacion().getDay(), autorizacion.getFechaAutorizacion().getHour(), autorizacion.getFechaAutorizacion().getMinute(),autorizacion.getFechaAutorizacion().getSecond());
	    		Timestamp ts = new Timestamp(fechaAutorizacion.getTimeInMillis()); 
	    		
	    		// Update Authorization
	    		a.setSRI_AuthorizationCode(autorizacion.getNumeroAutorizacion());
	    		a.setSRI_AuthorizationDate(ts);
				a.setSRI_ErrorCode_ID(0);
	    		a.setProcessed(true);
	    		a.saveEx();
	    		
	    		file_name = getFilename(this, folderComprobantesAutorizados);
	    		
	    		// FileUtils.writeStringToFile(new File(file_name), autorizacion.toString());
	    		StringBuffer contaut = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	    		contaut.append("<autorizacion>");
	    		contaut.append("<estado>"+autorizacion.getEstado()+"</estado>");
	    		contaut.append("<numeroAutorizacion>"+autorizacion.getNumeroAutorizacion()+"</numeroAutorizacion>");
	    		Date fecha = autorizacion.getFechaAutorizacion().toGregorianCalendar().getTime();
	    		SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yyyy hh:mm:ss aa");
	    		String fecha2 = dateFormat.format(fecha);
	    		contaut.append("<fechaAutorizacion class=\"fechaAutorizacion\">"+fecha2+"</fechaAutorizacion>");
	    		contaut.append("<comprobante><![CDATA["+autorizacion.getComprobante()+"]]></comprobante>");
	    		contaut.append("</autorizacion>");
	    		
	    		
	    		
		    	FileUtils.writeStringToFile(new File(file_name), contaut.toString());
		    	
		  		// Atach XML Autorizado
	    		if (isAutorizacion && isAttachXml())
	    			LEC_FE_Utils.attachXmlFile(a.getCtx(), a.get_TrxName(), a.getSRI_Authorization_ID(), file_name);
		    	break;	//Si almenos un comprobante está autorizado se omiten los no autorizados.
	    	} else if (autorizacion.getEstado().equals(comprobanteNoAutorizado))
	    		file_name = getFilename(this, folderComprobantesNoAutorizados);
	    	else
	    		file_name = getFilename(this, folderComprobantesRechazados);
	    	
	    	if (autorizacion.getEstado().equals(comprobanteNoAutorizado)
    			// Completar en estos casos, luego usar Boton Reprocesar
	        	// 70-Clave de acceso en procesamiento
    			&& (a.getSRI_ErrorCode().getValue().equals("70")) ) {
    	    		isAutorizacion = true;
    	    		file_name = getFilename(this, folderComprobantesAutorizados);
    			}
	    	
	    	// TODO Extraer y guardar autorizacion xml signed and authorised en file_name
	    	// FileUtils.writeStringToFile(new File(file_name), autorizacion.toString());
	    	FileUtils.writeStringToFile(new File(file_name), autorizacion.getComprobante());
	    	
	  		// Atach XML Autorizado
    		if (isAutorizacion && isAttachXml())
    			LEC_FE_Utils.attachXmlFile(a.getCtx(), a.get_TrxName(), a.getSRI_Authorization_ID(), file_name);
  	    	
	    	//break;	// Solo Respuesta autorizacion mas reciente segun accesscode
	    	
	    	}
	    
    	//
    	} catch (SecurityException se) {
    		msg = se.getLocalizedMessage();
    		if (msg == null)
    			msg = se.toString();
    		
    		System.out.println("@Bypass Exception@ -> " + msg);
    		msg = null;
    	} catch (Exception e) {
    		msg = e.getLocalizedMessage();
    		if (msg == null)
    			msg = e.toString();
    		
    		System.out.println("@Bypass Exception@ -> " + msg);
    		msg = null;
		}
    	
    	if (msg == null)
    		if (! isAutorizacion) {
    		
    			a.setSRI_ErrorCode_ID(LEC_FE_Utils.getErrorCode("70"));
	    		a.saveEx();
	    		file_name = getFilename(this, folderComprobantesFirmados);
		  		// Atach XML Autorizado
	    		if (isAttachXml())
	    			LEC_FE_Utils.attachXmlFile(a.getCtx(), a.get_TrxName(), a.getSRI_Authorization_ID(), file_name);
     			msg = "@Autorizacion Xml@ -> No hay Respuesta Autorizacion SRI, ¡Comprobante Enviado!";
    		}
    	a.saveEx();
    	ac.saveEx();
    	
     	return msg;
	}
    
    public RespuestaSolicitud validarComprobante(byte[] xml) {
        
    	RespuestaSolicitud response = null;    	
    	Class<?> serviceClass = null;
    	Class<?> interfaceClass = null;
    	Constructor<?> constructor = null;
		Method serviceClassGetPort = null;
		Method portClassValidar = null;
    	Object service = null;
    	Object port = null;
		try {
			interfaceClass = Class.forName(m_responseReceptionClass);
			serviceClass = Class.forName(m_responseReceptionClass+serviceWord);
			constructor = serviceClass.getConstructor(URL.class, QName.class);
			serviceClassGetPort = serviceClass.getMethod("getRecepcionComprobantesPort");
			portClassValidar = interfaceClass.getMethod("validarComprobante", byte[].class);
		} catch (Exception e) {
        	System.out.println(e.getLocalizedMessage());
        	return null;
		}

    	//RecepcionComprobantesService service = null;    	
    	String wsdlLocation = getUrlWSRecepcionComprobantes();
        
        try {
            QName qname = new QName(recepcionComprobantesQname, m_responseReception);
            URL url = new URL(wsdlLocation);
            service = constructor.newInstance(url, qname);
            //service = new RecepcionComprobantesService(url, qname);
            port = serviceClassGetPort.invoke(service);
        } catch (MalformedURLException ex) {
        	System.out.println(ex.getLocalizedMessage());
        	return null;
        } catch (WebServiceException ws) {
        	System.out.println(ws.getLocalizedMessage());
        	return null;
        } catch (Exception ex) {
        	System.out.println(ex.getLocalizedMessage());
        	return null;
		}
        
        // Controlar el tiempo de espera al consumir un webservice
        if (getSriWSTimeout() > 0) {
        	((BindingProvider) port).getRequestContext().put("com.sun.xml.internal.ws.connect.timeout", getSriWSTimeout());
        	((BindingProvider) port).getRequestContext().put("com.sun.xml.internal.ws.request.timeout", getSriWSTimeout());
        }
    	
        try {
			response = (RespuestaSolicitud) portClassValidar.invoke(port, xml);
		} catch (Exception ex) {
        	System.out.println(ex.getLocalizedMessage());
        	return null;
		}
    	return response;
    }
    
    public RespuestaComprobante autorizacionComprobante(String claveAccesoComprobante) {
        
    	RespuestaComprobante response = null;    	
    	Class<?> serviceClass = null;
    	Class<?> interfaceClass = null;
    	Constructor<?> constructor = null;
		Method serviceClassGetPort = null;
		Method portClassValidar = null;
    	Object service = null;
    	Object port = null;
		try {
			interfaceClass = Class.forName(m_responseAutorizationClass);
			serviceClass = Class.forName(m_responseAutorizationClass+serviceWord);
			constructor = serviceClass.getConstructor(URL.class, QName.class);
			serviceClassGetPort = serviceClass.getMethod("getAutorizacionComprobantesPort");
			portClassValidar = interfaceClass.getMethod("autorizacionComprobante", String.class);
		} catch (Exception e) {
        	System.out.println(e.getLocalizedMessage());
        	return null;
		}

    	String wsdlLocation = getUrlWSAutorizacionComprobantes();
        
        try {
            QName qname = new QName(autorizacionComprobantesQname, m_responseAutorization);
            URL url = new URL(wsdlLocation);
            service = constructor.newInstance(url, qname);
            //service = new AutorizacionComprobantesService(url, qname);
            port = serviceClassGetPort.invoke(service);
        } catch (MalformedURLException ex) {
        	System.out.println(ex.getLocalizedMessage());
            return null;
        } catch (WebServiceException ws) {
        	System.out.println(ws.getLocalizedMessage());
        	return null;
        }  catch (Exception ex) {
        	System.out.println(ex.getLocalizedMessage());
        	return null;
        }
        
    	// Controlar el tiempo de espera al consumir un webservice
        if (getSriWSTimeout() > 0) {
        	((BindingProvider) port).getRequestContext().put("com.sun.xml.internal.ws.connect.timeout", getSriWSTimeout());
        	((BindingProvider) port).getRequestContext().put("com.sun.xml.internal.ws.request.timeout", getSriWSTimeout());
        }
        
        try {
			response = (RespuestaComprobante) portClassValidar.invoke(port, claveAccesoComprobante);
		} catch (Exception ex) {
        	System.out.println(ex.getLocalizedMessage());
        	return null;
		}
        
        return response;
    
    }
    
    public boolean existeConexion(String accionComprobantesService) {
        
    	int i = 0;
        boolean respuesta = false;
        
        String url = null;
        
        try {
        	
	        if (accionComprobantesService.equals(m_responseReception))
	        	url = getUrlWSRecepcionComprobantes();
	        else
		        url = getUrlWSAutorizacionComprobantes();
	    
	        while (i < 3) {
	            Object obj = getWebService(url, accionComprobantesService);
	            if (obj  == null) {
	                return true;
	            }
	            if ((obj  instanceof WebServiceException)) {
	                respuesta = false;
	            }
	            i++;
	        }
	        
        } catch (Exception ex) {
        	return false;
		}
        
        return respuesta;
    }
    
    public Object getWebService(String wsdlLocation, String accionComprobantesService) {
        
    	try {
            
            URL url = new URL(wsdlLocation);
            if (accionComprobantesService.equals(m_responseReception)) {
            	QName qname = new QName(recepcionComprobantesQname, accionComprobantesService);
            	RecepcionComprobantesService service = new RecepcionComprobantesService(url, qname);
            } else {
            	QName qname = new QName(autorizacionComprobantesQname, accionComprobantesService);
            	AutorizacionComprobantesService service = new AutorizacionComprobantesService(url, qname);
            }
            return null;
        } catch (MalformedURLException ex) {
            return ex;
        } catch (WebServiceException ws) {
            return ws;
        }
    
    }
    
    public byte[] getBytesFromFile(String xmlFilePath) throws Exception {
    	
    	byte[] bytes = null;
        File file = new File(xmlFilePath);
        InputStream is;
        is = new FileInputStream(file);
        long length = file.length();
        bytes = new byte[(int) length];
        
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        is.close();
	        
        return bytes;
    }
    
    public File getFileFromStream(String xmlFilePath, int sri_authorization_id) throws Exception {
    	
    	InputStream inputStream = null;
		OutputStream outputStream = null;
		File file = null;
		MAttachment attach =  MAttachment.get(Env.getCtx(), MTable.getTable_ID("SRI_Authorization"), sri_authorization_id);
		if (attach != null) {
    		for (MAttachmentEntry entry : attach.getEntries()) {
            	if (entry.getName().endsWith("xml") && !entry.getName().contains("old")) {
            		setResource_To_Sign(entry.getName());
            		xmlFilePath = getOutput_Directory() + File.separator + folderComprobantesFirmados + File.separator + getSignatureFileName().substring(getSignatureFileName().lastIndexOf(File.separator) + 1);
            		inputStream = new FileInputStream(entry.getFile());
            		outputStream = new FileOutputStream(xmlFilePath);
            		
            		int numRead = 0;
            		byte[] bytes = new byte[1024];
            		 
            		while ((numRead = inputStream.read(bytes)) != -1) {
            			outputStream.write(bytes, 0, numRead);
            		}
            		inputStream.close();
            		outputStream.flush();
            		outputStream.close();
            		file = (new File (xmlFilePath));
            		break;	// First
            	}
            }
		}
		
		return file;
    }
    
	public String getFilename(LEC_FE_UtilsXml signature, String folderComprobantesDestino)	// Trace temporal
	{
		
		String file_name = signature.getFolderRaiz() + File.separator + folderComprobantesDestino + File.separator
        		+ signature.getSignatureFileName().substring(signature.getSignatureFileName().lastIndexOf(File.separator) + 1);
	
		return file_name;
	}
    
	/**
	 * The XML file has already been built and attached to an Authorization, 
	 * this process only sends the XML and query their status
	 * SRISendXMLAndRequestAuthorization
	 */
	public static String SRISendXMLAndRequestAuthorization(
			X_SRI_Authorization authorization,Properties ctx,String trxName) {
		CLogger log = CLogger.getCLogger(X_SRI_Authorization.class);
		String file_name = "";
		String msg = null;
		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();
		try {

			X_SRI_AccessCode accesscode = new X_SRI_AccessCode(ctx,
					authorization.getSRI_AccessCode_ID(), trxName);

			File file = signature.getFileFromStream(file_name,
					authorization.getSRI_Authorization_ID());

			file_name = signature.getFilename(signature,
					LEC_FE_UtilsXml.folderComprobantesFirmados);

			if (file.exists() || file.isFile() || file.canRead()) {

				// Procesar Recepcion SRI
				log.warning("@Sending Xml@ -> " + file_name);
				msg = signature.respuestaRecepcionComprobante(file_name);

				if (msg != null)
					if (!msg.equals("RECIBIDA")) {
						if (!msg.contains("CLAVE ACCESO REGISTRADA"))
							throw new AdempiereException(msg);
						else
							log.warning("@Authorizing Xml@ -> "
									+ file_name
									+ "CLAVE ACCESO REGISTRADA-CONSULTANDO AUTORIZACIONES VÁLIDAS");
					}
				// Procesar Autorizacion SRI
				log.warning("@Authorizing Xml@ -> " + file_name);
				msg = signature.respuestaAutorizacionComprobante(accesscode,
						authorization, authorization.getValue());
				if (msg != null) {
					if (msg.contains("¡Comprobante Enviado!")) {
						msg = signature.respuestaAutorizacionComprobante(
								accesscode, authorization,
								authorization.getValue());
					} else {
						throw new AdempiereException(msg);
					}
				}
				log.warning("@Authorization Succesfully Xml@ -> " + file_name);
				file_name = signature.getFilename(signature,
						LEC_FE_UtilsXml.folderComprobantesAutorizados);

			}
			} catch (Exception e) {
			// msg = "No se pudo obtener autorizacion - " + e.getMessage();
				log.severe(msg);
				return msg;
			}

		return msg;

	} // SRISendXMLAndRequestAuthorization
	
}	// LEC_FE_UtilsXml
