/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.project.taskfactory;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Task;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.internal.ClassGenerator;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.reflect.ObjectInstantiationException;
import org.gradle.api.tasks.TaskInstantiationException;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.NameValidator;

import java.util.concurrent.Callable;

public class TaskFactory implements ITaskFactory {
    private static final Object[] NO_ARGS = new Object[0];

    private final ClassGenerator generator;
    private final ProjectInternal project;
    private final Instantiator instantiator;

    public TaskFactory(ClassGenerator generator) {
        this(generator, null, null);
    }

    TaskFactory(ClassGenerator generator, ProjectInternal project, Instantiator instantiator) {
        this.generator = generator;
        this.project = project;
        this.instantiator = instantiator;
    }

    public ITaskFactory createChild(ProjectInternal project, Instantiator instantiator) {
        return new TaskFactory(generator, project, instantiator);
    }

    @Override
    public <S extends Task> S create(String name, Class<S> type) {
        return create(name, type, NO_ARGS);
    }

    @Override
    public <S extends Task> S create(String name, final Class<S> type, final Object... args) {
        if (!Task.class.isAssignableFrom(type)) {
            throw new InvalidUserDataException(String.format(
                "Cannot create task of type '%s' as it does not implement the Task interface.",
                type.getSimpleName()));
        }
        NameValidator.validate(name, "task name", "");

        final Class<? extends Task> generatedType;
        if (type.isAssignableFrom(DefaultTask.class)) {
            generatedType = generator.generate(DefaultTask.class);
        } else {
            generatedType = generator.generate(type);
        }

        return type.cast(AbstractTask.injectIntoNewInstance(project, name, type, new Callable<Task>() {
            public Task call() throws Exception {
                try {
                    return instantiator.newInstance(generatedType, args);
                } catch (ObjectInstantiationException e) {
                    throw new TaskInstantiationException(String.format("Could not create task of type '%s'.", type.getSimpleName()),
                        e.getCause());
                }
            }
        }));
    }
}
