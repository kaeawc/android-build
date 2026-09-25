package dev.jasonpearson.android.subsystem.experimentation

import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
public class HashingExperimentRepository(@InstallId private val installId: String) :
    ExperimentRepository {
    override fun treatmentFor(experiment: Experiment): Treatment {
        if (experiment.treatments.isEmpty()) return experiment.default
        val bucket =
            Math.floorMod("$installId:${experiment.id}".hashCode(), experiment.treatments.size)
        return experiment.treatments[bucket]
    }
}
