package ocha.itolab.koala.core.mesh;

import java.util.ArrayList;
import ocha.itolab.koala.core.data.Node;
import ocha.itolab.koala.core.data.Graph;

public class BundleList {
	ArrayList<ArrayList<Bundle>> bundlelist;
	Mesh mesh;
	Graph graph;
	
	public BundleList(Graph g, Mesh m) {
		// TODO Auto-generated constructor stub
		graph = g;
		mesh = m;
		
		newBundles();
		setBundles();
		
	}
	
	//meshÇÃêîÇ…çáÇÌÇπÇΩÉäÉXÉgÇÃê∂ê¨
	public void newBundles(){
		bundlelist = new ArrayList<ArrayList<Bundle>>();
		for(int i=0;i<mesh.getNumVertices();i++){
			ArrayList list = new ArrayList();
			for(int j=0;j<mesh.getNumVertices();j++){
				Bundle b = new Bundle(i,j);
				list.add(b);
			}
			bundlelist.add(list);
		}
	}
	
	//
	public void setBundles(){
		/*BundlesÇÃêîéöÇê›íËÇ∑ÇÈ*/
		for(int i = 0;i<mesh.getNumVertices();i++){

			Vertex v1 = mesh.getVertex(i);
			ArrayList<Node> nodes1 = v1.getNodes();
			double v1pos[] = v1.getPosition();

			for(int j = (i+1);j<mesh.getNumVertices();j++){
				Vertex v2 = mesh.getVertex(j);  
				ArrayList<Node> nodes2 = v2.getNodes();
				double v2pos[] = v2.getPosition();
				boolean connected=false;
				boolean connecting=false;
				int num_connected=0;
				int num_connecting=0;
				for(int ii = 0; ii < nodes1.size(); ii++) {
					Node n1 = nodes1.get(ii);
					for(int jj = 0; jj < nodes2.size(); jj++) {
						Node n2 = nodes2.get(jj);
						if(graph.isNodeConnected1to2(n1, n2) == true){
							connecting=true;
							num_connecting++;
						}
						if(graph.isNodeConnected1to2(n2, n1) == true){
							connected=true;
							num_connected++;
						}
					}
				}


				if(connected&connecting){
					mesh.getBundle(i,j).setRotation(1);
					mesh.getBundle(j,i).setRotation(1);
				}


				mesh.getBundle(i, j).setNum(num_connecting, num_connected);
				mesh.getBundle(j, i).setNum(num_connected, num_connecting);             

				if(connected||connecting){
					/*angleÇãÅÇﬂÇÈ*/
					double vec[] = new double[2];
					double angle;
					vec[0] = v2pos[0] - v1pos[0];
					vec[1] = v2pos[1] - v1pos[1];
					angle = (1.0*vec[0])/Math.sqrt((vec[0]*vec[0]+vec[1]*vec[1]));
					/*
					if(vec[1]>0){
						angle = Math.acos(angle);
						mesh.getBundle(j,i).setAngle(angle);
						angle+=Math.PI;
						mesh.getBundle(i,j).setAngle(angle);
					}else{
						angle = Math.acos(angle);
						mesh.getBundle(i,j).setAngle(angle);
						angle+=Math.PI;
						mesh.getBundle(j,i).setAngle(angle);
					}
					*/
					if(vec[1]<0){
						angle = Math.acos(angle);
						mesh.getBundle(i,j).setAngle(angle);
						angle+=Math.PI;
						mesh.getBundle(j,i).setAngle(angle);
					}else{
						angle = 2*Math.PI - Math.acos(angle);
						mesh.getBundle(i,j).setAngle(angle);
						angle-=Math.PI;
						mesh.getBundle(j,i).setAngle(angle);
					}
					//System.out.println(angle);
				}
			}
		}

		/*äevertì‡Ç≈ÇÃbundleÇÃèáî‘Çê›íËÇ∑ÇÈ*/
		for(int i = 0;i<mesh.getNumVertices();i++){
			Vertex vertex = mesh.getVertex(i);
			double angles[] = new double[mesh.getNumVertices()];
			ArrayList<Bundle> bundles = (ArrayList<Bundle>)mesh.getBundles(i);
			for(int j = 0;j<mesh.getNumVertices();j++){  
				Bundle bundle = bundles.get(j);
				angles[j] = bundle.getAngle();
			}
			for(int j = 0;j<mesh.getNumVertices();j++){  
				double buf = 1000;
				int num = -1;
				for(int jj =0;jj<mesh.getNumVertices();jj++){
					if(i==jj)
						continue;
					if(angles[jj]>=0){
						if(angles[jj]<buf){
							num = jj;
							buf=angles[jj];
						}
					}
				}
				if(num==-1){
					// System.out.println(j);
					break;
				}
				angles[num]*=-1;
				vertex.getOrder().add(bundles.get(num));
				//System.out.println(num + " , " + buf);
			}
		}
		
	}
	
	
	private ArrayList<Bundle> getBundles(int i){
		return bundlelist.get(i);
	}
	
	private Bundle getBundle(int i, int j){
		return getBundles(i).get(j);
	}
	
	public int getRotation(int i, int j){
		Bundle bundle = getBundle(i, j);
		return bundle.getRotation();
	}
}
