/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package ec.ingeint.erp.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for LLA_WithholdingSequence
 *  @author iDempiere (generated) 
 *  @version Release 2.1 - $Id$ */
public class X_LLA_WithholdingSequence extends PO implements I_LLA_WithholdingSequence, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20150213L;

    /** Standard Constructor */
    public X_LLA_WithholdingSequence (Properties ctx, int LLA_WithholdingSequence_ID, String trxName)
    {
      super (ctx, LLA_WithholdingSequence_ID, trxName);
      /** if (LLA_WithholdingSequence_ID == 0)
        {
			setAD_Sequence_ID (0);
			setIsGeneralWithholding (true);
// Y
			setIsSOTrx (false);
// N
			setLLA_WithholdingSequence_ID (0);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
			setValidTo (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Load Constructor */
    public X_LLA_WithholdingSequence (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_LLA_WithholdingSequence[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Sequence getAD_Sequence() throws RuntimeException
    {
		return (org.compiere.model.I_AD_Sequence)MTable.get(getCtx(), org.compiere.model.I_AD_Sequence.Table_Name)
			.getPO(getAD_Sequence_ID(), get_TrxName());	}

	/** Set Sequence.
		@param AD_Sequence_ID 
		Document Sequence
	  */
	public void setAD_Sequence_ID (int AD_Sequence_ID)
	{
		if (AD_Sequence_ID < 1) 
			set_Value (COLUMNNAME_AD_Sequence_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Sequence_ID, Integer.valueOf(AD_Sequence_ID));
	}

	/** Get Sequence.
		@return Document Sequence
	  */
	public int getAD_Sequence_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Sequence_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Is General Withholding.
		@param IsGeneralWithholding Is General Withholding	  */
	public void setIsGeneralWithholding (boolean IsGeneralWithholding)
	{
		set_Value (COLUMNNAME_IsGeneralWithholding, Boolean.valueOf(IsGeneralWithholding));
	}

	/** Get Is General Withholding.
		@return Is General Withholding	  */
	public boolean isGeneralWithholding () 
	{
		Object oo = get_Value(COLUMNNAME_IsGeneralWithholding);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Sales Transaction.
		@param IsSOTrx 
		This is a Sales Transaction
	  */
	public void setIsSOTrx (boolean IsSOTrx)
	{
		set_Value (COLUMNNAME_IsSOTrx, Boolean.valueOf(IsSOTrx));
	}

	/** Get Sales Transaction.
		@return This is a Sales Transaction
	  */
	public boolean isSOTrx () 
	{
		Object oo = get_Value(COLUMNNAME_IsSOTrx);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public org.globalqss.model.I_LCO_WithholdingType getLCO_WithholdingType() throws RuntimeException
    {
		return (org.globalqss.model.I_LCO_WithholdingType)MTable.get(getCtx(), org.globalqss.model.I_LCO_WithholdingType.Table_Name)
			.getPO(getLCO_WithholdingType_ID(), get_TrxName());	}

	/** Set Withholding Type.
		@param LCO_WithholdingType_ID Withholding Type	  */
	public void setLCO_WithholdingType_ID (int LCO_WithholdingType_ID)
	{
		if (LCO_WithholdingType_ID < 1) 
			set_Value (COLUMNNAME_LCO_WithholdingType_ID, null);
		else 
			set_Value (COLUMNNAME_LCO_WithholdingType_ID, Integer.valueOf(LCO_WithholdingType_ID));
	}

	/** Get Withholding Type.
		@return Withholding Type	  */
	public int getLCO_WithholdingType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LCO_WithholdingType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Withholding Sequence.
		@param LLA_WithholdingSequence_ID Withholding Sequence	  */
	public void setLLA_WithholdingSequence_ID (int LLA_WithholdingSequence_ID)
	{
		if (LLA_WithholdingSequence_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LLA_WithholdingSequence_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LLA_WithholdingSequence_ID, Integer.valueOf(LLA_WithholdingSequence_ID));
	}

	/** Get Withholding Sequence.
		@return Withholding Sequence	  */
	public int getLLA_WithholdingSequence_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LLA_WithholdingSequence_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set LLA_WithholdingSequence_UU.
		@param LLA_WithholdingSequence_UU LLA_WithholdingSequence_UU	  */
	public void setLLA_WithholdingSequence_UU (String LLA_WithholdingSequence_UU)
	{
		set_Value (COLUMNNAME_LLA_WithholdingSequence_UU, LLA_WithholdingSequence_UU);
	}

	/** Get LLA_WithholdingSequence_UU.
		@return LLA_WithholdingSequence_UU	  */
	public String getLLA_WithholdingSequence_UU () 
	{
		return (String)get_Value(COLUMNNAME_LLA_WithholdingSequence_UU);
	}

	/** Set Valid from.
		@param ValidFrom 
		Valid from including this date (first day)
	  */
	public void setValidFrom (Timestamp ValidFrom)
	{
		set_Value (COLUMNNAME_ValidFrom, ValidFrom);
	}

	/** Get Valid from.
		@return Valid from including this date (first day)
	  */
	public Timestamp getValidFrom () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidFrom);
	}

	/** Set Valid to.
		@param ValidTo 
		Valid to including this date (last day)
	  */
	public void setValidTo (Timestamp ValidTo)
	{
		set_Value (COLUMNNAME_ValidTo, ValidTo);
	}

	/** Get Valid to.
		@return Valid to including this date (last day)
	  */
	public Timestamp getValidTo () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidTo);
	}
}