package org.eclipse.core.internal.boot.update;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 */
public class LogStoreException extends Exception
{
	public String _strMessage    = null;
	public String _strFilename   = null;
	public String _strLine       = null;
	public int    _iLineNumber   = -1;
	public int    _iColumnNumber = -1;
/**
 */
public LogStoreException( String strMessage, String strFilename, String strLine, int iLineNumber, int iColumnNumber )
{
	super( strMessage );
	
	_strMessage    = strMessage;
	_strFilename   = strFilename;
	_strLine       = strLine;
	_iLineNumber   = iLineNumber;
	_iColumnNumber = iColumnNumber;
}
}
