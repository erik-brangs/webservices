/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 * yyyymmdd bug      Email and other contact information
 * -------- -------- -----------------------------------------------------------
 * 20071024   196997 pmoogk@ca.ibm.com - Peter Moogk
 * 20071113   209701 pmoogk@ca.ibm.com - Peter Moogk
 *******************************************************************************/
package org.eclipse.wst.ws.service.internal.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.wst.ws.service.policy.IFilter;
import org.eclipse.wst.ws.service.policy.IServicePolicy;
import org.eclipse.wst.ws.service.policy.IStateEnumerationItem;
import org.eclipse.wst.ws.service.policy.ServicePolicyActivator;
import org.eclipse.wst.ws.service.policy.listeners.IPolicyChildChangeListener;
import org.eclipse.wst.ws.service.policy.listeners.IPolicyPlatformLoadListener;
import org.osgi.service.prefs.BackingStoreException;


public class ServicePolicyPlatformImpl 
{
  private List<IPolicyPlatformLoadListener>        loadListeners;
  private Map<String, ServicePolicyImpl>           committedPolicyMap;
  private Map<String, ServicePolicyImpl>           policyMap;
  private Map<String, List<IStateEnumerationItem>> enumList;
  private Map<String, StateEnumerationItemImpl>    enumItemList;
  private Map<IProject, ProjectEntry>              enabledProjectMap;
  private List<Expression>                         enabledList;
  private List<IPolicyChildChangeListener>         childChangeListeners;
  
  public ServicePolicyPlatformImpl()
  {
  }
  
  public void load()
  {
    ServicePolicyRegistry registry  = new ServicePolicyRegistry( this );
    List<String>          localIds  = LocalUtils.getLocalPolicyIds();
    
    loadListeners     = new Vector<IPolicyPlatformLoadListener>();
    policyMap         = new HashMap<String, ServicePolicyImpl>();
    enumList          = new HashMap<String, List<IStateEnumerationItem>>();
    enumItemList      = new HashMap<String, StateEnumerationItemImpl>();
    enabledProjectMap = new HashMap<IProject, ProjectEntry>();
    enabledList       = new Vector<Expression>();
    childChangeListeners = new Vector<IPolicyChildChangeListener>();
    
    //Load local policies
    for( String localPolicyId : localIds )
    {
      ServicePolicyImpl localPolicy = LocalUtils.loadLocalPolicy( localPolicyId, this );
      
      policyMap.put( localPolicyId, localPolicy );
    }
   
    registry.load( loadListeners, policyMap, enumList, enumItemList );
    
    for( IPolicyPlatformLoadListener loadListener : loadListeners )
    {
      loadListener.load();
    }
    
    commitChanges( false );    
  }
  
  public void addEnabledExpression( Expression expression )
  {
    if( expression != null )
    {
      enabledList.add( expression );
    }
  }
  
  public boolean isEnabled( Object object )
  {
    boolean            result = false;
    IEvaluationContext context = new EvaluationContext( null, object );
    context.addVariable( "selection", object ); //$NON-NLS-1$
    context.setAllowPluginActivation( true );
    
    for( Expression enabledItem : enabledList )
    {
      try
      {
        EvaluationResult expResult = enabledItem.evaluate( context );
        
        // If any expression returns TRUE or NOT_LOADED we will return true as
        // the result.
        if( expResult != EvaluationResult.FALSE )
        {
          result = true;
          break;
        }
      }
      catch( CoreException exc )
      {
        // Ignore the expression if an exception occurs.
        ServicePolicyActivator.logError( "Error evaluating enablement expression.", exc ); //$NON-NLS-1$
      }
    }
    
    return result;
  }
  
  public void commitChanges( boolean saveLocals )
  {
    List<String> localIds = new Vector<String>();
    
    if( saveLocals ) LocalUtils.removeAllLocalPolicies();
    
    for( ServicePolicyImpl policy : policyMap.values() )
    {
      policy.commitChanges();
      
      if( saveLocals && !policy.isPredefined() )
      {
        LocalUtils.saveLocalPolicy( policy );
        localIds.add( policy.getId() );
      }
    }
    
    if( saveLocals ) LocalUtils.saveLocalIds( localIds );
    
    committedPolicyMap = new HashMap<String, ServicePolicyImpl>();
    committedPolicyMap.putAll( policyMap );
    ServicePolicyActivator.getDefault().savePluginPreferences();
  }
  
  /**
   * This method is called internally to remove a service policy.
   * 
   * @param policy
   */
  public void removePolicy( ServicePolicyImpl policy )
  {
    policyMap.remove( policy.getId() );
    fireChildChangeEvent( policy, false );
  }
  
  /**
   * This method is only called from the platform API
   * 
   * @param policy
   */
  public void removePlatformPolicy( IServicePolicy policy )
  {
    IServicePolicy parent = policy.getParentPolicy();
    
    if( parent == null )
    {
      // Remove any children first
      List<IServicePolicy> children = new Vector<IServicePolicy>( policy.getChildren() );
      
      for( IServicePolicy child : children )
      {
        policy.removeChild( child );  
      }
      
      removePolicy( (ServicePolicyImpl)policy);
    }
    else
    {
      parent.removeChild( policy );
    }
  }
  
  public void addChildChangeListener( IPolicyChildChangeListener listener )
  {
    childChangeListeners.add( listener );
  }
  
  public void removeChildChangeListener( IPolicyChildChangeListener listener )
  {
    childChangeListeners.remove( listener );  
  }
  
  private void fireChildChangeEvent( IServicePolicy policy, boolean isAdd )
  {
    for( IPolicyChildChangeListener listener : childChangeListeners )
    {
      listener.childChange( policy, isAdd );     
    }
  }
  
  public void commitChanges( IProject project )
  {
    for( ServicePolicyImpl policy : policyMap.values() )
    {
      ((PolicyStateImpl)policy.getPolicyState( project )).commitChanges();
    }
    
    ProjectEntry entry = getProjectEntry( project );
    
    entry.isEnabledCommitted = entry.isEnabled;  
    setProjectEnabled( project, entry.isEnabledCommitted );
    
    try
    {
      IEclipsePreferences projectPrefs = new ProjectScope( project ).getNode( ServicePolicyActivator.PLUGIN_ID );
      
      projectPrefs.flush();
    }
    catch( BackingStoreException exc )
    {
      ServicePolicyActivator.logError( "Error while committing project preferences.", exc ); //$NON-NLS-1$
    }
    
  }
  
  public void discardChanges()
  {
    for( ServicePolicyImpl policy : policyMap.values() )
    {
      policy.discardChanges();
    }
    
    policyMap = new HashMap<String, ServicePolicyImpl>();
    policyMap.putAll( committedPolicyMap );
  }
  
  public void discardChanges( IProject project )
  {
    for( ServicePolicyImpl policy : policyMap.values() )
    {
      ((PolicyStateImpl)policy.getPolicyState( project )).discardChanges();
    }
    
    ProjectEntry entry = getProjectEntry( project );
    
    entry.isEnabled = entry.isEnabledCommitted;  
  }
  
  public void restoreDefaults()
  {
    for( ServicePolicyImpl policy : policyMap.values() )
    {
      policy.restoreDefaults();
    }
  }
  
  public void restoreDefaults( IProject project )
  {
    for( ServicePolicyImpl policy : policyMap.values() )
    {
      policy.restoreDefaults( project );
    }    
  }
  
  public Set<String> getAllPolicyIds()
  {
    return policyMap.keySet();
  }
    
  public List<IServicePolicy> getRootServicePolicies( IFilter filter )
  {
    List<IServicePolicy> rootPolicies = new Vector<IServicePolicy>();
    
    for( String policyId : policyMap.keySet() )
    {
      ServicePolicyImpl policy = policyMap.get( policyId );
      
      if( policy.getParentPolicy() == null )
      {
        if( filter == null || (filter != null && filter.accept( policy )) )
        {
          rootPolicies.add( policy );
        }
      }
    }
    
    return rootPolicies;
  }
  
  public ServicePolicyImpl createServicePolicy( IServicePolicy parent, 
                                                String         id, 
                                                String         enumListId, 
                                                String         defaultEnumId )
  {
    String            uniqueId = makeUniqueId( id );
    ServicePolicyImpl policy   = new ServicePolicyImpl( false, uniqueId, this );
    
    policy.setParent( (ServicePolicyImpl)parent );
    policy.setEnumListId( enumListId );
    policy.setDefaultEnumId( defaultEnumId );
    policyMap.put( uniqueId, policy );
    fireChildChangeEvent( policy, true );
    
    return policy;
  }
  
  public ServicePolicyImpl getServicePolicy( String id )
  {
    return id == null ? null : policyMap.get( id );  
  }
  
  public List<IStateEnumerationItem> getStateEnumeration( String enumId )
  {
    return enumList.get( enumId ); 
  }
  
  public IStateEnumerationItem getStateItemEnumeration( String stateItemId )
  {
    return enumItemList.get( stateItemId );
  }
  
  public boolean isProjectPreferencesEnabled( IProject project )
  {
    ProjectEntry entry  = getProjectEntry( project );
    
    return entry.isEnabled;
  }
  
  private boolean getProjectPreferenceEnabled( IProject project )
  {
    String              pluginId          = ServicePolicyActivator.PLUGIN_ID;
    IEclipsePreferences projectPreference = new ProjectScope( project ).getNode( pluginId );
    String              key               = pluginId + ".projectEnabled"; //$NON-NLS-1$
    
    return projectPreference.getBoolean( key, false );    
  }
  
  public void setProjectPreferencesEnabled( IProject project, boolean value )
  {
    ProjectEntry entry  = getProjectEntry( project );
    
    entry.isEnabled = value;
  }
  
  private void setProjectEnabled( IProject project, boolean value )
  {
    String              pluginId          = ServicePolicyActivator.PLUGIN_ID;
    IEclipsePreferences projectPreference = new ProjectScope( project ).getNode( pluginId );
    String              key               = pluginId + ".projectEnabled"; //$NON-NLS-1$

    projectPreference.putBoolean( key, value );    
  }
  
  private ProjectEntry getProjectEntry( IProject project )
  {
    ProjectEntry entry  = enabledProjectMap.get( project );
    
    if( entry == null )
    {
      entry = new ProjectEntry();
      enabledProjectMap.put( project, entry );
      entry.isEnabledCommitted = getProjectPreferenceEnabled( project );
      entry.isEnabled = entry.isEnabledCommitted;
    }

    return entry;
  }
  
  private String makeUniqueId( String id )
  {
    String  result  = id;
    int     idCount = 1;
    Pattern pattern = Pattern.compile( "\\d*$" ); // Match any numerical digits at the end //$NON-NLS-1$
                                                  // of the string.
    
    while( policyMap.containsKey( result ) )
    {
      Matcher matcher = pattern.matcher( result );
    
      result = matcher.replaceFirst( "" ) + idCount; //$NON-NLS-1$
      idCount++;
    }
    
    return result;
  }
  
  private class ProjectEntry
  {
     boolean isEnabledCommitted;
     boolean isEnabled;
  }
}