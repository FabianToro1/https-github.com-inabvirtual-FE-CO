package org.globalqss.process;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MMovement;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.globalqss.model.LEC_FE_ModelValidator;
import org.globalqss.model.LEC_FE_Movement;
import org.globalqss.model.X_SRI_IssueProcessBath;

public class SRIProcessBatchMovements extends SvrProcess {

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
		List<MMovement> movements = new Query(getCtx(),
				MMovement.Table_Name,
				"Docstatus IN ('CO','CL') AND SRI_Authorization_ID IS NULL AND MovementDate = ? AND C_Doctype_ID IN (SELECT C_Doctype_ID FROM C_Doctype WHERE SRI_ShortDoctype = '06' AND IsActive = 'Y')",
				get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(p_DateTrx)
				.setOrderBy("DocumentNo")
				.list();
		
		if (movements == null) {
			log.warning("No unprocessed Invoices on MInvoice");
			return "No unprocessed Movements on MMovement";
		}
		
		Trx trx = Trx.get(Trx.createTrxName("SRIBatchMovement"),true);
		
		for (MMovement movement : movements){
			log.warning("SRIProcessBatch - Movement No. ".concat(movement.getDocumentNo().trim()));
			trx.start();
				try{	
					LEC_FE_Movement mov = new LEC_FE_Movement(getCtx(), movement.get_ID(), get_TrxName());
					mov.set_TrxName(trx.getTrxName());
					MUser user = new MUser(getCtx(), mov.getAD_User_ID(), mov.get_TrxName());
					if (LEC_FE_ModelValidator.valideUserMail(user)){
						msg = mov.lecfeMovement_SriExportMovementXML100();
						mov.saveEx();
						if (msg != null){
							X_SRI_IssueProcessBath issue = new X_SRI_IssueProcessBath(mov.getCtx(), 0, mov.get_TrxName());
							issue.setAD_Org_ID(mov.getAD_Org_ID());
							issue.setM_Movement_ID(mov.get_ID());
							issue.setComments(msg);
							issue.setDateTrx(timeDate);
							issue.setAD_Process_ID(getProcessInfo().getAD_Process_ID());
							issue.saveEx();
							
						}
						trx.commit();
						if (mov.get_Value("SRI_Authorization_ID")!=null){
							LEC_FE_ModelValidator.sendMail(mov.get_ValueAsInt("SRI_Authorization_ID"),null);
							mov.saveEx();
						}
					}
					else{
						X_SRI_IssueProcessBath issue = new X_SRI_IssueProcessBath(mov.getCtx(), 0, mov.get_TrxName());
						issue.setAD_Org_ID(mov.getAD_Org_ID());
						issue.setM_Movement_ID(mov.get_ID());
						issue.setComments("Error en Usuario Registrado para Envio de Correo");
						issue.setDateTrx(timeDate);
						issue.setAD_Process_ID(getProcessInfo().getAD_Process_ID());
						issue.saveEx();
					}
					trx.commit();
				}
				catch(Exception e){
					trx.rollback();
					log.severe("Movement can not be processed - " + e.getMessage());
					throw new AdempiereException(e.getMessage());
				}
			}
		
		return "Proceso Completado Satifactoriamente";
	}

}
