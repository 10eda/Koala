package ocha.itolab.koala.core.mesh;

import java.util.*;

import ocha.itolab.koala.core.data.*;

public class Mesh {
	public static double CLUSTER_NODE_DISTANCE = 0.005;
	public static double NODE_EMPHASIS_MAGNITUDE = 0.05;
	public static int[] LAYER_NUMNODES = {6, 18, 36, 60, 90, 126, 168, 216, 270, 330, 396, 468};
	
	ArrayList triangles;
	ArrayList<Vertex> vertices;
	ArrayList<Vertex> verticesLarge;
	BundlesController bundles;
	double minx, miny, maxx, maxy;
	public double keyEmphasis = 0.0;	
	public double rotationStrength = 0.5;
	public double bundleEv = 0;
	public int map[][];
	public int mapSize = 500;
	
	public Mesh() {
		vertices = new ArrayList();
		verticesLarge = new ArrayList();
		triangles = new ArrayList();
		bundles = new BundlesController();
		minx = miny = 1.0e+30;
		maxx = maxy = -1.0e+30;
	}

	public Vertex addOneVertex() {
		Vertex v = new Vertex();
		v.setId(vertices.size());
		vertices.add(v);
		return v;
	}

	public Vertex addOneVertexLarge() {
		Vertex v = new Vertex();
		v.setId(verticesLarge.size());
		verticesLarge.add(v);
		return v;
	}

	public void removeOneVertex(Vertex vertex) {
		int id = vertex.getId();
		vertices.remove(vertex);
		for (int i = id; i < vertices.size(); i++) {
			Vertex v = (Vertex) vertices.get(i);
			v.setId(i);
		}
	}
	
	public void removeOneVertexLarge(Vertex vertex) {
		int id = vertex.getId();
		verticesLarge.remove(vertex);
		for (int i = id; i < verticesLarge.size(); i++) {
			Vertex v = (Vertex) verticesLarge.get(i);
			v.setId(i);
		}
	}

	public ArrayList<Vertex> getVertices() {
		return vertices;
	}
	
	public ArrayList<Vertex> getVerticesLarge() {
		return verticesLarge;
	}

	public Vertex getVertex(int id) {
		return (Vertex) vertices.get(id);
	}
	
	public Vertex getVertexLarge(int id) {
		return (Vertex) verticesLarge.get(id);
	}

	public ArrayList<Bundle> getBundles(int id){
		return (ArrayList<Bundle>) bundles.get(id);
	}

	public Bundle getBundle(int id1, int id2){
		ArrayList list = bundles.get(id1);
		return (Bundle) list.get(id2);
	}
	
	public int getNumVertices() {
		return vertices.size();
	}	
	
	public int getNumVerticesLarge() {
		return verticesLarge.size();
	}


	public Triangle addOneTriangle() {
		Triangle t = new Triangle();
		t.setId(triangles.size());
		triangles.add(t);
		return t;
	}

	public void removeOneTriangle(Triangle triangle) {
		int id = triangle.getId();
		triangles.remove(triangle);
		for (int i = id; i < triangles.size(); i++) {
			Triangle t = (Triangle) triangles.get(i);
			t.setId(i);
		}
	}

	public ArrayList getTriangles() {
		return triangles;
	}

	public Triangle getTriangle(int id) {
		return (Triangle) triangles.get(id);
	}

	public int getNumTriangles() {
		return triangles.size();
	}

	

	/**
	 * Calculate the positions of nodes from the positions of vertices
	 */
	public void finalizePosition() {
		
		// for each vertex
		for(int i = 0; i < vertices.size(); i++) {
			Vertex v = (Vertex)vertices.get(i);
			double pos[] = v.pos;

			// If no nodes are associated
			if(v.nodes.size() <= 0) continue;

			// Copy the position of the vertex if just one node is related
			else if(v.nodes.size() == 1) {
				Node n = (Node)v.nodes.get(0);
				n.setPosition(pos[0], pos[1]);
			}
			
			// for each layer
			double layerDistance = Mesh.CLUSTER_NODE_DISTANCE * (1.0 - keyEmphasis);
			for(int j = 0; j <= LAYER_NUMNODES.length; j++) {
				
				// determine the number of the nodes in this layer
				int nlnodes = 0;
				if(j == 0) {
					nlnodes = (v.nodes.size() <= LAYER_NUMNODES[0]) ? v.nodes.size() : LAYER_NUMNODES[0];
				}
				else if(j < LAYER_NUMNODES.length)
					nlnodes = (v.nodes.size() >= LAYER_NUMNODES[j])
						? (LAYER_NUMNODES[j] - LAYER_NUMNODES[j - 1]) : (v.nodes.size() - LAYER_NUMNODES[j - 1]);
				else
					nlnodes = v.nodes.size() - LAYER_NUMNODES[j - 1];				
				if(nlnodes <= 0) {
					//v.setNumMaxLayer(j);
					break;
				}
				v.setNumMaxLayer(j);
				
				for(int k = 0; k < nlnodes; k++) {
					Node n = (j == 0) ? (Node)v.nodes.get(k) : (Node)v.nodes.get(k + LAYER_NUMNODES[j - 1]);
					double theta = 2.0 * Math.PI * (double)k / (double)nlnodes;
					double dx = Math.cos(theta);
					double dy = Math.sin(theta);
					//System.out.println("    " + j + " theta=" + theta + " x=" + dx + " y=" + dy);
					double x = pos[0] + dx * layerDistance * (double)(j + 1);
					double y = pos[1] + dy * layerDistance * (double)(j + 1);
					n.setPosition(x, y);
				}
			}
			
			
		}
		
	}
	
	public void generateMap(){
		map = new int[mapSize][mapSize];
		
		for(int i=0;i<mapSize;i++){
			for(int j=0;j<mapSize;j++){
				map[i][j]=0;
			}
		}
		
		for(int i = 0; i < vertices.size(); i++) {
			ArrayList<Node> nodes = vertices.get(i).getNodes();
			double maxX = -10;
			double minX = 10;
			double maxY = -10;
			double minY = 10;
			for(int j = 0;j<nodes.size();j++){
				Node node = (Node)nodes.get(j);
				double x = node.getX();
				double y = node.getY();
				if(x>maxX)
					maxX=x;
				if(x<minX)
					minX=x;
				if(y>maxY)
					maxY=y;
				if(y<minY)
					minY=y;
			}
			if(maxX>=minX && maxY>=minY){
				int maxXmap = (int)((maxX+1)/2.0 * mapSize)+3;
				int minXmap = (int)((minX+1)/2.0 * mapSize)-3;	
				int maxYmap = (int)((maxY+1)/2.0 * mapSize)+3;
				int minYmap = (int)((minY+1)/2.0 * mapSize)-3;
				if(maxXmap>mapSize-1) maxXmap = mapSize-1;
				else if(maxXmap<0) maxXmap = 0;
				if(minXmap>mapSize-1) minXmap = mapSize-1;
				else if(minXmap<0) minXmap = 0;
				if(maxYmap>mapSize-1) maxYmap = mapSize-1;
				else if(maxYmap<0) maxYmap = 0;
				if(minYmap>mapSize-1) minYmap = mapSize-1;
				else if(minYmap<0) minYmap = 0;
				for(int x=minXmap; x<=maxXmap; x++){
					for(int y=minYmap;y<=maxYmap; y++){
						if(map[x][y]==0){
							map[x][y]=i;
						}else{
							
						}
						
					}
				}
			}
		}
	}
	
	public void generateMap_node(){
		map = new int[mapSize][mapSize];
		
		for(int i=0;i<mapSize;i++){
			for(int j=0;j<mapSize;j++){
				map[i][j]=0;
			}
		}
		
		for(int i = 0; i < vertices.size(); i++) {
			ArrayList<Node> nodes = vertices.get(i).getNodes();
			for(int j=0;j<nodes.size();j++){
				Node node = (Node)nodes.get(j);
				int x = (int)((node.getX()+1)/2.0 * mapSize);
				int y = (int)((node.getY()+1)/2.0 * mapSize);	
				if(x<0) x=0;
				else if(x>mapSize-1) x=mapSize-1;
				if(y<0) y=0;
				else if(y>mapSize-1) y=mapSize-1;
				//System.out.println(x+", "+y);
				map[x][y]=i;
			}
			
		}
	}
	
	//交差してるエッジの割合を算出(0~1)
	public double judgeCross(double threshold){
		int count=0;
		int count_cross=0;
		int int1=1;
		int int2=1;
		
		for(int i=0; i<getNumVertices(); i++){
			double v1[] = getVertex(i).getPosition();
			for(int j=i+1; j<getNumVertices(); j++){
				if(getVertex(i).dissim[j]>threshold) continue;
				//if(getBundle(i,j).getConnectingNum()==0 && getBundle(i,j).getConnectedNum()==0) continue;
				if(getBundle(i,j).getConnectingNum()>0 && getBundle(i,j).getConnectedNum()>0) int1=2;
				else int1=1;
				double v2[] = getVertex(j).getPosition();
				for(int k=0; k<getNumVertices(); k++){
					if(k==i || k==j) continue;
					double v3[] = getVertex(k).getPosition();
					for(int l=k+1; l<getNumVertices(); l++){
						if(getVertex(k).dissim[l]>threshold) continue;
						//if(getBundle(k,l).getConnectingNum()==0 && getBundle(k,l).getConnectedNum()==0) continue;
						if(getBundle(k,l).getConnectingNum()>0 && getBundle(k,l).getConnectedNum()>0) int2=2;
						else int2=1;
						if(l==i || l==j) continue;
						double v4[] = getVertex(l).getPosition();
						count+=int1*int2;
						//System.out.println(v1[0]+","+v1[1]+","+v2[0]+","+v2[1]+","+v3[0]+","+v3[1]+","+v4[0]+","+v4[1]);
						if(judgeCrossOne(v1[0],v1[1],v2[0],v2[1],v3[0],v3[1],v4[0],v4[1])){
							//System.out.println("cross!");
							count_cross+=int1+int2;
						}
					}
				}
			}
		}
		
		System.out.println(count_cross+" "+count);
		if(count==0)
			return 0;
		else
			return (double)count_cross/(double)count;
	}
	
	public boolean judgeCrossOne(double ax,double ay,double bx,double by,double cx,double cy,double dx,double dy){
		double ta = (cx - dx) * (ay - cy) + (cy - dy) * (cx - ax);
		double tb = (cx - dx) * (by - cy) + (cy - dy) * (cx - bx);
		double tc = (ax - bx) * (cy - ay) + (ay - by) * (ax - cx);
		double td = (ax - bx) * (dy - ay) + (ay - by) * (ax - dx);
		
		return tc * td < 0 && ta * tb < 0;
	}

}
