/**********************************************************************
 * This file is part of Adempiere ERP Bazaar                           *
 * http://www.adempiere.org                                            *
 *                                                                     *
 * Copyright (C) Contributors                                          *
 *                                                                     *
 * This program is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU General Public License         *
 * as published by the Free Software Foundation; either version 2      *
 * of the License, or (at your option) any later version.              *
 *                                                                     *
 * This program is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
 * GNU General Public License for more details.                        *
 *                                                                     *
 * You should have received a copy of the GNU General Public License   *
 * along with this program; if not, write to the Free Software         *
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
 * MA 02110-1301, USA.                                                 *
 *                                                                     *
 * Contributors:                                                       *
 * - Jesus Garcia - GlobalQSS Colombia                                 *
 * - Carlos Ruiz  - GlobalQSS Colombia                                 *
 **********************************************************************/
package org.globalqss.process;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MInOut;
import org.compiere.model.MInvoice;
import org.compiere.model.MMailText;
import org.compiere.model.MMovement;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.globalqss.model.LEC_FE_MInOut;
import org.globalqss.model.LEC_FE_MInvoice;
import org.globalqss.model.LEC_FE_ModelValidator;
import org.globalqss.model.X_LEC_SRI_Format;
import org.globalqss.model.X_SRI_AccessCode;
import org.globalqss.model.X_SRI_Authorization;
import org.globalqss.model.X_SRI_IssueProcessBath;
import org.globalqss.util.LEC_FE_Utils;
import org.globalqss.util.LEC_FE_UtilsXml;

/**
 * Generate Contingency Authorizations
 * 
 * @author GlobalQSS/jjgq
 */
public class SRIContingencyGenerate extends SvrProcess {

	/** Client */
	private int m_AD_Client_ID = 0;

	/** Authorization */
	private int p_SRI_Authorization_ID = 0;

	/** Number of authorizations */
	private int m_created = 0;

	private int m_lec_sri_format_id = 0;

	private String file_name = "";
	private String m_retencionno = "";

	/** Invoice */
	private int p_C_Invoice_ID = 0;
	/**
	 * Prepare - e.g., get Parameters.
	 */
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("SRI_Authorization_ID"))
				p_SRI_Authorization_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Invoice_ID")){
				if (para[i].getParameter()!=null)
					p_C_Invoice_ID = para[i].getParameterAsInt();
			}
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}

		m_AD_Client_ID = getAD_Client_ID();

		if (p_SRI_Authorization_ID == 0)
			p_SRI_Authorization_ID = getRecord_ID();

	} // prepare

	/**
	 * Generate Invoices
	 * 
	 * @return info
	 * @throws Exception
	 */
	protected String doIt() throws Exception {
		String result = "";
		if (p_SRI_Authorization_ID>0){
			log.info("SRI_Authorization_ID=" + p_SRI_Authorization_ID);
			//
			String sql = null;
			sql = "SELECT * FROM SRI_Authorization a "
					+ " WHERE AD_Client_ID=?"
					+ "  AND SRI_AuthorizationCode IS NULL"
					+ "  AND IsActive = 'Y' AND Processed = 'N'"
					// 170-Clave de contingencia pendiente
					+ "  AND SRI_ErrorCode_ID IN (SELECT SRI_ErrorCode_ID FROM SRI_ErrorCode WHERE Value IN ('170','70'))"
					+ "  AND SUBSTR(Value,48,1)=? ";
			if (p_SRI_Authorization_ID != 0)
				sql += " AND SRI_Authorization_ID=?";

			// sql += " FOR UPDATE";

			PreparedStatement pstmt = null;
			try {
				pstmt = DB.prepareStatement(sql, get_TrxName());
				int index = 1;
				pstmt.setInt(index++, m_AD_Client_ID);
				// pstmt.setString(index++, LEC_FE_UtilsXml.emisionNormal);
				pstmt.setString(index++, LEC_FE_UtilsXml.emisionContingencia);
				if (p_SRI_Authorization_ID != 0)
					pstmt.setInt(index++, p_SRI_Authorization_ID);

			} catch (Exception e) {
				log.log(Level.SEVERE, sql, e);
			}

			result = generate(pstmt);

		}else if (p_C_Invoice_ID>0){
			log.info("SRI_Authorization_ID=" + p_SRI_Authorization_ID);
			reGenerateInvoiceAuthorization(p_C_Invoice_ID);
		}

		return result;
	} // doIt

	/**
	 * Generate Authorizations
	 * 
	 * @param pstmt
	 *            order query
	 * @return info
	 */
	private String generate(PreparedStatement pstmt) {
		String msg = null;

		try {

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {

				msg = null;

				X_SRI_Authorization authorization = new X_SRI_Authorization(
						getCtx(), rs, get_TrxName());

				// isSOTrx()
				if (authorization.getSRI_ShortDocType().equals("01")) // FACTURA
					msg = lecfeinvoice_SriExportInvoiceXML100(authorization);
				else if (authorization.getSRI_ShortDocType().equals("04")) // NOTA
																			// DE
																			// CRÉDITO
					msg = lecfeinvoice_SriExportInvoiceXML100(authorization);
				else if (authorization.getSRI_ShortDocType().equals("05")) // NOTA
																			// DE
																			// DÉBITO
					msg = lecfeinvoice_SriExportInvoiceXML100(authorization);
				else if (authorization.getSRI_ShortDocType().equals("06")
						&& LEC_FE_Utils.getAuthorisedInOut(authorization
								.getSRI_Authorization_ID()) > 0) // GUÍA DE
																	// REMISIÓN
																	// - Entrega
					msg = lecfeinout_SriExportInOutXML100(authorization);
				else if (authorization.getSRI_ShortDocType().equals("06")
						&& LEC_FE_Utils.getAuthorisedMovement(authorization
								.getSRI_Authorization_ID()) > 0) // GUÍA DE
																	// REMISIÓN
																	// -
																	// Movimiento
					msg = lecfemovement_SriExportMovementXML100(authorization);
				// !isSOTrx()
				else if (authorization.getSRI_ShortDocType().equals("07")) // COMPROBANTE
																			// DE
																			// RETENCIÓN
					msg = lecfeinvoice_SriExportInvoiceXML100(authorization);
				else
					log.warning("Formato no habilitado SRI: "
							+ authorization.getSRI_ShortDocType());

			} // for all authorizations
			rs.close();
			pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			log.log(Level.SEVERE, "", e);
			msg = e.getMessage();
		}
		try {
			if (pstmt != null)
				pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			pstmt = null;
		}
		if (m_created>0)
			return "@Created@ = " + m_created;
		else
			return msg;
	} // generate

	/**
	 * lecfeinvoice_SriExportInvoiceXML100
	 */
	public String lecfeinvoice_SriExportInvoiceXML100(
			X_SRI_Authorization authorization) {

		String msg = null;

		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();

		try {

			int c_invoice_id = 0;

			X_SRI_AccessCode accesscode = new X_SRI_AccessCode(getCtx(),
					authorization.getSRI_AccessCode_ID(), get_TrxName());

			File file = signature.getFileFromStream(file_name,
					authorization.getSRI_Authorization_ID());

			file_name = signature.getFilename(signature,
					LEC_FE_UtilsXml.folderComprobantesFirmados);
			log.warning("@Signed Xml@ -> " + file_name);

			if (file.exists() || file.isFile() || file.canRead()) {

				// Procesar Recepcion SRI
				log.warning("@Sending Xml@ -> " + file_name);
				msg = signature.respuestaRecepcionComprobante(file_name);

				commitEx();

				if (msg != null)
					if (!msg.equals("RECIBIDA")) {
						if (!msg.contains("CLAVE ACCESO REGISTRADA"))
							throw new AdempiereException(msg);
						else
							log.warning("@Authorizing Xml@ -> "
									+ file_name
									+ "CLAVE ACCESO REGISTRADA-CONSULTANDO AUTORIZACIONES VÁLIDAS");
					}
				// Procesar Autorizacion SRI
				log.warning("@Authorizing Xml@ -> " + file_name);
				msg = signature.respuestaAutorizacionComprobante(accesscode,
						authorization, authorization.getValue());
				if (msg != null)
					if (msg.contains("¡Comprobante Enviado!")) {
						msg = signature.respuestaAutorizacionComprobante(
								accesscode, authorization,
								authorization.getValue());
					} else {
						throw new AdempiereException(msg);
					}

				file_name = signature.getFilename(signature,
						LEC_FE_UtilsXml.folderComprobantesAutorizados);
				m_created++;

			}

			c_invoice_id = LEC_FE_Utils.getAuthorisedInvoice(authorization
					.getSRI_Authorization_ID());

			MInvoice invoice = new MInvoice(getCtx(), c_invoice_id,
					get_TrxName());

			// Formato
			m_lec_sri_format_id = LEC_FE_Utils.getLecSriFormat(
					getAD_Client_ID(), signature.getDeliveredType(),
					authorization.getSRI_ShortDocType(),
					invoice.getDateInvoiced(), invoice.getDateInvoiced());

			if (m_lec_sri_format_id < 1)
				throw new AdempiereUserError(
						"No existe formato para el comprobante");

			X_LEC_SRI_Format f = new X_LEC_SRI_Format(getCtx(),
					m_lec_sri_format_id, get_TrxName());

			// Emisor
			MOrgInfo oi = MOrgInfo.get(getCtx(), invoice.getAD_Org_ID(),
					get_TrxName());

			int c_bpartner_id = LEC_FE_Utils.getOrgBPartner(getAD_Client_ID(),
					oi.get_ValueAsString("TaxID"));
			MBPartner bpe = new MBPartner(getCtx(), c_bpartner_id,
					get_TrxName());

			m_retencionno = invoice.getDocumentNo();
			if (authorization.getSRI_ShortDocType().equals("07")) // COMPROBANTE
																	// DE
																	// RETENCIÓN
				m_retencionno = DB
						.getSQLValueString(
								get_TrxName(),
								"SELECT DISTINCT(DocumentNo) FROM LCO_InvoiceWithholding WHERE C_Invoice_ID = ? ",
								invoice.getC_Invoice_ID());

			//
			if (MSysConfig.getBooleanValue(
					"QSSLEC_FE_EnvioXmlAutorizadoBPEmail", false,
					getAD_Client_ID())) {
				List<File> atts = new ArrayList<File>();
				File attachment = (new File(file_name));
				atts.add(attachment);
				atts.add(invoice.createPDF());

				if (attachment.exists() || attachment.isFile()
						|| attachment.canRead()) {

					log.warning("@EMailing Xml@ -> " + file_name);
					// Enviar Email BPartner XML Autorizado
					MMailText mText = new MMailText(getCtx(), 0, get_TrxName()); // Solo
																					// en
																					// memoria
					mText.setPO(invoice);
					String subject = "SRI "
							+ (signature.isOnTesting ? LEC_FE_UtilsXml.nombreCertificacion
									: LEC_FE_UtilsXml.nombreProduccion) + " "
							+ bpe.getValue() + " : "
							+ f.get_ValueAsString("XmlPrintLabel") + " "
							+ m_retencionno;
					String text = " Emisor               : "
							+ bpe.getName()
							+ "\nFecha                : "
							+ LEC_FE_Utils.getDate(invoice.getDateInvoiced(),
									10)
							+ "\nCliente              : "
							+ invoice.getC_BPartner().getName()
							+ "\nComprobante          : "
							+ f.get_ValueAsString("XmlPrintLabel")
							+ "\nNumero               : "
							+ m_retencionno
							+ "\nAutorizacion No.     : "
							+ authorization.getSRI_AuthorizationCode()
							+ "\nFecha Autorizacion   : "
							+ authorization.getSRI_AuthorizationDate()
							+ "\nAdjunto              : "
							+ file_name.substring(file_name
									.lastIndexOf(File.separator) + 1);
					int SalesRep_ID = 0;
					if (MSysConfig.getBooleanValue(
							"QSSLEC_FE_EnvioXmlAutorizadoSalesRepEmail", true,
							getAD_Client_ID()))
						SalesRep_ID = invoice.getSalesRep_ID();

					int countMail = LEC_FE_Utils.notifyUsers(getCtx(), mText,
							authorization.getAD_UserMail_ID(), subject, text,
							atts, get_TrxName(), SalesRep_ID);
					if (countMail == 0)
						log.warning("@RequestActionEMailError@ -> " + file_name);
				}
			}

			//
		} catch (Exception e) {
			msg = "No se pudo obtener autorizacion - " + e.getMessage();
			log.severe(msg);
			throw new AdempiereException(msg);
		}

		return msg;

	} // lecfeinvoice_SriExportInvoiceXML100

	/**
	 * lecfeinout_SriExportInOutXML100
	 */
	public String lecfeinout_SriExportInOutXML100(
			X_SRI_Authorization authorization) {

		String msg = null;

		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();

		try {

			int m_inout_id = 0;

			X_SRI_AccessCode accesscode = new X_SRI_AccessCode(getCtx(),
					authorization.getSRI_AccessCode_ID(), get_TrxName());

			File file = signature.getFileFromStream(file_name,
					authorization.getSRI_Authorization_ID());

			file_name = signature.getFilename(signature,
					LEC_FE_UtilsXml.folderComprobantesFirmados);
			log.warning("@Signed Xml@ -> " + file_name);

			if (file.exists() || file.isFile() || file.canRead()) {

				// Procesar Recepcion SRI
				log.warning("@Sending Xml@ -> " + file_name);
				msg = signature.respuestaRecepcionComprobante(file_name);

				commitEx();
				if (msg != null)
					if (!msg.equals("RECIBIDA")) {
						if (!msg.contains("CLAVE ACCESO REGISTRADA"))
							throw new AdempiereException(msg);
						else
							log.warning("@Authorizing Xml@ -> "
									+ file_name
									+ "CLAVE ACCESO REGISTRADA-CONSULTANDO AUTORIZACIONES VÁLIDAS");
					}

				// Procesar Autorizacion SRI
				log.warning("@Authorizing Xml@ -> " + file_name);
				msg = signature.respuestaAutorizacionComprobante(accesscode,
						authorization, authorization.getValue());

				if (msg != null)
					if (msg.contains("¡Comprobante Enviado!")) {
						msg = signature.respuestaAutorizacionComprobante(
								accesscode, authorization,
								authorization.getValue());
					} else {
						throw new AdempiereException(msg);
					}

				file_name = signature.getFilename(signature,
						LEC_FE_UtilsXml.folderComprobantesAutorizados);
				m_created++;

			}

			m_inout_id = LEC_FE_Utils.getAuthorisedInOut(authorization
					.getSRI_Authorization_ID());

			MInOut inout = new MInOut(getCtx(), m_inout_id, get_TrxName());

			// Formato
			m_lec_sri_format_id = LEC_FE_Utils.getLecSriFormat(
					getAD_Client_ID(), signature.getDeliveredType(),
					authorization.getSRI_ShortDocType(),
					inout.getMovementDate(), inout.getMovementDate());

			if (m_lec_sri_format_id < 1)
				throw new AdempiereUserError(
						"No existe formato para el comprobante");

			X_LEC_SRI_Format f = new X_LEC_SRI_Format(getCtx(),
					m_lec_sri_format_id, get_TrxName());

			// Emisor
			MOrgInfo oi = MOrgInfo.get(getCtx(), inout.getAD_Org_ID(),
					get_TrxName());

			int c_bpartner_id = LEC_FE_Utils.getOrgBPartner(getAD_Client_ID(),
					oi.get_ValueAsString("TaxID"));
			MBPartner bpe = new MBPartner(getCtx(), c_bpartner_id,
					get_TrxName());

			//
			if (MSysConfig.getBooleanValue(
					"QSSLEC_FE_EnvioXmlAutorizadoBPEmail", false,
					getAD_Client_ID())) {
				List<File> atts = new ArrayList<File>();
				File attachment = (new File(file_name));
				atts.add(attachment);
				atts.add(inout.createPDF());

				if (attachment.exists() || attachment.isFile()
						|| attachment.canRead()) {

					log.warning("@EMailing Xml@ -> " + file_name);
					// Enviar Email BPartner XML Autorizado
					MMailText mText = new MMailText(getCtx(), 0, get_TrxName()); // Solo
																					// en
																					// memoria
					mText.setPO(inout);
					String subject = "SRI "
							+ (signature.isOnTesting ? LEC_FE_UtilsXml.nombreCertificacion
									: LEC_FE_UtilsXml.nombreProduccion) + " "
							+ bpe.getValue() + " : "
							+ f.get_ValueAsString("XmlPrintLabel") + " "
							+ inout.getDocumentNo();
					String text = " Emisor               : "
							+ bpe.getName()
							+ "\nFecha                : "
							+ LEC_FE_Utils.getDate(inout.getMovementDate(), 10)
							+ "\nCliente              : "
							+ inout.getC_BPartner().getName()
							+ "\nComprobante          : "
							+ f.get_ValueAsString("XmlPrintLabel")
							+ "\nNumero               : "
							+ inout.getDocumentNo()
							+ "\nAutorizacion No.     : "
							+ authorization.getSRI_AuthorizationCode()
							+ "\nFecha Autorizacion   : "
							+ authorization.getSRI_AuthorizationDate()
							+ "\nAdjunto              : "
							+ file_name.substring(file_name
									.lastIndexOf(File.separator) + 1);

					int SalesRep_ID = 0;
					if (MSysConfig.getBooleanValue(
							"QSSLEC_FE_EnvioXmlAutorizadoSalesRepEmail", true,
							getAD_Client_ID()))
						SalesRep_ID = inout.getSalesRep_ID();

					int countMail = LEC_FE_Utils.notifyUsers(getCtx(), mText,
							authorization.getAD_UserMail_ID(), subject, text,
							atts, get_TrxName(), SalesRep_ID);
					if (countMail == 0)
						log.warning("@RequestActionEMailError@ -> " + file_name);
				}
			}

			//
		} catch (Exception e) {
			msg = "No se pudo obtener autorizacion - " + e.getMessage();
			log.severe(msg);
			throw new AdempiereException(msg);
		}

		return msg;

	} // lecfeinout_SriExportInOutXML100

	/**
	 * lecfemovement_SriExportMovementXML100
	 */
	public String lecfemovement_SriExportMovementXML100(
			X_SRI_Authorization authorization) {

		String msg = null;

		LEC_FE_UtilsXml signature = new LEC_FE_UtilsXml();

		try {

			int m_movement_id = 0;

			X_SRI_AccessCode accesscode = new X_SRI_AccessCode(getCtx(),
					authorization.getSRI_AccessCode_ID(), get_TrxName());

			File file = signature.getFileFromStream(file_name,
					authorization.getSRI_Authorization_ID());

			file_name = signature.getFilename(signature,
					LEC_FE_UtilsXml.folderComprobantesFirmados);
			log.warning("@Signed Xml@ -> " + file_name);

			if (file.exists() || file.isFile() || file.canRead()) {

				// Procesar Recepcion SRI
				log.warning("@Sending Xml@ -> " + file_name);
				msg = signature.respuestaRecepcionComprobante(file_name);

				commitEx();

				if (msg != null)
					if (!msg.equals("RECIBIDA")) {
						if (!msg.contains("CLAVE ACCESO REGISTRADA"))
							throw new AdempiereException(msg);
						else
							log.warning("@Authorizing Xml@ -> "
									+ file_name
									+ "CLAVE ACCESO REGISTRADA-CONSULTANDO AUTORIZACIONES VÁLIDAS");
					}

				// Procesar Autorizacion SRI
				log.warning("@Authorizing Xml@ -> " + file_name);
				msg = signature.respuestaAutorizacionComprobante(accesscode,
						authorization, authorization.getValue());

				if (msg != null)
					if (msg.contains("¡Comprobante Enviado!")) {
						msg = signature.respuestaAutorizacionComprobante(
								accesscode, authorization,
								authorization.getValue());
					} else {
						throw new AdempiereException(msg);
					}

				file_name = signature.getFilename(signature,
						LEC_FE_UtilsXml.folderComprobantesAutorizados);
				m_created++;

			}

			m_movement_id = LEC_FE_Utils.getAuthorisedMovement(authorization
					.getSRI_Authorization_ID());

			MMovement movement = new MMovement(getCtx(), m_movement_id,
					get_TrxName());

			// Formato
			m_lec_sri_format_id = LEC_FE_Utils.getLecSriFormat(
					getAD_Client_ID(), signature.getDeliveredType(),
					authorization.getSRI_ShortDocType(),
					movement.getMovementDate(), movement.getMovementDate());

			if (m_lec_sri_format_id < 1)
				throw new AdempiereUserError(
						"No existe formato para el comprobante");

			X_LEC_SRI_Format f = new X_LEC_SRI_Format(getCtx(),
					m_lec_sri_format_id, get_TrxName());

			// Emisor
			MOrgInfo oi = MOrgInfo.get(getCtx(), movement.getAD_Org_ID(),
					get_TrxName());

			int c_bpartner_id = LEC_FE_Utils.getOrgBPartner(getAD_Client_ID(),
					oi.get_ValueAsString("TaxID"));
			MBPartner bpe = new MBPartner(getCtx(), c_bpartner_id,
					get_TrxName());

			//
			if (MSysConfig.getBooleanValue(
					"QSSLEC_FE_EnvioXmlAutorizadoBPEmail", false,
					getAD_Client_ID())) {
				List<File> atts = new ArrayList<File>();
				File attachment = (new File(file_name));
				atts.add(attachment);
				atts.add(movement.createPDF());

				if (attachment.exists() || attachment.isFile()
						|| attachment.canRead()) {

					log.warning("@EMailing Xml@ -> " + file_name);
					// Enviar Email BPartner XML Autorizado
					MMailText mText = new MMailText(getCtx(), 0, get_TrxName()); // Solo
																					// en
																					// memoria
					mText.setPO(movement);
					String subject = "SRI "
							+ (signature.isOnTesting ? LEC_FE_UtilsXml.nombreCertificacion
									: LEC_FE_UtilsXml.nombreProduccion) + " "
							+ bpe.getValue() + " : "
							+ f.get_ValueAsString("XmlPrintLabel") + " "
							+ movement.getDocumentNo();
					String text = " Emisor               : "
							+ bpe.getName()
							+ "\nFecha                : "
							+ LEC_FE_Utils.getDate(movement.getMovementDate(),
									10)
							+ "\nCliente              : "
							+ movement.getC_BPartner().getName()
							+ "\nComprobante          : "
							+ f.get_ValueAsString("XmlPrintLabel")
							+ "\nNumero               : "
							+ movement.getDocumentNo()
							+ "\nAutorizacion No.     : "
							+ authorization.getSRI_AuthorizationCode()
							+ "\nFecha Autorizacion   : "
							+ authorization.getSRI_AuthorizationDate()
							+ "\nAdjunto              : "
							+ file_name.substring(file_name
									.lastIndexOf(File.separator) + 1);

					int SalesRep_ID = 0;
					if (MSysConfig.getBooleanValue(
							"QSSLEC_FE_EnvioXmlAutorizadoSalesRepEmail", true,
							getAD_Client_ID()))
						SalesRep_ID = movement.getSalesRep_ID();

					int countMail = LEC_FE_Utils.notifyUsers(getCtx(), mText,
							authorization.getAD_UserMail_ID(), subject, text,
							atts, get_TrxName(), SalesRep_ID);
					if (countMail == 0)
						log.warning("@RequestActionEMailError@ -> " + file_name);
				}
			}

			//
		} catch (Exception e) {
			msg = "No se pudo obtener autorizacion - " + e.getMessage();
			log.severe(msg);
			throw new AdempiereException(msg);
		}

		return msg;

	} // lecfemovement_SriExportMovementXML100
	
	/**
	 * reGenerateInvoiceAuthorization (Build a new XML)
	 * 
	 * @param p_C_Invoice_ID
	 *            
	 * @return info
	 */
	private String reGenerateInvoiceAuthorization(int p_C_Invoice_ID) {
		String msg = null;
		MInvoice inv = new MInvoice(getCtx(),p_C_Invoice_ID,get_TrxName());
		if (!inv.get_ValueAsBoolean("SRI_IsUseContingency")){
			return null;
		}
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
				}else{
					a.setIsActive(false);
					a.setDescription("Inactivada por Re-Generación de Autorización, Factura:"+inv.getDocumentNo());
					a.saveEx();
				}
			}
		}
		inv.set_ValueOfColumn("SRI_Authorization_ID", null);
		inv.saveEx();
		
		msg = LEC_FE_ModelValidator.invoiceGenerateXml(inv);

		if (msg != null)
			throw new RuntimeException(msg);
		
		
		return msg;
	} // generate

} // SRIContingencyGenerate
