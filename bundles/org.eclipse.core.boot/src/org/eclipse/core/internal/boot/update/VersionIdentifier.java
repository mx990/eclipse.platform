package org.eclipse.core.internal.boot.update;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Vector;
import java.util.StringTokenizer;

/**
 * <p>
 * Version identifier. In its string representation, 
 * it consists of up to 3 integer numbers separated by decimal point.
 * For example, the following are valid version identifiers 
 * (as strings):
 * <ul>
 *   <li><code>0.0.0</code></li>
 *   <li><code>1.0.127564</code></li>
 *   <li><code>3.7.2</code></li>
 *   <li><code>1.9</code> (interpreted as <code>1.9.0</code>)</li>
 *   <li><code>3</code> (interpreted as <code>3.0.0</code>)</li>
 * </ul>
 * </p>
 * <p>
 * The version identifier can be decomposed into a major, minor
 * and service level component. A difference in the major 
 * component is interpreted as an incompatible version change. 
 * A difference in the minor (and not the major) component is 
 * interpreted as a compatible version change. The service
 * level component is interpreted as a cumulative 
 * and compatible service update of the minor version component.
 * </p>
 * <p>
 * Version identifiers can be matched for equality, equivalency,
 * and compatibility.
 * </p>
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 */	
public final class VersionIdentifier {
		
	private	int major = 0;
	private int minor = 0;
	private int service = 0;
	
	private static final String	SEPARATOR = ".";
/**
 * Creates a plug-in version identifier from its components.
 * 
 * @param major major component of the version identifier
 * @param minor minor component of the version identifier
 * @param service service update component of the version identifier
 */
public VersionIdentifier(int major, int minor, int service) {

	if(major<0) major=0;
	if(minor<0) minor=0;
	if(service<0) service=0;
	
	this.major = major;
	this.minor = minor;
	this.service = service;
}
/**
 * Creates a plug-in version identifier from the given string.
 * The string represenation consists of up to 3 integer 
 * numbers separated by decimal point.
 * For example, the following are valid version identifiers 
 * (as strings):
 * <ul>
 *   <li><code>0.0.0</code></li>
 *   <li><code>1.0.127564</code></li>
 *   <li><code>3.7.2</code></li>
 *   <li><code>1.9</code> (interpreted as <code>1.9.0</code>)</li>
 *   <li><code>3</code> (interpreted as <code>3.0.0</code>)</li>
 * </ul>
 * </p>
 * 
 * @param versionId string representation of the version identifier
 */
public VersionIdentifier(String versionId) {

	try{
		if( versionId == null )
			versionId = "0.0.0";
			
		String s = versionId.trim();
	
		StringTokenizer st = new StringTokenizer(s, SEPARATOR);
		Integer token;
		Vector elements = new Vector(3);

		while(st.hasMoreTokens()) {
			token = new Integer((String)st.nextToken());
			elements.addElement(token);
		}

		if (elements.size()>=1) this.major = ((Integer)elements.elementAt(0)).intValue();
		if (elements.size()>=2) this.minor = ((Integer)elements.elementAt(1)).intValue();
		if (elements.size()>=3) this.service = ((Integer)elements.elementAt(2)).intValue();
		
	} catch (Exception e) { // will use default version 0.0.0
		this.major   = 0;
		this.minor   = 0;
		this.service = 0;
	}

}
/**
 * Compare version identifiers for equality. Identifiers are
 * equal if all of their components are equal.
 *
 * @param object an object to compare
 * @return whehter or not the two objects are equal
 */
public boolean equals(Object object) {
	if (!(object instanceof VersionIdentifier))
		return false;
	VersionIdentifier v = (VersionIdentifier) object;
	return v.getMajorComponent() == major && v.getMinorComponent() == minor && v.getServiceComponent() == service;
}
/**
 * Returns the major (incompatible) component of this 
 * version identifier.
 *
 * @return the major version
 */
public int getMajorComponent() {
	return major;
}
/**
 * Returns the minor (compatible) component of this 
 * version identifier.
 *
 * @return the minor version
 */
public int getMinorComponent() {
	return minor;
}
/**
 * Returns the service level component of this 
 * version identifier.
 *
 * @return the service level
 */
public int getServiceComponent() {
	return service;
}
/**
 * Compares two version identifiers for compatibility.
 * <p>
 * A version identifier is considered to be compatible if its major 
 * component equals to the argument major component, and its minor component
 * is greater than or equal to the argument minor component.
 * If the minor components are equal, than the service level of the
 * version identifier must be greater than or equal to the service level
 * of the argument identifier.
 * </p>
 *
 * @param versionId the other version identifier
 * @return <code>true</code> is this version identifier
 *    is compatible with the given version identifier, and
 *    <code>false</code> otherwise
 */
public boolean isCompatibleWith(VersionIdentifier id) {
	if (id == null)
		return false;
	if (major != id.getMajorComponent())
		return false;
	if (minor > id.getMinorComponent())
		return true;
	if (minor < id.getMinorComponent())
		return false;
	if (service >= id.getServiceComponent())
		return true;
	else
		return false;
}
/**
 * Compares two version identifiers for equivalency.
 * <p>
 * Two version identifiers are considered to be equivalent if their major 
 * and minor component equal and are at least at the same service level 
 * as the argument.
 * </p>
 *
 * @param versionId the other version identifier
 * @return <code>true</code> is this version identifier
 *    is equivalent to the given version identifier, and
 *    <code>false</code> otherwise
 */
public boolean isEquivalentTo(VersionIdentifier id) {
	if (id == null)
		return false;
	if (major != id.getMajorComponent())
		return false;
	if (minor != id.getMinorComponent())
		return false;
	if (service >= id.getServiceComponent())
		return true;
	else
		return false;
}
/**
 * Compares two version identifiers for order using multi-decimal
 * comparison. 
 *
 * @param versionId the other version identifier
 * @return <code>true</code> is this version identifier
 *    is greater than the given version identifier, and
 *    <code>false</code> otherwise
 */
public boolean isGreaterThan(VersionIdentifier id) {

	if (id == null) {
		if (major==0 && minor==0 && service==0) return false;
		else return true;
	}

	if (major > id.getMajorComponent()) return true;
	if (major < id.getMajorComponent()) return false;
	if (minor > id.getMinorComponent()) return true;
	if (minor < id.getMinorComponent()) return false;	
	if (service > id.getServiceComponent()) return true;
	else return false;

}
/**
 * Returns the string representation of this version identifier. 
 * The result satisfies
 * <code>vi.equals(new PluginVersionIdentifier(vi.toString()))</code>.
 *
 * @return the string representation of this plug-in version identifier
 */
public String toString() {
	return major+SEPARATOR+minor+SEPARATOR+service;
}
}
