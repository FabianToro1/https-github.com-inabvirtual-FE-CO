package org.globalqss.util;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Properties;

import org.adempiere.util.ProcessUtil;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.globalqss.model.LEC_FE_MInvoice;
import org.globalqss.model.LEC_FE_ModelValidator;
import org.globalqss.model.X_SRI_Authorization;
import org.globalqss.model.X_SRI_IssueProcessBath;

public class LEC_FE_ProcessOnThread extends Thread implements Runnable {

	/** Invoice = 01 */
	public static final String SRI_ShortDocType_Invoice = "01";
	/** Credit Memo = 04 */
	public static final String SRI_ShortDocType_CreditMemo = "04";
	/** Debit Memo = 05 */
	public static final String SRI_ShortDocType_DebitMemo = "05";
	/** Shipment = 06 */
	public static final String SRI_ShortDocType_Shipment = "06";
	/** Withholding = 07 */
	public static final String SRI_ShortDocType_Withholding = "07";
	/** Logger */
	protected CLogger log = CLogger.getCLogger(getClass());

	private int documentID = 0;
	private int userID = 0;
	private String currentShortDocType = null;
	private String docAction = null;
	private Properties m_ctx = null;
	private ProcessInfo m_pi = null;

	public LEC_FE_ProcessOnThread(Properties ctx, int p_documentID,
			String p_shortDocType, String p_docAction, int p_AD_User_ID,
			ProcessInfo pi) {
		super();
		m_ctx = ctx;
		documentID = p_documentID;
		currentShortDocType = p_shortDocType;
		userID = p_AD_User_ID;
		m_pi = pi;
		docAction = p_docAction;
		// TODO Auto-generated constructor stub
	}

	public void run() {
		// TODO Auto-generated method stub
		Trx trx = Trx.get(
				Trx.createTrxName("SRIBatchDocument" + currentShortDocType),
				true);
		String msg = null;
		try {

			log.warning("SRIProcessBatch - ShortDocType:"
					.concat(currentShortDocType));
			trx.start();

			
			int authorizationID = 0;
			if (SRI_ShortDocType_Invoice.equals(currentShortDocType)) {
				if (docAction.equals("RQA")){
				LEC_FE_MInvoice sriDoc = new LEC_FE_MInvoice(m_ctx, documentID,
						trx.getTrxName());
				// TO_DO: Se planea lecfeinv_SriExportInvoiceXML100 para
				// reconsultar en caso de error
				msg = sriDoc.lecfeinv_SriExportInvoiceXML100();
				if (msg == null) {
					if (sriDoc.get_Value("SRI_Authorization_ID") != null) {
						authorizationID = sriDoc
								.get_ValueAsInt("SRI_Authorization_ID");
						sriDoc.saveEx();
					} else
						msg = "No se Actualizó el documento con la autorización.";
				}
				}
			}
			if (authorizationID > 0) {
				X_SRI_Authorization authorization = new X_SRI_Authorization(
						m_ctx, authorizationID, trx.getTrxName());
				msg = "Autorización:" + authorization.getValue() + ", Fecha: "
						+ authorization.getSRI_AuthorizationDate().toString();
			}
			logUpIssue(trx.getTrxName(), msg, true);
			trx.commit();
			LEC_FE_ModelValidator.sendMail(authorizationID, trx);

			trx.commit();

		} catch (Exception e) {
			log.warning("Error Thread SRIProcessBatch");
			e.printStackTrace();
			trx.rollback();
			logUpIssue(null, e.getMessage(), false);
			if (msg != null)
				log.warning(msg);
			log.severe("Invoice can not be processed - " + e.getMessage());
			// throw new AdempiereException(e.getMessage());

		} finally {
			trx.close();
		}
	}

	private void logUpIssue(String trxName, String summary, boolean process) {

		X_SRI_IssueProcessBath issue = new X_SRI_IssueProcessBath(m_ctx, 0,
				trxName);
		issue.setAD_Org_ID(Env.getAD_Org_ID(m_ctx));
		issue.setC_Invoice_ID(documentID);
		issue.setDateTrx((new Timestamp(Calendar.getInstance()
				.getTimeInMillis())));
		issue.setAD_Process_ID(this.m_pi.getAD_Process_ID());
		issue.set_ValueOfColumn("DocAction", docAction);
		issue.setComments(summary);
		issue.set_ValueOfColumn("Processed", process);
		issue.setDateTrx((new Timestamp(Calendar.getInstance()
				.getTimeInMillis())));
		issue.saveEx();

	}

}
