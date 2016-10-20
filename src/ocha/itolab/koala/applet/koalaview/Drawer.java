package ocha.itolab.koala.applet.koalaview;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.*;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.gl2.GLUgl2;

import com.jogamp.opengl.util.gl2.GLUT;

import ocha.itolab.koala.core.data.*;
import ocha.itolab.koala.core.mesh.*;


/**
 * 描画処理のクラス
 * 
 * @author itot
 */
public class Drawer implements GLEventListener {

	private GL gl;
	private GL2 gl2;
	private GLU glu;
	private GLUgl2 glu2;
	private GLUT glut;
	GLAutoDrawable glAD;
	GLCanvas glcanvas;
	Graph graph;

	Transformer trans = null;
	ViewingPanel vp = null;

	DoubleBuffer modelview, projection, p1, p2, p3, p4;
	IntBuffer viewport;
	int windowWidth, windowHeight;

	boolean isMousePressed = false, isAnnotation = true;

	double edgeDensityThreshold = 0.1;
	double edgeDensityThreshold_ = edgeDensityThreshold;
	double linewidth = 1.0;
	double bundleShape = 0.7;
	double rotationStrength = 0.5;
	double mergeStrength = 0.5;
	double transparency = 0.9;
	double ConfluenceThreshold = 0.5;
	boolean colorSwitch[] = null;
	int edgeDensityMode = 1;
	int colorMode = 1;
	double xmin, xmax, ymin, ymax;
	int rangeX1 = 0, rangeX2 = 0, rangeY1 = 0, rangeY2 = 0;
	DrawerUtility du = null;

	int dragMode = 1;
	private double angleX = 0.0;
	private double angleY = 0.0;
	private double shiftX = 0.0;
	private double shiftY = 0.0;
	private double scaleX = 0.5;
	private double scaleY = 0.5;
	private double centerX = 0.0;
	private double centerY = 0.0;
	private double centerZ = 0.0;
	private double size = 0.5;

	Node pickedNode = null;



	/**
	 * Constructor
	 * 
	 * @param width
	 *            描画領域の幅
	 * @param height
	 *            描画領域の高さ
	 */
	public Drawer(int width, int height, GLCanvas c) {
		glcanvas = c;
		windowWidth = width;
		windowHeight = height;
		du = new DrawerUtility(width, height);

		viewport = IntBuffer.allocate(4);
		modelview = DoubleBuffer.allocate(16);
		projection = DoubleBuffer.allocate(16);

		p1 = DoubleBuffer.allocate(3);
		p2 = DoubleBuffer.allocate(3);
		p3 = DoubleBuffer.allocate(3);
		p4 = DoubleBuffer.allocate(3);

		glcanvas.addGLEventListener(this);
	}

	public GLAutoDrawable getGLAutoDrawable() {
		return glAD;
	}

	/**
	 * ダミーメソッド
	 */
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
			boolean deviceChanged) {
	}

	/**
	 * Transformerをセットする
	 * 
	 * @param transformer
	 */
	public void setTransformer(Transformer view) {
		this.trans = view;
		du.setTransformer(view);
	}

	/**
	 * 描画領域のサイズを設定する
	 * 
	 * @param width
	 *            描画領域の幅
	 * @param height
	 *            描画領域の高さ
	 */
	public void setWindowSize(int width, int height) {
		// windowWidth = width;
		// windowHeight = height;
		// du.setWindowSize(width, height);
	}

	/**
	 * マウスボタンのON/OFFを設定する
	 * 
	 * @param isMousePressed
	 *            マウスボタンが押されていればtrue
	 */
	public void setMousePressSwitch(boolean isMousePressed) {
		this.isMousePressed = isMousePressed;
		if (isMousePressed == true) {
			// drawCategoryTextField();
		}
	}

	/**
	 * 線の太さをセットする
	 * 
	 * @param lw
	 *            線の太さ（画素数）
	 */
	public void setLinewidth(double lw) {
		linewidth = lw;
	}

	public void setGraph(Graph g) {
		graph = g;
		calcMeshColor();
	}

	/*描画本数が変わった時*/
	public void setEdgeThreshold(double ratio) {
		edgeDensityThreshold_ = edgeDensityThreshold;

		//System.out.println("setEdgeThreshold");
		if(graph==null)
			return;

		if(edgeDensityThreshold>ratio) //エッジの本数が少なくなった場合
			graph.BundlesDelete(edgeDensityThreshold,ratio);

		if(edgeDensityThreshold<ratio)
			graph.BundlesAdd(edgeDensityThreshold,ratio);

		edgeDensityThreshold = ratio;

	}

	public void setBundleShape(double ratio) {
		bundleShape = ratio;
	}

	public void setConfluenceThreshold(double ratio) {
		ConfluenceThreshold = ratio;
		graph.setConfluenceThreshold(1-ratio);
	}

	public void setRotationStrength(double ratio) {
		rotationStrength = ratio;
	}

	public void setMergeStrength(double ratio) {
		mergeStrength = ratio;
	}

	public void setEdgeDensityMode(int mode) {
		edgeDensityMode = mode;
	}

	public void setBackgroundTransparency(double t) {
		transparency = t;
		calcMeshColor();
	}

	public void setColorMode(int mode) {
		colorMode = mode;
		calcMeshColor();
	}


	/**
	 * マウスドラッグのモードを設定する
	 * 
	 * @param dragMode
	 *            (1:ZOOM 2:SHIFT 3:ROTATE)
	 */
	public void setDragMode(int newMode) {
		dragMode = newMode;
	}


	public void setColorSwitch(boolean[] st) {
		colorSwitch = st;
	}


	/**
	 * ViewingPanelを設定する
	 */
	public void setViewingPanel(ViewingPanel v) {
		vp = v;
	}




	void calcMeshColor() {
		Mesh mesh = graph.mesh;


		// for each vertex
		for(int i = 0; i < mesh.getNumVertices(); i++) {
			Vertex v = mesh.getVertex(i);
			double color[] = {0.0, 0.0, 0.0};
			int counter = 0;

			// if colorMode is COLOR_DEGREE
			if(colorMode == Canvas.COLOR_DEGREE) {
				v.setColor(transparency, transparency, transparency);
				continue;
			}

			// determine the color of the vertex
			ArrayList<Node> nodes = v.getNodes();
			for(int k = 0; k < nodes.size(); k++) {
				int colorId = nodes.get(k).getColorId();
				if(colorId >= 0) {
					Color cc = VectorParettePanel.calcColor(colorId, graph.vectorname.length);
					double rr = (double)cc.getRed() / 255.0;
					double gg = (double)cc.getGreen() / 255.0;
					double bb = (double)cc.getBlue() / 255.0;
					color[0] += rr;
					color[1] += gg;
					color[2] += bb;
					counter++;
				}
			}

			if(counter > 0) {
				color[0] /= (double)counter;
				color[1] /= (double)counter;
				color[2] /= (double)counter;
				color[0] = transparency + (1.0 - transparency) * color[0];
				color[1] = transparency + (1.0 - transparency) * color[1];
				color[2] = transparency + (1.0 - transparency) * color[2];
				v.setColor(color[0], color[1], color[2]);
			}
			else
				v.setColor(transparency, transparency, transparency);

		}

	}

	double calcZ(Node node) {
		double z = 0.0;
		double degratio = (double)(node.getNumConnectedEdge() + node.getNumConnectingEdge()) /  (double)graph.maxDegree;
		double ke = graph.mesh.keyEmphasis;
		if(degratio >  0.1 && ke > 0.1) 
			z = degratio * ke;

		return z;
	}

	/**
	 * 初期化
	 */
	public void init(GLAutoDrawable drawable) {

		gl = drawable.getGL();
		gl2 = drawable.getGL().getGL2();
		glu = new GLU();
		glu2 = new GLUgl2();
		glut = new GLUT();
		this.glAD = drawable;

		gl.glEnable(GL.GL_RGBA);
		gl.glEnable(GL2.GL_DEPTH);
		gl.glEnable(GL2.GL_DOUBLE);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_NORMALIZE);
		gl2.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, GL2.GL_TRUE);
		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

	}

	/**
	 * 再描画
	 */
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {

		windowWidth = width;
		windowHeight = height;

		// ビューポートの定義
		gl.glViewport(0, 0, width, height);

		// 投影変換行列の定義
		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glLoadIdentity();
		gl2.glOrtho(-width / 200.0, width / 200.0, -height / 200.0,
				height / 200.0, -1000.0, 1000.0);

		gl2.glMatrixMode(GL2.GL_MODELVIEW);

	}

	/**
	 * 描画を実行する
	 */
	public void display(GLAutoDrawable drawable) {

		long mill1 = System.currentTimeMillis();

		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		// 視点位置を決定
		gl2.glLoadIdentity();
		glu.gluLookAt(centerX, centerY, (centerZ + 20.0), centerX, centerY,
				centerZ, 0.0, 1.0, 0.0);

		shiftX = trans.getViewShift(0);
		shiftY = trans.getViewShift(1);
		scaleX = trans.getViewScaleX() * windowWidth / (size * 600.0);
		scaleY = trans.getViewScaleY() * windowHeight / (size * 600.0);
		angleX = trans.getViewRotateY() * 45.0;
		angleY = trans.getViewRotateX() * 45.0;

		// 行列をプッシュ
		gl2.glPushMatrix();

		// いったん原点方向に物体を動かす
		gl2.glTranslated(centerX, centerY, centerZ);

		// マウスの移動量に応じて回転
		gl2.glRotated(angleX, 1.0, 0.0, 0.0);
		gl2.glRotated(angleY, 0.0, 1.0, 0.0);

		// マウスの移動量に応じて移動
		gl2.glTranslated(shiftX, shiftY, 0.0);

		// マウスの移動量に応じて拡大縮小
		gl2.glScaled(scaleX, scaleY, 1.0);

		// 物体をもとの位置に戻す
		gl2.glTranslated(-centerX, -centerY, -centerZ);

		// 変換行列とビューポートの値を保存する
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport);
		gl2.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview);
		gl2.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection);

		// 描画
		paintMesh();
		if(edgeDensityMode == Canvas.EDGE_DENSITY_DISSIMILARITY)
			drawEdgesDissimilarity();
		if(edgeDensityMode == Canvas.EDGE_DENSITY_DEGREE)
			drawEdgesDegree();
		drawPickedEdges();

		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_LIGHT0);
		drawNodes();
		gl.glDisable(GL2.GL_LIGHTING);

		// 行列をポップ
		gl2.glPopMatrix();

		long mill2 = System.currentTimeMillis();
		// System.out.println("  drawer.display() time=" + (mill2 - mill1));
	}



	/**
	 * 背景色となるMeshを塗りつぶす
	 */
	void paintMesh() {
		if(graph == null) return;
		if(graph.mesh == null) return;

		// for each triangle
		Mesh mesh = graph.mesh;
		float colf[] = new float[3]; 
		for(int i = 0; i < mesh.getNumTriangles(); i++) {
			Triangle t = mesh.getTriangle(i);

			gl2.glBegin(gl2.GL_POLYGON);

			// for each vertex
			Vertex v[] = t.getVertices();
			for(int j = 0; j < 3; j++) {
				double pos[] = v[j].getPosition();
				double col[] = v[j].getColor();
				colf[0] = (float)col[0];
				colf[1] = (float)col[1];
				colf[2] = (float)col[2];

				gl2.glColor3d(col[0], col[1], col[2]);
				gl2.glVertex3d(pos[0], pos[1], -0.05);
			}

			gl2.glEnd();
		}

	}


	/**
	 * Select edges to be drawn based on dissimilarity between two vertices 
	 */	
	void drawEdgesDissimilarity() {
		if(graph == null) return;
		if(graph.mesh == null) return;
		
		int count_converge = 0;
		int count_interactive = 0;
		int count_nocurve = 0;
		

		int BundleNum = 0;
		int notBundleNum = 0;

		// Draw bundled edges
		gl2.glColor3d(0.7, 0.7, 0.7);

		Mesh mesh = graph.mesh;
		for(int i = 0; i < mesh.getNumVertices(); i++) {
			Vertex v1 = mesh.getVertex(i);
			ArrayList<Node> nodes1 = v1.getNodes();
			double dissim[] = v1.getDissim();
			double SQUARE_SIZE = 0.01 / trans.getViewScaleX();
			
			//テスト用
			
			/*
			
			//connecting
			if(v1.connectingPos[0]!=0 || v1.connectingPos[1]!=0){
				gl2.glColor3d(0,0,0);
				drawOneBarWithHeight(v1.connectingPos[0], v1.connectingPos[1], 0, SQUARE_SIZE);
				gl2.glBegin(GL2.GL_LINE_STRIP);
				gl2.glColor3d(0,0,1);
				gl2.glVertex3d(v1.getPosition()[0], v1.getPosition()[1], 0);
				gl2.glColor3d(1,0,0);
				gl2.glVertex3d(v1.connectingPos[0], v1.connectingPos[1], 0);
				gl2.glEnd();
			}
			
			//connected
			if(v1.connectedPos[0]!=0 || v1.connectedPos[1]!=0){
				gl2.glColor3d(0,0,0);
				drawOneBarWithHeight(v1.connectedPos[0], v1.connectedPos[1], 0, SQUARE_SIZE);
				gl2.glBegin(GL2.GL_LINE_STRIP);			
				gl2.glColor3d(1,0,0);
				gl2.glVertex3d(v1.getPosition()[0], v1.getPosition()[1], 0);
				gl2.glColor3d(0,0,1);
				gl2.glVertex3d(v1.connectedPos[0], v1.connectedPos[1], 0);
				gl2.glEnd();
			}
			
			*/
			
			
			// テスト用おわり


			for(int j = 0; j < mesh.getNumVertices(); j++) { //
				if(dissim[j] > edgeDensityThreshold) continue;
				if(i==j) continue;
				Vertex v2 = mesh.getVertex(j);
				ArrayList<Node> nodes2 = v2.getNodes();
				Bundle startBundle = graph.mesh.getBundle(i, j);
				Bundle endBundle = graph.mesh.getBundle(j, i);
				ArrayList<Integer> startBundlesList = startBundle.getConnectingMerge();
				ArrayList<Integer> endBundlesList = endBundle.getConnectedMerge();
				double angle;
				if(i>j)
					angle = endBundle.getAngle()/Math.PI;
				else
					angle = startBundle.getAngle()/Math.PI;
				//startBundle.getAngle()/Math.PI; //

				//angle = startBundle.getAngle()/Math.PI;
				//if(i>j) continue;

				int rotation = mesh.getBundle(i,j).getRotation();

				/*startのBundleの中心*/
				double startBundlePos[] = {0.0,0.0,0.0};
				if(startBundlesList==null){
					startBundlePos = v2.getPosition();
					startBundlePos = null;
				}
				else{
					//System.out.println(startBundlesList.size());
					for(int bb = 0; bb<startBundlesList.size(); bb++){
						Vertex v = mesh.getVertex(startBundlesList.get(bb));
						startBundlePos[0] += v.getPosition()[0];
						startBundlePos[1] += v.getPosition()[1];
						startBundlePos[2] += v.getPosition()[2];
					}
					startBundlePos[0] /= startBundlesList.size();
					startBundlePos[1] /= startBundlesList.size();
					startBundlePos[2] /= startBundlesList.size();
					rotation/=startBundlesList.size();
					startBundlePos = getMinBundle(v1,startBundlesList);
				}


				/*endのBundleの中心*/
				double endBundlePos[] = {0.0,0.0,0.0};
				if(endBundlesList==null){
					endBundlePos = v1.getPosition();
					endBundlePos = null;
				}
				else{
					//System.out.println(endBundlesList.size());
					for(int bb = 0; bb<endBundlesList.size(); bb++){
						Vertex v = mesh.getVertex(endBundlesList.get(bb));
						endBundlePos[0] += v.getPosition()[0];
						endBundlePos[1] += v.getPosition()[1];
						endBundlePos[2] += v.getPosition()[2];
					}
					endBundlePos[0] /= endBundlesList.size();
					endBundlePos[1] /= endBundlesList.size();
					endBundlePos[2] /= endBundlesList.size();
					rotation/=endBundlesList.size();
					endBundlePos = getMinBundle(v2,endBundlesList);
				}

				for(int ii = 0; ii < nodes1.size(); ii++) {
					Node n1 = nodes1.get(ii);
					for(int jj = 0; jj < nodes2.size(); jj++) {
						Node n2 = nodes2.get(jj);
						if(graph.isNodeConnected1to2(n1, n2) == false)
							continue;
						if(startBundlesList==null & endBundlesList==null){
							notBundleNum++;
							if(rotation!=0){
								count_interactive++;
								drawBundledEdges(v1, v2, n1, n2,rotation,angle); //合流しないけど双方向
							}else{	
								count_nocurve++;
								drawBundledEdges(v1, v2, n1, n2,0,angle); //合流しないし双方向じゃない
							}
						}else{
							count_converge++;
							BundleNum++;
							drawBundledEdges_(v1, v2, n1, n2,rotation, mesh, startBundlePos,endBundlePos,angle); //合流する
						}

					}
				}
			}

		}
		//System.out.println("Bundle Num. : " + BundleNum);
		//System.out.println("Not Bundle Num. : " + notBundleNum);
		//System.out.println("per : "+ (double)BundleNum/(notBundleNum+BundleNum));
		System.out.println("---------------------------");
		System.out.println("converge : "+count_converge);
		System.out.println("interactive : "+count_interactive);
		System.out.println("nocurve : "+count_nocurve);
		System.out.println("---------------------------");

		
	}

	/**
	 * Select edges to be drawn based on degrees of nodes
	 */
	/*こっちは書き換えてない*/
	void drawEdgesDegree() {
		if(graph == null) return;
		if(graph.mesh == null) return;

		// Draw bundled edges
		gl2.glColor3d(0.7, 0.7, 0.7);
		int mindeg = (int)((double)graph.maxDegree * (1.0 - edgeDensityThreshold));

		Mesh mesh = graph.mesh;
		for(int i = 0; i < mesh.getNumVertices(); i++) {
			Vertex v1 = mesh.getVertex(i);
			ArrayList<Node> nodes1 = v1.getNodes();

			for(int j = (i + 1); j < mesh.getNumVertices(); j++) {
				Vertex v2 = mesh.getVertex(j);
				ArrayList<Node> nodes2 = v2.getNodes();

				for(int ii = 0; ii < nodes1.size(); ii++) {
					Node n1 = nodes1.get(ii);
					int deg1 = n1.getNumConnectedEdge() + n1.getNumConnectingEdge();

					for(int jj = 0; jj < nodes2.size(); jj++) {
						Node n2 = nodes2.get(jj);
						int deg2 = n2.getNumConnectedEdge() + n2.getNumConnectingEdge();
						if(deg1 < mindeg && deg2 < mindeg) continue;
						if(graph.isTwoNodeConnected(n1, n2) == false)
							continue;
						drawBundledEdges(v1, v2, n1, n2,0,0);	
					}
				}
			}	
		}
	}


	

	double[] getMinBundle(Vertex v1,ArrayList<Integer> list){
		double central[] = new double[3] ;
		central[0] = central[1] = central[2] = 0;
		for(int i = 0; i<list.size(); i++){
			Vertex v = graph.mesh.getVertex(list.get(i));
			central[0] += v.getPosition()[0];
			central[1] += v.getPosition()[1];
			central[2] += v.getPosition()[2];
		}
		central[0] /= list.size();
		central[1] /= list.size();
		central[2] /= list.size();

		double vec1[] = new double[3] ;
		vec1[0] = central[0] - v1.getPosition()[0];
		vec1[1] = central[1] - v1.getPosition()[1];
		vec1[2] = central[2] - v1.getPosition()[2];
	
		
		//int num=0;
		double minLength = 100;
		for(int i = 0; i<list.size(); i++){
			Vertex v = graph.mesh.getVertex(list.get(i));
			double vec[] = new double[3] ;
			vec[0] = v.getPosition()[0] - v1.getPosition()[0];
			vec[1] = v.getPosition()[1] - v1.getPosition()[1];
			vec[2] = v.getPosition()[2] - v1.getPosition()[2];
			double cos = (vec1[0]*vec[0]+vec1[1]*vec[1])/
					Math.sqrt((vec1[0]*vec1[0]+vec1[1]*vec1[1])*(vec[0]*vec[0]+vec[1]*vec[1]));
			double length = Math.sqrt(vec[0]*vec[0] + vec[1]*vec[1]) * cos ;
			if(minLength>length){
				minLength = length;
				//num = i;
			}	
		}
		double length = Math.sqrt(vec1[0]*vec1[0] + vec1[1]*vec1[1]);
		
		vec1[0] = v1.getPosition()[0] + vec1[0] * (minLength/length);
		vec1[1] = v1.getPosition()[1] + vec1[1] * (minLength/length);
		vec1[2] = v1.getPosition()[2] + vec1[2] * (minLength/length);
		
		
		
		return vec1;
	}
	
	void drawPickedEdges() {
		// Draw edges of the picked node
		//gl2.glColor3d(0.8, 0.6, 0.8);
		gl2.glLineWidth(2.0f);
		if(pickedNode != null) {
			int NUM_T = 10;
			double z = calcZ(pickedNode);

			for(int i = 0; i < pickedNode.getNumConnectedEdge(); i++) {
				Edge e = pickedNode.getConnectedEdge(i);
				Node enode[] = e.getNode();
				gl2.glBegin(GL.GL_LINES);
				if(enode[0] == pickedNode) {
					double z2 = calcZ(enode[1]);
					gl2.glColor3d(0,0,1);
					gl2.glVertex3d(enode[0].getX(), enode[0].getY(), z);
					gl2.glColor3d(1,0,0);
					gl2.glVertex3d(enode[1].getX(), enode[1].getY(), z2);
				}
				else {
					double z2 = calcZ(enode[0]);
					gl2.glColor3d(0,0,1);
					gl2.glVertex3d(enode[0].getX(), enode[0].getY(), z2);
					gl2.glColor3d(1,0,0);
					gl2.glVertex3d(enode[1].getX(), enode[1].getY(), z);
				}
				gl2.glEnd();
			}
			for(int i = 0; i < pickedNode.getNumConnectingEdge(); i++) {
				Edge e = pickedNode.getConnectingEdge(i);
				Node enode[] = e.getNode();
				gl2.glBegin(GL.GL_LINES);
				if(enode[0] == pickedNode) {
					double z2 = calcZ(enode[1]);
					gl2.glColor3d(0,0,1);
					gl2.glVertex3d(enode[0].getX(), enode[0].getY(), z);
					gl2.glColor3d(1,0,0);
					gl2.glVertex3d(enode[1].getX(), enode[1].getY(), z2);
				}
				else {
					double z2 = calcZ(enode[0]);
					gl2.glColor3d(0,0,1);
					gl2.glVertex3d(enode[0].getX(), enode[0].getY(), z2);
					gl2.glColor3d(1,0,0);
					gl2.glVertex3d(enode[1].getX(), enode[1].getY(), z);
				}
				gl2.glEnd();
			}
		}		
		gl2.glLineWidth(1.0f);
	}

	void drawBundledEdges(Vertex v1, Vertex v2, Node n1, Node n2,int rotation,Mesh mesh) {
		int NUM_T = 10;
		double ONE_THIRD = 0.33333333333;

		int cid1 = n1.getColorId();
		int cid2 = n1.getColorId();
		if(colorSwitch != null 
				&& cid1 >= 0 && colorSwitch[cid1] == false
				&& cid2 >= 0 && colorSwitch[cid2] == false) {
			return;
		}


		double p0[] = new double[2];
		double p1[] = new double[2];
		double p2[] = new double[2];
		double p3[] = new double[2];
		double v1pos[] = v1.getPosition();
		double v2pos[] = v2.getPosition();

		p0[0] = n1.getX();    p0[1] = n1.getY();
		p3[0] = n2.getX();    p3[1] = n2.getY();

		double z1 = calcZ(n1);
		double z2 = calcZ(n2);

		if(bundleShape > 0.5) { //中心を結ぶ線分上に制御点を決定
			double ratio = (bundleShape + 0.5) * 2.0 * ONE_THIRD;
			p1[0] = v1pos[0] * ratio + v2pos[0] * (1.0 - ratio);
			p1[1] = v1pos[1] * ratio + v2pos[1] * (1.0 - ratio);
			p2[0] = v2pos[0] * ratio + v1pos[0] * (1.0 - ratio);
			p2[1] = v2pos[1] * ratio + v1pos[1] * (1.0 - ratio);
		}
		else { //三分割する点と三分割する点の線分上に制御点を決定
			double ratio = bundleShape * 2.0;
			p1[0] = (v1pos[0] * 2.0 + v2pos[0]) * ONE_THIRD * ratio
					+ (p0[0] * 2.0 + p3[0]) * ONE_THIRD * (1.0 - ratio);
			p1[1] = (v1pos[1] * 2.0 + v2pos[1]) * ONE_THIRD * ratio
					+ (p0[1] * 2.0 + p3[1]) * ONE_THIRD * (1.0 - ratio);
			p2[0] = (v2pos[0] * 2.0 + v1pos[0]) * ONE_THIRD * ratio
					+ (p3[0] * 2.0 + p0[0]) * ONE_THIRD * (1.0 - ratio);
			p2[1] = (v2pos[1] * 2.0 + v1pos[1]) * ONE_THIRD * ratio
					+ (p3[1] * 2.0 + p0[1]) * ONE_THIRD * (1.0 - ratio);
		}

		//ここからmerge処理		
		int id1 = v1.getId();
		int id2 = v2.getId();
		double cluster[] = new double[2]; //クラスタの中心
		double dis[] = new double[2]; //クラスタとv1の差
		double count=0;
		cluster[0]=0;
		cluster[1]=0;
		ArrayList<Bundle> bundles = mesh.getBundles(id1);
		double dissim[] = v1.getDissim();
		int clusterNum=0;

		/*とっとく
		 //merge処理 : connected開始
		clusterNum = bundles.get(id2).getVertsConnected();
		if(clusterNum > 1)
			//System.out.println(clusterNum);
		if(clusterNum!=0){
			for(int i =0;i<mesh.getNumVertices();i++){
				if(i==id1)
					continue;
				Bundle bundle = bundles.get(i);
				if(bundle.getVertsConnected()==clusterNum){
					if(dissim[i] > edgeDensityThreshold)
						continue;
					count++;
					cluster[0]+=mesh.getVertex(i).getPosition()[0];
					cluster[1]+=mesh.getVertex(i).getPosition()[1];
				}
			}
			cluster[0] = cluster[0]/count;
			cluster[1] = cluster[1]/count;
			dis[0] = (cluster[0] - p1[0]) * mergeStrength;
			dis[1] = (cluster[1] - p1[1]) * mergeStrength;	
			p1[0] = p1[0] + dis[0];
			p1[1] = p1[1] + dis[1];
		}
		//merge処理 : connected終了
		 */

		//merge処理 : connected開始
		clusterNum = bundles.get(id2).getVertsConnected();
		if(v2.getOrder()!=null){
			for(int i =0;i<v2.getOrder().size();i++){
				Bundle bundle = v2.getOrder().get(i);
				int n = bundle.getID1();
				count++;
				cluster[0]+=mesh.getVertex(n).getPosition()[0];
				cluster[1]+=mesh.getVertex(n).getPosition()[1];
			}
			cluster[0] = cluster[0]/count;
			cluster[1] = cluster[1]/count;
			dis[0] = (cluster[0] - p1[0]) * mergeStrength;
			dis[1] = (cluster[1] - p1[1]) * mergeStrength;	
			p1[0] = p1[0] + dis[0];
			p1[1] = p1[1] + dis[1];
		}
		//merge処理 : connected終了

		//merge処理 : connecting開始
		clusterNum = bundles.get(id2).getVertsConnecting();
		if(clusterNum > 0)
			//System.out.println(clusterNum);
			if(clusterNum!=0){
				for(int i =0;i<mesh.getNumVertices();i++){
					if(i==id1)
						continue;
					Bundle bundle = bundles.get(i);
					if(bundle.getVertsConnecting()==clusterNum){
						if(dissim[i] > edgeDensityThreshold)
							continue;
						count++;
						cluster[0]+=mesh.getVertex(i).getPosition()[0];
						cluster[1]+=mesh.getVertex(i).getPosition()[1];
					}
				}
				cluster[0] = cluster[0]/count;
				cluster[1] = cluster[1]/count;
				dis[0] = (cluster[0] - p2[0]) * mergeStrength;
				dis[1] = (cluster[1] - p2[1]) * mergeStrength;	
				p2[0] = p2[0] + dis[0];
				p2[1] = p2[1] + dis[1];
			}
		//merge処理 : connecting終了

		//merge処理終わり


		//ここからrotation処理		
		double disX = v2pos[0] - v1pos[0];
		double disY = v2pos[1] - v1pos[1];
		disX/=3;
		disY/=3;

		double para=rotationStrength;
		p1[0] = p1[0] + rotation*para*disY;
		p1[1] = p1[1] - rotation*para*disX;
		p2[0] = p2[0] + rotation*para*disY;
		p2[1] = p2[1] - rotation*para*disX;

		//rotation処理終わり



		double pt[] = new double[2];
		gl2.glBegin(GL2.GL_LINE_STRIP);
		for(int i = 0; i <= NUM_T; i++) {
			double interval = 1.0 / (double)NUM_T;
			double t0 = interval * (double)i;
			double t1 = 1.0 - t0;

			for(int j = 0; j < 2; j++) 
				pt[j] = p0[j] * t1 * t1 * t1 + p1[j] * 3.0 * t0 * t1 * t1
				+ p2[j] * 3.0 * t0 * t0 * t1 + p3[j] * t0 * t0 * t0;

			double z = (z1 * (NUM_T - i) + z2 * i) / (double)NUM_T;
			double t = 0.25+t0/2.0;
			gl2.glColor3d(t,0.5,1.0-t);
			gl2.glVertex3d(pt[0], pt[1], z);

		}
		gl2.glEnd();

	}


	void drawBundledEdges(Vertex v1, Vertex v2, Node n1, Node n2,int rotation,Mesh mesh, ArrayList<Integer> startList,ArrayList<Integer> endList) {
		int NUM_T = 10;
		double ONE_THIRD = 0.33333333333;

		int cid1 = n1.getColorId();
		int cid2 = n1.getColorId();
		if(colorSwitch != null 
				&& cid1 >= 0 && colorSwitch[cid1] == false
				&& cid2 >= 0 && colorSwitch[cid2] == false) {
			return;
		}


		double p0[] = new double[2];
		double p1[] = new double[2];
		double p2[] = new double[2];
		double p3[] = new double[2];
		double v1pos[] = v1.getPosition();
		double v2pos[] = v2.getPosition();

		p0[0] = n1.getX();    p0[1] = n1.getY();
		p3[0] = n2.getX();    p3[1] = n2.getY();

		double z1 = calcZ(n1);
		double z2 = calcZ(n2);

		if(bundleShape > 0.5) { //中心を結ぶ線分上に制御点を決定
			double ratio = (bundleShape + 0.5) * 2.0 * ONE_THIRD;
			p1[0] = v1pos[0] * ratio + v2pos[0] * (1.0 - ratio);
			p1[1] = v1pos[1] * ratio + v2pos[1] * (1.0 - ratio);
			p2[0] = v2pos[0] * ratio + v1pos[0] * (1.0 - ratio);
			p2[1] = v2pos[1] * ratio + v1pos[1] * (1.0 - ratio);
		}
		else { //三分割する点と三分割する点の線分上に制御点を決定
			double ratio = bundleShape * 2.0;
			p1[0] = (v1pos[0] * 2.0 + v2pos[0]) * ONE_THIRD * ratio
					+ (p0[0] * 2.0 + p3[0]) * ONE_THIRD * (1.0 - ratio);
			p1[1] = (v1pos[1] * 2.0 + v2pos[1]) * ONE_THIRD * ratio
					+ (p0[1] * 2.0 + p3[1]) * ONE_THIRD * (1.0 - ratio);
			p2[0] = (v2pos[0] * 2.0 + v1pos[0]) * ONE_THIRD * ratio
					+ (p3[0] * 2.0 + p0[0]) * ONE_THIRD * (1.0 - ratio);
			p2[1] = (v2pos[1] * 2.0 + v1pos[1]) * ONE_THIRD * ratio
					+ (p3[1] * 2.0 + p0[1]) * ONE_THIRD * (1.0 - ratio);
		}

		//ここからmerge処理		
		int id1 = v1.getId();
		int id2 = v2.getId();
		double cluster[] = new double[2]; //クラスタの中心
		double dis[] = new double[2]; //クラスタとv1の差
		double count=0;
		cluster[0]=0;
		cluster[1]=0;
		ArrayList<Bundle> bundles = mesh.getBundles(id1);
		double dissim[] = v1.getDissim();
		int clusterNum=0;

		/*とっとく
		 //merge処理 : connected開始
		clusterNum = bundles.get(id2).getVertsConnected();
		if(clusterNum > 1)
			//System.out.println(clusterNum);
		if(clusterNum!=0){
			for(int i =0;i<mesh.getNumVertices();i++){
				if(i==id1)
					continue;
				Bundle bundle = bundles.get(i);
				if(bundle.getVertsConnected()==clusterNum){
					if(dissim[i] > edgeDensityThreshold)
						continue;
					count++;
					cluster[0]+=mesh.getVertex(i).getPosition()[0];
					cluster[1]+=mesh.getVertex(i).getPosition()[1];
				}
			}
			cluster[0] = cluster[0]/count;
			cluster[1] = cluster[1]/count;
			dis[0] = (cluster[0] - p1[0]) * mergeStrength;
			dis[1] = (cluster[1] - p1[1]) * mergeStrength;	
			p1[0] = p1[0] + dis[0];
			p1[1] = p1[1] + dis[1];
		}
		//merge処理 : connected終了
		 */

		//merge処理 : connected開始
		clusterNum = bundles.get(id2).getVertsConnected();
		if(v2.getOrder()!=null){
			for(int i =0;i<v2.getOrder().size();i++){
				Bundle bundle = v2.getOrder().get(i);
				int n = bundle.getID1();
				count++;
				cluster[0]+=mesh.getVertex(n).getPosition()[0];
				cluster[1]+=mesh.getVertex(n).getPosition()[1];
			}
			cluster[0] = cluster[0]/count;
			cluster[1] = cluster[1]/count;
			dis[0] = (cluster[0] - p1[0]) * mergeStrength;
			dis[1] = (cluster[1] - p1[1]) * mergeStrength;	
			p1[0] = p1[0] + dis[0];
			p1[1] = p1[1] + dis[1];
		}
		//merge処理 : connected終了

		//merge処理 : connecting開始
		clusterNum = bundles.get(id2).getVertsConnecting();
		if(clusterNum > 0)
			//System.out.println(clusterNum);
			if(clusterNum!=0){
				for(int i =0;i<mesh.getNumVertices();i++){
					if(i==id1)
						continue;
					Bundle bundle = bundles.get(i);
					if(bundle.getVertsConnecting()==clusterNum){
						if(dissim[i] > edgeDensityThreshold)
							continue;
						count++;
						cluster[0]+=mesh.getVertex(i).getPosition()[0];
						cluster[1]+=mesh.getVertex(i).getPosition()[1];
					}
				}
				cluster[0] = cluster[0]/count;
				cluster[1] = cluster[1]/count;
				dis[0] = (cluster[0] - p2[0]) * mergeStrength;
				dis[1] = (cluster[1] - p2[1]) * mergeStrength;	
				p2[0] = p2[0] + dis[0];
				p2[1] = p2[1] + dis[1];
			}
		//merge処理 : connecting終了

		//merge処理終わり


		//ここからrotation処理		
		double disX = v2pos[0] - v1pos[0];
		double disY = v2pos[1] - v1pos[1];
		disX/=3;
		disY/=3;

		double para=rotationStrength;
		p1[0] = p1[0] + rotation*para*disY;
		p1[1] = p1[1] - rotation*para*disX;
		p2[0] = p2[0] + rotation*para*disY;
		p2[1] = p2[1] - rotation*para*disX;

		//rotation処理終わり

		//drawBezier(p0,p1,p2,p3,z1,z2,graph.mesh,v1,v2);

		double pt[] = new double[2];
		gl2.glBegin(GL2.GL_LINE_STRIP);
		for(int i = 0; i <= NUM_T; i++) {
			double interval = 1.0 / (double)NUM_T;
			double t0 = interval * (double)i;
			double t1 = 1.0 - t0;

			for(int j = 0; j < 2; j++) 
				pt[j] = p0[j] * t1 * t1 * t1 + p1[j] * 3.0 * t0 * t1 * t1
				+ p2[j] * 3.0 * t0 * t0 * t1 + p3[j] * t0 * t0 * t0;

			double z = (z1 * (NUM_T - i) + z2 * i) / (double)NUM_T;
			double t = 0.25+t0/2.0;
			gl2.glColor3d(t,0.5,1.0-t);
			gl2.glVertex3d(pt[0], pt[1], z);

		}
		gl2.glEnd();
		

	}


	void drawBundledEdges(Vertex v1, Vertex v2, Node n1, Node n2,int rotation,Mesh mesh, double startBundlePos[],double endBundlePos[],double angle) {
		int NUM_T = 10;
		double ONE_THIRD = 0.33333333333;

		int cid1 = n1.getColorId();
		int cid2 = n1.getColorId();
		if(colorSwitch != null 
				&& cid1 >= 0 && colorSwitch[cid1] == false
				&& cid2 >= 0 && colorSwitch[cid2] == false) {
			return;
		}


		double p0[] = new double[2];
		double p1[] = new double[2];
		double p2[] = new double[2];
		double p3[] = new double[2];
		double v1pos[] = v1.getPosition();
		double v2pos[] = v2.getPosition();

		p0[0] = n1.getX();    p0[1] = n1.getY();
		p3[0] = n2.getX();    p3[1] = n2.getY();

		double z1 = calcZ(n1);
		double z2 = calcZ(n2);
/*
		p2[0] = startBundlePos[0] * mergeStrength + v2.getPosition()[0] * (1-mergeStrength);
		p2[1] = startBundlePos[1] * mergeStrength + v2.getPosition()[1] * (1-mergeStrength);
		//p1[2] = startBundlePos[2] * (1-mergeStrength/2.0) + v2.getPosition()[2] * mergeStrength/2.0;

		p1[0] = endBundlePos[0] * mergeStrength + v1.getPosition()[0] * (1-mergeStrength);
		p1[1] = endBundlePos[1] * mergeStrength + v1.getPosition()[1] * (1-mergeStrength);
		//p2[2] = endBundlePos[2] * (1-mergeStrength/2.0) + v1.getPosition()[2] * mergeStrength/2.0;
*/
		p2[0] = startBundlePos[0] * mergeStrength + v2.getPosition()[0] * (1-mergeStrength) + (n2.getX() - v2.getPosition()[0]);
		p2[1] = startBundlePos[1] * mergeStrength + v2.getPosition()[1] * (1-mergeStrength) + (n2.getY() - v2.getPosition()[1]);
		//p1[2] = startBundlePos[2] * (1-mergeStrength/2.0) + v2.getPosition()[2] * mergeStrength/2.0;

		p1[0] = endBundlePos[0] * mergeStrength + v1.getPosition()[0] * (1-mergeStrength) + (n1.getX() - v1.getPosition()[0]);
		p1[1] = endBundlePos[1] * mergeStrength + v1.getPosition()[1] * (1-mergeStrength) + (n1.getY() - v1.getPosition()[1]);
		//p2[2] = endBundlePos[2] * (1-mergeStrength/2.0) + v1.getPosition()[2] * mergeStrength/2.0;

		
		
		//ここからrotation処理		
/*
		double disX = v2pos[0] - v1pos[0];
		double disY = v2pos[1] - v1pos[1];
		disX/=3;
		disY/=3;

		double para=rotationStrength;
		p1[0] = p1[0] + rotation*para*disY;
		p1[1] = p1[1] - rotation*para*disX;
		p2[0] = p2[0] + rotation*para*disY;
		p2[1] = p2[1] - rotation*para*disX;
		*/
		

		//rotation処理終わり



		double pt[] = new double[2];
		gl2.glBegin(GL2.GL_LINE_STRIP);
		for(int i = 0; i <= NUM_T; i++) {
			double interval = 1.0 / (double)NUM_T;
			double t0 = interval * (double)i;
			double t1 = 1.0 - t0;

			for(int j = 0; j < 2; j++) 
				pt[j] = p0[j] * t1 * t1 * t1 + p1[j] * 3.0 * t0 * t1 * t1
				+ p2[j] * 3.0 * t0 * t0 * t1 + p3[j] * t0 * t0 * t0;

			double z = (z1 * (NUM_T - i) + z2 * i) / (double)NUM_T;
			double t = 0.25+t0/2.0;
			gl2.glColor3d(t,0.5,1.0-t);
			//gl2.glColor3d(angle/2,angle/2,angle/2);
			gl2.glVertex3d(pt[0], pt[1], z);

		}

		gl2.glEnd();

		gl2.glBegin(GL2.GL_LINE_STRIP);
		float color[] = {(float)0.2,(float)0.5,(float)0.0};

		gl2.glMaterialfv(GL.GL_FRONT_AND_BACK,
				GL2.GL_AMBIENT_AND_DIFFUSE, color, 0);
		//drawOneBarWithHeight(p1[0], p1[1], 0, 100);
		drawOneBarWithHeight(v1.getPosition()[0], v1.getPosition()[1], 0, 1000);

	}

	void drawBundledEdges_(Vertex v1, Vertex v2, Node n1, Node n2,int rotation,Mesh mesh, double startBundlePos[],double endBundlePos[],double angle) {
		int NUM_T = 10;
		double ONE_THIRD = 0.33333333333;

		int cid1 = n1.getColorId();
		int cid2 = n1.getColorId();
		if(colorSwitch != null 
				&& cid1 >= 0 && colorSwitch[cid1] == false
				&& cid2 >= 0 && colorSwitch[cid2] == false) {
			return;
		} 


		double p0[] = new double[2];
		double p1[] = new double[2];
		double p2[] = new double[2];
		double p3[] = new double[2];
		double v1pos[] = v1.getPosition();
		double v2pos[] = v2.getPosition();
		double vec1[] = new double[2];
		double vec2[] = new double[2];
		double v;

		p0[0] = n1.getX();    p0[1] = n1.getY();
		p3[0] = n2.getX();    p3[1] = n2.getY();

		double z1 = calcZ(n1);
		double z2 = calcZ(n2);

		/*
		vec1[0] = endBundlePos[0] - v1pos[0];
		vec1[1] = endBundlePos[1] - v1pos[1];
		vec2[0] = startBundlePos[0] - v2pos[0];
		vec2[1] = startBundlePos[1] - v2pos[1];

		v = Math.sqrt(vec1[0]*vec1[0] + vec1[1]*vec1[1]);
		if(v!=0){
			vec1[0] /= v;
			vec1[1] /= v;
		}

		v = Math.sqrt(vec2[0]*vec2[0] + vec2[1]*vec2[1]);
		if(v!=0){
			vec2[0] /= v;
			vec2[1] /= v;
		}
		//System.out.println("v:"+v+", vec2[0]:"+vec2[0]+" , vec2[1]:"+vec2[1]);

		p1[0] = v1pos[0] + vec1[0]*(mergeStrength*ConfluenceThreshold*0.1);
		p1[1] = v1pos[1] + vec1[1]*(mergeStrength*ConfluenceThreshold*0.1);
		p2[0] = v2pos[0] + vec2[0]*(mergeStrength*ConfluenceThreshold*0.1);
		p2[1] = v2pos[1] + vec2[1]*(mergeStrength*ConfluenceThreshold*0.1);
		 */
		if(startBundlePos==null){
			vec1[0] = vec1[1] = 0;
		}else{
			vec1[0] = startBundlePos[0] - v1pos[0];
			vec1[1] = startBundlePos[1] - v1pos[1];
		}
		if(endBundlePos==null){
			vec2[0] = vec2[1] = 0;
		}else{
			vec2[0] = endBundlePos[0] - v2pos[0];
			vec2[1] = endBundlePos[1] - v2pos[1];
		}	
		
		/*
		v = Math.sqrt(vec1[0]*vec1[0] + vec1[1]*vec1[1]);
		if(v!=0){
			vec1[0] /= v;
			vec1[1] /= v;
		}

		v = Math.sqrt(vec2[0]*vec2[0] + vec2[1]*vec2[1]);
		if(v!=0){
			vec2[0] /= v;
			vec2[1] /= v;
		}
		*/
		
		
		//System.out.println("v:"+v+", vec2[0]:"+vec2[0]+" , vec2[1]:"+vec2[1]);
/*
		p1[0] = v1pos[0] + vec1[0]*(mergeStrength*ConfluenceThreshold*1) + (v1pos[0]-p0[0])*(mergeStrength*ConfluenceThreshold*0.1);
		p1[1] = v1pos[1] + vec1[1]*(mergeStrength*ConfluenceThreshold*1) + (v1pos[1]-p0[1])*(mergeStrength*ConfluenceThreshold*0.1);
		p2[0] = v2pos[0] + vec2[0]*(mergeStrength*ConfluenceThreshold*1) + (v2pos[0]-p3[0])*(mergeStrength*ConfluenceThreshold*0.1);
		p2[1] = v2pos[1] + vec2[1]*(mergeStrength*ConfluenceThreshold*1) + (v2pos[1]-p3[1])*(mergeStrength*ConfluenceThreshold*0.1);
*/
		p1[0] = v1pos[0] + vec1[0]*mergeStrength + (v1pos[0]-p0[0])*(mergeStrength*0.1);
		p1[1] = v1pos[1] + vec1[1]*mergeStrength + (v1pos[1]-p0[1])*(mergeStrength*0.1);
		p2[0] = v2pos[0] + vec2[0]*mergeStrength + (v2pos[0]-p3[0])*(mergeStrength*0.1);
		p2[1] = v2pos[1] + vec2[1]*mergeStrength + (v2pos[1]-p3[1])*(mergeStrength*0.1);
		
		if(startBundlePos!=null && endBundlePos!=null){
			p1[0] = v1pos[0] + vec1[0]*mergeStrength*0.5 + (v1pos[0]-p0[0])*(mergeStrength*0.5);
			p1[1] = v1pos[1] + vec1[1]*mergeStrength*0.5 + (v1pos[1]-p0[1])*(mergeStrength*0.5);
			p2[0] = v2pos[0] + vec2[0]*mergeStrength*0.5 + (v2pos[0]-p3[0])*(mergeStrength*0.5);
			p2[1] = v2pos[1] + vec2[1]*mergeStrength*0.5 + (v2pos[1]-p3[1])*(mergeStrength*0.5);
		}

		//if(startBundlePos!=null || endBundlePos!=null){
			//Kyokuritsu(p0[0],p0[1],p1[0],p1[1],p2[0],p2[1],p3[0],p3[1]);
		//}
		//Kyokuritsu(p0[0],p0[1],p1[0],p1[1],p2[0],p2[1],p3[0],p3[1]);
		//System.out.println(p0[0] + ","+p1[1]);
		
		drawBezier(p0,p1,p2,p3,z1,z2,mesh);
		//drawSpline(p0,p1,p2,p3,z1,z2,mesh,v1,v2);
		/*
		double pt[] = new double[2];
		gl2.glBegin(GL2.GL_LINE_STRIP);
		for(int i = 0; i <= NUM_T; i++) {
			double interval = 1.0 / (double)NUM_T;
			double t0 = interval * (double)i;
			double t1 = 1.0 - t0;

			for(int j = 0; j < 2; j++) 
				pt[j] = p0[j] * t1 * t1 * t1 + p1[j] * 3.0 * t0 * t1 * t1
				+ p2[j] * 3.0 * t0 * t0 * t1 + p3[j] * t0 * t0 * t0;

			double z = (z1 * (NUM_T - i) + z2 * i) / (double)NUM_T;
			double t = 0.25+t0/2.0;
			gl2.glColor3d(t,0.5,1.0-t);
			//gl2.glColor3d(angle/2,angle/2,angle/2);
			gl2.glVertex3d(pt[0], pt[1], z);

		}

		gl2.glEnd();
		*/
		
		
		
		/*
		gl2.glBegin(GL2.GL_POLYGON);
		gl2.glColor3d(0.0,1.0,1.0);
		gl2.glVertex3d(p1[0] - 0.0075, p1[1] + 0.0075, 0.0);
		gl2.glVertex3d(p1[0] - 0.0075, p1[1] - 0.0075, 0.0);
		gl2.glVertex3d(p1[0] + 0.0075, p1[1] - 0.0075, 0.0);
		gl2.glVertex3d(p1[0] + 0.0075, p1[1] + 0.0075, 0.0);
		gl2.glEnd();
		
		gl2.glBegin(GL2.GL_POLYGON);
		gl2.glColor3d(0.0,1.0,1.0);
		gl2.glVertex3d(p2[0] - 0.0075, p2[1] + 0.0075, 0.0);
		gl2.glVertex3d(p2[0] - 0.0075, p2[1] - 0.0075, 0.0);
		gl2.glVertex3d(p2[0] + 0.0075, p2[1] - 0.0075, 0.0);
		gl2.glVertex3d(p2[0] + 0.0075, p2[1] + 0.0075, 0.0);
		gl2.glEnd();
		*/
		

		gl2.glBegin(GL2.GL_LINE_STRIP);
		float color[] = {(float)0.2,(float)0.5,(float)0.0};

		gl2.glMaterialfv(GL.GL_FRONT_AND_BACK,
				GL2.GL_AMBIENT_AND_DIFFUSE, color, 0);
		//drawOneBarWithHeight(p1[0], p1[1], 0, 100);
		drawOneBarWithHeight(v1.getPosition()[0], v1.getPosition()[1], 0, 1000);

	}	
	
	void drawSpline(double p0[], double p1[],double p2[],double p3[],double z1, double z2, Mesh mesh, Vertex v1, Vertex v2){
		int NUM_T = 100;
		double pt[] = new double[2];
		gl2.glBegin(GL2.GL_LINE_STRIP);
		
		double p[][] = new double[8][2];
		p[0][0] = p0[0];
		p[0][1] = p0[1];
		p[1][0] = p0[0];
		p[1][1] = p0[1];
		p[2][0] = p0[0];
		p[2][1] = p0[1];

		p[3][0] = p1[0];
		p[3][1] = p1[1];
		p[4][0] = p2[0];
		p[4][1] = p2[1];

		p[5][0] = p3[0];
		p[5][1] = p3[1];
		p[6][0] = p3[0];
		p[6][1] = p3[1];
		p[7][0] = p3[0];
		p[7][1] = p3[1];
		/*
		p[1] = p0;
		p[2] = p0;
		p[3] = p1;
		p[4] = p2;
		p[5] = p3;
		p[6] = p3;
		p[7] = p3;
		*/
		
		double interval = 6.0 / (double)NUM_T;
		for(int i = 0; i <= NUM_T*5.0/6.0; i++) {
			double t = interval * (double)i - 1.0;
			double z = (z1 * (NUM_T - i) + z2 * i) / (double)NUM_T;
			//double col = 0.25+i/(NUM_T*2.0);
			double col = (double)i*6.0/(NUM_T*5.0); 
			gl2.glColor3d(col*0.6+0.4,0.4,1.0-col*0.6);
			//gl2.glColor3d(col,0.5,1.0-col);
			double x = 0;
			double y = 0;
			
			for(int j=-2; j<6; j++){
				x+=w(t-j)*p[j+2][0];
				y+=w(t-j)*p[j+2][1];
				//System.out.println(w(t-j)+","+p[j+2][0]+","+x+","+","+y);
			}
			//System.out.println(x+","+y);
			gl2.glVertex3d(x, y, z);
			
			//System.out.println(interval*(double)NUM_T - 1.0);
		}	
		gl2.glEnd();
	}
	
	double w(double t){
		if(-1<=t && t<=1){
			return (3.0*t*t*Math.abs(t) - 6.0*t*t +4)/6.0;
		}else if(-2<=t && t<=2){
			return ((Math.abs(t)-2)*(Math.abs(t)-2)*(Math.abs(t)-2))/(-6.0);
		}else{
			return 0.0;
		}
	}
	
	void drawBezier_avoiding(double p0[], double p1[],double p2[],double p3[],double z1, double z2, Mesh mesh, Vertex v1, Vertex v2){
		int NUM_T = 100;
		double pt[] = new double[2];
		gl2.glBegin(GL2.GL_LINE_STRIP);
		
		
		for(int i = 0; i <= NUM_T; i++) {
			double interval = 1.0 / (double)NUM_T;
			double t0 = interval * (double)i;
			double t1 = 1.0 - t0;

			for(int j = 0; j < 2; j++) 
				pt[j] = p0[j] * t1 * t1 * t1 + p1[j] * 3.0 * t0 * t1 * t1
				+ p2[j] * 3.0 * t0 * t0 * t1 + p3[j] * t0 * t0 * t0;

			double z = (z1 * (NUM_T - i) + z2 * i) / (double)NUM_T;
			double t = 0.25+t0/2.0;
			gl2.glColor3d(t,0.5,1.0-t);
			//gl2.glColor3d(angle/2,angle/2,angle/2);
			int x = (int)((pt[0]+1)/2.0 * mesh.mapSize);
			int y = (int)((pt[1]+1)/2.0 * mesh.mapSize);
			
			if(0<=x && x<=mesh.mapSize && 0<=y && y<=mesh.mapSize){
				if(mesh.map[x][y]!=0 && mesh.map[x][y]!=v1.getId()  && mesh.map[x][y]!=v2.getId()){
					int id = mesh.map[x][y];
					Vertex vertex = mesh.getVertices().get(id);
					double vpos[] = vertex.getPosition();
					double vecx = pt[0] - vpos[0];
					double vecy = pt[1] - vpos[1];
					double veclen = Math.sqrt(vecx*vecx + vecy*vecy);
					pt[0] = vpos[0] + vecx*(mesh.CLUSTER_NODE_DISTANCE*(1.0-mesh.keyEmphasis)/veclen * 4);
					pt[1] = vpos[1] + vecy*(mesh.CLUSTER_NODE_DISTANCE*(1.0-mesh.keyEmphasis)/veclen * 4);
				}
			}
			
			gl2.glVertex3d(pt[0], pt[1], z);
		}	
		gl2.glEnd();
	}
	
	//Bezier曲線の描画
	//p0, p3 : 始点終点
	//p1, p2 : 制御点
	void drawBezier(double p0[], double p1[],double p2[],double p3[],double z1, double z2, Mesh mesh){
		int NUM_T = 10;
		double pt[] = new double[2];
		

		gl2.glEnable(GL2.GL_BLEND);
		gl2.glBlendFunc(GL2.GL_ZERO,  GL2.GL_SRC_COLOR);
		gl2.glBegin(GL2.GL_LINE_STRIP);
		
		for(int i = 0; i <= NUM_T; i++) {
			double interval = 1.0 / (double)NUM_T;
			double t0 = interval * (double)i;
			double t1 = 1.0 - t0;

			for(int j = 0; j < 2; j++) 
				pt[j] = p0[j] * t1 * t1 * t1 + p1[j] * 3.0 * t0 * t1 * t1
				+ p2[j] * 3.0 * t0 * t0 * t1 + p3[j] * t0 * t0 * t0;

			double z = (z1 * (NUM_T - i) + z2 * i) / (double)NUM_T;
			double t = 0.25+t0/2.0;
			//gl2.glColor3d(t0,0,1.0-t0);
			gl2.glColor4d(t0*0.6+0.4,0.4,1.0-t0*0.6,0.8);
			//gl2.glColor4d(0.25 + t0*0.5, 0.25 + t0*0.5,0.25 + t0*0.5 , 1.0-t0);
			//gl2.glColor3d(angle/2,angle/2,angle/2);
			int x = (int)((pt[0]+1)/2.0 * mesh.mapSize);
			int y = (int)((pt[1]+1)/2.0 * mesh.mapSize);
			
			gl2.glVertex3d(pt[0], pt[1], z);
		}	
		gl2.glEnd();
		gl2.glDisable(GL2.GL_BLEND);
	}
	
	/*t0.1ごとに曲率半径を求める*/
	void Kyokuritsu(double x1, double y1 ,double x2, double y2, double x3, double y3, double x4, double y4){

		for(double t = 0; t <= 1;t+=0.1){
			double tp = 1-t;
			double dx = 3*(t*t*(x4-x3)+2*t*tp*(x3-x2)+tp*tp*(x2-x1));
			double dy = 3*(t*t*(y4-y3)+2*t*tp*(y3-y2)+tp*tp*(y2-y1));
			double ddx = 6*(t*(x4-2*x3+x2)+(1-t)*(x3-2*x2+x1));
			double ddy = 6*(t*(y4-2*y3+y2)+(1-t)*(y3-2*y2+y1));
			double kyokuritsu = Math.pow(dx*dx + dy*dy, 3.0/2.0) / (dx*ddy - dy*ddx);
			System.out.println("kyokuritsu : "+kyokuritsu);
		}
	}

	void drawBundledEdges(Vertex v1, Vertex v2, Node n1, Node n2,int rotation,double angle) {
		int NUM_T = 10;
		double ONE_THIRD = 0.33333333333;

		int cid1 = n1.getColorId();
		int cid2 = n1.getColorId();
		if(colorSwitch != null 
				&& cid1 >= 0 && colorSwitch[cid1] == false
				&& cid2 >= 0 && colorSwitch[cid2] == false) {
			return;
		}


		double p0[] = new double[2];
		double p1[] = new double[2];
		double p2[] = new double[2];
		double p3[] = new double[2];
		double v1pos[] = v1.getPosition();
		double v2pos[] = v2.getPosition();

		p0[0] = n1.getX();    p0[1] = n1.getY();
		p3[0] = n2.getX();    p3[1] = n2.getY();

		double z1 = calcZ(n1);
		double z2 = calcZ(n2);

		if(bundleShape > 0.5) { //中心を結ぶ線分上に制御点を決定
			double ratio = (bundleShape + 0.5) * 2.0 * ONE_THIRD;
			p1[0] = v1pos[0] * ratio + v2pos[0] * (1.0 - ratio);
			p1[1] = v1pos[1] * ratio + v2pos[1] * (1.0 - ratio);
			p2[0] = v2pos[0] * ratio + v1pos[0] * (1.0 - ratio);
			p2[1] = v2pos[1] * ratio + v1pos[1] * (1.0 - ratio);
		}
		else { //三分割する点と三分割する点の線分上に制御点を決定
			double ratio = bundleShape * 2.0;
			p1[0] = (v1pos[0] * 2.0 + v2pos[0]) * ONE_THIRD * ratio
					+ (p0[0] * 2.0 + p3[0]) * ONE_THIRD * (1.0 - ratio);
			p1[1] = (v1pos[1] * 2.0 + v2pos[1]) * ONE_THIRD * ratio
					+ (p0[1] * 2.0 + p3[1]) * ONE_THIRD * (1.0 - ratio);
			p2[0] = (v2pos[0] * 2.0 + v1pos[0]) * ONE_THIRD * ratio
					+ (p3[0] * 2.0 + p0[0]) * ONE_THIRD * (1.0 - ratio);
			p2[1] = (v2pos[1] * 2.0 + v1pos[1]) * ONE_THIRD * ratio
					+ (p3[1] * 2.0 + p0[1]) * ONE_THIRD * (1.0 - ratio);
		}

		//ここからrotation処理		

		double disX = v2pos[0] - v1pos[0];
		double disY = v2pos[1] - v1pos[1];
		disX/=3;
		disY/=3;
		double para=rotationStrength;
		p1[0] = p1[0] - rotation*para*disY;
		p1[1] = p1[1] + rotation*para*disX;
		p2[0] = p2[0] - rotation*para*disY;
		p2[1] = p2[1] + rotation*para*disX;
		

		drawBezier(p0,p1,p2,p3,z1,z2,graph.mesh);

		/*
		double pt[] = new double[2];
		gl2.glBegin(GL2.GL_LINE_STRIP);
		//gl2.glColor3d(0.3,0.7,0.3);

		//gl2.glColor3d(angle/2,angle/2,angle/2);
		for(int i = 0; i <= NUM_T; i++) {
			double interval = 1.0 / (double)NUM_T;
			double t0 = interval * (double)i;
			double t1 = 1.0 - t0;

			for(int j = 0; j < 2; j++) 
				pt[j] = p0[j] * t1 * t1 * t1 + p1[j] * 3.0 * t0 * t1 * t1
				+ p2[j] * 3.0 * t0 * t0 * t1 + p3[j] * t0 * t0 * t0;

			double z = (z1 * (NUM_T - i) + z2 * i) / (double)NUM_T;
			double t = 0.25+t0/2.0;
			gl2.glColor3d(t,0.5,1.0-t);
			//gl2.glColor3d(angle/2,angle/2,angle/2);
			gl2.glVertex3d(pt[0], pt[1], z);


		}
		gl2.glEnd();
		*/
		

	}	

	void drawNodes() {
		if(graph == null) return;
		float colf[] = new float[3];

		// Draw plots
		double SQUARE_SIZE = 0.01 / trans.getViewScaleX();
		//double SQUARE_SIZE = 0.0075 / trans.getViewScaleX();
		for(int i = 0; i < graph.nodes.size(); i++) {
			Node node = (Node)graph.nodes.get(i);
			double x = node.getX();
			double y = node.getY();			

			if(colorMode == Canvas.COLOR_DEGREE) {
				double dratio = (double)(node.getNumConnectedEdge() + node.getNumConnectingEdge()) / (double)graph.maxDegree;
				//dratio = Math.sqrt(dratio);
				double rr = 1.0 * dratio + 0.5 * (1.0 - dratio);
				double gg = 0.0 * dratio + 0.5 * (1.0 - dratio);
				double bb = 0.0 * dratio + 0.5 * (1.0 - dratio);
				double z = calcZ(node);
				if(z > 0.01) {
					colf[0] = (float)rr;
					colf[1] = (float)gg;
					colf[2] = (float)bb;
					gl2.glMaterialfv(GL.GL_FRONT_AND_BACK,
							GL2.GL_AMBIENT_AND_DIFFUSE, colf, 0);
					drawOneBarWithHeight(x, y, z, SQUARE_SIZE);

				}
				else {
					double ke2 = graph.mesh.keyEmphasis * 0.5;
					colf[0] = (float)(rr * (1.0 - ke2) + ke2);
					colf[1] = (float)(gg * (1.0 - ke2) + ke2);
					colf[2] = (float)(bb * (1.0 - ke2) + ke2);
					gl2.glMaterialfv(GL.GL_FRONT_AND_BACK,
							GL2.GL_AMBIENT_AND_DIFFUSE, colf, 0);
					gl2.glBegin(GL2.GL_POLYGON);
					gl2.glVertex3d(x - SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
					gl2.glVertex3d(x - SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
					gl2.glVertex3d(x + SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
					gl2.glVertex3d(x + SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
					gl2.glEnd();
				}
				continue;
			}

			int colorId = node.getColorId();
			if(colorId < 0) {
				colf[0] = colf[1] = colf[2] = 0.5f;
				gl2.glMaterialfv(GL.GL_FRONT_AND_BACK,
						GL2.GL_AMBIENT_AND_DIFFUSE, colf, 0);
				gl2.glBegin(GL2.GL_POLYGON);
				gl2.glVertex3d(x - SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
				gl2.glVertex3d(x - SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
				gl2.glVertex3d(x + SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
				gl2.glVertex3d(x + SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
				gl2.glEnd();
			}
			else if(colorSwitch != null && colorSwitch[colorId] == false) {
				continue;
			}
			else {
				Color color = VectorParettePanel.calcColor(colorId, graph.vectorname.length);
				double rr = (double)color.getRed() / 255.0;
				double gg = (double)color.getGreen() / 255.0;
				double bb = (double)color.getBlue() / 255.0;
				double z = calcZ(node);
				if(z > 0.01) {
					colf[0] = (float)rr;
					colf[1] = (float)gg;
					colf[2] = (float)bb;
					gl2.glMaterialfv(GL.GL_FRONT_AND_BACK,
							GL2.GL_AMBIENT_AND_DIFFUSE, colf, 0);
					drawOneBarWithHeight(x, y, z, SQUARE_SIZE);

				}
				else {
					double ke2 = graph.mesh.keyEmphasis * 0.5;
					colf[0] = (float)(rr * (1.0 - ke2) + ke2);
					colf[1] = (float)(gg * (1.0 - ke2) + ke2);
					colf[2] = (float)(bb * (1.0 - ke2) + ke2);
					gl2.glMaterialfv(GL.GL_FRONT_AND_BACK,
							GL2.GL_AMBIENT_AND_DIFFUSE, colf, 0);
					gl2.glBegin(GL2.GL_POLYGON);
					gl2.glVertex3d(x - SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
					gl2.glVertex3d(x - SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
					gl2.glVertex3d(x + SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
					gl2.glVertex3d(x + SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
					gl2.glEnd();
				}
			}

		}


		// Draw annotation
		if(pickedNode != null && pickedNode.getNumDescription() > 0) {
			String line = pickedNode.getDescription(0);
			for(int i = 1; i < pickedNode.getNumDescription(); i++) 
				line += " " + pickedNode.getDescription(i);
			glu2.gluUnProject(0.0, 0.0, 0.0, modelview, projection, viewport, p1);
			gl2.glColor3d(0.7, 0.0, 0.0);
			writeOneString(p1.get(0), p1.get(1), line);
		}
	}


	void drawOneBarWithHeight(double x, double y, double z, double SQUARE_SIZE) {

		gl2.glBegin(GL2.GL_POLYGON);
		gl2.glVertex3d(x - SQUARE_SIZE, y + SQUARE_SIZE, z);
		gl2.glVertex3d(x - SQUARE_SIZE, y - SQUARE_SIZE, z);
		gl2.glVertex3d(x + SQUARE_SIZE, y - SQUARE_SIZE, z);
		gl2.glVertex3d(x + SQUARE_SIZE, y + SQUARE_SIZE, z);
		gl2.glEnd();
		gl2.glBegin(GL2.GL_POLYGON);
		gl2.glVertex3d(x - SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
		gl2.glVertex3d(x - SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
		gl2.glVertex3d(x - SQUARE_SIZE, y - SQUARE_SIZE, z);
		gl2.glVertex3d(x - SQUARE_SIZE, y + SQUARE_SIZE, z);
		gl2.glEnd();
		gl2.glBegin(GL2.GL_POLYGON);
		gl2.glVertex3d(x + SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
		gl2.glVertex3d(x + SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
		gl2.glVertex3d(x + SQUARE_SIZE, y - SQUARE_SIZE, z);
		gl2.glVertex3d(x + SQUARE_SIZE, y + SQUARE_SIZE, z);
		gl2.glEnd();
		gl2.glBegin(GL2.GL_POLYGON);
		gl2.glVertex3d(x - SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
		gl2.glVertex3d(x + SQUARE_SIZE, y - SQUARE_SIZE, 0.0);
		gl2.glVertex3d(x + SQUARE_SIZE, y - SQUARE_SIZE, z);
		gl2.glVertex3d(x - SQUARE_SIZE, y - SQUARE_SIZE, z);
		gl2.glEnd();
		gl2.glBegin(GL2.GL_POLYGON);
		gl2.glVertex3d(x - SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
		gl2.glVertex3d(x + SQUARE_SIZE, y + SQUARE_SIZE, 0.0);
		gl2.glVertex3d(x + SQUARE_SIZE, y + SQUARE_SIZE, z);
		gl2.glVertex3d(x - SQUARE_SIZE, y + SQUARE_SIZE, z);
		gl2.glEnd();
	}

	public Object pick(int cx, int cy) {
		double PICK_DIST = 20.0;

		if(graph == null) return null;
		pickedNode = null;
		double dist = 1.0e+30;
		cy = viewport.get(3) - cy + 1;

		for(int i = 0; i < graph.nodes.size(); i++) {
			Node node = (Node)graph.nodes.get(i);
			double x = node.getX();
			double y = node.getY();
			glu2.gluProject(x, y, 0.0, modelview, projection, viewport, p1);
			double xx = p1.get(0);
			double yy = p1.get(1);
			double dd = (cx - xx) * (cx - xx) + (cy - yy) * (cy - yy);
			if(dd < PICK_DIST && dd < dist) {
				dist = dd;    pickedNode = node;
			}
		}

		return (Object)pickedNode;

	}


	void writeOneString(double x, double y, String word) {
		gl2.glRasterPos3d(x, y, 0.01);
		glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, word);
	}


	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
	}

}