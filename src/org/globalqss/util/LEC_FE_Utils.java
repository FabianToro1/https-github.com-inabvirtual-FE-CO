package org.globalqss.util;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MUserMail;
import org.compiere.util.DB;
import org.compiere.util.EMail;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.globalqss.model.X_SRI_AccessCode;
import org.globalqss.model.X_SRI_Authorization;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;


/**
 *	Utils for Localization LCO
 *
 *  @author Jesus Garcia - globalqss - Quality Systems & Solutions - http://globalqss.com
 *	@version $Id: LCO_Utils.java,v 1.0 2008/05/26 23:01:26 cruiz Exp $
 */

public class LEC_FE_Utils
{


	/**
	 *	Calculate SRI Digit based on AccesCode 48
	 *  Metodo Modulo 11 con factor de chequeo ponderado (2)
	 */
	public static int calculateDigitSri(String strAccessCode) {

		String strSriFactor = "765432";	// SRI Hardcoded
		
		if (strAccessCode.length() != 48)
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "LCO_WrongLength"));
		
		// Vector de cadena validacion Sismode
		Integer iNros[];
		iNros = new Integer[48];
	
		// Ciclo para asignar cada uno de los digitos del vector
		// La cadena de 6 se repite 8 veces
		try {
			for (int i = 0; i < 8 ; i++) {
				for (int j = 0; j < 6 ; j++) {
					iNros[(i * 6) + j] = Integer.parseInt(strSriFactor.substring(j, j + 1));
				}
			}
		} catch (NumberFormatException e) {
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "LCO_NotANumber"));
		}
	
		return modulo (strAccessCode, iNros, 11);
	
	}	// calculateDigitSri

	/**
	 * Implementa algoritmos modulo 10 y 11 con coeficientes
	 * @param valida - cadena de numeros a generar el digito de verificacion
	 * @param coeficientes - arreglo de enteros usados como coeficientes
	 * @param mod - modulo (LEC usa 10 u 11)
	 * @return
	 */
	public static int modulo(String valida, Integer[] coeficientes, int mod) {
		if (valida.length() > coeficientes.length) {
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "LCO_WrongLength"));
		}
		int iOperacion = 0;

		//Ciclo para multiplicar cada uno de los digitos con el vector
		for (int i = 0; i < valida.trim().length() ; i++) {
			try {
				int sumando = Integer.parseInt(valida.substring(i, i + 1)) * coeficientes[i];
				if (mod == 10 && sumando > 9) // para el algoritmo modulo 10 (Luhn algorithm) se utiliza el residuo de 9
					sumando -= 9;
				iOperacion = iOperacion + sumando;
			} catch (NumberFormatException e) {
				throw new AdempiereException(Msg.getMsg(Env.getCtx(), "LCO_NotANumber"));
			}
		}

		// Obtener el residuo de la operacion
		iOperacion %= mod;
		iOperacion = mod - iOperacion;
		if (iOperacion >= 10) {
			iOperacion = mod - iOperacion;
		}

		return iOperacion;
	}

	/**
	 * Get Doc or Current date in format - default yyyy/MM/dd
	 * @return Date
	 */
	public static String getDate(Date docDate, int format) {

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		
		if (docDate == null) docDate = new Date();
		
		if (format == 7)
			dateFormat = new SimpleDateFormat("MM/yyyy");
		else if (format == 8)
			dateFormat = new SimpleDateFormat("ddMMyyyy");
		else if (format == 9)
			dateFormat = new SimpleDateFormat("yyyyMMdd");
		else if (format == 10)
			dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		else if (format == 15)
			dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		
		return dateFormat.format(docDate);
	}
	
	/**
	 * 	String fillString count with char
	 * 	@return string
	 */
	public static String fillString(int count, char fill) {
	    if (count < 0)
	    	return "";
		char[] s = new char[count];
	    Arrays.fill(s, fill);
	    return new String(s);
	}

	/**
	 * 	String cutString lenght
	 * 	@return string
	 */
	public static String cutString(String str, int length) {
		if (length < str.length())
			return new String(str.substring(0, length).trim());

		return new String(str.trim());
	}
	
	/**
	 * 	String getStoreCode from standard DocumentNo
	 * 	@return string
	 */
	public static String getStoreCode(String docno) {
		
		return new String(docno.substring(4, 7));
	}
	
	/**
	 * 	String getSecuencial from DocumentNo
	 * 	@return string
	 */
	public static String getSecuencial(String docno, String shortdoctype) {
		if (shortdoctype.equals("01"))	// FACTURA
			return new String(docno.substring(docno.lastIndexOf('-') + 1));
		else if (shortdoctype.equals("04"))	// NOTA DE CR??DITO
			return new String(docno.substring(docno.lastIndexOf('-') + 1));
		else if (shortdoctype.equals("05"))	// NOTA DE D??BITO
			return new String(docno.substring(docno.lastIndexOf('-') + 1));
		else if (shortdoctype.equals("06"))	// GU??A DE REMISI??N
			return new String(docno.substring(docno.lastIndexOf('-') + 1));
		else if (shortdoctype.equals("07"))	// COMPROBANTE DE RETENCI??N
			return new String(docno.substring(docno.lastIndexOf('-') + 1));

		return new String(docno.trim());
	}
	
	/**
	 * 	String getSecuencial from DocumentNo
	 * 	@return string
	 */
	public static String formatDocNo(String docno, String shortdoctype) {
		
		// SRI Longitud valida 15 Digits, 17 con guiones
		String doc17Sri = "";
		
		try {
				
			if (docno.trim().length() < 17) {
				doc17Sri = docno.substring(0, 3);
				if (docno.lastIndexOf('-') == -1)		// 0 Guion
					doc17Sri = doc17Sri + docno.substring(0, 6) + "-";
				else if (docno.lastIndexOf('-') == 3)	// 1 Guion
					doc17Sri = doc17Sri + docno.substring(3, 7) + "-";
				else if (docno.lastIndexOf('-') == 6)	// 1 Guion
					doc17Sri = doc17Sri + "-" + docno.substring(3, 5);
				else if (docno.lastIndexOf('-') == 7)	// 2 Guiones
					doc17Sri = doc17Sri + docno.substring(3, 8);
				else doc17Sri = docno.substring(0, 6);
				
				doc17Sri = doc17Sri + LEC_FE_Utils.fillString(9 - (LEC_FE_Utils.cutString(LEC_FE_Utils.getSecuencial(docno, shortdoctype), 9)).length(), '0')
									+ LEC_FE_Utils.cutString(LEC_FE_Utils.getSecuencial(docno, shortdoctype), 9);
			}
			else doc17Sri = docno;
			
		} catch (Exception e) {
			//TODO Crear aviso del error
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "LCO_NotANumber"));
		}

		doc17Sri = LEC_FE_Utils.cutString(doc17Sri.trim(),17);
		
		if (doc17Sri.length() != 17) {
			
			//TODO Crear aviso del error
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "LCO_WrongLength"));
			
		}
			
		return doc17Sri;
	}
	
	/**
	 * 	String replaceGuion from DocumentNo
	 * 	@return string
	 */
	public static String replaceGuion(String docno) {

		// SRI Longitud valida 15 Digits
		String doc15Sri = docno.replaceAll("-", "").trim();
		
		if (doc15Sri.length() != 15)
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "LCO_WrongLength"));
		
		return doc15Sri;
	}

	/**
	 * 	String getTipoIdentificacionSri segun TaxCodeSRI
	 * 	@return string
	 */
	public static String getTipoIdentificacionSri(String taxcodesri) {
	
		String tipoidentificacionsri = "";
		
		tipoidentificacionsri = LEC_FE_Utils.fillString(2 - (LEC_FE_Utils.cutString(taxcodesri, 2)).length(), '0')
			+ LEC_FE_Utils.cutString(taxcodesri, 2);
		
		return tipoidentificacionsri;
	
	}
	
	/**
	 * 	String getLecSriFormat
	 * 	@return int
	 */
	public static int getLecSriFormat(int ad_client_id, String tipoemision, String coddoc, Date startdate, Date enddate) {
	
		int lec_sri_format_id = DB.getSQLValue(null, "SELECT MAX(LEC_SRI_Format_ID) FROM LEC_SRI_Format WHERE AD_Client_ID = ? AND IsActive = 'Y' AND SRI_DeliveredType = ? AND SRI_ShortDocType = ? AND ? >= ValidFrom AND ( ? <= ValidTo OR ValidTo IS NULL)", ad_client_id, tipoemision, coddoc, startdate, enddate);
		
		return lec_sri_format_id;

	}
	
	/**
	 * 	String getOrgBPartner
	 * 	@return int
	 */
	public static int getOrgBPartner(int ad_client_id, String taxid) {
	
		int c_bpartner_id = DB.getSQLValue(null, "SELECT C_BPartner_ID FROM C_BPartner WHERE AD_Client_ID = ? AND TaxId = ? ", ad_client_id, taxid);
		
		return c_bpartner_id;

	}
	
	/**
	 * 	String getInvoiceDocSustento
	 * 	@return int
	 */
	public static int getInvoiceDocSustento(int c_invoice_id) {
	
		int m_inout_sus_id = DB.getSQLValue(null, "SELECT COALESCE(MAX(io.M_InOut_ID), -1) "
				+ "FROM M_InOut io "
				+ "JOIN M_InOutLine iol ON iol.M_InOut_ID = io.M_InOut_ID "
				+ "JOIN C_InvoiceLine il ON il.M_InOutLine_ID = iol.M_InOutLine_ID "
				+ "JOIN C_DocType dt on io.C_DocType_ID = dt.C_DocType_ID "
				+ "WHERE io.DocStatus IN ('CO','CL') "
				+ "AND dt.SRI_ShortDocType<>'' AND il.C_Invoice_ID = ? ", c_invoice_id);
		
		return m_inout_sus_id;

	}
	
	/**
	 * 	String getInOutDocSustento
	 * 	@return int
	 */
	public static int getInOutDocSustento(int m_inout_id) {
	
		int c_invoice_sus_id = DB.getSQLValue(null, "SELECT COALESCE(MAX(i.C_Invoice_ID), -1) FROM M_InOut io JOIN M_InOutLine iol ON iol.M_InOut_ID = io.M_InOut_ID JOIN C_InvoiceLine il ON il.M_InOutLine_ID = iol.M_InOutLine_ID JOIN C_Invoice i ON i.C_Invoice_ID = il.C_Invoice_ID WHERE i.DocStatus IN ('CO','CL') AND io.M_InOut_ID = ? ", m_inout_id);
		
		return c_invoice_sus_id;

	}
	
	/**
	 * 	String getAuthorisedInvoice
	 * 	@return int
	 */
	public static int getAuthorisedInvoice(int sri_authosisation_id) {
	
		int c_invoice_authorised_id = DB.getSQLValue(null, "SELECT COALESCE(MAX(i.C_Invoice_ID), -1) FROM C_Invoice i WHERE i.SRI_Authorization_ID = ? ", sri_authosisation_id);
		
		return c_invoice_authorised_id;

	}
	
	/**
	 * 	String getAuthorisedInOut
	 * 	@return int
	 */
	public static int getAuthorisedInOut(int sri_authosization_id) {
	
		int m_inout_authorised_id = DB.getSQLValue(null, "SELECT COALESCE(MAX(io.M_InOut_ID), -1) FROM M_InOut io WHERE io.SRI_Authorization_ID = ? ", sri_authosization_id);
		
		return m_inout_authorised_id;

	}
	
	/**
	 * 	String getAuthorisedMovement
	 * 	@return int
	 */
	public static int getAuthorisedMovement(int sri_authosisation_id) {
	
		int m_movement_authorised_id = DB.getSQLValue(null, "SELECT COALESCE(MAX(m.M_Movement_ID), -1) FROM M_Movement m WHERE m.SRI_Authorization_ID = ? ", sri_authosisation_id);
		
		return m_movement_authorised_id;

	}
	
	/**
	 * 	String getMovLocation
	 * 	@return int
	 */
	public static int getMovLocator(int m_movement_id) {
	
		int c_location_id = DB.getSQLValue(null, "SELECT MIN(C_Location_ID) FROM M_MovementLine ml JOIN M_Locator l ON l.M_Locator_ID = ml.M_Locator_ID JOIN M_Warehouse w ON w.M_Warehouse_ID = l.M_Warehouse_ID WHERE M_Movement_ID = ? GROUP BY M_Movement_ID ", m_movement_id);
		
		return c_location_id;

	}
	
	/**
	 * 	String getInvAllDocSustento
	 * 	@return int
	 */
	public static int getInvAllDocSustento(int c_invoice_id) {	// Deprecated
	
		int c_invoice_sus_id = DB.getSQLValue(null, "SELECT al.C_Invoice_ID FROM C_AllocationHdr ah JOIN C_AllocationLine al ON al.C_AllocationHdr_ID = ah.C_AllocationHdr_ID WHERE ah.C_AllocationHdr_ID IN (SELECT al.C_AllocationHdr_ID FROM C_AllocationLine al WHERE al.C_Invoice_ID = ?) AND ah.DocStatus IN ('CO','CL') AND al.C_Invoice_ID != ? ", c_invoice_id, c_invoice_id);
		
		return c_invoice_sus_id;

	}
	
	/**
	 * 	String getErrorCode
	 * 	@return int
	 */
	public static int getErrorCode(String value) {
	
		int sri_errorcode_id = DB.getSQLValue(null, "SELECT SRI_ErrorCode_ID FROM SRI_ErrorCode WHERE AD_Client_ID = ? AND Value = ? ", Env.getAD_Client_ID(Env.getCtx()), value);
		
		if ( sri_errorcode_id < 1) {
			value = "150";	// Default 150-Error sin codificar
			sri_errorcode_id = DB.getSQLValue(null, "SELECT SRI_ErrorCode_ID FROM SRI_ErrorCode WHERE AD_Client_ID = ? AND Value = ? ", Env.getAD_Client_ID(Env.getCtx()), value);
		}
		
		return sri_errorcode_id;

	}
	
	/**
	 * 	String getInOutDocSustento
	 * 	@return int
	 */
	public static int getNextAccessCode(int ad_client_id, String envtype, String taxid, String trxName) {	
		
		int sri_accesscode_id = -1;
		
		StringBuffer sql = new StringBuffer("SELECT SRI_AccessCode_ID FROM SRI_AccessCode WHERE AD_Client_ID = ? AND EnvType = ? AND IsUsed = 'N' AND SUBSTR(Value,1,13) = ? ORDER BY Value");
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), trxName);
			pstmt.setInt(1, ad_client_id);
			pstmt.setString(2, envtype);
			pstmt.setString(3, taxid);
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next())
			{
				sri_accesscode_id = rs.getInt(1);
				break;	// El primero
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "NoAccessCode"));
		}
		
		return sri_accesscode_id;
	}
	
	/**
	 * 	String getAccessCode
	 * 	@return String
	 */
	public static String getAccessCode(Date docdate, String coddoc, String taxid, String orgcode, String storecode, String documentno, String documentcode, String tipoemision, X_SRI_AccessCode ac) {
	
		String accesscode;
				
		try {
			
			accesscode = ""
				+ String.format("%8s", getDate(docdate,8))				// fechaEmision
				+ String.format("%2s", coddoc)							// codDoc
				+ String.format("%13s", (fillString(13 - (cutString(taxid, 13)).length(), '0'))
					+ cutString(taxid,13))								// ruc
				+ String.format("%1s", ac.getEnvType());				// ambiente
			
			if (ac.getCodeAccessType().equals("1")) {	// claveAccesoAutomatica
				accesscode = accesscode
				+ String.format("%3s", orgcode)							// serie / estab
				+ String.format("%3s", storecode)						// serie / ptoEmi
				+ String.format("%9s", (fillString(9 - (cutString(getSecuencial(documentno, coddoc), 9)).length(), '0'))
					+ cutString(getSecuencial(documentno, coddoc), 9))	// numero / secuencial
				+ String.format("%8s", documentcode);					// codigo
			} else {
				accesscode = accesscode
				+ String.format("%23s", ac.getValue().substring(14));	// codigo numerico contingencia
			}	
			
			accesscode = accesscode + String.format("%1s", tipoemision);// tipoEmision

			accesscode = accesscode
				+ String.format("%1s", calculateDigitSri(accesscode));	// digito
		
		} catch (Exception e) {
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "LCO_NotANumber"));
		}
			
		return accesscode;
		
	}
		
	/**
	 * 	void atachXmlFile
	 * 	@return void
	 */
	public static void attachXmlFile(Properties ctx, String trxName, int sri_authorization_id, String file_name) {
		
		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();
		
		try {
		int  AD_Table_ID = MTable.getTable_ID(X_SRI_Authorization.Table_Name);
		//if one attach is found , it means that a xml file was attached before
		MAttachment attach =  MAttachment.get(ctx, AD_Table_ID, sri_authorization_id);
		//no se encontro archivo previo
		if (attach == null ) {
			attach = new  MAttachment(ctx, AD_Table_ID , sri_authorization_id, trxName);
			attach.addEntry(new File (file_name));
			attach.saveEx();

		} /* else {
			// se encontro un archivo adjunto previamente
			//toma el index  del penultimo archivo y lo renombra
			//REVIEWME
			int index = (attach.getEntryCount()-1);
			MAttachmentEntry entry = attach.getEntry(index) ;
			String renamed = signature.getFolderRaiz()+File.separator+entry.getName().substring(0,entry.getName().length()-4 )+"_old_"+ LEC_FE_Utils.getDate(null, 15) + ".xml";
			entry.setName(renamed);
			attach.saveEx();
			//agrega el nuevo archivo ya q el anterior ha sido renombrado
			attach.addEntry(new File (file_name));
			attach.saveEx();
		} */
		//DB.getSQLValue(get_TrxName(),"SELECT AD_Attachment_ID FROM AD_Attachment WHERE AD_Table_ID=? AND Record_ID=?",AD_Table_ID)
	
		// MAttachment
		/*
		  Si IsAttachXML
	      Anexar el archivo XML al XML Header (probar con Archivador
		  a ver si funciona, si no con Attachment)
	      NOTA: ??Que hacer si ya hay un archivo previo generado?
		  Verificar si el archivador es read-only, y si podria renombrar
		  un archivo previo generado para a??adirle un sufijo _old_yyyymmdd
		 */
		
		} catch (Exception e) {
			throw new AdempiereException(Msg.getMsg(Env.getCtx(), "AttachmentNull"));
		}
	}
	
	/**
	* EMail user
	* int notifyUsers
	* @return how many email were sent
	*/

	public static int notifyUsers(Properties ctx, MMailText mText, int ad_user_id, String subject, String message, List<File> attachments,  String trxName, int ad_user2_id)
	{
		int countMail = 0;
		
		if (ad_user_id > 0){
			MClient client = new MClient (ctx, Env.getAD_Client_ID(ctx), trxName);
			
			MUser bcc = null;

			MUser notifyTo = new MUser (ctx, ad_user_id, trxName);
			
			if (ad_user2_id > 0)
				bcc = new MUser(ctx, ad_user2_id, trxName);
			
			if (notifyTo.isNotificationEMail()) {
				
				String msg = null;
				
				EMail email = client.createEMail(notifyTo.getEMail(), subject, message);
				
				if (email.isValid()) {
					for (File att : attachments) {
						email.addAttachment(att);
					}
					
					if (bcc != null){
						if (bcc.isNotificationEMail())
							email.addBcc(bcc.getEMail());
					}
					
					msg = email.send();
				}
				
				MUserMail um = new MUserMail(mText, ad_user_id, email);
				um.setSubject(subject);
				um.setMailText(message);
				um.saveEx();
				
				if (msg.equals(EMail.SENT_OK))
				{
					countMail++;
				}
			}
		}

		return countMail;
	}
	
	public static boolean breakDialog(String msg)	// Trace temporal
	{
		int ad_user_id = Env.getAD_User_ID(Env.getCtx());
		
		if (ad_user_id != 100) return false;
		
		if (!FDialog.ask(0, null, msg)) {
			return true;
		}
	
		return false;
	}
	
	/**
	 *  @author Orlando Curieles - www.ingeint.com
	 *  @param date
	 *  @param days
	 *  @return
	 */
	public static Date calculateDate(Date date, int days) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_YEAR, days);
		int WeekDay = calendar.get(Calendar.DAY_OF_WEEK);

		if (WeekDay==7)
			calendar.add(Calendar.DAY_OF_YEAR, 2);
		else if (WeekDay==1)
			calendar.add(Calendar.DAY_OF_YEAR, 1);

		return calendar.getTime(); 
	}
	
	public static String Month(Integer month) {
		
		if (month == 1)
			return "Enero";
		else if (month == 2)
			return "Febrero";
		else if (month == 3)
			return "Marzo";
		else if (month == 4)
			return "Abril";
		else if (month == 5)
			return "Mayo";
		else if (month == 6)
			return "Junio";
		else if (month == 7)
			return "Julio";
		else if (month == 8)
			return "Agosto";
		else if (month == 9)
			return "Septiembre";
		else if (month == 10)
			return "Octubre";
		else if (month == 11)
			return "Noviembre";
		else if (month == 12)
			return "Diciembre";

		return "";
	}
	
	public static String allguides(Integer C_Invoice_ID, String trx, Integer record) {
		
		StringBuffer allguides = new StringBuffer();
		int count = 0;
		StringBuffer sql = new StringBuffer("SELECT distinct(mi.documentno),mi.dateAcct "
				+ "FROM  m_inout mi,c_invoiceline il,m_inoutline mil " + 
				"         WHERE il.c_invoice_id = ? " + 
				"         and il.m_inoutline_id=mil.m_inoutline_id " + 
				"         and mil.m_inout_id=mi.m_inout_id " + 
				"         and mi.docstatus in ('CO','CL') " + 
				"         and il.m_inoutline_id is not null " +
				"		  order by mi.dateAcct, mi.documentno ");
		
		PreparedStatement pstmt = DB.prepareStatement(sql.toString(), trx);
		
		try {
		pstmt.setInt(1, C_Invoice_ID);
		ResultSet rs = pstmt.executeQuery();
		//

		while (rs.next()) {
			if (record == 1) {
				allguides.append(rs.getString(1));
				break;
			}else {
				if (count>=1)
					allguides.append("/");
				allguides.append(rs.getString(1));
			}
			count++;
		}rs.close();
		pstmt.close();
	}
	catch (SQLException e)
	{
		return "Error al obtener las guias ";
	}	
		if (allguides.length()==0)
			return "-";
		else
			return allguides.toString();
	}
	
}	// LEC_FE_Utils
