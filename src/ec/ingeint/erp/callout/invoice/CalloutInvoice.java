package ec.ingeint.erp.callout.invoice;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoicePaySchedule;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

public class CalloutInvoice  implements IColumnCallout  {

	private static CLogger log = CLogger
			.getCLogger(CalloutInvoice.class);

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		
		
		if (value!=null){	

			String PaymentRule = value.toString();
			
			if (mTab.getValue("C_Invoice_ID")==null)
				return null;
			
			Integer C_Invoice_ID = Integer.valueOf(mTab.getValue("C_Invoice_ID").toString());

			if (!PaymentRule.equalsIgnoreCase("P") && !mTab.get_ValueAsString(MInvoice.COLUMNNAME_DocStatus).equalsIgnoreCase(MInvoice.DOCSTATUS_Completed)) {

				MInvoicePaySchedule[] ips = MInvoicePaySchedule.getInvoicePaySchedule(ctx,C_Invoice_ID, 0, null);

				int rows = ips.length;

				for (int i = 0; i < rows; i++){
				
					int delete = DB.executeUpdate(
							"DELETE FROM C_InvoicePaySchedule"
									+ " WHERE C_InvoicePaySchedule_ID = ?", ips[i].get_ID(),ips[i].get_TrxName());								
							
					mTab.setValue("setIsPayScheduleValid","false");
					log.warning("Registros borrados: "+delete);
				}
				
			}
		}
		return null;
	}
}
