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
package org.apache.aries.subsystem.itests.cmcontent.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;

public class Activator implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> blahProps = new Hashtable<String, Object>();
        blahProps.put(Constants.SERVICE_PID, "com.blah.Blah");
        context.registerService(ManagedService.class, new BlahManagedService(context), blahProps);

        Dictionary<String, Object> barProps = new Hashtable<String, Object>();
        barProps.put(Constants.SERVICE_PID, "org.foo.Bar");
        context.registerService(ManagedService.class, new BarManagedService(context), barProps);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
