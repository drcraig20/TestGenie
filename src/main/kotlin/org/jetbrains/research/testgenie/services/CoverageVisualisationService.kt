package org.jetbrains.research.testgenie.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import org.jetbrains.research.testgenie.TestGenieLabelsBundle
import org.jetbrains.research.testgenie.coverage.CoverageRenderer
import org.evosuite.utils.CompactReport
import java.awt.Color
import kotlin.math.roundToInt

/**
 * Service used to visualise the coverage and inject data in the toolWindow tab.
 *
 * @param project the project
 */
class CoverageVisualisationService(private val project: Project) {

    // Variable to keep reference to the coverage visualisation content
    private var content: Content? = null
    private var contentManager: ContentManager? = null
    private val textAttribute = TextAttributes()

    /**
     * Instantiates tab for coverage table and calls function to update coverage.
     *
     * @param testReport the generated tests summary
     * @param editor editor whose contents tests were generated for
     */
    fun showCoverage(testReport: CompactReport, editor: Editor) {
        // Show toolWindow statistics
        fillToolWindowContents(testReport)
        createToolWindowTab()

        updateCoverage(testReport.allCoveredLines, testReport.testCaseList.keys.toHashSet(), testReport, editor)
    }

    /**
     * Highlights lines covered by selected tests.
     * Shows coverage on the gutter next to the covered lines.
     *
     * @param linesToCover total set of lines  to cover
     * @param testReport report used for gutter information
     * @param selectedTests hash set of selected test names
     * @param editor editor instance where coverage should be updated
     */
    fun updateCoverage(
        linesToCover: Set<Int>,
        selectedTests: HashSet<String>,
        testReport: CompactReport,
        editor: Editor
    ) {
        // Show in-line coverage only if enabled in settings
        val quickAccessParametersState =
            ApplicationManager.getApplication().getService(QuickAccessParametersService::class.java).state

        if (quickAccessParametersState.showCoverage) {
            val settingsProjectState = project.service<SettingsProjectService>().state
            val color =
                Color(settingsProjectState.colorRed, settingsProjectState.colorGreen, settingsProjectState.colorBlue)
            val colorForLines = Color(
                settingsProjectState.colorRed,
                settingsProjectState.colorGreen,
                settingsProjectState.colorBlue,
                30
            )

            // Update the color used for highlighting if necessary
            textAttribute.backgroundColor = colorForLines

            editor.markupModel.removeAllHighlighters()

            // map of mutant operations -> List of names of tests which cover the mutant
            val mapMutantsToTests = HashMap<String, MutableList<String>>()

            testReport.testCaseList.values.forEach { compactTestCase ->
                val mutantsCovered = compactTestCase.coveredMutants
                val testName = compactTestCase.testName
                mutantsCovered.forEach {
                    val testCasesCoveringMutant = mapMutantsToTests.getOrPut(it.replacement) { ArrayList() }
                    testCasesCoveringMutant.add(testName)
                }
            }

            val mutationCovered =
                testReport.testCaseList.filter { x -> x.key in selectedTests }.map { x -> x.value.coveredMutants }
                    .flatten().groupBy { x -> x.lineNo }
            val mutationNotCovered =
                testReport.allUncoveredMutation.groupBy { x -> x.lineNo } + testReport.testCaseList.filter { x -> x.key !in selectedTests }
                    .map { x -> x.value.coveredMutants }.flatten().groupBy { x -> x.lineNo }

            for (i in linesToCover) {
                val line = i - 1

                val hl =
                    editor.markupModel.addLineHighlighter(line, HighlighterLayer.ADDITIONAL_SYNTAX, textAttribute)

                val testsCoveringLine =
                    testReport.testCaseList.filter { x -> i in x.value.coveredLines && x.key in selectedTests }
                        .map { x -> x.key }
                val mutationCoveredLine = mutationCovered.getOrDefault(i, listOf()).map { x -> x.replacement }
                val mutationNotCoveredLine = mutationNotCovered.getOrDefault(i, listOf()).map { x -> x.replacement }

                hl.lineMarkerRenderer = CoverageRenderer(
                    color,
                    line,
                    testsCoveringLine,
                    mutationCoveredLine,
                    mutationNotCoveredLine,
                    mapMutantsToTests,
                    project
                )
            }
        }
    }

    /**
     * Fill the toolWindow to contain the coverage in the labels.
     *
     * @param testReport the generated tests summary
     */
    private fun fillToolWindowContents(testReport: CompactReport) {

        // Calculate line coverage
        val coveredLines = testReport.allCoveredLines.size
        val allLines = testReport.allUncoveredLines.size + coveredLines
        var relativeLines = 100
        if (allLines != 0) {
            relativeLines = (coveredLines.toDouble() / allLines * 100).roundToInt()
        }

        // Call branch coverage
        val coveredBranches = testReport.allCoveredBranches.size
        val allBranches = testReport.allUncoveredBranches.size + coveredBranches
        var relativeBranch = 100
        if (allBranches != 0) {
            relativeBranch = (coveredBranches.toDouble() / allBranches * 100).roundToInt()
        }

        // Call mutations coverage
        val coveredMutations = testReport.allCoveredMutation.size
        val allMutations = testReport.allUncoveredMutation.size + coveredMutations
        var relativeMutations = 100
        if (allMutations != 0) {
            relativeMutations = (coveredMutations.toDouble() / allMutations * 100).roundToInt()
        }

        // Change the values in the table
        val coverageToolWindowDisplayService = project.service<CoverageToolWindowDisplayService>()
        coverageToolWindowDisplayService.data[0] = testReport.UUT
        coverageToolWindowDisplayService.data[1] = "$relativeLines% ($coveredLines/$allLines)"
        coverageToolWindowDisplayService.data[2] = "$relativeBranch% ($coveredBranches/$allBranches)"
        coverageToolWindowDisplayService.data[3] = "$relativeMutations% ($coveredMutations/$allMutations)"
    }

    /**
     * Creates a new toolWindow tab for the coverage visualisation.
     */
    private fun createToolWindowTab() {
        val visualisationService = project.service<CoverageToolWindowDisplayService>()

        // Remove coverage visualisation from content manager if necessary
        val toolWindowManager = ToolWindowManager.getInstance(project).getToolWindow("TestGenie")
        contentManager = toolWindowManager!!.contentManager
        if (content != null) {
            contentManager!!.removeContent(content!!, true)
        }

        // If there is no coverage visualisation tab, make it
        val contentFactory: ContentFactory = ContentFactory.getInstance()
        content = contentFactory.createContent(
            visualisationService.mainPanel, TestGenieLabelsBundle.defaultValue("coverageVisualisation"), true
        )
        contentManager!!.addContent(content!!)
    }

    /**
     * Closes the toolWindow tab for the coverage visualisation
     */
    fun closeToolWindowTab() {
        contentManager!!.removeContent(content!!, true)
    }
}
