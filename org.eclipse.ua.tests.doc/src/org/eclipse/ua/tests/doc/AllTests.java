/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.doc;

import org.eclipse.ua.tests.doc.internal.linkchecker.ApiDocTest;
import org.eclipse.ua.tests.doc.internal.linkchecker.PrebuiltIndexChecker;
import org.eclipse.ua.tests.doc.internal.linkchecker.TocLinkChecker;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
 * Tests all user assistance functionality (automated).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ PrebuiltIndexChecker.class, TocLinkChecker.class, ApiDocTest.class })
public class AllTests {
}
