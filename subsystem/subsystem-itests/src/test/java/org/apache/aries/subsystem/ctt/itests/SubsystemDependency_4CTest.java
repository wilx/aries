/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.subsystem.ctt.itests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;

/*
C) Test with pre-installed transitive resources
- Register repository R1
- Using the Root subsystem, install a composite subsystem S1 with the following content bundles (with no import/export policy)
  - Bundle A
  - Bundle B
- Using the subsystem S1, install a composite S2 that imports package x, requires bundle A and required capability y
- Verify the wiring of C, D and E wire to A->x, A, B->y respectively 
- Verify no new bundles are installed into the Root or S1 subsystems
*/

@ExamReactorStrategy(PerMethod.class)
public class SubsystemDependency_4CTest extends SubsystemDependencyTestBase 
{
	private static final String SUBSYSTEM_S1 = "sdt_composite.s1.esa";
	private static final String SUBSYSTEM_S2 = "sdt_composite.s2.esa";
	private Subsystem s1;
	private Subsystem s2;
	
	@Override
	protected void createApplications() throws Exception {
		super.createApplications();
		createSubsystemS1();
		createSubsystemS2();
		registerRepositoryR1();
	}
	
	// doing this within @Before doesn't work :(
	private void startSubsystems() throws Exception
	{ 
		s1 = installSubsystemFromFile(SUBSYSTEM_S1);
		startSubsystem(s1);
		s2 = installSubsystemFromFile(s1, SUBSYSTEM_S2);
		startSubsystem(s2);
	}
	
	private void stopSubsystems() throws Exception
	{
		stopSubsystem(s2);
		stopSubsystem(s1);
		//uninstallSubsystem(s2);
		//uninstallSubsystem(s1);
	}
	
	// Using the subsystem S1, install a composite S2 that 
	//   imports package x, 
	//   requires bundle A 
	//   and required capability y
    // - Verify the wiring of C, D and E wire to A->x, A, B->y respectively 

	@Test
	public void verify() throws Exception
	{
		startSubsystems();
		verifySinglePackageWiring (s2, BUNDLE_C, "x", BUNDLE_A);
		verifyRequireBundleWiring (s2, BUNDLE_D, BUNDLE_A);
		verifyCapabilityWiring (s2, BUNDLE_E, "y", BUNDLE_B);
		stopSubsystems();
	}
	
	@Test
	public void verifyNoUnexpectedBundlesProvisioned() throws Exception 
	{ 
		Bundle[] rootBundlesBefore = bundleContext.getBundles();
		s1 = installSubsystemFromFile(SUBSYSTEM_S1);
		startSubsystem(s1);
		Bundle[] s1BundlesBefore = bundleContext.getBundles();
		s2 = installSubsystemFromFile(s1, SUBSYSTEM_S2);
		startSubsystem(s2);
		Bundle[] rootBundlesAfter = bundleContext.getBundles();
		Bundle[] s1BundlesAfter = bundleContext.getBundles();
		checkNoNewBundles ("rootBundles", rootBundlesBefore, rootBundlesAfter);
		checkNoNewBundles ("s1Bundles", s1BundlesBefore, s1BundlesAfter);
		stopSubsystems();
	}
	
	/*
	 * a composite subsystem S1 with the following content bundles (with no import/export policy)
       - Bundle A
       - Bundle B
	 */
	private static void createSubsystemS1() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_S1);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		String appContent = BUNDLE_A + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_B + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\"";
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(SUBSYSTEM_S1 + ".mf", attributes);
		createSubsystem(SUBSYSTEM_S1);
	}
	
	/*
	 * a composite S2 that 
	 *  imports package x, 
	 *  requires bundle A 
	 *  and required capability y
	 *  
	 * Although the test plan is silent as to the content of S2, I think we have to assume
	 * that it contains bundles C, D and E
	 */
	private static void createSubsystemS2() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, SUBSYSTEM_S2);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		String appContent = BUNDLE_C + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_D + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\""
			+ ", " + BUNDLE_E + ";" + Constants.VERSION_ATTRIBUTE + "=\"[1.0.0,1.0.0]\"";
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		
		attributes.put(Constants.IMPORT_PACKAGE, "x");
		attributes.put(Constants.REQUIRE_BUNDLE, BUNDLE_A);
		attributes.put(Constants.REQUIRE_CAPABILITY, "y"); 
		
		createManifest(SUBSYSTEM_S2 + ".mf", attributes);
		createSubsystem(SUBSYSTEM_S2);
	}

}
