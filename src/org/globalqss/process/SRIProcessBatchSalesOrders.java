package org.globalqss.process;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MOrder;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.globalqss.model.LEC_FE_MInOut;
import org.globalqss.model.LEC_FE_MInvoice;
import org.globalqss.model.LEC_FE_ModelValidator;
import org.globalqss.model.X_SRI_IssueProcessBath;

public class SRIProcessBatchSalesOrders extends SvrProcess {

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
		if (p_DateTrx == null){
			p_DateTrx = Env.getContextAsDate(getCtx(), "#Date");
		}
			
		Timestamp timeDate = Env.getContextAsDate(getCtx(), "#Date");
		p_DateTrx.setHours(0);
		p_DateTrx.setMinutes(0);
		p_DateTrx.setSeconds(0);
		String msg = null;
		List<MOrder> orders = new Query(getCtx(),
				MOrder.Table_Name,
				"Docstatus IN ('CO','CL') AND IsSOTrx = 'Y' AND DateOrdered = ?",
				get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(p_DateTrx)
				.setOrderBy("DocumentNo")
				.list();
		
		if (orders == null) {
			log.warning("No unprocessed Orders on MOrder ");
			return "No unprocessed Orders on MOrder";
		}
		
		Trx trx = Trx.get(Trx.createTrxName("SRIBatchOrder"),true);
		
		for (MOrder order : orders){
			log.warning("SRIProcessBatch - Order No. ".concat(order.getDocumentNo().trim()));
			trx.start();
			
			try{
				order.set_TrxName(trx.getTrxName());
				
				MInvoice[] invoices = getInvoices(order, " AND SRI_Authorization_ID IS NULL AND DocStatus IN ('CO','CL') AND C_DoctypeTarget_ID IN (SELECT C_Doctype_ID FROM C_Doctype WHERE SRI_ShortDoctype = '01' AND IsActive = 'Y' AND IsSOTrx = 'Y')");
				
				for (MInvoice invoice : invoices) {
					LEC_FE_MInvoice inv = new LEC_FE_MInvoice(order.getCtx(), invoice.get_ID(), order.get_TrxName());
					MUser user = new MUser(getCtx(), inv.getAD_User_ID(), inv.get_TrxName());
					if (LEC_FE_ModelValidator.valideUserMail(user)){
						msg = inv.lecfeinv_SriExportInvoiceXML100();
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
						if (inv.get_Value("SRI_Authorization_ID")!=null){
							LEC_FE_ModelValidator.sendMail(inv.get_ValueAsInt("SRI_Authorization_ID"),null);
							inv.saveEx();
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
				
				if (!MDocType.DOCSUBTYPESO_POSOrder.equals(order.getC_DocType().getDocSubTypeSO())){
					MInOut[] inouts = getShipments(order, " AND SRI_Authorization_ID IS NULL AND DocStatus IN ('CO','CL') AND C_Doctype_ID IN (SELECT C_Doctype_ID FROM C_Doctype WHERE SRI_ShortDoctype = '06' AND IsActive = 'Y' AND IsSOTrx = 'Y')");
					
					for (MInOut io : inouts) {
						LEC_FE_MInOut ino = new LEC_FE_MInOut(order.getCtx(), io.get_ID(), order.get_TrxName());
						MUser user = new MUser(getCtx(), ino.getAD_User_ID(), ino.get_TrxName());
						if (LEC_FE_ModelValidator.valideUserMail(user)){
							ino.lecfeinout_SriExportInOutXML100();
							ino.saveEx();
							if (msg != null){
								X_SRI_IssueProcessBath issue = new X_SRI_IssueProcessBath(ino.getCtx(), 0, ino.get_TrxName());
								issue.setAD_Org_ID(ino.getAD_Org_ID());
								issue.setM_InOut_ID(ino.get_ID());
								issue.setComments(msg);
								issue.setDateTrx(timeDate);
								issue.setAD_Process_ID(getProcessInfo().getAD_Process_ID());
								issue.saveEx();
							}
							trx.commit();
							if (ino.get_Value("SRI_Authorization_ID")!=null){
								LEC_FE_ModelValidator.sendMail(ino.get_ValueAsInt("SRI_Authorization_ID"),null);
								ino.saveEx();
							}
						}
						else{
							X_SRI_IssueProcessBath issue = new X_SRI_IssueProcessBath(ino.getCtx(), 0, ino.get_TrxName());
							issue.setAD_Org_ID(ino.getAD_Org_ID());
							issue.setM_InOut_ID(ino.get_ID());
							issue.setComments("Error en Usuario Registrado para Envio de Correo");
							issue.setDateTrx(timeDate);
							issue.setAD_Process_ID(getProcessInfo().getAD_Process_ID());
							issue.saveEx();
						}
						trx.commit();
					}
				}
				
				order.saveEx();
				trx.commit();
			} catch(Exception e){
				trx.rollback();
				log.severe("Order can not be processed - " + e.getMessage());
				throw new AdempiereException(e.getMessage());
			}
		}
		
		return "Proceso Completado Satifactoriamente";
	}
	
	/**
	 * 	Get Invoices of Order
	 * 	@return invoices
	 */
	public MInvoice[] getInvoices(MOrder order, String where)
	{
		String whereClause = "EXISTS (SELECT 1 FROM C_InvoiceLine il, C_OrderLine ol"
							        +" WHERE il.C_Invoice_ID=C_Invoice.C_Invoice_ID"
							        		+" AND il.C_OrderLine_ID=ol.C_OrderLine_ID"
							        		+" AND ol.C_Order_ID=?)";
		
		if (where != null)
			whereClause += " "+where;
		
		List<MInvoice> list = new Query(getCtx(), LEC_FE_MInvoice.Table_Name, whereClause, get_TrxName())
									.setParameters(order.get_ID())
									.setOrderBy("C_Invoice_ID DESC")
									.list();
		return list.toArray(new MInvoice[list.size()]);
	}	//	getInvoices
	
	/**
	 * 	Get Shipments of Order
	 * 	@return shipments
	 */
	public MInOut[] getShipments(MOrder order, String where)
	{
		String whereClause = "EXISTS (SELECT 1 FROM M_InOutLine iol, C_OrderLine ol"
			+" WHERE iol.M_InOut_ID=M_InOut.M_InOut_ID"
			+" AND iol.C_OrderLine_ID=ol.C_OrderLine_ID"
			+" AND ol.C_Order_ID=?)";
		
		if (where != null)
			whereClause += " "+where;
		
		List<MInOut> list = new Query(getCtx(), LEC_FE_MInOut.Table_Name, whereClause, get_TrxName())
									.setParameters(order.get_ID())
									.setOrderBy("M_InOut_ID DESC")
									.list();
		return list.toArray(new MInOut[list.size()]);
	}	//	getShipments

}
