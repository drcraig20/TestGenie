package org.jetbrains.research.testgenie.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Caret
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import org.jetbrains.research.testgenie.evosuite.Pipeline
import org.jetbrains.research.testgenie.services.RunnerService

/**
 * This class contains all the logic related to generating tests for a class.
 * No actual generation happens in this class, rather it is responsible for displaying the action option to the user when it is available,
 *   getting the information about the selected class and passing it to (EvoSuite) Pipeline.
 */
class GenerateTestsActionClass : AnAction() {
    /**
     * Creates and calls (EvoSuite) Pipeline to generate tests for a class when the action is invoked.
     *
     * @param e an action event that contains useful information and corresponds to the action invoked by the user
     */
    override fun actionPerformed(e: AnActionEvent) {
        val psiFile: PsiFile = e.dataContext.getData(CommonDataKeys.PSI_FILE) ?: return

        val project = e.project ?: return

        val runnerService = project.service<RunnerService>()
        if (!runnerService.verify(psiFile)) return

        val linesToInvalidateFromCache = calculateLinesToInvalidate(psiFile)

        val evoSuitePipeline: Pipeline = createEvoSuitePipeline(e) ?: return
        evoSuitePipeline.forClass().invalidateCache(linesToInvalidateFromCache).runTestGeneration()
    }

    /**
     * Makes the action visible only if a class has been selected.
     * It also updates the action name depending on which class has been selected.
     *
     * @param e an action event that contains useful information and corresponds to the action invoked by the user
     */
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = false

        val caret: Caret = e.dataContext.getData(CommonDataKeys.CARET)?.caretModel?.primaryCaret ?: return
        val psiFile: PsiFile = e.dataContext.getData(CommonDataKeys.PSI_FILE) ?: return

        val psiClass: PsiClass = getSurroundingClass(psiFile, caret) ?: return

        e.presentation.isEnabledAndVisible = true
        e.presentation.text = "Generate Tests For ${getClassDisplayName(psiClass)}"
    }
}
