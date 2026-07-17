# LLM Doc Generator

An IntelliJ Platform plugin that generates JavaDoc/KDoc comments for methods
and functions using the Anthropic API (Claude), driven by PSI analysis
rather than plain text matching.

## How it works

1. An `AnAction` (`Ctrl+Alt+D`, or the editor context menu) resolves the
   Java method (`PsiMethod`) or Kotlin function (`KtNamedFunction`) under
   the caret.
2. PSI is used to extract the signature — name, parameters, return type,
   and body — without any regex or manual text parsing.
3. That data is assembled into a prompt and sent to the Anthropic API.
4. The generated JavaDoc/KDoc comment is inserted directly above the
   method/function.

If no API key is configured, or a request fails, the plugin inserts the
prompt itself as a comment instead of failing silently — useful for
inspecting exactly what PSI extracted and what would be sent to the model.

## Requirements

- IntelliJ IDEA (Community is sufficient)
- JDK 17
- An Anthropic API key (see [Configuration](#configuration))

## Getting started

```bash
git clone <repo-url>
cd doc-generator-plugin
```

Open the folder in IntelliJ IDEA as a Gradle project. It will resolve
`org.jetbrains.intellij` (Gradle IntelliJ Plugin) and the IntelliJ Platform
2023.3 distribution automatically.

Run the sandbox IDE with the plugin installed:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew runIde
```

This launches a separate IntelliJ instance with the plugin pre-installed.
Open or create a `.java` or `.kt` file, place the caret inside a method or
function body, and press `Ctrl+Alt+D` (or use the editor context menu).

## Configuration

| Variable               | Required | Default                     | Description        |
|-------------------------|----------|------------------------------|---------------------|
| `ANTHROPIC_API_KEY`     | yes      | —                             | Anthropic API key   |
| `DOC_GENERATOR_MODEL`   | no       | `claude-haiku-4-5-20251001`  | Model ID to use     |

Set these as environment variables before launching `runIde`, or in the
Gradle Run Configuration's environment variables in IntelliJ.

## Architecture

```
src/main/kotlin/com/example/
├── GenerateDocAction.kt   # Entry point: resolves PSI at the caret
├── DocGenerator.kt        # PSI -> prompt -> LLM -> comment insertion
└── LlmClient.kt           # Anthropic Messages API HTTP client
src/main/resources/META-INF/
└── plugin.xml             # Plugin descriptor
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for PSI/AST background and how the
IntelliJ Platform builds and exposes the syntax tree.

## Known limitations

- Java and Kotlin single functions only — no batch generation for a whole
  file or package.
- No Settings UI; configuration is environment-variable only.
- No quick-fix (Alt+Enter) integration.
- Kotlin doc insertion uses direct `Document` text insertion rather than
  `KtPsiFactory`, which is simpler but less PSI-idiomatic.

## License

MIT — see [LICENSE](LICENSE).