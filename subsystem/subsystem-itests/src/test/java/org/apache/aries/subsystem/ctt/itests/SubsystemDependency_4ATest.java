package org.apache.aries.subsystem.ctt.itests;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
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
/*
 * First block: section 4A
 * 
A) Test a transitively closed subsystem deploys no transitive resources
 - Register repository R1
 - Using the Root subsystem, install a scoped subsystem with the following content bundles and no local repository
   - Bundle A
   - Bundle B
   - Bundle C
   - Bundle D
   - Bundle E
 - Verify the wiring of C, D and E wire to A->x, A, B->y respectively
 - Verify no new bundles are installed into the Root subsystem (particularly bundles F and G)
 
*/

public class SubsystemDependency_4ATest extends SubsystemDependencyTestBase 
{
	protected static String APPLICATION_A="sdt_application.a.esa";
	
	@Override
	public void createApplications() throws Exception {
		super.createApplications();
		createTestApplicationA();
		registerRepositoryR1();
	}
	
	@Test
	public void verifyBundleCWiredToPackageXFromBundleA() throws Exception
	{ 
		Subsystem s = installSubsystemFromFile(APPLICATION_A);
		startSubsystem(s);
		
		verifySinglePackageWiring (s, BUNDLE_C, "x", BUNDLE_A);
 
		stopSubsystem(s);
		uninstallSubsystem(s);
	}
	
	@Test
	public void verifyBundleDWiredToBundleA() throws Exception
	{ 
		Subsystem s = installSubsystemFromFile(APPLICATION_A);
		startSubsystem(s);
		verifyRequireBundleWiring (s, BUNDLE_D, BUNDLE_A);
		stopSubsystem(s);
		uninstallSubsystem(s);
	}
	
	@Test
	public void verifyBundleEWiredToCapability_yFromBundleB() throws Exception
	{
		Subsystem s = installSubsystemFromFile (APPLICATION_A);
		startSubsystem(s);
		verifyCapabilityWiring (s, BUNDLE_E, "y", BUNDLE_B);
		stopSubsystem(s);
		uninstallSubsystem(s);
	}
	
	/*
	 * Verify no new bundles are installed into the Root subsystem 
	 * (particularly bundles F and G)
     * 
	 */
	@Test
	public void verifyNoUnexpectedBundlesProvisioned() throws Exception
	{ 
		Bundle[] rootBundlesBefore = bundleContext.getBundles();
		Subsystem s = installSubsystemFromFile(APPLICATION_A);
		startSubsystem(s);
		Bundle[] rootBundlesAfter = bundleContext.getBundles();
		for (Bundle b: rootBundlesAfter) {
			assertTrue ("Bundle F should not have been provisioned!", !b.getSymbolicName().equals(BUNDLE_F));
			assertTrue ("Bundle G should not have been provisioned!", !b.getSymbolicName().equals(BUNDLE_G));
		}
		checkNoNewBundles("SubsystemDependency_4ATest", rootBundlesBefore, rootBundlesAfter);
		stopSubsystem(s);
		uninstallSubsystem(s);
	}
	
	private static void createTestApplicationA() throws Exception
	{
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		String appContent = BUNDLE_A +","+ BUNDLE_B + "," + BUNDLE_C
			+ "," + BUNDLE_D + "," + BUNDLE_E;
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, appContent);
		createManifest(APPLICATION_A + ".mf", attributes);
		createSubsystem(APPLICATION_A);
	}

}
