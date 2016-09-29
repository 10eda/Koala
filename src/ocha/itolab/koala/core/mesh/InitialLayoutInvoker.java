package ocha.itolab.koala.core.mesh;

import ocha.itolab.koala.core.data.*;
import ocha.itolab.koala.core.forcedirected.*;

import java.util.*;
import java.io.*;



/**
 * ���f�[�^��LinLogLayout�̃f�[�^�\���ɕϊ����č��W�l���Z�o��
 * ���̌��ʂ����f�[�^�ɔ��f����
 */
public class InitialLayoutInvoker {
	static Graph graph;
	static Mesh mesh;
	static Vector edgelist = new Vector();
	static Vector nodelist = new Vector();
	static Vector largeedgelist = new Vector();
	static Vector fixlist = new Vector();
	
	static int LINLOG = 1;
	static int READGML = 2;
	static int method = LINLOG;
	static String path = "C:/itot/projects/FRUITSNet/Koala/ogdf-layout/";
 	static String filename = "edges_fme.gml";
	
	/**
	 * Execute
	 */
	public static void exec(Graph g, Mesh m) {
		graph = g;
		mesh = m;
		//constructEdge();
		constructEdgeLarge();
		System.out.println("exec edgelist:"+edgelist.size());

		// for test
		writeEdgeFile();
				
		if(method == LINLOG)
			LinLogLayout.exec(edgelist, nodelist, null);
		if(method == READGML)
			GmlFileReader.read(edgelist, nodelist, (path + filename));
		
		
		
		calcNodePosition();
		
		// for test
		//printEdgeLength();
	}
	
	
    /**
     * Construct edges
     */
    static void constructEdge() {
 
        // Clear
        edgelist.clear();
         
        // for each pair of vertices
        for(int i = 0; i < mesh.getNumVertices(); i++) {
            Vertex v1 = mesh.getVertex(i);
            for(int j = (i + 1); j < mesh.getNumVertices(); j++) {
                Vertex v2 = mesh.getVertex(j);
             
                int count = 0;
                ArrayList<Node> nodes1 = v1.getNodes();
                for(int ii = 0; ii < nodes1.size(); ii++) {
                    Node n1 = nodes1.get(ii);
                    ArrayList<Node> nodes2 = v2.getNodes();
                    for(int jj = 0; jj < nodes2.size(); jj++) {
                        Node n2 = nodes2.get(jj);
                        if(graph.isTwoNodeConnected(n1, n2) == true)
                            count++;
                    }
                }
         
                // Add an edge
                if(count > 0)
                    addEdge(i, j, (double)count);
            }
        }
 
    }
	
	/**
	 * Add an edge
	 */
	static void addEdge(int id1, int id2, double weight) {
		
		// ID�̑召�֌W�𓝈ꂷ��
		if(id1 > id2) {
			int tmp = id1;
			id1 = id2;   id2 = tmp;
		}
		
		// �����G�b�W����������
		for(int i = 0; i < edgelist.size(); i++) {
			InputEdge ie = (InputEdge)edgelist.elementAt(i);
			
			// �����G�b�W�����݂���Ȃ�A����ɏd�݂����Z����
			if(ie.node1 == id1 && ie.node2 == id2) {
				ie.weight += weight;
				return;
			}
		}
		
		// �V�K�G�b�W�𐶐�����
		InputEdge ie = new InputEdge();
		ie.node1 = id1;
		ie.node2 = id2;
		ie.weight = weight;
		edgelist.add(ie);
		
	}
	
	
	static void constructEdgeLarge(){	 
	    // Clear
        edgelist.clear();
         
        // for each pair of vertices
        for(int i = 0; i < mesh.getNumVerticesLarge(); i++) {
        	Vertex vertices1 = mesh.getVertexLarge(i);
        	for(int j = (i + 1); j < mesh.getNumVerticesLarge(); j++) {
        		Vertex vertices2 = mesh.getVertexLarge(j);
        		int count = 0;

        		for(int ii=0;ii<vertices1.smallVertices.size();ii++){
        			Vertex v1 = vertices1.smallVertices.get(ii);  
        			for(int jj=0;jj<vertices2.smallVertices.size();jj++){
        				Vertex v2 = vertices2.smallVertices.get(jj);

        				ArrayList<Node> nodes1 = v1.getNodes();
        				for(int iii = 0; iii < nodes1.size(); iii++) {
        					Node n1 = nodes1.get(iii);
        					ArrayList<Node> nodes2 = v2.getNodes();
        					for(int jjj = 0; jjj < nodes2.size(); jjj++) {
        						Node n2 = nodes2.get(jjj);
        						if(graph.isTwoNodeConnected(n1, n2) == true)
        							count++;
        					}
        				}

        			}
        		}
       		// Add an edge
        		if(count > 0)
                    addEdge(i, j, (double)count);
            }
        }
	}

	/**
	 * �m�[�h���W�l���Z�o����
	 */
	static void calcNodePosition() {
		double xmin = 1.0e+30, xmax = -1.0e+30;
		double ymin = 1.0e+30, ymax = -1.0e+30;
		double xa, xb, ya, yb;
		
		// ���X�g��̊e�m�[�h�ɂ��āF
		//  ���W�l�̍ő�E�ŏ��l�����߂�
		for(int i = 0; i < nodelist.size(); i++) {
			OutputNode on = (OutputNode)nodelist.elementAt(i);
			if(xmin > on.x) xmin = on.x;
			if(xmax < on.x) xmax = on.x;
			if(ymin > on.y) ymin = on.y;
			if(ymax < on.y) ymax = on.y;
		}

		// Set the positions
		for(int i = 0; i < nodelist.size(); i++) {
			OutputNode on = (OutputNode)nodelist.elementAt(i);
			Vertex v = mesh.getVertexLarge(on.id);
			double x = ((on.x - xmin) / (xmax - xmin)) * 2.0 - 1.0;
			double y = ((on.y - ymin) / (ymax - ymin)) * 2.0 - 1.0;
			v.setPosition(x, y, 0.0);
			System.out.println("x:"+x+", y:"+y);
			//ArrayList<Node> nodes = v.getNodes();
			smallLayout(v,x,y);
			/*
			for(int j = 0; j < nodes.size(); j++) {
					//System.out.println("     " + j + " x=" + x + " y=" + y);
				Node n = nodes.get(j);
				n.setPosition(x, y);
				//System.out.println("x:"+x+" , y:"+y);
			}
			*/
		}	
	}
	
	/**
	 * for test
	 */
	static void writeEdgeFile() {
		BufferedWriter writer;
		
		try {
			 writer = new BufferedWriter(
			    		new FileWriter(new File("edges.csv")));
			 if(writer == null) return;
			 
			 for(int i = 0; i < edgelist.size(); i++) {
				 InputEdge ie = (InputEdge)edgelist.get(i);
				 String line = ie.node1 + "," + ie.node2 + "," + ie.weight;
				 writer.write(line, 0, line.length());
				 writer.flush();
				 writer.newLine();
			 }
			
			 writer.close();
		
		} catch (Exception e) {
			System.err.println(e);
			writer = null;
			return;
		}
	}
	
	
	
	static void printEdgeLength() {
		BufferedWriter writer;
		
		try {
			 writer = new BufferedWriter(
			    		new FileWriter(new File("edgelength.txt")));
			 if(writer == null) return;
			 
			for(int i = 0; i < edgelist.size(); i++) {
				InputEdge ie = (InputEdge)edgelist.get(i);
				Vertex v1 = mesh.getVertex(ie.node1);
				Vertex v2 = mesh.getVertex(ie.node2);
				double p1[] = v1.getPosition();
				double p2[] = v2.getPosition();
				double dist = (p1[0] - p2[0]) * (p1[0] - p2[0]) + (p1[1] - p2[1]) * (p1[1] - p2[1]);
				String line = "  edge " + i + " weight=" + ie.weight + " length=" + dist;
				writer.write(line, 0, line.length());
				writer.flush();
				writer.newLine();
			}
			
			writer.close();
			
		} catch (Exception e) {
			System.err.println(e);
			writer = null;
			return;
		}
		
		
	}
	
	static void smallLayout(Vertex vert, double x_, double y_){
		/*����:�傫������vert*/
		Vector edgelist_small = new Vector();
		Vector nodelist_small = new Vector();
		constructEdge(vert,edgelist_small);
		//System.out.println("smallLayout edgelist:"+edgelist.size());
		LinLogLayout.exec(edgelist_small, nodelist_small, null);
		
		double xmin = 1.0e+30, xmax = -1.0e+30;
		double ymin = 1.0e+30, ymax = -1.0e+30;
		double xa, xb, ya, yb;
		
		// ���X�g��̊e�m�[�h�ɂ��āF
		//  ���W�l�̍ő�E�ŏ��l�����߂�
		//System.out.println("smallLayout nodelist:"+nodelist.size());
		for(int i = 0; i < nodelist_small.size(); i++) {
			OutputNode on = (OutputNode)nodelist_small.elementAt(i);
			if(xmin > on.x) xmin = on.x;
			if(xmax < on.x) xmax = on.x;
			if(ymin > on.y) ymin = on.y;
			if(ymax < on.y) ymax = on.y;
		}

		// Set the positions
		for(int i = 0; i < nodelist_small.size(); i++) {
			OutputNode on = (OutputNode)nodelist_small.elementAt(i);
			Vertex v = vert.smallVertices.get(on.id);
			double x = ((on.x - xmin) / (xmax - xmin)) * 2.0 - 1.0;
			double y = ((on.y - ymin) / (ymax - ymin)) * 2.0 - 1.0;

			System.out.println("x:"+x+", y:"+y);
			v.setPosition(vert.getPosition()[0] + x*0.1,vert.getPosition()[1] +  y*0.1, 0.0);
			ArrayList<Node> nodes = v.getNodes();
			for(int j = 0; j < nodes.size(); j++) {
					//System.out.println("     " + j + " x=" + x + " y=" + y);
				Node n = nodes.get(j);
				//n.setPosition(vert.getPosition()[0] + x*0.05,vert.getPosition()[1] +  y*0.05);
				n.setPosition(vert.getPosition()[0],vert.getPosition()[1]);
				//System.out.println("x:"+x+" , y:"+y);
			}
		}
	}
	
	static void constructEdge(Vertex vert, Vector edgelist_small) {

		// Clear
		edgelist_small.clear();
		
		// for each pair of vertices
		for(int i = 0; i < vert.smallVertices.size(); i++) {
			Vertex v1 = vert.smallVertices.get(i);
			for(int j = (i + 1); j < vert.smallVertices.size(); j++) {
				Vertex v2 = vert.smallVertices.get(j);
			
				int count = 0;
				ArrayList<Node> nodes1 = v1.getNodes();
				for(int ii = 0; ii < nodes1.size(); ii++) {
					Node n1 = nodes1.get(ii);
					ArrayList<Node> nodes2 = v2.getNodes();
					for(int jj = 0; jj < nodes2.size(); jj++) {
						Node n2 = nodes2.get(jj);
						if(graph.isTwoNodeConnected(n1, n2) == true)
							count++;
					}
				}
		
				// Add an edge
				if(count > 0)
					addEdge_small(i, j, (double)count,edgelist_small);
			}
		}

	}
	
	static void addEdge_small(int id1, int id2, double weight,Vector edgelist_small) {
		
		// ID�̑召�֌W�𓝈ꂷ��
		if(id1 > id2) {
			int tmp = id1;
			id1 = id2;   id2 = tmp;
		}
		
		// �����G�b�W����������
		for(int i = 0; i < edgelist_small.size(); i++) {
			InputEdge ie = (InputEdge)edgelist_small.elementAt(i);
			
			// �����G�b�W�����݂���Ȃ�A����ɏd�݂����Z����
			if(ie.node1 == id1 && ie.node2 == id2) {
				ie.weight += weight;
				return;
			}
		}
		
		// �V�K�G�b�W�𐶐�����
		InputEdge ie = new InputEdge();
		ie.node1 = id1;
		ie.node2 = id2;
		ie.weight = weight;
		edgelist_small.add(ie);
		
	}
	
}

