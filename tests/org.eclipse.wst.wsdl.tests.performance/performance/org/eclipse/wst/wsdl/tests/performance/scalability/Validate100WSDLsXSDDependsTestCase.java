/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.wsdl.tests.performance.scalability;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.ws.internal.plugin.WSPlugin;
import org.eclipse.wst.ws.internal.preferences.PersistentWSIContext;

public class Validate100WSDLsXSDDependsTestCase extends RunWSDLValidatorTestCase
{
	protected void setUp() throws Exception
	{
		WSPlugin.getDefault().getWSIAPContext().updateWSICompliances(PersistentWSIContext.IGNORE_NON_WSI);
		WSPlugin.getDefault().getWSISSBPContext().updateWSICompliances(PersistentWSIContext.IGNORE_NON_WSI);
	    IProject project = createProject("sp");
	    String bundleId = getBundleId();
	    for(int i = 1; i < 100; i++)
	    {
	      copyFile(bundleId, "data/100WSDLsXSDDepends/sample" + i +".wsdl", project);
	    }
	    super.setUp();
	}

	protected String getFilePath()
	{
	    return "data/100WSDLsXSDDepends/sample0.xsd";
	}
	
	public void testValidate100WSDLsXSDDepends()
	{
	    try
	    {
	      super.execute();
	    }
	    catch (Exception t)
	    {
	      fail(t.getMessage());
	    }
	}

}
