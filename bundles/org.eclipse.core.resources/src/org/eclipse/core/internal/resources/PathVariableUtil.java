/*******************************************************************************
 * Copyright (c) 2008, 2010 Freescale Semiconductor and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Serge Beauchamp (Freescale Semiconductor) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.projectvariables.*;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;

public class PathVariableUtil {
	
	static public String getUniqueVariableName(String variable, IPathVariableManager pathVariableManager, IResource resource) {
		int index = 1;
		variable = getValidVariableName(variable);
		String destVariable = variable;

		while (pathVariableManager.isDefined(destVariable, resource)) {
			destVariable = variable + index;
			index++;
		}
		return destVariable;
	}

	public static String getValidVariableName(String variable) {
		// remove the argument part if the variable is of the form ${VAR-ARG}
		int argumentIndex = variable.indexOf('-');
		if (argumentIndex != -1)
			variable = variable.substring(0, argumentIndex);
		
		variable = variable.trim();
		char first = variable.charAt(0);
		if (!Character.isLetter(first) && first != '_') {
			variable = 'A' + variable;
		}

		StringBuffer builder = new StringBuffer();
		for (int i = 0; i < variable.length(); i++) {
			char c = variable.charAt(i);
			if ((Character.isLetter(c) || Character.isDigit(c) || c == '_') &&
					!Character.isWhitespace(c))
				builder.append(c);
		}
		variable = builder.toString();
		return variable;
	}

	public static IPath convertToPathRelativeMacro(IPathVariableManager pathVariableManager, IPath originalPath, IResource resource, boolean force, String variableHint) throws CoreException {
		return convertToRelative(pathVariableManager, originalPath, resource, force, variableHint, true, true);
	}

	static public IPath convertToRelative(IPathVariableManager pathVariableManager, IPath originalPath, IResource resource, boolean force, String variableHint) throws CoreException {
		return convertToRelative(pathVariableManager, originalPath, resource, force, variableHint, true, false);
	}

	static public URI convertToRelative(IPathVariableManager pathVariableManager, URI originalPath, IResource resource, boolean force, String variableHint) throws CoreException {
		return URIUtil.toURI(convertToRelative(pathVariableManager, URIUtil.toPath(originalPath), resource, force, variableHint, true, false));
	}

	static public URI convertToRelative(IPathVariableManager pathVariableManager, URI originalPath, IResource resource, boolean force, String variableHint, boolean skipWorkspace, boolean generateMacro) throws CoreException {
		return URIUtil.toURI(convertToRelative(pathVariableManager, URIUtil.toPath(originalPath), resource, force, variableHint));
	}
	
	static private IPath convertToRelative(IPathVariableManager pathVariableManager, IPath originalPath, IResource resource, boolean force, String variableHint, boolean skipWorkspace, boolean generateMacro) throws CoreException {
		if (variableHint != null && pathVariableManager.isDefined(variableHint, resource)) {
			IPath value = URIUtil.toPath(pathVariableManager.getValue(variableHint, resource));
			if (value != null)
				return wrapInProperFormat(makeRelativeToVariable(pathVariableManager, originalPath, resource, force, variableHint, generateMacro), generateMacro);
		}
		IPath path = convertToProperCase(originalPath);
		IPath newPath = null;
		int maxMatchLength = -1;
		String[] existingVariables = pathVariableManager.getPathVariableNames(resource);
		for (int i = 0; i < existingVariables.length; i++) {
			String variable = existingVariables[i];
			if (skipWorkspace) {
				// Variables relative to the workspace are not portable, and defeat the purpose of having linked resource locations, 
				// so they should not automatically be created relative to the workspace.
				if (variable.equals(WorkspaceLocationVariableResolver.NAME))
					continue; 
			}
			if (variable.equals(ParentVariableResolver.NAME))
				continue;
			// find closest path to the original path
			IPath value = URIUtil.toPath(pathVariableManager.getValue(variable, resource));
			if (value != null) {
				value = convertToProperCase(URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(value), resource)));
				if (value.isPrefixOf(path)) {
					int matchLength = value.segmentCount();
					if (matchLength > maxMatchLength) {
						maxMatchLength = matchLength;
						newPath = makeRelativeToVariable(pathVariableManager, originalPath, resource, force, variable, generateMacro);
					}
				}
			}
		}
		if (newPath != null)
			return wrapInProperFormat(newPath, generateMacro);

		if (force) {
			int originalSegmentCount = originalPath.segmentCount();
			for (int j = 0; j <= originalSegmentCount; j++) {
				IPath matchingPath = path.removeLastSegments(j);
				int minDifference = Integer.MAX_VALUE;
				for (int k = 0; k < existingVariables.length; k++) {
					String variable = existingVariables[k];
					if (skipWorkspace) {
						if (variable.equals(WorkspaceLocationVariableResolver.NAME))
							continue;
					}
					if (variable.equals(ParentVariableResolver.NAME))
						continue;
					IPath value = URIUtil.toPath(pathVariableManager.getValue(variable, resource));
					if (value != null) {
						value = convertToProperCase(URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(value), resource)));
						if (matchingPath.isPrefixOf(value)) {
							int difference = value.segmentCount() - originalSegmentCount;
							if (difference < minDifference) {
								minDifference = difference;
								newPath = makeRelativeToVariable(pathVariableManager, originalPath, resource, force, variable, generateMacro);
							}
						}
					}
				}
				if (newPath != null)
					return wrapInProperFormat(newPath, generateMacro);
			}
			if (originalSegmentCount == 0) {
				String variable = ProjectLocationVariableResolver.NAME;
				IPath value = URIUtil.toPath(pathVariableManager.getValue(variable, resource));
				value = convertToProperCase(URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(value), resource)));
				if (originalPath.isPrefixOf(value))
					newPath = makeRelativeToVariable(pathVariableManager, originalPath, resource, force, variable, generateMacro);
				if (newPath != null)
					return wrapInProperFormat(newPath, generateMacro);
			}
		}

		if (skipWorkspace)
			return convertToRelative(pathVariableManager, originalPath, resource, force, variableHint, false, generateMacro);
		return originalPath;
	}

	private static IPath wrapInProperFormat(IPath newPath, boolean generateMacro) {
		if (generateMacro)
			newPath = PathVariableUtil.buildVariableMacro(newPath);
		return newPath;
	}

	private static IPath makeRelativeToVariable(IPathVariableManager pathVariableManager, IPath originalPath, IResource resource, boolean force, String variableHint, boolean generateMacro) throws CoreException {
		IPath path = convertToProperCase(originalPath);
		IPath value = URIUtil.toPath(pathVariableManager.getValue(variableHint, resource));
		value = convertToProperCase(URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(value), resource)));
		int valueSegmentCount = value.segmentCount();
		if (value.isPrefixOf(path)) {
			// transform "c:/foo/bar" into "FOO/bar"
			IPath tmp = Path.fromOSString(variableHint);
			for (int j = valueSegmentCount;j < originalPath.segmentCount(); j++) {
				tmp = tmp.append(originalPath.segment(j));
			}
			return tmp;
		} 

		if (force) {
			// transform "c:/foo/bar/other_child/file.txt" into "${PARENT-1-BAR_CHILD}/other_child/file.txt"
			int matchingFirstSegments = path.matchingFirstSegments(value);
			if (matchingFirstSegments >= 0) {
				String newValue = buildParentPathVariable(variableHint, valueSegmentCount - matchingFirstSegments, generateMacro);
				String originalName;
				if (generateMacro) 
					originalName = newValue;
				else {
					originalName = getExistingVariable(newValue, pathVariableManager, resource);
					if (originalName == null) {
						String name;
						if (matchingFirstSegments > 0)
							name = originalPath.segment(matchingFirstSegments - 1);
						else
							name = originalPath.getDevice();
						if (name == null)
							name = "ROOT"; //$NON-NLS-1$
						originalName = getUniqueVariableName(name, pathVariableManager, resource);
						pathVariableManager.setValue(originalName, resource, URIUtil.toURI(Path.fromOSString(newValue)));
					}
				}
				IPath tmp = Path.fromOSString(originalName);
				for (int j = matchingFirstSegments ;j < originalPath.segmentCount(); j++) {
					tmp = tmp.append(originalPath.segment(j));
				}
				return tmp;
			}
		}
		return originalPath;
	}

	private static String getExistingVariable(String newValue, IPathVariableManager pathVariableManager, IResource resource) {
		IPath resolvedNewValue = convertToProperCase(URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(Path.fromOSString(newValue)), resource)));
		String[] existingVariables = pathVariableManager.getPathVariableNames(resource);
		for (int i = 0; i < existingVariables.length; i++) {
			String variable = existingVariables[i];
			URI uri = pathVariableManager.getValue(variable, resource);
			if (uri != null) {
				IPath value = URIUtil.toPath(uri);
				if (value != null) {
					if (value.toOSString().equals(newValue))
						return variable;
					IPath resolvedValue = convertToProperCase(URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(value), resource)));
					if (resolvedValue.equals(resolvedNewValue))
						return variable;
				}
			}
		}
		return null;
	}
	
	static private IPath convertToProperCase(IPath path) {
		if (Platform.getOS().equals(Platform.OS_WIN32))
			return Path.fromPortableString(path.toPortableString().toLowerCase());
		return path;
	}

	static public boolean isParentVariable(String variableString) {
		return variableString.startsWith(ParentVariableResolver.NAME + '-');
	}
	
	// the format is PARENT-COUNT-ARGUMENT
	static public int getParentVariableCount(String variableString) {
		String items[] = variableString.split("-"); //$NON-NLS-1$
		if (items.length == 3) {
			try {
				Integer count = Integer.valueOf(items[1]);
				return count.intValue();
			} catch (NumberFormatException e) {
				// nothing
			}
		}
		return -1;
	}
	
	// the format is PARENT-COUNT-ARGUMENT
	static public String getParentVariableArgument(String variableString) {
		String items[] = variableString.split("-"); //$NON-NLS-1$
		if (items.length == 3) 
			return items[2];
		return null;
	}

	static public String buildParentPathVariable(String variable, int difference, boolean generateMacro) {
		String 	newString = "PARENT-" + difference + "-" + variable;    //$NON-NLS-1$//$NON-NLS-2$

		if (!generateMacro)
			newString = "${" + newString + "}";    //$NON-NLS-1$//$NON-NLS-2$
		return newString;
	}

	public static IPath buildVariableMacro(IPath relativeSrcValue) {
		String variable = relativeSrcValue.segment(0);
		variable = "${" + variable + "}";  //$NON-NLS-1$//$NON-NLS-2$
		return Path.fromOSString(variable).append(relativeSrcValue.removeFirstSegments(1));
	}

	public static String convertFromUserEditableFormatInternal(IPathVariableManager manager, String userFormat, IResource resource) {
		boolean isAbsolute = (userFormat.length() > 0) && (userFormat.charAt(0) == '/' || userFormat.charAt(0) == '\\');
		String components[] = splitPathComponents(userFormat);
		for (int i = 0; i < components.length; i++) {
			if (components[i] == null)
				continue;
			if (isDotDot(components[i])) {
				int parentCount = 1;
				components[i] = null;
				for (int j = i + 1; j < components.length; j++) {
					if (components[j] != null) {
						if (isDotDot(components[j])) {
							parentCount++;
							components[j] = null;
						} else
							break;
					}
				}
				if (i == 0) // this means the value is implicitly relative to the project location
					components[0] = PathVariableUtil.buildParentPathVariable(ProjectLocationVariableResolver.NAME, parentCount, false);
				else {
					for (int j = i - 1; j >= 0; j--) {
						if (parentCount == 0)
							break;
						if (components[j] == null)
							continue;
						String variable = extractVariable(components[j]);
						try {
							if (variable.length() > 0) {
								int indexOfVariable = components[j].indexOf(variable) - "${".length(); //$NON-NLS-1$
								String prefix = components[j].substring(0, indexOfVariable);
								String suffix = components[j].substring(indexOfVariable + "${".length() + variable.length() + "}".length()); //$NON-NLS-1$ //$NON-NLS-2$
								if (suffix.length() != 0) {
									// Create an intermediate variable, since a syntax of "${VAR}foo/../"
									// can't be converted to a "${PARENT-1-VAR}foo" variable.
									// So instead, an intermediate variable "VARFOO" will be created of value 
									// "${VAR}foo", and the string "${PARENT-1-VARFOO}" will be inserted.
									String intermediateVariable = PathVariableUtil.getValidVariableName(variable + suffix);
									IPath intermediateValue = Path.fromPortableString(components[j]);
									int intermediateVariableIndex = 1;
									String originalIntermediateVariableName = intermediateVariable;
									while (manager.isDefined(intermediateVariable, resource)) {
										IPath tmpValue = URIUtil.toPath(manager.getValue(intermediateVariable, resource));
										if (tmpValue.equals(intermediateValue))
											break;
										intermediateVariable = originalIntermediateVariableName + intermediateVariableIndex;
									}
									if (!manager.isDefined(intermediateVariable, resource))
										manager.setValue(intermediateVariable, resource, URIUtil.toURI(intermediateValue));
									variable = intermediateVariable;
									prefix = new String();
								}
								String newVariable = variable;
								if (PathVariableUtil.isParentVariable(variable)) {
									String argument = PathVariableUtil.getParentVariableArgument(variable);
									int count = PathVariableUtil.getParentVariableCount(variable);
									if (argument != null && count != -1)
										newVariable = PathVariableUtil.buildParentPathVariable(argument, count + parentCount, false);
									else
										newVariable = PathVariableUtil.buildParentPathVariable(variable, parentCount, false);
								} else
									newVariable = PathVariableUtil.buildParentPathVariable(variable, parentCount, false);
								components[j] = prefix + newVariable;
								break;
							}
							components[j] = null;
							parentCount--;
						} catch (CoreException e) {
							components[j] = null;
							parentCount--;
						}
					}
				}
			}
		}
		StringBuffer buffer = new StringBuffer();
		if (isAbsolute)
			buffer.append('/');
		for (int i = 0; i < components.length; i++) {
			if (components[i] != null) {
				if (i > 0)
					buffer.append('/');
				buffer.append(components[i]);
			}
		}
		return buffer.toString();
	}

	private static boolean isDotDot(String component) {
		return component.equals(".."); //$NON-NLS-1$
	}

	private static String[] splitPathComponents(String userFormat) {
		ArrayList list = new ArrayList();
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < userFormat.length(); i++) {
			char c = userFormat.charAt(i);
			if (c == '/' || c == '\\') {
				if (buffer.length() > 0)
					list.add(buffer.toString());
				buffer = new StringBuffer();
			} else
				buffer.append(c);
		}
		if (buffer.length() > 0)
			list.add(buffer.toString());
		return (String[]) list.toArray(new String[0]);
	}

	public static String convertToUserEditableFormatInternal(String value) {
		StringBuffer buffer = new StringBuffer();
		String components[] = splitVariablesAndContent(value);
		for (int i = 0; i < components.length; i++) {
			String variable = extractVariable(components[i]);
			if (PathVariableUtil.isParentVariable(variable)) {
				String argument = PathVariableUtil.getParentVariableArgument(variable);
				int count = PathVariableUtil.getParentVariableCount(variable);
				if (argument != null && count != -1) {
					buffer.append(PathVariableUtil.buildVariableMacro(Path.fromOSString(argument)));
					for (int j = 0; j < count; j++) {
						buffer.append("/.."); //$NON-NLS-1$
					}
				} else
					buffer.append(components[i]);
			} else
				buffer.append(components[i]);
		}
		return buffer.toString();
	}
	/*
	 * Splits a value (returned by this.getValue(variable) in an array of
	 * string, where the array is divided between the value content and the
	 * value variables.
	 * 
	 * For example, if the value is "${ECLIPSE_HOME}/plugins", the value
	 * returned will be {"${ECLIPSE_HOME}" "/plugins"}
	 */
	static String[] splitVariablesAndContent(String value) {
		LinkedList result = new LinkedList();
		while (true) {
			// we check if the value contains referenced variables with ${VAR}
			int index = value.indexOf("${"); //$NON-NLS-1$
			if (index != -1) {
				int endIndex = getMatchingBrace(value, index);
				if (index > 0)
					result.add(value.substring(0, index));
				result.add(value.substring(index, endIndex + 1));
				value = value.substring(endIndex + 1);
			} else
				break;
		}
		if (value.length() > 0)
			result.add(value);
		return (String[]) result.toArray(new String[0]);
	}

	/*
	 * Splits a value (returned by this.getValue(variable) in an array of
	 * string of the variables contained in the value.
	 * 
	 * For example, if the value is "${ECLIPSE_HOME}/plugins", the value
	 * returned will be {"ECLIPSE_HOME"}. If the value is 
	 * "${ECLIPSE_HOME}/${FOO}/plugins", the value returned will be 
	 * {"ECLIPSE_HOME", "FOO"}.
	 */
	static String[] splitVariableNames(String value) {
		LinkedList result = new LinkedList();
		while (true) {
			int index = value.indexOf("${"); //$NON-NLS-1$
			if (index != -1) {
				int endIndex = getMatchingBrace(value, index);
				result.add(value.substring(index + 2, endIndex));
				value = value.substring(endIndex + 1);
			} else
				break;
		}
		return (String[]) result.toArray(new String[0]);
	}

	/*
	 * Extracts the variable name from a variable segment.
	 * 
	 * For example, if the value is "${ECLIPSE_HOME}", the value returned will
	 * be "ECLIPSE_HOME". If the segment doesn't contain any variable, the value
	 * returned will be "".
	 */
	static String extractVariable(String segment) {
		int index = segment.indexOf("${"); //$NON-NLS-1$
		if (index != -1) {
			int endIndex = getMatchingBrace(segment, index);
			return segment.substring(index + 2, endIndex);
		}
		return ""; //$NON-NLS-1$
	}

	// getMatchingBrace("${FOO}/something") returns 5
	// getMatchingBrace("${${OTHER}}/something") returns 10
	// getMatchingBrace("${FOO") returns 5
	static int getMatchingBrace(String value, int index) {
		int scope = 0;
		for (int i = index + 1; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '}') {
				if (scope == 0)
					return i;
				scope--;
			}
			if (c == '$') {
				if ((i + 1 < value.length()) && (value.charAt(i + 1) == '{'))
					scope++;
			}
		}
		return value.length();
	}
}
