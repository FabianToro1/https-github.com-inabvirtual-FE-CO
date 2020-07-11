package ec.ingeint.erp.callout.inout;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.util.CLogger;

public class LEC_C_DocType implements IColumnCallout {

	private static CLogger log = CLogger.getCLogger(LEC_C_DocType.class);
	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		
		if (value!=null){	
			int C_DocType_ID = Integer.valueOf(value.toString());
			MDocType doctype = MDocType.get(ctx, C_DocType_ID);
			if (doctype.get_Value("SRI_ShortDocType")!=null)
				if (doctype.get_Value("SRI_ShortDocType").toString().equals("06"))//Shipment 
						mTab.setValue(MInOut.COLUMNNAME_DeliveryViaRule, MInOut.DELIVERYVIARULE_Shipper);
		}
		return "";
	  }

}
