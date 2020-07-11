package org.globalqss.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.ProcessUtil;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MInvoicePaySchedule;
import org.compiere.model.MMovement;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.globalqss.util.LEC_FE_Utils;
import org.osgi.service.event.Event;

public class LEC_FE_ModelValidator extends AbstractEventHandler {

	/** Logger */
	private static CLogger log = CLogger
			.getCLogger(LEC_FE_ModelValidator.class);

	// 04/07/2016 MHG Offline Schema added
	private boolean isOfflineSchema = false;

	@Override
	protected void initialize() {
		log.warning("");
		// Documents to be monitored
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MMovement.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE,MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE,	MMovement.Table_Name);



		//New events capture for offline schema 
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MInvoice.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE,	MMovement.Table_Name);

		registerTableEvent(IEventTopics.PO_AFTER_NEW,MLCOInvoiceWithholding.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MMovement.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MMovement.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MInvoiceLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE,MInvoiceLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW,MInvoiceLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MOrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MOrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE,MOrderLine.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, X_SRI_Authorization.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_COMPLETE, MOrder.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MInOut.Table_Name);
	}

	@Override
	protected void doHandleEvent(Event event) {

		String type = event.getTopic();
		PO po = getPO(event);
		log.info(po.get_TableName() + " Type: "+type);
		String msg;

		// 04/07/2016 MHG Offline Schema added
		isOfflineSchema=MSysConfig.getBooleanValue("QSSLEC_FE_OfflineSchema", false, Env.getAD_Client_ID(Env.getCtx()));

		if (po.get_TableName().equals(MInOut.Table_Name) 
				&& type.equals(IEventTopics.PO_BEFORE_NEW)) {
			MInOut inout = (MInOut)po;
			WarehouseOrder(inout);
		}

		if (po.get_TableName().equals(MOrder.Table_Name)
				&& type.equals(IEventTopics.DOC_AFTER_COMPLETE)) {
			MOrder order = (MOrder)po;
			delpaymentprogram(order);
		}
		
		if (po.get_TableName().equals(MInOut.Table_Name)
				&& type.equals(IEventTopics.DOC_BEFORE_COMPLETE)) {
			MInOut inout = ((MInOut) po);
			msg = validateInOut(inout);
			if (msg != null)
				throw new RuntimeException(msg);
		}

		if (po.get_TableName().equals(MInvoice.Table_Name)
				&& type.equals(IEventTopics.DOC_AFTER_PREPARE)) {
			MInvoice invoice = ((MInvoice) po);
			msg = validateInvoice(invoice);
			if (msg != null)
				throw new RuntimeException(msg);
		}

		if (po.get_TableName().equals(MMovement.Table_Name)
				&& (type.equals(IEventTopics.DOC_BEFORE_COMPLETE))) {
			MMovement movement = (MMovement) po;
			msg = validateMovement(movement);
			if (msg != null)
				throw new RuntimeException(msg);
		}

		// before completing SO invoice set SO DocumentNo -- Previene custom
		// e-evolution Morder.completeIt
		if (po.get_TableName().equals(MInvoice.Table_Name)
				&& type.equals(IEventTopics.DOC_BEFORE_COMPLETE)) {

			MInvoice invoice = (MInvoice) po;
			if (MDocType.DOCSUBTYPESO_OnCreditOrder.equals(invoice.getC_Order()
					.getC_DocType().getDocSubTypeSO())) { // (W)illCall(I)nvoice
				invoice.setDocumentNo(invoice.getC_Order().getDocumentNo());
				invoice.saveEx();
			}
		}

		if(po.get_TableName().equals(MOrderLine.Table_Name)
				&& type.equals(IEventTopics.PO_AFTER_CHANGE)){
			MOrderLine oline = (MOrderLine) po;
			generateDiscount(oline);
		}

		// after completing SO invoice process electronic invoice
		if (po.get_TableName().equals(MInvoice.Table_Name)
				&& type.equals(IEventTopics.DOC_AFTER_COMPLETE)) {
			MInvoice invoice = (MInvoice) po;
			boolean IsGenerateInBatch = false;
			MDocType dt = new MDocType(invoice.getCtx(),
					invoice.getC_DocTypeTarget_ID(), invoice.get_TrxName());
			String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

			if (invoice.getC_Order_ID() > 0) {
				MDocType dto = new MDocType(invoice.getCtx(), invoice
						.getC_Order().getC_DocType_ID(), invoice.get_TrxName());

				if (dto.get_ValueAsBoolean("IsGenerateInBatch"))
					IsGenerateInBatch = true;
			}

			if (dt.get_ValueAsBoolean("IsGenerateInBatch"))
				IsGenerateInBatch = true;

			//
			if (!shortdoctype.equals("") && !IsGenerateInBatch && !isOfflineSchema) {

				msg = invoiceGenerateXml(invoice);

				if (msg != null)
					throw new RuntimeException(msg);
				/*Trx sriTrx = null;
				sriTrx = Trx.get(invoice.get_TrxName(), false);
				if (sriTrx != null) {
					sriTrx.commit();
				}
				invoice.load(sriTrx.getTrxName());
			 */
				if (invoice.get_Value("SRI_Authorization_ID") != null) {
					int authorization = Integer.valueOf(invoice.get_Value(
							"SRI_Authorization_ID").toString());
					if (!isOfflineSchema) 
						sendMail(authorization, null);
				}
			}
		}

		if (po.get_TableName().equals(MInvoice.Table_Name)
				&& type.equals(IEventTopics.DOC_AFTER_PREPARE)) {

			log.warning("-----------------DOC_AFTER_PREPARE");
			MInvoice invoice = (MInvoice) po;
			boolean IsGenerateInBatch = false;
			MDocType dt = new MDocType(invoice.getCtx(),
					invoice.getC_DocTypeTarget_ID(), invoice.get_TrxName());
			String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

			if (invoice.getC_Order_ID() > 0) {
				MDocType dto = new MDocType(invoice.getCtx(), invoice
						.getC_Order().getC_DocType_ID(), invoice.get_TrxName());

				if (dto.get_ValueAsBoolean("IsGenerateInBatch"))
					IsGenerateInBatch = true;
			}

			if (dt.get_ValueAsBoolean("IsGenerateInBatch"))
				IsGenerateInBatch = true;

			if(isOfflineSchema && shortdoctype!="")
				invoice.set_ValueOfColumn("isSRIOfflineSchema", "Y");
			else
				invoice.set_ValueOfColumn("isSRIOfflineSchema", "N");

			invoice.saveEx();

			//
			if (!shortdoctype.equals("") && !IsGenerateInBatch && isOfflineSchema) {

				if(invoice.get_Value("SRI_Authorization_ID") == null)
				{	
					//					msg = invoiceGenerateXml(invoice);
					//	
					//					if (msg != null)
					//						throw new RuntimeException(msg);
					//Trx sriTrx = null;
					//sriTrx = Trx.get(invoice.get_TrxName(), false);
					//if (sriTrx != null) {
					//	sriTrx.commit();
					//}
				}
			}
		}

		// after completing SO inout process electronic inout
		if (po.get_TableName().equals(MInOut.Table_Name)
				&& type.equals(IEventTopics.DOC_AFTER_COMPLETE)) {

			MInOut inout = (MInOut) po;
			boolean IsGenerateInBatch = false;
			MDocType dt = new MDocType(inout.getCtx(), inout.getC_DocType_ID(),
					inout.get_TrxName());
			String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

			if (inout.getC_Order_ID() > 0) {
				MDocType dto = new MDocType(inout.getCtx(), inout.getC_Order()
						.getC_DocType_ID(), inout.get_TrxName());

				if (dto.get_ValueAsBoolean("IsGenerateInBatch"))
					IsGenerateInBatch = true;
			}

			if (dt.get_ValueAsBoolean("IsGenerateInBatch"))
				IsGenerateInBatch = true;

			//
			if (!shortdoctype.equals("") && !IsGenerateInBatch && !isOfflineSchema) {

				msg = inoutGenerateXml(inout);

				if (msg != null) {
					log.warning(msg);
					throw new RuntimeException(msg);
				}
				/*Trx sriTrx = null;
				sriTrx = Trx.get(inout.get_TrxName(), false);
				if (sriTrx != null) {
					sriTrx.commit();
				}*/
				
				//inout.load(sriTrx.getTrxName());

				if (inout.get_Value("SRI_Authorization_ID") != null) {
					int authorization = Integer.valueOf(inout.get_Value(
							"SRI_Authorization_ID").toString());
					sendMail(authorization, null);
				}
			}
		}


		if (po.get_TableName().equals(MInOut.Table_Name)
				&& type.equals(IEventTopics.DOC_AFTER_PREPARE)) {
			log.warning("MInOut-----------------DOC_AFTER_PREPARE");

			if (isOfflineSchema) {

				MInOut inout = (MInOut) po;
				boolean IsGenerateInBatch = false;
				MDocType dt = new MDocType(inout.getCtx(), inout.getC_DocType_ID(),
						inout.get_TrxName());
				String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

				if (inout.getC_Order_ID() > 0) {
					MDocType dto = new MDocType(inout.getCtx(), inout.getC_Order()
							.getC_DocType_ID(), inout.get_TrxName());

					if (dto.get_ValueAsBoolean("IsGenerateInBatch"))
						IsGenerateInBatch = true;
				}

				if (dt.get_ValueAsBoolean("IsGenerateInBatch"))
					IsGenerateInBatch = true;
				if(isOfflineSchema && shortdoctype!="")
					inout.set_ValueOfColumn("isSRIOfflineSchema", "Y");
				else
					inout.set_ValueOfColumn("isSRIOfflineSchema", "N");

				inout.saveEx();

				//
				if (!shortdoctype.equals("") && !IsGenerateInBatch && isOfflineSchema) {

					//					msg = inoutGenerateXml(inout);
					//
					//					if (msg != null) {
					//						log.warning(msg);
					//						throw new RuntimeException(msg);
					//					}
				/*	Trx sriTrx = null;
					sriTrx = Trx.get(inout.get_TrxName(), false);
					if (sriTrx != null) {
						sriTrx.commit();
					}
					inout.load(sriTrx.getTrxName());
				*/
					if (inout.get_Value("SRI_Authorization_ID") != null) {
						int authorization = Integer.valueOf(inout.get_Value(
								"SRI_Authorization_ID").toString());
						sendMail(authorization, null);
					}
				}
			}
		}

		if (po.get_TableName().equals(MMovement.Table_Name)
				&& type.equals(IEventTopics.DOC_AFTER_COMPLETE)) {
			MMovement movement = (MMovement) po;

			MDocType dt = new MDocType(movement.getCtx(),
					movement.getC_DocType_ID(), movement.get_TrxName());
			String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");
			//
			if (!shortdoctype.equals("")
					&& !dt.get_ValueAsBoolean("IsGenerateInBatch") && !isOfflineSchema) {
				//
				msg = movementGenerateXml(movement);
				if (msg != null)
					throw new RuntimeException(msg);
			/*	Trx sriTrx = null;
				sriTrx = Trx.get(movement.get_TrxName(), false);
				if (sriTrx != null) {
					sriTrx.commit();
				}
				movement.load(sriTrx.getTrxName());
			*/
				if (movement.get_Value("SRI_Authorization_ID") != null) {
					int authorization = Integer.valueOf(movement.get_Value(
							"SRI_Authorization_ID").toString());
					sendMail(authorization, null);
				}
			}
		}

		if (po.get_TableName().equals(MMovement.Table_Name)
				&& type.equals(IEventTopics.DOC_AFTER_PREPARE)) {

			log.warning("-----------------DOC_AFTER_PREPARE");

			MMovement movement = (MMovement) po;

			MDocType dt = new MDocType(movement.getCtx(),
					movement.getC_DocType_ID(), movement.get_TrxName());
			String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

			if(isOfflineSchema && shortdoctype!="")
				movement.set_ValueOfColumn("isSRIOfflineSchema", "Y");
			else
				movement.set_ValueOfColumn("isSRIOfflineSchema", "N");

			movement.saveEx();

			//
			if (!shortdoctype.equals("")
					&& !dt.get_ValueAsBoolean("IsGenerateInBatch") && isOfflineSchema) {
				//
				//				msg = movementGenerateXml(movement);
				//				if (msg != null)
				//					throw new RuntimeException(msg);
				/* Trx sriTrx = null;
				sriTrx = Trx.get(movement.get_TrxName(), false);
				if (sriTrx != null) {
					sriTrx.commit();
				}
				movement.load(sriTrx.getTrxName());
				*/
				if (movement.get_Value("SRI_Authorization_ID") != null) {
					int authorization = Integer.valueOf(movement.get_Value(
							"SRI_Authorization_ID").toString());
					sendMail(authorization, null);
				}
			}
		}

		//Update Discount parameters  http://support.ingeint.com/issues/727
		if(po.get_TableName().equals(MInvoiceLine.Table_Name) && (type.equals(IEventTopics.PO_BEFORE_NEW))){

			MInvoiceLine invoiceline = (MInvoiceLine) po;
			MOrderLine oline = new MOrderLine(invoiceline.getCtx(),invoiceline.getM_InOutLine().getC_OrderLine_ID(),invoiceline.get_TrxName());
			if(oline.get_Value("DiscountAmt") !=null){
				invoiceline.set_ValueOfColumn("DiscountAmt", oline.get_Value("DiscountAmt"));
			}
		}

		if (po.get_TableName().equals(MInvoiceLine.Table_Name)
				&& (type.equals(IEventTopics.PO_BEFORE_NEW) || type
						.equals(IEventTopics.PO_BEFORE_CHANGE))) {
			MInvoiceLine invoiceLine = (MInvoiceLine) po;
			MInvoice invoice = (MInvoice) invoiceLine.getC_Invoice();
			MDocType dt = new MDocType(invoice.getCtx(),
					invoice.getC_DocTypeTarget_ID(), invoice.get_TrxName());
			String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");
			//
			if (!shortdoctype.equals("")) {
				validateDigitAllowed(invoiceLine.getQtyEntered().doubleValue(),
						6, MInvoiceLine.COLUMNNAME_QtyEntered);// Max 6 digit on
				// factura_v1.1.0.xsd
				// SRI
				if(invoice.isSOTrx())
					validateDigitAllowed(invoiceLine.getPriceEntered()
							.doubleValue(), 6, MInvoiceLine.COLUMNNAME_PriceEntered);// Max
				// 2
				// digit
				// on
				// factura_v1.1.0.xsd
				// SRI
				validateDigitAllowed(invoiceLine.getLineNetAmt().doubleValue(),
						2, MInvoiceLine.COLUMNNAME_LineNetAmt);// Max 2 digit on
				// factura_v1.1.0.xsd
				// SRI
			}

		}

		if (po.get_TableName().equals(MOrderLine.Table_Name)
				&& (type.equals(IEventTopics.PO_BEFORE_NEW) || type
						.equals(IEventTopics.PO_BEFORE_CHANGE))) {
			MOrderLine orderLine = (MOrderLine) po;
			MOrder order = (MOrder) orderLine.getC_Order();
			MDocType dt = new MDocType(order.getCtx(),
					order.getC_DocTypeTarget_ID(), order.get_TrxName());
			int dtinvId = 0;
			if (dt.get_Value("C_DocTypeInvoice_ID") != null) {
				if (Integer.valueOf(dt.get_Value("C_DocTypeInvoice_ID")
						.toString()) > 0) {
					dtinvId = Integer.valueOf(dt.get_Value(
							"C_DocTypeInvoice_ID").toString());
					MDocType dtinv = new MDocType(order.getCtx(), dtinvId,
							order.get_TrxName());
					String shortdoctype = dtinv
							.get_ValueAsString("SRI_ShortDocType");
					//
					if (!shortdoctype.equals("")) {
						validateDigitAllowed(orderLine.getQtyEntered()
								.doubleValue(), 6,
								MOrderLine.COLUMNNAME_QtyEntered); // Max 6
						// digit on
						// factura_v1.1.0.xsd
						validateDigitAllowed(orderLine.getPriceEntered()
								.doubleValue(), 6,
								MOrderLine.COLUMNNAME_PriceEntered);// Max 2
						// digit on
						// factura_v1.1.0.xsd
						if(order.isSOTrx())
							validateDigitAllowed(orderLine.getLineNetAmt()
									.doubleValue(), 2,
									MOrderLine.COLUMNNAME_LineNetAmt);// Max 2 digit
						// on
						// factura_v1.1.0.xsd
					}
				}
			}
		}

		if (po.get_TableName().equals(MLCOInvoiceWithholding.Table_Name)
				&& (type.equals(IEventTopics.PO_AFTER_NEW))) {
			MLCOInvoiceWithholding iwh = (MLCOInvoiceWithholding) po;
			MInvoice invoice = (MInvoice) iwh.getC_Invoice();
			if (!invoice.isSOTrx()) {
				boolean newWithholding = false;

				Object withholdingNo = invoice.get_Value("WithholdingNo");
				if (withholdingNo != null) {
					if (withholdingNo.toString().isEmpty()) {
						newWithholding = true;
					} else {
						newWithholding = false;
					}
				} else {
					newWithholding = true;
				}
				keepWithholdingDocumentNoAfterGenerated(iwh,invoice,
						newWithholding, invoice.get_TrxName());
			}

		}
	}
	private void WarehouseOrder(MInOut inout) { 

		if(inout.isSOTrx() && inout.getC_Order_ID()>0 && inout.getC_Order().getC_DocType().getDocSubTypeSO()!=null
				&& inout.getC_Order().getC_DocType().getDocSubTypeSO().equals("WP")) {

			MOrder order = new MOrder(inout.getCtx(), inout.getC_Order_ID(), inout.get_TrxName());
			inout.set_ValueOfColumn("ShipDateE", order.get_Value("ShipDateE"));
			inout.setShipDate((Timestamp) order.get_Value("ShipDate"));
			inout.set_ValueOfColumn("R_StandardResponse_ID", order.get_Value("R_StandardResponse_ID"));
			inout.setDeliveryViaRule("S");
			inout.setDeliveryRule("F");
			//inout.saveEx();
		}		
	}

	private void delpaymentprogram(MOrder order) {

		int result = DB.executeUpdate("DELETE FROM C_OrderPaySchedule WHERE C_Order_ID = ? ",
				order.get_ID(), true, order.get_TrxName());
		//log.warning("programa de pago borrado en la orden: "+order.getDocumentNo()+ result);
	}

	private String validateMovement(MMovement movement) {//BeforeComplete
		String msg = null;
		MDocType dt = new MDocType(movement.getCtx(), movement.getC_DocType_ID(), movement.get_TrxName());

		if (!(dt.get_Value("SRI_ShortDocType")==null)) {
			String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

			movement.set_ValueOfColumn("IsElectronicDocument", true);

			if (shortdoctype.equals("06")
					&& (movement.getDescription() == null || movement
					.getDescription().trim().length() == 0)) { // GUÍA DE REMISIÓN
				msg = "Descripcion obligatoria para el comprobante electronico";
			}
			if (dt.get_Value("SRI_ShortDocType") != null) {
				if (movement.getAD_User_ID() <= 0)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MMovement.COLUMNNAME_AD_User_ID);
				if (movement.getAD_User().getEMail()==null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MUser.COLUMNNAME_AD_User_ID)
							+ " - "
							+ Msg.getElement(Env.getCtx(), MUser.COLUMNNAME_EMail);
				MBPartner bp = MBPartner.get(Env.getCtx(),
						movement.getC_BPartner_ID());
				if (bp.get_Value(X_LCO_TaxIdType.COLUMNNAME_LCO_TaxIdType_ID) == null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MBPartner.COLUMNNAME_C_BPartner_ID)
							+ " - "
							+ Msg.getElement(Env.getCtx(),
									X_LCO_TaxIdType.COLUMNNAME_LCO_TaxIdType_ID);
				if(movement.getDeliveryViaRule()==null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MMovement.COLUMNNAME_DeliveryViaRule);

				if (!movement.getDeliveryViaRule().equals(MInOut.DELIVERYVIARULE_Shipper))
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MMovement.COLUMNNAME_DeliveryViaRule);
				if (movement.getM_Shipper() == null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MMovement.COLUMNNAME_M_Shipper_ID);
				if (movement.get_Value("ShipDate") == null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(), "ShipDate");
				if (movement.get_Value("ShipDateE") == null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(), "ShipDateE");
				if (movement.getC_BPartner_Location() == null) {
					if (movement.getC_BPartner_Location_ID() <= 0)
						throw new AdempiereException(
								Msg.translate(Env.getCtx(), "FillMandatory")
								+ " "
								+ Msg.getElement(
										Env.getCtx(),
										MMovement.COLUMNNAME_C_BPartner_Location_ID));
				}
			}
		}
		return msg;
	}//Validate Movement - before complete

	//  www.ingeint.com - http://support.ingeint.com/issues/727
	//AfterChange

	private void generateDiscount(MOrderLine oline) {


		if(oline.get_ValueAsBoolean("IsDiscountApplied")){
			BigDecimal calcdesc = DB.getSQLValueBD(oline.get_TrxName(),"SELECT sum(DiscountAmt) "
					+ "FROM C_OrderLine WHERE C_Order_ID = ? ",oline.getC_Order_ID());

			MOrder order = (MOrder) oline.getC_Order();
			int res = calcdesc.compareTo(order.getTotalLines());

			if(res==1){
				throw new AdempiereException("El Descuento no puede ser mayor al total de las lineas de la orden");
			}
			BigDecimal discount = (BigDecimal) oline.get_Value("DiscountAmt");
			if(discount !=null){
				Integer LineOrder = DB.getSQLValue(oline.get_TrxName(), "SELECT C_OrderLine_ID FROM C_OrderLine WHERE C_Order_ID = ? "
						+ "AND C_Charge_ID IN "
						+ " (Select C_Charge_ID FROM C_Charge WHERE isDiscountFE='Y') ", oline.getC_Order_ID());

				if(LineOrder >0){

					MOrderLine chargeline = new MOrderLine(oline.getCtx(),LineOrder,oline.get_TrxName());
					if(discount.compareTo(BigDecimal.ZERO) == 0){
						chargeline.delete(true);
					}else{
						chargeline.setPriceEntered(calcdesc.negate());
						chargeline.setPriceActual(calcdesc.negate());
						chargeline.setLineNetAmt(calcdesc.negate());
						chargeline.saveEx();
						log.info("-----------------Aqui Actualizo el Cargo");
					}
				}
				else
				{
					MOrderLine chargeline = new MOrderLine(order);

					Integer C_ChargeID = DB.getSQLValue(null, " SELECT C_Charge_ID FROM C_Charge WHERE IsDiscountFE='Y' ");
					if (C_ChargeID <=0)
						throw new AdempiereException("Debe configurar un cargo para descuentos de Facturación Electrónica");

					chargeline.setC_Charge_ID(C_ChargeID);
					chargeline.setQtyOrdered(Env.ONE);
					chargeline.setQtyEntered(BigDecimal.ONE);
					chargeline.setPriceEntered(calcdesc.negate());
					chargeline.setPriceActual(calcdesc.negate());
					chargeline.setLineNetAmt(calcdesc.negate());
					chargeline.saveEx();
					log.info("-----------------Aqui Creo el Nuevo Cargo");
				}
			}

			oline.set_ValueOfColumn("IsDiscountApplied", "N");
			log.info("----Se Ejecutó el descuento");
			oline.saveEx();
		}
	}
	
	public static String invoiceGenerateXml(MInvoice inv) {
		int autorization_id = 0;
		if (inv.get_Value("SRI_Authorization_ID") != null) {
			autorization_id = inv.get_ValueAsInt("SRI_Authorization_ID");
		}
		
		if (autorization_id != 0) {
			X_SRI_Authorization a = new X_SRI_Authorization(inv.getCtx(),
					autorization_id, inv.get_TrxName());
			if (a != null) {
				if (a.getSRI_AuthorizationDate() != null) {
					// Comprobante autorizado, no se envia de nuevo el xml.
					return null;
				}
			}
		}
		String msg = null;

		MDocType dt = new MDocType(inv.getCtx(), inv.getC_DocTypeTarget_ID(),
				inv.get_TrxName());

		String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

		if (shortdoctype.equals("")) {
			msg = "No existe definicion SRI_ShortDocType: " + dt.toString();
			log.info("Invoice: " + inv.toString() + msg);

			// if (LEC_FE_Utils.breakDialog(msg)) return "Cancelado..."; // Temp

		}

		MUser user = new MUser(inv.getCtx(), inv.getAD_User_ID(),
				inv.get_TrxName());

		if (!valideUserMail(user) && !shortdoctype.equals("")) {
			msg = "@RequestActionEMailNoTo@";
			return msg;
		}

		msg = null;
		LEC_FE_MInvoice lecfeinv = new LEC_FE_MInvoice(inv.getCtx(),
				inv.getC_Invoice_ID(), inv.get_TrxName());
		LEC_FE_MNotaCredito lecfeinvnc = new LEC_FE_MNotaCredito(inv.getCtx(),
				inv.getC_Invoice_ID(), inv.get_TrxName());
		LEC_FE_MNotaDebito lecfeinvnd = new LEC_FE_MNotaDebito(inv.getCtx(),
				inv.getC_Invoice_ID(), inv.get_TrxName());
		LEC_FE_MRetencion lecfeinvret = new LEC_FE_MRetencion(inv.getCtx(),
				inv.getC_Invoice_ID(), inv.get_TrxName());
		// isSOTrx()
		if (inv.isSOTrx())
			LEC_FE_MRetencion.generateWitholdingNo(inv);

		if (shortdoctype.equals("01")) { // FACTURA
			msg = lecfeinv.lecfeinv_SriExportInvoiceXML100();
		} else if (shortdoctype.equals("04")) { // NOTA DE CRÉDITO
			msg = lecfeinvnc.lecfeinvnc_SriExportNotaCreditoXML100();
		} else if (shortdoctype.equals("05")) { // NOTA DE DÉBITO
			msg = lecfeinvnd.lecfeinvnd_SriExportNotaDebitoXML100();
			// !isSOTrx()
		} else if (shortdoctype.equals("07")) { // COMPROBANTE DE RETENCIÓN
			if (lecfeinvret.get_ValueAsInt("SRI_Authorization_ID") < 1 && MSysConfig.getBooleanValue("LEC_GenerateWitholdingToComplete", false,lecfeinvret.getAD_Client_ID())) {
				LEC_FE_MRetencion.generateWitholdingNo(inv);
				//Trx tra = Trx.get(inv.get_TrxName(), false);
				//tra.commit();
				msg = lecfeinvret.lecfeinvret_SriExportRetencionXML100();
			}
		} else
			log.warning("Formato no habilitado SRI: " + dt.toString()
			+ shortdoctype);

		return msg;
	}

	private String inoutGenerateXml(MInOut inout) {
		int autorization_id = 0;
		if (inout.get_Value("SRI_Authorization_ID") != null) {
			autorization_id = inout.get_ValueAsInt("SRI_Authorization_ID");
		}
		X_SRI_Authorization a = new X_SRI_Authorization(inout.getCtx(),
				autorization_id, inout.get_TrxName());
		if (a != null) {
			if (a.getSRI_AuthorizationDate() != null) {
				// Comprobante autorizado, no se envia de nuevo el xml.
				return null;
			}
		}
		String msg = null;

		MDocType dt = new MDocType(inout.getCtx(), inout.getC_DocType_ID(),
				inout.get_TrxName());

		String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

		if (shortdoctype.equals("")) {
			msg = "No existe definicion SRI_ShortDocType: " + dt.toString();
			log.info("Invoice: " + inout.toString() + msg);

			// if (LEC_FE_Utils.breakDialog(msg)) return "Cancelado..."; // Temp
		}

		MUser user = new MUser(inout.getCtx(), inout.getAD_User_ID(),
				inout.get_TrxName());

		if (!valideUserMail(user) && !shortdoctype.equals("")) {
			msg = "@RequestActionEMailNoTo@";
			return msg;
		}

		msg = null;
		LEC_FE_MInOut lecfeinout = new LEC_FE_MInOut(inout.getCtx(),
				inout.getM_InOut_ID(), inout.get_TrxName());
		// isSOTrx()
		if (shortdoctype.equals("06")) // GUÍA DE REMISIÓN
			msg = lecfeinout.lecfeinout_SriExportInOutXML100();
		else
			log.warning("Formato no habilitado SRI: " + dt.toString()
			+ shortdoctype);

		return msg;
	}

	private String movementGenerateXml(MMovement movement) {
		int autorization_id = 0;
		if (movement.get_Value("SRI_Authorization_ID") != null) {
			autorization_id = movement.get_ValueAsInt("SRI_Authorization_ID");
		}
		X_SRI_Authorization a = new X_SRI_Authorization(movement.getCtx(),
				autorization_id, movement.get_TrxName());
		if (a != null) {
			if (a.getSRI_AuthorizationDate() != null) {
				// Comprobante autorizado, no se envia de nuevo el xml.
				return null;
			}
		}
		String msg = null;

		MDocType dt = new MDocType(movement.getCtx(),
				movement.getC_DocType_ID(), movement.get_TrxName());

		String shortdoctype = dt.get_ValueAsString("SRI_ShortDocType");

		if (shortdoctype.equals("")) {
			msg = "No existe definicion SRI_ShortDocType: " + dt.toString();
			log.info("Invoice: " + movement.toString() + msg);

			// if (LEC_FE_Utils.breakDialog(msg)) return "Cancelado..."; // Temp
		}

		MUser user = new MUser(movement.getCtx(), movement.getAD_User_ID(),
				movement.get_TrxName());

		if (!valideUserMail(user) && !shortdoctype.equals("")) {
			msg = "@RequestActionEMailNoTo@";
			return msg;
		}

		msg = null;
		LEC_FE_Movement lecfemovement = new LEC_FE_Movement(movement.getCtx(),
				movement.getM_Movement_ID(), movement.get_TrxName());
		// Hardcoded 1000418-SIS UIO COMPANIA RELACIONADA
		// if (shortdoctype.equals("06") && dt.getC_DocType_ID() == 1000418) //
		// GUÍA DE REMISIÓN
		if (shortdoctype.equals("06"))
			msg = lecfemovement.lecfeMovement_SriExportMovementXML100();
		else
			log.warning("Formato no habilitado SRI: " + dt.toString()
			+ shortdoctype);

		return msg;
	}

	public static boolean valideUserMail(MUser user) {
		if (MSysConfig.getBooleanValue("QSSLEC_FE_EnvioXmlAutorizadoBPEmail",
				false, user.getAD_Client_ID())) {

			if ((user.get_ID() == 0 || user.isNotificationEMail()
					&& (user.getEMail() == null || user.getEMail().length() == 0))) {
				return false;
			}
		}

		return true;

	} // valideUserMail

	public static void sendMail(int p_authorization, Trx trx) {
		MProcess process = new Query(Env.getCtx(), MProcess.Table_Name,
				"classname = ?", null).setParameters(
						"org.globalqss.process.SRIEmailAuthorization").first();
		if (process != null) {

			ProcessInfo processInfo = new ProcessInfo(process.getName(),
					process.get_ID());
			MPInstance instance = new MPInstance(Env.getCtx(),
					processInfo.getAD_Process_ID(), processInfo.getRecord_ID());
			instance.save();

			ProcessInfoParameter[] para = { new ProcessInfoParameter(
					"SRI_Authorization_ID", p_authorization, null, null, null) };
			processInfo.setAD_Process_ID(process.get_ID());
			processInfo.setClassName(process.getClassname());
			processInfo.setAD_PInstance_ID(instance.getAD_PInstance_ID());
			processInfo.setParameter(para);

			ProcessUtil.startJavaProcess(Env.getCtx(), processInfo, trx, true);
		}
	}

	/**
	 * valideOrgInfoSri
	 *
	 * @param MOrgInfo
	 *            orginfo
	 * @return error message or null
	 */
	public static String valideOrgInfoSri(MOrgInfo orginfo) {

		String msg = null;

		int c_location_matriz_id = MSysConfig.getIntValue(
				"QSSLEC_FE_LocalizacionDireccionMatriz", -1,
				orginfo.getAD_Client_ID());

		if (c_location_matriz_id < 1)
			msg = "No existe parametro para LocalizacionDireccionMatriz";

		String clavecert = MSysConfig.getValue(
				"QSSLEC_FE_ClaveCertificadoDigital", null,
				orginfo.getAD_Client_ID(), orginfo.getAD_Org_ID());

		if (clavecert == null)
			msg = "No existe parametro para ClaveCertificadoDigital";

		String rutacert = MSysConfig.getValue(
				"QSSLEC_FE_RutaCertificadoDigital", null,
				orginfo.getAD_Client_ID(), orginfo.getAD_Org_ID());
		// msg = "No existe parametro para RutaCertificadoDigital";

		if (rutacert != null) {
			File folder = new File(rutacert);

			if (!folder.exists() || !folder.isFile() || !folder.canRead())
				msg = "No existe o no se puede leer el archivo de Certificado Digital";

		} else {
			// Obtencion del certificado para firmar. Utilizando un attachment -
			// AD_Org
			boolean isattachcert = false;
			MAttachment attach = MAttachment.get(orginfo.getCtx(),
					MTable.getTable_ID("AD_Org"), orginfo.getAD_Org_ID());
			if (attach != null) {
				for (MAttachmentEntry entry : attach.getEntries()) {
					if (entry.getName().endsWith("p12")
							|| entry.getName().endsWith("pfx"))
						isattachcert = true;
				}
			}
			if (!isattachcert)
				msg = "No existe parametro o adjunto de Certificado Digital";
		}

		int c_bpartner_id = LEC_FE_Utils.getOrgBPartner(
				orginfo.getAD_Client_ID(), orginfo.get_ValueAsString("TaxID"));

		if (c_bpartner_id < 1)
			msg = "No existe BP relacionado a OrgInfo.Documento: "
					+ orginfo.get_ValueAsString("TaxID");
		else if (orginfo.get_ValueAsString("TaxID").equals(""))
			msg = "No existe definicion OrgInfo.Documento: "
					+ orginfo.toString();
		else if (orginfo.get_ValueAsString("SRI_DocumentCode").equals(""))
			msg = "No existe definicion OrgInfo.DocumentCode: "
					+ orginfo.toString();
		else if (orginfo.get_ValueAsString("SRI_OrgCode").equals(""))
			msg = "No existe definicion OrgInfo.SRI_OrgCode: "
					+ orginfo.toString();
		// else if (orginfo.get_ValueAsString("SRI_StoreCode").equals(""))
		// msg = "No existe definicion OrgInfo.SRI_StoreCode: " +
		// orginfo.toString();
		else if (orginfo.get_ValueAsString("SRI_DocumentCode").equals(""))
			msg = "No existe definicion OrgInfo.SRI_DocumentCode: "
					+ orginfo.toString();
		else if (orginfo.get_ValueAsString("SRI_IsKeepAccounting").equals(""))
			msg = "No existe definicion OrgInfo.SRI_IsKeepAccounting: "
					+ orginfo.toString();
		else if (orginfo.getC_Location_ID() == 0)
			msg = "No existe definicion OrgInfo.Address1: "
					+ orginfo.toString();
		else {
			MBPartner bpe = new MBPartner(orginfo.getCtx(), c_bpartner_id,
					orginfo.get_TrxName());
			if ((Integer) bpe.get_Value("LCO_TaxPayerType_ID") == 1000027) // Hardcoded
				if (orginfo.get_ValueAsString("SRI_TaxPayerCode").equals(""))
					msg = "No existe definicion OrgInfo.SRI_TaxPayerCode: "
							+ orginfo.toString();
			;
		}

		return msg;

	} // valideOrgInfoSri

	/**
	 * valideInvoice
	 *
	 * @param MInvoice
	 *
	 * @return error message or null
	 */

	private String validateInvoice(MInvoice invoice) {
		
		String msg = null;

		if (invoice.getReversal_ID()==0) {

			MDocType dt = new MDocType(invoice.getCtx(),invoice.getC_DocTypeTarget_ID(), invoice.get_TrxName());
			if (dt.get_Value("SRI_ShortDocType") != null ) { 

				if (invoice.getAD_User_ID() <= 0)
					throw new AdempiereException(Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MInvoice.COLUMNNAME_AD_User_ID));
				MBPartner bp = MBPartner.get(Env.getCtx(),
						invoice.getC_BPartner_ID());
				if (invoice.getAD_User().getEMail()==null) {
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MUser.COLUMNNAME_AD_User_ID)
							+ " - "
							+ Msg.getElement(Env.getCtx(), MUser.COLUMNNAME_EMail);
				}
				if (bp.get_Value(X_LCO_TaxIdType.COLUMNNAME_LCO_TaxIdType_ID) == null) {
					 msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MBPartner.COLUMNNAME_C_BPartner_ID)
							+ " - "
							+ Msg.getElement(Env.getCtx(),
									X_LCO_TaxIdType.COLUMNNAME_LCO_TaxIdType_ID);
				}
				if (bp.get_Value(X_LCO_TaxPayerType.COLUMNNAME_LCO_TaxPayerType_ID) == null) {
					msg =   Msg.translate(Env.getCtx(), "FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MBPartner.COLUMNNAME_C_BPartner_ID)
							+ " - "
							+ Msg.getElement(
									Env.getCtx(),
									X_LCO_TaxPayerType.COLUMNNAME_LCO_TaxPayerType_ID);
				}
				if (dt.get_ValueAsString("SRI_ShortDocType").equals("05")
						|| dt.get_ValueAsString("SRI_ShortDocType").equals("04")) {
					if (invoice.get_Value("SRI_RefInvoice_ID") == null && invoice.get_Value("InvoiceDocumentReference") == null)
						msg = Msg.translate(Env.getCtx(),
								"FillMandatory")
								+ " "
								+ Msg.getElement(Env.getCtx(), "SRI_RefInvoice_ID");
					if (invoice.getDescription() == null)
						msg = Msg.translate(Env.getCtx(),
								"FillMandatory")
								+ " "
								+ Msg.getElement(Env.getCtx(),
										MInvoice.COLUMNNAME_Description);
				}

				if(dt.get_ValueAsString("SRI_ShortDocType")!=null && !invoice.isSOTrx()
						&& (invoice.getDocumentNo().length()<17))
					msg = "La factura debe contener 17 dígitos (Incluidos los guiones) en el N° de Documento";

			}
			if (!invoice.getPaymentRule().equals(MInvoice.PAYMENTRULE_OnCredit)){
				MInvoicePaySchedule[] ips = MInvoicePaySchedule.getInvoicePaySchedule(invoice.getCtx(),
						invoice.get_ID(), 0, invoice.get_TrxName());

				int rows = ips.length;

				for (int i = 0; i < rows; i++){
					ips[i].deleteEx(true);
				}
				invoice.setIsPayScheduleValid(false);
			}
		}
		return msg;
		
	}

	/**
	 * valideMInOut
	 *
	 * @param MInOut
	 *
	 * @return error message or null
	 */

	private String validateInOut(MInOut inout) {
		
		String msg = null;
		
		if(inout.getReversal_ID()==0) {

			MDocType doctype = new MDocType(inout.getCtx(), inout.getC_DocType_ID(), inout.get_TrxName());

			if (!(doctype.get_Value("SRI_ShortDocType")==null)) {

				if (inout.getAD_User_ID() <= 0)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MInvoice.COLUMNNAME_AD_User_ID);
				if (inout.getAD_User().getEMail()==null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MUser.COLUMNNAME_AD_User_ID)
							+ " - "
							+ Msg.getElement(Env.getCtx(), MUser.COLUMNNAME_EMail);
				MBPartner bp = MBPartner
						.get(Env.getCtx(), inout.getC_BPartner_ID());
				if (bp.get_Value(X_LCO_TaxIdType.COLUMNNAME_LCO_TaxIdType_ID) == null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MBPartner.COLUMNNAME_C_BPartner_ID)
							+ " - "
							+ Msg.getElement(Env.getCtx(),
									X_LCO_TaxIdType.COLUMNNAME_LCO_TaxIdType_ID);
				if (!inout.getDeliveryViaRule().equals(
						MInOut.DELIVERYVIARULE_Shipper))
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MInOut.COLUMNNAME_DeliveryViaRule);
				if (inout.getM_Shipper() == null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MInOut.COLUMNNAME_M_Shipper_ID);
				if (inout.getShipDate() == null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(),
									MInOut.COLUMNNAME_ShipDate);
				if (inout.get_Value("ShipDateE") == null)
					msg = Msg.translate(Env.getCtx(),
							"FillMandatory")
							+ " "
							+ Msg.getElement(Env.getCtx(), "ShipDateE");
				if (inout.getC_BPartner_Location() == null) {
					if (inout.getC_BPartner_Location_ID() <= 0)
						msg = Msg.translate(Env.getCtx(),
								"FillMandatory")
								+ " "
								+ Msg.getElement(Env.getCtx(),
										MInOut.COLUMNNAME_C_BPartner_Location_ID);
				}//End validations				 
			}
		}
		return msg;
	}

	/**
	 * valide DigitAllowed
	 *
	 * @param qtyEntered
	 *
	 * @return void or throw Maximum allowed two decimal digits
	 */
	private void validateDigitAllowed(double amt, int digitAllowed,
			String ColumnName) {
		String amtStr = String.valueOf(amt);
		int pos = 0;
		String decimal = "";
		pos = amtStr.lastIndexOf('.');

		if (pos > 0) {
			decimal = amtStr.substring(pos);
		}
		if ((decimal.length() - 1) > digitAllowed) { // .00 = 3 Digits
			throw new AdempiereException(Msg.translate(Env.getCtx(),
					"Maximo permitido "+digitAllowed+" digitos decimales")
					+ " - "
					+ Msg.getElement(Env.getCtx(), ColumnName));
		}
	}

	/**
	 * keepWithholdingDocumentNoAfterGenerated
	 *
	 * @param MLCOInvoiceWithholding iwh,MInvoice invoice,
				boolean newWithholding, String trxName
	 *
	 * @return String withhodingNo
	 */
	private String keepWithholdingDocumentNoAfterGenerated(MLCOInvoiceWithholding iwh,MInvoice invoice,
			boolean newWithholding, String trxName) {
		String withhodingNo = "";
		boolean keep = MSysConfig.getBooleanValue(
				"QSSLEC_FE_KEEP_WITHHOLDING_DOCNO_AFTER_GENERATED", false,
				Env.getAD_Client_ID(Env.getCtx()));
		if (!keep)
			return null;
		log.warning(
				"QSSLEC_FE_KEEP_WITHHOLDING_DOCNO_AFTER_GENERATED = Y");
		if (newWithholding) {
			withhodingNo = LEC_FE_MRetencion.generateWitholdingNo(iwh);
			invoice.set_ValueOfColumn("WithholdingNo", withhodingNo);
			invoice.saveEx();
		} else {
			withhodingNo = invoice.get_ValueAsString("WithholdingNo");
		}
		iwh.setDocumentNo(withhodingNo);
		iwh.saveEx();
		return withhodingNo;
	}
}
