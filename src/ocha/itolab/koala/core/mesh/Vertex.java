package ocha.itolab.koala.core.mesh;

import java.util.*;
import ocha.itolab.koala.core.data.*;

/**
 * Vertex
 */
public class Vertex {
	double pos[] = new double[3];
	double color[] = new double[3];
	ArrayList<Node> nodes = new ArrayList<Node>();
	int id;
	double dissim[];  //dissim[n] : vertexs(n)との類似度(現在は隣接ノード数で計算している)
	double dissim_connected[];
	double dissim_connecting[];
	ArrayList<Bundle> bundleOrder = new ArrayList<Bundle>();
	ArrayList<Vertex> smallVertices = new ArrayList<Vertex>();
	int numMaxLayer=0;
	public double connectingPos[] = new double[3];
	public double connectedPos[] = new double[3];

	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
 	
	public void setPosition(double x, double y, double z) {
		pos[0] = x;   pos[1] = y;   pos[2] = z;
	}
	
	public double[] getPosition() {
		return pos;
	}
	
	public int getNodeNum(){
		return nodes.size();
	}
	
	public void setColor(double r, double g, double b) {
		color[0] = r;   color[1] = g;   color[2] = b;
	}
	
	public double[] getColor() {
		return color;
	}
	
	public ArrayList<Node> getNodes() {
		return nodes;
	}
	
	public double[] getDissim() {
		return dissim;
	}
	
	public ArrayList<Bundle> getOrder(){
		return bundleOrder;
	}

	
	/**
	 * Return the number of layers of nodes
	 */
	public int getNumNodeLayers() {
		for(int i = 0; i < Mesh.LAYER_NUMNODES.length; i++) {
			if(nodes.size() < Mesh.LAYER_NUMNODES[i])
				return (i + 1);
		}
		
		return Mesh.LAYER_NUMNODES.length;
	}

	public void setNumMaxLayer(int num){
		numMaxLayer = num;
	}
	
	public int getNumMaxLayer(){
		return numMaxLayer;
	}

	
	
}
