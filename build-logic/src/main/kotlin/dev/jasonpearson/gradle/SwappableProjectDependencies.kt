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

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import xyz.block.artifactswap.gradle.artifactSwapCoordinates
import xyz.block.gradle.isArtifactSwapActive

/**
 * Name of the extension (registered by `androidbuild.kotlin-common` on every module's
 * [DependencyHandler]) that carries the owning [Project] so the [projects] accessors below can
 * reach Artifact Swap state.
 */
public const val SWAP_CONTEXT_EXTENSION: String = "androidbuildSwapContext"

/**
 * Plain holder for the owning [Project]. A raw [Project] must not be registered as an extension
 * directly: Gradle's Kotlin DSL accessor schema generation recurses into ExtensionAware extension
 * values, and a Project-typed extension makes that walk cyclic (StackOverflowError). This holder is
 * not ExtensionAware, so the walk terminates.
 */
public class SwapContext(public val project: Project)

/**
 * Artifact-Swap-aware, hand-maintained replacement for Gradle's generated type-safe project
 * accessors. Module build scripts import this explicitly:
 * ```
 * import dev.jasonpearson.gradle.projects
 *
 * dependencies { implementation(projects.foundation.designsystem) }
 * ```
 *
 * Why not the built-in TYPESAFE_PROJECT_ACCESSORS or `project(":path")`?
 * - The generated accessors require every referenced project to exist in the build, but Artifact
 *   Swap's whole point is to exclude swapped projects during IDE sync -- the generated accessors
 *   can never work with it.
 * - `project(":path")` cannot be intercepted from Kotlin build scripts at all: inside `dependencies
 *   {}` the call binds to Gradle's own Kotlin DSL extension ahead of any importable override (this
 *   is also why Artifact Swap's bundled Kotlin `project()` override never engages; its Groovy
 *   metaprogramming override has no Kotlin equivalent).
 *
 * The accessor *names* are load-bearing: Spotlight (and Artifact Swap's module selection on top of
 * it) discovers the project graph by statically parsing build scripts, and its type-safe pattern
 * `projects(\.\w+)+` matches these call sites exactly like the generated accessors it was designed
 * for.
 *
 * Each accessor returns:
 * - swap inactive (all CLI builds, or sync with `artifactswap.enabled=false`): a normal
 *   [ProjectDependency], identical to Gradle's own behavior; or
 * - swap active (IDE sync): the versionless artifact notation
 *   `<primaryArtifactsMavenGroup>:<path-as-artifactId>`, exactly like Artifact Swap's Groovy
 *   override. Artifact Swap's `ArtifactSwapProjectPlugin` dependency substitution then rewrites it
 *   back to the real project when the project is part of the build, or pins the content-hash
 *   version from the BOM when the project has been swapped out.
 */
public val DependencyHandler.projects: RootProjects
    get() = RootProjects(this)

public class RootProjects internal constructor(private val handler: DependencyHandler) {
    public val core: CoreProjects
        get() = CoreProjects(handler)

    public val foundation: FoundationProjects
        get() = FoundationProjects(handler)

    public val subsystem: SubsystemProjects
        get() = SubsystemProjects(handler)

    public val feature: FeatureProjects
        get() = FeatureProjects(handler)
}

public class CoreProjects internal constructor(private val handler: DependencyHandler) {
    public val common: ModuleDependency
        get() = handler.swappable(":core:common")

    public val model: ModuleDependency
        get() = handler.swappable(":core:model")
}

public class FoundationProjects internal constructor(private val handler: DependencyHandler) {
    public val designassets: ModuleDependency
        get() = handler.swappable(":foundation:designassets")

    public val designsystem: ModuleDependency
        get() = handler.swappable(":foundation:designsystem")

    public val navigation: ModuleDependency
        get() = handler.swappable(":foundation:navigation")
}

public class SubsystemProjects internal constructor(private val handler: DependencyHandler) {
    public val analytics: ModuleDependency
        get() = handler.swappable(":subsystem:analytics")

    public val experimentation: ModuleDependency
        get() = handler.swappable(":subsystem:experimentation")

    public val storage: ModuleDependency
        get() = handler.swappable(":subsystem:storage")
}

public class FeatureProjects internal constructor(private val handler: DependencyHandler) {
    public val demos: ModuleDependency
        get() = handler.swappable(":feature:demos")

    public val discover: ModuleDependency
        get() = handler.swappable(":feature:discover")

    public val home: ModuleDependency
        get() = handler.swappable(":feature:home")

    public val login: ModuleDependency
        get() = handler.swappable(":feature:login")

    public val mediaplayer: ModuleDependency
        get() = handler.swappable(":feature:mediaplayer")

    public val onboarding: ModuleDependency
        get() = handler.swappable(":feature:onboarding")

    public val settings: ModuleDependency
        get() = handler.swappable(":feature:settings")

    public val slides: ModuleDependency
        get() = handler.swappable(":feature:slides")
}

private fun DependencyHandler.swappable(path: String): ModuleDependency {
    val owner =
        ((this as ExtensionAware).extensions.findByName(SWAP_CONTEXT_EXTENSION) as? SwapContext)
            ?.project
            ?: error(
                "Dependency on $path was declared via the swap-aware projects.* accessors, but " +
                    "no $SWAP_CONTEXT_EXTENSION extension is registered on this project's " +
                    "dependencies. Apply the androidbuild.kotlin-common convention (directly, " +
                    "or via androidbuild.kotlin-jvm / androidbuild.android-library / " +
                    "androidbuild.android-compose). A silent fallback here would only fail " +
                    "later, during a swap-active IDE sync, far from the cause."
            )

    if (!owner.isArtifactSwapActive) {
        return project(mapOf("path" to path)) as ProjectDependency
    }

    val group = owner.providers.gradleProperty("artifactswap.primaryArtifactsMavenGroup").get()
    // Path -> artifactId via upstream's own helper, so consumer coordinates stay in
    // lockstep with what the publish plugin and BOM use. Versionless notation, exactly
    // like the Groovy override: ArtifactSwapProjectPlugin's dependency substitution
    // supplies the BOM version (or swaps back to the project).
    return create("$group:${path.artifactSwapCoordinates}") as ModuleDependency
}
