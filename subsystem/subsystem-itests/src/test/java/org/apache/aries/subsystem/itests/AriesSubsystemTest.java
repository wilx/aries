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
package org.apache.aries.subsystem.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.aries.subsystem.AriesSubsystem;
import org.apache.aries.subsystem.core.internal.BasicRequirement;
import org.apache.aries.util.filesystem.FileSystem;
import org.apache.aries.util.filesystem.IDirectory;
import org.easymock.EasyMock;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionFilter;
import org.junit.Test;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.SubsystemConstants;
import org.osgi.service.subsystem.SubsystemException;

@ExamReactorStrategy(PerMethod.class)
public class AriesSubsystemTest extends SubsystemTest {
	/*
	 * Subsystem-SymbolicName: application.a.esa
	 * Subsystem-Content: bundle.a.jar
	 */
	private static final String APPLICATION_A = "application.a.esa";
	/*
	 * Subsystem-SymbolicName: application.b.esa
	 * Subsystem-Content: bundle.b.jar
	 */
	private static final String APPLICATION_B = "application.b.esa";
	/*
	 * Bundle-SymbolicName: bundle.a.jar
	 * Import-Package: org.osgi.framework,org.osgi.resource
	 */
	private static final String BUNDLE_A = "bundle.a.jar";
	/*
	 * Bundle-SymbolicName: bundle.b.jar
	 * Import-Package: org.osgi.resource
	 */
	private static final String BUNDLE_B = "bundle.b.jar";
	/*
	 * Subsystem-SymbolicName: composite.a.esa
	 * Subsystem-Type: osgi.subsystem.composite
	 */
	private static final String COMPOSITE_A = "composite.a.esa";
	
	private void createApplicationA() throws IOException {
		createApplicationAManifest();
		createSubsystem(APPLICATION_A, BUNDLE_A);
	}
	
	private void createApplicationB() throws IOException {
		createApplicationBManifest();
		createSubsystem(APPLICATION_B, BUNDLE_B);
	}
	
	private void createApplicationAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_A);
		createManifest(APPLICATION_A + ".mf", attributes);
	}
	
	private void createApplicationBManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, APPLICATION_B);
		createManifest(APPLICATION_B + ".mf", attributes);
	}
	
	private void createBundleA() throws IOException {
		createBundle(name(BUNDLE_A), importPackage("org.osgi.framework,org.osgi.resource"));
	}
	
	private void createBundleB() throws IOException {
		createBundle(name(BUNDLE_B), importPackage("org.osgi.resource"));
	}
	
	private void createCompositeA() throws IOException {
		createCompositeAManifest();
		createSubsystem(COMPOSITE_A, BUNDLE_B, APPLICATION_B);
	}
	
	private void createCompositeAManifest() throws IOException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, COMPOSITE_A);
		attributes.put(SubsystemConstants.SUBSYSTEM_TYPE, SubsystemConstants.SUBSYSTEM_TYPE_COMPOSITE);
		attributes.put(SubsystemConstants.SUBSYSTEM_CONTENT, 
				BUNDLE_B + ';' + IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE + "=\"[0,0]\","
				+ APPLICATION_B + ';' + IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE + "=\"[0,0]\";" + IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE + '=' + SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		attributes.put(Constants.IMPORT_PACKAGE, "org.osgi.resource");
		createManifest(COMPOSITE_A + ".mf", attributes);
	}
	
	@Override
	public void createApplications() throws Exception {
		createBundleA();
		createBundleB();
		createApplicationA();
		createApplicationB();
		createCompositeA();
	}
	
	/*
	 * The region copy process when adding additional requirements should
	 * keep all edges, not just the ones running between parent and child. This
	 * is of particular concern with regard to the connections all subsystem
	 * regions have with the root region to allow the subsystem services
	 * through. However, it may also be of concern if the region digraph is
	 * modified outside of the subsystems API.
	 */
	@Test
	public void testAddRequirementsKeepsEdgesOtherThanParentChild() throws Exception {
		AriesSubsystem compositeA = (AriesSubsystem)installSubsystemFromFile(COMPOSITE_A);
		try {
			AriesSubsystem applicationB = (AriesSubsystem)getConstituentAsSubsystem(compositeA, APPLICATION_B, null, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
			Region bRegion = getRegion(applicationB);
			// One edge to parent for import package. One edge to root for subsystem
			// service.
			assertEquals("Wrong number of edges", 2, bRegion.getEdges().size());
			Requirement requirement = new BasicRequirement.Builder()
					.namespace(PackageNamespace.PACKAGE_NAMESPACE)
					.directive(
							PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, 
							"(osgi.wiring.package=org.osgi.framework)")
					.resource(EasyMock.createMock(Resource.class))
					.build();
			applicationB.addRequirements(Collections.singleton(requirement));
			bRegion = getRegion(applicationB);
			// Still one edge to parent for import package. One edge to root for 
			// subsystem service.
			assertEquals("Wrong number of edges", 2, bRegion.getEdges().size());
			Region rootRegion = getRegion(getRootSubsystem());
			// The root region won't be the tail region for any connection unless
			// manually added.
			assertEquals("Wrong number of edges", 0, rootRegion.getEdges().size());
			// Manually add a connection from root to application B.
			rootRegion.connectRegion(
					bRegion, 
					rootRegion.getRegionDigraph().createRegionFilterBuilder().allow(
							"com.foo", 
							"(bar=b)").build());
			// The root region should now have an edge.
			assertEquals("Wrong number of edges", 1, rootRegion.getEdges().size());
			// Add another requirement to force a copy.
			requirement = new BasicRequirement.Builder()
					.namespace(PackageNamespace.PACKAGE_NAMESPACE)
					.directive(
							PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, 
							"(osgi.wiring.package=org.osgi.framework.wiring)")
					.resource(EasyMock.createMock(Resource.class))
					.build();
			applicationB.addRequirements(Collections.singleton(requirement));
			rootRegion = getRegion(getRootSubsystem());
			// The root region should still have its edge.
			assertEquals("Wrong number of edges", 1, rootRegion.getEdges().size());
			bRegion = getRegion(applicationB);
			// Still one edge to parent for import package. One edge to root for 
			// subsystem service.
			assertEquals("Wrong number of edges", 2, bRegion.getEdges().size());
		}
		finally {
			uninstallSubsystemSilently(compositeA);
		}
	}
	
	/*
	 * Test the AriesSubsystem.addRequirements(Collection<Requirement>) method.
	 * 
	 * There are several things to consider for this test.
	 * 
	 * (1) Installing a child subsystem before the requirement has been added
	 *     should fail.
	 * (2) Installing a child subsystem after the requirement has been added
	 *     should succeed.
	 * (3) The newly created region should contain all of the bundles from the
	 *     old one.
	 * (4) The connections between the subsystem with the added requirement and
	 *     its parents should be reestablished.
	 * (5) The connections between the subsystem with the added requirement and
	 *     its children should be reestablished.
	 */
	@Test
	public void testAddRequirements() throws Exception {
		AriesSubsystem compositeA = (AriesSubsystem)installSubsystemFromFile(COMPOSITE_A);
		try {
			startSubsystem(compositeA);
			assertCompositeABefore(compositeA);
			// Test that the installation of applicationA fails.
			try {
				installSubsystemFromFile(compositeA, APPLICATION_A);
				fail("Subsystem should not have installed due to unresolved org.osgi.framework package requirement");
			} catch (SubsystemException e) {
				// Okay.
			}
			// Add the org.osgi.framework package requirement.
			Requirement requirement = new BasicRequirement.Builder()
				.namespace(PackageNamespace.PACKAGE_NAMESPACE)
				.directive(
					PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE, 
					"(osgi.wiring.package=org.osgi.framework)")
					.resource(EasyMock.createMock(Resource.class))
					.build();
			compositeA.addRequirements(Collections.singleton(requirement));
			// Test that the bundles were copied over to the newly created region.
			assertCompositeABefore(compositeA);
			// Test that the parent connections were reestablished.
			assertRefreshAndResolve(Collections.singletonList(getConstituentAsBundle(compositeA, BUNDLE_B, null, null)));
			// Test that the child connections were reestablished.
			assertRefreshAndResolve(Collections.singletonList(getConstituentAsBundle(getConstituentAsSubsystem(compositeA, APPLICATION_B, null, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION), BUNDLE_B, null, null)));
			// Test that the installation of applicationA succeeds.
			AriesSubsystem applicationA;
			try {
				applicationA = (AriesSubsystem)installSubsystemFromFile(compositeA, APPLICATION_A);
				startSubsystem(applicationA);
			} catch (SubsystemException e) {
				fail("Subsystem should have installed and started");
			}
			assertCompositeAAfter(compositeA);
		}
		finally {
			stopAndUninstallSubsystemSilently(compositeA);
		}
	}
	
	/*
	 * Aries Subsystems uses Equinox Region Digraph as its isolation engine.
	 * Digraph has a "special" namespace value that tells the region to allow
	 * everything a bundle offers. This test ensures that a correctly formatted
	 * requirement in that namespace works as expected.
	 */
	@Test
	public void testAddRequirementWithVisibleBundleNamespace() throws Exception {
		Requirement requirement = new BasicRequirement.Builder()
				.namespace(RegionFilter.VISIBLE_BUNDLE_NAMESPACE)
				.directive(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(id=0)")
				.resource(EasyMock.createMock(Resource.class)).build();
		AriesSubsystem compositeA = (AriesSubsystem) installSubsystemFromFile(COMPOSITE_A);
		try {
			startSubsystem(compositeA);
			// Test that the installation of applicationA fails.
			try {
				installSubsystemFromFile(compositeA, APPLICATION_A);
				fail("Subsystem should not have installed due to unresolved org.osgi.framework package requirement");
			} catch (SubsystemException e) {
				// Okay.
			}
			// Add the requirement with the region digraph specific namespace.
			compositeA.addRequirements(Collections.singleton(requirement));
			// Test that the installation and startup of applicationA succeeds.
			AriesSubsystem applicationA;
			try {
				applicationA = (AriesSubsystem) installSubsystemFromFile(
						compositeA, APPLICATION_A);
				startSubsystem(applicationA);
			} catch (SubsystemException e) {
				fail("Subsystem should have installed and started");
			}
			assertCompositeAAfter(compositeA);
		} finally {
			stopAndUninstallSubsystemSilently(compositeA);
		}
	}
	
	@Test
	public void testInstallIDirectory() {
		File file = new File(COMPOSITE_A);
		IDirectory directory = FileSystem.getFSRoot(file);
		try {
			AriesSubsystem compositeA = getRootAriesSubsystem().install(COMPOSITE_A, directory);
			uninstallSubsystemSilently(compositeA);
		}
		catch (Exception e) {
			fail("Installation from IDirectory should have succeeded");
		}
	}
	
	@Test
	public void testServiceRegistrations() {
		Subsystem root1 = null;
		try {
			root1 = getRootSubsystem();
		}
		catch (Exception e) {
			fail(Subsystem.class.getName() + " service not registered");
		}
		AriesSubsystem root2 = null;
		try {
			root2 = getRootAriesSubsystem();
		}
		catch (Exception e) {
			fail(AriesSubsystem.class.getName() + " service not registered");
		}
		assertSame("Services should be the same instance", root1, root2);
	}
	
	private void assertCompositeAAfter(Subsystem compositeA) {
		// applicationA, applicationB, bundleB, region context bundle
		assertConstituents(4, compositeA);
		assertConstituent(compositeA, APPLICATION_A, null, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		assertConstituent(compositeA, APPLICATION_B, null, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		assertConstituent(compositeA, BUNDLE_B);
		assertNotNull("Bundle not in region", getRegion(compositeA).getBundle(BUNDLE_B, Version.emptyVersion));
		assertConstituent(compositeA, "org.osgi.service.subsystem.region.context.1", Version.parseVersion("1"));
		// applicationA, applicationB
		assertChildren(2, compositeA);
		assertApplicationA(assertChild(compositeA, APPLICATION_A));
		assertApplicationB(assertChild(compositeA, APPLICATION_B));
	}
	
	private void assertCompositeABefore(Subsystem compositeA) {
		// applicationB, bundleB, region context bundle
		assertConstituents(3, compositeA);
		assertConstituent(compositeA, APPLICATION_B, null, SubsystemConstants.SUBSYSTEM_TYPE_APPLICATION);
		assertConstituent(compositeA, BUNDLE_B);
		assertNotNull("Bundle not in region", getRegion(compositeA).getBundle(BUNDLE_B, Version.emptyVersion));
		assertConstituent(compositeA, "org.osgi.service.subsystem.region.context.1", Version.parseVersion("1"));
		// applicationB
		assertChildren(1, compositeA);
		assertApplicationB(assertChild(compositeA, APPLICATION_B));
	}
	
	private void assertApplicationA(Subsystem applicationA) {
		// bundleA, region context bundle
		assertConstituents(2, applicationA);
		assertConstituent(applicationA, BUNDLE_A);
		// The subsystem id is 4 instead of 3 due to the first installation that failed.
		assertConstituent(applicationA, "org.osgi.service.subsystem.region.context.4", Version.parseVersion("1"));
		assertChildren(0, applicationA);
	}
	
	private void assertApplicationB(Subsystem applicationB) {
		// bundleB, region context bundle
		assertConstituents(2, applicationB);
		assertConstituent(applicationB, BUNDLE_B);
		assertConstituent(applicationB, "org.osgi.service.subsystem.region.context.2", Version.parseVersion("1"));
		assertChildren(0, applicationB);
	}
}
