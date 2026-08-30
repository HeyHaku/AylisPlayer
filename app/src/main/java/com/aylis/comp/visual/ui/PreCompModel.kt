package com.aylis.comp.visual.ui

import com.aylis.comp.visual.core.Elements.Element

sealed class VisualizerItem {
    abstract val id: String
    abstract val index: Int
    abstract val name: String
    abstract val isPreComp: Boolean
    var depth: Int = 0
    var isLastChild: Boolean = false
    var ancestorMask: BooleanArray = BooleanArray(0)
    var isMenuExpanded: Boolean = false
}

data class SimpleItem(
    override val id: String,
    override val index: Int,
    override val name: String
) : VisualizerItem() {
    override val isPreComp: Boolean = false
}

data class PreCompItem(
    override val id: String,
    override val index: Int,
    override val name: String,
    var isExpanded: Boolean = false,
    val children: MutableList<VisualizerItem> = mutableListOf()
) : VisualizerItem() {
    override val isPreComp: Boolean = true
}

object PreCompModelBuilder {

    @JvmStatic
    fun buildTree(
        treeNodes: List<CustomizeMain1.ElementNode>,
        customizationDataList: Element.CustomizationList?,
        expandedIds: Set<String>
    ): List<VisualizerItem> {
        if (customizationDataList == null) return emptyList()

        val nodesByIndex = treeNodes.associateBy { it.index }

        fun findNearestPreCompAncestor(startIndex: Int): Int {
            var currIndex = startIndex
            while (true) {
                val node = nodesByIndex[currIndex] ?: break
                val parentIdx = node.parentIndex
                if (parentIdx <= 0) break
                val parentNode = nodesByIndex[parentIdx] ?: break
                if (parentNode.isPreComp) {
                    return parentIdx
                }
                currIndex = parentIdx
            }
            return -1
        }

        val itemsMap = mutableMapOf<Int, VisualizerItem>()
        for (node in treeNodes) {
            val idx = node.index
            if (idx == 0) continue

            val data = customizationDataList.getData(idx)
            val name = data?.customizationName ?: ""

            if (name.isEmpty() && !node.isPreComp) {
                continue
            }

            val id = node.sceneElement?.id ?: ""
            if (node.isPreComp) {
                val isExpanded = expandedIds.contains(name)
                itemsMap[idx] = PreCompItem(
                    id = id,
                    index = idx,
                    name = name,
                    isExpanded = isExpanded,
                    children = mutableListOf()
                )
            } else {
                itemsMap[idx] = SimpleItem(
                    id = id,
                    index = idx,
                    name = name
                )
            }
        }

        val topLevelItems = mutableListOf<VisualizerItem>()

        for (node in treeNodes) {
            val idx = node.index
            val item = itemsMap[idx] ?: continue

            val parentPreCompIdx = findNearestPreCompAncestor(idx)
            val parentPreCompItem = itemsMap[parentPreCompIdx] as? PreCompItem

            if (parentPreCompItem != null) {
                parentPreCompItem.children.add(item)
            } else {
                topLevelItems.add(item)
            }
        }

        return topLevelItems
    }

    @JvmStatic
    @JvmOverloads
    fun flattenItems(
        items: List<VisualizerItem>,
        depth: Int = 0,
        currentMask: BooleanArray = BooleanArray(0)
    ): List<VisualizerItem> {
        val result = mutableListOf<VisualizerItem>()
        for ((i, item) in items.withIndex()) {
            item.depth = depth
            item.isLastChild = (i == items.size - 1)
            item.ancestorMask = currentMask
            result.add(item)
            if (item is PreCompItem && item.isExpanded) {
                val newMask = currentMask.copyOf(currentMask.size + 1)
                newMask[currentMask.size] = item.isLastChild
                result.addAll(flattenItems(item.children, depth + 1, newMask))
            }
        }
        return result
    }
}
