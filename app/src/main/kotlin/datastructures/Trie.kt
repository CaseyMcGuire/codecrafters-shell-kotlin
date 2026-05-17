package datastructures

class Trie(words: List<String> = emptyList()) {

  private val root = TrieNode()

  init {
    words.forEach { insert(it) }
  }

  fun insert(word: String) {
    var node = root
    for (char in word) {
      node = node.children.getOrPut(char) { TrieNode() }
    }
    node.word = word
  }

  fun getWordsWithPrefix(prefix: String): List<String> {
    val words = mutableListOf<String>()

    fun recurse(curNode: TrieNode) {
      curNode.word?.let { words.add(it) }
      curNode.children.values.forEach { recurse(it) }
    }

    val startingNode = getNodeWithPrefix(prefix)
      ?: return emptyList()
    recurse(startingNode)
    return words
  }


  private fun getNodeWithPrefix(prefix: String): TrieNode? {
    var node = root
    for (char in prefix) {
      node = node.children[char] ?: return null
    }
    return node
  }

  fun getLongestCommonPrefix(str: String): String? {
    var node = getNodeWithPrefix(str)
      ?: return null

    val prefix = StringBuilder()
    while (node.children.size == 1) {
      prefix.append(node.children.keys.first())
      node = node.children.values.first()
    }
    return prefix.toString().takeIf { it.isNotEmpty() }
  }


  internal class TrieNode {
    var children = mutableMapOf<Char, TrieNode>()
    var word: String? = null
  }
}

