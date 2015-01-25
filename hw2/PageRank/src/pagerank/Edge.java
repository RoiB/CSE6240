package pagerank;
/**
 * Edge represents a link
 * 
 * @author Ke Wang
 *
 */
public class Edge {
	private Node v;
	private Node w;
	
	public Edge(Node v, Node w) {
		this.v = v;
		this.w = w;
	}

	
	public Node getAnotherNode(Node n) {
		if (n == v) {
			return w;
		}
		if (n == w) {
			return v;
		}
		return null;
	}
}
