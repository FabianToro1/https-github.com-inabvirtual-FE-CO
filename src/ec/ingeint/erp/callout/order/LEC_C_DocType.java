package ec.ingeint.erp.callout.order;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MRefList;
import org.compiere.model.MRefTable;
import org.compiere.model.Query;

public class LEC_C_DocType implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {
		if (value!=null){	
			int C_DocType_ID = Integer.valueOf(value.toString());
			MDocType doctype = MDocType.get(ctx, C_DocType_ID);
			if (doctype.get_Value("IsInternal")!=null)
				//Tipo Interno para exportación
				mTab.setValue("IsInternal", Boolean.valueOf(doctype.get_Value("IsInternal").toString()));
		}
		return null;
	}

}
