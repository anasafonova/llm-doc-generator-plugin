package com.example

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Entry point action that generates documentation for the method or function
 * under the caret. Supports Java (PsiMethod) and Kotlin (KtNamedFunction).
 */
class GenerateDocAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor == null) {
            Messages.showWarningDialog(project, "No active editor.", "Generate Documentation")
            return
        }

        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (psiFile == null) {
            Messages.showWarningDialog(
                project,
                "Could not resolve PSI for this file (it may be unsaved or still indexing).",
                "Generate Documentation"
            )
            return
        }

        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
        if (element == null) {
            Messages.showWarningDialog(project, "Caret is outside the file text.", "Generate Documentation")
            return
        }

        // Try Java first.
        val javaMethod = element.parentOfType<PsiMethod>()
        if (javaMethod != null) {
            DocGenerator(project, editor).generate(javaMethod)
            return
        }

        // Fall back to Kotlin.
        val ktFunction = element.parentOfType<KtNamedFunction>()
        if (ktFunction != null) {
            DocGenerator(project, editor).generateForKotlin(ktFunction)
            return
        }

        Messages.showWarningDialog(
            project,
            "No Java method or Kotlin function found at the caret position.\n" +
                    "Place the caret inside a method or function body.",
            "Generate Documentation"
        )
    }

    // Enabled whenever there is an open editor with a resolvable PSI file.
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabled = editor != null && psiFile != null
    }
}