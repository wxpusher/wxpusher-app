# wxpusher-app 项目约定

继承上级 monorepo 的 AGENTS.md 约定。

## 代码风格（项目记忆）

- `if` / `else`、`for`、`while`、`do-while` 的分支或循环体必须使用花括号，即使只有一条语句或只是 `return`，也不得省略。
- Kotlin 的 `if` 表达式也应为各分支使用花括号；新增和修改代码均遵守本规则，不引入单行无花括号写法。
- 来源：PR #27 审核意见，2026-11-13 确认。
