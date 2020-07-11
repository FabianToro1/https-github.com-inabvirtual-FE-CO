package org.globalqss.process;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.globalqss.model.LEC_FE_MInOut;
import org.globalqss.model.LEC_FE_ModelValidator;
import org.globalqss.model.X_SRI_IssueProcessBath;

public class SRIProcessBatchInOuts extends SvrProcess {

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
		List<MInOut> inouts = new Query(getCtx(),
				MInOut.Table_Name,
				"Docstatus IN ('CO','CL') AND SRI_Authorization_ID IS NULL AND MovementDate = ? AND C_Doctype_ID IN (SELECT C_Doctype_ID FROM C_Doctype WHERE SRI_ShortDoctype = '06' AND IsActive = 'Y' AND IsSOTrx = 'Y') AND M_InOut_ID = 1146922",
				get_TrxName())
				.setOnlyActiveRecords(true)
				.setParameters(p_DateTrx)
				.setOrderBy("DocumentNo")
				.list();
		
		if (inouts == null) {
			log.warning("No unprocessed Inouts on MInOut");
			return "No unprocessed Inouts on MInOut";
		}
		
		Trx trx = Trx.get(Trx.createTrxName("SRIBatchInOut"),true);
		
		for (MInOut io : inouts){
			log.warning("SRIProcessBatch - InOut No. ".concat(io.getDocumentNo().trim()));
			trx.start();
				try{
					LEC_FE_MInOut ino = new LEC_FE_MInOut(getCtx(), io.get_ID(), get_TrxName());
					ino.set_TrxName(trx.getTrxName());
					MUser user = new MUser(getCtx(), ino.getAD_User_ID(), ino.get_TrxName());
					if (LEC_FE_ModelValidator.valideUserMail(user)){
						msg = ino.lecfeinout_SriExportInOutXML100();
						ino.saveEx();
						if (msg != null){
							X_SRI_IssueProcessBath issue = new X_SRI_IssueProcessBath(ino.getCtx(), 0, ino.get_TrxName());
							issue.setAD_Org_ID(ino.getAD_Org_ID());
							issue.setM_InOut_ID(io.get_ID());
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
				catch(Exception e){
					trx.rollback();
					log.severe("Invoice can not be processed - " + e.getMessage());
					throw new AdempiereException(e.getMessage());
				}
			}
		
		return "Proceso Completado Satifactoriamente";
	}

}
