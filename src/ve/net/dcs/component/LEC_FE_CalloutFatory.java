package ve.net.dcs.component;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.IColumnCalloutFactory;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MMovement;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;


public class LEC_FE_CalloutFatory implements IColumnCalloutFactory {

	@Override
	public IColumnCallout[] getColumnCallouts(String tableName,
			String columnName) {
		if (tableName.equalsIgnoreCase(MInOut.Table_Name)) {
			if (columnName.equalsIgnoreCase(MInOut.COLUMNNAME_C_DocType_ID))
				return new IColumnCallout[]{new ec.ingeint.erp.callout.inout.LEC_C_DocType()};
		}
		
		if (tableName.equalsIgnoreCase(MOrder.Table_Name)) {
			if (columnName.equalsIgnoreCase(MOrder.COLUMNNAME_C_DocTypeTarget_ID))
				return new IColumnCallout[]{new ec.ingeint.erp.callout.order.LEC_C_DocType()};
		}
		
		if (tableName.equalsIgnoreCase(MOrderLine.Table_Name)) {
			if (columnName.equalsIgnoreCase("DiscountAmt"))
				return new IColumnCallout[]{new ec.ingeint.erp.callout.order.LEC_Discount()};
		}
		
		if (tableName.equalsIgnoreCase(MInvoice.Table_Name)) {
			if (columnName.equalsIgnoreCase(MInvoice.COLUMNNAME_PaymentRule))
				return new IColumnCallout[] {new ec.ingeint.erp.callout.invoice.CalloutInvoice()};
		}
		
		if (tableName.equals(MMovement.Table_Name)) {
			if (columnName.equalsIgnoreCase("ShipDateE"))
				return new IColumnCallout[] {new ec.ingeint.erp.callout.movement.CalloutMovementDateE()};
		}
		
		if (tableName.equals(MInOut.Table_Name)) {
			if (columnName.equals("ShipDateE"))
				return new IColumnCallout[] {new ec.ingeint.erp.callout.movement.CalloutMovementDateE()};
		}
		
		if (tableName.equals(MMovement.Table_Name)) {
			if (columnName.equalsIgnoreCase("ShipDate"))
				return new IColumnCallout[] {new ec.ingeint.erp.callout.movement.CalloutMovementDateE()};
		}
		
		if (tableName.equals(MInOut.Table_Name)) {
			if (columnName.equals("ShipDate"))
				return new IColumnCallout[] {new ec.ingeint.erp.callout.movement.CalloutMovementDateE()};
		}	

		return null;
	}

}
