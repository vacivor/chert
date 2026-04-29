import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

export type HeaderBreadcrumbItem = {
  label: string
  href?: string
}

type HeaderBreadcrumbsContextValue = {
  items: HeaderBreadcrumbItem[]
  setItems: (items: HeaderBreadcrumbItem[]) => void
}

const HeaderBreadcrumbsContext = createContext<HeaderBreadcrumbsContextValue | null>(null)

type HeaderBreadcrumbsProviderProps = {
  children: ReactNode
}

export function HeaderBreadcrumbsProvider({ children }: HeaderBreadcrumbsProviderProps) {
  const [items, setItemsState] = useState<HeaderBreadcrumbItem[]>([])

  const setItems = useCallback((nextItems: HeaderBreadcrumbItem[]) => {
    setItemsState((currentItems) => {
      if (JSON.stringify(currentItems) === JSON.stringify(nextItems)) {
        return currentItems
      }

      return nextItems
    })
  }, [])

  const value = useMemo(
    () => ({
      items,
      setItems,
    }),
    [items],
  )

  return (
    <HeaderBreadcrumbsContext.Provider value={value}>
      {children}
    </HeaderBreadcrumbsContext.Provider>
  )
}

export function useHeaderBreadcrumbs(items: HeaderBreadcrumbItem[]) {
  const context = useContext(HeaderBreadcrumbsContext)
  const signature = JSON.stringify(items)
  const setItems = context?.setItems

  useEffect(() => {
    if (!setItems) {
      return
    }

    setItems(items)

    return () => setItems([])
  }, [setItems, signature])
}

export function useHeaderBreadcrumbsContext() {
  const context = useContext(HeaderBreadcrumbsContext)

  if (!context) {
    throw new Error('useHeaderBreadcrumbsContext must be used within HeaderBreadcrumbsProvider')
  }

  return context
}
