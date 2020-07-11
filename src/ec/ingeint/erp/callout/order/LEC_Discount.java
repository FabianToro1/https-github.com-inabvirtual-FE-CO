package ec.ingeint.erp.callout.order;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.CLogger;



public class LEC_Discount implements IColumnCallout {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(LEC_Discount.class);
	
	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) {

		if(mTab.get_ValueAsString("DiscountAmt") !=null && mTab.getRecord_ID()>0){
			log.info("Ejecutar el descuento MValidator");
			mTab.setValue("isDiscountApplied","Y");
		}

		return null;
		}
}
		


