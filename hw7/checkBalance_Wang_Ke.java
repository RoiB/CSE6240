import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

/**
 * Check if a signed graph is balanced
 * 
 * @author Ke Wang
 *
 */
public class checkBalance_Wang_Ke {

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		int numberOfNodes = sc.nextInt();
		int numberOfEdges = sc.nextInt();
		List<Node> nodes = new ArrayList<Node>();
		for (int i = 0;i != numberOfNodes;i++) {
			nodes.add(new Node(i));
		}
		List<Edge> edges = new ArrayList<Edge>();
		for (int i = 0;i != numberOfEdges;i++) {
			int nodeId1 = sc.nextInt()-1;
			int nodeId2 = sc.nextInt()-1;
			String sign = sc.next();
			Edge newEdge = new Edge(i, nodeId1, nodeId2, sign.equals("+"));
			edges.add(newEdge);
			nodes.get(nodeId1).addEdge(newEdge);
			nodes.get(nodeId2).addEdge(newEdge);
		}
		sc.close();
		
		int index = 0;
		while (index < numberOfNodes) {
			if (nodes.get(index).visited == true) { 
				index++;
				continue; 
			}
			Queue<Node> queue = new LinkedList<Node>();
			Node n = nodes.get(index);
			n.sign = 1;
			n.visited = true;
			queue.add(n);
			while (queue.isEmpty() == false) {
				Node current = queue.poll();
				for (Edge e : current.edges) {
					Node otherEnd = nodes.get(e.getAnotherSide(current.id));
					int signExpected = 0;
					if (e.ifPositive == true) {
						signExpected = current.sign;
					} else {
						signExpected = -current.sign;
					}
					if (otherEnd.visited == false) {
						otherEnd.sign = signExpected;
						otherEnd.visited = true;
						queue.add(otherEnd);
					} else {
						if (otherEnd.sign != signExpected) {
							System.out.println("NO");
							System.exit(1);
						}
					}
				}
			}
			index++;
		}
		
		System.out.println("YES");
	}

}


class Node {
	int id;
	int sign; // 1: positive -1: negative 0: no sign yet
	boolean visited;
	List<Edge> edges;
	
	Node(int id) {
		this.id = id;
		this.sign = 0;
		this.visited = false;
		this.edges = new ArrayList<Edge>();
	}
	
	void addEdge(Edge e) {
		this.edges.add(e);
	}
	
}


class Edge {
	int id;
	int nodeId1;
	int nodeId2;
	boolean ifPositive;
	
	Edge(int id, int nodeId1, int nodeId2, boolean ifPositive) {
		this.id = id;
		this.nodeId1 = nodeId1;
		this.nodeId2 = nodeId2;
		this.ifPositive = ifPositive;
	}
	
	int getAnotherSide(int nodeId) {
		if (nodeId1 == nodeId) {
			return nodeId2;
		} else if (nodeId2 == nodeId) {
			return nodeId1;
		} else {
			return -1;
		}
	}
	
}
