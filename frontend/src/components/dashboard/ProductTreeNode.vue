<template>
  <li class="tree-node" role="treeitem" :aria-expanded="expanded">
    <div class="tree-node-content" :style="{ paddingLeft: `${level * 1.25}rem` }" @click="toggle">
      <button
        v-if="node.children && node.children.length > 0"
        class="tree-toggle"
        :aria-label="expanded ? '접기' : '펼치기'"
      >
        <i :class="expanded ? 'pi pi-chevron-down' : 'pi pi-chevron-right'"></i>
      </button>
      <span v-else class="tree-toggle-placeholder"></span>
      <span class="tree-node-icon">
        <i :class="levelIcon"></i>
      </span>
      <span class="tree-node-name" @click.stop="$emit('select', node)">{{ node.name }}</span>
      <span class="tree-node-metrics">
        <span class="metric">{{ formatCurrency(node.revenue) }}</span>
        <span class="metric-sep">|</span>
        <span class="metric">{{ node.salesVolume.toLocaleString('ko-KR') }}개</span>
      </span>
    </div>
    <ul v-if="expanded && node.children" class="tree-children" role="group">
      <ProductTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        @select="$emit('select', $event)"
      />
    </ul>
  </li>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

export interface ProductNode {
  id: number
  name: string
  level: 'category' | 'subcategory' | 'brand' | 'sku'
  revenue: number
  salesVolume: number
  children?: ProductNode[]
}

const props = defineProps<{
  node: ProductNode
  level: number
}>()

defineEmits<{
  select: [node: ProductNode]
}>()

const expanded = ref(false)

function toggle() {
  if (props.node.children && props.node.children.length > 0) {
    expanded.value = !expanded.value
  }
}

const levelIcon = computed(() => {
  switch (props.node.level) {
    case 'category': return 'pi pi-folder'
    case 'subcategory': return 'pi pi-folder-open'
    case 'brand': return 'pi pi-tag'
    case 'sku': return 'pi pi-box'
    default: return 'pi pi-circle'
  }
})

function formatCurrency(value: number) {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(value)
}
</script>

<style scoped>
.tree-node {
  list-style: none;
}

.tree-node-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.tree-node-content:hover {
  background-color: #f8fafc;
}

.tree-toggle {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  width: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 0.75rem;
  flex-shrink: 0;
}

.tree-toggle-placeholder {
  width: 1rem;
  flex-shrink: 0;
}

.tree-node-icon {
  color: #64748b;
  font-size: 0.875rem;
  flex-shrink: 0;
}

.tree-node-name {
  font-size: 0.875rem;
  color: #334155;
  font-weight: 500;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-name:hover {
  color: #3b82f6;
}

.tree-node-metrics {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8125rem;
  color: #64748b;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.metric-sep {
  color: #e2e8f0;
}

.tree-children {
  list-style: none;
  padding: 0;
  margin: 0;
}
</style>
