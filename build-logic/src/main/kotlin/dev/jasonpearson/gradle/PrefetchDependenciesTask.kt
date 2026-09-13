/*
 * MIT License
 *
 * Copyright (c) 2026 Jason Pearson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.jasonpearson.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Warms the Gradle dependency cache for one project (Shrinking Elephants, "dependency
 * pre-fetching"). Its only input is a file collection of every *external* artifact on the project's
 * compile and runtime classpaths; declaring them as task inputs makes Gradle download (and run
 * artifact transforms for) all of them before the action runs, so the action itself has nothing
 * left to do but report. Project dependencies are filtered out at the artifact-view level, so no
 * compilation is triggered.
 *
 * The task deliberately declares no outputs: it is never up-to-date and re-resolves on every run,
 * which is cheap once the cache is warm and exactly the point when it is not.
 */
public abstract class PrefetchDependenciesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val artifacts: ConfigurableFileCollection

    @TaskAction
    public fun prefetch() {
        val count = artifacts.files.size
        logger.lifecycle("Pre-fetched $count external artifact(s) for $path")
    }
}
