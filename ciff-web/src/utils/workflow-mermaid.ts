import type { WorkflowDefinition, StepDefinition } from '@/api/workflow'

/**
 * Convert WorkflowDefinition JSON to Mermaid flowchart syntax.
 */
export function toMermaid(definition: WorkflowDefinition): string {
  if (!definition?.steps?.length) return 'graph TD\n    empty[\"No steps defined\"]'

  const lines: string[] = ['graph TD']
  const stepMap = new Map<string, StepDefinition>()
  for (const step of definition.steps) {
    stepMap.set(step.id, step)
  }

  // node declarations
  for (const step of definition.steps) {
    const safeId = safeName(step.id)
    if (step.type === 'condition') {
      lines.push(`    ${safeId}{${step.name}}`)
    } else {
      lines.push(`    ${safeId}["${step.name}"]`)
    }
  }

  lines.push('')

  // edges from nextStepId (linear flow)
  for (const step of definition.steps) {
    if (step.nextStepId && step.type !== 'condition') {
      lines.push(`    ${safeName(step.id)} --> ${safeName(step.nextStepId)}`)
    }
  }

  // edges from condition rules
  for (const step of definition.steps) {
    if (step.type === 'condition' && step.config?.rules) {
      const rules = step.config.rules as Array<{
        operator: string
        value?: unknown
        nextStepId: string
      }>
      for (const rule of rules) {
        const label = rule.operator === 'default'
          ? '其他'
          : String(rule.value ?? '')
        lines.push(`    ${safeName(step.id)} -->|"${label}"| ${safeName(rule.nextStepId)}`)
      }
    }
  }

  return lines.join('\n')
}

/**
 * Mermaid IDs cannot contain hyphens. Replace with underscores.
 */
function safeName(id: string): string {
  return id.replace(/-/g, '_')
}
