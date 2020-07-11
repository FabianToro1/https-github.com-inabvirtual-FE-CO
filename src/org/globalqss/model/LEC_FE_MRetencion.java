package org.globalqss.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocation;
import org.compiere.model.MNote;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MSequence;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Trx;
import org.globalqss.util.LEC_FE_Utils;
import org.globalqss.util.LEC_FE_UtilsXml;
import org.xml.sax.helpers.AttributesImpl;

import ec.ingeint.erp.model.X_LLA_WithholdingSequence;


/**
 *	LEC_FE_MInvoice
 *
 *  @author Carlos Ruiz - globalqss - Quality Systems & Solutions - http://globalqss.com 
 *  @version  $Id: LEC_FE_MNotaCredito.java,v 1.0 2014/05/06 03:37:29 cruiz Exp $
 */
public class LEC_FE_MRetencion extends MInvoice
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -924606040343895114L;
	
	private int		m_lec_sri_format_id = 0;

	private String file_name = "";
	private String m_obligadocontabilidad = "NO";
	private String m_coddoc = "";
	private String m_accesscode;
	private String m_identificacionconsumidor = "";
	private String m_tipoidentificacioncomprador = "";
	private String m_identificacioncomprador = "";
	private String m_razonsocial = "";
	private String m_retencionno = "";

	private BigDecimal m_totalbaseimponible = Env.ZERO;
	private BigDecimal m_totalvalorimpuesto = Env.ZERO;
	private BigDecimal m_sumabaseimponible = Env.ZERO;
	private BigDecimal m_sumavalorimpuesto = Env.ZERO;
	
	// 04/07/2016 MHG Offline Schema added
	private boolean isOfflineSchema = false;

	public LEC_FE_MRetencion(Properties ctx, int C_Invoice_ID, String trxName) {
		super(ctx, C_Invoice_ID, trxName);
		// 04/07/2016 MHG Offline Schema added
		isOfflineSchema=MSysConfig.getBooleanValue("QSSLEC_FE_OfflineSchema", false, Env.getAD_Client_ID(Env.getCtx()));
	}
	
	public String lecfeinvret_SriExportRetencionXML100 ()
	{
		
		int autorizationID = 0;
		String msg = null;
		String ErrorDocumentno = "Error en Factura No "+getDocumentNo()+" ";
		
		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();
		
		try
		{
			
		signature.setAD_Org_ID(getAD_Org_ID());
		
		m_identificacionconsumidor=MSysConfig.getValue("QSSLEC_FE_IdentificacionConsumidorFinal", null, getAD_Client_ID());
		
		m_razonsocial=MSysConfig.getValue("QSSLEC_FE_RazonSocialPruebas", null, getAD_Client_ID());
		
		signature.setPKCS12_Resource(MSysConfig.getValue("QSSLEC_FE_RutaCertificadoDigital", null, getAD_Client_ID(), getAD_Org_ID()));
		signature.setPKCS12_Password(MSysConfig.getValue("QSSLEC_FE_ClaveCertificadoDigital", null, getAD_Client_ID(), getAD_Org_ID()));
		
		if (signature.getFolderRaiz() == null)
			return ErrorDocumentno+"No existe parametro para Ruta Generacion Xml";
		
		MDocType dt = new MDocType(getCtx(), getC_DocTypeTarget_ID(), get_TrxName());
		
		m_coddoc = dt.get_ValueAsString("SRI_ShortDocType");
		
		if ( m_coddoc.equals(""))
			return ErrorDocumentno+"No existe definicion SRI_ShortDocType: " + dt.toString();
		
		// Formato
		m_lec_sri_format_id = LEC_FE_Utils.getLecSriFormat(getAD_Client_ID(), signature.getDeliveredType(), m_coddoc, getDateInvoiced(), getDateInvoiced());
				
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
		
		// Comprador
		MBPartner bp = new MBPartner(getCtx(), getC_BPartner_ID(), get_TrxName());
		if (!signature.isOnTesting()) m_razonsocial = bp.getName();
		
		X_LCO_TaxIdType ttc = new X_LCO_TaxIdType(getCtx(), (Integer) bp.get_Value("LCO_TaxIdType_ID"), get_TrxName());
		
		m_tipoidentificacioncomprador = LEC_FE_Utils.getTipoIdentificacionSri(ttc.get_Value("LEC_TaxCodeSRI").toString());
		
		m_identificacioncomprador = bp.getTaxID();
		
		m_retencionno = DB.getSQLValueString(get_TrxName(), "SELECT DISTINCT(DocumentNo) FROM LCO_InvoiceWithholding WHERE C_Invoice_ID = ? ", getC_Invoice_ID());
		if (m_retencionno.length()<17)
			return "Error en longitud del número de retención";
		// IsUseContingency
		int sri_accesscode_id = 0;
		if (signature.IsUseContingency) {
			sri_accesscode_id = LEC_FE_Utils.getNextAccessCode(getAD_Client_ID(), signature.getEnvType(), oi.getTaxID(), get_TrxName());
			if ( sri_accesscode_id < 1)
				return ErrorDocumentno+"No hay clave de contingencia para el comprobante";
		}
		
		// New/Upd Access Code
				
		X_SRI_AccessCode ac = null;
		ac = new X_SRI_AccessCode (getCtx(), sri_accesscode_id, get_TrxName());
		ac.setAD_Org_ID(getAD_Org_ID());
		ac.setOldValue(null);	// Deprecated
		ac.setEnvType(signature.getEnvType());
		ac.setCodeAccessType(signature.getCodeAccessType());
		ac.setSRI_ShortDocType(m_coddoc);
		ac.setIsUsed(true);
		
		// Access Code
		m_accesscode = LEC_FE_Utils.getAccessCode(getDateInvoiced(), m_coddoc, bpe.getTaxID(), oi.get_ValueAsString("SRI_OrgCode"), 
				LEC_FE_Utils.getStoreCode(LEC_FE_Utils.formatDocNo(m_retencionno, m_coddoc)), m_retencionno, oi.get_ValueAsString("SRI_DocumentCode"), 
				signature.getDeliveredType(), ac);

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
		a.set_ValueOfColumn("C_Invoice_ID", getC_Invoice_ID());
		
		if (!a.save()) {
			msg = "@SaveError@ No se pudo grabar SRI Autorizacion";
			return ErrorDocumentno+msg;
		}
		
		autorizationID = a.get_ID();
					
		OutputStream  mmDocStream = null;
		
 		String xmlFileName = "SRI_" + m_coddoc + "-" + LEC_FE_Utils.getDate(getDateInvoiced(),9) + "-" + m_accesscode + ".xml";
	
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
			// Numérico49
			addHeaderElement(mmDoc, "claveAcceso", a.getValue(), atts);
			// Numerico2
			addHeaderElement(mmDoc, "codDoc", m_coddoc, atts);
			// Numerico3
			addHeaderElement(mmDoc, "estab", oi.get_ValueAsString("SRI_OrgCode"), atts);
			// Numerico3
			addHeaderElement(mmDoc, "ptoEmi", LEC_FE_Utils.getStoreCode(LEC_FE_Utils.formatDocNo(m_retencionno, m_coddoc)), atts);
			// Numerico9
			addHeaderElement(mmDoc, "secuencial", (LEC_FE_Utils.fillString(9 - (LEC_FE_Utils.cutString(LEC_FE_Utils.getSecuencial(m_retencionno, m_coddoc), 9)).length(), '0'))
					+ LEC_FE_Utils.cutString(LEC_FE_Utils.getSecuencial(m_retencionno, m_coddoc), 9), atts);
			// dirMatriz ,Alfanumerico Max 300
			addHeaderElement(mmDoc, "dirMatriz", lm.getAddress1(), atts);
		mmDoc.endElement("","","infoTributaria");
		
		mmDoc.startElement("","","infoCompRetencion",atts);
		// Emisor
			// Fecha8 ddmmaaaa
			addHeaderElement(mmDoc, "fechaEmision", LEC_FE_Utils.getDate(getDateAcct(),10), atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "dirEstablecimiento", lo.getAddress1(), atts);
			// Numerico3-5
			addHeaderElement(mmDoc, "contribuyenteEspecial", oi.get_ValueAsString("SRI_TaxPayerCode"), atts);
			// Texto2
			addHeaderElement(mmDoc, "obligadoContabilidad", m_obligadocontabilidad, atts);
		// Comprador
			// Numerico2
			addHeaderElement(mmDoc, "tipoIdentificacionSujetoRetenido", m_tipoidentificacioncomprador, atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "razonSocialSujetoRetenido", m_razonsocial, atts);
			// Numerico Max 13
			addHeaderElement(mmDoc, "identificacionSujetoRetenido", m_identificacioncomprador, atts);
			// Fecha6 mm/aaaa
			addHeaderElement(mmDoc, "periodoFiscal", LEC_FE_Utils.getDate(getDateInvoiced(),7), atts);
		
		mmDoc.endElement("","","infoCompRetencion");
		
		m_totalbaseimponible = getGrandTotal().add((BigDecimal) get_Value("WithholdingAmt"));
		m_totalvalorimpuesto = (BigDecimal) get_Value("WithholdingAmt");
				
		// Impuestos
		sql = new StringBuffer(
	            "SELECT i.C_Invoice_ID, dt.SRI_ShortDocType AS codDocSustento, i.DocumentNo AS numDocSustento, "
	            + "i.DateInvoiced AS fechaEmisionDocSustento "
	            + ", COALESCE(tc.SRI_TaxCodeValue, '0') AS codigo "
	    		+ ", COALESCE(tr.SRI_TaxRateValue, 'X') AS codigoPorcentaje "
				+ ", iwh.TaxBaseAmt AS baseImponible "
				+ ", t.Rate AS porcentajeRetener "
				+ ", iwh.TaxAmt AS valorRetenido "
	            + "FROM C_Invoice i "
	            + "JOIN C_DocType dt ON dt.C_DocType_ID = i.C_DocTypeTarget_ID "
	            + "JOIN LCO_InvoiceWithholding iwh ON iwh.C_Invoice_ID = i.C_Invoice_ID "
	            + "LEFT JOIN LCO_WithholdingRule whr ON iwh.LCO_WithholdingRule_ID = whr.LCO_WithholdingRule_ID "
	            + "JOIN C_Tax t ON t.C_Tax_ID = iwh.C_Tax_ID "
	            + "LEFT JOIN SRI_TaxCode tc ON t.SRI_TaxCode_ID = tc.SRI_TaxCode_ID "
				+ "LEFT JOIN SRI_TaxRate tr ON t.SRI_TaxRate_ID = tr.SRI_TaxRate_ID "
	            + "WHERE i.C_Invoice_ID=? "
	            + "ORDER BY tc.SRI_TaxCodeValue");
		
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setInt(1, getC_Invoice_ID());
			ResultSet rs = pstmt.executeQuery();
			//
			
			mmDoc.startElement("","","impuestos",atts);
			
			while (rs.next())
			{
				if (rs.getString(5).equals("0") || rs.getString(6).equals("X") ) {
					msg = "Impuesto sin Tipo ó Porcentaje impuesto SRI";
					DB.executeUpdate("DELETE FROM SRI_Authorization WHERE SRI_Authorization_ID = "+a.get_ID(), true, get_TrxName());
						return ErrorDocumentno+msg;
					}
					
					mmDoc.startElement("","","impuesto",atts);
						// Numerico 1
						addHeaderElement(mmDoc, "codigo", rs.getString(5), atts);
						// Numerico 1 to 4
						addHeaderElement(mmDoc, "codigoRetencion", rs.getString(6), atts);
						// Numerico Max 14
						addHeaderElement(mmDoc, "baseImponible", rs.getBigDecimal(7).toString(), atts);
						// Numerico 1 to 3
						addHeaderElement(mmDoc, "porcentajeRetener", rs.getBigDecimal(8).toString(), atts);
						// Numerico Max 14
						addHeaderElement(mmDoc, "valorRetenido", rs.getBigDecimal(9).toString(), atts);
						// Numerico2
						if (rs.getString(2).equals("07"))
							addHeaderElement(mmDoc, "codDocSustento", "01", atts);	// Hardcoded
						// Numerico15 -- Sin guiones
						addHeaderElement(mmDoc, "numDocSustento", LEC_FE_Utils.replaceGuion(LEC_FE_Utils.formatDocNo(rs.getString(3), "01")), atts);
						// Fecha8 ddmmaaaa
						addHeaderElement(mmDoc, "fechaEmisionDocSustento", LEC_FE_Utils.getDate(rs.getDate(4),10), atts);
					mmDoc.endElement("","","impuesto");
				
				m_sumabaseimponible = m_sumabaseimponible.add(rs.getBigDecimal(7));
				m_sumavalorimpuesto = m_sumavalorimpuesto.add(rs.getBigDecimal(9));
				
			}
			
			mmDoc.endElement("","","impuestos");
			
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
		
		if (m_sumabaseimponible.compareTo(m_totalbaseimponible) != 0
			&& m_totalbaseimponible.subtract(m_sumabaseimponible).abs().compareTo(LEC_FE_UtilsXml.HALF) > 1) {
			msg = "Error Diferencia Base Impuesto Total: " + m_totalbaseimponible.toString() + " Detalles: " + m_sumabaseimponible.toString();
			return ErrorDocumentno+msg;
		}
			
		if (m_sumavalorimpuesto.compareTo(m_totalvalorimpuesto) != 0
			&& m_totalvalorimpuesto.subtract(m_sumavalorimpuesto).abs().compareTo(LEC_FE_UtilsXml.HALF) > 1) {
			msg = "Error Diferencia Impuesto Total: " + m_totalvalorimpuesto.toString() + " Detalles: " + m_sumavalorimpuesto.toString();
			return ErrorDocumentno+msg;
		}
	
//		if (LEC_FE_Utils.breakDialog("Firmando Xml")) return "Cancelado...";	// TODO Temp
		
		log.warning("@Signing Xml@ -> " + file_name);
		signature.setResource_To_Sign(file_name);
		signature.setOutput_Directory(signature.getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesFirmados);
        signature.execute();
        
        file_name = signature.getFilename(signature, LEC_FE_UtilsXml.folderComprobantesFirmados);
        
        if (! signature.IsUseContingency) {
	        
//        	if (LEC_FE_Utils.breakDialog("Enviando Comprobante al SRI")) return "Cancelado...";	// TODO Temp
	        
	        // Procesar Recepcion SRI
        	log.warning("@Sending Xml@ -> " + file_name);
	        msg = signature.respuestaRecepcionComprobante(file_name);
	        
	    	if (msg != null)
	        	if (!msg.equals("RECIBIDA")){
	        			String DocumentNo = DB.getSQLValueString(get_TrxName(), "SELECT Description FROM AD_Note WHERE Description = ?", getDocumentNo());
	        			DB.executeUpdate("DELETE FROM SRI_Authorization WHERE SRI_Authorization_ID = "+a.get_ID(), true, get_TrxName());
	        			if (DocumentNo==null) {
		        			MNote note = new MNote(getCtx(), 0, null);
		        			note.setAD_Table_ID(318);
		        			note.setReference(ErrorDocumentno+ " de compra, por favor valide la info de retencion");
		        			note.setAD_Org_ID(getAD_Org_ID());
		        			note.setTextMsg(msg);
		        			note.setAD_Message_ID("ErrorFE");
		        			note.setRecord(318, getC_Invoice_ID());
		        			note.setAD_User_ID(MSysConfig.getIntValue("ING_FEUserNotes", 100, getAD_Client_ID()));
		        			note.setDescription(getDocumentNo());
		        			note.saveEx();
	        			}
	        			        		
	        		return ErrorDocumentno+msg;
	        	}
	        String invoiceNo = getDocumentNo();		
	        String invoiceID = String.valueOf(get_ID());
	        a.setDescription(invoiceNo);
	        a.set_ValueOfColumn("DocumentID", invoiceID);
	        a.saveEx();
        		        
	        // Procesar Autorizacion SRI
        	// 04/07/2016 MHG Offline Schema added
        	if (!isOfflineSchema) {
		        log.warning("@Authorizing Xml@ -> " + file_name);
		        try {
		        	msg = signature.respuestaAutorizacionComprobante(ac, a, m_accesscode);
		        	
		        	if (msg != null){
		        		return ErrorDocumentno+msg;
		        	}
		        } catch (Exception ex) {
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
		
//		if (LEC_FE_Utils.breakDialog("Completando Retencion")) return "Cancelado...";	// TODO Temp

		//
		}
		catch (Exception e)
		{
			msg = "No se pudo crear XML - " + e.getMessage();
			log.severe(msg);
			
			return ErrorDocumentno+msg;
		}catch (Error e) {
			msg = "No se pudo crear XML- Error en Conexion con el SRI";
			return ErrorDocumentno+msg;
		}
		
		log.warning("@SRI_FileGenerated@ -> " + file_name);
		
		set_Value("SRI_Authorization_ID",autorizationID);
		this.saveEx();
		
		return msg;
	
	} // lecfeinvret_SriExportRetencionXML100
	
	public void addHeaderElement(TransformerHandler mmDoc, String att, String value, AttributesImpl atts) throws Exception {
		if (att != null) {
			mmDoc.startElement("","",att,atts);
			mmDoc.characters(value.toCharArray(),0,value.toCharArray().length);
			mmDoc.endElement("","",att);
		} else {
			throw new AdempiereUserError(att + " empty");
		}
	}
	
	public static String generateWitholdingNo(MInvoice invoice) {
		
		String generalSequenceNo = "";
		String othersequenceno = "";
		
		if (!invoice.get_ValueAsBoolean("IsGenerated")){
			List<MLCOInvoiceWithholding> withholdings = new Query(invoice.getCtx(), MLCOInvoiceWithholding.Table_Name, "C_Invoice_ID=?", invoice.get_TrxName())
			.setParameters(invoice.getC_Invoice_ID())
			.setOnlyActiveRecords(true)
			.list();
			
			boolean isGeneralSequence = false;
			List<KeyNamePair> wthtypesequences = new ArrayList<KeyNamePair>();
			
			for (MLCOInvoiceWithholding wth : withholdings) {
				X_LLA_WithholdingSequence wtsequence = getSqlToWithholdingSequenceNo(wth, " AND IsGeneralWithholding = 'N' AND LCO_WithholdingType_ID = "+wth.getLCO_WithholdingType_ID());
				boolean othersequence = false;
				
				for (KeyNamePair keysequence : wthtypesequences) {
					if (keysequence.getID().equals(wtsequence.get_ID())){
						othersequenceno = keysequence.getName();
						othersequence = true;
					}
				}				
				
				if (othersequence)
					wth.setDocumentNo(othersequenceno);
				else if(!othersequence && wtsequence != null){
					othersequence = true;
					MSequence sq = new MSequence(wtsequence.getCtx(), wtsequence.getAD_Sequence_ID(), wtsequence.get_TrxName());
					othersequenceno = MSequence.getDocumentNoFromSeq(sq,wtsequence.get_TrxName(),wth);
					KeyNamePair seq = new KeyNamePair(wtsequence.getLCO_WithholdingType_ID(), othersequenceno);
					wthtypesequences.add(seq);
					wth.setDocumentNo(othersequenceno);
				}
				else if (isGeneralSequence)
					wth.setDocumentNo(generalSequenceNo);
				else if (!isGeneralSequence) {
					
					X_LLA_WithholdingSequence wsequence  = getSqlToWithholdingSequenceNo(wth, null);
					
					if (wsequence != null){
						isGeneralSequence = true;
						MSequence sq = new MSequence(wsequence.getCtx(), wsequence.getAD_Sequence_ID(), wsequence.get_TrxName());
						generalSequenceNo = MSequence.getDocumentNoFromSeq(sq,wsequence.get_TrxName(),wth);
						wth.setDocumentNo(generalSequenceNo);
					}
				}
				
				wth.saveEx();
				
				if (!generalSequenceNo.equals("") || !othersequenceno.equals("")){
					invoice.set_ValueOfColumn("IsGenerated", true);
					invoice.saveEx();
				}
				
			}
		}
		
		String nosecuencia = "";
		if (!othersequenceno.equalsIgnoreCase(""))
			nosecuencia = othersequenceno+" - ";
		
		if (!generalSequenceNo.equalsIgnoreCase(""))
			nosecuencia += generalSequenceNo;

		return "No. de Retención Generado "+nosecuencia;
	}
	
	private static X_LLA_WithholdingSequence getSqlToWithholdingSequenceNo(MLCOInvoiceWithholding wth, String whereclause) {
		X_LLA_WithholdingSequence wsequence = null;
		StringBuffer sql = new StringBuffer("SELECT LLA_WithholdingSequence_ID  ");
		sql.append("FROM LLA_WithholdingSequence x ");
		sql.append("WHERE x.IsActive='Y' ");
		sql.append("AND x.AD_Org_ID = ? ");
		sql.append("AND x.IsSOTrx = ? ");
		sql.append("AND ? BETWEEN x.ValidFrom AND x.ValidTo ");
		
		if (whereclause != null)
			sql.append(whereclause);
		else
			sql.append("AND IsGeneralWithholding = 'Y'");
		
		int LLA_WithholdingSequence_ID = DB.getSQLValue(wth.get_TrxName(), sql.toString(), 
				new Object[] {wth.getAD_Org_ID(), wth.getC_Invoice().isSOTrx(), wth.getDateAcct()});

		if (LLA_WithholdingSequence_ID > 0)
		wsequence = new X_LLA_WithholdingSequence(wth.getCtx(), LLA_WithholdingSequence_ID, wth.get_TrxName());
		
		return wsequence;
	}
	
	/**
	 * generateWitholdingNo
	 * 
	 * @param MLCOInvoiceWithholding iwh
	 * 
	 * @return SequenceNo
	 */
	public static String generateWitholdingNo(MLCOInvoiceWithholding iwh) {

		String SequenceNo = "";

		X_LLA_WithholdingSequence wtsequence = getSqlToWithholdingSequenceNo(
				iwh,
				" AND IsGeneralWithholding = 'N' AND LCO_WithholdingType_ID = "
						+ iwh.getLCO_WithholdingType_ID());

		if (wtsequence != null) {
			MSequence sq = new MSequence(wtsequence.getCtx(),
					wtsequence.getAD_Sequence_ID(), wtsequence.get_TrxName());
			SequenceNo = MSequence.getDocumentNoFromSeq(sq,
					wtsequence.get_TrxName(), iwh);
		} else {
			X_LLA_WithholdingSequence wsequence = getSqlToWithholdingSequenceNo(
					iwh, null);

			if (wsequence != null) {
				MSequence sq = new MSequence(wsequence.getCtx(),
						wsequence.getAD_Sequence_ID(), wsequence.get_TrxName());
				SequenceNo = MSequence.getDocumentNoFromSeq(sq,
						wsequence.get_TrxName(), iwh);

			}
		}

		return SequenceNo;
	}// generateWitholdingNo
	
}	// LEC_FE_MRetencion