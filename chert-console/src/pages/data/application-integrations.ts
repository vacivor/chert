import type { LucideIcon } from 'lucide-react'
import {
  AppWindow,
  Bell,
  Blocks,
  Bot,
  Cloud,
  Database,
  FileText,
  GitBranch,
  Mail,
  PenTool,
  Server,
  Workflow,
} from 'lucide-react'

export type ApplicationIntegration = {
  name: string
  description: string
  connected: boolean
  icon: LucideIcon
}

export const applicationIntegrations: ApplicationIntegration[] = [
  {
    name: 'GitHub',
    description: 'Sync repositories, pull request metadata, and release signals.',
    connected: true,
    icon: GitBranch,
  },
  {
    name: 'Figma',
    description: 'Review design references and keep implementation notes aligned.',
    connected: false,
    icon: PenTool,
  },
  {
    name: 'Notification Hub',
    description: 'Fan out console events to subscribed channels and alert rules.',
    connected: true,
    icon: Bell,
  },
  {
    name: 'Mail Relay',
    description: 'Route verification, invite, and recovery messages through one lane.',
    connected: false,
    icon: Mail,
  },
  {
    name: 'Control Plane',
    description: 'Expose environment-level operations through a hardened admin surface.',
    connected: true,
    icon: AppWindow,
  },
  {
    name: 'Workflow Engine',
    description: 'Trigger release pipelines and operator tasks from shared states.',
    connected: false,
    icon: Workflow,
  },
  {
    name: 'Config Blocks',
    description: 'Compose reusable configuration bundles across multiple applications.',
    connected: false,
    icon: Blocks,
  },
  {
    name: 'Runtime Fleet',
    description: 'Inspect deployment groups and their current config consumption.',
    connected: true,
    icon: Server,
  },
  {
    name: 'Storage Index',
    description: 'Track config snapshots, release artifacts, and rollback references.',
    connected: false,
    icon: Database,
  },
  {
    name: 'Agent Worker',
    description: 'Run automation workers for validation, scanning, and housekeeping.',
    connected: false,
    icon: Bot,
  },
  {
    name: 'Cloud Edge',
    description: 'Project selected configuration to remote regions and edge runtimes.',
    connected: true,
    icon: Cloud,
  },
  {
    name: 'Docs Portal',
    description: 'Publish release notes and environment guidance next to live resources.',
    connected: false,
    icon: FileText,
  },
]
