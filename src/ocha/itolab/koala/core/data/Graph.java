package ocha.itolab.koala.core.data;

import java.util.*;

import mdsj.MDSJ;
import ocha.itolab.koala.core.mesh.*;


public class Graph {
	public int attributeType = -1;
	public static int ATTRIBUTE_VECTOR = 1;
	public static int ATTRIBUTE_DISSIM = 2;

	public String vectorname[];
	public double clustersizeRatio = 0.5;
	public double clustersizeRatio2 = 0.7;
	public int maxDegree = 0;

	public ArrayList<Node> nodes = new ArrayList<Node>();
	public ArrayList<Edge> edges = new ArrayList<Edge>();	
	HashMap edgemap; 
	public Mesh mesh = null;

	double edgeDensityThreshold = 0.1;

	double edgeConfluenceThreshold = 0.5;
	
	int map[][];


	public void postprocess() {
		generateEdges();
		mesh = MeshGenerator.generate(this);
		mesh.newBundles();
		this.setBundles();		//meshのbundleクラスの整理
		postBundleEv();
	}

	public void generateEdges() {
		edges.clear();
		edgemap = new HashMap();

		// for each node
		for(int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);

			// for each node referred node
			for(int j = 0; j < node.connected.length; j++) {
				int id = node.connected[j];
				String key = Integer.toString(node.id) + "-" + Integer.toString(id);
				Edge e = (Edge)edgemap.get(key);
				if(e == null) {
					e = new Edge();
					e.id = edges.size();
					edges.add(e);
					Node node2 = nodes.get(id);
					e.nodes[0] = node;
					e.nodes[1] = node2;
					node.connectedEdge.add(e);
					node2.connectingEdge.add(e);
				}
			}

			// for each node referred node
			for(int j = 0; j < node.connecting.length; j++) {
				int id = node.connecting[j];
				String key = Integer.toString(id) + "-" + Integer.toString(node.id);
				Edge e = (Edge)edgemap.get(key);
				if(e == null) {
					e = new Edge();
					e.id = edges.size();
					edges.add(e);
					Node node2 = nodes.get(id);
					e.nodes[0] = node2;
					e.nodes[1] = node;
					node2.connectedEdge.add(e);
					node.connectingEdge.add(e);
				}
			}


		}

		edgemap.clear();

		// Specify the max number of degrees
		maxDegree = 0;
		for(int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			int nc = node.getNumConnectedEdge() + node.getNumConnectingEdge();
			maxDegree = (maxDegree < nc) ? nc : maxDegree;
		}

	}

	public void setupDissimilarityForPlacement() {
		// Copy dissimilarity to an allocated array
		for(int i = 0; i < nodes.size(); i++) {
			Node n1 = nodes.get(i);
			n1.dissim2 = new double[nodes.size()];
		}
		for(int i = 0; i < nodes.size(); i++) {
			Node n1 = nodes.get(i);
			for(int j = (i + 1); j < nodes.size(); j++) {
				Node n2 = nodes.get(j);
				double d = NodeDistanceCalculator.calcPlacementDistance(this, n1, n2);
				n1.dissim2[j] = n2.dissim2[i] = d;
			}
		}
	}


	public void setupDissimilarityForClustering(int mode) {
		// Copy dissimilarity to an allocated array
		for(int i = 0; i < nodes.size(); i++) {
			Node n1 = nodes.get(i);
			n1.dissim2 = new double[nodes.size()];
		}
		for(int i = 0; i < nodes.size(); i++) {
			Node n1 = nodes.get(i);
			for(int j = (i + 1); j < nodes.size(); j++) {
				Node n2 = nodes.get(j);
				double d = NodeDistanceCalculator.calcClusteringDistance(this, n1, n2, mode);
				n1.dissim2[j] = n2.dissim2[i] = d;
			}
		}

	}



	public boolean isTwoNodeConnected(Node n1, Node n2) {

		for(int i = 0; i < n1.connected.length; i++) {
			int id1 = n1.connected[i];
			if(id1 == n2.id) return true;
		}
		for(int i = 0; i < n2.connected.length; i++) {
			int id2 = n2.connected[i];
			if(id2 == n1.id) return true;
		}
		for(int i = 0; i < n1.connecting.length; i++) {
			int id1 = n1.connecting[i];
			if(id1 == n2.id) return true;
		}
		for(int i = 0; i < n2.connecting.length; i++) {
			int id2 = n2.connecting[i];
			if(id2 == n1.id) return true;
		}

		return false;
	}


	public boolean isNodeConnected1to2(Node n1, Node n2) {
		for(int i = 0; i < n1.connected.length; i++) {
			int id1 = n1.connected[i];
			if(id1 == n2.id) return true;
		}
		for(int i = 0; i < n2.connecting.length; i++) {
			int id2 = n2.connecting[i];
			if(id2 == n1.id) return true;
		}

		return false;
	}


	public void setBundles(){
		/*Bundlesの数字を設定する*/
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
						if(this.isNodeConnected1to2(n1, n2) == true){
							connecting=true;
							num_connecting++;
						}
						if(this.isNodeConnected1to2(n2, n1) == true){
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
					/*angleを求める*/
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

		/*各vert内でのbundleの順番を設定する*/
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



	public double valueGraph(){
		return 1.0;
	}

	public void postBundleEv(){
		mesh.bundleEv=0;

		int nums[] = new int[mesh.getNumVertices()];

		for(int i = 0;i<mesh.getNumVertices();i++){
			nums[i] = 0;
			int n = 0;
			//Vertex vert = mesh.getVertex(i);
			ArrayList<Node> nodes = mesh.getVertex(i).getNodes();

			for(int j=0;j<nodes.size();j++){
				Node node = nodes.get(j);
				n += node.getNumConnectedEdge();
				n += node.getNumConnectingEdge();
				nums[i] += n;
			}
		}


		for(int i = 0;i<mesh.getNumVertices();i++){
			//System.out.println("postBundleEv : "+i);
			//mesh.bundleEv+=VertexEv(i);
			int buf_num=0;
			int buf_no=-1;			
			for(int j = 0;j<mesh.getNumVertices();j++){
				if(buf_num<nums[j]){
					buf_num=nums[j];
					buf_no=j;
				}
			}
			if(buf_no==-1){
				return;
			}
			nums[buf_no]=0;	
			mesh.bundleEv+=PostVertexEv(buf_no);
			//System.out.println(buf_no);
		}
	}

	public double PostVertexEv(int num){
		Vertex vertex = mesh.getVertices().get(num);
		ArrayList<Bundle> bundles = vertex.getOrder();
		boolean connecting=true;
		double dissim[] = mesh.getVertex(num).getDissim();
		double value=0;
		//edgeDensityThreshold = 0.2;
		//System.out.println("Ev : "+bundles.size());
		if(bundles.size()<2)
			return 0.0;
		//for(int i=0;i<bundles.size();i++){
		for(int i=1;i<bundles.size();i++){
			Bundle b1 = bundles.get(i);
			//if(dissim[b1.getID2()]!=1)
			//System.out.println(dissim[b1.getID2()]);
			//System.out.println("i:"+i);
			if(dissim[b1.getID2()] > edgeDensityThreshold)
				continue;
			Bundle b2 = null;
			//for(int j=(i+1)%bundles.size();j==i;j=(j+1)%bundles.size()){
			//for(int j=i+1;j<bundles.size();j++){
			for(int j=i-1;j>=0;j--){
				//System.out.println("j:"+j);
				//System.out.println("dissim : "+dissim[bundles.get(j).getID2()]);
				if(dissim[bundles.get(j).getID2()] > edgeDensityThreshold)
					continue;
				else{
					b2 = bundles.get(j);
					break;
				}
			}
			if(b2==null)
				continue;




			/*2本のバンドルの評価*/
			Vertex v1 = mesh.getVertices().get(b1.getID2());
			Vertex v2 = mesh.getVertices().get(b2.getID2());

			double pos1[] = new double[2];
			if(b1.getConnectedMerge() == null){
				pos1[0] = v1.getPosition()[0] - vertex.getPosition()[0];
				pos1[1] = v1.getPosition()[1] - vertex.getPosition()[1];
			}else{
				pos1[0]=0;
				pos1[1]=0;
				for(int b = 0;b<b1.getConnectedMerge().size();b++){
					pos1[0] += mesh.getVertex(b1.getConnectedMerge().get(b)).getPosition()[0];
					pos1[1] += mesh.getVertex(b1.getConnectedMerge().get(b)).getPosition()[1];
				}
				pos1[0] /= b1.getConnectedMerge().size();
				pos1[1] /= b1.getConnectedMerge().size();
			}

			double pos2[] = new double[2];
			if(b2.getConnectedMerge() == null){
				pos2[0] = v2.getPosition()[0] - vertex.getPosition()[0];
				pos2[1] = v2.getPosition()[1] - vertex.getPosition()[1];
			}else{
				pos2[0]=0;
				pos2[1]=0;
				for(int b = 0;b<b2.getConnectedMerge().size();b++){
					pos2[0] += mesh.getVertex(b2.getConnectedMerge().get(b)).getPosition()[0];
					pos2[1] += mesh.getVertex(b2.getConnectedMerge().get(b)).getPosition()[1];
				}
				pos2[0] /= b2.getConnectedMerge().size();
				pos2[1] /= b2.getConnectedMerge().size();
			}

			double ev = BundleEv(pos1,pos2);

			double pos1_[] = new double[2];
			pos1_[0] = v1.getPosition()[0] - vertex.getPosition()[0];
			pos1_[1] = v1.getPosition()[1] - vertex.getPosition()[1];			

			double pos2_[] = new double[2];
			pos2_[0] = v2.getPosition()[0] - vertex.getPosition()[0];
			pos2_[1] = v2.getPosition()[1] - vertex.getPosition()[1];

			ev = BundleEv_(pos1,pos2);

			//System.out.println(ev);

			if(ev<edgeConfluenceThreshold)
				continue;


			//System.out.println(ev);
			//System.out.println("BundleEv");

			/*2本の向きの決定・逆向きの場合はcontinue*/
			int b1_rote = b1.getRotation();
			int b2_rote = b2.getRotation();
			boolean b1_connecting = b1.getConnectingNum()>0;
			boolean b1_connected = b1.getConnectedNum()>0;
			boolean b2_connecting = b2.getConnectingNum()>0;
			boolean b2_connected = b2.getConnectedNum()>0;



			if((b1.getConnectingMerge()==null && b1.getConnectedMerge()==null) && (b1_connected && b1_connecting)){
				//双方向だけど何ともバンドリングしてない場合->rotationが変えられる

				if((b2.getConnectingMerge()==null && b2.getConnectedMerge()==null) && (b2_connected && b2_connecting)){
					//b2も双方向かつ何ともバンドリングしてない場合->より本数の多い方をバンドリング
					if(b1.getConnectingNum()+b2.getConnectingNum()>b1.getConnectedNum()+b2.getConnectedNum()){
						connecting = true;
						b1.setRotation(1);
						mesh.getBundle(b1.getID2(), b1.getID1()).setRotation(1);
						b2.setRotation(-1);
						mesh.getBundle(b2.getID2(), b2.getID1()).setRotation(-1);
					}else{
						connecting = false;
						b1.setRotation(-1);
						mesh.getBundle(b1.getID2(), b1.getID1()).setRotation(-1);
						b2.setRotation(1);
						mesh.getBundle(b2.getID2(), b2.getID1()).setRotation(1);
					}
					
				}else{
					//b2はrotationが変えられない場合
					if(b2_connecting && (!b2_connected || b2_rote==-1)){
						connecting = true;
						b1.setRotation(1);
						mesh.getBundle(b1.getID2(), b1.getID1()).setRotation(1);
					}else if(b2_connected && (!b2_connecting || b2_rote==1)){
						connecting = false;
						b1.setRotation(-1);
						mesh.getBundle(b1.getID2(), b1.getID1()).setRotation(-1);
					}else{
						continue;
					}
				}

				ArrayList<Integer> b1_merge;
				ArrayList<Integer> b2_merge;

				if(connecting){
					b1_merge = b1.getConnectingMerge();
					b2_merge = b2.getConnectingMerge();
					if(b1_merge==null && b2_merge==null){
						ArrayList<Integer> array = new ArrayList<Integer>();
						array.add(b1.getID2());
						array.add(b2.getID2());
						b1.setConnectingMerge(array);
						b2.setConnectingMerge(array);
						//System.out.println( "nullnull : "+b1.getID2()+","+b2.getID2());
					}else if(b1_merge==null){
						b2_merge.add(b1.getID2());
						b1.setConnectingMerge(b2_merge);
						//System.out.println( "null1 : "+b1.getID2()+","+b2.getID2());
					}else if(b2_merge==null){
						b1_merge.add(b2.getID2());
						b2.setConnectingMerge(b1_merge);
						//System.out.println( "null2 : "+b1.getID2()+","+b2.getID2());
					}else{
						for(int l=0;l<b2_merge.size();l++){
							int n = b2_merge.get(l);
							if(b1_merge.indexOf(n) == -1){
								b1_merge.add(n);
							}
							mesh.getBundle(b1.getID1(),n).setConnectingMerge(b1_merge);
						}
						//System.out.println( "12 : "+b1.getID2()+","+b2.getID2());
					}
				}else{
					b1_merge = b1.getConnectedMerge();
					b2_merge = b2.getConnectedMerge();	
					if(b1_merge==null && b2_merge==null){
						ArrayList<Integer> array = new ArrayList<Integer>();
						array.add(b1.getID2());
						array.add(b2.getID2());
						b1.setConnectedMerge(array);
						b2.setConnectedMerge(array);
					}else if(b1_merge==null){
						b2_merge.add(b1.getID2());
						b1.setConnectedMerge(b2_merge);
					}else if(b2_merge==null){
						b1_merge.add(b2.getID2());
						b2.setConnectedMerge(b1_merge);
					}else{
						for(int l=0;l<b2_merge.size();l++){
							int n = b2_merge.get(l);
							if(b1_merge.indexOf(n) == -1){
								b1_merge.add(n);
							}
							mesh.getBundle(b1.getID1(),n).setConnectedMerge(b1_merge);
						}
					}

				}

			}else{
				//どれかとくっついてるから回転できない場合
				if((b2.getConnectingMerge()==null && b2.getConnectedMerge()==null) && (b2_connected && b2_connecting)){
					//b2(だけ)が双方向かつ何ともバンドリングしてない場合
					if(b1_rote==0 && b1_connecting){
						connecting =true;
						b2.setRotation(-1);
						mesh.getBundle(b2.getID2(), b2.getID1()).setRotation(-1);
					}else if(b1_rote==0 && !b1_connecting){
						connecting = false;
						b2.setRotation(1);
						mesh.getBundle(b2.getID2(), b2.getID1()).setRotation(1);
					}else if(b1_rote==1){
						connecting = true;
						b2.setRotation(-1);
						mesh.getBundle(b2.getID2(), b2.getID1()).setRotation(-1);	
					}else if(b1_rote==-1){
						connecting = false;
						b2.setRotation(1);
						mesh.getBundle(b2.getID2(), b2.getID1()).setRotation(1);
					}
				}else if(b2_rote==0 && b1_rote==0 && b2_connecting && b1_connecting){
					connecting = true;
				}else if(b2_rote==0 && b1_rote==0 && !b2_connecting && !b1_connecting){
					connecting = false;
				}else if(b2_rote==-1 && b1_rote==0 && b1_connecting){
					connecting = true;				
				}else if(b2_rote==1 && b1_rote==0 && !b1_connecting){
					connecting = false;				
				}else if(b1_rote==1 && b2_rote==0 && b2_connecting){
					connecting = true;					
				}else if(b1_rote==-1 && b2_rote==0 && !b2_connecting){
					connecting = false;					
				}else if(b2_rote==-1 && b1_rote==1){
					connecting = true;
				}else if(b2_rote==1 && b1_rote==-1){
					connecting = false;				
				}else{
					continue;
				}

				ArrayList<Integer> b1_merge;
				ArrayList<Integer> b2_merge;

				if(connecting){
					b1_merge = b1.getConnectingMerge();
					b2_merge = b2.getConnectingMerge();
					if(b1_merge==null && b2_merge==null){
						ArrayList<Integer> array = new ArrayList<Integer>();
						array.add(b1.getID2());
						array.add(b2.getID2());
						b1.setConnectingMerge(array);
						b2.setConnectingMerge(array);
						//System.out.println( "nullnull : "+b1.getID2()+","+b2.getID2());
					}else if(b1_merge==null){
						b2_merge.add(b1.getID2());
						b1.setConnectingMerge(b2_merge);
						//System.out.println( "null1 : "+b1.getID2()+","+b2.getID2());
					}else if(b2_merge==null){
						b1_merge.add(b2.getID2());
						b2.setConnectingMerge(b1_merge);
						//System.out.println( "null2 : "+b1.getID2()+","+b2.getID2());
					}else{
						for(int l=0;l<b2_merge.size();l++){
							int n = b2_merge.get(l);
							if(b1_merge.indexOf(n) == -1){
								b1_merge.add(n);
							}
							mesh.getBundle(b1.getID1(),n).setConnectingMerge(b1_merge);
						}
						//System.out.println( "12 : "+b1.getID2()+","+b2.getID2());
					}
				}else{
					b1_merge = b1.getConnectedMerge();
					b2_merge = b2.getConnectedMerge();	
					if(b1_merge==null && b2_merge==null){
						ArrayList<Integer> array = new ArrayList<Integer>();
						array.add(b1.getID2());
						array.add(b2.getID2());
						b1.setConnectedMerge(array);
						b2.setConnectedMerge(array);
					}else if(b1_merge==null){
						b2_merge.add(b1.getID2());
						b1.setConnectedMerge(b2_merge);
					}else if(b2_merge==null){
						b1_merge.add(b2.getID2());
						b2.setConnectedMerge(b1_merge);
					}else{
						for(int l=0;l<b2_merge.size();l++){
							int n = b2_merge.get(l);
							if(b1_merge.indexOf(n) == -1){
								b1_merge.add(n);
							}
							mesh.getBundle(b1.getID1(),n).setConnectedMerge(b1_merge);
						}
					}
				}
			}
			//continue;


		}


		return 0.0;
	}

	public double VertexEv(int num){
		Vertex vertex = mesh.getVertices().get(num);
		ArrayList<Bundle> bundles = vertex.getOrder();
		boolean connecting=true;
		double dissim[] = mesh.getVertex(num).getDissim();
		double value=0;
		//double edgeDensityThreshold = 0.2;
		//System.out.println("Ev : "+bundles.size());
		if(bundles.size()<2)
			return 0.0;
		for(int i=0;i<bundles.size();i++){
			Bundle b1 = bundles.get(i);
			if(dissim[b1.getID2()] > edgeDensityThreshold)
				continue;

			Bundle b2 = null;
			//for(int j=(i+1)%bundles.size();j==i;j=(j+1)%bundles.size()){
			for(int j=i+1;j<bundles.size();j++){
				//System.out.println("dissim : "+dissim[bundles.get(j).getID2()]);
				if(dissim[bundles.get(j).getID2()] > edgeDensityThreshold)
					continue;			
				else{
					b2 = bundles.get(j);
					break;
				}
			}
			if(b2==null)
				continue;


			//System.out.println("BundleEv");

			/*2本の向きの決定・逆向きの場合はcontinue*/
			int b1_rote = b1.getRotation();
			int b2_rote = b2.getRotation();
			boolean b1_connecting = b1.getConnectingNum()>0;
			boolean b2_connecting = b2.getConnectingNum()>0;
			if(b1_rote==0 && b2_rote==0 && b1_connecting && b2_connecting){
				connecting = true;
				//System.out.println("BundleEv1");
			}else if(b1_rote==0 && b2_rote==0 && !b1_connecting && !b2_connecting){
				connecting = false;
				//System.out.println("BundleEv2");
			}else if(b1_rote==1 && b2_rote==0 && b2_connecting){
				connecting = true;				
				//System.out.println("BundleEv3");
			}else if(b1_rote==-1 && b2_rote==0 && !b2_connecting){
				connecting = false;				
				//System.out.println("BundleEv4");
			}else if(b2_rote==-1 && b1_rote==0 && b1_connecting){
				connecting = true;					
				//System.out.println("BundleEv5");
			}else if(b2_rote==1 && b1_rote==0 && !b1_connecting){
				connecting = false;					
				//System.out.println("BundleEv6");
			}else if(b1_rote==1 && b2_rote==-1){
				connecting = true;
				//System.out.println("BundleEv7");
			}else if(b1_rote==-1 && b2_rote==1){
				connecting = false;				
				//System.out.println("BundleEv8");
			}else{				
				//System.out.println("BundleEv00");
				continue;
			}

			/*評価・バンドルの変更*/
			Vertex v1 = mesh.getVertices().get(b1.getID2());
			Vertex v2 = mesh.getVertices().get(b2.getID2());

			double pos1[] = new double[2];
			pos1[0] = v1.getPosition()[0] - vertex.getPosition()[0];
			pos1[1] = v1.getPosition()[1] - vertex.getPosition()[1];

			double pos2[] = new double[2];
			pos2[0] = v2.getPosition()[0] - vertex.getPosition()[0];
			pos2[1] = v2.getPosition()[1] - vertex.getPosition()[1];

			/*
			double cos = (pos1[0]*pos2[0]+pos1[1]*pos2[1])/
					Math.sqrt((pos1[0]*pos1[0]+pos1[1]*pos1[1])*(pos2[0]*pos2[0]+pos2[1]*pos2[1]));
			System.out.println("cos : "+cos);
			 */

			double ev = BundleEv(pos1,pos2);

			if(ev>0.5){
				ArrayList<Integer> b1_merge;
				ArrayList<Integer> b2_merge;
				if(connecting){
					b1_merge = b1.getConnectingMerge();
					b2_merge = b2.getConnectingMerge();
					if(b1_merge==null && b2_merge==null){
						ArrayList<Integer> array = new ArrayList<Integer>();
						array.add(b1.getID2());
						array.add(b2.getID2());
						b1.setConnectingMerge(array);
						b2.setConnectingMerge(array);
						//System.out.println( "nullnull : "+b1.getID2()+","+b2.getID2());
					}else if(b1_merge==null){
						b2_merge.add(b1.getID2());
						b1.setConnectingMerge(b2_merge);
						//System.out.println( "null1 : "+b1.getID2()+","+b2.getID2());
					}else if(b2_merge==null){
						b1_merge.add(b2.getID2());
						b2.setConnectingMerge(b1_merge);
						//System.out.println( "null2 : "+b1.getID2()+","+b2.getID2());
					}else{
						for(int l=0;l<b2_merge.size();l++){
							int n = b2_merge.get(l);
							if(b1_merge.indexOf(n) == -1){
								b1_merge.add(n);
							}
							mesh.getBundle(b1.getID1(),n).setConnectingMerge(b1_merge);
						}
						//System.out.println( "12 : "+b1.getID2()+","+b2.getID2());
					}
				}else{
					b1_merge = b1.getConnectedMerge();
					b2_merge = b2.getConnectedMerge();	
					if(b1_merge==null && b2_merge==null){
						ArrayList<Integer> array = new ArrayList<Integer>();
						array.add(b1.getID2());
						array.add(b2.getID2());
						b1.setConnectedMerge(array);
						b2.setConnectedMerge(array);
					}else if(b1_merge==null){
						b2_merge.add(b1.getID2());
						b1.setConnectedMerge(b2_merge);
					}else if(b2_merge==null){
						b1_merge.add(b2.getID2());
						b2.setConnectedMerge(b1_merge);
					}else{
						for(int l=0;l<b2_merge.size();l++){
							int n = b2_merge.get(l);
							if(b1_merge.indexOf(n) == -1){
								b1_merge.add(n);
							}
							mesh.getBundle(b1.getID1(),n).setConnectedMerge(b1_merge);
						}
					}

				}

			}


		}


		return 0.0;
	}

	public void BundlesDelete_old(double oldRate,double newRate){
		edgeDensityThreshold = newRate;
		ArrayList<Vertex> vertexs = mesh.getVertices();
		for(int vert = 0;vert<mesh.getNumVertices();vert++){
			Vertex v = vertexs.get(vert);
			double dissim[] = v.getDissim();
			ArrayList<Bundle> bundles = v.getOrder();
			if(bundles==null)
				continue;
			for(int i=0;i<bundles.size();i++){
				if(oldRate<dissim[bundles.get(i).getID2()] && dissim[bundles.get(i).getID2()]<newRate){

				}
			}
		}
	}

	public void BundlesDelete(double oldRate,double newRate){
		for(int i = 0;i<mesh.getNumVertices();i++){
			//System.out.println("postBundleEv : "+i);
			mesh.bundleEv+=BundleDelete(newRate,i); //ほんとはこっち
			//mesh.bundleEv+=BundleDelete_(newRate,i); //とりあえず
		}
		edgeDensityThreshold = newRate;
		
		//とりあえず
		for(int i = 0;i<mesh.getNumVertices();i++){
			mesh.bundleEv+=PostVertexEv(i);
		}
		
	}

	public void BundlesAdd(double oldRate,double newRate){
		edgeDensityThreshold = newRate;
		for(int i = 0;i<mesh.getNumVertices();i++){
			//System.out.println("postBundleEv : "+i);
			mesh.bundleEv+=PostVertexEv(i);
		}
	}

	//BundleEv : 合流させるべきかの評価。戻り値:0~1
	public double BundleEv(double vec1[], double vec2[]){


		/*cos : 角度 -1<=0<=1 を 0~1にしたもの*/
		double cos = (vec1[0]*vec2[0]+vec1[1]*vec2[1])/
				Math.sqrt((vec1[0]*vec1[0]+vec1[1]*vec1[1])*(vec2[0]*vec2[0]+vec2[1]*vec2[1]));
		cos = 0.5 + cos*0.5;
		
		if(cos<0.95)
			return 0;
		

		double vec[] = new double[2];
		vec[0] = vec1[0]-vec2[0];
		vec[1] = vec1[1]-vec2[1];
		/*dis : vec1とvec2の先端の距離 0<dis<1 -> 1の方が近いように値を変換*/
		double dis = Math.sqrt(vec[0]*vec[0]+vec[1]*vec[1]) 
				/ (Math.sqrt(vec1[0]*vec1[0]+vec1[1]*vec1[1]) + Math.sqrt(vec2[0]*vec2[0]+vec2[1]*vec2[1]));
		dis = (1-dis)*(1-dis)*(1-dis);

		return (cos*1/3 + dis*2/3);
		//return (cos-dis*2);
	}

	public double BundleEv_(double vec1[], double vec2[]){
		double vec[] = new double[2];
		vec[0] = vec1[0] + vec2[0];
		vec[1] = vec1[1] + vec2[1];
		
		double cos = (vec1[0]*vec2[0]+vec1[1]*vec2[1])/
				(Math.sqrt(vec1[0]*vec1[0]+vec1[1]*vec1[1])*Math.sqrt(vec2[0]*vec2[0]+vec2[1]*vec2[1]));
		System.out.println("cos:"+cos);
		
		if(cos<0.50){
			return 0.0;
		}
		if(cos>1.0){
			return 0.0;
		}
	

		double cos1 = (vec1[0]*vec[0]+vec1[1]*vec[1])/
				Math.sqrt((vec1[0]*vec1[0]+vec1[1]*vec1[1])*(vec[0]*vec[0]+vec[1]*vec[1]));
		double cos2 = (vec2[0]*vec[0]+vec2[1]*vec[1])/
				Math.sqrt((vec2[0]*vec2[0]+vec2[1]*vec2[1])*(vec[0]*vec[0]+vec[1]*vec[1]));

		double v1 = Math.sqrt(vec1[0]*vec1[0] + vec1[1]*vec1[1]) * cos1 ;
		double v2 = Math.sqrt(vec2[0]*vec2[0] + vec2[1]*vec2[1]) * cos2 ;

		return (v1<v2) ? v1:v2;
	}

	public void setConfluenceThreshold(double ratio){
		if(edgeConfluenceThreshold>ratio){
			for(int i = 0;i<mesh.getNumVertices();i++){
				//System.out.println("postBundleEv : "+i);
				edgeConfluenceThreshold=ratio;
				mesh.bundleEv+=PostVertexEv(i);
			}
		}else if(edgeConfluenceThreshold<ratio){
			for(int i = 0;i<mesh.getNumVertices();i++){
				//System.out.println("postBundleEv : "+i);
				mesh.bundleEv+=VertexEv_delete(ratio, i);
			}
		}

		edgeConfluenceThreshold=ratio;
	}
	
	public double VertexEv_delete(double newRate,int num){
		Vertex vertex = mesh.getVertices().get(num);
		ArrayList<Bundle> bundles = vertex.getOrder();
		double dissim[] = mesh.getVertex(num).getDissim();
		if(bundles.size()<2)
			return 0.0;
		ArrayList<Integer> bundles_no = new ArrayList<Integer>();
		
		for(int i=0;i<bundles.size();i++){
			bundles_no.add(bundles.get(i).getID2());
		}
		
		for(int i=1;i<bundles.size();i++){
			Bundle b1 = bundles.get(i);
			if(dissim[b1.getID2()] > edgeDensityThreshold)
				continue;
			Bundle b2 = null;
			for(int j=i-1;j>=0;j--){
				if(dissim[bundles.get(j).getID2()] > edgeDensityThreshold)
					continue;
				else{
					b2 = bundles.get(j);
					break;
				}
			}
			if(b2==null)
				continue;


			/*2本のバンドルの評価*/
			Vertex v1 = mesh.getVertices().get(b1.getID2());
			Vertex v2 = mesh.getVertices().get(b2.getID2());

			double pos1[] = new double[2];
			if(b1.getConnectedMerge() == null){
				pos1[0] = v1.getPosition()[0] - vertex.getPosition()[0];
				pos1[1] = v1.getPosition()[1] - vertex.getPosition()[1];
			}else{
				pos1[0]=0;
				pos1[1]=0;
				for(int b = 0;b<b1.getConnectedMerge().size();b++){
					pos1[0] += mesh.getVertex(b1.getConnectedMerge().get(b)).getPosition()[0];
					pos1[1] += mesh.getVertex(b1.getConnectedMerge().get(b)).getPosition()[1];
				}
				pos1[0] /= b1.getConnectedMerge().size();
				pos1[1] /= b1.getConnectedMerge().size();
			}

			double pos2[] = new double[2];
			if(b2.getConnectedMerge() == null){
				pos2[0] = v2.getPosition()[0] - vertex.getPosition()[0];
				pos2[1] = v2.getPosition()[1] - vertex.getPosition()[1];
			}else{
				pos2[0]=0;
				pos2[1]=0;
				for(int b = 0;b<b2.getConnectedMerge().size();b++){
					pos2[0] += mesh.getVertex(b2.getConnectedMerge().get(b)).getPosition()[0];
					pos2[1] += mesh.getVertex(b2.getConnectedMerge().get(b)).getPosition()[1];
				}
				pos2[0] /= b2.getConnectedMerge().size();
				pos2[1] /= b2.getConnectedMerge().size();
			}
			double ev = BundleEv(pos1,pos2);

			double pos1_[] = new double[2];
			pos1_[0] = v1.getPosition()[0] - vertex.getPosition()[0];
			pos1_[1] = v1.getPosition()[1] - vertex.getPosition()[1];						
			double pos2_[] = new double[2];
			pos2_[0] = v2.getPosition()[0] - vertex.getPosition()[0];
			pos2_[1] = v2.getPosition()[1] - vertex.getPosition()[1];
			ev = BundleEv_(pos1_,pos2_);

			if(!(ev>edgeConfluenceThreshold && ev<newRate))
				continue;
			
			//System.out.println("conflu");
			
			//とりあえず
				Bundle b = bundles.get(i);
				//System.out.println("delete");
				ArrayList<Integer> connected = b.getConnectedMerge();
				ArrayList<Integer> connecting = b.getConnectingMerge();
				ArrayList<Integer> right = null;
				ArrayList<Integer> left = null;
				if(connected!=null){
					//System.out.println("connected");
					right = new ArrayList<Integer>();
					left = new ArrayList<Integer>();
					for(int j=0;j<connected.size();j++){
						int c = connected.get(j);
						//System.out.println("c:"+c);
						if(c==b.getID2()) continue;
						int index = bundles_no.indexOf(c);
						//System.out.println("index:"+index);
						if(index<0) continue;
						if(index<i){
							left.add(c);
						}
						if(index>i){
							right.add(c);
						}
					}
					if(left!=null){
						for(int j=0;j<left.size();j++){
							bundles.get(bundles_no.indexOf(left.get(j))).setConnectedMerge(left);
							//System.out.println("left");
						}
					}
					if(right!=null){
						for(int j=0;j<right.size();j++){
							bundles.get(bundles_no.indexOf(right.get(j))).setConnectedMerge(right);
							//System.out.println("right");
						}
					}
					for(int j=0;j<i;j++){
						Bundle bundle = bundles.get(j);
						int no = bundle.getID2();
						if(left.indexOf(no)>-1){
							bundle.setConnectedMerge(left);
							mesh.getBundle(j,num).setConnectingMerge(left);
							//System.out.println("left");
						}
					}
					for(int j=i+1;j<bundles.size();j++){
						Bundle bundle = bundles.get(j);
						int no = bundle.getID2();
						if(right.indexOf(no)>-1){
							bundle.setConnectedMerge(right);
							mesh.getBundle(j,num).setConnectingMerge(right);
							//System.out.println("right");
						}
					}
							
				}
				
				if(connecting!=null){
					//System.out.println("connecting");
					right = new ArrayList<Integer>();
					left = new ArrayList<Integer>();
					for(int j=0;j<connecting.size();j++){
						int c = connecting.get(j);
						//System.out.println("c:"+c);
						if(c==b.getID2()) continue;
						int index = bundles_no.indexOf(c);
						//System.out.println("index:"+index);
						if(index<0) continue;
						if(index<i){
							left.add(c);
						}
						if(index>i){
							right.add(c);
						}
					}
					if(left!=null){
						for(int j=0;j<left.size();j++){
							bundles.get(bundles_no.indexOf(left.get(j))).setConnectingMerge(left);
							//System.out.println("left");
						}
					}
					if(right!=null){
						for(int j=0;j<right.size();j++){
							bundles.get(bundles_no.indexOf(right.get(j))).setConnectingMerge(right);
							//System.out.println("right");
						}
					}
					for(int j=0;j<i;j++){
						Bundle bundle = bundles.get(j);
						int no = bundle.getID2();
						if(left.indexOf(no)>-1){
							//System.out.println("left");
							bundle.setConnectedMerge(left);
							mesh.getBundle(j,num).setConnectingMerge(left);
						}
					}
					for(int j=i+1;j<bundles.size();j++){
						Bundle bundle = bundles.get(j);
						int no = bundle.getID2();
						if(right.indexOf(no)>-1){
							//System.out.println("right");
							bundle.setConnectedMerge(right);
							mesh.getBundle(j,num).setConnectingMerge(right);
						}
					}
				}
				b.setConnectedMerge(null);
				b.setConnectingMerge(null);
				
			
			
			//とりあえず
			
			
			

		}


		return 0.0;
	}
	
	
	/*未完成(思ったように動かない)*/
	public double BundleDelete(double newRatio, int num){
		Vertex vertex = mesh.getVertices().get(num);
		ArrayList<Bundle> bundles = vertex.getOrder();
		double dissim[] = mesh.getVertex(num).getDissim();
		ArrayList<Integer> bundles_no = new ArrayList<Integer>();
		
		for(int i=0;i<bundles.size();i++){
			bundles_no.add(bundles.get(i).getID2());
		}
		
		if(bundles.size()<2)
			return 0.0;
		for(int i=1;i<bundles.size();i++){
			Bundle b = bundles.get(i);
			if(!(dissim[b.getID2()] < edgeDensityThreshold && dissim[b.getID2()] > newRatio))
				continue;
			//System.out.println("delete");
			ArrayList<Integer> connected = b.getConnectedMerge();
			ArrayList<Integer> connecting = b.getConnectingMerge();
			ArrayList<Integer> right = null;
			ArrayList<Integer> left = null;
			if(connected!=null){
				//System.out.println("connected");
				right = new ArrayList<Integer>();
				left = new ArrayList<Integer>();
				for(int j=0;j<connected.size();j++){
					int c = connected.get(j);
					//System.out.println("c:"+c);
					if(c==b.getID2()) continue;
					int index = bundles_no.indexOf(c);
					//System.out.println("index:"+index);
					if(index<0) continue;
					if(index<i){
						left.add(c);
					}
					if(index>i){
						right.add(c);
					}
				}
				if(left!=null){
					for(int j=0;j<left.size();j++){
						bundles.get(bundles_no.indexOf(left.get(j))).setConnectedMerge(left);
						//System.out.println("left");
					}
				}
				if(right!=null){
					for(int j=0;j<right.size();j++){
						bundles.get(bundles_no.indexOf(right.get(j))).setConnectedMerge(right);
						//System.out.println("right");
					}
				}
				for(int j=0;j<i;j++){
					Bundle bundle = bundles.get(j);
					int no = bundle.getID2();
					if(left.indexOf(no)>-1){
						bundle.setConnectedMerge(left);
						mesh.getBundle(j,num).setConnectingMerge(left);
						//System.out.println("left");
					}
				}
				for(int j=i+1;j<bundles.size();j++){
					Bundle bundle = bundles.get(j);
					int no = bundle.getID2();
					if(right.indexOf(no)>-1){
						bundle.setConnectedMerge(right);
						mesh.getBundle(j,num).setConnectingMerge(right);
						//System.out.println("right");
					}
				}
						
			}
			
			if(connecting!=null){
				//System.out.println("connecting");
				right = new ArrayList<Integer>();
				left = new ArrayList<Integer>();
				for(int j=0;j<connecting.size();j++){
					int c = connecting.get(j);
					//System.out.println("c:"+c);
					if(c==b.getID2()) continue;
					int index = bundles_no.indexOf(c);
					//System.out.println("index:"+index);
					if(index<0) continue;
					if(index<i){
						left.add(c);
					}
					if(index>i){
						right.add(c);
					}
				}
				if(left!=null){
					for(int j=0;j<left.size();j++){
						bundles.get(bundles_no.indexOf(left.get(j))).setConnectingMerge(left);
						//System.out.println("left");
					}
				}
				if(right!=null){
					for(int j=0;j<right.size();j++){
						bundles.get(bundles_no.indexOf(right.get(j))).setConnectingMerge(right);
						//System.out.println("right");
					}
				}
				for(int j=0;j<i;j++){
					Bundle bundle = bundles.get(j);
					int no = bundle.getID2();
					if(left.indexOf(no)>-1){
						//System.out.println("left");
						bundle.setConnectedMerge(left);
						mesh.getBundle(j,num).setConnectingMerge(left);
					}
				}
				for(int j=i+1;j<bundles.size();j++){
					Bundle bundle = bundles.get(j);
					int no = bundle.getID2();
					if(right.indexOf(no)>-1){
						//System.out.println("right");
						bundle.setConnectedMerge(right);
						mesh.getBundle(j,num).setConnectingMerge(right);
					}
				}
			}
			b.setConnectedMerge(null);
			b.setConnectingMerge(null);
			
		}
		
		return 1.0;
	}
	
	/*仮のやつ*/
	public double BundleDelete_(double newRatio, int num){
		Vertex vertex = mesh.getVertices().get(num);
		ArrayList<Bundle> bundles = vertex.getOrder();
		double dissim[] = mesh.getVertex(num).getDissim();
		ArrayList<Integer> bundles_no = new ArrayList<Integer>();
		
		for(int i=0;i<bundles.size();i++){
			bundles_no.add(bundles.get(i).getID2());
		}
		
		if(bundles.size()<2)
			return 0.0;
		for(int i=1;i<bundles.size();i++){
			Bundle b = bundles.get(i);
			b.setConnectedMerge(null);
			b.setConnectingMerge(null);
		}
		
		return 1.0;
	}
	
	
	//マップ作成
	public void generateMap_node(){
		int mapSize = 100;
		map = new int[mapSize][mapSize];
		
		for(int i=0;i<mapSize;i++){
			for(int j=0;j<mapSize;j++){
				map[i][j]=0;
			}
		}
		
		for(int i = 0; i < nodes.size(); i++) {
			Node node = (Node)nodes.get(i);
			int x = (int)((node.getX()+1)/2.0 * mapSize);
			int y = (int)((node.getY()+1)/2.0 * mapSize);	
			if(x<0) x=0;
			else if(x>mapSize-1) x=mapSize-1;
			if(y<0) y=0;
			else if(y>mapSize-1) y=mapSize-1;
			//System.out.println(x+", "+y);
			map[x][y]++;			
		}		
	}
	
	public void generateMap_node2(){
		int mapSize = 100;
		map = new int[mapSize][mapSize];
		
		for(int i=0;i<mapSize;i++){
			for(int j=0;j<mapSize;j++){
				map[i][j]=0;
			}
		}
		
		for(int i = 0; i < nodes.size(); i++) {
			Node node = (Node)nodes.get(i);
			int x = (int)((node.getX()+1)/2.0 * mapSize);
			int y = (int)((node.getY()+1)/2.0 * mapSize);	
			if(x<0) x=0;
			else if(x>mapSize-1) x=mapSize-1;
			if(y<0) y=0;
			else if(y>mapSize-1) y=mapSize-1;
			//System.out.println(x+", "+y);
			map[x][y]++;			
		}
	}
	
	public void generateMap(){
		int mapSize = 100;
		map = new int[mapSize][mapSize];
		
		for(int i=0;i<mapSize;i++){
			for(int j=0;j<mapSize;j++){
				map[i][j]=0;
			}
		}
		map = new int[mapSize][mapSize];
		for(int i = 0; i < mesh.getVertices().size(); i++) {
			Vertex vertex = (Vertex)mesh.getVertices().get(i);
			ArrayList nodes = vertex.getNodes();
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
				int maxXmap = (int)((maxX+1)/2.0 * mapSize);
				int minXmap = (int)((minX+1)/2.0 * mapSize);	
				int maxYmap = (int)((maxY+1)/2.0 * mapSize);
				int minYmap = (int)((minY+1)/2.0 * mapSize);
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

}
