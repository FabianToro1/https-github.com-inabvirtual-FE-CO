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

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.logging.Level;


import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MMovement;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.globalqss.model.LEC_FE_MInOut;
import org.globalqss.model.LEC_FE_MInvoice;
import org.globalqss.model.LEC_FE_MNotaCredito;
import org.globalqss.model.LEC_FE_MNotaDebito;
import org.globalqss.model.LEC_FE_MRetencion;
import org.globalqss.model.LEC_FE_ModelValidator;
import org.globalqss.model.LEC_FE_Movement;

import org.globalqss.model.X_SRI_Authorization;


/**
 *	Generate Contingency Authorizations
 *	
 *  @author GlobalQSS/jjgq
 */
public class SRIGenerateOfflineAuthorizations extends SvrProcess
{

	/** Invoice					*/
	private int			p_C_Invoice_ID = 0;

	/** Number of authorizations	*/
	private int			m_created = 0;
	
	String[] m_tables = {"C_Invoice","M_Movement","M_InOut"};
	
	private static CLogger log = CLogger
			.getCLogger(LEC_FE_ModelValidator.class);

	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
	}	//	prepare

	/**
	 * 	Generate Invoices
	 *	@return info
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
	
		String msg="";
		
		for(String table : m_tables)
		{
			log.info("Processing " + table);
			//
			String sql = null;
						
			sql = "SELECT * FROM "+ table +" a "
					+ " JOIN C_DocType dt on a.C_DocType_ID = dt.C_DocType_ID "
					+ " WHERE a.AD_Client_ID=? "
					+ " AND a.IsActive = 'Y' AND a.Processed = 'Y' "
					+ " AND a.isSRIOfflineSchema = 'Y' "
					+ " AND a.docstatus = 'CO' "
					+ " AND a.SRI_Authorization_ID is null "
					+ " AND a.IsSRI_Error = 'N' "
					+ " AND dt.sri_shortdoctype notnull ";
			
			
			PreparedStatement pstmt = null;
			try
			{
				pstmt = DB.prepareStatement (sql, get_TrxName());
				int index = 1;
				pstmt.setInt(index++, getAD_Client_ID());

			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, sql, e);
			}
			msg = generate(pstmt, table);
		}
		
		return msg;
		
	}	//	doIt

	private String generate (PreparedStatement pstmt, String table)
	{
		String msg = null;
		try
		{
			
			ResultSet rs = pstmt.executeQuery ();
			while (rs.next ())
			{				
				msg= "";
				if ("C_Invoice".equals(table)){
					MInvoice inv = new MInvoice (getCtx(), rs, get_TrxName());
					invoiceGenerateXml(inv);
				}
				else if ("M_InOut".equals(table)){
					MInOut io = new MInOut (getCtx(), rs, get_TrxName());
					inoutGenerateXml(io);			
				}
				else if ("M_Movement".equals(table)){
					MMovement mov = new MMovement (getCtx(), rs, get_TrxName());
					movementGenerateXml(mov);
				}
				m_created++;
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
	}
	
	
	private String invoiceGenerateXml(MInvoice inv) {
		int autorization_id = 0;
		if (inv.get_Value("SRI_Authorization_ID") != null) {
			autorization_id = inv.get_ValueAsInt("SRI_Authorization_ID");
		}
		if (autorization_id != 0) {
			X_SRI_Authorization a = new X_SRI_Authorization(inv.getCtx(),
					autorization_id, inv.get_TrxName());
			if (a != null) {
				if (a.getSRI_AuthorizationDate() != null) {
					// Comprobante autorizado, no se envia de nuevo el xml.
					return null;
				}
			}
		}
		String msg = null;

		MDocType dt = new MDocType(inv.getCtx(), inv.getC_DocTypeTarget_ID(),
				inv.get_TrxName());

		String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

		if (shortdoctype.equals("")) {
			msg = "No existe definicion SRI_ShortDocType: " + dt.toString();
			log.info("Invoice: " + inv.toString() + msg);

			// if (LEC_FE_Utils.breakDialog(msg)) return "Cancelado..."; // Temp

		}

		MUser user = new MUser(inv.getCtx(), inv.getAD_User_ID(),
				inv.get_TrxName());

		if (!valideUserMail(user) && !shortdoctype.equals("")) {
			msg = "@RequestActionEMailNoTo@";
			return msg;
		}

		msg = null;
		LEC_FE_MInvoice lecfeinv = new LEC_FE_MInvoice(inv.getCtx(),
				inv.getC_Invoice_ID(), inv.get_TrxName());
		LEC_FE_MNotaCredito lecfeinvnc = new LEC_FE_MNotaCredito(inv.getCtx(),
				inv.getC_Invoice_ID(), inv.get_TrxName());
		LEC_FE_MNotaDebito lecfeinvnd = new LEC_FE_MNotaDebito(inv.getCtx(),
				inv.getC_Invoice_ID(), inv.get_TrxName());
		LEC_FE_MRetencion lecfeinvret = new LEC_FE_MRetencion(inv.getCtx(),
				inv.getC_Invoice_ID(), inv.get_TrxName());
		// isSOTrx()
		if (inv.isSOTrx())
			LEC_FE_MRetencion.generateWitholdingNo(inv);

		if (shortdoctype.equals("01")) { // FACTURA
			msg = lecfeinv.lecfeinv_SriExportInvoiceXML100();
		} else if (shortdoctype.equals("04")) { // NOTA DE CRÉDITO
			msg = lecfeinvnc.lecfeinvnc_SriExportNotaCreditoXML100();
		} else if (shortdoctype.equals("05")) { // NOTA DE DÉBITO
			msg = lecfeinvnd.lecfeinvnd_SriExportNotaDebitoXML100();
			// !isSOTrx()
		} else if (shortdoctype.equals("07")) { // COMPROBANTE DE RETENCIÓN
			if (lecfeinvret.get_ValueAsInt("SRI_Authorization_ID") < 1	&& MSysConfig.getBooleanValue("LEC_GenerateWitholdingToComplete", false,lecfeinvret.getAD_Client_ID())) {
				LEC_FE_MRetencion.generateWitholdingNo(inv);
				//Trx tra = Trx.get(inv.get_TrxName(), false);
				//tra.commit();
				msg = lecfeinvret.lecfeinvret_SriExportRetencionXML100();
			}
		} else
			log.warning("Formato no habilitado SRI: " + dt.toString()
					+ shortdoctype);

		return msg;
	}

	private String inoutGenerateXml(MInOut inout) {
		int autorization_id = 0;
		if (inout.get_Value("SRI_Authorization_ID") != null) {
			autorization_id = inout.get_ValueAsInt("SRI_Authorization_ID");
		}
		X_SRI_Authorization a = new X_SRI_Authorization(inout.getCtx(),
				autorization_id, inout.get_TrxName());
		if (a != null) {
			if (a.getSRI_AuthorizationDate() != null) {
				// Comprobante autorizado, no se envia de nuevo el xml.
				return null;
			}
		}
		String msg = null;

		MDocType dt = new MDocType(inout.getCtx(), inout.getC_DocType_ID(),
				inout.get_TrxName());

		String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

		if (shortdoctype.equals("")) {
			msg = "No existe definicion SRI_ShortDocType: " + dt.toString();
			log.info("Invoice: " + inout.toString() + msg);

			// if (LEC_FE_Utils.breakDialog(msg)) return "Cancelado..."; // Temp
		}

		MUser user = new MUser(inout.getCtx(), inout.getAD_User_ID(),
				inout.get_TrxName());

		if (!valideUserMail(user) && !shortdoctype.equals("")) {
			msg = "@RequestActionEMailNoTo@";
			return msg;
		}

		msg = null;
		LEC_FE_MInOut lecfeinout = new LEC_FE_MInOut(inout.getCtx(),
				inout.getM_InOut_ID(), inout.get_TrxName());
		// isSOTrx()
		if (shortdoctype.equals("06")) // GUÍA DE REMISIÓN
			msg = lecfeinout.lecfeinout_SriExportInOutXML100();
		else
			log.warning("Formato no habilitado SRI: " + dt.toString()
					+ shortdoctype);

		return msg;
	}

	private String movementGenerateXml(MMovement movement) {
		int autorization_id = 0;
		if (movement.get_Value("SRI_Authorization_ID") != null) {
			autorization_id = movement.get_ValueAsInt("SRI_Authorization_ID");
		}
		X_SRI_Authorization a = new X_SRI_Authorization(movement.getCtx(),
				autorization_id, movement.get_TrxName());
		if (a != null) {
			if (a.getSRI_AuthorizationDate() != null) {
				// Comprobante autorizado, no se envia de nuevo el xml.
				return null;
			}
		}
		String msg = null;

		MDocType dt = new MDocType(movement.getCtx(),
				movement.getC_DocType_ID(), movement.get_TrxName());

		String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

		if (shortdoctype.equals("")) {
			msg = "No existe definicion SRI_ShortDocType: " + dt.toString();
			log.info("Invoice: " + movement.toString() + msg);

			// if (LEC_FE_Utils.breakDialog(msg)) return "Cancelado..."; // Temp
		}

		MUser user = new MUser(movement.getCtx(), movement.getAD_User_ID(),
				movement.get_TrxName());

		if (!valideUserMail(user) && !shortdoctype.equals("")) {
			msg = "@RequestActionEMailNoTo@";
			return msg;
		}

		msg = null;
		LEC_FE_Movement lecfemovement = new LEC_FE_Movement(movement.getCtx(),
				movement.getM_Movement_ID(), movement.get_TrxName());
		// Hardcoded 1000418-SIS UIO COMPANIA RELACIONADA
		// if (shortdoctype.equals("06") && dt.getC_DocType_ID() == 1000418) //
		// GUÍA DE REMISIÓN
		if (shortdoctype.equals("06"))
			msg = lecfemovement.lecfeMovement_SriExportMovementXML100();
		else
			log.warning("Formato no habilitado SRI: " + dt.toString()
					+ shortdoctype);

		return msg;
	}

	public static boolean valideUserMail(MUser user) {
		if (MSysConfig.getBooleanValue("QSSLEC_FE_EnvioXmlAutorizadoBPEmail",
				false, user.getAD_Client_ID())) {

			if ((user.get_ID() == 0 || user.isNotificationEMail()
					&& (user.getEMail() == null || user.getEMail().length() == 0))) {
				return false;
			}
		}

		return true;

	} // valideUserMail

}	//	SRIProcessOfflineAuthorizations

