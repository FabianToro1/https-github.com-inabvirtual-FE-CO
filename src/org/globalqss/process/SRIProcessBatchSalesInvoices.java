package org.globalqss.process;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.ProcessUtil;
import org.compiere.model.MBPartner;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MMailText;
import org.compiere.model.MOrder;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.globalqss.model.LEC_FE_MInvoice;
import org.globalqss.model.LEC_FE_ModelValidator;
import org.globalqss.model.X_LEC_SRI_Format;
import org.globalqss.model.X_SRI_AccessCode;
import org.globalqss.model.X_SRI_Authorization;
import org.globalqss.model.X_SRI_IssueProcessBath;
import org.globalqss.util.LEC_FE_ProcessOnThread;
import org.globalqss.util.LEC_FE_Utils;
import org.globalqss.util.LEC_FE_UtilsXml;

public class SRIProcessBatchSalesInvoices extends SvrProcess {

	Timestamp p_DateTrx = null;
	String p_DocAction = "";

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (name.equals("DateTrx"))
				p_DateTrx = para[i].getParameterAsTimestamp();
			else if (name.equals("DocAction"))
				p_DocAction = para[i].getParameterAsString();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected String doIt() throws Exception {
		String result = "";

		if (p_DocAction.equals("RPA")) {// Reprocess Autorization NOT
										// CONTINGENCY
			if (p_DateTrx == null) {
				p_DateTrx = Env.getContextAsDate(getCtx(), "#Date");
			}

			String whereSql = "SRI_AuthorizationCode Is NUll AND (SRI_ErrorCode_ID IS NULL OR SRI_ErrorCode_ID = "
					+ "(Select ec.SRI_ErrorCode_ID FROM SRI_ErrorCode ec WHERE ec.Value ='70')) "
					+ "AND SRI_ShortDocType = '01' AND Value IS NOT NULL AND AD_UserMail_ID IS NOT NULL "
					+ "AND EXISTS (Select acc.SRI_AccessCode_ID FROM SRI_AccessCode acc WHERE acc.SRI_AccessCode_ID = SRI_Authorization.SRI_AccessCode_ID AND acc.CodeAccessType = '1') "
					+ "AND Date_Trunc('day'::text, Created::timestamp) = ? ";
			List<X_SRI_Authorization> autorizations = new Query(getCtx(),
					X_SRI_Authorization.Table_Name, whereSql, get_TrxName())
					.setOnlyActiveRecords(true).setParameters(p_DateTrx)
					.setOrderBy("Created").list();

			if (autorizations == null) {
				log.warning("No unprocessed Autorization on MInvoice ");
				return "No unprocessed Autorization on MInvoice";
			}

			int count = 0;
			Vector<Thread> threadAutorizations = new Vector<Thread>();
			for (X_SRI_Authorization autorization : autorizations) {
				Integer autorizationID = autorization.getSRI_Authorization_ID();
				LEC_FE_ProcessOnThread tautorization = new LEC_FE_ProcessOnThread(
						getCtx(), autorizationID,
						LEC_FE_ProcessOnThread.SRI_ShortDocType_Invoice, "RQA",
						autorization.getAD_UserMail_ID(),
						Env.getProcessInfo(getCtx()));
				tautorization.start();
				threadAutorizations.add(tautorization);
				count++;
				addLog(0, null, null,
						" \nAutorización-> " + autorization.getValue());
			}
			result = "Se Programaron " + count
					+ " Autorizaciones para su aprobación\n";
			// End Reprocess Autorization
		} else if (p_DocAction.equals("RQA")) {// Request Autorization

			if (p_DateTrx == null) {
				p_DateTrx = Env.getContextAsDate(getCtx(), "#Date");
			}

			String whereSql = "Docstatus IN ('CO','CL') AND SRI_Authorization_ID IS NULL AND IsSOTrx = 'Y' AND Date_Trunc('day'::text, DateInvoiced::timestamp) = ? "
					+ "AND C_DocTypeTarget_ID IN (SELECT C_Doctype_ID FROM C_Doctype WHERE SRI_ShortDoctype IN ('01') AND IsActive = 'Y' "
					+ "AND IsSOTrx = 'Y') AND SRI_IsUseContingency = 'N' "
					+ "AND NOT Exists (Select 1 FROM SRI_Authorization WHERE DocumentID = c_invoice_id::Text)";
			List<MInvoice> invoices = new Query(getCtx(), MInvoice.Table_Name,
					whereSql, get_TrxName()).setOnlyActiveRecords(true)
					.setParameters(p_DateTrx).setOrderBy("DocumentNo").list();

			if (invoices.isEmpty()) {
				log.warning("No unprocessed Invoices on MInvoice ");
				return "No unprocessed Invoices on MInvoice";
			}

			Vector<Thread> threadInvoices = new Vector<Thread>();
			int count = 0;
			for (MInvoice invoice : invoices) {
				Integer invoiceID = invoice.getC_Invoice_ID();
				// Thread tinivoice = threadBatchSalesInvoiceRQA(
				// invoiceID.intValue(), timeDate);
				LEC_FE_ProcessOnThread tinivoice = new LEC_FE_ProcessOnThread(
						getCtx(), invoiceID,
						LEC_FE_ProcessOnThread.SRI_ShortDocType_Invoice, "RQA",
						invoice.getAD_User_ID(), Env.getProcessInfo(getCtx()));
				tinivoice.start();
				threadInvoices.add(tinivoice);
				count++;
				addLog(0, null, null, " \nFactura-> " + invoice.getDocumentNo());

			}

			result = "Se programaron :"
					+ count
					+ " documentos para su aprobación, recuerde ver los resultados en:"
					+ Msg.translate(Env.getAD_Language(getCtx()),
							X_SRI_IssueProcessBath.Table_Name);

			// End Request Autorization
		} else if (p_DocAction.equals("PRC")) {// Processing Contingency
			if (p_DateTrx == null) {
				p_DateTrx = Env.getContextAsDate(getCtx(), "#Date");
			}

			Timestamp timeDate = Env.getContextAsDate(getCtx(), "#Date");
			p_DateTrx.setHours(0);
			p_DateTrx.setMinutes(0);
			p_DateTrx.setSeconds(0);

			String whereSql = "SRI_AuthorizationCode Is NUll AND (SRI_ErrorCode_ID = "
					+ " (Select ec.SRI_ErrorCode_ID FROM SRI_ErrorCode ec WHERE ec.Value ='70') OR SRI_ErrorCode_ID = "
					+ " (Select ec.SRI_ErrorCode_ID FROM SRI_ErrorCode ec WHERE ec.Value ='170')) "
					+ " AND SRI_ShortDocType = '01' AND Value IS NOT NULL AND AD_UserMail_ID IS NOT NULL AND  "
					+ "	EXISTS (Select acc.SRI_AccessCode_ID FROM SRI_AccessCode acc WHERE acc.SRI_AccessCode_ID = SRI_Authorization.SRI_AccessCode_ID "
					+ "	AND acc.CodeAccessType = '2') AND Date_Trunc('day'::text, Created::timestamp) = Date_Trunc('day'::text, ?::timestamp) ";
			List<X_SRI_Authorization> autorizations = new Query(getCtx(),
					X_SRI_Authorization.Table_Name, whereSql, get_TrxName())
					.setOnlyActiveRecords(true).setParameters(p_DateTrx)
					.setOrderBy("Created").list();

			if (autorizations == null) {
				log.warning("No unprocessed Autorization on MInvoice ");
				return "No unprocessed Autorization on MInvoice";
			}

			int count = 0;
			Vector<Thread> threadAutorizations = new Vector<Thread>();
			for (X_SRI_Authorization autorization : autorizations) {
				Integer autorizationID = autorization.getSRI_Authorization_ID();
				// Thread tautorization = threadBatchSalesInvoicePRC(
				// autorizationID.intValue(), timeDate);
				// tautorization.start();
				// threadAutorizations.add(tautorization);
				count++;

				addLog(0, null, null,
						" \nAutorización-> " + autorization.getValue());
			}
			result = "Se Programaron " + count
					+ " Autorizaciones para su aprobación";
			// End Reprocess Autorization
		}// End Processing Contingency

		return result;
	}

	/**
	 * Get Shipments of Order
	 * 
	 * @return shipments
	 */
	public MInOut[] getShipments(MOrder order, String where) {
		String whereClause = "EXISTS (SELECT 1 FROM M_InOutLine iol, C_OrderLine ol"
				+ " WHERE iol.M_InOut_ID=M_InOut.M_InOut_ID"
				+ " AND iol.C_OrderLine_ID=ol.C_OrderLine_ID"
				+ " AND ol.C_Order_ID=?)";

		if (where != null)
			whereClause += " " + where;

		List<MInOut> list = new Query(getCtx(), MInOut.Table_Name, whereClause,
				get_TrxName()).setParameters(order.get_ID())
				.setOrderBy("M_InOut_ID DESC").list();
		return list.toArray(new MInOut[list.size()]);
	} // getShipments

	

}
