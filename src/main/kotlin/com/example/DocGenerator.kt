package com.example

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Builds an LLM prompt from PSI, sends it to [LlmClient], and inserts the
 * generated documentation comment above the target method or function.
 */
class DocGenerator(private val project: Project, private val editor: Editor) {

    // ---------- Java ----------

    fun generate(method: PsiMethod) {
        val prompt = buildPrompt(method)

        // The LLM call runs on a background thread; it must never block the EDT.
        ApplicationManager.getApplication().executeOnPooledThread {
            val doc = LlmClient.generate(prompt)

            // PSI writes must happen on the EDT, inside a write action.
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    insertDoc(method, doc)
                }
            }
        }
    }

    fun buildPrompt(method: PsiMethod): String {
        val params = method.parameterList.parameters
            .joinToString { "${it.type.presentableText} ${it.name}" }

        return """
            Generate JavaDoc for this Java method.
            Name: ${method.name}
            Parameters: $params
            Return type: ${method.returnType?.presentableText ?: "void"}
            Body:
            ${method.body?.text}

            Return only the JavaDoc comment, nothing else.
        """.trimIndent()
    }

    fun insertDoc(method: PsiMethod, docText: String) {
        val factory = JavaPsiFacade.getElementFactory(project)
        val docComment = factory.createDocCommentFromText(docText)
        method.parent.addBefore(docComment, method)
    }

    // ---------- Kotlin ----------

    fun generateForKotlin(function: KtNamedFunction) {
        val prompt = buildPromptKotlin(function)

        ApplicationManager.getApplication().executeOnPooledThread {
            val doc = LlmClient.generate(prompt)

            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    insertDocKotlin(function, doc)
                }
            }
        }
    }

    fun buildPromptKotlin(function: KtNamedFunction): String {
        val params = function.valueParameters.joinToString {
            "${it.name}: ${it.typeReference?.text ?: "Any"}"
        }
        val returnType = function.typeReference?.text ?: "Unit"
        val body = function.bodyExpression?.text
            ?: function.bodyBlockExpression?.text
            ?: ""

        return """
            Generate KDoc for this Kotlin function.
            Name: ${function.name}
            Parameters: $params
            Return type: $returnType
            Body:
            $body

            Return only the KDoc comment (/** ... */ style), nothing else.
        """.trimIndent()
    }

    fun insertDocKotlin(function: KtNamedFunction, docText: String) {
        // Inserted directly through the Document API rather than KtPsiFactory:
        // simpler and more robust for this first pass of Kotlin support.
        // PSI is rebuilt from the document automatically inside the write action.
        val startOffset = function.textRange.startOffset
        val trimmedDoc = docText.trim()
        editor.document.insertString(startOffset, "$trimmedDoc\n")
    }
}