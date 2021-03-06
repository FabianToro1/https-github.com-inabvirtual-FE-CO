package org.globalqss.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocation;
import org.compiere.model.MNote;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MShipper;
import org.compiere.model.MSysConfig;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.globalqss.util.LEC_FE_Utils;
import org.globalqss.util.LEC_FE_UtilsXml;
import org.xml.sax.helpers.AttributesImpl;
import org.globalqss.model.X_LCO_TaxPayerType;
import org.globalqss.model.X_LCO_TaxIdType;;



/**
 *	LEC_FE_MInvoice
 *
 *  @author Carlos Ruiz - globalqss - Quality Systems & Solutions - http://globalqss.com 
 *  @version  $Id: LEC_FE_MInOut.java,v 1.0 2014/05/06 03:37:29 cruiz Exp $
 * @param <X_LCO_TaxPayerType>
 */
public class LEC_FE_MInOut extends MInOut
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -924606040343895114L;
	
	private int		m_SRI_Authorization_ID = 0;
	private int		m_lec_sri_format_id = 0;
	private int		m_c_invoice_sus_id = 0;

	private String file_name = "";
	private String m_obligadocontabilidad = "NO";
	private String m_coddoc = "";
	private String m_accesscode;
	private String m_identificacionconsumidor = "";
	private String m_tipoidentificacioncomprador = "";
	private String m_tipoidentificaciontransportista = "";
	private String m_identificacioncomprador = "";
	private String m_razonsocial = "";
	// 04/07/2016 MHG Offline Schema added
	private boolean isOfflineSchema = false;

	public LEC_FE_MInOut(Properties ctx, int M_InOut_ID, String trxName) {
		super(ctx, M_InOut_ID, trxName);
		// 04/07/2016 MHG Offline Schema added
		isOfflineSchema=MSysConfig.getBooleanValue("QSSLEC_FE_OfflineSchema", false, Env.getAD_Client_ID(Env.getCtx()));
	}
	
	public String lecfeinout_SriExportInOutXML100 ()
	{
		int autorizationID = 0;
		String msg = null;
		String ErrorDocumentno = "Error en Entrega a Cliente No "+getDocumentNo()+" ";  
		
		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();
		
		try
		{
			
		signature.setAD_Org_ID(getAD_Org_ID());
			
		m_identificacionconsumidor=MSysConfig.getValue("QSSLEC_FE_IdentificacionConsumidorFinal", null, getAD_Client_ID());
		
		signature.setPKCS12_Resource(MSysConfig.getValue("QSSLEC_FE_RutaCertificadoDigital", null, getAD_Client_ID(), getAD_Org_ID()));
		signature.setPKCS12_Password(MSysConfig.getValue("QSSLEC_FE_ClaveCertificadoDigital", null, getAD_Client_ID(), getAD_Org_ID()));
		
		if (signature.getFolderRaiz() == null)
			return ErrorDocumentno+"No existe parametro para Ruta Generacion Xml";
		
		MDocType dt = new MDocType(getCtx(), getC_DocType_ID(), get_TrxName());
		
		m_coddoc = dt.get_ValueAsString("SRI_ShortDocType");
		
		if ( m_coddoc.equals(""))
			return ErrorDocumentno+"No existe definicion SRI_ShortDocType: " + dt.toString();
		
		// Formato
		m_lec_sri_format_id = LEC_FE_Utils.getLecSriFormat(getAD_Client_ID(), signature.getDeliveredType(), m_coddoc, getMovementDate(), getMovementDate());
		
		if ( m_lec_sri_format_id < 1)
			return ErrorDocumentno+"No existe formato para el comprobante";
		
		X_LEC_SRI_Format f = new X_LEC_SRI_Format (getCtx(), m_lec_sri_format_id, get_TrxName());
		
		// Emisor
		MOrgInfo oi = MOrgInfo.get(getCtx(), getAD_Org_ID(), get_TrxName());
		
		msg = LEC_FE_ModelValidator.valideOrgInfoSri (oi);
		
		if (msg != null)
			return ErrorDocumentno+msg;
		
		if ( (Boolean) oi.get_Value("SRI_IsKeepAccounting"))
			m_obligadocontabilidad = "SI";
		
		int c_bpartner_id = LEC_FE_Utils.getOrgBPartner(getAD_Client_ID(), oi.get_ValueAsString("TaxID"));
		MBPartner bpe = new MBPartner(getCtx(), c_bpartner_id, get_TrxName());
		
		MLocation lo = new MLocation(getCtx(), oi.getC_Location_ID(), get_TrxName());
		
		int c_location_matriz_id = MSysConfig.getIntValue("QSSLEC_FE_LocalizacionDireccionMatriz", -1, oi.getAD_Client_ID(), getAD_Org_ID());

		MLocation lm = new MLocation(getCtx(), c_location_matriz_id, get_TrxName());
		
		MLocation lw = new MLocation(getCtx(), getM_Warehouse().getC_Location_ID(), get_TrxName());
		
		// Comprador
		MBPartner bp = new MBPartner(getCtx(), getC_BPartner_ID(), get_TrxName());
		if (!signature.isOnTesting()) m_razonsocial = bp.getName();
		
		MLocation bpl = new MLocation(getCtx(), getC_BPartner_Location().getC_Location_ID(), get_TrxName());	// TODO Reviewme
		
		X_LCO_TaxIdType ttc = new X_LCO_TaxIdType(getCtx(), (Integer) bp.get_Value("LCO_TaxIdType_ID"), get_TrxName());
		
		m_tipoidentificacioncomprador = LEC_FE_Utils.getTipoIdentificacionSri(ttc.get_Value("LEC_TaxCodeSRI").toString());
		
		m_identificacioncomprador = bp.getTaxID();
		
		X_LCO_TaxIdType tt = new X_LCO_TaxIdType(getCtx(), (Integer) bp.get_Value("LCO_TaxIdType_ID"), get_TrxName());
		if (tt.getLCO_TaxIdType_ID() == 1000011)	// Hardcoded F Final	// TODO Deprecated
			m_identificacioncomprador = m_identificacionconsumidor;
		
		// Transportista
		Boolean isventamostrador = false;
		Timestamp datets = getShipDate();
		Timestamp datete = (Timestamp) get_Value("ShipDateE");
		int m_shipper_id = getM_Shipper_ID();
		
		if (m_shipper_id == 0) {
			if (MDocType.DOCSUBTYPESO_OnCreditOrder.equals(getC_Order().getC_DocType().getDocSubTypeSO())) {
				isventamostrador = true;
				datets = getMovementDate();
				datete = getMovementDate();
				m_shipper_id = MSysConfig.getIntValue("QSSLEC_FE_TransportadorVentaMostrador", 0, getAD_Client_ID());
				}
				else {
					return ErrorDocumentno+"No existe definicion Transportista";
				}
		}
		
		MShipper st = new MShipper(getCtx(), m_shipper_id, get_TrxName());
		MBPartner bpt = new MBPartner(getCtx(), st.getC_BPartner_ID(), get_TrxName());
		
		X_LCO_TaxIdType ttt = new X_LCO_TaxIdType(getCtx(), (Integer) bpt.get_Value("LCO_TaxIdType_ID"), get_TrxName());
		
		X_LCO_TaxPayerType tpt = new X_LCO_TaxPayerType(getCtx(), (Integer) bpt.get_Value("LCO_TaxPayerType_ID"), get_TrxName());
		
		m_tipoidentificaciontransportista = LEC_FE_Utils.getTipoIdentificacionSri(ttt.get_Value("LEC_TaxCodeSRI").toString());
		
		m_c_invoice_sus_id = LEC_FE_Utils.getInOutDocSustento(getM_InOut_ID());
		
		if ( m_c_invoice_sus_id < 1)
			log.warning("No existe documento sustento para el comprobante");	// TODO Reviewme
			//throw new AdempiereUserError("No existe documento sustento para el comprobante");
		
		MInvoice invsus = null;
		X_SRI_Authorization asus = null;
		if ( m_c_invoice_sus_id > 0) {
			invsus = new MInvoice(getCtx(), m_c_invoice_sus_id, get_TrxName());
			if ( invsus.get_Value("SRI_Authorization_ID") != null)
				//log.warning("No existe documento sustento autorizado para el comprobante");	// TODO Reviewme
				//	return ErrorDocumentno+"No existe documento sustento autorizado para el comprobante";
			asus = new X_SRI_Authorization (getCtx(), (Integer) invsus.get_Value("SRI_Authorization_ID"), get_TrxName());
		}
		
		if (!isventamostrador && (getShipDate() == null || get_Value("ShipDateE") == null))
			return ErrorDocumentno+"Debe indicar fechas de transporte";
		
		// IsUseContingency
		int sri_accesscode_id = 0;
		if (signature.IsUseContingency) {
			sri_accesscode_id = LEC_FE_Utils.getNextAccessCode(getAD_Client_ID(), signature.getEnvType(), oi.getTaxID(), get_TrxName());
			if ( sri_accesscode_id < 1)
				return ErrorDocumentno+"No hay clave de contingencia para el comprobante";
		}
		
		// New/Upd Access Code
		
		//Se crea una transaccion nueva para la autorizacion y clave de acceso, para realizar un commit, en el
				//caso de que se envie el comprobante y no se obtenga respuesta del SRI
		
		X_SRI_AccessCode ac = null;
		ac = new X_SRI_AccessCode (getCtx(), sri_accesscode_id, get_TrxName());
				
		
		ac.setAD_Org_ID(getAD_Org_ID());
		ac.setOldValue(null);	// Deprecated
		ac.setEnvType(signature.getEnvType());
		ac.setCodeAccessType(signature.getCodeAccessType());
		ac.setSRI_ShortDocType(m_coddoc);
		ac.setIsUsed(true);
		
		// Access Code
		m_accesscode = LEC_FE_Utils.getAccessCode(datets, m_coddoc, bpe.getTaxID(), oi.get_ValueAsString("SRI_OrgCode"), LEC_FE_Utils.getStoreCode(LEC_FE_Utils.formatDocNo(getDocumentNo(), m_coddoc)), getDocumentNo(), oi.get_ValueAsString("SRI_DocumentCode"), signature.getDeliveredType(), ac);
		
		if (signature.getCodeAccessType().equals(LEC_FE_UtilsXml.claveAccesoAutomatica))
			ac.setValue(m_accesscode);
		
		if (!ac.save()) {
			msg = "@SaveError@ No se pudo grabar SRI Access Code";
			return ErrorDocumentno+msg;
		}
		
		// New Authorization
		X_SRI_Authorization a = null;
		a = new X_SRI_Authorization (getCtx(), 0,get_TrxName());
		a.setAD_Org_ID(getAD_Org_ID());
		a.setSRI_ShortDocType(m_coddoc);
		a.setValue(m_accesscode);
		a.setSRI_AuthorizationCode(null);
		a.setSRI_AccessCode_ID(ac.get_ID());
		a.setSRI_ErrorCode_ID(0);
		a.setAD_UserMail_ID(getAD_User_ID());
		a.set_ValueOfColumn("isSRIOfflineSchema", isOfflineSchema);
		a.set_ValueOfColumn("M_InOut_ID", get_ID());
		
		if (!a.save()) {
			msg = "@SaveError@ No se pudo grabar SRI Autorizacion";
			return ErrorDocumentno+msg;
		}
		
		autorizationID = a.get_ID();
					
		OutputStream  mmDocStream = null;
				
		String xmlFileName = "SRI_" + m_coddoc + "-" + LEC_FE_Utils.getDate(getMovementDate(),9) + "-" + m_accesscode + ".xml";
	
		//ruta completa del archivo xml
		file_name = signature.getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesGenerados + File.separator + xmlFileName;	
		//Stream para el documento xml
		mmDocStream = new FileOutputStream (file_name, false);
		StreamResult streamResult_menu = new StreamResult(new OutputStreamWriter(mmDocStream,signature.getXmlEncoding()));
		SAXTransformerFactory tf_menu = (SAXTransformerFactory) SAXTransformerFactory.newInstance();					
		try {
			tf_menu.setAttribute("indent-number", new Integer(0));
		} catch (Exception e) {
			// swallow
		}
		TransformerHandler mmDoc = tf_menu.newTransformerHandler();	
		Transformer serializer_menu = mmDoc.getTransformer();	
		serializer_menu.setOutputProperty(OutputKeys.ENCODING,signature.getXmlEncoding());
		try {
			serializer_menu.setOutputProperty(OutputKeys.INDENT,"yes");
		} catch (Exception e) {
			// swallow
		}
		mmDoc.setResult(streamResult_menu);
		
		mmDoc.startDocument();
		
		AttributesImpl atts = new AttributesImpl();
		
		StringBuffer sql = null;

		// Encabezado
		atts.clear();
		atts.addAttribute("", "", "id", "CDATA", "comprobante");
		atts.addAttribute("", "", "version", "CDATA", f.get_ValueAsString("VersionNo"));
		// atts.addAttribute("", "", "xmlns:ds", "CDATA", "http://www.w3.org/2000/09/xmldsig#");
		// atts.addAttribute("", "", "xmlns:xsi", "CDATA", "http://www.w3.org/2001/XMLSchema-instance");
		// atts.addAttribute("", "", "xsi:noNamespaceSchemaLocation", "CDATA", f.get_ValueAsString("Url_Xsd"));
		mmDoc.startElement("", "", f.get_ValueAsString("XmlPrintLabel"), atts);
		
		atts.clear();
		
		// Emisor
		mmDoc.startElement("","","infoTributaria", atts);
			// Numerico1
			addHeaderElement(mmDoc, "ambiente", signature.getEnvType(), atts);
			// Numerico1
			addHeaderElement(mmDoc, "tipoEmision", signature.getDeliveredType(), atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "razonSocial", bpe.getName(), atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "nombreComercial", bpe.getName2()==null ? bpe.getName() : bpe.getName2(), atts);
			// Numerico13
			addHeaderElement(mmDoc, "ruc", (LEC_FE_Utils.fillString(13 - (LEC_FE_Utils.cutString(bpe.getTaxID(), 13)).length(), '0'))
				+ LEC_FE_Utils.cutString(bpe.getTaxID(),13), atts);
			// Num??rico49
			addHeaderElement(mmDoc, "claveAcceso", a.getValue(), atts);
			// Numerico2
			addHeaderElement(mmDoc, "codDoc", m_coddoc, atts);
			// Numerico3
			addHeaderElement(mmDoc, "estab", oi.get_ValueAsString("SRI_OrgCode"), atts);
			// Numerico3
			addHeaderElement(mmDoc, "ptoEmi", LEC_FE_Utils.getStoreCode(LEC_FE_Utils.formatDocNo(getDocumentNo(), m_coddoc)), atts);
			// Numerico9
			addHeaderElement(mmDoc, "secuencial", (LEC_FE_Utils.fillString(9 - (LEC_FE_Utils.cutString(LEC_FE_Utils.getSecuencial(getDocumentNo(), m_coddoc), 9)).length(), '0'))
					+ LEC_FE_Utils.cutString(LEC_FE_Utils.getSecuencial(getDocumentNo(), m_coddoc), 9), atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "dirMatriz", lm.getAddress1(), atts);
		mmDoc.endElement("","","infoTributaria");
		
		mmDoc.startElement("","","infoGuiaRemision",atts);
		// Emisor
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "dirEstablecimiento", lo.getAddress1(), atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "dirPartida", lw.getAddress1(), atts);	// TODO Reviewme
		// Transportista
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "razonSocialTransportista", bpt.getName(), atts);
			// Numerico2
			addHeaderElement(mmDoc, "tipoIdentificacionTransportista", m_tipoidentificaciontransportista, atts);
			// Numerico Max 13
			addHeaderElement(mmDoc, "rucTransportista", bpt.getTaxID(), atts);			
			// Alfanumerico Max 40
			addHeaderElement(mmDoc, "rise", LEC_FE_Utils.cutString(tpt.getName(), 40), atts);
			// Texto2
			addHeaderElement(mmDoc, "obligadoContabilidad", m_obligadocontabilidad, atts);
			// Numerico3-5
			addHeaderElement(mmDoc, "contribuyenteEspecial", oi.get_ValueAsString("SRI_TaxPayerCode"), atts);
			// Fecha8 ddmmaaaa
			addHeaderElement(mmDoc, "fechaIniTransporte", LEC_FE_Utils.getDate(new Date ((datets).getTime()),10), atts);
			// Fecha8 ddmmaaaa
			addHeaderElement(mmDoc, "fechaFinTransporte", LEC_FE_Utils.getDate(new Date ((datete).getTime()),10), atts);
			// Alfanumerico Max 20
			addHeaderElement(mmDoc, "placa", LEC_FE_Utils.cutString(st.getName(), 40), atts);
			
		mmDoc.endElement("","","infoGuiaRemision");	
		
		// Destinatarios
		mmDoc.startElement("","","destinatarios",atts);
		
			mmDoc.startElement("","","destinatario",atts);
			
			// Numerico Max 13
			addHeaderElement(mmDoc, "identificacionDestinatario", m_identificacioncomprador, atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "razonSocialDestinatario", bp.getName(), atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "dirDestinatario", bpl.getAddress1(), atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "motivoTraslado", "Venta", atts);
			// Alfanumerico Max 20
			if (get_Value("SRI_SingleCustomsDocument") != null)
				addHeaderElement(mmDoc, "docAduaneroUnico", get_Value("SRI_SingleCustomsDocument").toString(), atts);
			// Numerico3
			// addHeaderElement(mmDoc, "codEstabDestino", "TODO", atts);	// No Aplica Sismode
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "ruta", lw.getCityRegionPostal() + " - " + bpl.getCityRegionPostal(), atts);
			if  ( m_c_invoice_sus_id > 0) {
				// Numerico2
				if (m_coddoc.equals("06"))
					addHeaderElement(mmDoc, "codDocSustento", "01", atts);	// Hardcoded
				// Numerico15 -- Incluye guiones
				addHeaderElement(mmDoc, "numDocSustento", LEC_FE_Utils.formatDocNo(invsus.getDocumentNo(), "01"), atts);
				// Numerico10-37
				if (asus != null){
					if (asus.getSRI_AuthorizationCode() != null)
						addHeaderElement(mmDoc, "numAutDocSustento", asus.getSRI_AuthorizationCode(), atts);
					if(invsus.getDateInvoiced() != null)
						// Fecha8 ddmmaaaa
						addHeaderElement(mmDoc, "fechaEmisionDocSustento", LEC_FE_Utils.getDate(invsus.getDateInvoiced(),10), atts);
				}
			}
			
		// Detalles
		mmDoc.startElement("","","detalles",atts);
		
		sql = new StringBuffer(
	            "SELECT io.M_InOut_ID, COALESCE(p.value, '0'), 0::text, ilt.name, ilt.qtyentered "
				+ ", ilt.description AS description1 "
	            + "FROM M_InOut io "
	            + "JOIN M_InOutLine iol ON iol.M_InOut_ID = io.M_InOut_ID "
	            + "JOIN M_InOut_Line_VT ilt ON  ilt.M_InOut_ID = io.M_InOut_ID AND ilt.M_InOutLine_ID = iol.M_InOutLine_ID  "
	            + "LEFT JOIN M_Product p ON p.M_Product_ID = iol.M_Product_ID "
	            + "LEFT JOIN M_Product_Category pc ON pc.M_Product_Category_ID = p.M_Product_Category_ID "
	            + "WHERE iol.IsDescription = 'N' AND io.M_InOut_ID=? AND iol.MovementQty>0 "
	            + "ORDER BY iol.line");
		
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setInt(1, getM_InOut_ID());
			ResultSet rs = pstmt.executeQuery();
			//
			
			while (rs.next())
			{
				mmDoc.startElement("","","detalle",atts);
				
				// Alfanumerico MAx 25
				addHeaderElement(mmDoc, "codigoInterno",  LEC_FE_Utils.cutString(rs.getString(2),25), atts);
				// Alfanumerico MAx 25
				addHeaderElement(mmDoc, "codigoAdicional", LEC_FE_Utils.cutString(rs.getString(3),25), atts);
				// Alfanumerico Max 300
				addHeaderElement(mmDoc, "descripcion", LEC_FE_Utils.cutString(rs.getString(4),300), atts);
				// Numerico Max 14
				addHeaderElement(mmDoc, "cantidad", rs.getBigDecimal(5).toString(), atts);
				/*
				if (rs.getString(6) != null)  {
					mmDoc.startElement("","","detallesAdicionales",atts);
					
					atts.clear();
					atts.addAttribute("", "", "nombre", "CDATA", "descripcion1");
					atts.addAttribute("", "", "valor", "CDATA", LEC_FE_Utils.cutString(rs.getString(6),300));
					mmDoc.startElement("", "", "detAdicional", atts);
					mmDoc.endElement("","","detAdicional");
						
					mmDoc.endElement("","","detallesAdicionales");
				}
				*/
				atts.clear();
				//
				mmDoc.endElement("","","detalle");
				
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			DB.executeUpdate("DELETE FROM SRI_Authorization WHERE SRI_Authorization_ID = "+a.get_ID(), true, get_TrxName());
			log.log(Level.SEVERE, sql.toString(), e);
			msg = "Error SQL: " + sql.toString();
			return ErrorDocumentno+msg;
		}
		
		mmDoc.endElement("","","detalles");
		
		mmDoc.endElement("","","destinatario");
		
		mmDoc.endElement("","","destinatarios");
		/*
		if (getDescription() != null)  {
			mmDoc.startElement("","","infoAdicional",atts);
			
				atts.clear();
				atts.addAttribute("", "", "nombre", "CDATA", "descripcion2");
				mmDoc.startElement("", "", "campoAdicional", atts);
				String valor = LEC_FE_Utils.cutString(getDescription(),300);
				mmDoc.characters(valor.toCharArray(), 0, valor.length());
				mmDoc.endElement("","","campoAdicional");
			
			mmDoc.endElement("","","infoAdicional");
		}
		*/
		mmDoc.endElement("","",f.get_ValueAsString("XmlPrintLabel"));
		
		mmDoc.endDocument();
	
		if (mmDocStream != null) {
			try {
				mmDocStream.close();
			} catch (Exception e2) {}
		}
	
//		if (LEC_FE_Utils.breakDialog("Firmando Xml")) return "Cancelado...";	// TODO Temp
		
		log.warning("@Signing Xml@ -> " + file_name);
		signature.setResource_To_Sign(file_name);
		signature.setOutput_Directory(signature.getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesFirmados);
        signature.execute();
        
        file_name = signature.getFilename(signature, LEC_FE_UtilsXml.folderComprobantesFirmados);
        
        if (! signature.IsUseContingency) {
        	
//	        if (LEC_FE_Utils.breakDialog("Enviando Comprobante al SRI")) return "Cancelado...";	// TODO Temp
	        
	        // Procesar Recepcion SRI
	        log.warning("@Sending Xml@ -> " + file_name);
	        msg = signature.respuestaRecepcionComprobante(file_name);
	        
	        if (msg != null)
	        	if (!msg.equals("RECIBIDA")) {
	        		DB.executeUpdate("DELETE FROM SRI_Authorization WHERE SRI_Authorization_ID = "+a.get_ID(), true, get_TrxName());
	        		return ErrorDocumentno+msg;
	        	}
	        String invoiceNo = getDocumentNo();		
	        String invoiceID = String.valueOf(get_ID());
	        a.setDescription(invoiceNo);
	        a.set_ValueOfColumn("DocumentID", invoiceID);
	        a.saveEx();
        	        
        	// 04/07/2016 MHG Offline Schema added
        	if (!isOfflineSchema) {
 
	        	// Procesar Autorizacion SRI        	
		        log.warning("@Authorizing Xml@ -> " + file_name);
		        try {
		        	msg = signature.respuestaAutorizacionComprobante(ac, a, m_accesscode);
		        	
		        	if (msg != null){
		        		return ErrorDocumentno+msg;
		        	}
		        } catch (Exception ex) {
		        	// Completar en estos casos, luego usar Boton Reprocesar Autorizacion
		        	// 70-Clave de acceso en procesamiento
		        	if (a.getSRI_ErrorCode().getValue().equals("70"))
			        	// ignore exceptions
			        	log.warning(msg + ex.getMessage());
		        	else
		        		return msg;
		        }
			    file_name = signature.getFilename(signature, LEC_FE_UtilsXml.folderComprobantesAutorizados);
        	} else {
        		msg=null;
			    file_name = signature.getFilename(signature, LEC_FE_UtilsXml.folderComprobantesEnProceso);        		        		
        	}
		} else {	// emisionContingencia
			// Completar en estos casos, luego usar Boton Procesar Contingencia
			// 170-Clave de contingencia pendiente
			a.setSRI_ErrorCode_ID(LEC_FE_Utils.getErrorCode("170"));
    		a.saveEx();
			
    		if (signature.isAttachXml())
        		LEC_FE_Utils.attachXmlFile(a.getCtx(), a.get_TrxName(), a.getSRI_Authorization_ID(), file_name);
    
		}
		
//	    if (MSysConfig.getBooleanValue("QSSLEC_FE_EnvioXmlAutorizadoBPEmail", false, getAD_Client_ID()))
//		{
//	    	try {
//	    		List<File> attchs = new ArrayList<File>();
//		    	File attachment = (new File (file_name));
//		    	attchs.add(attachment);
//		    	attchs.add(createPDF());
//		    	
//		    	if (attachment.exists() || attachment.isFile() || attachment.canRead()) {
//		    		
//			    	log.warning("@EMailing Xml@ -> " + file_name);
//					// Enviar Email BPartner XML Autorizado
//					MMailText mText = new MMailText(getCtx(), 0, get_TrxName());	// Solo en memoria
//					mText.setPO(this);
//					String subject = "SRI " + (signature.isOnTesting ? LEC_FE_UtilsXml.nombreCertificacion : LEC_FE_UtilsXml.nombreProduccion) + " " + bpe.getValue() + " : " + f.get_ValueAsString("XmlPrintLabel") + " " + getDocumentNo();
//					String text =
//							" Emisor               : " + bpe.getName() +
//							"\nFecha                : " + LEC_FE_Utils.getDate(getMovementDate(),10) +
//							"\nCliente              : " + bp.getName() +
//							"\nComprobante          : " + f.get_ValueAsString("XmlPrintLabel") +
//							"\nNumero               : " + getDocumentNo() +
//							"\nAutorizacion No.     : " + a.getSRI_AuthorizationCode() +
//							"\nFecha Autorizacion   : " + a.getSRI_AuthorizationDate() +
//							"\nAdjunto              : " + file_name.substring(file_name.lastIndexOf(File.separator) + 1);
//						
//					int countMail = LEC_FE_Utils.notifyUsers(getCtx(), mText, getAD_User_ID(), subject, text, attchs, get_TrxName());
//					if (countMail == 0)
//						log.warning("@RequestActionEMailError@ -> " + file_name);
//		    	}
//	    	} catch (Exception ex) {
//	        	// ignore exceptions
//	        	log.warning(ex.getMessage());
//			}
//		}
		
//		if (LEC_FE_Utils.breakDialog("Completando Entrega")) return "Cancelado...";	// TODO Temp
		
		//
		}
		catch (Exception e)
		{
			msg = "No se pudo crear XML - " + e.getMessage();
			log.severe(msg);
			notifyError(msg);				
			return ErrorDocumentno+msg;
		}catch (Error e) {
			msg = "No se pudo crear XML- Error en Conexion con el SRI";
			return ErrorDocumentno+msg;
		}
		log.warning("@SRI_FileGenerated@ -> " + file_name);
		set_Value("SRI_Authorization_ID",autorizationID);
		this.saveEx();
		return msg;
	
	} // lecfeinout_SriExportInOutXML100
	
	public void addHeaderElement(TransformerHandler mmDoc, String att, String value, AttributesImpl atts) throws Exception {
		if (att != null) {
			mmDoc.startElement("","",att,atts);
			mmDoc.characters(value.toCharArray(),0,value.toCharArray().length);
			mmDoc.endElement("","",att);
		} else {
			throw new AdempiereUserError(att + " empty");
		}
	}
	
	public MNote notifyError(String msg) {
		Integer exist = DB.getSQLValue(get_TrxName(), "SELECT Record_id FROM AD_Note WHERE AD_Table_ID = 319 AND Record_ID=? ", get_ID());
		if (exist<=0) {
			MNote note = new MNote(getCtx(), 0, null);
			note.setAD_Table_ID(319);
			note.setReference("Error en Entrega a cliente, por favor valide la info del documento: "+getDocumentNo());
			note.setAD_Org_ID(getAD_Org_ID());
			note.setTextMsg(msg);
			note.setAD_Message_ID("ErrorFE");
			note.setRecord(318, getC_Invoice_ID());
			note.setAD_User_ID(MSysConfig.getIntValue("ING_FEUserNotes", 100, getAD_Client_ID()));
			note.setDescription(getDocumentNo());
			note.saveEx();
			return note;
		}
		return null;
	}
	
}	// LEC_FE_MInOut