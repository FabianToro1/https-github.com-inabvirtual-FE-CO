package org.globalqss.process;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

public class SRIProcessATS  extends SvrProcess {
	int p_Month = (Calendar.getInstance()).get(Calendar.MONTH);
	int p_Year = (Calendar.getInstance()).get(Calendar.YEAR);

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("Month"))
				p_Month = para[i].getParameterAsInt();
			else if (name.equals("Year"))
				p_Year = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}		
	}

	@Override
	protected String doIt() throws Exception {

		GeberateXML();
		
		return null;
	}
	
	private void GeberateXML() throws Exception {		
		String file_name = "";
		String XmlEncoding = "UTF-8";
		file_name = "AT-102018" + ".xml";
		Boolean EsValido = true;
		
		//Stream para el documento xml
		OutputStream mmDocStream = new FileOutputStream (file_name, false);
		StreamResult streamResult_menu = new StreamResult(new OutputStreamWriter(mmDocStream,XmlEncoding));
		SAXTransformerFactory tf_menu = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		tf_menu.setAttribute("indent-number", new Integer(0));
		
		TransformerHandler mmDoc = tf_menu.newTransformerHandler();
		Transformer serializer_menu = mmDoc.getTransformer();
		serializer_menu.setOutputProperty(OutputKeys.ENCODING,XmlEncoding);
		serializer_menu.setOutputProperty(OutputKeys.INDENT,"yes");
		
		mmDoc.setResult(streamResult_menu);

		mmDoc.startDocument();

		AttributesImpl atts = new AttributesImpl();
		
		// Encabezado
		mmDoc.startElement("","","iva", atts);

		StringBuffer sql = new StringBuffer(
	             "select 'R' TipoIDInformante, \n" + 
	             "	oi.TaxID IdInformante,\n" + 
	             "	replace(o.name,'.','') razonSocial,\n" + 
	             "	v.Anio,\n" + 
	             "	v.Mes,\n" + 
	             "	oi.sri_orgcode numEstabRuc, \n" + 
	             "	v.baseImponible totalVentas,\n" + 
	             "	'IVA' codigoOperativo,\n" + 
	             "	oi.sri_orgcode\n" + 
	             "from ad_org o \n" + 
	             "	join ad_orginfo oi on o.ad_org_id = oi.ad_org_id\n" + 
	             "	left join (select c.ad_client_id, \n" + 
	             "		c.ad_org_id, \n" + 
	             "		'' || extract(year from c.dateacct) Anio,\n" + 
	             "		right('0' || extract(month from c.dateacct),2) Mes, \n" + 
	             "		sum(case when cl.C_Tax_ID != 1000073 then cl.linenetamt - cl.taxamt else 0 end) baseImponible \n" + 
	             "	from c_invoice c\n" + 
	             "		join c_invoiceline cl on c.c_invoice_id = cl.c_invoice_id\n" + 
	             "	where c.issotrx = 'Y'\n" + 
	             "		and c.docstatus in ('CO','CL')\n" + 
	             "		and extract(month from c.dateacct) = ? \n" + 
	             "		and extract(year from c.dateacct) = ? \n" + 
	             "	group by c.ad_client_id, c.ad_org_id, extract(month from c.dateacct), extract(year from c.dateacct)) v on o.ad_client_id = v.ad_client_id and o.ad_org_id = v.ad_org_id \n" + 
	             "where o.ad_client_id = ? and o.ad_org_id = ? ");

		PreparedStatement pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
		pstmt.setInt(1, p_Month);
		pstmt.setInt(2, p_Year);
		pstmt.setInt(3, getAD_Client_ID());
		pstmt.setInt(4, 1000003);
		ResultSet rs = pstmt.executeQuery();

		while (rs.next()) {
			EsValido = true;
			//EsValido = false; // 
			
			if (rs.getString(1).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs.getMetaData().getColumnName(1) + " no puede estar vacio. Identificacion del Informante");
			if (rs.getString(2).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs.getMetaData().getColumnName(2) + " no puede estar vacio. Identificacion del Informante");
			if (rs.getString(3).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs.getMetaData().getColumnName(3) + " no puede estar vacio. Identificacion del Informante");
			if (rs.getString(6).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs.getMetaData().getColumnName(6) + " no puede estar vacio. Identificacion del Informante");
			
			if (EsValido) {
				addHeaderElement(mmDoc, "TipoIDInformante", rs.getString(1), atts);
				addHeaderElement(mmDoc, "IdInformante", rs.getString(2), atts);
				addHeaderElement(mmDoc, "razonSocial", rs.getString(3), atts);
				addHeaderElement(mmDoc, "Anio", rs.getString(4), atts);
				addHeaderElement(mmDoc, "Mes", rs.getString(5), atts);
				addHeaderElement(mmDoc, "numEstabRuc", rs.getString(6), atts);
				addHeaderElement(mmDoc, "totalVentas", FormatDecimalToString(rs.getDouble(7)), atts);
				addHeaderElement(mmDoc, "codigoOperativo", rs.getString(8), atts);
			}
		}
		rs.close();
		pstmt.close();
		
		StringBuffer sql2 = new StringBuffer(
				"select COALESCE(COALESCE(p.ING_TaxSustance,ch.ING_TaxSustance),'') codSustento,\n" + 
				"	COALESCE(lco.value,'') tpIdProv,\n" + 
				"	cb.taxid idProv,\n" + 
				"	dt.SRI_ShortDocType tipoComprobante,\n" + 
				"	case when cb.IsRelatedPart = 'Y' then 'SI' else 'NO' end parteRel,\n" + 
				"	to_char(c.dateacct,'DD/MM/YYYY') fechaRegistro,\n" + 
				"	COALESCE(c.ING_Establishment, '') establecimiento,\n" + 
				"	COALESCE(c.ING_Emission, '') puntoEmision,\n" + 
				"	COALESCE(c.ING_Sequence, '') secuencial,\n" + 
				"	to_char(c.dateinvoiced,'DD/MM/YYYY') fechaEmision,\n" + 
				"	COALESCE(c.SRI_AuthorizationCode, '') autorizacion,\n" + 
				"	sum(case when cl.C_Tax_ID  = 1000073 then cl.linenetamt - cl.taxamt else 0 end) baseNoGraIva,\n" + 
				"	0 baseImponible,\n" + 
				"	sum(case when cl.C_Tax_ID != 1000073 then cl.linenetamt - cl.taxamt else 0 end) baseImpGrav,\n" + 
				"	0 baseImpExe,\n" + 
				"	0 montoIce,\n" + 
				"	sum(cl.taxamt) montoIva,\n" + 
				"	coalesce(retiva.valRetBien10, 0) valRetBien10,\n" + 
				"	coalesce(retiva.valRetServ20, 0) valRetServ20,\n" + 
				"	coalesce(retiva.valorRetBienes, 0) valorRetBienes,\n" + 
				"	coalesce(retiva.valRetServ50, 0) valRetServ50,\n" + 
				"	coalesce(retiva.valorRetServicios, 0) valorRetServicios,\n" + 
				"	coalesce(retiva.valRetServ100, 0) valRetServ100,\n" + 
				"	0 totbasesImpReemb,\n" + 
				"	coalesce(c.ING_PaymentInfo,'') pagoLocExt,\n" + 
				"	coalesce(c.C_Country_ID, 0) paisEfecPagoGen,\n" + 
				"	'NA' paisEfecPago,\n" + 
				"	case when c.ING_ApplyDoubleTax = 'Y' then 'SI' else 'NO' end aplicConvDobTrib,\n" + 
				"	'NA' pagExtSujRetNorLeg, \n" + 
				"	c.documentno \n" + 
				"from c_invoice c\n" + 
				"	join c_invoiceline cl on c.c_invoice_id = cl.c_invoice_id \n" + 
				"	join c_bpartner cb on c.c_bpartner_id = cb.c_bpartner_id\n" + 
				"	join lco_taxpayertype lco on cb.lco_taxpayertype_id = lco.lco_taxpayertype_id\n" + 
				"	join C_DocType dt on c.c_doctype_id = dt.c_doctype_id \n" + 
				"	left join m_product p on cl.m_product_id = p.m_product_id \n" + 
				"	left join c_charge ch on cl.c_charge_id = ch.c_charge_id\n" + 
				"	left join (select ret.C_Invoice_ID,\n" + 
				"			sum(case when percent = 10 then ret.taxamt else 0 end) valRetBien10,\n" + 
				"			sum(case when percent = 20 then ret.taxamt else 0 end) valRetServ20,\n" + 
				"			sum(case when percent = 30 then ret.taxamt else 0 end) valorRetBienes,\n" + 
				"			sum(case when percent = 50 then ret.taxamt else 0 end) valRetServ50,\n" + 
				"			sum(case when percent = 70 then ret.taxamt else 0 end) valorRetServicios,\n" + 
				"			sum(case when percent = 100 then ret.taxamt else 0 end) valRetServ100,\n" + 
				"			sum(ret.taxamt) valRetIva\n" + 
				"		from LCO_InvoiceWithholding ret \n" + 
				"		where ret.LCO_WithholdingType_ID = 1000034\n" + 
				"		group by ret.C_Invoice_ID) retiva on c.c_invoice_id = retiva.c_invoice_id\n" + 
				"where c.issotrx = 'N'\n" + 
				"	and c.docstatus in ('CO','CL') \n" + 
	            "	and c.ad_client_id = ? and c.ad_org_id = ? \n" +
				"	and extract(month from c.dateacct) = ? \n" + 
				"	and extract(year from c.dateacct) = ? \n" + 
				"group by p.ING_TaxSustance,ch.ING_TaxSustance,lco.value,cb.taxid,dt.SRI_ShortDocType,cb.IsRelatedPart,\n" + 
				"	c.dateacct,c.ING_Establishment,c.ING_Emission,c.ING_Sequence,c.dateinvoiced,c.SRI_AuthorizationCode,\n" + 
				"	retiva.valRetBien10,retiva.valRetServ20,retiva.valorRetBienes,retiva.valRetServ50,retiva.valorRetServicios,retiva.valRetServ100,retiva.valRetIva,\n" + 
				"	c.ING_PaymentInfo,c.ING_ForeignTaxTregime,c.C_Country_ID,c.ING_ApplyDoubleTax,c.documentno");
		
		PreparedStatement pstmt2 = DB.prepareStatement(sql2.toString(), get_TrxName());
		pstmt2.setInt(1, getAD_Client_ID());
		pstmt2.setInt(2, 1000003);
		pstmt2.setInt(3, p_Month);
		pstmt2.setInt(4, p_Year);
		ResultSet rs2 = pstmt2.executeQuery();
		
		mmDoc.startElement("","","compras", atts);
		while (rs2.next()) {
			EsValido = true;
			
			if (rs2.getString(1).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs2.getMetaData().getColumnName(1) + " no puede estar vacio. Compra: "+ rs2.getString(30));
			if (rs2.getString(2).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs2.getMetaData().getColumnName(2) + " no puede estar vacio. Compra: "+ rs2.getString(30));
			if (rs2.getString(3).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs2.getMetaData().getColumnName(3) + " no puede estar vacio. Compra: "+ rs2.getString(30));
			if (rs2.getString(7).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs2.getMetaData().getColumnName(7) + " no puede estar vacio. Compra: "+ rs2.getString(30));
			if (rs2.getString(8).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs2.getMetaData().getColumnName(8) + " no puede estar vacio. Compra: "+ rs2.getString(30));
			if (rs2.getString(9).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs2.getMetaData().getColumnName(9) + " no puede estar vacio. Compra: "+ rs2.getString(30));
			if (rs2.getString(11).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs2.getMetaData().getColumnName(11) + " no puede estar vacio. Compra: "+ rs2.getString(30));
			if (rs2.getString(25).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs2.getMetaData().getColumnName(25) + " no puede estar vacio. Compra: "+ rs2.getString(30));

			if (EsValido) {
				mmDoc.startElement("", "", "detalleCompras", atts);
				addHeaderElement(mmDoc, "codSustento", rs2.getString(1), atts);
				addHeaderElement(mmDoc, "tpIdProv", rs2.getString(2), atts);
				addHeaderElement(mmDoc, "idProv", rs2.getString(3), atts);
				addHeaderElement(mmDoc, "tipoComprobante", rs2.getString(4), atts);
				addHeaderElement(mmDoc, "parteRel", rs2.getString(5), atts);
				addHeaderElement(mmDoc, "fechaRegistro", rs2.getString(6), atts);
				addHeaderElement(mmDoc, "establecimiento", rs2.getString(7), atts);
				addHeaderElement(mmDoc, "puntoEmision", rs2.getString(8), atts);
				addHeaderElement(mmDoc, "secuencial", rs2.getString(9), atts);
				addHeaderElement(mmDoc, "fechaEmision", rs2.getString(10), atts);
				addHeaderElement(mmDoc, "autorizacion", rs2.getString(11), atts);
				addHeaderElement(mmDoc, "baseNoGraIva", FormatDecimalToString(rs2.getDouble(12)), atts);
				addHeaderElement(mmDoc, "baseImponible", FormatDecimalToString(rs2.getDouble(13)), atts);
				addHeaderElement(mmDoc, "baseImpGrav", FormatDecimalToString(rs2.getDouble(14)), atts);
				addHeaderElement(mmDoc, "baseImpExe", FormatDecimalToString(rs2.getDouble(15)), atts);
				addHeaderElement(mmDoc, "montoIce", FormatDecimalToString(rs2.getDouble(16)), atts);
				addHeaderElement(mmDoc, "montoIva", FormatDecimalToString(rs2.getDouble(17)), atts);
				addHeaderElement(mmDoc, "valRetBien10", FormatDecimalToString(rs2.getDouble(18)), atts);
				addHeaderElement(mmDoc, "valRetServ20", FormatDecimalToString(rs2.getDouble(19)), atts);
				addHeaderElement(mmDoc, "valorRetBienes", FormatDecimalToString(rs2.getDouble(20)), atts);
				addHeaderElement(mmDoc, "valRetServ50", FormatDecimalToString(rs2.getDouble(21)), atts);
				addHeaderElement(mmDoc, "valorRetServicios", FormatDecimalToString(rs2.getDouble(22)), atts);
				addHeaderElement(mmDoc, "valRetServ100", FormatDecimalToString(rs2.getDouble(23)), atts);
				addHeaderElement(mmDoc, "totbasesImpReemb", FormatDecimalToString(rs2.getDouble(24)), atts);
				mmDoc.startElement("", "", "pagoExterior", atts);
				addHeaderElement(mmDoc, "pagoLocExt", rs2.getString(25), atts);
				addHeaderElement(mmDoc, "paisEfecPagoGen", rs2.getString(26), atts);
				addHeaderElement(mmDoc, "paisEfecPago", rs2.getString(27), atts);
				addHeaderElement(mmDoc, "aplicConvDobTrib", rs2.getString(28), atts);
				addHeaderElement(mmDoc, "pagExtSujRetNorLeg", rs2.getString(29), atts);
				mmDoc.endElement("", "", "pagoExterior");
				mmDoc.endElement("", "", "detalleCompras");
			}
		}
		rs2.close();
		pstmt2.close();
		mmDoc.endElement("","","compras");
		
		StringBuffer sql3 = new StringBuffer(
				"select COALESCE(lco.value, '04') tpIdCliente,\n" + 
				"	cb.taxid idCliente,\n" + 
				"	case when cb.IsRelatedPart = 'Y' then 'SI' else 'NO' end parteRel,\n" + 
				"	dt.SRI_ShortDocType tipoComprobante,\n" + 
				"	'E' tipoEm,\n" + 
				"	1 numeroComprobantes,\n" + 
				"	sum(case when cl.C_Tax_ID  = 1000073 then cl.linenetamt - cl.taxamt else 0 end) baseNoGraIva,\n" + 
				"	0 baseImponible,\n" + 
				"	sum(case when cl.C_Tax_ID != 1000073 then cl.linenetamt - cl.taxamt else 0 end) baseImpGrav,\n" + 
				"	sum(cl.taxamt) montoIva,\n" + 
				"	0 montoIce,\n" + 
				"	coalesce(retiva.valRetIva, 0) valRetIva,\n" + 
				"	0 valorRetRenta,\n" + 
				"	c.LEC_PaymentMethod formaPago, \n" + 
				"	c.documentno \n" + 
				"from c_invoice c\n" + 
				"	join c_invoiceline cl on c.c_invoice_id = cl.c_invoice_id \n" + 
				"	join c_bpartner cb on c.c_bpartner_id = cb.c_bpartner_id\n" + 
				"	join lco_taxpayertype lco on cb.lco_taxpayertype_id = lco.lco_taxpayertype_id\n" + 
				"	join C_DocType dt on c.c_doctype_id = dt.c_doctype_id \n" + 
				"	left join m_product p on cl.m_product_id = p.m_product_id \n" + 
				"	left join c_charge ch on cl.c_charge_id = ch.c_charge_id\n" + 
				"	left join (select ret.C_Invoice_ID,\n" + 
				"			sum(case when percent = 10 then ret.taxamt else 0 end) valRetBien10,\n" + 
				"			sum(case when percent = 20 then ret.taxamt else 0 end) valRetServ20,\n" + 
				"			sum(case when percent = 30 then ret.taxamt else 0 end) valorRetBienes,\n" + 
				"			sum(case when percent = 50 then ret.taxamt else 0 end) valRetServ50,\n" + 
				"			sum(case when percent = 70 then ret.taxamt else 0 end) valorRetServicios,\n" + 
				"			sum(case when percent = 100 then ret.taxamt else 0 end) valRetServ100,\n" + 
				"			sum(ret.taxamt) valRetIva\n" + 
				"		from LCO_InvoiceWithholding ret \n" + 
				"		where ret.LCO_WithholdingType_ID = 1000034\n" + 
				"		group by ret.C_Invoice_ID) retiva on c.c_invoice_id = retiva.c_invoice_id\n" + 
				"where c.issotrx = 'Y'\n" + 
				"	and c.docstatus in ('CO','CL')\n" + 
				"	and c.ad_client_id = ? and c.ad_org_id = ? \n" +  
				"	and extract(month from c.dateacct) = ? \n" +
				"	and extract(year from c.dateacct) = ? \n" + 
				"group by p.ING_TaxSustance,ch.ING_TaxSustance,lco.value,cb.taxid,dt.SRI_ShortDocType,cb.IsRelatedPart,\n" + 
				"	c.dateacct,c.ING_Establishment,c.ING_Emission,c.ING_Sequence,c.dateinvoiced,c.SRI_AuthorizationCode,\n" + 
				"	retiva.valRetBien10,retiva.valRetServ20,retiva.valorRetBienes,retiva.valRetServ50,retiva.valorRetServicios,retiva.valRetServ100,retiva.valRetIva,\n" + 
				"	c.ING_PaymentInfo,c.ING_ForeignTaxTregime,c.C_Country_ID,c.ING_ApplyDoubleTax,c.LEC_PaymentMethod,c.documentno");
		
		PreparedStatement pstmt3 = DB.prepareStatement(sql3.toString(), get_TrxName());
		pstmt3.setInt(1, getAD_Client_ID());
		pstmt3.setInt(2, 1000003);
		pstmt3.setInt(3, p_Month);
		pstmt3.setInt(4, p_Year);
		ResultSet rs3 = pstmt3.executeQuery();

		mmDoc.startElement("","","ventas", atts);
		while (rs3.next()) {
			EsValido = true;
			if (rs3.getString(1).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs3.getMetaData().getColumnName(1) + " no puede estar vacio. Venta: "+ rs3.getString(15));
			if (rs3.getString(2).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs3.getMetaData().getColumnName(2) + " no puede estar vacio. Venta: "+ rs3.getString(15));
			if (rs3.getString(5).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs3.getMetaData().getColumnName(5) + " no puede estar vacio. Venta: "+ rs3.getString(15));

			if (EsValido) {
				mmDoc.startElement("","","detalleVentas", atts);
				addHeaderElement(mmDoc, "tpIdCliente", rs3.getString(1), atts);
				addHeaderElement(mmDoc, "idCliente", rs3.getString(2), atts);
				addHeaderElement(mmDoc, "parteRelVtas", rs3.getString(3), atts);
				addHeaderElement(mmDoc, "tipoComprobante", rs3.getString(4), atts);
				addHeaderElement(mmDoc, "tipoEmision", rs3.getString(5), atts);
				addHeaderElement(mmDoc, "numeroComprobantes", rs3.getString(6), atts);
				addHeaderElement(mmDoc, "baseNoGraIva", FormatDecimalToString(rs3.getDouble(7)), atts);
				addHeaderElement(mmDoc, "baseImponible", FormatDecimalToString(rs3.getDouble(8)), atts);
				addHeaderElement(mmDoc, "baseImpGrav", FormatDecimalToString(rs3.getDouble(9)), atts);
				addHeaderElement(mmDoc, "montoIva", FormatDecimalToString(rs3.getDouble(10)), atts);
				addHeaderElement(mmDoc, "montoIce", FormatDecimalToString(rs3.getDouble(11)), atts);
				addHeaderElement(mmDoc, "valorRetIva", FormatDecimalToString(rs3.getDouble(12)), atts);
				addHeaderElement(mmDoc, "valorRetRenta", FormatDecimalToString(rs3.getDouble(13)), atts);
				mmDoc.startElement("","","formasDePago", atts);
				addHeaderElement(mmDoc, "formaPago", rs3.getString(14), atts);
				mmDoc.endElement("","","formasDePago");
				mmDoc.endElement("","","detalleVentas");
			}
		}
		rs3.close();
		pstmt3.close();
		mmDoc.endElement("","","ventas");

		StringBuffer sql4 = new StringBuffer(
	             "select oi.sri_orgcode codEstab,\n" + 
	             "	(select sum(case when cl.C_Tax_ID != 1000073 then cl.linenetamt - cl.taxamt else 0 end) baseImponible\n" + 
	             "	from c_invoice c\n" + 
	             "		join c_invoiceline cl on c.c_invoice_id = cl.c_invoice_id \n" + 
	             "		join c_bpartner cb on c.c_bpartner_id = cb.c_bpartner_id\n" + 
	             "		join lco_taxpayertype lco on cb.lco_taxpayertype_id = lco.lco_taxpayertype_id\n" + 
	             "		join C_DocType dt on c.c_doctype_id = dt.c_doctype_id \n" + 
	             "		left join m_product p on cl.m_product_id = p.m_product_id \n" + 
	             "		left join c_charge ch on cl.c_charge_id = ch.c_charge_id\n" + 
	             "		left join (select ret.C_Invoice_ID,\n" + 
	             "				sum(case when percent = 10 then ret.taxamt else 0 end) valRetBien10,\n" + 
	             "				sum(case when percent = 20 then ret.taxamt else 0 end) valRetServ20,\n" + 
	             "				sum(case when percent = 30 then ret.taxamt else 0 end) valorRetBienes,\n" + 
	             "				sum(case when percent = 50 then ret.taxamt else 0 end) valRetServ50,\n" + 
	             "				sum(case when percent = 70 then ret.taxamt else 0 end) valorRetServicios,\n" + 
	             "				sum(case when percent = 100 then ret.taxamt else 0 end) valRetServ100,\n" + 
	             "				sum(ret.taxamt) valRetIva\n" + 
	             "			from LCO_InvoiceWithholding ret \n" + 
	             "			where ret.LCO_WithholdingType_ID = 1000034\n" + 
	             "			group by ret.C_Invoice_ID) retiva on c.c_invoice_id = retiva.c_invoice_id\n" + 
	             "	where c.issotrx = 'Y'\n" + 
	             "		and c.docstatus in ('CO','CL')\n" + 
	             "		and c.ad_client_id = o.ad_client_id and c.ad_org_id = o.ad_org_id \n" + 
	             "		and extract(month from c.dateacct) = ? \n" + 
	             "		and extract(year from c.dateacct) = ? ) ventasEstab,\n" + 
	             "	0 ivaComp\n" + 
	             "from ad_org o \n" + 
	             "	join ad_orginfo oi on o.ad_org_id = oi.ad_org_id\n" + 
	             "where o.ad_client_id = ? and o.ad_org_id = ? ");

		PreparedStatement pstmt4 = DB.prepareStatement(sql4.toString(), get_TrxName());
		pstmt4.setInt(1, p_Month);
		pstmt4.setInt(2, p_Year);
		pstmt4.setInt(3, getAD_Client_ID());
		pstmt4.setInt(4, 1000003);
		ResultSet rs4 = pstmt4.executeQuery();

		mmDoc.startElement("","","ventasEstablecimiento", atts);
		mmDoc.startElement("","","ventaEst", atts);
		while (rs4.next()) {
			EsValido = true;
			if (rs4.getString(1).equals("")) 
				throw new AdempiereException("@Error@ El campo " + rs4.getMetaData().getColumnName(1) + " no puede estar vacio. Ventas Establecimiento");

			if (EsValido) {
				addHeaderElement(mmDoc, "codEstab", rs4.getString(1), atts);
				addHeaderElement(mmDoc, "ventasEstab", FormatDecimalToString(rs4.getDouble(2)), atts);
				addHeaderElement(mmDoc, "ivaComp", rs4.getString(3), atts);
			}
		}
		rs.close();
		pstmt.close();
		mmDoc.endElement("","","ventaEst");
		mmDoc.endElement("","","ventasEstablecimiento");
		
		/*
		mmDoc.startElement("","","anulados", atts);
		mmDoc.startElement("","","detalleAnulados", atts);
		mmDoc.endElement("","","detalleAnulados");
		mmDoc.endElement("","","anulados");
		*/
		
		mmDoc.endElement("","","iva");
		
		mmDoc.endDocument();

		if (mmDocStream != null) {
			mmDocStream.close();
		}
	}

	public void addHeaderElement(TransformerHandler mmDoc, String att, String value, AttributesImpl atts) throws Exception {
		if (att != null) {
			mmDoc.startElement("","",att,atts);
			mmDoc.characters(value.toCharArray(),0,value.toCharArray().length);
			mmDoc.endElement("","",att);
		} else {
			throw new AdempiereUserError(att + " empty");
		}
	}
	
	private String FormatDecimalToString (Double valor) {
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
		String xValor = decimalFormat.format(valor).replace(".", "").replace(",", ".");
		
		if (xValor.substring(0, 1).equals("."))
			xValor = "0" + xValor;
		
		return xValor;
	}

}
