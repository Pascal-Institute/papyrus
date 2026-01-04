# AGENTS.md

## Development Principles for Agents

### Core Guideline

**Write code that is intuitive, concise, and self-explanatory.**

Every line of code in an agent should make its intent obvious to anyone reading it — including your future self.

### Key Practices

1. **Be Intuitive**

   - Favor readability over cleverness.
   - Choose straightforward logic and common patterns.
   - Avoid unnecessary abstraction or over-engineering.
   - A new team member should understand the agent's behavior within minutes.

2. **Be Concise**

   - Eliminate redundant code and boilerplate.
   - Use meaningful defaults and sensible shortcuts.
   - Keep functions short and focused (ideally < 30 lines).
   - Remove commented-out code and unused imports.

3. **Make the Code Itself Meaningful**
   - Use descriptive, domain-specific names for variables, functions, and classes.
   - Bad: `data`, `tmp`, `handle_result`
   - Good: `user_query`, `search_results`, `summarize_findings`
   - Structure code so the flow tells the story:
     ```python
     query = extract_user_intent(message)
     results = search_tools(query)
     response = generate_reply(results)
     return response
     ```
