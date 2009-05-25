/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.blueprint.di;

import java.util.Collections;
import java.util.List;

import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.container.NoSuchComponentException;

public class RefRecipe extends AbstractRecipe {
    private String referenceName;

    public RefRecipe(String name, String referenceName) {
        super(name);
        this.referenceName = referenceName;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String name) {
        this.referenceName = name;
    }

    public List<Recipe> getNestedRecipes() {
        // TODO: this does not looks good as the list of nested recipes depends if the objects have already been created or not
        // TODO: it could lead to problems when determining the list of dependencies for example
        ExecutionContext context = ExecutionContext.getContext();
        if (!context.containsObject(referenceName)) {
            throw new NoSuchComponentException(referenceName);
        }

        Object object = ExecutionContext.getContext().getObject(referenceName);
        if (object instanceof Recipe) {
            Recipe recipe = (Recipe) object;
            return Collections.singletonList(recipe);
        }
        return Collections.emptyList();
    }

    protected Object internalCreate() throws ComponentDefinitionException {
        if (referenceName == null) {
            throw new ComponentDefinitionException("Reference name has not been set");
        }

        ExecutionContext context = ExecutionContext.getContext();

        Object object;
        if (!context.containsObject(referenceName)) {
            throw new ComponentDefinitionException("Currently no object registered with name " + referenceName +
                    " and a lazy reference not allowed");
        } else {
            object = context.getObject(referenceName);
            if (object instanceof Recipe) {
                Recipe recipe = (Recipe) object;
                object = recipe.create();
            }
        }

        return object;
    }

}
