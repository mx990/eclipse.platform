/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor.support;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor;
import org.eclipse.ant.internal.ui.editor.outline.AntModel;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.w3c.dom.Element;

public class TestTextCompletionProcessor extends AntEditorCompletionProcessor {

	private File editedFile;

	public TestTextCompletionProcessor(AntModel model) {
		super(model);
	}
	
	public TestTextCompletionProcessor() {
		super(null);
	}
    public ICompletionProposal[] getAttributeProposals(String aTaskName, String aPrefix) {
    	cursorPosition= aTaskName.length();
        return super.getAttributeProposals(aTaskName, aPrefix);
    }

    public Element findChildElementNamedOf(Element anElement, String aChildElementName) {
        return super.findChildElementNamedOf(anElement, aChildElementName);
    }

    public ICompletionProposal[] getTaskProposals(String text, String parentName, String aPrefix) {
    	cursorPosition= Math.max(0, text.length() - 1);
        return super.getTaskProposals(new Document(text), parentName, aPrefix);
    }
    
    public ICompletionProposal[] getTaskProposals(Document document, String parentName, String aPrefix) {
    	cursorPosition= Math.max(0, document.getLength() - 1);
    	return super.getTaskProposals(document, parentName, aPrefix);
    }

    public int determineProposalMode(String text, int aCursorPosition, String aPrefix) {
        return super.determineProposalMode(new Document(text), aCursorPosition, aPrefix);
    }

    public String getParentName(String text, int aLineNumber, int aColumnNumber) {
        return super.getParentName(new Document(text), aLineNumber, aColumnNumber);
    }
    
    public String getParentName(Document doc, int aLineNumber, int aColumnNumber) {
    	return super.getParentName(doc, aLineNumber, aColumnNumber);
    }

    public String getPrefixFromDocument(String aDocumentText, int anOffset) {
        return super.getPrefixFromDocument(aDocumentText, anOffset);
    }

    public ICompletionProposal[] getPropertyProposals(String text, String aPrefix, int aCursorPosition) {
        return super.getPropertyProposals(new Document(text), aPrefix, aCursorPosition);
    }

    /**
     * Returns the edited File that org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor sets or a temporary 
     * file, which only serves as a dummy.
     * @see org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor#getEditedFile()
     */
	public File getEditedFile() {
		if (editedFile != null){
			return editedFile;
		}
		File tempFile = null;
        try {
            tempFile = File.createTempFile("test", null);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        tempFile.deleteOnExit();
        return tempFile;
    }

	public void setLineNumber(int aLineNumber) {
    	lineNumber = aLineNumber;
    }

	public void setColumnNumber(int aColumnNumber) {
    	columnNumber = aColumnNumber;
    }
	
	public void setCursorPosition(int cursorPosition) {
		this.cursorPosition = cursorPosition;
	}
    
	public void setEditedFile(File aFile) {
		editedFile= aFile;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.editor.AntEditorCompletionProcessor#getTargetAttributeValueProposals(org.eclipse.jface.text.IDocument, java.lang.String, java.lang.String, java.lang.String)
	 */
	public ICompletionProposal[] getTargetAttributeValueProposals(String documentText, String textToSearch, String prefix, String attributeName) {
		return super.getTargetAttributeValueProposals(new Document(documentText), textToSearch, prefix, attributeName);
	}
}