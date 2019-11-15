package org.droidmate.saigen

import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import org.droidmate.saigen.utils.NLP
import org.droidmate.saigen.utils.nouns
import java.nio.file.Files
import java.nio.file.Path
import org.droidmate.saigen.Lib.Companion.extractWidgetsAndLabels // = return LabelMatcher.getLabels(state)

/**
 * Concept to action mapping
 */
data class CAM(private val sourceConcepts: List<String>, private val targetConcepts: List<String>) {
    private fun matchesSources(state: State<*>): Boolean {
        val widgetLabelMap = extractWidgetsAndLabels(state)

        return widgetLabelMap.values
            .all { this.sourceConcepts.contains(it) }
    }

    private fun matchesTargets(state: State<*>): Boolean {
        val concepts = state.visibleTargets
            .filter { it.isVisible }
            .flatMap { widget ->
                val nouns = widget.nouns()
                val synonyms = nouns.flatMap { NLP.getSynonyms(it) }

                synonyms
            }

        return this.targetConcepts
            .all { concepts.contains(it) }
    }

    fun getTargets(state: State<*>): List<Widget> {
        val concepts = state.visibleTargets
            .flatMap { widget ->
                val nouns = widget.nouns()
                val synonyms = nouns.flatMap { NLP.getSynonyms(it) }

                synonyms.map { Pair(widget, it) }
            }

        return concepts
            .filter { targetConcepts.contains(it.second) }
            .map { it.first }
    }

    /**
     * Checks if the [state] applies to all source and target concepts from this CAM.
     * For sources, only input fields are considered.
     */
    fun matches(state: State<*>): Boolean {
        return matchesSources(state) &&
                matchesTargets(state)
    }

    companion object {

        /**
         * Reads an input file and generate multiple test case descriptors
         *
         * @return List of test case descriptors, or empty if the input file doesn't exist
         */
        fun fromFile(camDescriptor: Path): List<CAM> {
            if (!Files.exists(camDescriptor)) {
                return emptyList()
            }

            return Files.readAllLines(camDescriptor)
                .filter { it.isNotEmpty() }
                .map { line ->
                    val lineData = line.split("->")
                    assert(lineData.size == 2) { "Unknown information on descriptor file: $line" }

                    val sourceConcepts = lineData.first()
                        .split(",")
                        .map { it.trim() }
                    val targetConcepts = lineData.last()
                        .split(",")
                        .map { it.trim() }

                    CAM(sourceConcepts, targetConcepts)
                }
        }
    }
}