package ocha.itolab.koala.applet.koalaview;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import ocha.itolab.koala.core.data.*;
import ocha.itolab.koala.core.mesh.*;

public class ViewingPanel extends JPanel {
	static int SMOOTHING_ITERATION = 30;
	
	public JButton  fileOpenButton, placeAgainButton, viewResetButton;
	public JSlider  bundleDensitySlider, confluenceDensitySlider, bundleShapeSlider, transparencySlider,
		placeRatioSlider, clusteringRatioSlider, clusteringRatioSlider2, clusterSizeSlider, clusterSizeSlider2, keyEmphasisSlider, rotationSlider,mergeSlider;
	public JRadioButton edgeDissimilarityButton, edgeDegreeButton, clickButton, moveButton, 
		colorTopicButton, colorDegreeButton;
	public Container container;
	JTabbedPane pane = null;
	VectorPanel vecpanel = null;
	TextPanel textpanel = null;
	
	
	/* Selective canvas */
	Canvas canvas;
	CursorListener listener;
	FileOpener fileOpener;
	Graph graph;
	File currentDirectory = null;
	
	/* Action listener */
	ButtonListener bl = null;
	RadioButtonListener rbl = null;
	CheckBoxListener cbl = null;
	SliderListener sl = null;
	
	public ViewingPanel() {
		// super class init
		super();
		setSize(300, 800);
		
		//
		// ファイル入力のパネル
		//
		JPanel p1 = new JPanel();
		p1.setLayout(new GridLayout(2,1));
		//p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
		fileOpenButton = new JButton("File Open");
		viewResetButton = new JButton("View Reset");
		p1.add(fileOpenButton);
		p1.add(viewResetButton);
		
		JPanel p1_ = new JPanel();
		p1_.setLayout(new GridLayout(3,1));
		
		BevelBorder border = new BevelBorder(BevelBorder.LOWERED);
		
		JPanel p1_1 = new JPanel();
		p1_1.setLayout(new GridLayout(2,1));
		edgeDissimilarityButton = new JRadioButton("Num. Edge by Dissimilarity");
		edgeDegreeButton = new JRadioButton("Num. Edge by Degree");
		ButtonGroup group1 = new ButtonGroup();
		p1_1.add(edgeDissimilarityButton);
		p1_1.add(edgeDegreeButton);
		group1.add(edgeDissimilarityButton);
		group1.add(edgeDegreeButton);
		p1_1.setBorder(border);
		p1_.add(p1_1);
		
		clickButton = new JRadioButton("React by Click");
		moveButton = new JRadioButton("React by Move");
		ButtonGroup group2 = new ButtonGroup();
		JPanel p1_2 = new JPanel();
		p1_2.setLayout(new GridLayout(2,1));
		p1_2.add(clickButton);
		p1_2.add(moveButton);
		group2.add(clickButton);
		group2.add(moveButton);
		p1_2.setBorder(border);
		p1_.add(p1_2);
		
		colorTopicButton = new JRadioButton("Topic Color");
		colorDegreeButton = new JRadioButton("Degree Color");
		ButtonGroup group3 = new ButtonGroup();
		JPanel p1_3 = new JPanel();
		p1_3.setLayout(new GridLayout(2,1));
		p1_3.add(colorTopicButton);
		p1_3.add(colorDegreeButton);
		group3.add(colorTopicButton);
		group3.add(colorDegreeButton);
		p1_3.setBorder(border);
		p1_.add(p1_3);
		
		//エッジのパネル
		JPanel p2 = new JPanel();
		//p2.setLayout(new GridLayout(14,1));
		p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
		bundleDensitySlider = new JSlider(0, 100, 10);
		p2.add(new JLabel("Num. of Drawn Edges"));
		p2.add(bundleDensitySlider);
		bundleShapeSlider = new JSlider(0, 100, 70);
		p2.add(new JLabel("Bundle Shape (Linear <-> Curved)"));
		p2.add(bundleShapeSlider);
		confluenceDensitySlider = new JSlider(0, 100, 50);
		p2.add(new JLabel("Confluence Edges"));
		p2.add(confluenceDensitySlider);
		transparencySlider = new JSlider(0, 100, 50);
		p2.add(new JLabel("Background Transparency"));
		p2.add(transparencySlider);
		keyEmphasisSlider = new JSlider(0, 100, 50);
		p2.add(new JLabel("Key Node Emphasis"));
		p2.add(keyEmphasisSlider);
		rotationSlider = new JSlider(0, 100, 50);
		p2.add(new JLabel("Interactive Edges"));
		p2.add(rotationSlider);
		mergeSlider = new JSlider(0, 100, 50);
		p2.add(new JLabel("Merge Edges"));
		p2.add(mergeSlider);
		
		// スライダのパネル
		JPanel p3 = new JPanel();
		//p3.setLayout(new GridLayout(9, 1));
		p3.setLayout(new BoxLayout(p3, BoxLayout.Y_AXIS));
		p3.add(new JLabel(" "));
		placeAgainButton = new JButton("Place Again");
		p3.add(placeAgainButton);
		p3.add(new JLabel(" "));
		p3.add(new JLabel(" "));
		placeRatioSlider = new JSlider(0, 100, 50);
		p3.add(new JLabel("Distance Ratio for Layout"));
		p3.add(placeRatioSlider);
		p3.add(new JLabel(" "));
		clusteringRatioSlider = new JSlider(0, 100, 50);
		p3.add(new JLabel("Distance Ratio for Clustering1 (d<->c)"));
		p3.add(clusteringRatioSlider);
		p3.add(new JLabel(" "));
		clusteringRatioSlider2 = new JSlider(0, 100, 50);
		p3.add(new JLabel("Distance Ratio for Clustering2 (d<->c)"));
		p3.add(clusteringRatioSlider2);
		p3.add(new JLabel(" "));
		clusterSizeSlider = new JSlider(0, 100, 50);
		p3.add(new JLabel("Cluster Size Ratio1 (Small <-> Large)"));
		p3.add(clusterSizeSlider);
		p3.add(new JLabel(" "));
		clusterSizeSlider2 = new JSlider(0, 100, 70);
		p3.add(new JLabel("Cluster Size Ratio2 (Small <-> Large)"));
		p3.add(clusterSizeSlider2);
		
		//
		// パネル群のレイアウト
		//
		
		/*もともとのレイアウト
		JPanel pp = new JPanel();
		pp.setLayout(new BoxLayout(pp, BoxLayout.Y_AXIS));
		pp.add(p1);
		pp.add(p2);
		pp.add(p3);
		pp.add(p1_);
		//pp.add(p1);
		
		pane = new JTabbedPane();
		pane.add(pp);
		pane.setTabComponentAt(0, new JLabel("Main"));
		this.add(pane);
		*/
		
		pane = new JTabbedPane();
		JPanel pp = new JPanel();
		//pp.setLayout(new GridLayout(2,1));
		pp.setLayout(new BoxLayout(pp, BoxLayout.Y_AXIS));
		pp.add(p1);
		pp.add(p1_);
		pane.addTab("Main",pp);
		pane.addTab("Edges",p2);
		pane.addTab("Nodes",p3);
		this.add(pane);
		
		
		
		
		
		//
		// リスナーの追加
		//
		if (bl == null)
			bl = new ButtonListener();
		addButtonListener(bl);

		if (rbl == null)
			rbl = new RadioButtonListener();
		addRadioButtonListener(rbl);
		
		if (cbl == null)
			cbl = new CheckBoxListener();
		addCheckBoxListener(cbl);
		
		if (sl == null)
			sl = new SliderListener();
		addSliderListener(sl);
	}
	
	

	
	/**
	 * Canvasをセットする
	 * @param c Canvas
	 */
	public void setCanvas(Object c) {
		canvas = (Canvas) c;
		canvas.setViewingPanel(this);
	}
	
	/**
	 * FileOpenerをセットする
	 */
	public void setFileOpener(FileOpener fo) {
		fileOpener = fo;
	}
	
	public void setCursorListener(CursorListener l) {
		listener = l;
	}

	/**
	 * タブで区切られた別のパネルを作る
	 */
	public void generatePanels() {
		if(graph == null) return;
		
		if (textpanel != null) {
			textpanel.setVisible(false);
			textpanel = null;
			pane.remove(2);
		}
		if (vecpanel != null) {
			vecpanel.setVisible(false);
			vecpanel = null;
			pane.remove(1);
		}
		
		if(graph.attributeType == graph.ATTRIBUTE_VECTOR) {
			vecpanel = new VectorPanel(graph, (Object)canvas);
			pane.add(vecpanel);
			pane.setTabComponentAt(3, new JLabel("Vector"));
		}
		textpanel = new TextPanel(graph);
		pane.add(textpanel);
		pane.setTabComponentAt(4, new JLabel("Text"));
		
	}
	
	
	public void setPickedObject(Object picked) {
		if(textpanel != null)
			textpanel.setPickedObject(picked);
	}
	
	/**
	 * ラジオボタンのアクションの検出を設定する
	 * @param actionListener ActionListener
	 */
	public void addRadioButtonListener(ActionListener actionListener) {
		edgeDissimilarityButton.addActionListener(actionListener);
		edgeDegreeButton.addActionListener(actionListener);
		clickButton.addActionListener(actionListener);
		moveButton.addActionListener(actionListener);
		colorDegreeButton.addActionListener(actionListener);
		colorTopicButton.addActionListener(actionListener);
	}

	/**
	 * ボタンのアクションの検出を設定する
	 * @param actionListener ActionListener
	 */
	public void addButtonListener(ActionListener actionListener) {
		fileOpenButton.addActionListener(actionListener);
		placeAgainButton.addActionListener(actionListener);
		viewResetButton.addActionListener(actionListener);
	}
	
	
	/**
	 *CheckBoxのアクションの検出を設定する
	 * @param actionListener ActionListener
	 */
	public void addCheckBoxListener(ActionListener actionListener) {
	}
	
	
	/**
	 * スライダのアクションの検出を設定する
	 * @param actionListener ActionListener
	 */
	public void addSliderListener(ChangeListener changeListener) {
		bundleDensitySlider.addChangeListener(changeListener);
		bundleShapeSlider.addChangeListener(changeListener);
		confluenceDensitySlider.addChangeListener(changeListener);
		transparencySlider.addChangeListener(changeListener);
		clusteringRatioSlider.addChangeListener(changeListener);
		clusteringRatioSlider2.addChangeListener(changeListener);
		placeRatioSlider.addChangeListener(changeListener);
		clusterSizeSlider.addChangeListener(changeListener);
		clusterSizeSlider2.addChangeListener(changeListener);
		keyEmphasisSlider.addChangeListener(changeListener);
		rotationSlider.addChangeListener(changeListener);
		mergeSlider.addChangeListener(changeListener);
	}
	
	/**
	 * ボタンのアクションを検知するActionListener
	 * @or itot
	 */
	class ButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			
			JButton buttonPushed = (JButton) e.getSource();
			if(buttonPushed == fileOpenButton) {	
				// ファイルを指定する
				fileOpener.setCanvas(canvas);
				File datafile = fileOpener.getFile();
				currentDirectory = fileOpener.getCurrentDirectory();
				graph = fileOpener.readFile(datafile);
				canvas.setGraph(graph);
				generatePanels();
				canvas.viewReset();
				//graph.postBundleEv();
				canvas.display();			
				
				for(int i = 0; i < SMOOTHING_ITERATION; i++) {
					MeshTriangulator.triangulate(graph.mesh);
					MeshSmoother.smooth(graph.mesh, graph.maxDegree);
					canvas.display();
				}
				
				graph.mesh.finalizePosition();
				graph.mesh.generateMap();
				canvas.display();
			}	
			
			if(buttonPushed == placeAgainButton) {
				if(graph == null) return;
				
				graph.postprocess();
				canvas.setGraph(graph);
				canvas.viewReset();
				canvas.display();
				
				for(int i = 0; i < SMOOTHING_ITERATION; i++) {
					MeshTriangulator.triangulate(graph.mesh);
					MeshSmoother.smooth(graph.mesh, graph.maxDegree);
					canvas.display();
				}

				graph.mesh.finalizePosition();				
				canvas.display();			
			}
			
			if(buttonPushed == viewResetButton) {
				canvas.viewReset();
				canvas.display();
			}
		}
	}

	/**
	 * ラジオボタンのアクションを検知するActionListener
	 * @or itot
	 */
	class RadioButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JRadioButton buttonPushed = (JRadioButton) e.getSource();
			if(buttonPushed == edgeDissimilarityButton) {
				canvas.setEdgeDensityMode(canvas.EDGE_DENSITY_DISSIMILARITY);
				canvas.display();
			}
			if(buttonPushed == edgeDegreeButton) {
				canvas.setEdgeDensityMode(canvas.EDGE_DENSITY_DEGREE);
				canvas.display();
			}
			if(buttonPushed == clickButton) {
				listener.pickByMove(false);
			}
			if(buttonPushed == moveButton) {
				listener.pickByMove(true);
			}
			if(buttonPushed == colorTopicButton) {
				canvas.setColorMode(canvas.COLOR_TOPIC);
				canvas.display();
			}
			if(buttonPushed == colorDegreeButton) {
				canvas.setColorMode(canvas.COLOR_DEGREE);
				canvas.display();
			}
		}
	}
	

	/**
	 * チェックボックスのアクションを検知するActionListener
	 * @or itot
	 */
    class CheckBoxListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {

		}
	}
    
	/**
	 * スライダのアクションを検知するActionListener
	 * @or itot
	 */
	class SliderListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			JSlider sliderChanged = (JSlider) e.getSource();
			if(sliderChanged == bundleDensitySlider) {
				double ratio = (double)bundleDensitySlider.getValue() * 0.01;
				canvas.setEdgeDensityThreshold(ratio);
				canvas.display();
			}
			if(sliderChanged == bundleShapeSlider) {
				double ratio = (double)bundleShapeSlider.getValue() * 0.01;
				canvas.setBundleShape(ratio);
				canvas.display();
			}
			if(sliderChanged == confluenceDensitySlider) {
				double ratio = (double)confluenceDensitySlider.getValue() * 0.01;
				canvas.setConfluenceDensityThreshold(ratio);
				canvas.display();
			}
			if(sliderChanged == rotationSlider) {
				double ratio = (double)rotationSlider.getValue() * 0.01;
				canvas.setRotationStrength(ratio);
				canvas.display();
			}
			if(sliderChanged == mergeSlider) {
				double ratio = (double)mergeSlider.getValue() * 0.01;
				canvas.setMergeStrength(ratio);
				canvas.display();
			}
			if(sliderChanged == transparencySlider) {
				double ratio = 1.0 - (double)transparencySlider.getValue() * 0.003;
				canvas.setBackgroundTransparency(ratio);
				canvas.display();
			}
			if(sliderChanged == placeRatioSlider) {
				double ratio = (double)placeRatioSlider.getValue() * 0.01;
				NodeDistanceCalculator.setPlacementRatio(ratio);
			}
			if(sliderChanged == clusteringRatioSlider) {
				double ratio = (double)clusteringRatioSlider.getValue() * 0.01;
				NodeDistanceCalculator.setClusteringRatio(ratio);
			}
			if(sliderChanged == clusteringRatioSlider2) {
				double ratio = (double)clusteringRatioSlider2.getValue() * 0.01;
				NodeDistanceCalculator.setClusteringRatio2(ratio);
			}
			if(sliderChanged == clusterSizeSlider) {
				double ratio = (double)clusterSizeSlider.getValue() * 0.01;
				if(graph != null)
					graph.clustersizeRatio = ratio;
			}
			if(sliderChanged == clusterSizeSlider2) {
				double ratio = (double)clusterSizeSlider2.getValue() * 0.01;
				if(graph != null)
					graph.clustersizeRatio2 = ratio;
			}
			if(sliderChanged == keyEmphasisSlider) {
				double ratio = (double)keyEmphasisSlider.getValue() * 0.01;
				if(graph != null) {
					graph.mesh.keyEmphasis = ratio;
					canvas.display();			
				}
			}
		}
	}
	
}
