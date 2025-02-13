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

import java.io.InputStream;

import org.apache.aries.itest.RichBundleContext;
import org.apache.aries.subsystem.itests.bundles.blueprint.BPHelloImpl;
import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.junit.Test;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.subsystem.Subsystem;

/*
 * iTest for blueprint with subsystems
 */
public class BlueprintTest extends SubsystemTest 
{
    private static final String BLUEPRINT_ESA = "target/blueprint.esa";

    protected void init() throws Exception {
        writeToFile(createBlueprintEsa(), BLUEPRINT_ESA);
    }

	@Test
	public void checkBlueprint() throws Exception
	{
	    Subsystem subsystem = installSubsystemFromFile(BLUEPRINT_ESA);
		try { 
			startSubsystem(subsystem);
			BundleContext bc = subsystem.getBundleContext();
			Hello h = new RichBundleContext(bc).getService(Hello.class);
			String message = h.saySomething();
			assertEquals("Wrong message back", "messageFromBlueprint", message);
		} finally { 
			stopSubsystem(subsystem);
			uninstallSubsystem(subsystem);
		}
	}

    private InputStream createBlueprintEsa() throws Exception {
	    return TinyBundles.bundle()
	        .add("OSGI-INF/SUBSYSTEM.MF", getResource("blueprint/OSGI-INF/SUBSYSTEM.MF"))
	        .add("blueprint.jar", createBlueprintTestBundle())
	        .build(TinyBundles.withBnd());
    }

    private InputStream createBlueprintTestBundle() {
        return TinyBundles.bundle()
	        .add(BPHelloImpl.class)
	        .add("OSGI-INF/blueprint/blueprint.xml", getResource("blueprint/OSGI-INF/blueprint/blueprint.xml"))
	        .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.aries.subsystem.itests.blueprint")
	        .build(TinyBundles.withBnd());
    }
	
}
