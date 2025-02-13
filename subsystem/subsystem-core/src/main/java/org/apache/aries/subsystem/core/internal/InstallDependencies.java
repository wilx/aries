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
package org.apache.aries.subsystem.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Resource;
import org.osgi.service.coordinator.Coordination;

public class InstallDependencies {
    public void install(BasicSubsystem subsystem, BasicSubsystem parent, Coordination coordination) throws Exception{
        // Install dependencies first...
        List<Resource> dependencies = new ArrayList<Resource>(subsystem.getResource().getInstallableDependencies());
        Collections.sort(dependencies, new InstallResourceComparator());
        for (Resource dependency : dependencies)
            ResourceInstaller.newInstance(coordination, dependency, subsystem).install();
        for (Resource dependency : subsystem.getResource().getSharedDependencies()) {
            // TODO This needs some more thought. The following check
            // protects against a child subsystem that has its parent as a
            // dependency. Are there other places of concern as well? Is it
            // only the current parent that is of concern or should all
            // parents be checked?
            if (parent==null || !dependency.equals(parent))
                ResourceInstaller.newInstance(coordination, dependency, subsystem).install();
        }
    }
}
