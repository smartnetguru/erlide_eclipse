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
package org.erlide.core.erlang.internal;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlModelStatus;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlModel;
import org.erlide.core.erlang.IErlModelStatusConstants;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlProject;
import org.erlide.core.erlang.IOpenable;
import org.erlide.core.erlang.IParent;
import org.erlide.core.erlang.ISourceRange;
import org.erlide.core.erlang.ISourceReference;
import org.erlide.core.erlang.util.Assert;
import org.erlide.core.erlang.util.Util;
import org.erlide.runtime.backend.BackendManager;
import org.erlide.runtime.backend.BackendUtil;
import org.erlide.runtime.backend.exceptions.BackendException;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangString;
import com.ericsson.otp.erlang.OtpErlangTuple;

/**
 * Root of Erlang element handle hierarchy.
 * 
 * @see IErlElement
 */
public abstract class ErlElement extends PlatformObject implements IErlElement {

	public static final char EM_ESCAPE = '\\';

	public static final char EM_PROJECT = '=';

	public static final char EM_PACKAGE = '/';

	public static final char EM_MODULE = '{';

	public static final char EM_ATTRIBUTE = '<';

	public static final char EM_FUNCTION = '^';

	public static final char EM_LIBRARY = '~';

	public static final char EM_BEAMFILE = '|';

	public static final char EM_COUNT = '!';

	/**
	 * A count to uniquely identify this element in the case that a duplicate
	 * named element exists. For example, if there are two fields in a
	 * compilation unit with the same name, the occurrence count is used to
	 * distinguish them. The occurrence count starts at 1 (thus the first
	 * occurrence is occurrence 1, not occurrence 0).
	 */
	public int fOccurrenceCount = 1;

	/**
	 * This element's parent, or <code>null</code> if this element does not
	 * have a parent.
	 */
	protected IErlElement fParent;

	/**
	 * This element's name, or an empty <code>String</code> if this element
	 * does not have a name.
	 */
	protected String fName;

	public static final ErlElement[] NO_ELEMENTS = new ErlElement[0];

	protected static final Object NO_INFO = new Object();

	/**
	 * Constructs a handle for a Erlang element with the given parent element
	 * and name.
	 * 
	 * @param parent
	 *            The parent of Erlang element
	 * @param name
	 *            The name of Erlang element
	 * 
	 * @throws IllegalArgumentException
	 *             if the type is not one of the valid Erlang element type
	 *             constants
	 * 
	 */
	protected ErlElement(IErlElement parent, String name) {
		this.fParent = parent;
		this.fName = name;
		this.fChildren = ErlElement.NO_ELEMENTS;
	}

	/**
	 * @see IOpenable
	 */
	public void close() throws ErlModelException {
		// /ErlModelManager.getErlangModelManager().removeInfoAndChildren(this);
	}

	/**
	 * This element is being closed. Do any necessary cleanup.
	 */
	protected abstract void closing(Object info) throws ErlModelException;

	/**
	 * Returns true if this handle represents the same Erlang element as the
	 * given handle. By default, two handles represent the same element if they
	 * are identical or if they represent the same type of element, have equal
	 * names, parents, and occurrence counts.
	 * 
	 * <p>
	 * If a subclass has other requirements for equality, this method must be
	 * overridden.
	 * 
	 * @see Object#equals
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}

		// Erlang model parent is null
		if (this.fParent == null) {
			return super.equals(o);
		}

		// assume instanceof check is done in subclass
		final ErlElement other = (ErlElement) o;
		return this.fOccurrenceCount == other.fOccurrenceCount
				&& this.fName.equals(other.fName)
				&& this.fParent.equals(other.fParent);
	}

	protected void escapeMementoName(StringBuffer buffer, String mementoName) {
		for (int i = 0, length = mementoName.length(); i < length; i++) {
			final char character = mementoName.charAt(i);
			switch (character) {
			case EM_ESCAPE:
			case EM_COUNT:
			case EM_PROJECT:
			case EM_PACKAGE:
			case EM_MODULE:
			case EM_ATTRIBUTE:
			case EM_FUNCTION:
			case EM_LIBRARY:
			case EM_BEAMFILE:
				buffer.append(EM_ESCAPE);
			}
			buffer.append(character);
		}
	}

	/**
	 * @see IErlElement
	 */
	public boolean exists() {
		return true;
	}

	/**
	 * @see IErlElement
	 */
	public IErlElement getAncestor(String ancestorType) {

		IErlElement element = this;
		while (element != null) {
			if (element.getElementType() == ancestorType) {
				return element;
			}
			element = element.getParent();
		}
		return null;
	}

	/**
	 * Returns a collection of (immediate) children of this node of the
	 * specified type.
	 * 
	 * @param type -
	 *            one of the constants defined by IErlElement
	 */
	public ArrayList<? extends IErlElement> getChildrenOfType(String type)
			throws ErlModelException {
		// final IErlElement[] children = getChildren();
		final int size = fChildren.length;
		final ArrayList<IErlElement> list = new ArrayList<IErlElement>(size);
		for (int i = 0; i < size; ++i) {
			final ErlElement elt = (ErlElement) fChildren[i];
			if (elt.getElementType() == type) {
				list.add(elt);
			}
		}
		return list;
	}

	/**
	 * @see IMember
	 */
	public IErlModule getModule() {
		return null;
	}

	/**
	 * @see IErlElement
	 */
	public String getElementName() {
		return this.fName;
	}

	/**
	 * @see IErlElement
	 */
	public IErlModel getModel() {
		IErlElement current = this;
		do {
			if (current instanceof IErlModel) {
				return (IErlModel) current;
			}
		} while ((current = current.getParent()) != null);
		return null;
	}

	/**
	 * @see IErlElement
	 */
	public IErlProject getErlProject() {
		IErlElement current = this;
		do {
			if (current instanceof IErlProject) {
				return (IErlProject) current;
			}
		} while ((current = current.getParent()) != null);
		return null;
	}

	/**
	 * Return the first instance of IOpenable in the parent hierarchy of this
	 * element.
	 * 
	 * <p>
	 * Subclasses that are not IOpenable's must override this method.
	 */
	public IOpenable getOpenableParent() {
		return (IOpenable) this.fParent;
	}

	/**
	 * @see IErlElement
	 */
	public IErlElement getParent() {
		return this.fParent;
	}

	/**
	 * Returns the element that is located at the given source position in this
	 * element. This is a helper method for <code>IErlModule#getElementAt</code>,
	 * and only works on compilation units and types. The position given is
	 * known to be within this element's source range already, and if no finer
	 * grained element is found at the position, this element is returned.
	 */
	protected IErlElement getSourceElementAt(int position)
			throws ErlModelException {
		if (this instanceof ISourceReference) {
			final IErlElement[] children = getChildren();
			for (int i = children.length - 1; i >= 0; i--) {
				final IErlElement aChild = children[i];
				if (aChild instanceof SourceRefElement) {
					final SourceRefElement child = (SourceRefElement) children[i];
					final ISourceRange range = child.getSourceRange();
					final int start = range.getOffset();
					final int end = start + range.getLength();
					if (start <= position && position <= end) {
						if (child instanceof IParent) {
							return child.getSourceElementAt(position);
						} else {
							return child;
						}
					}
				}
			}
		} else {
			// should not happen
			Assert.isTrue(false);
		}
		return this;
	}

	/*
	 * (non-Edoc)
	 * 
	 * @see org.erlide.core.erlang.IErlElement#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		final IResource resource = getResource();
		if (resource == null) {
			class NoResourceSchedulingRule implements ISchedulingRule {

				public IPath fPath;

				public NoResourceSchedulingRule(IPath path) {
					this.fPath = path;
				}

				public boolean contains(ISchedulingRule rule) {
					if (rule instanceof NoResourceSchedulingRule) {
						return this.fPath
								.isPrefixOf(((NoResourceSchedulingRule) rule).fPath);
					} else {
						return false;
					}
				}

				public boolean isConflicting(ISchedulingRule rule) {
					if (rule instanceof NoResourceSchedulingRule) {
						final IPath otherPath = ((NoResourceSchedulingRule) rule).fPath;
						return this.fPath.isPrefixOf(otherPath)
								|| otherPath.isPrefixOf(this.fPath);
					} else {
						return false;
					}
				}
			}
			return new NoResourceSchedulingRule(getResource()
					.getProjectRelativePath());
		} else {
			return resource;
		}
	}

	/**
	 * @see IParent
	 */
	public boolean hasChildren() {
		// if I am not open, return true to avoid opening (case of a Erlang
		// project, a compilation unit or a class file).
		// also see https://bugs.eclipse.org/bugs/show_bug.cgi?id=52474
		final Object elementInfo = ErlangCore.getModelManager().getInfo(this);
		if (elementInfo instanceof ErlElement) {
			return ((ErlElement) elementInfo).getChildren().length > 0;
		} else {
			return true;
		}
	}

	/**
	 * Returns the hash code for this Erlang element. By default, the hash code
	 * for an element is a combination of its name and parent's hash code.
	 * Elements with other requirements must override this method.
	 */
	@Override
	public int hashCode() {
		if (this.fParent == null) {
			return super.hashCode();
		}
		return Util.combineHashCodes(this.fName.hashCode(), this.fParent
				.hashCode());
	}

	/**
	 * Returns true if this element is an ancestor of the given element,
	 * otherwise false.
	 */
	public boolean isAncestorOf(IErlElement e) {
		IErlElement parentElement = e.getParent();
		while (parentElement != null && !parentElement.equals(this)) {
			parentElement = parentElement.getParent();
		}
		return parentElement != null;
	}

	/**
	 * @see IErlElement
	 */
	public boolean isReadOnly() {
		return false;
	}

	/**
	 * Creates and returns and not present exception for this element.
	 */
	protected ErlModelException newNotPresentException() {
		System.out.println("not found: " + this.fName);
		return new ErlModelException(new ErlModelStatus(
				IErlModelStatusConstants.ELEMENT_DOES_NOT_EXIST, this));
	}

	/**
	 */
	public String readableName() {
		return this.getElementName();
	}

	protected String tabString(int tab) {
		final StringBuffer buffer = new StringBuffer();
		for (int i = tab; i > 0; i--) {
			buffer.append("  "); //$NON-NLS-1$
		}
		return buffer.toString();
	}

	/**
	 * Debugging purposes
	 */
	public String toDebugString() {
		final StringBuffer buffer = new StringBuffer();
		this.toStringInfo(0, buffer, NO_INFO);
		return buffer.toString();
	}

	/**
	 * Debugging purposes
	 */
	@Override
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		toString(0, buffer);
		return buffer.toString();
	}

	/**
	 * Debugging purposes
	 */
	protected void toString(int tab, StringBuffer buffer) {
		final Object info = this.toStringInfo(tab, buffer);
		if (tab == 0) {
			this.toStringAncestors(buffer);
		}
		this.toStringChildren(tab, buffer, info);
	}

	/**
	 * Debugging purposes
	 */
	public String toStringWithAncestors() {
		final StringBuffer buffer = new StringBuffer();
		this.toStringInfo(0, buffer, NO_INFO);
		this.toStringAncestors(buffer);
		return buffer.toString();
	}

	/**
	 * Debugging purposes
	 */
	protected void toStringAncestors(StringBuffer buffer) {
		final ErlElement parentElement = (ErlElement) this.getParent();
		if (parentElement != null && parentElement.getParent() != null) {
			buffer.append(" [in "); //$NON-NLS-1$
			parentElement.toStringInfo(0, buffer, NO_INFO);
			parentElement.toStringAncestors(buffer);
			buffer.append("]"); //$NON-NLS-1$
		}
	}

	/**
	 * Debugging purposes
	 */
	protected void toStringChildren(int tab, StringBuffer buffer, Object info) {
		if (info == null || !(info instanceof ErlElement)) {
			return;
		}
		final IErlElement[] achildren = ((ErlElement) info).getChildren();
		for (IErlElement element : achildren) {
			buffer.append("\n"); //$NON-NLS-1$
			((ErlElement) element).toString(tab + 1, buffer);
		}
	}

	/**
	 * Debugging purposes
	 */
	public Object toStringInfo(int tab, StringBuffer buffer) {
		final Object info = ErlangCore.getModelManager().getInfo(this);
		this.toStringInfo(tab, buffer, info);
		return info;
	}

	/**
	 * Debugging purposes
	 */
	protected void toStringInfo(int tab, StringBuffer buffer, Object info) {
		buffer.append(this.tabString(tab));
		toStringName(buffer);
		if (info == null) {
			buffer.append(" (not open)"); //$NON-NLS-1$
		}
	}

	/**
	 * Debugging purposes
	 */
	protected void toStringName(StringBuffer buffer) {
		buffer.append(getElementName());
		if (this.fOccurrenceCount > 1) {
			buffer.append("#"); //$NON-NLS-1$
			buffer.append(this.fOccurrenceCount);
		}
	}

	/**
	 * Collection of handles of immediate children of this object. This is an
	 * empty array if this element has no children.
	 */
	protected IErlElement[] fChildren;

	/**
	 * Is the structure of this element known
	 * 
	 * @see IErlElement#isStructureKnown()
	 */
	protected boolean isStructureKnown = false;

	/**
	 * Shared empty collection used for efficiency.
	 */
	static final IProject[] NO_NON_ERLANG_RESOURCES = new IProject[] {};

	public void addChild(IErlElement child) {
		if (this.fChildren == ErlElement.NO_ELEMENTS) {
			setChildren(new IErlElement[] { child });
		} else {
			if (!includesChild(child)) {
				setChildren(growAndAddToArray(this.fChildren, child));
			}
		}
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (final CloneNotSupportedException e) {
			throw new Error();
		}
	}

	public IErlElement[] getChildren() {
		return this.fChildren;
	}

	/**
	 * Adds the new element to a new array that contains all of the elements of
	 * the old array. Returns the new array.
	 */
	protected IErlElement[] growAndAddToArray(IErlElement[] array,
			IErlElement addition) {
		final IErlElement[] old = array;
		array = new IErlElement[old.length + 1];
		System.arraycopy(old, 0, array, 0, old.length);
		array[old.length] = addition;
		return array;
	}

	/**
	 * Returns <code>true</code> if this child is in my children collection
	 */
	protected boolean includesChild(IErlElement child) {

		for (IErlElement element : this.fChildren) {
			if (element.equals(child)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @see IErlElement#isStructureKnown()
	 */
	public boolean isStructureKnown() {
		return this.isStructureKnown;
	}

	/**
	 * Returns an array with all the same elements as the specified array except
	 * for the element to remove. Assumes that the deletion is contained in the
	 * array.
	 */
	protected IErlElement[] removeAndShrinkArray(IErlElement[] array,
			IErlElement deletion) {
		final IErlElement[] old = array;
		array = new IErlElement[old.length - 1];
		int j = 0;
		for (int i = 0; i < old.length; i++) {
			if (!old[i].equals(deletion)) {
				array[j] = old[i];
			} else {
				System.arraycopy(old, i + 1, array, j, old.length - (i + 1));
				return array;
			}
			j++;
		}
		return array;
	}

	public void removeChild(IErlElement child) {
		if (includesChild(child)) {
			setChildren(removeAndShrinkArray(this.fChildren, child));
		}
	}

	public void setChildren(IErlElement[] children) {
		this.fChildren = children;
	}

	/**
	 * Sets whether the structure of this element known
	 * 
	 * @see IErlElement#isStructureKnown()
	 */
	public void setIsStructureKnown(boolean newIsStructureKnown) {
		this.isStructureKnown = newIsStructureKnown;
	}

	protected String pp(OtpErlangObject e) {
		if (e == null) {
			return "";
		}
		if (e instanceof OtpErlangList) {
			final OtpErlangList ll = (OtpErlangList) e;
			String r = "";
			for (int i = 0; i < ll.arity(); i++) {
				final OtpErlangObject x = ll.elementAt(i);
				r = r + pp(x) + ", ";
			}
			final String rr = r.length() > 2 ? r.substring(0, r.length() - 2)
					: "";
			return "(" + rr + ")";
		} else if (e instanceof OtpErlangTuple) {
			try {
				OtpErlangObject p = BackendUtil.checkRpc(BackendManager
						.getDefault().getIdeBackend().rpc("erlide_pp", "expr",
								e));
				p = BackendUtil.checkRpc(BackendManager.getDefault()
						.getIdeBackend().rpc("lists", "flatten", p));
				return ((OtpErlangString) p).stringValue();
			} catch (final BackendException e1) {
				return "?";
			}
		} else {
			return e.toString();
		}
	}

	protected String pp_1(OtpErlangList e) {
		if (e == null) {
			return "";
		}
		String r = "";
		for (int i = 0; i < e.arity(); i++) {
			final OtpErlangList x = (OtpErlangList) e.elementAt(i);
			r = r + pp_2(x) + "; ";
		}
		final String rr = r.length() > 2 ? r.substring(0, r.length() - 2) : "";
		return rr;
	}

	protected String pp_2(OtpErlangList e) {
		if (e == null) {
			return "";
		}
		String r = "";
		for (int i = 0; i < e.arity(); i++) {
			final OtpErlangObject x = e.elementAt(i);
			r = r + pp(x) + ", ";
		}
		final String rr = r.length() > 2 ? r.substring(0, r.length() - 2) : "";
		return rr;
	}

}
