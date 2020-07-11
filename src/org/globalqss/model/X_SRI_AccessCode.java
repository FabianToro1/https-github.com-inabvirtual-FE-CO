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

/** Generated Model for SRI_AccessCode
 *  @author iDempiere (generated) 
 *  @version Release 2.0 - $Id$ */
public class X_SRI_AccessCode extends PO implements I_SRI_AccessCode, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20140829L;

    /** Standard Constructor */
    public X_SRI_AccessCode (Properties ctx, int SRI_AccessCode_ID, String trxName)
    {
      super (ctx, SRI_AccessCode_ID, trxName);
      /** if (SRI_AccessCode_ID == 0)
        {
			setCodeAccessType (null);
			setEnvType (null);
			setIsUsed (false);
// N
			setSRI_AccessCode_ID (0);
			setSRI_ShortDocType (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_SRI_AccessCode (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SRI_AccessCode[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Automatic = 1 */
	public static final String CODEACCESSTYPE_Automatic = "1";
	/** Contingency = 2 */
	public static final String CODEACCESSTYPE_Contingency = "2";
	/** Set Code Access Type.
		@param CodeAccessType Code Access Type	  */
	public void setCodeAccessType (String CodeAccessType)
	{

		set_Value (COLUMNNAME_CodeAccessType, CodeAccessType);
	}

	/** Get Code Access Type.
		@return Code Access Type	  */
	public String getCodeAccessType () 
	{
		return (String)get_Value(COLUMNNAME_CodeAccessType);
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

	/** Test = 1 */
	public static final String ENVTYPE_Test = "1";
	/** Production = 2 */
	public static final String ENVTYPE_Production = "2";
	/** Set Env Type.
		@param EnvType Env Type	  */
	public void setEnvType (String EnvType)
	{

		set_Value (COLUMNNAME_EnvType, EnvType);
	}

	/** Get Env Type.
		@return Env Type	  */
	public String getEnvType () 
	{
		return (String)get_Value(COLUMNNAME_EnvType);
	}

	/** Set Is Used.
		@param IsUsed Is Used	  */
	public void setIsUsed (boolean IsUsed)
	{
		set_Value (COLUMNNAME_IsUsed, Boolean.valueOf(IsUsed));
	}

	/** Get Is Used.
		@return Is Used	  */
	public boolean isUsed () 
	{
		Object oo = get_Value(COLUMNNAME_IsUsed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Old Value.
		@param OldValue 
		The old file data
	  */
	public void setOldValue (String OldValue)
	{
		set_ValueNoCheck (COLUMNNAME_OldValue, OldValue);
	}

	/** Get Old Value.
		@return The old file data
	  */
	public String getOldValue () 
	{
		return (String)get_Value(COLUMNNAME_OldValue);
	}

	/** Set SRI_AccessCode.
		@param SRI_AccessCode_ID SRI_AccessCode	  */
	public void setSRI_AccessCode_ID (int SRI_AccessCode_ID)
	{
		if (SRI_AccessCode_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SRI_AccessCode_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SRI_AccessCode_ID, Integer.valueOf(SRI_AccessCode_ID));
	}

	/** Get SRI_AccessCode.
		@return SRI_AccessCode	  */
	public int getSRI_AccessCode_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SRI_AccessCode_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SRI_AccessCode_UU.
		@param SRI_AccessCode_UU SRI_AccessCode_UU	  */
	public void setSRI_AccessCode_UU (String SRI_AccessCode_UU)
	{
		set_Value (COLUMNNAME_SRI_AccessCode_UU, SRI_AccessCode_UU);
	}

	/** Get SRI_AccessCode_UU.
		@return SRI_AccessCode_UU	  */
	public String getSRI_AccessCode_UU () 
	{
		return (String)get_Value(COLUMNNAME_SRI_AccessCode_UU);
	}

	/** Invoice = 01 */
	public static final String SRI_SHORTDOCTYPE_Invoice = "01";
	/** Credit Memo = 04 */
	public static final String SRI_SHORTDOCTYPE_CreditMemo = "04";
	/** Debit Memo = 05 */
	public static final String SRI_SHORTDOCTYPE_DebitMemo = "05";
	/** Shipment = 06 */
	public static final String SRI_SHORTDOCTYPE_Shipment = "06";
	/** Withholding = 07 */
	public static final String SRI_SHORTDOCTYPE_Withholding = "07";
	/** Set SRI Short DocType.
		@param SRI_ShortDocType SRI Short DocType	  */
	public void setSRI_ShortDocType (String SRI_ShortDocType)
	{

		set_Value (COLUMNNAME_SRI_ShortDocType, SRI_ShortDocType);
	}

	/** Get SRI Short DocType.
		@return SRI Short DocType	  */
	public String getSRI_ShortDocType () 
	{
		return (String)get_Value(COLUMNNAME_SRI_ShortDocType);
	}

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}
}