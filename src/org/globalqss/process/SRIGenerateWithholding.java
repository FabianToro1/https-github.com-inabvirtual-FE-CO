package org.globalqss.process;

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInvoice;
import org.compiere.model.MUser;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Trx;
import org.globalqss.model.LEC_FE_MRetencion;
import org.globalqss.model.LEC_FE_ModelValidator;

public class SRIGenerateWithholding extends SvrProcess {

	int  m_C_Invoice_ID = 0;
	
	@Override
	protected void prepare() {
		
		ProcessInfoParameter[] para = getParameter();
		
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("C_Invoice_ID"))
				m_C_Invoice_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
		
		if (m_C_Invoice_ID == 0)
			m_C_Invoice_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		
		String msg = null;
		//String noretencion= "";
		
		MInvoice inv =new MInvoice(getCtx(), m_C_Invoice_ID, get_TrxName());
		
		MUser user = new MUser(inv.getCtx(), inv.getAD_User_ID(), inv.get_TrxName());
		
		if (!LEC_FE_ModelValidator.valideUserMail (user)) {
			msg = "@RequestActionEMailNoTo@";
			return msg;
		}
		
		try
		{
			
			LEC_FE_MRetencion lecfeinvret = new LEC_FE_MRetencion(inv.getCtx(), inv.getC_Invoice_ID(), inv.get_TrxName());
			
			// !isSOTrx()
			if (lecfeinvret.get_ValueAsInt("SRI_Authorization_ID") < 1){
				//noretencion = LEC_FE_MRetencion.generateWitholdingNo(inv);
				commitEx();
				msg = lecfeinvret.lecfeinvret_SriExportRetencionXML100();
				lecfeinvret.saveEx();
				Trx sriTrx = null;
				sriTrx = Trx.get(lecfeinvret.get_TrxName(), false);
				if (sriTrx!=null){
					sriTrx.commit();
				}
				lecfeinvret.load(sriTrx.getTrxName());
				if (lecfeinvret.get_Value("SRI_Authorization_ID")!=null){
					LEC_FE_ModelValidator.sendMail(lecfeinvret.get_ValueAsInt("SRI_Authorization_ID"),null);
					lecfeinvret.saveEx();
				}
			}else
				msg = "Ya existe una Autorización asociadas a las Retenciones.";
			
			if (msg != null)
        		throw new AdempiereException(msg);
		
		}
		catch (Exception e)
		{
			msg = e.getMessage();
			log.severe(msg);
			throw new AdempiereException(msg);
		}
		
		return "@OK@";
	}

}
