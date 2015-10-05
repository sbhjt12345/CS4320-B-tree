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
				int i = 1;
				while (i<tmp2.keys.size()){
					if (key.compareTo(tmp2.keys.get(i-1))>=0 && key.compareTo(tmp2.keys.get(i))<0){
						return search_helper((Node<K,T>)tmp2.children.get(i),key);
					}
					i++;
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
		//Situation 1: when root==null
		//the inserting node will be the root
		if (root==null){
			LeafNode<K,T> leaf = new LeafNode<>(key,value);
			root = leaf;
			return;
		}

		Stack<IndexNode<K,T>> trace = new Stack<>();
		LeafNode<K,T> traceLeaf = searchForInsert(root,key,trace);
		traceLeaf.insertSorted(key,value);

		// the stack is used to trace the IndexNode where the inserted entry should be located at.
		if (((Node<K,T>)traceLeaf).isOverflowed()){
			Entry<K, Node<K,T>> righthalf = splitLeafNode(traceLeaf);
			IndexNode<K,T> preNode = trace.isEmpty()?null:trace.pop();
			// next decide where to insert the newly splitted entry
			if (preNode != null) insertHelper(traceLeaf,righthalf,preNode);
			else{
				ArrayList<K> keysss = new ArrayList<>();
				keysss.add(righthalf.getKey());
				ArrayList<Node<K,T>> mychildren = new ArrayList<>();
				mychildren.add(traceLeaf);
				mychildren.add(righthalf.getValue());
				IndexNode<K,T> index = new IndexNode<>(keysss,mychildren);
				root = index;
				return;
			}
			/*	
			if (((Node<K,T>)preNode).isOverflowed() && trace.isEmpty()){
				Entry<K,Node<K,T>> righthalf2 = splitIndexNode(preNode);
			//	preNode = trace.pop();
				insertHelper(traceLeaf,righthalf2,preNode);
			}
			else{
			 */
			while (((Node<K,T>)preNode) !=null && ((Node<K,T>)preNode).isOverflowed()){
				Entry<K,Node<K,T>> righthalf2 = splitIndexNode(preNode);
				if (trace.isEmpty()){
					ArrayList<K> keysss = new ArrayList<>();
					keysss.add(righthalf2.getKey());
					ArrayList<Node<K,T>> mychildren = new ArrayList<>();
					mychildren.add(preNode);
					mychildren.add(righthalf2.getValue());
					IndexNode<K,T> index = new IndexNode<>(keysss,mychildren);
					root = index;
					return;
				}
				IndexNode<K,T> copy = preNode;
				preNode = trace.pop();
				//insertHelper(copy,righthalf2,preNode);
				if (preNode !=null){
					insertHelper(copy,righthalf2,preNode);
				}
			}
		}
		//	}
	}



	// this helper function is used to find out the leafNode where the inserting entry will be in
	private LeafNode<K,T> searchForInsert(Node<K,T> root,K key,Stack<IndexNode<K,T>> trace){

		if (root.isLeafNode) return (LeafNode<K,T>) root;
		else{
			IndexNode<K,T> tmp = (IndexNode<K,T>) root;
			trace.push(tmp);
			if (key.compareTo(tmp.keys.get(0))<0){
				return searchForInsert((Node<K,T>)tmp.children.get(0),key,trace);
			}
			else if (key.compareTo(tmp.keys.get(root.keys.size()-1))>=0){
				return searchForInsert((Node<K,T>)tmp.children.get(tmp.children.size()-1),key,trace);
			}
			else{
				for (int i=1;i<tmp.keys.size();i++){
					if (key.compareTo(tmp.keys.get(i-1))>=0 && key.compareTo(tmp.keys.get(i))<0){
						// key.compareTo(tmp.keys.get(i))>0 ? because no duplicate
						return searchForInsert((Node<K,T>)tmp.children.get(i),key,trace);
					}
				}
			}
		}
		return null;
	}

	private void insertHelper(Node<K,T> preNode,Entry<K,Node<K,T>> righthalf,IndexNode<K,T> grandpreNode){
		if (righthalf.getKey().compareTo(grandpreNode.keys.get(0))<0) {
			grandpreNode.insertSorted(righthalf, 0);
		}
		else if (righthalf.getKey().compareTo(grandpreNode.keys.get(grandpreNode.keys.size()-1))>0){
			grandpreNode.insertSorted(righthalf, grandpreNode.keys.size());
		}
		else{
			for (int i=1;i<grandpreNode.keys.size();i++){
				if (righthalf.getKey().compareTo(grandpreNode.keys.get(i-1))>0 && 
						righthalf.getKey().compareTo(grandpreNode.keys.get(i))<0){
					grandpreNode.insertSorted(righthalf, i);
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
			childrens.add(index.children.get(i+1));
		}
		//		childrens.add(index.children.get(2*D+1));
		for (int i=D;i<=2*D;i++){
			index.keys.remove(index.keys.size()-1);
			index.children.remove(index.children.size()-1);
		}
		//	index.children.remove(D);
		K headKey = keyss.get(0);
		keyss.remove(0);
		IndexNode<K,T> newright = new IndexNode<>(keyss,childrens);
		Entry<K,Node<K,T>> result = new AbstractMap.SimpleEntry<K,Node<K,T>>(headKey,(Node<K,T>) newright);
		return result;

	}

	/**
	 * TODO Delete a key/value pair from this B+Tree
	 * 
	 * @param key
	 */
	public void delete(K key) {
		if (root==null) return;
		int index = -1;
		Stack<IndexNode<K,T>> trace = new Stack<>();
		LeafNode<K,T> tmp = searchForInsert(root,key,trace);    //trace where the node to be deleted is belonged to

		for (int i=0;i<tmp.keys.size();i++){
			if (key==tmp.keys.get(i)){
				tmp.keys.remove(i);
				tmp.values.remove(i);
				break;
			}
		}
		if (((Node<K,T>)tmp).isUnderflowed()){
			IndexNode<K,T> parent = trace.isEmpty()?null:trace.pop();
			if (parent==null) return;
			IndexNode<K,T> grandparent = trace.isEmpty()?null:trace.peek();
			if (grandparent==null){
				if (tmp.keys.get(tmp.keys.size()-1).compareTo(parent.keys.get(0))<0){
					tmp.previousLeaf = null;
					tmp.nextLeaf = (LeafNode<K, T>) parent.children.get(1);
				}
				else if (tmp.keys.get(0).compareTo(parent.keys.get(parent.keys.size()-1))>=0){
					tmp.previousLeaf = (LeafNode<K, T>) parent.children.get(parent.children.size()-2);
					tmp.nextLeaf = null;
				}
				else{
					for (int i=1;i<parent.keys.size();i++){
						if (parent.keys.get(i-1).compareTo(tmp.keys.get(0))<=0 &&
							parent.keys.get(i).compareTo(tmp.keys.get(tmp.keys.size()-1))>0){
							tmp.previousLeaf = (LeafNode<K, T>) parent.children.get(i-1);
							tmp.nextLeaf = (LeafNode<K, T>) parent.children.get(i);
						}
					}
				}
			}
			else{
				// this part handle more complicated situations
				if (tmp.keys.get(tmp.keys.size()-1).compareTo(root.keys.get(0))<0){
					tmp.previousLeaf = null;
					tmp.nextLeaf = (LeafNode<K, T>) parent.children.get(1);
				}
				else if (tmp.keys.get(0).compareTo(root.keys.get(root.keys.size()-1))>=0){
					tmp.nextLeaf = null;
					tmp.previousLeaf =  (LeafNode<K, T>) parent.children.get(parent.children.size()-2);
				}
				else{
					if (tmp.keys.get(tmp.keys.size()-1).compareTo(parent.keys.get(0))<0){
						tmp.nextLeaf = (LeafNode<K, T>) parent.children.get(1);
						for (int i=0;i<grandparent.keys.size()-1;i++){
							if (grandparent.keys.get(i).compareTo(parent.keys.get(0))<=0 &&
								grandparent.keys.get(i+1).compareTo(parent.keys.get(parent.keys.size()-1))>0){
								IndexNode<K,T> preIndex = (IndexNode<K,T>)grandparent.children.get(i);
								tmp.previousLeaf = (LeafNode<K, T>) preIndex.children.get(preIndex.keys.size());
							}
						}
					}
					else if (tmp.keys.get(0).compareTo(parent.keys.get(parent.keys.size()-1))>=0){
						tmp.previousLeaf =  (LeafNode<K, T>) parent.children.get(parent.children.size()-2);
						for (int i=0;i<grandparent.keys.size()-1;i++){
							if (grandparent.keys.get(i).compareTo(parent.keys.get(0))<=0 &&
								grandparent.keys.get(i+1).compareTo(parent.keys.get(parent.keys.size()-1))>0){
								IndexNode<K,T> nextIndex = (IndexNode<K,T>)grandparent.children.get(i+1);
								tmp.previousLeaf = (LeafNode<K, T>) nextIndex.children.get(0);
							}
						}
					}
					else{
						for (int i=0;i<parent.keys.size()-1;i++){
							if(parent.keys.get(i).compareTo(tmp.keys.get(0))<=0 &&
							   parent.keys.get(i+1).compareTo(tmp.keys.get(tmp.keys.size()-1))>0){
								tmp.previousLeaf = (LeafNode<K, T>) parent.children.get(i);
								tmp.nextLeaf=(LeafNode<K, T>) parent.children.get(i+2);
							}
						}
					}
				}		
			}

			int res = handleLeafNodeUnderflow(tmp.previousLeaf,tmp,parent);
            if (res==-1) res = handleLeafNodeUnderflow(tmp,tmp.nextLeaf,parent);
			if (res>=0){
				parent.keys.remove(res);
				parent.children.remove(res+1);
			}

                
			
			while (parent != null && parent.isUnderflowed()){
				IndexNode<K,T> parent2 = trace.isEmpty()?null:trace.pop();
		        if (parent2==null) return;
				if (parent.keys.get(parent.keys.size()-1).compareTo(parent2.keys.get(0))<0){
					index = 0;
				}
				else if (parent.keys.get(0).compareTo(parent2.keys.get(parent2.keys.size()-1))>=0){
					index = parent.keys.size();
				}
				else{
					for (int i=1;i<parent2.keys.size();i++){
						if (parent2.keys.get(i-1).compareTo(parent.keys.get(0))<=0
								&&parent2.keys.get(i).compareTo(parent.keys.get(0))>0){
							index = i;
							break;
						}
					}
				}
				if (index==0){
					IndexNode<K,T> myright = (IndexNode<K, T>) parent2.children.get(1);
					res = handleIndexNodeUnderflow(parent,myright,parent2);
				}
				else{
					IndexNode<K,T> myleft = (IndexNode<K, T>) parent2.children.get(index-1);
					res = handleIndexNodeUnderflow(myleft,parent,parent2);
				}
				parent = parent2;
			}
		}





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
		if(left==null || right==null) return -1;
		int res = -1;
		for (int i=0;i<parent.keys.size();i++){
			if (left.keys.get(left.keys.size()-1).compareTo(parent.keys.get(i))<0 &&
					parent.keys.get(i).compareTo(right.keys.get(0))<=0){
				res = i;
			}
		}
		if (res==-1) return -1;

		if (left.keys.size() + right.keys.size()< 2*D){
			for (int i=0;i<right.keys.size();i++){
				left.keys.add(right.keys.get(i));
				left.values.add(right.values.get(i));
			}

			//			parent.keys.remove(res);
			//			parent.children.remove(res+1);	
			return res;
		}
		else if (right.keys.size()>D){
			left.keys.add(right.keys.get(0));
			left.values.add(right.values.get(0));
			right.keys.remove(0);
			right.values.remove(0);
			parent.keys.remove(res);
			parent.keys.add(res,right.keys.get(0));
			return -2;
		}else if (left.keys.size()>D){
			right.keys.add(0,left.keys.get(left.keys.size()-1));
			left.keys.remove(left.keys.size()-1);
			right.values.add(0,left.values.get(left.values.size()-1));
			left.values.remove(left.values.size()-1);
			parent.keys.remove(res);
			parent.keys.add(res,right.keys.get(0));
			return -2;
		}
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
		if (leftIndex==null || rightIndex==null) return -1;
		int res = -1;
		for (int i=0;i<parent.keys.size();i++){
			if (leftIndex.keys.get(leftIndex.keys.size()-1).compareTo(parent.keys.get(i))<0
					&& rightIndex.keys.get(0).compareTo(parent.keys.get(i))>=0){
				res = i;
			}
		}
		if (res==-1) return res;
		if (leftIndex.keys.size() + rightIndex.keys.size()<2*D){
			leftIndex.keys.add(parent.keys.get(res));
			leftIndex.keys.addAll(rightIndex.keys);
			leftIndex.children.addAll(rightIndex.children);
			parent.keys.remove(res);
			parent.children.remove(res+1);
			if (parent.keys.size()==0) root = leftIndex;
			return res;
		}
		else if (rightIndex.keys.size()>D){
			leftIndex.keys.add(parent.keys.get(res));
			leftIndex.children.add(rightIndex.children.get(0));
			parent.keys.remove(res);
			parent.keys.add(res,rightIndex.keys.get(0));
			rightIndex.keys.remove(0);
			rightIndex.children.remove(0);
			return res;
		}
		else if (leftIndex.keys.size()>D){
			rightIndex.keys.add(0,parent.keys.get(res));
			rightIndex.children.add(0,leftIndex.children.get(leftIndex.children.size()-1));
			parent.keys.remove(res);
			parent.keys.add(res,leftIndex.keys.get(leftIndex.keys.size()-1));
			leftIndex.keys.remove(leftIndex.keys.size()-1);
			leftIndex.children.remove(leftIndex.children.size()-1);
			return res;
		}
		return -1;
	}

}
