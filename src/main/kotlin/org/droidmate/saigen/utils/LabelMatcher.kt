package org.droidmate.saigen.utils

import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import org.droidmate.saigen.SaigenMF
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Checks if the widget is currently visible on the screen, can be interacted with and is an input field
 */
fun Widget.isVisibleDataWidget(): Boolean = this.isVisible && this.isInputField && this.canInteractWith

/**
 * Return all the nouns associated with the widget text
 */
fun Widget.nouns() = NLP.getNouns(this.nlpText)

/**
 * Checks if the widget text has nouns
 */
fun Widget.hasNouns() = nouns().isNotEmpty()

/**
 * Check if the widget has text or content-desc
 */
fun Widget.hasText() = this.nlpText.isNotBlank()

/**
 * Get the label widget which better describe this widget using Modified Becce Metrics
 *
 * Only candidate widgets are filtered
 */
fun Widget.getLabelWidget(candidateLabels: List<Widget>): Widget? {
    val bestWidget = candidateLabels
        .filter { candidateLabel -> this.isCandidateLabel(candidateLabel) }
        .map { Pair(it, it.becceDistance(this)) }
        .minBy { it.second }

    return bestWidget?.first
}

/**
 * Add a check that the text attribute contains no longer than 3 or 5 words, otherwise not a label
 */
fun Widget.isCandidateLabel(labelWidget: Widget): Boolean {
    val className = labelWidget.className.toLowerCase()

    return when {
        !className.contains("view") -> false
        labelWidget.isInputField -> false
        // Proximity (Pt 1)
        this.visibleBounds.center.first < labelWidget.boundaries.leftX -> false
        // Homogeneity
        !this.hasParent || !labelWidget.hasParent -> false
        this.parentId != labelWidget.parentId -> false
        else -> true
    }
}

/**
 * Compute the distance between two widgets' representative points (according to Becce's paper)
 */
fun Widget.becceDistance(dataWidget: Widget): Double {
    val widgetPoint = Pair(dataWidget.boundaries.leftX, dataWidget.boundaries.topY)

    // If the candidate widget is lower than the data widget, apply top right as rep point. (x+width, y)
    val candidatePoint = if (this.boundaries.topY >= dataWidget.boundaries.topY) {
        // top right point
        Pair(this.boundaries.rightX, this.boundaries.topY)
    }
    // Else apply bottom left as rep point. (x, y+height)
    else {
        // bottom left
        Pair(this.boundaries.leftX, this.boundaries.bottomY)
    }

    // Distance calculation: sqRoot(pow2(x1-x2) + pow2(y1-y2))
    return sqrt(
        abs(widgetPoint.first - candidatePoint.first).toDouble().pow(2) +
                abs(widgetPoint.second - candidatePoint.second).toDouble().pow(2)
    )
}

object LabelMatcher {
    @JvmStatic
    val log: Logger by lazy { LoggerFactory.getLogger(SaigenMF::class.java) }

    /**
     * Store the data widgets which have already been mapped
     */
    private val widgetLabelCache = mutableMapOf<UUID, String>()

    private fun Widget.getLabel(state: State<*>): String {

        if (widgetLabelCache.containsKey(this.uid)) {
            return widgetLabelCache[this.uid].orEmpty()
        }

        val labelCandidates = state.widgets
            .filterNot { it.isVisibleDataWidget() }
            .filter { it.hasText() }

        if (labelCandidates.isEmpty()) {
            log.warn("No cachedLabel candidate widgets in state ${state.uid}\t ${state.configId}")
            return ""
        }

        val match = if (this.hasText() && this.hasNouns()) {
            this
        } else {
            this.getLabelWidget(labelCandidates)
        }

        // Local cache
        val label = match?.nouns()?.firstOrNull().orEmpty()
        if (label.isEmpty()) {
            log.warn("Widget: ${this.uid} not matched to any cachedLabel. NLP Text: ${match?.nlpText}")
        }

        widgetLabelCache[this.uid] = label

        return label
    }

    /**
     * Returns a label from the cache
     */
    fun cachedLabel(widget: Widget) = widgetLabelCache[widget.uid].orEmpty()
    fun cachedLabel(widget: UUID) = widgetLabelCache[widget].orEmpty()
    /**
     * Returns all data widgets on the screen and their associated cachedLabel descriptors
     */
    fun getLabels(state: State<*>): Map<Widget, String> {
        val uiWidgets = state.widgets.toList()

        val dataWidgets = uiWidgets
            .filter { it.isVisibleDataWidget() }

        if (dataWidgets.isEmpty()) {
            log.warn("No data widget in state ${state.uid}\t ${state.configId}")
            return emptyMap()
        }

        return dataWidgets
            .map { Pair(it, it.getLabel(state)) }
            .filter { it.second.isNotEmpty() }
            .toMap()
    }
}