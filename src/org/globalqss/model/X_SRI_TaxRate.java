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
package org.globalqss.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for SRI_TaxRate
 *  @author iDempiere (generated) 
 *  @version Release 5.1 - $Id$ */
public class X_SRI_TaxRate extends PO implements I_SRI_TaxRate, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20180604L;

    /** Standard Constructor */
    public X_SRI_TaxRate (Properties ctx, int SRI_TaxRate_ID, String trxName)
    {
      super (ctx, SRI_TaxRate_ID, trxName);
      /** if (SRI_TaxRate_ID == 0)
        {
			setSRI_TaxCode_ID (0);
// @SRI_TaxCode_ID@
			setSRI_TaxRate_ID (0);
			setSRI_TaxRateName (null);
			setSRI_TaxRateValue (null);
        } */
    }

    /** Load Constructor */
    public X_SRI_TaxRate (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SRI_TaxRate[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	public org.globalqss.model.I_SRI_TaxCode getSRI_TaxCode() throws RuntimeException
    {
		return (org.globalqss.model.I_SRI_TaxCode)MTable.get(getCtx(), org.globalqss.model.I_SRI_TaxCode.Table_Name)
			.getPO(getSRI_TaxCode_ID(), get_TrxName());	}

	/** Set SRI Tax Code.
		@param SRI_TaxCode_ID SRI Tax Code	  */
	public void setSRI_TaxCode_ID (int SRI_TaxCode_ID)
	{
		if (SRI_TaxCode_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SRI_TaxCode_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SRI_TaxCode_ID, Integer.valueOf(SRI_TaxCode_ID));
	}

	/** Get SRI Tax Code.
		@return SRI Tax Code	  */
	public int getSRI_TaxCode_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SRI_TaxCode_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SRI_TaxRate.
		@param SRI_TaxRate_ID SRI_TaxRate	  */
	public void setSRI_TaxRate_ID (int SRI_TaxRate_ID)
	{
		if (SRI_TaxRate_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SRI_TaxRate_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SRI_TaxRate_ID, Integer.valueOf(SRI_TaxRate_ID));
	}

	/** Get SRI_TaxRate.
		@return SRI_TaxRate	  */
	public int getSRI_TaxRate_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SRI_TaxRate_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SRI Tax Rate Name.
		@param SRI_TaxRateName SRI Tax Rate Name	  */
	public void setSRI_TaxRateName (String SRI_TaxRateName)
	{
		set_Value (COLUMNNAME_SRI_TaxRateName, SRI_TaxRateName);
	}

	/** Get SRI Tax Rate Name.
		@return SRI Tax Rate Name	  */
	public String getSRI_TaxRateName () 
	{
		return (String)get_Value(COLUMNNAME_SRI_TaxRateName);
	}

	/** Set SRI_TaxRate_UU.
		@param SRI_TaxRate_UU SRI_TaxRate_UU	  */
	public void setSRI_TaxRate_UU (String SRI_TaxRate_UU)
	{
		set_Value (COLUMNNAME_SRI_TaxRate_UU, SRI_TaxRate_UU);
	}

	/** Get SRI_TaxRate_UU.
		@return SRI_TaxRate_UU	  */
	public String getSRI_TaxRate_UU () 
	{
		return (String)get_Value(COLUMNNAME_SRI_TaxRate_UU);
	}

	/** Set SRI Tax Rate Value.
		@param SRI_TaxRateValue SRI Tax Rate Value	  */
	public void setSRI_TaxRateValue (String SRI_TaxRateValue)
	{
		set_Value (COLUMNNAME_SRI_TaxRateValue, SRI_TaxRateValue);
	}

	/** Get SRI Tax Rate Value.
		@return SRI Tax Rate Value	  */
	public String getSRI_TaxRateValue () 
	{
		return (String)get_Value(COLUMNNAME_SRI_TaxRateValue);
	}
}