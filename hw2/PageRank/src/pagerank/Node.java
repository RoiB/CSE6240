package pagerank;

import java.util.ArrayList;
import java.util.List;

/**
 * Node represents a webpage 
 * 
 * @author Ke Wang
 *
 */
public class Node {
	private int id;
	private double value;
	private double tempValue;
	private List<Edge> incomingEdges;
	private List<Edge> outgoingEdges;
	
	{
		id = 0;
		value = 0;
		tempValue = 0;
		incomingEdges = new ArrayList<Edge>();
		outgoingEdges = new ArrayList<Edge>();
	}
	
	public Node(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public void updateValue(double value) {
		this.value = value;
	}
	
	public double getTempValue() {
		return this.tempValue;
	}
	
	public void setTempValue(double value) {
		this.tempValue = value;
	}
	
	public void resetTempValue() {
		this.tempValue = 0;
	}
	
	public void addTempValue(double v) {
		this.tempValue += v;
	}
	
	public List<Edge> getIncomingEdges() {
		return incomingEdges;
	}

	public void setIncomingEdges(ArrayList<Edge> incomingEdges) {
		this.incomingEdges = incomingEdges;
	}

	public void addIncmongEdge(Edge edge) {
		this.incomingEdges.add(edge);
	}
	
	public List<Edge> getOutgoingEdges() {
		return outgoingEdges;
	}

	public void setOutgoingEdges(ArrayList<Edge> outgoingEdges) {
		this.outgoingEdges = outgoingEdges;
	}
	
	public void addOutgoingEdge(Edge edge) {
		this.outgoingEdges.add(edge);
	}
	
}
