/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.*;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.SynchronizationResourceMappingContext;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.eclipse.ui.navigator.IExtensionStateModel;

/**
 * Abstract team aware content provider that delegates to another content provider
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public abstract class SynchronizationContentProvider implements ICommonContentProvider, IDiffChangeListener, IPropertyChangeListener {

	private ISynchronizationScope scope;
	private ISynchronizationContext context;
	private Viewer viewer;
	private IExtensionStateModel stateModel;
	private boolean empty;
	
	/* public */ boolean hasChildren(TreePath path) {
		Object element = path.getLastSegment();
		return internalHasChildren(element);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreePathContentProvider#getChildren(org.eclipse.jface.viewers.TreePath)
	 */
	/* public*/ Object[] getChildren(TreePath parentPath) {
		Object parent = parentPath.getLastSegment();
		if (parentPath.getSegmentCount() == 1)
			return getElements(parent);
		return internalGetChildren(parent, false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parent) {
		return internalGetChildren(parent, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object parent) {
		return internalGetChildren(parent, true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof ModelProvider)
			return null;
		if (element == getModelRoot())
			return null;
		Object parent = getDelegateContentProvider().getParent(element);
		if (parent == getModelRoot())
			return getModelProvider();
		return parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return internalHasChildren(element);
	}
	
	private Object[] internalGetChildren(Object parent, boolean isElement) {
		if (parent instanceof ISynchronizationScope) {
			// If the root is a scope, we want to include all models in the scope
			ISynchronizationScope rms = (ISynchronizationScope) parent;
			if (rms.getMappings(getModelProviderId()).length > 0) {
				empty = false;
				return new Object[] { getModelProvider() };
			}
			empty = true;
			return new Object[0];
		} else if (parent instanceof ISynchronizationContext) {
			// If the root is a context, we want to filter by the context
			ISynchronizationContext sc = (ISynchronizationContext) parent;
			if (sc.getScope().getMappings(getModelProviderId()).length > 0) {
				Object root = getModelRoot();
				boolean initialized = isInitialized(context);
				if (!initialized || getChildrenInContext(sc, root, getDelegateChildren(root, isElement)).length > 0) {
					if (!initialized)
						requestInitialization(context);
					empty = false;
					return new Object[] { getModelProvider() };
				}
			}
			empty = true;
			return new Object[0];
		}
		if (parent == getModelProvider()) {
			if (context != null && !isInitialized(context)) {
				return new Object[0];
			}
			parent = getModelRoot();
		}
		Object[] delegateChildren = getDelegateChildren(parent, isElement);
		ISynchronizationContext sc = getContext();
		if (context == null) {
			ISynchronizationScope scope = getScope();
			if (scope == null) {
				return delegateChildren;
			} else {
				return getChildrenInScope(scope, parent, delegateChildren);
			}
		} else {
			return getChildrenInContext(sc, parent, delegateChildren);
		}
	}

	/**
	 * Return whether the content provider has been initialized and is ready to
	 * provide content in the given context. By default, <code>true</code> is returned. Subclasses
	 * that need to perform extra processing to prepare should override this method and 
	 * also override {@link #requestInitialization(ISynchronizationContext)}.
	 * 
	 * @param context the context
	 * @return whether the content provider has been initialized and is ready to
	 * provide content in he given context.
	 */
	protected boolean isInitialized(ISynchronizationContext context) {
		return true;
	}
	
	/**
	 * Subclasses that need to perform extra processing to prepare their model
	 * to be displayed by this content provider should override this method and
	 * launch a background task to prepare what is required to display their
	 * model for the given context. An appropriate viewer refresh on the model
	 * provider should be issued when the model is prepared.
	 * 
	 * @param context
	 *            the context
	 * @return whether the content provider has been initialized and is ready to
	 *         provide content in he given context.
	 */
	protected void requestInitialization(ISynchronizationContext context) {
		// Do nothing by default
	}

	/**
	 * Return the children for the given element from the
	 * delegate content provider.
	 * @param parent the parent element
	 * @return the children for the given element from the
	 * delegate content provider
	 */
	protected Object[] getDelegateChildren(Object parent) {
		return getDelegateContentProvider().getChildren(parent);
	}

	private Object[] getDelegateChildren(Object parent, boolean isElement) {
		if (isElement)
			return getDelegateContentProvider().getElements(parent);
		return getDelegateChildren(parent);
	}

	private boolean internalHasChildren(Object element) {
		//TODO: What about the context and scope
		if (element instanceof ModelProvider) {
			element = getModelRoot();
		}
		if (getDelegateContentProvider().hasChildren(element)) {
			ISynchronizationContext sc = getContext();
			if (context == null) {
				ISynchronizationScope scope = getScope();
				if (scope == null) {
					return true;
				} else {
					return hasChildrenInScope(scope, element);
				}
			} else {
				return hasChildrenInContext(sc, element);
			}
		}
		return false;
	}

	/**
	 * Return whether the given element has children in the given scope.
	 * By default, true is returned if the given element contains any elements
	 * in the scope or if any of the elements in the scope contain the given 
	 * element and the delegate provider returns children for the element.
	 * The {@link ResourceMapping#contains(ResourceMapping)} is used to test
	 * for containment.
	 * Subclasses may override to provide a more efficient implementation.
	 * @param scope the scope
	 * @param element the element
	 * @return whether the given element has children in the given scope
	 */
	protected boolean hasChildrenInScope(ISynchronizationScope scope, Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			ResourceMapping[] mappings = scope.getMappings(mapping.getModelProviderId());
			for (int i = 0; i < mappings.length; i++) {
				ResourceMapping sm = mappings[i];
				if (mapping.contains(sm)) {
					return true;
				}
				if (sm.contains(mapping)) {
					return getDelegateChildren(element).length > 0;
				}
			}
		}
		return false;
	}

	/**
	 * Return whether the given element has children in the given
	 * context. The children may or may not exist locally.
	 * By default, this method returns true if the traversals for
	 * the element contain any diffs. This could result in false 
	 * positives. Subclasses can override to provide a more
	 * efficient or precise answer.
	 * @param element a model element.
	 * @return whether the given element has children in the given context
	 */
	protected boolean hasChildrenInContext(ISynchronizationContext context, Object element) {
		ResourceTraversal[] traversals = getTraversals(context, element);
		if (traversals == null)
			return true;
		return context.getDiffTree().getDiffs(traversals).length > 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		stateModel.removePropertyChangeListener(this);
		if (context != null)
			context.getDiffTree().removeDiffChangeListener(this);
		ISynchronizePageConfiguration configuration = getConfiguration();
		if (configuration != null)
			configuration.removePropertyChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		getDelegateContentProvider().inputChanged(viewer, oldInput, newInput);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.internal.extensions.ICommonContentProvider#init(org.eclipse.ui.navigator.IExtensionStateModel, org.eclipse.ui.IMemento)
	 */
	public void init(IExtensionStateModel aStateModel, IMemento aMemento) {
		stateModel = aStateModel;
		stateModel.addPropertyChangeListener(this);
		ISynchronizePageConfiguration configuration = getConfiguration();
		if (configuration != null)
			configuration.addPropertyChangeListener(this);
		scope = (ISynchronizationScope)aStateModel.getProperty(ISynchronizationConstants.P_RESOURCE_MAPPING_SCOPE);
		context = (ISynchronizationContext)aStateModel.getProperty(ISynchronizationConstants.P_SYNCHRONIZATION_CONTEXT);
		ITreeContentProvider provider = getDelegateContentProvider();
		if (provider instanceof ICommonContentProvider) {
			((ICommonContentProvider) provider).init(aStateModel, aMemento);	
		}
		if (context != null)
			context.getDiffTree().addDiffChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		// TODO: this could happen at the root as well
		if (event.getProperty().equals(ISynchronizePageConfiguration.P_MODE)) {
			refresh();
		}
	}
	
	/**
	 * Return whether elements with the given synchronization kind (as define in
	 * the {@link SyncInfo} class) should be included in the contents. This
	 * method is invoked by the {@link #getChildrenInContext(ISynchronizationContext, Object, Object[])}
	 * method to filter the list of children returned when {@link #getChildren(Object) }
	 * is called. It accessing the <code>ISynchronizePageConfiguration.P_MODE</code>
	 * property on the state model provided by the view to determine what kinds
	 * should be included.
	 * 
	 * @param direction the synchronization kind as described in the {@link SyncInfo}
	 *            class
	 * @return whether elements with the given synchronization kind should be
	 *         included in the contents
	 */
	protected boolean includeDirection(int direction) {
		int mode = getConfiguration().getMode();
		switch (mode) {
		case ISynchronizePageConfiguration.BOTH_MODE:
			return true;
		case ISynchronizePageConfiguration.CONFLICTING_MODE:
			return direction == IThreeWayDiff.CONFLICTING;
		case ISynchronizePageConfiguration.INCOMING_MODE:
			return direction == IThreeWayDiff.CONFLICTING || direction == IThreeWayDiff.INCOMING;
		case ISynchronizePageConfiguration.OUTGOING_MODE:
			return direction == IThreeWayDiff.CONFLICTING || direction == IThreeWayDiff.OUTGOING;
		default:
			break;
		}
		return true;
	}
	
	/**
	 * Return the synchronization context associated with the view to which
	 * this content provider applies. A <code>null</code> is returned if
	 * no context is available.
	 * @return the synchronization context or <code>null</code>
	 * @deprecated is now provided in method calls
	 */
	protected ISynchronizationContext getContext() {
		return context;
	}

	/**
	 * Return the resource mapping scope associated with the view to which
	 * this content provider applies. A <code>null</code> is returned if
	 * no scope is available.
	 * @return the resource mapping scope or <code>null</code>
	 * @deprecated is now provided in method calls
	 */
	protected ISynchronizationScope getScope() {
		return scope;
	}
	
	/**
	 * Return the synchronization page configuration associated with the view to which
	 * this content provider applies. A <code>null</code> is returned if
	 * no configuration is available.
	 * @return the synchronization page configuration or <code>null</code>
	 */
	protected ISynchronizePageConfiguration getConfiguration() {
		return (ISynchronizePageConfiguration)stateModel.getProperty(ISynchronizationConstants.P_SYNCHRONIZATION_PAGE_CONFIGURATION);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#restoreState(org.eclipse.ui.IMemento)
	 */
	public void restoreState(IMemento aMemento) {
		ITreeContentProvider provider = getDelegateContentProvider();
		if (provider instanceof ICommonContentProvider) {
			((ICommonContentProvider) provider).restoreState(aMemento);	
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#saveState(org.eclipse.ui.IMemento)
	 */
	public void saveState(IMemento aMemento) {
		ITreeContentProvider provider = getDelegateContentProvider();
		if (provider instanceof ICommonContentProvider) {
			((ICommonContentProvider) provider).saveState(aMemento);	
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.delta.ISyncDeltaChangeListener#syncDeltaTreeChanged(org.eclipse.team.core.delta.ISyncDeltaChangeEvent, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void diffChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		refresh();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.diff.IDiffChangeListener#propertyChanged(int, org.eclipse.core.runtime.IPath[])
	 */
	public void propertyChanged(int property, IPath[] paths) {
		// Property changes only effect labels
	}

	/**
	 * Refresh the subtree associated with this model.
	 */
	protected void refresh() {
		Utils.syncExec(new Runnable() {
			public void run() {
				TreeViewer treeViewer = ((TreeViewer)getViewer());
				// TODO: Need to know if the model root is present in order to refresh properly
				if (empty)
					treeViewer.refresh();
				else
					treeViewer.refresh(getModelProvider());
			}
		
		}, getViewer().getControl());
	}

	/**
	 * Return the model content provider that the team aware content
	 * provider delegates to.
	 * @return the model content provider
	 */
	protected abstract ITreeContentProvider getDelegateContentProvider();
	
	/**
	 * Return the model provider for this content provider.
	 * @return the model provider for this content provider
	 */
	protected final ModelProvider getModelProvider() {
		try {
			return ModelProvider.getModelProviderDescriptor(getModelProviderId()).getModelProvider();
		} catch (CoreException e) {
			// TODO: this is a bit harsh. can we do something less destructive
			throw new IllegalStateException();
		}
	}
	
	/**
	 * Return the id of model provider for this content provider.
	 * @return the model provider for this content provider
	 */
	protected abstract String getModelProviderId();
	
	/**
	 * Return the object that acts as the model root. It is used when getting the children
	 * for a model provider.
	 * @return the object that acts as the model root
	 */
	protected abstract Object getModelRoot();

	/**
	 * Return the viewer to which the content provider is associated.
	 * @return the viewer to which the content provider is associated
	 */
	protected final Viewer getViewer() {
		return viewer;
	}
	
	/**
	 * Return the subset of the given children that are in the
	 * given scope or are parents of elements that are in scope.
	 * @param scope the scope
	 * @param parent the parent of the given children
	 * @param children all the children of the parent that are in scope.
	 * @return the subset of the given children that are in the
	 * scope of the content provider
	 */
	protected Object[] getChildrenInScope(ISynchronizationScope scope, Object parent, Object[] children) {
		List result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			Object object = children[i];
			if (object != null && isInScope(scope, parent, object)) {
				result.add(object);
			}
		}
		return result.toArray(new Object[result.size()]);
	}
	
	/**
	 * Return the subset of children that are of interest from the given context.
	 * By default, this method returns those children whose traversals contain
	 * a diff in the context. However, it does not include those model elements
	 * that do not exist locally but are within the context (e.g. locally deleted
	 * elements and remotely added elements). Subclasses must override to include
	 * these.
	 * @param context the context
	 * @param parent the parent of the children
	 * @param children the children
	 * @return the subset of children that are of interest from the given context
	 */
	protected Object[] getChildrenInContext(ISynchronizationContext context, Object parent, Object[] children) {
		if (children.length != 0)
			children = getChildrenInScope(context.getScope(), parent, children);
		if (children.length == 0)
			return children;
		return internalGetChildren(context, parent, children);
	}

	private Object[] internalGetChildren(ISynchronizationContext context, Object parent, Object[] children) {
		List result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			Object object = children[i];
			ResourceTraversal[] traversals = getTraversals(context, object);
			IDiff[] deltas = context.getDiffTree().getDiffs(traversals);
			if (deltas.length > 0) {
				boolean include = false;
				for (int j = 0; j < deltas.length; j++) {
					IDiff delta = deltas[j];
					if (delta instanceof IThreeWayDiff) {
						IThreeWayDiff twd = (IThreeWayDiff) delta;
						if (includeDirection(twd.getDirection())) {
							include = true;
							break;
						}
					}
				}
				if (include)
					result.add(object);
			}
		}
		// TODO: may need to get phantoms as well
		return result.toArray(new Object[result.size()]);
	}

	/**
	 * Return the traversals for the given object in the given context. This 
	 * method must not be long running. If a long running calculation is required
	 * to calculate the traversals, an empty traversal should be returned and the
	 * content provider should initiate a background task to calculate the 
	 * required traversals and update the vew according when the task completes.
	 * @param context the synchronization context
	 * @param object the object
	 * @return the traversals for the given object in the given context
	 */
	protected abstract ResourceTraversal[] getTraversals(ISynchronizationContext context, Object object);
	
	/*
	 * Return the traversals for the given model object. By default, the scope
	 * is checked in case the traversals have already been calculated. If they have not,
	 * the provided context is wrapped in a {@link SynchronizationResourceMappingContext}
	 * and passed to the {@link ResourceMapping#getTraversals(ResourceMappingContext, IProgressMonitor)}
	 * method of the mapping for the object. If a subclass can calculate the traversals quickly
	 * (i.e. without a progress monitor), they can override {@link #getTraversals(ISynchronizationContext, Object)}
	 * 
	 * @param context the synchronization context
	 * @param object the model object
	 * @param monitor a progress monitor
	 * @return the traversals for the given object in the scope of this content provider
	 */
	private ResourceTraversal[] getTraversals(ISynchronizationContext context, Object object, IProgressMonitor monitor) {
		ResourceMapping mapping = Utils.getResourceMapping(object);
		if (mapping != null) {
			ResourceTraversal[] traversals = context.getScope().getTraversals(mapping);
			if (traversals != null)
				return traversals;
			try {
				return mapping.getTraversals(new SynchronizationResourceMappingContext(context), monitor);
			} catch (CoreException e) {
				handleException(e);
			}
		}
		return new ResourceTraversal[0];
	}

	/**
	 * Handle the given exception that occurred while calculating the
	 * children for an element.
	 * @param e the exception
	 */
	protected void handleException(CoreException e) {
		TeamPlugin.log(e);
	}

	/**
	 * Return whether the given object is within the scope of this
	 * content provider. The object is in scope if it is part of
	 * a resource mapping in the scope or is the parent of resources
	 * covered by one or more resource mappings in the scope.
	 * By default, this compares the mapping of the given element
	 * with those in the scope using the {@link ResourceMapping#contains(ResourceMapping)}
	 * method to determine if the element is in the scope. Subclasses may
	 * override to provide a more efficient means of doing the check.
	 * @param scope the scope
	 * @param parent the parent of the object
	 * @param element the object
	 * @return whether the given object is within the scope of this
	 * content provider
	 */
	protected boolean isInScope(IResourceMappingScope scope, Object parent, Object element) {
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping != null) {
			ResourceMapping[] mappings = ((ISynchronizationScope)scope).getMappings(mapping.getModelProviderId());
			for (int i = 0; i < mappings.length; i++) {
				ResourceMapping sm = mappings[i];
				if (mapping.contains(sm)) {
					return true;
				}
				if (sm.contains(mapping)) {
					return true;
				}
			}
		}
		return false;
	}
}
