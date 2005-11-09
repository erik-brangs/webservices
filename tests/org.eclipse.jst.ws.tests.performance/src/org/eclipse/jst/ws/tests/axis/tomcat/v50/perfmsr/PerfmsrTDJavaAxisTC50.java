package org.eclipse.jst.ws.tests.axis.tomcat.v50.perfmsr;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jem.util.emf.workbench.ProjectUtilities;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jst.ws.internal.common.J2EEUtils;
import org.eclipse.jst.ws.tests.axis.tomcat.v50.WSWizardTomcat50Test;
import org.eclipse.jst.ws.tests.performance.util.PerformanceJUnitUtils;
import org.eclipse.jst.ws.tests.unittest.WSJUnitConstants;
import org.eclipse.jst.ws.tests.util.JUnitUtils;
import org.eclipse.jst.ws.tests.util.ScenarioConstants;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

/**
 * Top down performance scenario with Axis and Tomcat v5.0
 */
public class PerfmsrTDJavaAxisTC50 extends WSWizardTomcat50Test {

  private final String WS_RUNTIMEID_AXIS = WSJUnitConstants.WS_RUNTIMEID_AXIS;
  
  private final String PROJECT_NAME = WSJUnitConstants.TD_PROJECT_NAME;
  
  /*module name to be removed later */
  private final String WEB_MODULE_NAME = "TestTDWebModule"; 
  
  private IFile sourceFile_;
	

  /**
   * Sets up the input data;
   * - create project(s),
   * - copy resources to workspace 
   */  
	protected void installInputData() throws Exception {

		/*
		// Create an associated Web project (TestWeb) targetted to Tomcat 5.0
		IStatus s = JUnitUtils.createWebModule(PROJECT_NAME, WEB_MODULE_NAME, SERVERTYPEID_TC50, String.valueOf(J2EEVersionConstants.J2EE_1_4_ID), env_, null);
		if (s.getSeverity() != Status.OK)
			throw new Exception(s.getException());
    
		IProject webProject = ProjectUtilities.getProject(PROJECT_NAME);
		assertTrue(webProject.exists());

		IFolder destFolder = (IFolder)J2EEUtils.getWebContentContainer(webProject);		
		JUnitUtils.copyTestData("TDJava",destFolder,env_, null);
		sourceFile_ = destFolder.getFile(new Path("Echo.wsdl"));
		assertTrue(sourceFile_.exists());
		*/
		
		IProject webProject = ProjectUtilities.getProject(PROJECT_NAME);		
		IFolder destFolder = (IFolder)J2EEUtils.getWebContentContainer(webProject);
		sourceFile_ = destFolder.getFile(new Path("Echo.wsdl"));
		JUnitUtils.syncBuildProject(webProject,env_, null);
	}

  /**
   * Set the persistent server runtime context preferences
   */  
	protected void initJ2EEWSRuntimeServerDefaults() throws Exception {
		// Set default preferences for Axis and Tomcat v5.0 server
		JUnitUtils.setWSRuntimeServer(WS_RUNTIMEID_AXIS, SERVERTYPEID_TC50);		
	}

  /**
   * Set the initial selection
   */  
	protected void initInitialSelection() throws Exception {
		initialSelection_ = new StructuredSelection(sourceFile_);

	}

  /**
   * Launches the pop-up command to initiate the scenario
   * @throws Exception
   */  
	public void testTDJavaAxisTC50() throws Exception {
	  
	  IStatus status = Status.OK_STATUS;
		Performance perf= Performance.getDefault();
		PerformanceMeter performanceMeter= perf.createPerformanceMeter(perf.getDefaultScenarioId(this));	    
	    try {
    
	      performanceMeter.start();
	      status = PerformanceJUnitUtils.launchCreationWizard(ScenarioConstants.WIZARDID_TOP_DOWN,ScenarioConstants.OBJECT_CLASS_ID_IFILE,initialSelection_);
	      performanceMeter.stop();
	      performanceMeter.commit();
	      perf.assertPerformance(performanceMeter);
	    }
	    finally {
			performanceMeter.dispose();
	 	}
		if (status.getSeverity() == Status.OK)
		  verifyOutput();
		else
		  throw new Exception(status.getException());

	}

  /**
   * Verify the scenario completed successfully
   * @throws Exception
   */
	private final void verifyOutput() throws Exception
	{
        IProject webProject = ProjectUtilities.getProject(PROJECT_NAME);    
        IFolder webContentFolder = (IFolder)J2EEUtils.getWebContentContainer(webProject);    
    
		
		IFolder wsdlFolder = webContentFolder.getFolder("wsdl");
		assertTrue(wsdlFolder.exists());
		assertTrue(wsdlFolder.members().length > 0);

        //TODO Verify that wsdd contains this Web service
        //TODO Verify that the service can be invoked by a client
	}
	
  /**
   * Clear workspace if required
   */
	protected void deleteInputData() throws Exception {
		// Delete the Web project.
		IProject webProject = ProjectUtilities.getProject(PROJECT_NAME);
		webProject.delete(true,true,null);
		
	}

}
