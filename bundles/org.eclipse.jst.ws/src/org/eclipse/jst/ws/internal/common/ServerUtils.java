/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jst.ws.internal.common;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jst.j2ee.internal.J2EEVersionConstants;
import org.eclipse.jst.j2ee.internal.servertarget.IServerTargetConstants;
import org.eclipse.jst.j2ee.internal.servertarget.ServerTargetHelper;
import org.eclipse.wst.command.internal.provisional.env.core.common.Environment;
import org.eclipse.wst.command.internal.provisional.env.core.common.MessageUtils;
import org.eclipse.wst.command.internal.provisional.env.core.common.SimpleStatus;
import org.eclipse.wst.command.internal.provisional.env.core.common.Status;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IProjectProperties;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.IRunningActionServer;

/**
 * This class contains useful methods for working with Server plugin functions
 */
public final class ServerUtils {

  private MessageUtils msgUtils_;		
  private Hashtable serverIdToLabel_;
  private Hashtable serverLabelToId_;
  private static ServerUtils instance_;

  public static ServerUtils getInstance() {
    if (instance_ == null) {
      instance_ = new ServerUtils();
    }
    return instance_;

  }

  public ServerUtils(){
    String pluginId = "org.eclipse.jst.ws";
    msgUtils_ = new MessageUtils(pluginId + ".plugin", this);  	
  }  
  
  // Gets the Server labels given the server factory id
  public void getServerLabelsAndIds() {
    serverIdToLabel_ = new Hashtable();
    serverLabelToId_ = new Hashtable();
    IServerType[] serverTypes = ServerCore.getServerTypes();
    for (int idx = 0; idx < serverTypes.length; idx++) {

      IServerType serverType = (IServerType) serverTypes[idx];

      String id = serverType.getId();
      String serverLabel = serverType.getName();
      if (!(id == null) && !(serverLabel == null)) {
        serverIdToLabel_.put(id, serverLabel);
        serverLabelToId_.put(serverLabel, id);
      }
    }
  }

  public String getServerLabelForId(String factoryId) {
    if (serverIdToLabel_ == null)
      this.getServerLabelsAndIds();
    return (String) serverIdToLabel_.get(factoryId);
  }

  public String getServerIdForLabel(String factoryLabel) {
    if (serverLabelToId_ == null)
      this.getServerLabelsAndIds();
    return (String) serverLabelToId_.get(factoryLabel);
  }
  
  public Status modifyModules(Environment env, IServer server, IModule module, boolean add, IProgressMonitor monitor) {

  	IServerWorkingCopy wc = null;
  	Status status = new SimpleStatus("");
    try {

      if (module==null || !module.getProject().exists()) {
    	return status;
      }


      // check if module is a true Java project
  	  if (module instanceof IModule){
  	  	IModule pm = (IModule)module;
  	  	if (pm!=null){
  	  		IProject project = pm.getProject();
  	  		if (project==null || ResourceUtils.isTrueJavaProject(project)) {
  				return status;
  	  		}
  	  	}
  	  }
       
      wc = server.createWorkingCopy();     
      if (wc!=null){
      	Object x = server.getAdapter(IRunningActionServer.class);
        if (x!=null && x instanceof IRunningActionServer) {
         int state = server.getServerState();
         if (state == IServer.STATE_STOPPED || state == IServer.STATE_UNKNOWN) {
           String mode = ILaunchManager.RUN_MODE;
           server.synchronousStart(mode, monitor);
         }
        }
     
      List list = Arrays.asList(server.getModules());
      if (add) {
        if (!list.contains(module)) {
          ServerUtil.modifyModules(wc, new IModule[] { module}, new IModule[0], monitor);
        }
      }
      else { // removes module
        if (list.contains(module)) {
          ServerUtil.modifyModules(wc, new IModule[0], new IModule[] { module}, monitor);
        }
      }
      }
      else {
      	// handle case of Null WC; non-issue for now
      }
    }
    catch (CoreException ce) {
        status = new SimpleStatus("", msgUtils_.getMessage("MSG_ERROR_SERVER"), Status.ERROR, ce);
        env.getStatusHandler().reportError(status);
        return status;
    }
    finally {
        if (wc != null) {
          // Always saveAll and release the serverWorkingCopy
          try {
          	wc.saveAll(true, monitor);
          }
          catch(CoreException cex){
            status = new SimpleStatus("", msgUtils_.getMessage("MSG_ERROR_SERVER"), Status.ERROR, cex);
            env.getStatusHandler().reportError(status);
            return status;
          }
        }
      }
    return status;
  }  

  public static IServer getServerForModule(IModule module, String serverTypeId, IServer server, boolean create, IProgressMonitor monitor) {
    if (server != null)
      return server;
    else
      return getServerForModule(module, serverTypeId, create, monitor);
  }

  public static IServer getServerForModule(IModule module, String serverTypeId, boolean create, IProgressMonitor monitor) {
    try {

      IServer[] servers = ServerUtil.getServersByModule(module, monitor);

      if (servers != null && servers.length > 0) {
        if (serverTypeId == null || serverTypeId.length() == 0)
          return servers[0]; // WSAD v4 behavior

        for (int i = 0; i < servers.length; i++) {
          if (servers[i].getServerType().getId().equalsIgnoreCase(serverTypeId))
            return servers[i];
        }
      }

      return createServer(module, serverTypeId, monitor);

    }
    catch (Exception e) {
      return null;
    }
  }

  public static IServer getServerForModule(IModule module) {
    try {
      IServer[] servers = ServerUtil.getServersByModule(module, null);
      return ((servers != null && servers.length > 0) ? servers[0] : null);
    }
    catch (Exception e) {
      return null;
    }
  }

  public IServer createServer(Environment env, IModule module, String serverTypeId, IProgressMonitor monitor){
  	IServerWorkingCopy serverWC = null;
  	IServer server = null;
  	try {
      IServerType serverType = ServerCore.findServerType(serverTypeId);
      serverWC = serverType.createServer(serverTypeId, null, monitor);
      try{
        if (serverWC!=null){
          server = serverWC.saveAll(true, monitor);
        }
      }
      catch(CoreException ce){
            Status status = new SimpleStatus("", msgUtils_.getMessage("MSG_ERROR_SERVER"), Status.ERROR, ce);
            env.getStatusHandler().reportError(status);       
        return null;
      }

      if (server != null) {
        
      	Object x = server.getAdapter(IRunningActionServer.class);
        if (x!=null && x instanceof IRunningActionServer) {
         int state = server.getServerState();
         if (state == IServer.STATE_STOPPED || state == IServer.STATE_UNKNOWN) {
           String mode = ILaunchManager.RUN_MODE;
           server.synchronousStart(mode, monitor);
         }
        }  
      
        if (module != null && module.getProject().exists()) {
          IModule[] parentModules = server.getRootModules(module, monitor);
          if (parentModules!=null && parentModules.length!=0) {
          	module = (IModule)parentModules[0];
          }
          serverWC.modifyModules(new IModule[] { module}, new IModule[0], monitor);
        }
        
      }

      return server;
    }
    catch (Exception e) {
        Status status = new SimpleStatus("", msgUtils_.getMessage("MSG_ERROR_SERVER"), Status.ERROR, e);
        env.getStatusHandler().reportError(status);
    	return null;
    }
    finally{
    	try{
    		if (serverWC!=null){
    			serverWC.saveAll(true, monitor);
    		}
    	}
    	catch(CoreException ce){
            Status status = new SimpleStatus("", msgUtils_.getMessage("MSG_ERROR_SERVER"), Status.ERROR, ce);
            env.getStatusHandler().reportError(status);    		
    		return null;
    	}
    }
  }  
  
  /*
   * createServer This creates a server but does not report errors. 
   * @param module 
   * @param serverTypeId
   * @param monitor progress monitor
   * @return IServer returns null if unsuccessful
   * 
   */
  public static IServer createServer(IModule module, String serverTypeId, IProgressMonitor monitor) {
  	IServerWorkingCopy serverWC = null;
  	IServer server= null;
  	try {
      IServerType serverType = ServerCore.findServerType(serverTypeId);
      serverWC = serverType.createServer(serverTypeId, null, monitor);
      
      try{
        if (serverWC!=null){
          server = serverWC.saveAll(true, monitor);
        }
      }
      catch(CoreException ce){
        return null;
      }
      
      if (server != null) {
        
      	Object x = server.getAdapter(IRunningActionServer.class);
        if (x!=null && x instanceof IRunningActionServer) {
         int state = server.getServerState();
         if (state == IServer.STATE_STOPPED || state == IServer.STATE_UNKNOWN) {
           String mode = ILaunchManager.RUN_MODE;
           server.synchronousStart(mode, monitor);
         }
        }  
        if (module != null) {
          IModule[] parentModules = server.getRootModules(module, monitor);
          if (parentModules!=null && parentModules.length!=0) {
          	module = (IModule)parentModules[0];
          }
          serverWC.modifyModules(new IModule[] { module}, new IModule[0], monitor);
        }
        
      }

      return server;
    }
    catch (Exception e) {
      return null;
    }
    finally{
    	try{
    		if (serverWC!=null){
    			serverWC.saveAll(true, monitor);
    		}
    	}
    	catch(CoreException ce){
    		return null;
    		//handler core exception
    	}
    }
  }

  public static String[] getServerTypeIdsByModule( IVirtualComponent component )
  {
	IProject project   = component.getProject();
	String[] serverIds = null;
	
	if( project != null )
	{
	  IServer[] servers = ServerUtil.getServersByModule(getModule(project, component.getName() ), null);
	  
	  if( servers != null )
	  {
		serverIds = new String[servers.length];
		
		for( int index = 0; index < servers.length; index++ )
		{
		  serverIds[index] = servers[index].getId();
		  
		}
	  }
	}
	
	if( serverIds == null )
	{
	  serverIds = new String[0];	
	}
	
	return serverIds;  
  }
  
  /**
   * 
   * @param project
   * @return
   * @deprecated  should be using getServerTypeIdsByModule( IVirtualComponent )
   */
  public static String[] getServerTypeIdsByModule(IProject project) {
    Vector serverIds = new Vector();
    if (project != null) {
      IServer[] servers = ServerUtil.getServersByModule(ResourceUtils.getModule(project), null);
      if (servers != null && servers.length > 0) {
        for (int i = 0; i < servers.length; i++) {
          serverIds.add(servers[i].getId());
        }
      }
    }
    return (String[]) serverIds.toArray(new String[serverIds.size()]);
  }
  
  public static IModule getModule(IProject project, String componentName ) 
  {
	IModule[] modules     = ServerUtil.getModules(project);
	IModule   moduleFound = null;
	
	for( int index = 0; index < modules.length; index++ )
	{
	  if( modules[index].getName().equals( componentName ) )
	  {
	    moduleFound = modules[index];
		break;
	  }
	}
	
	return moduleFound;
  }
  
  public static IServer getDefaultExistingServer( IVirtualComponent component )
  {
	IProject           project           = component.getProject();
	IProjectProperties props             = ServerCore.getProjectProperties(project);
	IServer            preferredServer   = props.getDefaultServer();
	
	if( preferredServer == null )
	{
	  IModule   module            = getModule( project, component.getName() );
	  IServer[] configuredServers = ServerUtil.getServersByModule( module, null );
	  
	  if( configuredServers != null && configuredServers.length > 0) 
	  { 
	    preferredServer = configuredServers[0];
	  }
	  else
	  {
	    IServer[] nonConfiguredServers = ServerUtil.getAvailableServersForModule( module, false, null );
		
	    if (nonConfiguredServers != null && nonConfiguredServers.length > 0) 
		{ 
		  preferredServer = nonConfiguredServers[0]; 
		}
	  }
	}
	
    return preferredServer;
  }
  
  /**
   * 
   * @param project
   * @return
   * @deprecated  should be using getDefaultExistingServer( IVirtualComponent )
   */
  public static IServer getDefaultExistingServer(IProject project) {
    String defaultServerName;

    //IServerPreferences serverPreferences = ServerCore.getServerPreferences();
    IProjectProperties props = ServerCore.getProjectProperties(project);
    IServer preferredServer = props.getDefaultServer();
    //IServer preferredServer = serverPreferences.getDeployableServerPreference(ResourceUtils.getModule(project));
    if (preferredServer != null)
      return preferredServer;

    IServer[] configuredServers = ServerUtil.getServersByModule(ResourceUtils.getModule(project), null);
    if (configuredServers != null && configuredServers.length > 0) { return configuredServers[0]; }

    IServer[] nonConfiguredServers = ServerUtil.getAvailableServersForModule(ResourceUtils.getModule(project), false, null);
    if (nonConfiguredServers != null && nonConfiguredServers.length > 0) { return nonConfiguredServers[0]; }
    return null;
  }
  
  
  /*
   * @param moduleType - ad defined in IServerTargetConstants (i.e. EAR_TYPE, WEB_TYPE, etc.)
   * @param j2eeVersion String representation of the int values in J2EEVersionConstants
   * i.e. "12" or "13" or "14"
   * @return String the id of the server target - to be used in project creation operations.
   */
  public static String getServerTargetIdFromFactoryId(String serverFactoryId, String moduleType, String j2eeVersion)
  {
    IServerType serverType = ServerCore.findServerType(serverFactoryId);
    if (serverType == null)
      return null;
    
    String serverRuntimeTypeId = serverType.getRuntimeType().getId();
    
    String stJ2EEVersion = ServerUtils.getServerTargetJ2EEVersion(j2eeVersion);
    List runtimes = ServerTargetHelper.getServerTargets(moduleType, stJ2EEVersion);
    for (int i=0; i<runtimes.size(); i++)
    {
      IRuntime runtime = (IRuntime)runtimes.get(i);
      String thisRuntimeTypeId = runtime.getRuntimeType().getId();
      if (thisRuntimeTypeId.equals(serverRuntimeTypeId))
      {
        return runtime.getId();
      }
    }
    
    return null;    
  }
  
  /*
   * @param serverFactoryId the server's factory id
   * @returns the runtime type id given the server's factory id. Returns a blank String if the no ServerType exists for the given factory id.
   */
  public static String getRuntimeTargetIdFromFactoryId(String serverFactoryId)
  {
    IServerType serverType = ServerCore.findServerType(serverFactoryId);
    if (serverType!=null)
    {
      String serverRuntimeId = serverType.getRuntimeType().getId();
      return serverRuntimeId;
    }
    else
      return "";
  }
  
  /*
   * @param serverFactoryId the server's factory id
   * @returns the server type id given the server's factory id. Returns a blank String if the no ServerType exists for the given factory id.
   */  
  public static String getServerTypeIdFromFactoryId(String serverFactoryId)
  {
  	IServerType serverType = ServerCore.findServerType(serverFactoryId);
  	if (serverType!=null)
  	{
  	  String serverTypeId = serverType.getId();
  	  return serverTypeId;
  	}
  	else
  	  return "";
  }

  /*
   * @param j2eeVersion String representation of the int values in J2EEVersionConstants
   * i.e. "12" or "13" or "14"
   */
  public static boolean isTargetValidForEAR(String runtimeTargetId, String j2eeVersion)
  {
  	if (runtimeTargetId == null)
  		return false;
  	
  	String earModuleType = IServerTargetConstants.EAR_TYPE;
	String stJ2EEVersion = ServerUtils.getServerTargetJ2EEVersion(j2eeVersion);
  	List runtimes = ServerTargetHelper.getServerTargets(earModuleType, stJ2EEVersion);
    for (int i=0; i<runtimes.size(); i++ )
    {
      IRuntime runtime = (IRuntime)runtimes.get(i);
      String thisId = runtime.getRuntimeType().getId();
      if (thisId.equals(runtimeTargetId))
      	return true;
    }
    
    return false;
  }
  /*
   * @param j2eeVersion String representation of the int values in J2EEVersionConstants
   * i.e. "12" or "13" or "14"
   * @param the project type from IServerTargetConstants
   */
  public static boolean isTargetValidForProjectType(String runtimeTargetId, String j2eeVersion, String projectType)
  {
  	if (runtimeTargetId == null)
  		return false;
  	
  	if (projectType==null || projectType.length()==0)
  	  return false;
  	
  	String stJ2EEVersion = ServerUtils.getServerTargetJ2EEVersion(j2eeVersion);
  	List runtimes = ServerTargetHelper.getServerTargets(projectType, stJ2EEVersion);
    for (int i=0; i<runtimes.size(); i++ )
    {
      IRuntime runtime = (IRuntime)runtimes.get(i);
      String thisId = runtime.getRuntimeType().getId();
      if (thisId.equals(runtimeTargetId))
      	return true;
    }
    
    return false;
  }
  
  public static String getServerTargetJ2EEVersion(String j2eeVersion)
  {
    String stJ2EEVersion = null;
    if (j2eeVersion==null || j2eeVersion.length()==0)
      return null;
    
    int j2eeVersionInt = Integer.parseInt(j2eeVersion);
    switch (j2eeVersionInt)
    {
      case (J2EEVersionConstants.J2EE_1_2_ID):
        return IServerTargetConstants.J2EE_12;
      case (J2EEVersionConstants.J2EE_1_3_ID):
        return IServerTargetConstants.J2EE_13;
      case (J2EEVersionConstants.J2EE_1_4_ID):
        return IServerTargetConstants.J2EE_14;
      default:
        return null;
    }
  }
}
