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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;

/** Generated Model for LEC_SRI_Format
 *  @author iDempiere (generated) 
 *  @version Release 2.0 - $Id$ */
public class X_LEC_SRI_Format extends PO implements I_LEC_SRI_Format, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20140829L;

    /** Standard Constructor */
    public X_LEC_SRI_Format (Properties ctx, int LEC_SRI_Format_ID, String trxName)
    {
      super (ctx, LEC_SRI_Format_ID, trxName);
      /** if (LEC_SRI_Format_ID == 0)
        {
			setLEC_SRI_Format_ID (0);
			setName (null);
			setRecordsQty (Env.ZERO);
			setSRI_DeliveredType (null);
			setSRI_ShortDocType (null);
			setValidFrom (new Timestamp( System.currentTimeMillis() ));
			setXMLPrintLabel (null);
        } */
    }

    /** Load Constructor */
    public X_LEC_SRI_Format (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_LEC_SRI_Format[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Classname.
		@param Classname 
		Java Classname
	  */
	public void setClassname (String Classname)
	{
		set_Value (COLUMNNAME_Classname, Classname);
	}

	/** Get Classname.
		@return Java Classname
	  */
	public String getClassname () 
	{
		return (String)get_Value(COLUMNNAME_Classname);
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

	/** Set LEC_SRI_Format.
		@param LEC_SRI_Format_ID LEC_SRI_Format	  */
	public void setLEC_SRI_Format_ID (int LEC_SRI_Format_ID)
	{
		if (LEC_SRI_Format_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_LEC_SRI_Format_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_LEC_SRI_Format_ID, Integer.valueOf(LEC_SRI_Format_ID));
	}

	/** Get LEC_SRI_Format.
		@return LEC_SRI_Format	  */
	public int getLEC_SRI_Format_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_LEC_SRI_Format_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set LEC_SRI_Format_UU.
		@param LEC_SRI_Format_UU LEC_SRI_Format_UU	  */
	public void setLEC_SRI_Format_UU (String LEC_SRI_Format_UU)
	{
		set_Value (COLUMNNAME_LEC_SRI_Format_UU, LEC_SRI_Format_UU);
	}

	/** Get LEC_SRI_Format_UU.
		@return LEC_SRI_Format_UU	  */
	public String getLEC_SRI_Format_UU () 
	{
		return (String)get_Value(COLUMNNAME_LEC_SRI_Format_UU);
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair() 
    {
        return new KeyNamePair(get_ID(), getName());
    }

	/** Set Records Qty.
		@param RecordsQty Records Qty	  */
	public void setRecordsQty (BigDecimal RecordsQty)
	{
		set_Value (COLUMNNAME_RecordsQty, RecordsQty);
	}

	/** Get Records Qty.
		@return Records Qty	  */
	public BigDecimal getRecordsQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_RecordsQty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Normal Delivery = 1 */
	public static final String SRI_DELIVEREDTYPE_NormalDelivery = "1";
	/** System Unavailability Delivery = 2 */
	public static final String SRI_DELIVEREDTYPE_SystemUnavailabilityDelivery = "2";
	/** Set SRI_DeliveredType.
		@param SRI_DeliveredType SRI_DeliveredType	  */
	public void setSRI_DeliveredType (String SRI_DeliveredType)
	{

		set_Value (COLUMNNAME_SRI_DeliveredType, SRI_DeliveredType);
	}

	/** Get SRI_DeliveredType.
		@return SRI_DeliveredType	  */
	public String getSRI_DeliveredType () 
	{
		return (String)get_Value(COLUMNNAME_SRI_DeliveredType);
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

	/** Set URL XSD.
		@param URL_XSD URL XSD	  */
	public void setURL_XSD (String URL_XSD)
	{
		set_Value (COLUMNNAME_URL_XSD, URL_XSD);
	}

	/** Get URL XSD.
		@return URL XSD	  */
	public String getURL_XSD () 
	{
		return (String)get_Value(COLUMNNAME_URL_XSD);
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

	/** Set Version No.
		@param VersionNo 
		Version Number
	  */
	public void setVersionNo (String VersionNo)
	{
		set_ValueNoCheck (COLUMNNAME_VersionNo, VersionNo);
	}

	/** Get Version No.
		@return Version Number
	  */
	public String getVersionNo () 
	{
		return (String)get_Value(COLUMNNAME_VersionNo);
	}

	/** Set XML Print Label.
		@param XMLPrintLabel XML Print Label	  */
	public void setXMLPrintLabel (String XMLPrintLabel)
	{
		set_Value (COLUMNNAME_XMLPrintLabel, XMLPrintLabel);
	}

	/** Get XML Print Label.
		@return XML Print Label	  */
	public String getXMLPrintLabel () 
	{
		return (String)get_Value(COLUMNNAME_XMLPrintLabel);
	}
}