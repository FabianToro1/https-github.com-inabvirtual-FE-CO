package ec.ingeint.erp.callout.movement;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;


import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;

public class CalloutMovementDateE implements IColumnCallout{

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		if (value!=null){
			
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			String sdatee = mTab.get_ValueAsString("ShipDateE");
			
			if(sdatee=="")
				return null;
			sdatee = sdatee.substring(0,10);
			
			String movementdate = mTab.get_ValueAsString("MovementDate");
			if(movementdate=="")
				return null;
			movementdate = movementdate.substring(0, 10);
						
			String sdates = mTab.get_ValueAsString("ShipDate");
			if(sdates=="")
				return null;
			sdates = sdates.substring(0,10);
		
			try {
				Date datee = format.parse(sdatee);
				Date dates = format.parse(sdates);
				Date moved = format.parse(movementdate);

				if (datee.compareTo(dates)==-1) {
					mTab.setValue("ShipDateE", null);
					mTab.fireDataStatusEEvent("La fecha fin de entrega no puede ser menor a la fecha del inicio", sdates, true);
				}
				
				if (dates.compareTo(moved)==-1) {
					mTab.setValue("ShipDate", null);
					mTab.fireDataStatusEEvent("La fecha inicio de entrega no puede ser menor a la fecha del movimiento", movementdate, true);
				}				
					
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}

