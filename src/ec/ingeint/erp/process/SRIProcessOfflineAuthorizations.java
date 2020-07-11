/**********************************************************************
 * This file is part of Adempiere ERP Bazaar                           *
 * http://www.adempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Jesus Garcia - GlobalQSS Colombia                                 *
 * - Carlos Ruiz  - GlobalQSS Colombia                                 *
 **********************************************************************/
package ec.ingeint.erp.process;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.ProcessUtil;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MMovement;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.globalqss.model.X_SRI_AccessCode;
import org.globalqss.model.X_SRI_Authorization;
import org.globalqss.util.LEC_FE_Utils;
import org.globalqss.util.LEC_FE_UtilsXml;

/**
 *	Generate Contingency Authorizations
 *	
 *  @author INGEINT/ocurieles
 */
public class SRIProcessOfflineAuthorizations extends SvrProcess
{

	/**	Client							*/
	private int				m_AD_Client_ID = 0;

	/** Authorization					*/
	private int			p_SRI_Authorization_ID = 0;

	/** Number of authorizations	*/
	private int			m_created = 0;


	private String file_name = "";

	private String msgError = null;

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null);

			else if (name.equals("SRI_Authorization_ID"))
				p_SRI_Authorization_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}

		m_AD_Client_ID = getAD_Client_ID();

		if (p_SRI_Authorization_ID == 0)
			p_SRI_Authorization_ID = getRecord_ID();

	}	//	prepare

	/**
	 * 	Generate Invoices
	 *	@return info
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		log.info("SRI_Authorization_ID=" + p_SRI_Authorization_ID);
		//
		String sql = null;
		sql = "SELECT * FROM SRI_Authorization a "
				+ " WHERE AD_Client_ID=?"
				+ "  AND IsActive = 'Y' AND Processed = 'N'"
				+ "  AND isSRIOfflineSchema = 'Y' "
				+ "  AND IsSRI_Error = 'N' ";
		if (p_SRI_Authorization_ID != 0)
			sql += " AND SRI_Authorization_ID=?";

		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			int index = 1;
			pstmt.setInt(index++, m_AD_Client_ID);
			if (p_SRI_Authorization_ID != 0)
				pstmt.setInt(index++, p_SRI_Authorization_ID);

		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}

		return generate(pstmt);

	}	//	doIt


	/**
	 * 	Generate Authorizations
	 * 	@param pstmt order query 
	 *	@return info
	 */
	private String generate (PreparedStatement pstmt)
	{
		String msg = null;
		try
		{

			ResultSet rs = pstmt.executeQuery ();
			while (rs.next ())
			{

				msg= "";
				X_SRI_Authorization authorization = new X_SRI_Authorization (getCtx(), rs, get_TrxName());

				// isSOTrx()
				if (authorization.getSRI_ShortDocType().equals("01"))	// FACTURA
					msg = lecfeinvoice_SriExportInvoiceXML100(authorization);
				else if (authorization.getSRI_ShortDocType().equals("04"))	// NOTA DE CRÉDITO
					msg = lecfeinvoice_SriExportInvoiceXML100(authorization);
				else if (authorization.getSRI_ShortDocType().equals("05"))	// NOTA DE DÉBITO
					msg = lecfeinvoice_SriExportInvoiceXML100(authorization);
				else if (authorization.getSRI_ShortDocType().equals("06")){

					if(LEC_FE_Utils.getAuthorisedInOut(authorization.getSRI_Authorization_ID()) > 0)
						msg = lecfeinout_SriExportInOutXML100(authorization);//GUÍA DE REMISIÓN - Entrega
					else if(LEC_FE_Utils.getAuthorisedMovement(authorization.getSRI_Authorization_ID()) > 0)
						msg = lecfemovement_SriExportMovementXML100(authorization);//GUÍA DE REMISIÓN - Movimiento
					else if (authorization.getDescription()!=null){
						if (authorization.getDescription().endsWith("-Movimiento"))
							msg = lecfemovement_SriExportMovementXML100(authorization);//GUÍA DE REMISIÓN - Movimiento
						else
							msg = lecfeinout_SriExportInOutXML100(authorization);//GUÍA DE REMISIÓN - Entrega
					}
				}

				// !isSOTrx()
				else if (authorization.getSRI_ShortDocType().equals("07"))	// COMPROBANTE DE RETENCIÓN
					msg = lecfeinvoice_SriExportInvoiceXML100(authorization);
				else
					log.warning("Formato no habilitado SRI: " + authorization.getSRI_ShortDocType());

				if(msg!="" && msg!=null)
					log.log(Level.SEVERE, msg);

			}	//	for all authorizations
			rs.close ();
			pstmt.close ();
			pstmt = null;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, msg, e);
			//return "Error:"+msgError+e.getMessage();
		}
		try
		{
			if (pstmt != null)
				pstmt.close ();
			pstmt = null;
		}
		catch (Exception e)
		{
			pstmt = null;
		}

		return "@Created@ = " + m_created;
	}	//	generate


	/**
	 * 	lecfeinvoice_SriExportInvoiceXML100
	 */
	public String lecfeinvoice_SriExportInvoiceXML100 (X_SRI_Authorization authorization) {
		String msg = null;

		int c_invoice_id = LEC_FE_Utils.getAuthorisedInvoice(authorization.getSRI_Authorization_ID());

		if(c_invoice_id<=0) {
			authorization.set_ValueOfColumn("IsSRI_Error", true);
			authorization.saveEx();
			msg = "Error obteniendo factura";
			log.severe(msg);
			msgError = msg;
			return msg;
		}

		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();
		MInvoice invoice = new MInvoice(getCtx(), c_invoice_id, get_TrxName());
		MDocType dt = new MDocType(getCtx(), invoice.getC_DocTypeTarget_ID(), get_TrxName());

		String coddoc = dt.get_ValueAsString("SRI_ShortDocType");

		try {

			X_SRI_AccessCode accesscode = new X_SRI_AccessCode (getCtx(), authorization.getSRI_AccessCode_ID(), get_TrxName());
			String xmlFileName = "SRI_" + coddoc + "-" + LEC_FE_Utils.getDate(invoice.getDateInvoiced(),9) + "-" + accesscode.getValue() + "_sig.xml";

			//ruta completa del archivo xml
			file_name = signature.getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesAutorizados + File.separator + xmlFileName;

			log.warning("@Authorizing Xml@ -> " + file_name);
			signature.setResource_To_Sign(file_name);
			msg = signature.respuestaAutorizacionComprobante(accesscode, authorization, authorization.getValue());

			if (msg != null) {
				authorization.set_ValueOfColumn("IsSRI_Error", true);
				authorization.saveEx();
				throw new AdempiereException(msg);
			}

			m_created++;

			if (invoice.get_Value("SRI_Authorization_ID")==null){
				invoice.set_ValueOfColumn("SRI_Authorization_ID", authorization.getSRI_Authorization_ID());
				invoice.saveEx();
			}

			if (authorization.getAD_UserMail()==null){
				authorization.setAD_UserMail_ID(invoice.getAD_User_ID());
				authorization.saveEx();
			}

			Trx sriTrx = null;
			sriTrx = Trx.get(invoice.get_TrxName(), false);
			if (sriTrx != null) 
				sriTrx.commit();

			invoice.load(sriTrx.getTrxName());
			sendMail(authorization.get_ID(), null);

		} catch (Exception e) {
			msg = "No se pudo obtener autorizacion - " + e.getMessage();
			log.severe(msg);
			msgError = msg;
			//throw new AdempiereException(msg);
		}

		return msg;

	} // lecfeinvoice_SriExportInvoiceXML100

	/**
	 * 	lecfeinout_SriExportInOutXML100
	 */
	public String lecfeinout_SriExportInOutXML100 (X_SRI_Authorization authorization)
	{
		String msg = null;

		int c_inout_id = LEC_FE_Utils.getAuthorisedInOut(authorization.getSRI_Authorization_ID());

		if(c_inout_id==0) {
			authorization.set_ValueOfColumn("IsSRI_Error", true);
			authorization.saveEx();
			msg = "Error obteniendo Entrada/Salida";
			log.severe(msg);
			msgError = msg;
			return msg;
		}

		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();
		MInOut inout = new MInOut(getCtx(), c_inout_id, get_TrxName());
		MDocType dt = new MDocType(getCtx(), inout.getC_DocType_ID(), get_TrxName());

		String coddoc = dt.get_ValueAsString("SRI_ShortDocType");

		try {

			X_SRI_AccessCode accesscode = new X_SRI_AccessCode (getCtx(), authorization.getSRI_AccessCode_ID(), get_TrxName());
			String xmlFileName = "SRI_" + coddoc + "-" + LEC_FE_Utils.getDate(inout.getMovementDate(),9) + "-" + accesscode.getValue() + "_sig.xml";

			//ruta completa del archivo xml
			file_name = signature.getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesFirmados + File.separator + xmlFileName;

			log.warning("@Authorizing Xml@ -> " + file_name);

			signature.setResource_To_Sign(file_name);

			msg = signature.respuestaAutorizacionComprobante(accesscode, authorization, authorization.getValue());

			if (msg != null) {
				authorization.set_ValueOfColumn("IsSRI_Error", true);
				authorization.saveEx();
				throw new AdempiereException(msg);
			}

			m_created++;

			if (inout.get_Value("SRI_Authorization_ID")==null){
				inout.set_ValueOfColumn("SRI_Authorization_ID", authorization.getSRI_Authorization_ID());
				inout.saveEx();
			}

			if (authorization.getAD_UserMail()==null){
				authorization.setAD_UserMail_ID(inout.getAD_User_ID());
				authorization.saveEx();
			}

			Trx sriTrx = null;
			sriTrx = Trx.get(inout.get_TrxName(), false);
			if (sriTrx != null) 
				sriTrx.commit();

			inout.load(sriTrx.getTrxName());
			sendMail(authorization.get_ID(), null);
		} catch (Exception e) {
			msg = "No se pudo obtener autorizacion - " + e.getMessage();
			log.severe(msg);
			msgError = msg;
			//throw new AdempiereException(msg);
		}

		return msg;

	} // lecfeinout_SriExportInOutXML100

	/**
	 * 	lecfemovement_SriExportMovementXML100
	 */
	public String lecfemovement_SriExportMovementXML100 (X_SRI_Authorization authorization)
	{
		String msg = null;

		int c_movement_id = LEC_FE_Utils.getAuthorisedMovement(authorization.getSRI_Authorization_ID());

		if(c_movement_id==0) {
			authorization.set_ValueOfColumn("IsSRI_Error", true);
			authorization.saveEx();
			msg = "Error obteniendo movimiento";
			log.severe(msg);
			msgError = msg;
			return msg;
		}

		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();
		MMovement movement = new MMovement(getCtx(), c_movement_id, get_TrxName());
		MDocType dt = new MDocType(getCtx(), movement.getC_DocType_ID(), get_TrxName());

		String coddoc = dt.get_ValueAsString("SRI_ShortDocType");

		try {

			X_SRI_AccessCode accesscode = new X_SRI_AccessCode (getCtx(), authorization.getSRI_AccessCode_ID(), get_TrxName());
			String xmlFileName = "SRI_" + coddoc + "-" + LEC_FE_Utils.getDate(movement.getMovementDate(),9) + "-" + accesscode.getValue() + "_sig.xml";

			//ruta completa del archivo xml
			file_name = signature.getFolderRaiz() + File.separator + LEC_FE_UtilsXml.folderComprobantesFirmados + File.separator + xmlFileName;

			log.warning("@Authorizing Xml@ -> " + file_name);

			signature.setResource_To_Sign(file_name);

			msg = signature.respuestaAutorizacionComprobante(accesscode, authorization, authorization.getValue());

			if (msg != null) {
				authorization.set_ValueOfColumn("IsSRI_Error", true);
				authorization.saveEx();
				throw new AdempiereException(msg);
			}
			
			m_created++;

			if (movement.get_Value("SRI_Authorization_ID")==null){
				movement.set_ValueOfColumn("SRI_Authorization_ID", authorization.getSRI_Authorization_ID());
				movement.saveEx();
			}

			if (authorization.getAD_UserMail()==null){
				authorization.setAD_UserMail_ID(movement.getAD_User_ID());
				authorization.saveEx();
			}

			Trx sriTrx = null;
			sriTrx = Trx.get(movement.get_TrxName(), false);
			if (sriTrx != null) 
				sriTrx.commit();

			movement.load(sriTrx.getTrxName());
			sendMail(authorization.get_ID(), null);

			//
		} catch (Exception e) {
			msg = "No se pudo obtener autorizacion - " + e.getMessage();
			log.severe(msg);
			msgError = msg;
			//throw new AdempiereException(msg);
		}

		return msg;

	} // lecfemovement_SriExportMovementXML100

	public static void sendMail(int p_authorization, Trx trx) {
		MProcess process = new Query(Env.getCtx(), MProcess.Table_Name,
				"classname = ?", null).setParameters(
						"org.globalqss.process.SRIEmailAuthorization").first();
		if (process != null) {

			ProcessInfo processInfo = new ProcessInfo(process.getName(),
					process.get_ID());
			MPInstance instance = new MPInstance(Env.getCtx(),
					processInfo.getAD_Process_ID(), processInfo.getRecord_ID());
			instance.save();

			ProcessInfoParameter[] para = { new ProcessInfoParameter(
					"SRI_Authorization_ID", p_authorization, null, null, null) };
			processInfo.setAD_Process_ID(process.get_ID());
			processInfo.setClassName(process.getClassname());
			processInfo.setAD_PInstance_ID(instance.getAD_PInstance_ID());
			processInfo.setParameter(para);

			ProcessUtil.startJavaProcess(Env.getCtx(), processInfo, trx, true);
		}
	}

}	//	SRIProcessOfflineAuthorizations

