# PSI and AST notes

Background on how IntelliJ represents source code, and the specific PSI
APIs this plugin relies on.

## AST vs. PSI

IntelliJ builds its understanding of a file in two stages:

1. **Lexer → Parser → AST.** The lexer splits the file into tokens, the
   parser assembles a raw `ASTNode` tree. This is a purely syntactic
   structure — a hierarchy of typed nodes with no semantic information.
2. **AST → PSI.** Each `ASTNode` is wrapped by a PSI element with a richer
   API. `PsiMethod`, for example, is a wrapper over the corresponding
   `ASTNode` that adds convenience accessors: `getReturnType()`,
   `getParameterList()`, `getDocComment()`, and so on.

In short: the AST is the raw data, PSI is the API on top of it.

## Java: `PsiMethod`

```kotlin
val method: PsiMethod = ...

method.name                              // "calculateTotal"
method.returnType?.presentableText       // "double"
method.containingClass?.name             // "OrderService"

method.parameterList.parameters.forEach { param ->
    param.name                           // "count"
    param.type.presentableText           // "int"
}

method.throwsList.referencedTypes.map { it.presentableText }
method.docComment                        // null or PsiDocComment
method.body?.text                        // method body, as text
```

## Kotlin: `KtNamedFunction`

Kotlin functions use `KtNamedFunction` from the Kotlin plugin's PSI, not
`PsiMethod`:

```kotlin
val function = element.parentOfType<KtNamedFunction>() ?: return
function.name
function.valueParameters.map { "${it.typeReference?.text} ${it.name}" }
function.typeReference?.text             // return type
```

## PSI tree shape (example)

```
PsiMethod: "calculateTotal"
├── PsiModifierList: "public"
├── PsiTypeElement: "double"       ← return type
├── PsiIdentifier: "calculateTotal"
├── PsiParameterList
│   ├── PsiParameter: "int count"
│   └── PsiParameter: "double price"
├── PsiReferenceList (throws)
└── PsiCodeBlock { ... }           ← method body
```