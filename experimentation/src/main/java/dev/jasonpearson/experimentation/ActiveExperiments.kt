package dev.jasonpearson.experimentation

import dev.jasonpearson.experimentation.experiments.MoodExperiment

enum class ActiveExperiments(val experimentName: String) {
    Mood(MoodExperiment.EXPERIMENT_NAME)
}
