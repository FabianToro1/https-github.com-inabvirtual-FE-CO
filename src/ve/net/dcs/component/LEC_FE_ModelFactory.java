package ve.net.dcs.component;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.globalqss.model.LEC_FE_MInOut;
import org.globalqss.model.LEC_FE_MInvoice;
import org.globalqss.model.LEC_FE_MNotaCredito;
import org.globalqss.model.LEC_FE_MNotaDebito;
import org.globalqss.model.LEC_FE_MRetencion;
import org.globalqss.model.LEC_FE_Movement;
import org.globalqss.model.MSRITaxCode;
import org.globalqss.model.X_LEC_SRI_Format;
import org.globalqss.model.X_SRI_AccessCode;
import org.globalqss.model.X_SRI_Authorization;
import org.globalqss.model.X_SRI_ErrorCode;
import org.globalqss.model.X_SRI_TaxCode;
import org.globalqss.model.X_SRI_TaxRate;

import ec.ingeint.erp.model.X_LLA_WithholdingSequence;

public class LEC_FE_ModelFactory implements IModelFactory  {

	@Override
	public Class<?> getClass(String tableName) {
		if (LEC_FE_Movement.Table_Name.equals(tableName))
			return LEC_FE_Movement.class;
		if (LEC_FE_MInOut.Table_Name.equals(tableName))
			return LEC_FE_MInOut.class;
		if (LEC_FE_MInvoice.Table_Name.equals(tableName))
			return LEC_FE_MInvoice.class;
		if (LEC_FE_MNotaCredito.Table_Name.equals(tableName))
			return LEC_FE_MNotaCredito.class;
		if (LEC_FE_MNotaDebito.Table_Name.equals(tableName))
			return LEC_FE_MNotaDebito.class;
		if (LEC_FE_MRetencion.Table_Name.equals(tableName))
			return LEC_FE_MRetencion.class;
		if (X_LEC_SRI_Format.Table_Name.equals(tableName))
			return X_LEC_SRI_Format.class;
		if (X_SRI_AccessCode.Table_Name.equals(tableName))
			return X_SRI_AccessCode.class;
		if (X_SRI_Authorization.Table_Name.equals(tableName))
			return X_SRI_Authorization.class;
		if (X_SRI_ErrorCode.Table_Name.equals(tableName))
			return X_SRI_ErrorCode.class;
		if (X_LLA_WithholdingSequence.Table_Name.equals(tableName))
			return X_LLA_WithholdingSequence.class;
		if (MSRITaxCode.Table_Name.equals(tableName))
			return MSRITaxCode.class;
		if (X_SRI_TaxRate.Table_Name.equals(tableName)) 
			return X_SRI_TaxRate.class;
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {	
		if (LEC_FE_Movement.Table_Name.equals(tableName))
			return new LEC_FE_Movement(Env.getCtx(), Record_ID, trxName);
		if (LEC_FE_MInOut.Table_Name.equals(tableName))
			return new LEC_FE_MInOut(Env.getCtx(), Record_ID, trxName);
		if (LEC_FE_MInvoice.Table_Name.equals(tableName))
			return new LEC_FE_MInvoice(Env.getCtx(), Record_ID, trxName);
		if (LEC_FE_MNotaCredito.Table_Name.equals(tableName))
			return new LEC_FE_MNotaCredito(Env.getCtx(), Record_ID, trxName);
		if (LEC_FE_MNotaDebito.Table_Name.equals(tableName))
			return new LEC_FE_MNotaDebito(Env.getCtx(), Record_ID, trxName);
		if (LEC_FE_MRetencion.Table_Name.equals(tableName))
			return new LEC_FE_MRetencion(Env.getCtx(), Record_ID, trxName);
		if (X_LEC_SRI_Format.Table_Name.equals(tableName))
			return new X_LEC_SRI_Format(Env.getCtx(), Record_ID, trxName);
		if (X_SRI_AccessCode.Table_Name.equals(tableName))
			return new X_SRI_AccessCode(Env.getCtx(), Record_ID, trxName);
		if (X_SRI_Authorization.Table_Name.equals(tableName))
			return new X_SRI_Authorization(Env.getCtx(), Record_ID, trxName);
		if (X_SRI_ErrorCode.Table_Name.equals(tableName))
			return new X_SRI_ErrorCode(Env.getCtx(), Record_ID, trxName);
		if (X_LLA_WithholdingSequence.Table_Name.equals(tableName))
			return new X_LLA_WithholdingSequence(Env.getCtx(), Record_ID, trxName);
		if (MSRITaxCode.Table_Name.equals(tableName))
			return new MSRITaxCode(Env.getCtx(), Record_ID, trxName);
		if (X_SRI_TaxRate.Table_Name.equals(tableName))
			return new X_SRI_TaxRate(Env.getCtx(), Record_ID, trxName);
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		if (X_LEC_SRI_Format.Table_Name.equals(tableName))
			return new X_LEC_SRI_Format(Env.getCtx(), rs, trxName);
		if (X_SRI_AccessCode.Table_Name.equals(tableName))
			return new X_SRI_AccessCode(Env.getCtx(), rs, trxName);
		if (X_SRI_Authorization.Table_Name.equals(tableName))
			return new X_SRI_Authorization(Env.getCtx(), rs, trxName);
		if (X_SRI_ErrorCode.Table_Name.equals(tableName))
			return new X_SRI_ErrorCode(Env.getCtx(), rs, trxName);
		if (X_LLA_WithholdingSequence.Table_Name.equals(tableName))
			return new X_LLA_WithholdingSequence(Env.getCtx(), rs, trxName);
		if (X_SRI_TaxRate.Table_Name.equals(tableName))
			return new X_SRI_TaxRate(Env.getCtx(), rs, trxName);
		if (MSRITaxCode.Table_Name.equals(tableName))
			return new MSRITaxCode(Env.getCtx(), rs, trxName);		
		return null;
	}
}
