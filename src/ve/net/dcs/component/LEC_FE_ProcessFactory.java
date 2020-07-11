package ve.net.dcs.component;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;
import org.globalqss.process.SRIContingencyGenerate;
import org.globalqss.process.SRIEmailAuthorization;
import org.globalqss.process.SRIGenerateWithholding;
import org.globalqss.process.SRIProcessBatchInOuts;
import org.globalqss.process.SRIProcessBatchMovements;
import org.globalqss.process.SRIProcessBatchSalesInvoices;
import org.globalqss.process.SRIProcessBatchSalesOrders;
import org.globalqss.process.SRIProcessBatchWithholdings;
import org.globalqss.process.SRIReprocessAuthorization;

import ec.ingeint.erp.process.LEC_InvoiceGenerate;
import ec.ingeint.erp.process.SRIGeneateOffLineAutForDocument;
import ec.ingeint.erp.process.SRIGenerateOfflineAuthorizations;
import ec.ingeint.erp.process.SRIProcessOfflineAuthorizations;

public class LEC_FE_ProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		ProcessCall process = null;
		if ("org.globalqss.process.SRIContingencyGenerate".equals(className)) {
			try {
				process =  SRIContingencyGenerate.class.newInstance();
			} catch (Exception e) {}
		}
		else if ("org.globalqss.process.SRIEmailAuthorization".equals(className)) {
			try {
				process =  SRIEmailAuthorization.class.newInstance();
			} catch (Exception e) {}
		}
		else if ("org.globalqss.process.SRIReprocessAuthorization".equals(className)) {
			try {
				process =  SRIReprocessAuthorization.class.newInstance();
			} catch (Exception e) {}
		}
		else if ("org.globalqss.process.SRIGenerateWithholding".equals(className)) {
			try {
				process =  SRIGenerateWithholding.class.newInstance();
			} catch (Exception e) {}
		}
		else if ("org.globalqss.process.SRIProcessBatchWithholdings".equals(className)) {
			try {
				process =  SRIProcessBatchWithholdings.class.newInstance();
			} catch (Exception e) {}
		}
		else if ("org.globalqss.process.SRIProcessBatchSalesOrders".equals(className)) {
			try {
				process =  SRIProcessBatchSalesOrders.class.newInstance();
			} catch (Exception e) {}
		}
		else if ("org.globalqss.process.SRIProcessBatchSalesInvoices".equals(className)) {
			try {
				process =  SRIProcessBatchSalesInvoices.class.newInstance();
			} catch (Exception e) {}
		}
		else if ("org.globalqss.process.SRIProcessBatchMovements".equals(className)) {
			try {
				process =  SRIProcessBatchMovements.class.newInstance();
			} catch (Exception e) {}
		}
		else if ("org.globalqss.process.SRIProcessBatchInOuts".equals(className)) {
			try {
				process =  SRIProcessBatchInOuts.class.newInstance();
			} catch (Exception e) {}
		}
		else if (LEC_InvoiceGenerate.class.getCanonicalName().equals(className)) {
			try {
				process =  LEC_InvoiceGenerate.class.newInstance();
			} catch (Exception e) {}
		}
		else if (SRIProcessOfflineAuthorizations.class.getCanonicalName().equals(className)) {
			try {
				process =  SRIProcessOfflineAuthorizations.class.newInstance();
			} catch (Exception e) {}
		}
		else if (SRIGenerateOfflineAuthorizations.class.getCanonicalName().equals(className)) {
			try {
				process =  SRIGenerateOfflineAuthorizations.class.newInstance();
			} catch (Exception e) {}
		}
		else if (SRIGeneateOffLineAutForDocument.class.getCanonicalName().equals(className)) {
			try {
				process = SRIGeneateOffLineAutForDocument.class.newInstance();
			} catch (Exception e) {}			
		}
		return process;
	}
}
