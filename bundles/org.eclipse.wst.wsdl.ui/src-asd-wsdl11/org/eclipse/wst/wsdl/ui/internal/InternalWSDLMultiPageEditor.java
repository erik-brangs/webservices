/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.wsdl.ui.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.asd.editor.ASDMultiPageEditor;
import org.eclipse.wst.wsdl.asd.editor.actions.ASDAddMessageAction;
import org.eclipse.wst.wsdl.asd.editor.actions.BaseSelectionAction;
import org.eclipse.wst.wsdl.asd.editor.util.ASDEditPartFactoryHelper;
import org.eclipse.wst.wsdl.asd.editor.util.IOpenExternalEditorHelper;
import org.eclipse.wst.wsdl.asd.facade.IDescription;
import org.eclipse.wst.wsdl.ui.internal.adapters.WSDLBaseAdapter;
import org.eclipse.wst.wsdl.ui.internal.adapters.actions.W11AddPartAction;
import org.eclipse.wst.wsdl.ui.internal.adapters.basic.W11Description;
import org.eclipse.wst.wsdl.ui.internal.edit.W11BindingReferenceEditManager;
import org.eclipse.wst.wsdl.ui.internal.edit.W11InterfaceReferenceEditManager;
import org.eclipse.wst.wsdl.ui.internal.edit.WSDLXSDTypeReferenceEditManager;
import org.eclipse.wst.wsdl.ui.internal.text.WSDLModelAdapter;
import org.eclipse.wst.wsdl.ui.internal.util.ComponentReferenceUtil;
import org.eclipse.wst.wsdl.ui.internal.util.W11OpenExternalEditorHelper;
import org.eclipse.wst.wsdl.ui.internal.util.WSDLAdapterFactoryHelper;
import org.eclipse.wst.wsdl.ui.internal.util.WSDLEditorUtil;
import org.eclipse.wst.wsdl.ui.internal.util.WSDLResourceUtil;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xsd.editor.XSDTypeReferenceEditManager;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class InternalWSDLMultiPageEditor extends ASDMultiPageEditor
{
	ResourceSet resourceSet;
	Resource wsdlResource;
	
	protected WSDLEditorResourceChangeHandler resourceChangeHandler;
	
	protected WSDLModelAdapter modelAdapter;
	protected SourceEditorSelectionListener fSourceEditorSelectionListener;
	protected WSDLSelectionManagerSelectionListener fWSDLSelectionListener;
	
	public IDescription buildModel(IFileEditorInput editorInput)
	{   
		try
		{
			Object obj = null;
			Document document = ((IDOMModel) editor.getModel()).getDocument();
			if (document instanceof INodeNotifier) {
				INodeNotifier notifier = (INodeNotifier) document;
				modelAdapter = (WSDLModelAdapter) notifier.getAdapterFor(WSDLModelAdapter.class);
				if (modelAdapter == null) {
					modelAdapter = new WSDLModelAdapter();
					notifier.addAdapter(modelAdapter);
					obj = modelAdapter.createDefinition(document.getDocumentElement(), document);
				}
				if (obj == null) {
					obj = modelAdapter.createDefinition(document.getDocumentElement(), document);
				}
			}
			
			if (obj instanceof Definition)
			{
				Definition definition = (Definition) obj;
				model = (IDescription) WSDLAdapterFactoryHelper.getInstance().adapt(definition); 
			}
			wsdlResource.setModified(false);
//			}
		}
		catch (StackOverflowError e)
		{
		}
		catch (Exception ex)
		{
		}
		
		return model;
	}
	
	public Object getAdapter(Class type) {
		if (type == ISelectionMapper.class)
		{
			return new WSDLSelectionMapper();
		}
        else if (type == Definition.class && model instanceof Adapter)
        {
          return ((Adapter)model).getTarget(); 
        }
        else if (type == XSDTypeReferenceEditManager.class)
        {
          IEditorInput editorInput = getEditorInput();
          if (editorInput instanceof IFileEditorInput)
          {
            IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
            return new WSDLXSDTypeReferenceEditManager(fileEditorInput.getFile(), null);
          }
        }
        else if (type == W11BindingReferenceEditManager.class) {
            IEditorInput editorInput = getEditorInput();
            if (editorInput instanceof IFileEditorInput)
            {
            	IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
            	return new W11BindingReferenceEditManager((W11Description) getModel(), fileEditorInput.getFile());
            }
        }
        else if (type == W11InterfaceReferenceEditManager.class) {
            IEditorInput editorInput = getEditorInput();
            if (editorInput instanceof IFileEditorInput)
            {
            	IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
            	return new W11InterfaceReferenceEditManager((W11Description) getModel(), fileEditorInput.getFile());
            }        	
        }
		
		return super.getAdapter(type);
	}
	
	/**
	 * Listener on SSE's source editor's selections that converts DOM
	 * selections into xsd selections and notifies WSDL selection manager
	 */
	private class SourceEditorSelectionListener implements ISelectionChangedListener {
		/**
		 * Determines WSDL node based on object (DOM node)
		 * 
		 * @param object
		 * @return
		 */
		private Object getWSDLNode(Object object) {
			// get the element node
			Element element = null;
			if (object instanceof Node) {
				Node node = (Node) object;
				if (node != null) {
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						element = (Element) node;
					}
					else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
						element = ((Attr) node).getOwnerElement();
					}
				}
			}
			Object o = element;
			if (element != null) {
				Definition def = (Definition) ((W11Description) model).getTarget();
				Object modelObject = WSDLEditorUtil.getInstance().findModelObjectForElement(def, element);
				if (modelObject != null) {
					o = WSDLAdapterFactoryHelper.getInstance().adapt((Notifier) modelObject);
				}
			}
			return o;
		}
		
		public void selectionChanged(SelectionChangedEvent event) {
			
			if (getActivePage() == 0)
//				if (getSelectionManager().getEnableNotify() && getActivePage() == 1)
			{
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection)
				{
					List selections = new ArrayList();
					for (Iterator i = ((IStructuredSelection) selection).iterator(); i.hasNext();)
					{
						Object domNode = i.next();
						Object node = getWSDLNode(domNode);
						if (node != null)
						{
							selections.add(node);
						}
					}
					
					if (!selections.isEmpty())
					{
						StructuredSelection wsdlSelection = new StructuredSelection(selections);
						getSelectionManager().setSelection(wsdlSelection, editor.getSelectionProvider());
					}
				}
			}
		}
	}
	
	/**
	 * Listener on WSDL's selection manager's selections that converts WSDL
	 * selections into DOM selections and notifies SSE's selection provider
	 */
	private class WSDLSelectionManagerSelectionListener implements ISelectionChangedListener {
		/**
		 * Determines DOM node based on object (wsdl node)
		 * 
		 * @param object
		 * @return
		 */
		private Object getObjectForOtherModel(Object object) {
			Node node = null;
			
			if (object instanceof Node) {
				node = (Node) object;
			}
			else {
				node = WSDLEditorUtil.getInstance().getNodeForObject(object);
			}
			
			// the text editor can only accept sed nodes!
			//
			if (!(node instanceof IDOMNode)) {
				node = null;
			}
			return node;
		}
		
		public void selectionChanged(SelectionChangedEvent event) {
			// do not fire selection in source editor if selection event came
			// from source editor
			if (event.getSource() != editor.getSelectionProvider()) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					List otherModelObjectList = new ArrayList();
					for (Iterator i = ((IStructuredSelection) selection).iterator(); i.hasNext();) {
						Object facadeObject = i.next();
						if (facadeObject instanceof WSDLBaseAdapter) {
							Object wsdlObject = ((WSDLBaseAdapter) facadeObject).getTarget();
							Object otherModelObject = getObjectForOtherModel(wsdlObject);
							if (otherModelObject != null) {
								otherModelObjectList.add(otherModelObject);
							}
						}
					}
					if (!otherModelObjectList.isEmpty()) {
						StructuredSelection nodeSelection = new StructuredSelection(otherModelObjectList);
						editor.getSelectionProvider().setSelection(nodeSelection);
					}
				}
			}
		}
	}
	
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		setEditPartFactory(ASDEditPartFactoryHelper.getInstance().getEditPartFactory());
	}
	
	protected void createPages() {
		super.createPages();
		
		if (resourceChangeHandler == null) {
			resourceChangeHandler = new WSDLEditorResourceChangeHandler(this);
			resourceChangeHandler.attach();
		}
		
		fSourceEditorSelectionListener = new SourceEditorSelectionListener();
		ISelectionProvider provider = editor.getSelectionProvider();
		if (provider instanceof IPostSelectionProvider) {
			((IPostSelectionProvider) provider).addPostSelectionChangedListener(fSourceEditorSelectionListener);
		}
		else {
			provider.addSelectionChangedListener(fSourceEditorSelectionListener);
		}
		
		fWSDLSelectionListener = new WSDLSelectionManagerSelectionListener();
		getSelectionManager().addSelectionChangedListener(fWSDLSelectionListener);
	}
	
	public void dispose() {
		if (resourceChangeHandler != null) {
			resourceChangeHandler.dispose();
		}
	}
	
	public void reloadDependencies() {
		try {
			Definition definition = (Definition) ((W11Description) getModel()).getTarget();
			if (definition != null) {
				WSDLResourceUtil.reloadDirectives(definition);
				ComponentReferenceUtil.updateBindingReferences(definition);
				ComponentReferenceUtil.updatePortTypeReferences(definition);
				ComponentReferenceUtil.updateMessageReferences(definition);
				ComponentReferenceUtil.updateSchemaReferences(definition);
				// the line below simply causes a notification in order to
				// update our
				// views
				//
				definition.setDocumentationElement(definition.getDocumentationElement());
			}
		}
		finally {
		}
	}
	
	protected void createActions() {
		super.createActions();
	    ActionRegistry registry = getActionRegistry();

	    BaseSelectionAction action = new ASDAddMessageAction(this);
	    action.setSelectionProvider(getSelectionManager());
	    registry.registerAction(action);

	    action = new W11AddPartAction(this);
	    action.setSelectionProvider(getSelectionManager());
	    registry.registerAction(action);	    
	  }
	
  public IOpenExternalEditorHelper getOpenExternalEditorHelper() {
    return new W11OpenExternalEditorHelper(((IFileEditorInput) getEditorInput()).getFile());
  }
}
