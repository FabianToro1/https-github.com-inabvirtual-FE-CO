package ec.ingeint.erp.callout.order;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Msg;

public class LEC_QtyEntered implements IColumnCallout{
	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (value!=null){	
			
//			int C_DocType_ID = Integer.valueOf(value.toString());
//			MDocType doctype = MDocType.get(ctx, C_DocType_ID);
//			if (doctype.get_Value("SRI_ShortDocType")!=null)
//				if (doctype.get_Value("SRI_ShortDocType").toString().equals("06"))//Shipment

			String qtyEntered = value.toString();
			int pos = 0;
			String decimal = "";
			pos = qtyEntered.lastIndexOf ('.');    // Old
			
			if (pos>0){
				decimal = qtyEntered.substring(pos);
			}
			if (decimal.length()>3){
				mTab.setValue("QtyEntered", "0");
				throw new AdempiereException(Msg.translate(Env.getCtx(),"Maximum allowed two decimal digits"));
			}
		}
		return null;
	}
}
