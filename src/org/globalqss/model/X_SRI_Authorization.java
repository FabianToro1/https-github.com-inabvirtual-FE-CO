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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for SRI_Authorization
 *  @author iDempiere (generated) 
 *  @version Release 2.0 - $Id$ */
public class X_SRI_Authorization extends PO implements I_SRI_Authorization, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20140829L;

    /** Standard Constructor */
    public X_SRI_Authorization (Properties ctx, int SRI_Authorization_ID, String trxName)
    {
      super (ctx, SRI_Authorization_ID, trxName);
      /** if (SRI_Authorization_ID == 0)
        {
			setProcessed (false);
// N
			setSRI_Authorization_ID (0);
			setSRI_ShortDocType (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_SRI_Authorization (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_SRI_Authorization[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_UserMail getAD_UserMail() throws RuntimeException
    {
		return (org.compiere.model.I_AD_UserMail)MTable.get(getCtx(), org.compiere.model.I_AD_UserMail.Table_Name)
			.getPO(getAD_UserMail_ID(), get_TrxName());	}

	/** Set User Mail.
		@param AD_UserMail_ID 
		Mail sent to the user
	  */
	public void setAD_UserMail_ID (int AD_UserMail_ID)
	{
		if (AD_UserMail_ID < 1) 
			set_Value (COLUMNNAME_AD_UserMail_ID, null);
		else 
			set_Value (COLUMNNAME_AD_UserMail_ID, Integer.valueOf(AD_UserMail_ID));
	}

	/** Get User Mail.
		@return Mail sent to the user
	  */
	public int getAD_UserMail_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_UserMail_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Contingency Processing.
		@param ContingencyProcessing Contingency Processing	  */
	public void setContingencyProcessing (String ContingencyProcessing)
	{
		set_Value (COLUMNNAME_ContingencyProcessing, ContingencyProcessing);
	}

	/** Get Contingency Processing.
		@return Contingency Processing	  */
	public String getContingencyProcessing () 
	{
		return (String)get_Value(COLUMNNAME_ContingencyProcessing);
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

	/** Set Mailing.
		@param Mailing Mailing	  */
	public void setMailing (String Mailing)
	{
		set_Value (COLUMNNAME_Mailing, Mailing);
	}

	/** Get Mailing.
		@return Mailing	  */
	public String getMailing () 
	{
		return (String)get_Value(COLUMNNAME_Mailing);
	}

	/** Set Processed.
		@param Processed 
		The document has been processed
	  */
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed () 
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set ReProcessing.
		@param ReProcessing ReProcessing	  */
	public void setReProcessing (String ReProcessing)
	{
		set_Value (COLUMNNAME_ReProcessing, ReProcessing);
	}

	/** Get ReProcessing.
		@return ReProcessing	  */
	public String getReProcessing () 
	{
		return (String)get_Value(COLUMNNAME_ReProcessing);
	}

	public org.globalqss.model.I_SRI_AccessCode getSRI_AccessCode() throws RuntimeException
    {
		return (org.globalqss.model.I_SRI_AccessCode)MTable.get(getCtx(), org.globalqss.model.I_SRI_AccessCode.Table_Name)
			.getPO(getSRI_AccessCode_ID(), get_TrxName());	}

	/** Set SRI_AccessCode.
		@param SRI_AccessCode_ID SRI_AccessCode	  */
	public void setSRI_AccessCode_ID (int SRI_AccessCode_ID)
	{
		if (SRI_AccessCode_ID < 1) 
			set_Value (COLUMNNAME_SRI_AccessCode_ID, null);
		else 
			set_Value (COLUMNNAME_SRI_AccessCode_ID, Integer.valueOf(SRI_AccessCode_ID));
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

	/** Set SRI Authorization Code.
		@param SRI_AuthorizationCode SRI Authorization Code	  */
	public void setSRI_AuthorizationCode (String SRI_AuthorizationCode)
	{
		set_Value (COLUMNNAME_SRI_AuthorizationCode, SRI_AuthorizationCode);
	}

	/** Get SRI Authorization Code.
		@return SRI Authorization Code	  */
	public String getSRI_AuthorizationCode () 
	{
		return (String)get_Value(COLUMNNAME_SRI_AuthorizationCode);
	}

	/** Set SRI Authorization Date.
		@param SRI_AuthorizationDate SRI Authorization Date	  */
	public void setSRI_AuthorizationDate (Timestamp SRI_AuthorizationDate)
	{
		set_Value (COLUMNNAME_SRI_AuthorizationDate, SRI_AuthorizationDate);
	}

	/** Get SRI Authorization Date.
		@return SRI Authorization Date	  */
	public Timestamp getSRI_AuthorizationDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_SRI_AuthorizationDate);
	}

	/** Set SRI_Authorization.
		@param SRI_Authorization_ID SRI_Authorization	  */
	public void setSRI_Authorization_ID (int SRI_Authorization_ID)
	{
		if (SRI_Authorization_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_SRI_Authorization_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_SRI_Authorization_ID, Integer.valueOf(SRI_Authorization_ID));
	}

	/** Get SRI_Authorization.
		@return SRI_Authorization	  */
	public int getSRI_Authorization_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SRI_Authorization_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SRI_Authorization_UU.
		@param SRI_Authorization_UU SRI_Authorization_UU	  */
	public void setSRI_Authorization_UU (String SRI_Authorization_UU)
	{
		set_Value (COLUMNNAME_SRI_Authorization_UU, SRI_Authorization_UU);
	}

	/** Get SRI_Authorization_UU.
		@return SRI_Authorization_UU	  */
	public String getSRI_Authorization_UU () 
	{
		return (String)get_Value(COLUMNNAME_SRI_Authorization_UU);
	}

	public org.globalqss.model.I_SRI_ErrorCode getSRI_ErrorCode() throws RuntimeException
    {
		return (org.globalqss.model.I_SRI_ErrorCode)MTable.get(getCtx(), org.globalqss.model.I_SRI_ErrorCode.Table_Name)
			.getPO(getSRI_ErrorCode_ID(), get_TrxName());	}

	/** Set SRI_ErrorCode.
		@param SRI_ErrorCode_ID SRI_ErrorCode	  */
	public void setSRI_ErrorCode_ID (int SRI_ErrorCode_ID)
	{
		if (SRI_ErrorCode_ID < 1) 
			set_Value (COLUMNNAME_SRI_ErrorCode_ID, null);
		else 
			set_Value (COLUMNNAME_SRI_ErrorCode_ID, Integer.valueOf(SRI_ErrorCode_ID));
	}

	/** Get SRI_ErrorCode.
		@return SRI_ErrorCode	  */
	public int getSRI_ErrorCode_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SRI_ErrorCode_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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