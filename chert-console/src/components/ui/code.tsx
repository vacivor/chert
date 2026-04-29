import * as React from "react"
import { ScrollArea } from "@/components/ui/scroll-area"
import { cn } from "@/lib/utils"

function InlineCode({
  className,
  ...props
}: React.ComponentProps<"code">) {
  return (
    <code
      data-slot="inline-code"
      className={cn(
        "rounded-md bg-muted/70 px-1.5 py-0.5 font-mono text-[12px] text-foreground",
        className
      )}
      {...props}
    />
  )
}

type CodeBlockProps = React.ComponentProps<"pre"> & {
  codeClassName?: string
  viewportClassName?: string
  language?: "yaml" | "json" | "properties" | "xml" | "plaintext"
}

function CodeBlock({
  className,
  codeClassName,
  viewportClassName,
  language = "plaintext",
  children,
  ...props
}: CodeBlockProps) {
  const content =
    typeof children === "string" ? children : Array.isArray(children) ? children.join("") : ""
  const lines = content.split("\n")

  return (
    <div
      data-slot="code-block"
      className={cn(
        "overflow-hidden rounded-2xl border border-slate-700 bg-slate-900 text-slate-100",
        className
      )}
    >
      <ScrollArea className="h-[280px] w-full">
        <pre
          data-slot="code-block-pre"
          className={cn(
            "min-w-max bg-transparent py-0 font-mono text-[12px] leading-5 text-slate-100",
            viewportClassName
          )}
          {...props}
        >
          <code
            data-slot="code-block-code"
            className={cn("block min-w-max whitespace-pre", codeClassName)}
          >
            {lines.map((line, index) => (
              <span
                key={`${index}-${line}`}
                className="grid min-w-max grid-cols-[3.25rem_minmax(0,1fr)]"
              >
                <span className="select-none border-r border-slate-700 bg-slate-800 px-3 py-0.5 text-right text-[10px] text-slate-500">
                  {index + 1}
                </span>
                <span className="px-4 py-0.5">
                  {renderHighlightedLine(line, language)}
                </span>
              </span>
            ))}
          </code>
        </pre>
      </ScrollArea>
    </div>
  )
}

function renderHighlightedLine(
  line: string,
  language: CodeBlockProps["language"]
) {
  if (!line) {
    return <span>&nbsp;</span>
  }

  switch (language) {
    case "yaml":
      return highlightYaml(line)
    case "json":
      return highlightJson(line)
    case "properties":
      return highlightProperties(line)
    case "xml":
      return highlightXml(line)
    default:
      return <span>{line}</span>
  }
}

function highlightYaml(line: string) {
  const commentIndex = line.indexOf("#")
  const base = commentIndex >= 0 ? line.slice(0, commentIndex) : line
  const comment = commentIndex >= 0 ? line.slice(commentIndex) : ""
  const keyMatch = base.match(/^(\s*-\s*|\s*)([^:#]+?)(\s*:\s*)(.*)$/)

  if (!keyMatch) {
    return (
      <>
        {highlightScalars(base)}
        {comment ? <CommentToken value={comment} /> : null}
      </>
    )
  }

  const [, indent, key, separator, rawValue] = keyMatch

  return (
    <>
      <span>{indent}</span>
      <KeyToken value={key.trimEnd()} />
      <PunctuationToken value={separator} />
      {highlightScalars(rawValue)}
      {comment ? <CommentToken value={comment} /> : null}
    </>
  )
}

function highlightJson(line: string) {
  const tokens: React.ReactNode[] = []
  const pattern =
    /("(?:[^"\\]|\\.)*")|(\btrue\b|\bfalse\b|\bnull\b)|(-?\b\d+(?:\.\d+)?\b)|([{}[\],:])/g
  let lastIndex = 0
  let match: RegExpExecArray | null

  match = pattern.exec(line)
  while (match) {
    if (match.index > lastIndex) {
      tokens.push(<span key={`plain-${lastIndex}`}>{line.slice(lastIndex, match.index)}</span>)
    }

    const [value, stringToken, booleanToken, numberToken, punctuationToken] = match
    const key = `${match.index}-${value}`

    if (stringToken) {
      const isKey = /^\s*:$/.test(line.slice(match.index + stringToken.length).trimStart().slice(0, 1))
      tokens.push(
        isKey ? <KeyToken key={key} value={stringToken} /> : <StringToken key={key} value={stringToken} />
      )
    } else if (booleanToken) {
      tokens.push(<KeywordToken key={key} value={booleanToken} />)
    } else if (numberToken) {
      tokens.push(<NumberToken key={key} value={numberToken} />)
    } else if (punctuationToken) {
      tokens.push(<PunctuationToken key={key} value={punctuationToken} />)
    }

    lastIndex = match.index + value.length
    match = pattern.exec(line)
  }

  if (lastIndex < line.length) {
    tokens.push(<span key={`tail-${lastIndex}`}>{line.slice(lastIndex)}</span>)
  }

  return <>{tokens}</>
}

function highlightProperties(line: string) {
  if (/^\s*[#!]/.test(line)) {
    return <CommentToken value={line} />
  }

  const separatorIndex = line.search(/\s*[:=]\s*/)
  if (separatorIndex < 0) {
    return <span>{line}</span>
  }

  const separatorMatch = line.slice(separatorIndex).match(/^\s*[:=]\s*/)
  const separator = separatorMatch?.[0] ?? "="
  const key = line.slice(0, separatorIndex)
  const value = line.slice(separatorIndex + separator.length)

  return (
    <>
      <KeyToken value={key} />
      <PunctuationToken value={separator} />
      {highlightScalars(value)}
    </>
  )
}

function highlightXml(line: string) {
  const tokens: React.ReactNode[] = []
  const pattern = /(<\/?|\/?>)|([A-Za-z_:][\w:.-]*)(?=[\s/>])|("(?:[^"\\]|\\.)*")/g
  let lastIndex = 0
  let match: RegExpExecArray | null

  match = pattern.exec(line)
  while (match) {
    if (match.index > lastIndex) {
      tokens.push(<span key={`plain-${lastIndex}`}>{line.slice(lastIndex, match.index)}</span>)
    }

    const [value, punct, tagName, stringToken] = match
    const key = `${match.index}-${value}`

    if (punct) {
      tokens.push(<PunctuationToken key={key} value={punct} />)
    } else if (tagName) {
      tokens.push(<KeyToken key={key} value={tagName} />)
    } else if (stringToken) {
      tokens.push(<StringToken key={key} value={stringToken} />)
    }

    lastIndex = match.index + value.length
    match = pattern.exec(line)
  }

  if (lastIndex < line.length) {
    tokens.push(<span key={`tail-${lastIndex}`}>{line.slice(lastIndex)}</span>)
  }

  return <>{tokens}</>
}

function highlightScalars(value: string) {
  const pattern = /(".*?"|'.*?'|\btrue\b|\bfalse\b|\bnull\b|-?\b\d+(?:\.\d+)?\b)/g
  const tokens: React.ReactNode[] = []
  let lastIndex = 0
  let match: RegExpExecArray | null

  match = pattern.exec(value)
  while (match) {
    if (match.index > lastIndex) {
      tokens.push(<span key={`plain-${lastIndex}`}>{value.slice(lastIndex, match.index)}</span>)
    }

    const token = match[0]
    const key = `${match.index}-${token}`
    if (/^['"]/.test(token)) {
      tokens.push(<StringToken key={key} value={token} />)
    } else if (/^(true|false|null)$/.test(token)) {
      tokens.push(<KeywordToken key={key} value={token} />)
    } else {
      tokens.push(<NumberToken key={key} value={token} />)
    }

    lastIndex = match.index + token.length
    match = pattern.exec(value)
  }

  if (lastIndex < value.length) {
    tokens.push(<span key={`tail-${lastIndex}`}>{value.slice(lastIndex)}</span>)
  }

  return <>{tokens}</>
}

function KeyToken({ value }: { value: string }) {
  return <span className="font-medium text-sky-300">{value}</span>
}

function StringToken({ value }: { value: string }) {
  return <span className="text-emerald-300">{value}</span>
}

function KeywordToken({ value }: { value: string }) {
  return <span className="font-medium text-fuchsia-300">{value}</span>
}

function NumberToken({ value }: { value: string }) {
  return <span className="text-amber-300">{value}</span>
}

function CommentToken({ value }: { value: string }) {
  return <span className="text-slate-500 italic">{value}</span>
}

function PunctuationToken({ value }: { value: string }) {
  return <span className="text-slate-400">{value}</span>
}

export { CodeBlock, InlineCode }
