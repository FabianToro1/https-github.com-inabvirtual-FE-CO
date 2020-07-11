package org.globalqss.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MSRITaxCode extends X_SRI_TaxCode{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4562685470804360624L;

	public MSRITaxCode(Properties ctx, int SRI_TaxCode_ID, String trxName) {
		super(ctx, SRI_TaxCode_ID, trxName);
		// TODO Auto-generated constructor stub
	}
	
	public MSRITaxCode(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

}
