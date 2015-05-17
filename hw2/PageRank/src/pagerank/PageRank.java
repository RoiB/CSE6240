package pagerank;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Scanner;

/**
 * PageRank
 * 
 * @author Ke Wang
 *
 */
public class PageRank {
	
	public static double PRECISION = 0.000001;
	public static double ALPHA = 1;
	
	public void start() {
		Scanner sc = new Scanner(System.in);
		
		int numberOfNodes = 0;
		int numberOfEdges = 0;
		
		if (sc.hasNextInt()) {
			numberOfNodes = sc.nextInt();
		} else {
			System.err.println("Input Format Error");
			System.exit(1);
		}
		
		if (sc.hasNextInt()) {
			numberOfEdges = sc.nextInt();
		} else {
			System.err.println("Input Format Error");
			System.exit(1);
		}
		
		// create a list of nodes
		// initialize value in Node
		List<Node> nodeList = new ArrayList<Node>();
		for (int i = 1;i <= numberOfNodes;i++) {
			Node newNode = new Node(i);
			newNode.setValue(1.0/numberOfNodes);
			nodeList.add(newNode);
		}
		
		// scan edges
		for (int i = 1;i <= numberOfEdges;i++) {
			int nodeFrom = 0;
			int nodeTo = 0;
			if (sc.hasNextInt()) {
				nodeFrom = sc.nextInt();
			} else {
				System.err.println("Input Format Error");
				System.exit(1);
			}
			if (sc.hasNextInt()) {
				nodeTo = sc.nextInt();
			} else {
				System.err.println("Input Format Error");
				System.exit(1);
			}
			Edge newEdge = new Edge(nodeList.get(nodeFrom-1),nodeList.get(nodeTo-1));
			nodeList.get(nodeFrom-1).addOutgoingEdge(newEdge);
			nodeList.get(nodeTo-1).addIncmongEdge(newEdge);
		}
		
		sc.close();
		
		OptionalDouble maxDifference = OptionalDouble.empty();
		
		while (maxDifference.isPresent()==false || maxDifference.getAsDouble()>PRECISION) {
			
			maxDifference = OptionalDouble.empty();
			
			// reset temp value in nodes
			nodeList.forEach(node -> {
				node.resetTempValue();
			});
			
			// give out weights
			for (Node n : nodeList) {
				int numberOfOutgoingEdges = n.getOutgoingEdges().size();
				if (numberOfOutgoingEdges == 0) {
					double temp = n.getValue() / (numberOfNodes);
					nodeList.forEach(node -> {
						node.addTempValue(temp);
					});
				} else {
					double temp = n.getValue() / numberOfOutgoingEdges;
					n.getOutgoingEdges().forEach(edge -> {
						Node anotherEnd = edge.getAnotherNode(n);
						anotherEnd.addTempValue(temp);
					});
				}
			}
			
			// calculate new weight
			for (Node n : nodeList) {
				n.setTempValue(n.getTempValue()*ALPHA + (1-ALPHA)/numberOfNodes);
				double difference = Math.abs(n.getTempValue() - n.getValue());
				if (maxDifference.isPresent()==false || difference>maxDifference.getAsDouble()) {
					maxDifference = OptionalDouble.of(difference);
				}
				n.setValue(n.getTempValue());
			}
		}
		
		// output final results
		nodeList.forEach(node -> {
			System.out.println(node.getValue());
		});
	}
}
