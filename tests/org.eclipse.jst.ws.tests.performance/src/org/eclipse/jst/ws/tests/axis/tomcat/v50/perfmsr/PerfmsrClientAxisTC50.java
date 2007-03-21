package org.eclipse.jst.ws.tests.axis.tomcat.v50.perfmsr;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jem.util.emf.workbench.ProjectUtilities;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jst.j2ee.internal.J2EEVersionConstants;
import org.eclipse.jst.ws.internal.common.J2EEUtils;
import org.eclipse.jst.ws.internal.common.ResourceUtils;
import org.eclipse.jst.ws.internal.consumption.command.common.CreateModuleCommand;
import org.eclipse.jst.ws.tests.axis.tomcat.v50.WSWizardTomcat50Test;
import org.eclipse.jst.ws.tests.performance.util.PerformanceJUnitUtils;
import org.eclipse.jst.ws.tests.unittest.WSJUnitConstants;
import org.eclipse.jst.ws.tests.util.JUnitUtils;
import org.eclipse.jst.ws.tests.util.ScenarioConstants;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

/**
 * Client performance scenario with Axis and Tomcat v5.0
 */
public class PerfmsrClientAxisTC50 extends WSWizardTomcat50Test {

	private final String WS_RUNTIMEID_AXIS = WSJUnitConstants.WS_RUNTIMEID_AXIS;
  
	private final String CLIENT_PROJECT_NAME = WSJUnitConstants.CLIENT_PROJECT_NAME;
	
	private IFile sourceFile_;

    protected void createProjects() throws Exception{
        IProject webProject = ProjectUtilities.getProject(CLIENT_PROJECT_NAME);
        if (!webProject.exists()){
          createWebModule(CLIENT_PROJECT_NAME, CLIENT_PROJECT_NAME,J2EEVersionConstants.J2EE_1_4_ID);
        }
      }
      
      private void createWebModule(String projectNm, String componentName, int j2eeVersion){

        CreateModuleCommand cmc = new CreateModuleCommand();
        cmc.setJ2eeLevel(new Integer(j2eeVersion).toString());
        cmc.setModuleName(componentName);
        cmc.setModuleType(CreateModuleCommand.WEB);
        cmc.setProjectName(projectNm);
        cmc.setServerFactoryId(SERVERTYPEID_TC50);
        cmc.setServerInstanceId(server_.getId());
        cmc.execute(null, null );
        
      }
      
	/**
   * Sets up the input data;
   * - create project(s),
   * - copy resources to workspace 
	 */
	protected void installInputData() throws Exception {
		
		IProject webProject = ProjectUtilities.getProject(CLIENT_PROJECT_NAME);
        IFolder destFolder = (IFolder)J2EEUtils.getWebContentContainer(webProject);
        JUnitUtils.copyTestData("TDJava",destFolder,env_, null);
		sourceFile_ = destFolder.getFile(new Path("Echo.wsdl"));		
		JUnitUtils.syncBuildProject(webProject,env_, null);
		
	}

  /**
   * Set the persistent server runtime context preferences
   */
	protected void initJ2EEWSRuntimeServerDefaults() throws Exception {
        // Set default preferences for Axis and Tomcat 5.0    
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
	public void testClientAxisTC50() throws Exception
	{	
	  	IStatus status = Status.OK_STATUS;
	  	
		JUnitUtils.enableProxyGeneration(true);
		JUnitUtils.enableOverwrite(true);
		Performance perf= Performance.getDefault();
		PerformanceMeter performanceMeter= perf.createPerformanceMeter(perf.getDefaultScenarioId(this));	    
	    try {
    
	      performanceMeter.start();
	      PerformanceJUnitUtils.launchCreationWizard(ScenarioConstants.WIZARDID_CLIENT,ScenarioConstants.OBJECT_CLASS_ID_IFILE,initialSelection_);
	      performanceMeter.stop();
	      performanceMeter.commit();
	      perf.assertPerformance(performanceMeter);
	    }
	    finally {
	    	if (performanceMeter==null)
	    		performanceMeter.dispose();
	 	}
	    
		if (status.getSeverity() == Status.OK) {
		  verifyOutput();
		} else {
		  throw new Exception(status.getException());
		}

	}
	
  /**
   * Verify the scenario completed succesfully
   * @throws Exception
   */
	private final void verifyOutput() throws Exception {
        IProject webProject = ProjectUtilities.getProject(CLIENT_PROJECT_NAME);
    
        IPath destPath = ResourceUtils.getJavaSourceLocation(webProject);
        IFolder srcFolder = (IFolder)ResourceUtils.findResource(destPath);
    
		//IFolder srcFolder = JUnitUtils.getSourceFolderForWebProject(CLIENT_PROJECT_NAME);
		IFolder folder = srcFolder.getFolder("foo");
		assertTrue(folder.exists());
		assertTrue(folder.members().length > 0);
		
		//TODO Check that the client runs    

	}
	
  /**
   * Remove workspace if necessary
   */
	protected void deleteInputData() throws Exception {

		// Delete the Web project.
		IProject webProject = ProjectUtilities.getProject(CLIENT_PROJECT_NAME);
		webProject.delete(true,true, null);
		
	}

}