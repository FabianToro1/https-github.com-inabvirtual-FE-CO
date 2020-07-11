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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInvoice;
import org.compiere.model.MLocation;
import org.compiere.model.MMailText;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MSysConfig;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.globalqss.util.LEC_FE_Utils;
import org.globalqss.util.LEC_FE_UtilsXml;
import org.xml.sax.helpers.AttributesImpl;

import com.sun.jmx.snmp.Timestamp;

import org.globalqss.model.X_LCO_TaxPayerType;


/**
 *	LEC_FE_MInvoice
 *
 *  @author Carlos Ruiz - globalqss - Quality Systems & Solutions - http://globalqss.com 
 *  @version  $Id: LEC_FE_MNotaCredito.java,v 1.0 2014/05/06 03:37:29 cruiz Exp $
 */
public class LEC_FE_MNotaCredito extends MInvoice
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
	private String m_identificacioncomprador = "";
	private String m_razonsocial = "";

	private BigDecimal m_totaldescuento = Env.ZERO;
	private BigDecimal m_totalbaseimponible = Env.ZERO;
	private BigDecimal m_totalvalorimpuesto = Env.ZERO;
	private BigDecimal m_sumadescuento = Env.ZERO;
	private BigDecimal m_sumabaseimponible = Env.ZERO;
	private BigDecimal m_sumavalorimpuesto = Env.ZERO;
	private String m_trxAutorizacionName = null;

	// 04/07/2016 MHG Offline Schema added
	private boolean isOfflineSchema = false;

	public LEC_FE_MNotaCredito(Properties ctx, int C_Invoice_ID, String trxName) {
		super(ctx, C_Invoice_ID, trxName);
		// 04/07/2016 MHG Offline Schema added
		isOfflineSchema=MSysConfig.getBooleanValue("QSSLEC_FE_OfflineSchema", false, Env.getAD_Client_ID(Env.getCtx()));
	}

	public String lecfeinvnc_SriExportNotaCreditoXML100 ()
	{
		Trx autorizacionTrx = null;
		m_trxAutorizacionName=null;
		int autorizationID = 0;	
		String msg = null;
		String ErrorDocumentno = "Error en Nota de Crédito No "+getDocumentNo()+" ";  

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
				throw new AdempiereUserError(ErrorDocumentno+"No existe formato para el comprobante");

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

			X_LCO_TaxIdType tt = new X_LCO_TaxIdType(getCtx(), (Integer) bp.get_Value("LCO_TaxIdType_ID"), get_TrxName());
			if (tt.getLCO_TaxIdType_ID() == 1000011)	// Hardcoded F Final	// TODO Deprecated
				m_identificacioncomprador = m_identificacionconsumidor;

			X_LCO_TaxPayerType tp = new X_LCO_TaxPayerType(getCtx(), bp.get_ValueAsInt("LCO_TaxPayerType_ID"), get_TrxName());		

			if (get_Value("SRI_RefInvoice_ID") == null && get_Value("InvoiceDocumentReference") == null)
				return ErrorDocumentno+"No existe documento sustento para el comprobante";

			MInvoice invsus = null;
			String DocumentRefNo = "";
			java.sql.Timestamp DocRefDate = null;

			if (get_Value("SRI_RefInvoice_ID")!=null) {
				m_c_invoice_sus_id = (Integer) get_Value("SRI_RefInvoice_ID");
				invsus = new MInvoice(getCtx(), m_c_invoice_sus_id, get_TrxName());
				DocumentRefNo = invsus.getDocumentNo();
				DocRefDate = invsus.getDateAcct();
			}else {
				DocumentRefNo = (String) get_Value("InvoiceDocumentReference");
				DocRefDate = (java.sql.Timestamp) get_Value("InvoiceDocumentReferenceDate");
			}

			m_totaldescuento = Env.ZERO; // DB.getSQLValueBD(get_TrxName(), "SELECT COALESCE(SUM(ilt.discount), 0) FROM c_invoice_linetax_vt ilt WHERE ilt.C_Invoice_ID = ? ", getC_Invoice_ID());

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
			if (m_trxAutorizacionName == null)
			{
				StringBuilder l_trxname = new StringBuilder("SRI_")
						.append(get_TableName()).append(get_ID());
				m_trxAutorizacionName = Trx.createTrxName(l_trxname.toString());
				autorizacionTrx = Trx.get(m_trxAutorizacionName, true);
			}
			X_SRI_AccessCode ac = null;
			if (autorizacionTrx!=null)
				ac = new X_SRI_AccessCode (getCtx(), sri_accesscode_id,autorizacionTrx.getTrxName());
			else
				ac = new X_SRI_AccessCode (getCtx(), sri_accesscode_id, get_TrxName());

			ac.setAD_Org_ID(getAD_Org_ID());
			ac.setOldValue(null);	// Deprecated
			ac.setEnvType(signature.getEnvType());
			ac.setCodeAccessType(signature.getCodeAccessType());
			ac.setSRI_ShortDocType(m_coddoc);
			ac.setIsUsed(true);

			// Access Code
			m_accesscode = LEC_FE_Utils.getAccessCode(getDateInvoiced(), m_coddoc, bpe.getTaxID(), oi.get_ValueAsString("SRI_OrgCode"), LEC_FE_Utils.getStoreCode(LEC_FE_Utils.formatDocNo(getDocumentNo(), m_coddoc)), getDocumentNo(), oi.get_ValueAsString("SRI_DocumentCode"), signature.getDeliveredType(), ac);

			if (signature.getCodeAccessType().equals(LEC_FE_UtilsXml.claveAccesoAutomatica))
				ac.setValue(m_accesscode);

			if (!ac.save()) {
				msg = "@SaveError@ No se pudo grabar SRI Access Code";
				return ErrorDocumentno+msg;
			}

			// New Authorization
			X_SRI_Authorization a = null;
			if (autorizacionTrx!=null)
				a = new X_SRI_Authorization (getCtx(), 0, autorizacionTrx.getTrxName());
			else
				a = new X_SRI_Authorization (getCtx(), 0,get_TrxName());
			a.setAD_Org_ID(getAD_Org_ID());
			a.setSRI_ShortDocType(m_coddoc);
			a.setValue(m_accesscode);
			a.setSRI_AuthorizationCode(null);
			a.setSRI_AccessCode_ID(ac.get_ID());
			a.setSRI_ErrorCode_ID(0);
			a.setAD_UserMail_ID(getAD_User_ID());
			a.set_ValueOfColumn("isSRIOfflineSchema", isOfflineSchema);

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
			addHeaderElement(mmDoc, "ptoEmi", LEC_FE_Utils.getStoreCode(LEC_FE_Utils.formatDocNo(getDocumentNo(), m_coddoc)), atts);
			// Numerico9
			addHeaderElement(mmDoc, "secuencial", (LEC_FE_Utils.fillString(9 - (LEC_FE_Utils.cutString(LEC_FE_Utils.getSecuencial(getDocumentNo(), m_coddoc), 9)).length(), '0'))
					+ LEC_FE_Utils.cutString(LEC_FE_Utils.getSecuencial(getDocumentNo(), m_coddoc), 9), atts);
			// dirMatriz ,Alfanumerico Max 300
			addHeaderElement(mmDoc, "dirMatriz", lm.getAddress1(), atts);
			mmDoc.endElement("","","infoTributaria");

			mmDoc.startElement("","","infoNotaCredito",atts);
			// Emisor
			// Fecha8 ddmmaaaa
			addHeaderElement(mmDoc, "fechaEmision", LEC_FE_Utils.getDate(getDateInvoiced(),10), atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "dirEstablecimiento", lo.getAddress1(), atts);
			// Comprador
			// Numerico2
			addHeaderElement(mmDoc, "tipoIdentificacionComprador", m_tipoidentificacioncomprador, atts);
			// Alfanumerico Max 300
			addHeaderElement(mmDoc, "razonSocialComprador", m_razonsocial, atts);
			// Numerico Max 13
			addHeaderElement(mmDoc, "identificacionComprador", m_identificacioncomprador, atts);
			// Numerico3-5
			addHeaderElement(mmDoc, "contribuyenteEspecial", oi.get_ValueAsString("SRI_TaxPayerCode"), atts);
			// Texto2
			addHeaderElement(mmDoc, "obligadoContabilidad", m_obligadocontabilidad, atts);
			// Alfanumerico Max 40
			addHeaderElement(mmDoc, "rise", LEC_FE_Utils.cutString(tp.getName(), 40), atts);
			// Numerico2
			if (m_coddoc.equals("04"))
				addHeaderElement(mmDoc, "codDocModificado", "01", atts);	// Hardcoded
			// Numerico15 -- Incluye guiones
			addHeaderElement(mmDoc, "numDocModificado", LEC_FE_Utils.formatDocNo(DocumentRefNo, "01"), atts);
			// Fecha8 ddmmaaaa
			addHeaderElement(mmDoc, "fechaEmisionDocSustento", LEC_FE_Utils.getDate(DocRefDate,10), atts);
			// Numerico Max 14
			addHeaderElement(mmDoc, "totalSinImpuestos", getTotalLines().toString(), atts);
			// Numerico MAx 14
			addHeaderElement(mmDoc, "valorModificacion", getGrandTotal().toString(), atts);
			// Alfanumerico MAx 25
			addHeaderElement(mmDoc, "moneda", getCurrencyISO(), atts);

			// Impuestos
			mmDoc.startElement("","","totalConImpuestos",atts);

			sql = new StringBuffer(
					"SELECT COALESCE(tc.SRI_TaxCodeValue, '0') AS codigo "
							+ ", COALESCE(tr.SRI_TaxRateValue, 'X') AS codigoPorcentaje "
							+ ", SUM(it.TaxBaseAmt) AS baseImponible "
							+ ", SUM(it.TaxAmt) AS valor "
							+ ", 0::numeric AS descuentoAdicional "
							+ "FROM C_Invoice i "
							+ "JOIN C_InvoiceTax it ON it.C_Invoice_ID = i.C_Invoice_ID "
							+ "JOIN C_Tax t ON t.C_Tax_ID = it.C_Tax_ID "
							+ "LEFT JOIN SRI_TaxCode tc ON t.SRI_TaxCode_ID = tc.SRI_TaxCode_ID "
							+ "LEFT JOIN SRI_TaxRate tr ON t.SRI_TaxRate_ID = tr.SRI_TaxRate_ID "
							+ "WHERE i.C_Invoice_ID = ? "
							+ "GROUP BY codigo, codigoPorcentaje "
							+ "ORDER BY codigo, codigoPorcentaje");

			try
			{
				PreparedStatement pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
				pstmt.setInt(1, getC_Invoice_ID());
				ResultSet rs = pstmt.executeQuery();
				//

				while (rs.next())
				{
					if (rs.getString(1).equals("0") || rs.getString(2).equals("X") ) {
						msg = "Impuesto sin Tipo ó Porcentaje impuesto SRI";
						return ErrorDocumentno+msg;
					}

					mmDoc.startElement("","","totalImpuesto",atts);

					// Numerico 1
					addHeaderElement(mmDoc, "codigo", rs.getString(1), atts);
					// Numerico 1 to 4
					addHeaderElement(mmDoc, "codigoPorcentaje", rs.getString(2), atts);
					// Numerico Max 14
					addHeaderElement(mmDoc, "baseImponible", rs.getBigDecimal(3).toString(), atts);
					// Numerico Max 14
					addHeaderElement(mmDoc, "valor", rs.getBigDecimal(4).toString(), atts);

					mmDoc.endElement("","","totalImpuesto");

					m_totalbaseimponible = m_totalbaseimponible.add(rs.getBigDecimal(3));
					m_totalvalorimpuesto = m_totalvalorimpuesto.add(rs.getBigDecimal(4));

				}
				rs.close();
				pstmt.close();
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, sql.toString(), e);
				msg = "Error SQL: " + sql.toString();
				return ErrorDocumentno+msg;
			}

			mmDoc.endElement("","","totalConImpuestos");

			// Alfanumerico MAx 300
			if (getDescription()==null){
				msg = "Debe agregar un Motivo";
				return ErrorDocumentno+msg;
			}
			addHeaderElement(mmDoc, "motivo", LEC_FE_Utils.cutString(getDescription(),300), atts);

			mmDoc.endElement("","","infoNotaCredito");

			// Detalles
			mmDoc.startElement("","","detalles",atts);

			sql = new StringBuffer();

			if (!dt.get_ValueAsBoolean("IsSummaryInvoice")) {

				sql = new StringBuffer(
						"SELECT i.C_Invoice_ID, COALESCE(p.value, '0'), 0::text, ilt.name, ilt.qtyinvoiced, ROUND(ilt.priceactual, 2), COALESCE(0, ilt.discount), ilt.linenetamt "
								+ ", COALESCE(tc.SRI_TaxCodeValue, '0') AS codigo "
								+ ", COALESCE(tr.SRI_TaxRateValue, 'X') AS codigoPorcentaje "
								+ ", t.rate AS tarifa "
								+ ", ilt.linenetamt AS baseImponible "
								+ ", ROUND(ilt.linenetamt * t.rate / 100, 2) AS valor "
								+ ", il.description AS description1 "
								+ ", 0::numeric AS descuentoAdicional "
								+ "FROM C_Invoice i "
								+ "JOIN C_InvoiceLine il ON il.C_Invoice_ID = i.C_Invoice_ID "
								+ "JOIN C_Invoice_LineTax_VT ilt ON ilt.C_InvoiceLine_ID = il.C_InvoiceLine_ID "
								+ "JOIN C_Tax t ON t.C_Tax_ID = ilt.C_Tax_ID "
								+ "LEFT JOIN SRI_TaxCode tc ON t.SRI_TaxCode_ID = tc.SRI_TaxCode_ID "
								+ "LEFT JOIN SRI_TaxRate tr ON t.SRI_TaxRate_ID = tr.SRI_TaxRate_ID "
								+ "LEFT JOIN M_Product p ON p.M_Product_ID = il.M_Product_ID "
								+ "LEFT JOIN M_Product_Category pc ON pc.M_Product_Category_ID = p.M_Product_Category_ID "
								+ "LEFT JOIN C_Charge c ON il.C_Charge_ID = c.C_Charge_ID "
								+ "WHERE il.IsDescription = 'N' AND i.C_Invoice_ID=? " );

			} else {

				sql = new StringBuffer("SELECT i.C_Invoice_ID, COALESCE(p.value, '0'), 0::text, ilt.name, "
						+ "sum(ilt.qtyinvoiced) as qtyinvoiced, ROUND(ilt.priceactual, 2), COALESCE(0, ilt.discount), sum(ilt.linenetamt)  as linenetamt \n" + 
						", COALESCE(tc.SRI_TaxCodeValue, '0') AS codigo \n" + 
						", COALESCE(tr.SRI_TaxRateValue, 'X') AS codigoPorcentaje \n" + 
						", t.rate AS tarifa \n" + 
						", sum(ilt.linenetamt) AS baseImponible \n" + 
						", ROUND(ilt.linenetamt * t.rate / 100, 2) AS valor \n" + 
						", il.description AS description1 \n" + 
						", 0::numeric AS descuentoAdicional \n" + 
						"FROM C_Invoice i \n" + 
						"JOIN C_InvoiceLine il ON il.C_Invoice_ID = i.C_Invoice_ID \n" + 
						"JOIN C_Invoice_LineTax_VT ilt ON ilt.C_InvoiceLine_ID = il.C_InvoiceLine_ID \n" + 
						"JOIN C_Tax t ON t.C_Tax_ID = ilt.C_Tax_ID \n" + 
						"LEFT JOIN SRI_TaxCode tc ON t.SRI_TaxCode_ID = tc.SRI_TaxCode_ID \n" + 
						"LEFT JOIN SRI_TaxRate tr ON t.SRI_TaxRate_ID = tr.SRI_TaxRate_ID \n" + 
						"LEFT JOIN M_Product p ON p.M_Product_ID = il.M_Product_ID \n" + 
						"LEFT JOIN M_Product_Category pc ON pc.M_Product_Category_ID = p.M_Product_Category_ID \n" + 
						"LEFT JOIN C_Charge c ON il.C_Charge_ID = c.C_Charge_ID \n" + 
						"WHERE il.IsDescription = 'N' AND i.C_Invoice_ID=? \n" + 
						"GROUP By COALESCE(p.value, '0'), (case  WHEN p.m_product_id isnull THEN '0' when p.upc isnull or p.upc ='' then p.value else p.upc END), \n" + 
						"ilt.name, i.c_invoice_id, ilt.priceentered, il.discountamt,ilt.linenetamt, tc.SRI_TaxCodeValue, tr.SRI_TaxRateValue, t.rate, il.description, il.c_uom_id \n" + 
						", ilt.priceactual, ilt.discount");		
			}

			try
			{
				PreparedStatement pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
				pstmt.setInt(1, getC_Invoice_ID());
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
					// Numerico Max 14
					addHeaderElement(mmDoc, "precioUnitario", rs.getBigDecimal(6).toString(), atts);
					// Numerico Max 14
					addHeaderElement(mmDoc, "descuento", rs.getBigDecimal(7).toString(), atts);
					// Numerico Max 14
					addHeaderElement(mmDoc, "precioTotalSinImpuesto", rs.getBigDecimal(8).toString(), atts);
					/*
				if (rs.getString(14) != null)  {
					mmDoc.startElement("","","detallesAdicionales",atts);

					atts.clear();
					atts.addAttribute("", "", "nombre", "CDATA", "descripcion1");
					atts.addAttribute("", "", "valor", "CDATA", LEC_FE_Utils.cutString(rs.getString(14),300));
					mmDoc.startElement("", "", "detAdicional", atts);
					mmDoc.endElement("","","detAdicional");

					mmDoc.endElement("","","detallesAdicionales");
				}
					 */
					atts.clear();
					//
					mmDoc.startElement("","","impuestos",atts);
					if (rs.getString(9).equals("0") || rs.getString(10).equals("X") ) {
						msg = "Impuesto sin Tipo ó Porcentaje impuesto SRI";
						return ErrorDocumentno+msg;
					}

					mmDoc.startElement("","","impuesto",atts);
					// Numerico 1
					addHeaderElement(mmDoc, "codigo", rs.getString(9), atts);
					// Numerico 1 to 4
					addHeaderElement(mmDoc, "codigoPorcentaje", rs.getString(10), atts);
					// Numerico 1 to 4
					addHeaderElement(mmDoc, "tarifa", rs.getBigDecimal(11).toString(), atts);
					// Numerico Max 14
					addHeaderElement(mmDoc, "baseImponible", rs.getBigDecimal(12).toString(), atts);
					// Numerico Max 14
					addHeaderElement(mmDoc, "valor", rs.getBigDecimal(13).toString(), atts);
					mmDoc.endElement("","","impuesto");
					mmDoc.endElement("","","impuestos");

					mmDoc.endElement("","","detalle");

					m_sumadescuento = m_sumadescuento.add(rs.getBigDecimal(7));
					m_sumabaseimponible = m_sumabaseimponible.add(rs.getBigDecimal(12));
					m_sumavalorimpuesto = m_sumavalorimpuesto.add(rs.getBigDecimal(13));

				}
				rs.close();
				pstmt.close();
			}
			catch (SQLException e)
			{
				log.log(Level.SEVERE, sql.toString(), e);
				msg = "Error SQL: " + sql.toString();
				return ErrorDocumentno+msg;
			}

			mmDoc.endElement("","","detalles");
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

			if (m_sumadescuento.compareTo(m_totaldescuento) != 0 ) {
				msg = "Error Diferencia Descuento Total: " + m_totaldescuento.toString() + " Detalles: " + m_sumadescuento.toString();
				return ErrorDocumentno+msg;
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

				//            if (LEC_FE_Utils.breakDialog("Enviando Comprobante al SRI")) return "Cancelado...";	// Temp

				// Procesar Recepcion SRI
				log.warning("@Sending Xml@ -> " + file_name);
				msg = signature.respuestaRecepcionComprobante(file_name);

				if (msg != null)
					if (!msg.equals("RECIBIDA")){
						if (autorizacionTrx!=null){
							autorizacionTrx.rollback();
							autorizacionTrx.close();
							autorizacionTrx = null;
						}
						return ErrorDocumentno+msg;
					}
				String invoiceNo = getDocumentNo();		
				String invoiceID = String.valueOf(get_ID());
				a.setDescription(invoiceNo);
				a.set_ValueOfColumn("DocumentID", invoiceID);
				a.saveEx();
				if (autorizacionTrx!=null){
					autorizacionTrx.commit(true);
				}

				// Procesar Autorizacion SRI
				// 04/07/2016 MHG Offline Schema added
				if (!isOfflineSchema) {
					log.warning("@Authorizing Xml@ -> " + file_name);
					try {
						msg = signature.respuestaAutorizacionComprobante(ac, a, m_accesscode);

						if (msg != null){
							if (autorizacionTrx!=null){
								autorizacionTrx.commit();
								autorizacionTrx.close();
								autorizacionTrx = null;
							}
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
				if (autorizacionTrx!=null){
					autorizacionTrx.commit(true);
				}
			}

			//		if (LEC_FE_Utils.breakDialog("Completando Nota Credito")) return "Cancelado...";	// TODO Temp

			//
		}
		catch (Exception e)
		{
			msg = "No se pudo crear XML - " + e.getMessage();
			log.severe(msg);
			if (autorizacionTrx!=null){
				autorizacionTrx.rollback();			
				autorizacionTrx.close();
				autorizacionTrx = null;
			}
			return ErrorDocumentno+msg;
		}catch (Error e) {
			msg = "No se pudo crear XML- Error en Conexion con el SRI";
			return ErrorDocumentno+msg;
		}

		log.warning("@SRI_FileGenerated@ -> " + file_name);
		if (autorizacionTrx!=null){
			autorizacionTrx.commit();

			autorizacionTrx.close();
			autorizacionTrx = null;			
		}

		set_Value("SRI_Authorization_ID", autorizationID);
		this.saveEx();
		return msg;

	} // lecfeinvnc_SriExportNotaCreditoXML100

	public void addHeaderElement(TransformerHandler mmDoc, String att, String value, AttributesImpl atts) throws Exception {
		if (att != null) {
			mmDoc.startElement("","",att,atts);
			mmDoc.characters(value.toCharArray(),0,value.toCharArray().length);
			mmDoc.endElement("","",att);
		} else {
			throw new AdempiereUserError(att + " empty");
		}
	}


}	// LEC_FE_MNotaCredito