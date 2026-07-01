workspace "Architecture Workbench" "AI-native architecture platform" {

  model {
    architect = person "Lead Architect" "Owns architecture knowledge graph, validation, review, and generated artefacts."
    developer = person "Developer" "Uses generated contracts, AGENTS.md files, and scaffolds."
    platformEngineer = person "Platform Engineer" "Uses generated local/cloud deployment artefacts."
    aiAgent = person "AI Agent" "Consumes controlled MCP tools and generated instructions. Cannot mutate the graph directly."

    workbench = softwareSystem "Architecture Workbench" "Maintains a governed architecture knowledge graph and projects it into design, discovery, review, and delivery artefacts." {
      ui = container "Workbench UI" "React + TypeScript UI for Design Mode, Discovery Mode, validation, healthchecks, review, and projections." "React, TypeScript, Vite"
      api = container "Workbench API" "Backend API for workspace management, graph workflows, validation, healthchecks, projections, and review records." "Java 21, Spring Boot"
      kg = container "Architecture Knowledge Graph" "Canonical graph of architecture elements, relationships, decisions, risks, reviews, ADRs, and evidence." "Java 21"
      validation = container "Validation and Healthcheck Engine" "Runs DDD, architecture, governance, and existing-system healthcheck rules against the graph." "Java 21"
      projection = container "Projection Engine" "Projects the graph into Event Storming, React Flow, C4, BPMN, DMN, ADR, OpenAPI, and AI review views." "Java 21"
      reviewBoard = container "AI Architecture Review Board" "Stores reviewer assessments, disagreements, consensus recommendations, ADR drafts, evidence links, risks, and decision outcomes." "Java 21"
      mcp = container "MCP Agent Collaboration Layer" "Controlled MCP tool boundary for graph context, validation, healthchecks, projections, review proposals, and traceability." "MCP Server"
      generation = container "Generation Engine" "Runs generator plugins for C4, ADRs, OpenAPI, AsyncAPI, BPMN, DMN, docs, infra, and AGENTS.md." "Java 21"
      db = container "PostgreSQL" "Stores workspace metadata, graph snapshots, validation runs, healthcheck runs, review records, and audit log." "PostgreSQL" "Database"
      files = container "Workspace Files" "Generated architecture artefacts and workspace documents." "Git/File System" "File System"
    }

    aiProvider = softwareSystem "AI Provider" "Claude, OpenAI, and Codex providers in later milestones. M3 defines the boundary only." "External"
    gitProvider = softwareSystem "Git Provider" "GitLab initially; GitHub later." "External"
    localstack = softwareSystem "LocalStack" "Local AWS-compatible runtime for generated environments." "External"

    architect -> ui "Designs target architecture, discovers existing systems, validates, reviews, and generates artefacts"
    developer -> ui "Reviews generated service contracts and implementation guidance"
    platformEngineer -> ui "Generates and verifies local deployment artefacts"
    aiAgent -> mcp "Uses controlled tools for graph context, validation, healthchecks, projections, and review proposals"
    aiAgent -> files "Reads generated AGENTS.md, contracts, and implementation tasks"

    ui -> api "Calls REST API"
    api -> kg "Routes graph mutations through validated application services"
    api -> validation "Runs validation"
    api -> projection "Generates projections"
    api -> reviewBoard "Creates and retrieves Review Board records"
    api -> mcp "Exposes controlled agent collaboration tools"
    api -> generation "Runs generators"
    api -> db "Persists metadata and audit history"
    api -> files "Reads/writes workspace artefacts"

    validation -> kg "Reads graph"
    projection -> kg "Reads graph"
    reviewBoard -> kg "Links reviews, decisions, risks, and evidence"
    mcp -> kg "Reads graph context and routes accepted changes through services"
    mcp -> aiProvider "Later invokes reviewer providers through governed adapters"
    generation -> kg "Reads projection inputs"
    generation -> files "Writes generated artefacts"
    api -> gitProvider "Creates repositories, commits artefacts"
    generation -> localstack "Produces local deployment assets for"
  }

  views {
    systemContext workbench "SystemContext" {
      include *
      autolayout lr
    }

    container workbench "Containers" {
      include *
      autolayout lr
    }

    component api "WorkbenchAPIComponents" {
      include *
      autolayout lr
    }

    styles {
      element "Person" {
        shape person
        background #58A6FF
        color #000000
      }
      element "Software System" {
        background #1F3557
        color #E6EDF3
      }
      element "Container" {
        background #161B22
        color #E6EDF3
        stroke #58A6FF
      }
      element "Database" {
        shape cylinder
        background #30363D
        color #E6EDF3
      }
      element "File System" {
        shape folder
        background #30363D
        color #E6EDF3
      }
      element "External" {
        background #484F58
        color #E6EDF3
      }
    }
  }
}
