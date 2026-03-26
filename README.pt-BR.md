# Prism — IDE Companion for Claude Code

[![Version](https://img.shields.io/badge/version-1.0.1-blue.svg)](https://github.com/VGirotto/prism-claude-code-plugin/releases)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![JetBrains](https://img.shields.io/badge/JetBrains-2024.3+-orange.svg)](https://plugins.jetbrains.com/)

> [Read in English](README.md)

Plugin completo para JetBrains que integra o **Claude Code CLI** diretamente na sua IDE — com interface gráfica, diff view por interação, histórico de conversas e suporte a múltiplas sessões.

Prism é um **wrapper visual local** — ele executa o Claude Code CLI via PTY real e **não faz chamadas externas**. Você precisa ter o CLI instalado e autenticado de forma independente.

> **Aviso:** Este é um plugin da comunidade, não afiliado ou endossado pela Anthropic, PBC. "Claude" e "Claude Code" são marcas da Anthropic, PBC.

---

## Features

### Terminal Interativo
- Terminal completo com Claude Code rodando dentro da IDE
- Suporte total a ANSI colors e formatação de texto
- PTY real (pty4j + JediTerm) para máxima compatibilidade

### Claude Changes Panel
- Diff view automático de todos os arquivos modificados por interação
- Diff side-by-side nativo da IDE (original vs. modificado)
- Revert por arquivo ou por interação completa
- Navegação de histórico entre interações
- Auto-refresh quando Claude termina de responder

### Integração com a IDE
- Menu de contexto: **Explain** / **Review** / **Fix** / **Generate Tests** / **Refactor**
- Enviar seleção diretamente ao Claude
- Referência de arquivo com `@path` no terminal
- Auto-capture de contexto (arquivo ativo, seleção, arquivos abertos)

### Produtividade
- Toolbar compacta com botões de ação rápida
- Dropdowns: Model (opus/sonnet/haiku), Effort (auto/low/medium/high/max)
- [Prompt Templates](docs/prompt-templates.md) reutilizáveis com variáveis `{selection}`, `{file}`, `{language}`
- Atalhos de teclado customizáveis

### Histórico & Sessões
- Browser de histórico de conversas com busca full-text
- Multi-session: múltiplas sessões simultâneas em tabs independentes
- Status bar widget mostrando estado (working/idle/stopped)
- i18n: English, Português, Español

---

## Pré-requisitos

| Requisito | Versão | Notas |
|-----------|--------|-------|
| **IDE JetBrains** | 2024.3+ | IntelliJ IDEA, GoLand, WebStorm, PyCharm, CLion |
| **Claude Code CLI** | 1.0+ | `npm install -g @anthropic-ai/claude-code` |
| **JDK** | 17+ | Apenas para desenvolvimento (IDE fornece JBR) |

---

## Instalação

### Opção 1: Download do Release (Recomendado)

1. Baixar a última versão em [Releases](https://github.com/VGirotto/prism-claude-code-plugin/releases)
2. Na IDE: **Settings > Plugins > Engrenagem > Install Plugin from Disk**
3. Selecionar o arquivo `.zip` baixado
4. **Reiniciar** a IDE
5. O painel "Claude Code" aparece na barra inferior

### Opção 2: Compilar Localmente

```bash
git clone https://github.com/VGirotto/prism-claude-code-plugin.git
cd prism-claude-code-plugin

# Defina JAVA_HOME se não tiver JDK global
export JAVA_HOME="/caminho/para/sua/IDE.app/Contents/jbr/Contents/Home"

./gradlew buildPlugin

# Instalar: Settings > Plugins > Install Plugin from Disk
# Selecione: build/distributions/*.zip
```

---

## Como Usar

### Atalhos de Teclado

| Atalho | Ação | Plataforma |
|--------|------|------------|
| `Cmd+Shift+C` | Abrir/fechar Claude Code | macOS |
| `Alt+Shift+C` | Abrir/fechar Claude Code | Linux/Windows |
| `Ctrl+Shift+D` | Mostrar Claude Changes (diff) | macOS |
| `Ctrl+Alt+Shift+D` | Mostrar Claude Changes (diff) | Linux/Windows |
| `Ctrl+Shift+Enter` | Enviar seleção ao Claude | macOS |
| `Ctrl+Alt+Shift+Enter` | Enviar seleção ao Claude | Linux/Windows |
| `Ctrl+Shift+K` | Inserir referência @arquivo | macOS |
| `Ctrl+Alt+Shift+K` | Inserir referência @arquivo | Linux/Windows |

> No macOS, `Ctrl` = tecla Control física (não Cmd).

### Menu de Contexto (Clique direito no editor)

- **Send Selection to Claude** — enviar texto selecionado
- **Ask Claude...** submenu:
  - Explain this code
  - Review this code
  - Fix this code
  - Generate Tests
  - Refactor this code

### Toolbar

| Botão | Ação |
|-------|------|
| Resume | Retomar última conversa |
| Compact | Compactar contexto da sessão |
| Model | Trocar modelo (opus/sonnet/haiku) |
| Effort | Trocar effort (auto/low/medium/high/max) |
| Cost | Ver estimativa de custo |
| Templates | Usar/criar prompt templates |

### Acessos Rápidos

- **Menu IDE**: `Tools > Toggle Claude Code`
- **Configurações**: `Settings > Tools > Prism — Claude Code`
- **Status Bar**: Clique no widget para abrir o painel Claude

---

## Screenshots

### Terminal Interativo

<img src="docs/images/terminal.png" width="700" />

*Terminal completo com Claude Code, toolbar (Model, Effort, Cost, Resume) e Status Bar widget.*

---

### Claude Changes Panel

<img src="docs/images/changes-panel.png" width="700" />

*Diff side-by-side nativo da IDE com lista de arquivos modificados, revert e navegação de histórico.*

---

### Menu de Contexto

<img src="docs/images/context-menu.png" width="700" />

*Clique direito no editor: Explain, Review, Fix, Generate Tests, Refactor.*

---

### Prompt Templates

<img src="docs/images/templates.png" width="700" />

<img src="docs/images/edit-templates.png" width="700" />

*Templates reutilizáveis com variáveis {selection}, {file}, {language}.*

---

### Histórico de Conversas

<img src="docs/images/history.png" width="700" />

<img src="docs/images/history2.png" width="700" />

*Browser de conversas anteriores com busca full-text.*

---

### Configurações

<img src="docs/images/settings.png" width="700" />

*Claude path, shell, idioma, exclusions, auto-start e toggles.*

---

### Multi-Session

<img src="docs/images/multi-session.png" width="700" />

*Múltiplas sessões simultâneas em tabs independentes.*

---

## Contribuindo

Veja [CONTRIBUTING.md](CONTRIBUTING.md) para setup de desenvolvimento, comandos de build e workflow de contribuição.

Encontrou um bug ou tem uma ideia? Abra uma [Issue](https://github.com/VGirotto/prism-claude-code-plugin/issues).

---

## Documentação

- [Guia de Prompt Templates](docs/prompt-templates.md)
- [Arquitetura & Estrutura do Projeto](docs/architecture.md)

---

## Licença

Apache License 2.0 — veja [LICENSE](LICENSE) para detalhes.
