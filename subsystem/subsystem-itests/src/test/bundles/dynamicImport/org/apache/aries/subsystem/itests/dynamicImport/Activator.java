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
package org.apache.aries.subsystem.itests.dynamicImport;

import org.apache.aries.subsystem.itests.hello.api.Hello;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator 
{

	ServiceRegistration _sr = null;
	
	@Override
	public void start(BundleContext bc) throws Exception 
	{
		System.out.println ("into " + this.getClass().getCanonicalName() + ".start()");
		
		Hello helloService = new DynamicImportHelloImpl();
		
		_sr = bc.registerService(Hello.class, helloService, null);
		
		System.out.println ("exiting " + this.getClass().getCanonicalName() + ".start()");
	}

	@Override
	public void stop(BundleContext bc) throws Exception 
	{
		if (_sr != null) { 
			_sr.unregister();
		}
	}
}
