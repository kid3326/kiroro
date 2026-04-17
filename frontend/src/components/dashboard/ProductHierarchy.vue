<template>
  <div class="product-hierarchy">
    <div class="hierarchy-header">
      <h3 class="hierarchy-title">상품 계층</h3>
    </div>
    <LoadingSkeleton v-if="loading" variant="table" :rows="6" aria-label="상품 계층 로딩 중" />
    <div v-else class="hierarchy-tree">
      <ul class="tree-list" role="tree">
        <ProductTreeNode
          v-for="node in nodes"
          :key="node.id"
          :node="node"
          :level="0"
          @select="handleSelect"
        />
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'

const ProductTreeNode = defineAsyncComponent(() => import('./ProductTreeNode.vue'))

export interface ProductNode {
  id: number
  name: string
  level: 'category' | 'subcategory' | 'brand' | 'sku'
  revenue: number
  salesVolume: number
  children?: ProductNode[]
}

withDefaults(defineProps<{
  nodes: ProductNode[]
  loading?: boolean
}>(), {
  loading: false,
})

const emit = defineEmits<{
  select: [node: ProductNode]
}>()

function handleSelect(node: ProductNode) {
  emit('select', node)
}
</script>

<style scoped>
.product-hierarchy {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

.hierarchy-header {
  margin-bottom: 1rem;
}

.hierarchy-title {
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.tree-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
</style>
