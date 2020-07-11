package org.globalqss.process;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInvoice;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.globalqss.model.LEC_FE_MRetencion;
import org.globalqss.model.LEC_FE_ModelValidator;
import org.globalqss.model.X_SRI_IssueProcessBath;

public class SRIProcessBatchWithholdings extends SvrProcess {

	Timestamp p_DateTrx = null;
	
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("DateTrx"))
				p_DateTrx = para[i].getParameterAsTimestamp();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}			
	}

	@SuppressWarnings("deprecation")
	@Override
	protected String doIt() throws Exception {
		
		String sqlWhere = "DocStatus IN ('IP','CO') AND SRI_Authorization_ID IS NULL AND IsSOTrx = 'N' AND C_Invoice_ID IN (SELECT C_Invoice_ID FROM LCO_InvoiceWithholding WHERE IsActive = 'Y' AND DateAcct = ?) AND C_DoctypeTarget_ID IN (SELECT C_Doctype_ID FROM C_Doctype WHERE SRI_ShortDoctype = '07' AND IsActive = 'Y' AND IsSOTrx = 'N')";
		if (p_DateTrx == null){
			p_DateTrx = Env.getContextAsDate(getCtx(), "#Date");
		}
			
		Timestamp timeDate = Env.getContextAsDate(getCtx(), "#Date");
		p_DateTrx.setHours(0);
		p_DateTrx.setMinutes(0);
		p_DateTrx.setSeconds(0);
		String msg = null;
		List<MInvoice> invoices = new Query(getCtx(),
				MInvoice.Table_Name,
				sqlWhere,
				get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(p_DateTrx)
				.setOrderBy("DateAcct, DocumentNo")
				.list();
		
		if (invoices.size() < 1) {
			log.warning("No unprocessed Invoices on MInvoice");
			return "No unprocessed Invoices on MInvoice";
		}
		
		Trx trx = Trx.get(Trx.createTrxName("SRIBatchWithholding"),true);
		for (MInvoice invoice : invoices){
			log.warning("SRIProcessBatch - Invoice No. ".concat(invoice.getDocumentNo().trim()));
			trx.start();
				try{
					LEC_FE_MRetencion inv = new LEC_FE_MRetencion(getCtx(), invoice.get_ID(), get_TrxName());
					inv.set_TrxName(trx.getTrxName());
					LEC_FE_MRetencion.generateWitholdingNo(new MInvoice(getCtx(), inv.get_ID(), inv.get_TrxName()));
					trx.commit();
					MUser user = new MUser(getCtx(), inv.getAD_User_ID(), inv.get_TrxName());
					if (LEC_FE_ModelValidator.valideUserMail(user)){
						msg = inv.lecfeinvret_SriExportRetencionXML100();
						inv.saveEx();
						if (msg != null){
							X_SRI_IssueProcessBath issue = new X_SRI_IssueProcessBath(inv.getCtx(), 0, inv.get_TrxName());
							issue.setAD_Org_ID(inv.getAD_Org_ID());
							issue.setC_Invoice_ID(inv.get_ID());
							issue.setComments(msg);
							issue.setDateTrx(timeDate);
							issue.setAD_Process_ID(getProcessInfo().getAD_Process_ID());
							issue.saveEx();
						}
					}
					else{
						X_SRI_IssueProcessBath issue = new X_SRI_IssueProcessBath(inv.getCtx(), 0, inv.get_TrxName());
						issue.setAD_Org_ID(inv.getAD_Org_ID());
						issue.setC_Invoice_ID(inv.get_ID());
						issue.setComments("Error en Usuario Registrado para Envio de Correo");
						issue.setDateTrx(timeDate);
						issue.setAD_Process_ID(getProcessInfo().getAD_Process_ID());
						issue.saveEx();
					}
					
					trx.commit();
				}
				catch(Exception e){
					trx.rollback();
					log.severe("Invoice can not be processed - " + e.getMessage());
					throw new AdempiereException(e.getMessage());
				}
			}
		
		return "Proceso Completado Satifactoriamente";
	}

}
