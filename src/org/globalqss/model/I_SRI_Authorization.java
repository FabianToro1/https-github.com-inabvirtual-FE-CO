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
package org.globalqss.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for SRI_Authorization
 *  @author iDempiere (generated) 
 *  @version Release 2.0
 */
@SuppressWarnings("all")
public interface I_SRI_Authorization 
{

    /** TableName=SRI_Authorization */
    public static final String Table_Name = "SRI_Authorization";

    /** AD_Table_ID=1000012 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name AD_UserMail_ID */
    public static final String COLUMNNAME_AD_UserMail_ID = "AD_UserMail_ID";

	/** Set User Mail.
	  * Mail sent to the user
	  */
	public void setAD_UserMail_ID (int AD_UserMail_ID);

	/** Get User Mail.
	  * Mail sent to the user
	  */
	public int getAD_UserMail_ID();

	public org.compiere.model.I_AD_UserMail getAD_UserMail() throws RuntimeException;

    /** Column name ContingencyProcessing */
    public static final String COLUMNNAME_ContingencyProcessing = "ContingencyProcessing";

	/** Set Contingency Processing	  */
	public void setContingencyProcessing (String ContingencyProcessing);

	/** Get Contingency Processing	  */
	public String getContingencyProcessing();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name Mailing */
    public static final String COLUMNNAME_Mailing = "Mailing";

	/** Set Mailing	  */
	public void setMailing (String Mailing);

	/** Get Mailing	  */
	public String getMailing();

    /** Column name Processed */
    public static final String COLUMNNAME_Processed = "Processed";

	/** Set Processed.
	  * The document has been processed
	  */
	public void setProcessed (boolean Processed);

	/** Get Processed.
	  * The document has been processed
	  */
	public boolean isProcessed();

    /** Column name ReProcessing */
    public static final String COLUMNNAME_ReProcessing = "ReProcessing";

	/** Set ReProcessing	  */
	public void setReProcessing (String ReProcessing);

	/** Get ReProcessing	  */
	public String getReProcessing();

    /** Column name SRI_AccessCode_ID */
    public static final String COLUMNNAME_SRI_AccessCode_ID = "SRI_AccessCode_ID";

	/** Set SRI_AccessCode	  */
	public void setSRI_AccessCode_ID (int SRI_AccessCode_ID);

	/** Get SRI_AccessCode	  */
	public int getSRI_AccessCode_ID();

	public org.globalqss.model.I_SRI_AccessCode getSRI_AccessCode() throws RuntimeException;

    /** Column name SRI_AuthorizationCode */
    public static final String COLUMNNAME_SRI_AuthorizationCode = "SRI_AuthorizationCode";

	/** Set SRI Authorization Code	  */
	public void setSRI_AuthorizationCode (String SRI_AuthorizationCode);

	/** Get SRI Authorization Code	  */
	public String getSRI_AuthorizationCode();

    /** Column name SRI_AuthorizationDate */
    public static final String COLUMNNAME_SRI_AuthorizationDate = "SRI_AuthorizationDate";

	/** Set SRI Authorization Date	  */
	public void setSRI_AuthorizationDate (Timestamp SRI_AuthorizationDate);

	/** Get SRI Authorization Date	  */
	public Timestamp getSRI_AuthorizationDate();

    /** Column name SRI_Authorization_ID */
    public static final String COLUMNNAME_SRI_Authorization_ID = "SRI_Authorization_ID";

	/** Set SRI_Authorization	  */
	public void setSRI_Authorization_ID (int SRI_Authorization_ID);

	/** Get SRI_Authorization	  */
	public int getSRI_Authorization_ID();

    /** Column name SRI_Authorization_UU */
    public static final String COLUMNNAME_SRI_Authorization_UU = "SRI_Authorization_UU";

	/** Set SRI_Authorization_UU	  */
	public void setSRI_Authorization_UU (String SRI_Authorization_UU);

	/** Get SRI_Authorization_UU	  */
	public String getSRI_Authorization_UU();

    /** Column name SRI_ErrorCode_ID */
    public static final String COLUMNNAME_SRI_ErrorCode_ID = "SRI_ErrorCode_ID";

	/** Set SRI_ErrorCode	  */
	public void setSRI_ErrorCode_ID (int SRI_ErrorCode_ID);

	/** Get SRI_ErrorCode	  */
	public int getSRI_ErrorCode_ID();

	public org.globalqss.model.I_SRI_ErrorCode getSRI_ErrorCode() throws RuntimeException;

    /** Column name SRI_ShortDocType */
    public static final String COLUMNNAME_SRI_ShortDocType = "SRI_ShortDocType";

	/** Set SRI Short DocType	  */
	public void setSRI_ShortDocType (String SRI_ShortDocType);

	/** Get SRI Short DocType	  */
	public String getSRI_ShortDocType();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();
}
