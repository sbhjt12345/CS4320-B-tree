import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Stack;

/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 * TODO: Rename to BPlusTree
 */
public class BPlusTree<K extends Comparable<K>, T> {

	public Node<K,T> root;
	public static final int D = 2;

	/**
	 * TODO Search the value for a specific key
	 * 
	 * @param key
	 * @return value
	 */
	public T search(K key) {
		return search_helper(root,key);
	}

	public T search_helper(Node<K,T> root, K key){
		if (root.isLeafNode==true) {
			LeafNode<K,T> tmp = (LeafNode<K,T>) root;
			int i = 0;
			while (i<tmp.keys.size()){
				if (key==tmp.keys.get(i)) return tmp.values.get(i);
				i++;
			}
		}
		else{
			IndexNode<K,T> tmp2 = (IndexNode<K,T>) root;
			if (key.compareTo(tmp2.keys.get(0)) < 0){
				return search_helper((Node<K,T>)tmp2.children.get(0),key);
			}
			else if (key.compareTo(tmp2.keys.get(tmp2.keys.size()-1))>0){
				return search_helper((Node<K,T>)tmp2.children.get(tmp2.children.size()-1),key);
			}
			else{
				int i = 0;
				while (i<tmp2.keys.size()){
					if (key.compareTo(tmp2.keys.get(i))>=0 && key.compareTo(tmp2.keys.get(i+1))<0){
						return search_helper((Node<K,T>)tmp2.children.get(i+1),key);
					}
				}
			}
		}
		return null;
	}

	/**
	 * TODO Insert a key/value pair into the BPlusTree
	 * 
	 * @param key
	 * @param value
	 */
	public void insert(K key, T value) {
		if (root==null){
			LeafNode<K,T> leaf = new LeafNode<>(key,value);
			root = leaf;
			return;
		}
		Stack<IndexNode<K,T>> trace = new Stack<>();
		LeafNode<K,T> traceLeaf = searchForInsert(root,key,trace);
		traceLeaf.insertSorted(key,value);
		if (root.isLeafNode){
			if (((Node<K,T>)traceLeaf).isOverflowed()){
				Entry<K, Node<K,T>> righthalf = splitLeafNode(traceLeaf);
				ArrayList<K> keysss = new ArrayList<>();
				keysss.add(righthalf.getKey());
				ArrayList<Node<K,T>> mychildren = new ArrayList<>();
				mychildren.add(traceLeaf);
				mychildren.add(righthalf.getValue());
				IndexNode<K,T> index = new IndexNode<>(keysss,mychildren);
				root = index;
				return;

			}
		}

		// the stack is used to trace the IndexNode where the inserted entry should be located at.
		if (((Node<K,T>)traceLeaf).isOverflowed()){
			Entry<K, Node<K,T>> righthalf = splitLeafNode(traceLeaf);
			IndexNode<K,T> preNode = trace.pop();
			// next decide where to insert the newly splitted entry
			insertHelper(righthalf,preNode);

			while (((Node<K,T>)preNode).isOverflowed() && !trace.isEmpty()){
				Entry<K,Node<K,T>> righthalf2 = splitIndexNode(preNode);
				preNode = trace.pop();
				insertHelper(righthalf2,preNode);
			}
		}
	}

	private LeafNode<K,T> searchForInsert(Node<K,T> root,K key,Stack<IndexNode<K,T>> trace){
		if (root.isLeafNode) return (LeafNode<K,T>) root;
		else{
			IndexNode<K,T> tmp = (IndexNode<K,T>) root;
			trace.push(tmp);
			if (key.compareTo(tmp.keys.get(0))<0){
				return searchForInsert((Node<K,T>)tmp.children.get(0),key,trace);
			}
			else if (key.compareTo(tmp.keys.get(root.keys.size()-1))>0){
				return searchForInsert((Node<K,T>)tmp.children.get(tmp.children.size()-1),key,trace);
			}
			else{
				for (int i=0;i<tmp.keys.size();i++){
					if (key.compareTo(tmp.keys.get(i))>=0 && key.compareTo(tmp.keys.get(i+1))<0){
						// key.compareTo(tmp.keys.get(i))>0 ? because no duplicate
						return searchForInsert((Node<K,T>)tmp.children.get(i+1),key,trace);
					}
				}
			}
		}
		return null;
	}

	private void insertHelper(Entry<K,Node<K,T>> righthalf,IndexNode<K,T> preNode){
		if (righthalf.getKey().compareTo(preNode.keys.get(0))<0) preNode.insertSorted(righthalf, 0);
		else if (righthalf.getKey().compareTo(preNode.keys.get(preNode.keys.size()-1))>0){
			preNode.insertSorted(righthalf, preNode.keys.size());
		}
		else{
			for (int i=0;i<preNode.keys.size();i++){
				if (righthalf.getKey().compareTo(preNode.keys.get(i))>0 && 
						righthalf.getKey().compareTo(preNode.keys.get(i+1))<0){
					preNode.insertSorted(righthalf, i+1);
				}
			}
		}
	}

	/**
	 * TODO Split a leaf node and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 * 
	 * @param leaf, any other relevant data
	 * @return the key/node pair as an Entry
	 */
	public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> leaf) {
		ArrayList<K> keyss = new ArrayList<K>();
		ArrayList<T> valuess = new ArrayList<T>();
		for (int i=D;i<=2*D;i++){
			keyss.add(leaf.keys.get(i));
			valuess.add(leaf.values.get(i));
		}
		for (int i=D;i<=2*D;i++){
			leaf.keys.remove(leaf.keys.size()-1);
			leaf.values.remove(leaf.values.size()-1);
		}
		LeafNode<K,T> newright = new LeafNode<>(keyss,valuess) ;
		Entry<K,Node<K,T>> result = new AbstractMap.SimpleEntry<K,Node<K,T>>(keyss.get(0),(Node<K,T>)newright);
		return result;
	}

	/**
	 * TODO split an indexNode and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 * 
	 * @param index, any other relevant data
	 * @return new key/node pair as an Entry
	 */
	public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> index) {
		ArrayList<K> keyss = new ArrayList<K>();
		ArrayList<Node<K,T>> childrens = new ArrayList<>();
		for (int i=D;i<=2*D;i++){
			keyss.add(index.keys.get(i));
			childrens.add(index.children.get(i));
		}
		childrens.add(index.children.get(2*D+1));
		for (int i=D;i<=2*D;i++){
			index.keys.remove(index.keys.size()-1);
			index.children.remove(index.children.size()-1);
		}
		index.children.remove(D);
		IndexNode<K,T> newright = new IndexNode<>(keyss,childrens);
		Entry<K,Node<K,T>> result = new AbstractMap.SimpleEntry<K,Node<K,T>>(keyss.get(0),(Node<K,T>) newright);
		return result;

	}

	/**
	 * TODO Delete a key/value pair from this B+Tree
	 * 
	 * @param key
	 */
	public void delete(K key) {

	}

	/**
	 * TODO Handle LeafNode Underflow (merge or redistribution)
	 * 
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleLeafNodeUnderflow(LeafNode<K,T> left, LeafNode<K,T> right,
			IndexNode<K,T> parent) {
		return -1;

	}

	/**
	 * TODO Handle IndexNode Underflow (merge or redistribution)
	 * 
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleIndexNodeUnderflow(IndexNode<K,T> leftIndex,
			IndexNode<K,T> rightIndex, IndexNode<K,T> parent) {
		return -1;
	}

}
