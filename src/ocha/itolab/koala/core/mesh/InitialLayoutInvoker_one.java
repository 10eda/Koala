package ocha.itolab.koala.core.mesh;
 
import ocha.itolab.koala.core.data.*;
import ocha.itolab.koala.core.forcedirected.*;
 
import java.util.*;
import java.io.*;
 
 
 
/**
 * 元データをLinLogLayoutのデータ構造に変換して座標値を算出し
 * その結果を元データに反映する
 */
public class InitialLayoutInvoker_one {
    static Graph graph;
    static Mesh mesh;
    static Vector edgelist = new Vector();
    static Vector nodelist = new Vector();
    static Vector fixlist = new Vector();
     
    static int LINLOG = 1;
    static int READGML = 2;
    static int method = LINLOG;
    static String path = "/Users/ntoeda/eclipse/LayoutTest/";
    static String filename = "edges_fme.gml";
     
    /**
     * Execute
     */
    public static void exec(Graph g, Mesh m) {
        graph = g;
        mesh = m;
        constructEdge();
 
        
        //writeEdgeFile();
        
        if(method == READGML){
        	try {
                String command = "../OGDF/_examples/layout/energy-based/main";
                Runtime runtime = Runtime.getRuntime();
                Process p = runtime.exec(command);
                int ret = p.waitFor();
                p.destroy();
        	} catch(Exception e) {
                e.printStackTrace();
        	}
        }
        
        if(method == LINLOG)
            LinLogLayout.exec(edgelist, nodelist, null);
        if(method == READGML)
            GmlFileReader.read(edgelist, nodelist, (path + filename));

        calcNodePosition();
         
        // for test
        printEdgeLength();
    }
     
     
    /**
     * Construct edges もともとのconstructEdge
     */
    static void constructEdge_old() {
 
        // Clear
        edgelist.clear();
         
        // for each pair of vertices
        for(int i = 0; i < mesh.getNumVertices(); i++) {
            Vertex v1 = mesh.getVertex(i);
            for(int j = (i + 1); j < mesh.getNumVertices(); j++) {
                Vertex v2 = mesh.getVertex(j);
             
                int count = 0;
                double count_dim = 0;
                ArrayList<Node> nodes1 = v1.getNodes();
                for(int ii = 0; ii < nodes1.size(); ii++) {
                    Node n1 = nodes1.get(ii);
                    ArrayList<Node> nodes2 = v2.getNodes();
                    for(int jj = 0; jj < nodes2.size(); jj++) {
                        Node n2 = nodes2.get(jj);
                        if(graph.isTwoNodeConnected(n1, n2) == true){
                            count++;
                            count_dim += n1.getDisSim2(jj);
                            
                        }
                    }
                }
         
                // Add an edge
                if(count > 0)
                    addEdge(i, j, count_dim);
                    //addEdge(i, j, (double)count);
            }
        }
 
    }
    
    /**
     * Construct edges 出るノード(id+vertexs.num)と入るノード(id+vertexs.num*2)を別につくる
     */
    static void constructEdge() {
 
        // Clear
        edgelist.clear();
         
        // for each pair of vertices
        
        int num_v = mesh.getNumVertices();
        
        for(int i = 0; i < mesh.getNumVertices(); i++) {
            Vertex v1 = mesh.getVertex(i);
            for(int j = (i + 1); j < mesh.getNumVertices(); j++) {
                Vertex v2 = mesh.getVertex(j);
             
                int count_connecting = 0;
                double count_dim_connecting = 0;
                int count_connected = 0;
                double count_dim_connected = 0;
                ArrayList<Node> nodes1 = v1.getNodes();
                for(int ii = 0; ii < nodes1.size(); ii++) {
                    Node n1 = nodes1.get(ii);
                    ArrayList<Node> nodes2 = v2.getNodes();
                    for(int jj = 0; jj < nodes2.size(); jj++) {
                        Node n2 = nodes2.get(jj);
                        //if(graph.isTwoNodeConnected(n1, n2) == true){
                        if(graph.isNodeConnected1to2(n1, n2) == true){
                        	count_connected++;
                        	count_dim_connected += n1.getDisSim2(jj);
                        }
                        //if(graph.isTwoNodeConnected(n2, n1) == true){
                        if(graph.isNodeConnected1to2(n2, n1) == true){
                        	count_connecting++;
                        	count_dim_connecting += n1.getDisSim2(jj);
                        }
                    }
                }
         
                // Add an edge
                if(count_connected > 0)
                	addEdge(i+num_v*2,j+num_v,count_dim_connected);
                if(count_connecting > 0)
                	addEdge(i+num_v,j+num_v*2,count_dim_connecting);
            }
        }
        
        for(int i = 0; i < mesh.getNumVertices(); i++) {
            	double weight = countEdgeWeight(i+num_v);
                if(weight>0)
                	addEdge(i,i+num_v,weight);
                weight = countEdgeWeight(i+num_v*2);
                if(weight>0)
                	addEdge(i,i+num_v*2,weight);
                    	
        }
 
    }
 
     
     
     
    /**
     * Add an edge
     */
    static void addEdge(int id1, int id2, double weight) {
         
        // IDの大小関係を統一する
        if(id1 > id2) {
            int tmp = id1;
            id1 = id2;   id2 = tmp;
        }
         
        // 既存エッジを検索する
        for(int i = 0; i < edgelist.size(); i++) {
            InputEdge ie = (InputEdge)edgelist.elementAt(i);
             
            // 既存エッジが存在するなら、それに重みを加算する
            if(ie.node1 == id1 && ie.node2 == id2) {
                ie.weight += weight;
                return;
            }
        }
         
        // 新規エッジを生成する
        InputEdge ie = new InputEdge();
        ie.node1 = id1;
        ie.node2 = id2;
        ie.weight = weight;
        edgelist.add(ie);
         
    }
    
    static double countEdgeWeight(int id){
    	double count=0;
        for(int i = 0; i < edgelist.size(); i++) {
            InputEdge ie = (InputEdge)edgelist.elementAt(i);
            // 既存エッジが存在するなら、それに重みを加算する
            if(ie.node1 == id || ie.node2 == id) {
            	count+=ie.weight;
            }
        }
    	
    	return count;
    }
     
     
 
    /**
     * ノード座標値を算出する
     */
    static void calcNodePosition() {
        double xmin = 1.0e+30, xmax = -1.0e+30;
        double ymin = 1.0e+30, ymax = -1.0e+30;
        double xa, xb, ya, yb;
         
        // リスト上の各ノードについて：
        //  座標値の最大・最小値を求める
        for(int i = 0; i < nodelist.size(); i++) {
        //for(int i = 0; i < mesh.getNumVertices(); i++) {
            OutputNode on = (OutputNode)nodelist.elementAt(i);
            if(xmin > on.x) xmin = on.x;
            if(xmax < on.x) xmax = on.x;
            if(ymin > on.y) ymin = on.y;
            if(ymax < on.y) ymax = on.y;
        }
 
        // Set the positions
        for(int i = 0; i < nodelist.size(); i++) {
        //for(int i = 0; i < mesh.getNumVertices(); i++) {
            OutputNode on = (OutputNode)nodelist.elementAt(i);
            if(on.id>=mesh.getNumVertices()){
            	int id = on.id-mesh.getNumVertices();
            	if(id>=mesh.getNumVertices()){
            		id = id-mesh.getNumVertices();
            		if(id>=mesh.getNumVertices())
            			continue;
            		mesh.getVertex(id).connectedPos[0] = ((on.x - xmin) / (xmax - xmin)) * 2.0 - 1.0;
            		mesh.getVertex(id).connectedPos[1] = ((on.y - ymin) / (ymax - ymin)) * 2.0 - 1.0;
            	}else{
            		mesh.getVertex(id).connectingPos[0] = ((on.x - xmin) / (xmax - xmin)) * 2.0 - 1.0;
            		mesh.getVertex(id).connectingPos[1] = ((on.y - ymin) / (ymax - ymin)) * 2.0 - 1.0;
            	}
            	continue;
            }
            Vertex v = mesh.getVertex(on.id);
            double x = ((on.x - xmin) / (xmax - xmin)) * 2.0 - 1.0;
            double y = ((on.y - ymin) / (ymax - ymin)) * 2.0 - 1.0;
            v.setPosition(x, y, 0.0);
            ArrayList<Node> nodes = v.getNodes();
            for(int j = 0; j < nodes.size(); j++) {
                    //System.out.println("     " + j + " x=" + x + " y=" + y);
                Node n = nodes.get(j);
                n.setPosition(x, y);
            }
        }   
    }
     
     
    /**
     * for test
     */
    static void writeEdgeFile() {
        BufferedWriter writer;
         
        try {
             writer = new BufferedWriter(
                        new FileWriter(new File(path + "edges.gml")));
             if(writer == null) return;
              
             /*
             for(int i = 0; i < edgelist.size(); i++) {
                 InputEdge ie = (InputEdge)edgelist.get(i);
                 String line = ie.node1 + "," + ie.node2 + "," + ie.weight;
                 writer.write(line, 0, line.length());
                 writer.flush();
                 writer.newLine();
             }
             */
            

             writer.write("graph [", 0, "graph [".length());
             writer.flush();
             writer.newLine();
             writer.write(" Creator \"makegml\" directed 0 label \"\"", 0," Creator \"makegml\" directed 0 label \"\"".length());
             writer.flush();
             writer.newLine();
             
             
             for(int i= 0;i<mesh.getNumVertices();i++){
                 String line = "  node [ id " + i + " ]";
                 writer.write(line, 0, line.length());
                 writer.flush();
                 writer.newLine();
             }  
             
             
             for(int i = 0; i < edgelist.size(); i++) {
                 InputEdge ie = (InputEdge)edgelist.get(i);
                 String line = "edge [ source "+ ie.node1 + " target " + ie.node2 + " ]";
                 writer.write(line, 0, line.length());
                 writer.flush();
                 writer.newLine();
             }

             System.out.println("a");
             
             writer.write("]", 0, "]".length());
             writer.flush();
             writer.newLine();
             
             
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
     
}