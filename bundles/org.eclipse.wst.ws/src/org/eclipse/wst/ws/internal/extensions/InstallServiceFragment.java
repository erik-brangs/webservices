/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.wst.ws.internal.extensions;

import java.util.Vector;

import org.eclipse.wst.command.internal.provisional.env.core.ICommandFactory;
import org.eclipse.wst.command.internal.provisional.env.core.SimpleCommandFactory;

public class InstallServiceFragment extends AbstractServiceFragment 
{
  public InstallServiceFragment()
  {
  }
  
  protected InstallServiceFragment( InstallServiceFragment fragment )
  {
	super( fragment );  
  }
  
  public Object clone() 
  {
	return new InstallServiceFragment();
  }

  public ICommandFactory getICommandFactory() 
  {
	ICommandFactory factory = null;
	
	if( webService_ == null )
	{
	  factory = new SimpleCommandFactory( new Vector() );
	}
	else
	{
	  factory = webService_.install( environment_, context_, selection_, project_, module_, earProject_, ear_ );	
	}
	
	return factory;
  }
}
