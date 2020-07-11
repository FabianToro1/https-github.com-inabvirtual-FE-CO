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

/** Generated Model for SRI_TaxCode
 *  @author iDempiere (generated) 
 *  @version Release 5.1 - $Id$ */
public class X_SRI_TaxCode extends PO implements I_SRI_TaxCode, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20180604L;

    /** Standard Constructor */
    public X_SRI_TaxCode (Properties ctx, int SRI_TaxCode_ID, String trxName)
    {
      super (ctx, SRI_TaxCode_ID, trxName);
      /** if (SRI_TaxCode_ID == 0)
        {
			setSRI_TaxCode_ID (0);
			setSRI_TaxCodeName (null);
			setSRI_TaxCodeValue (null);
        } */
    }

    /** Load Constructor */
    public X_SRI_TaxCode (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SRI_TaxCode[")
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

	/** Set SRI Tax Code Name.
		@param SRI_TaxCodeName SRI Tax Code Name	  */
	public void setSRI_TaxCodeName (String SRI_TaxCodeName)
	{
		set_Value (COLUMNNAME_SRI_TaxCodeName, SRI_TaxCodeName);
	}

	/** Get SRI Tax Code Name.
		@return SRI Tax Code Name	  */
	public String getSRI_TaxCodeName () 
	{
		return (String)get_Value(COLUMNNAME_SRI_TaxCodeName);
	}

	/** Set SRI_TaxCode_UU.
		@param SRI_TaxCode_UU SRI_TaxCode_UU	  */
	public void setSRI_TaxCode_UU (String SRI_TaxCode_UU)
	{
		set_Value (COLUMNNAME_SRI_TaxCode_UU, SRI_TaxCode_UU);
	}

	/** Get SRI_TaxCode_UU.
		@return SRI_TaxCode_UU	  */
	public String getSRI_TaxCode_UU () 
	{
		return (String)get_Value(COLUMNNAME_SRI_TaxCode_UU);
	}

	/** Set SRI Tax Code Value.
		@param SRI_TaxCodeValue SRI Tax Code Value	  */
	public void setSRI_TaxCodeValue (String SRI_TaxCodeValue)
	{
		set_Value (COLUMNNAME_SRI_TaxCodeValue, SRI_TaxCodeValue);
	}

	/** Get SRI Tax Code Value.
		@return SRI Tax Code Value	  */
	public String getSRI_TaxCodeValue () 
	{
		return (String)get_Value(COLUMNNAME_SRI_TaxCodeValue);
	}
}