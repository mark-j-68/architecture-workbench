import React from 'react'
import ReactDOM from 'react-dom/client'
import { ArchitectureWorkflowShell } from './features/workflow/ArchitectureWorkflowShell'

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <ArchitectureWorkflowShell />
  </React.StrictMode>,
)
