/** 将扁平菜单列表按 parentId 组装为树（管理端列表专用） */
export function buildMenuTreeFromFlat(flatList) {
  if (!Array.isArray(flatList) || !flatList.length) {
    return []
  }

  const nodeMap = new Map()
  const roots = []

  for (const item of flatList) {
    nodeMap.set(item.menuId, { ...item, children: [] })
  }

  for (const item of flatList) {
    const node = nodeMap.get(item.menuId)
    const parentId = item.parentId == null ? 0 : Number(item.parentId)
    if (parentId === 0) {
      roots.push(node)
      continue
    }
    const parent = nodeMap.get(parentId)
    if (parent) {
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  }

  sortMenuTreeNodes(roots)
  stripEmptyChildren(roots)
  return roots
}

function sortMenuTreeNodes(nodes) {
  nodes.sort((a, b) => (a.orderNum ?? 0) - (b.orderNum ?? 0))
  for (const node of nodes) {
    if (node.children?.length) {
      sortMenuTreeNodes(node.children)
    }
  }
}

function stripEmptyChildren(nodes) {
  for (const node of nodes) {
    if (node.children?.length) {
      stripEmptyChildren(node.children)
    } else {
      delete node.children
    }
  }
}
